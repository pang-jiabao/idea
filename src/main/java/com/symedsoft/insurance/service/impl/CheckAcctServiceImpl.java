package com.symedsoft.insurance.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.config.UploadParamConfig;
import com.symedsoft.insurance.mapper.CheckAcctMapper;
import com.symedsoft.insurance.mapper.File2TableMapper;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.CheckAcctService;
import com.symedsoft.insurance.service.InsuranceConfigService;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.utils.*;
import com.symedsoft.insurance.vo.CommonRequestVO;
import com.symedsoft.insurance.vo.CommonResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.hutool.core.date.DatePattern.PURE_DATETIME_FORMAT;

@Service
public class CheckAcctServiceImpl implements CheckAcctService {
    private static final Logger logger = LoggerFactory.getLogger(CheckAcctServiceImpl.class);

    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;

    @Autowired
    private CheckAcctMapper checkAcctMapper;

    @Autowired
    private SignService signService;

    @Autowired
    private File2TableMapper file2TableMapper;

    @Autowired
    private RequestParamConfig requestParamConfig;

    @Autowired
    private UploadParamConfig uploadParamConfig;



    @Override
    public String doCheckAcctDetail(String serialNo) throws Exception {
        //1、根据 serialNo 查询 入参表信息，取对账时间
        Map<String , Object> param = checkAcctMapper.queryCheckDetail(serialNo);
        if (MapUtil.isEmpty(param)) {
            logger.error("并未找到serialNo为:{} 的入参信息！" , serialNo);
            return "并未找到serialNo为:"+serialNo+"的入参信息！";
        }
        Date beginDate = (Date) param.get("stmt_begndate");
        Date endDate = (Date) param.get("stmt_enddate");
        if (beginDate == null || endDate == null) {
            logger.error("serialNo为: {} 的入参信息中未写开始时间或结束时间！" , serialNo);
            return "serialNo为: "+serialNo+"的入参信息中未写开始时间或结束时间！";
        }
        String beginTime = DateUtil.formatDate(beginDate) + " 00:00:00";
        String endTime = DateUtil.formatDate(endDate) + " 23:59:59";

        //2、根据对账时间取待上传数据
        Map<String , Object> queryDetailParam = Maps.newHashMap();
        List<Map<String , Object>> queryDetailData = Lists.newArrayList();
        queryDetailParam.put("beginTime" , beginTime);
        queryDetailParam.put("endTime" , endTime);

        queryDetailParam.put("type" , 1);
        queryDetailData.addAll(checkAcctMapper.queryDetailDataByTime(queryDetailParam));

        queryDetailParam.put("type" , 2);
        queryDetailData.addAll(checkAcctMapper.queryDetailDataByTime(queryDetailParam));
//
        queryDetailParam.put("type" , 3);
        queryDetailData.addAll(checkAcctMapper.queryDetailDataByTime(queryDetailParam));

        queryDetailParam.put("type" , 4);
        queryDetailData.addAll(checkAcctMapper.queryDetailDataByTime(queryDetailParam));

        //3、对待上传数据进行校验
        BigDecimal count = (BigDecimal) param.get("fixmedins_setl_cnt");
        String patientId = MapUtils.getObject2String(param , "patient_id");
        String visitId = MapUtils.getObject2String(param , "visit_id");
        Date visitDate = (Date) param.get("visit_date");
        String operateNo = MapUtils.getObject2String(param , "operate_no");

//        if (count.intValue() != queryDetailData.size()) {
//            logger.error("待对账条数与入参表中记录数量不一致，入参表中数量为：{}，查询数量为：{}" ,
//                    count.intValue() , queryDetailData.size());
//            return;
//        }

        //4、生成数据文件并压缩
        List<String> sortList = Stream.of(uploadParamConfig.getQueryUpload().split(",")).collect(Collectors.toList());
        if (sortList == null || sortList.size() == 0) {
            logger.error("config表未有填报项数据。");
            return "config表未有填报项数据";
        }
        String path = System.getProperty("user.dir");
        String sourceFileNamePath = path + File.separator + "test.txt";

        if (!FileUtils.writeList2File(queryDetailData , sortList , sourceFileNamePath)){
            logger.error("写文件失败！");
            return "写入上传文件失败";
        }

        //压缩文件
        String targetFileName = "data" + DateUtil.format(new Date() , PURE_DATETIME_FORMAT) + ".zip";
        String targetZipFileNamePath = path + File.separator + targetFileName;
        ZipUtils.toZip(sourceFileNamePath , targetZipFileNamePath , false);

        //5、将文件转换成base64，并调用上传接口上传该文件
        String base64Code = FileUtils.file2Base64(targetZipFileNamePath);
        logger.info("base64字符串为：{}" , base64Code);
        String seq = interfaceMapper.getMsgIdSequence();
        CommonRequestVO request = new CommonRequestVO(requestParamConfig , seq);
        //入参数据
        HashMap<String,Object> input = new HashMap<>();
        Map<String , String> params = Maps.newHashMap();
        params.put("in" , base64Code);
        params.put("filename" , targetFileName);
        params.put("fixmedins_code" , requestParamConfig.getFixmedins_code());
        input.put("fsUploadIn" , params);
        request.setSign_no(signService.getSignNo(MapUtils.getObject2String(param,"operate_no")));
        request.setInput(input);
        request.setInfno("9101");
        request.setRecer_sys_code("YBXT");
        String inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);

        byte[] outputchar = new byte[1024*1024] ;
        String outputStr = null;
        int res = 0;
        try {
            logger.info("9101" +"*************************医保调用开始*************************" );
            logger.info("9101"+"**************入参内容:*********:"+inputStr);
            res = ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outputchar);
            outputStr = new String(outputchar,"GBK").trim();
            logger.info("9101"+"**************出参内容:*********:"+outputStr.trim());
            logger.info("9101" + "*************************医保调用结束*************************");
        } catch (Exception e) {
            logger.error("上传文件调用医保接口异常：{}", e);
            return "上传文件调用医保接口异常"+e.getMessage();
        }

        if (res < 0) {
            logger.error("上传文件调用医保接口失败！");
            return "上传文件调用医保接口失败"+ outputStr;
        }

        //6、解析出参，并将 file_qury_no 加入入参，并调用3202接口
        CommonResponseVO response = JSONObject.parseObject(outputStr , CommonResponseVO.class);
        if (!"0".equals(response.getInfCode())) {
            logger.error("上传失败，医保接口返回异常，{}" , outputStr);
            return "上传失败，医保接口返回异常"+outputStr;
        }
        Map <String , Object> ret = response.getOutput();
        String fileQuryNo = MapUtils.getObject2String(ret , "file_qury_no");
        //重新包装入参并调用对账接口
        param.put("file_qury_no" , fileQuryNo);
        input.clear();
        input.put("data" , param);
        request.setInput(input);
        request.setInfno("3202");
        param.put("stmt_begndate",new SimpleDateFormat("yyyy-MM-dd").format(beginDate));
        param.put("stmt_enddate",new SimpleDateFormat("yyyy-MM-dd").format(endDate));
        inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        try {
            logger.info("3202" +"*************************医保调用开始*************************" );
            logger.info("3202"+"**************入参内容:*********:"+inputStr);
            outputchar = null;
            outputchar = new byte[1024*100];
            res = ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outputchar);
            outputStr = new String(outputchar,"GBK").trim();
            logger.info("3202"+"**************出参内容:*********:"+outputStr.trim());
            logger.info("3202" + "*************************医保调用结束*************************");
        } catch (Exception e) {
            logger.error("详细对账调用医保接口异常：{}", e);
            return "详细对账调用医保接口异常："+ e.getMessage();
        }

        if (res < 0) {
            logger.error("详细对账调用医保接口调用失败！");
            return "详细对账调用医保接口调用失败"+outputStr;
        }
        //7、解析出参，获取需要下载的文件的file_qury_no，并调用下载
        response = JSONObject.parseObject(outputStr , CommonResponseVO.class);
        if (!"0".equals(response.getInfCode())) {
            logger.error("详细对账调用医保接口调返回异常，{}" , outputStr.trim());
            return "详细对账调用医保接口调返回异常:"+outputStr;
        }
        ret = response.getOutput();

        //8、将出参加入入参，继续下载文件
        Map<String , Object> fileinfo = (Map) ret.get("fileinfo");
        fileQuryNo = MapUtils.getObject2String(fileinfo , "file_qury_no");
        targetFileName = MapUtils.getObject2String(fileinfo , "filename");

        param.clear();
        param.put("file_qury_no" , fileQuryNo);
        param.put("filename" , targetFileName);
        param.put("fixmedins_code" , "plc");
        input.clear();
        input.put("fsDownloadIn" , param);
        request.setInput(input);
        request.setInfno("9102");
        inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        try {
            logger.info("9102" +"*************************医保调用开始*************************" );
            logger.info("9102"+"**************入参内容:*********:"+inputStr);
            outputchar = null;
            outputchar = new byte[1024];
            res = ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outputchar);
            outputStr = new String(outputchar,"GBK").trim();
            logger.info("9102"+"**************出参内容:*********:"+outputStr.trim());
            logger.info("9102" + "*************************医保调用结束*************************");
        } catch (Exception e) {
            logger.error("下载文件调用医保接口异常：{}", e);
            return "下载文件调用医保接口异常"+e.getMessage();
        }

        if (res < 0) {
            logger.error("下载文件调用医保接口失败！");
            return "下载文件调用医保接口失败:"+outputStr;
        }
        //解析出参
        logger.info("下载文件的出参字符串为：{}" , outputStr.trim());
        Map<String , Object> finalResponse = JSONObject.parseObject(outputStr , Map.class);
        if ("-1".equals(MapUtils.getObject2String(finalResponse , "infcode"))) {
            logger.error("下载文件调用医保接口返回异常", outputStr);
            return "下载文件调用医保接口返回异常"+outputStr;
        }
        path = MapUtils.getObject2String(finalResponse , "output");
        File file=new File(path + File.separator + "result.txt");
        if (!file.exists()){
            return path + File.separator + "result.txt 文件不存在";
        }
        BufferedReader br = new BufferedReader(new FileReader(path + File.separator + "result.txt"));

        String line = null ;
        List<List<String>> list = new ArrayList<>();

        while((line = br.readLine()) != null) {
            List<String> record = new ArrayList<>();
            record.add(serialNo);
            record.add(patientId);
            record.add(visitId);
            if (visitDate != null) {
                record.add(visitDate.toString());
            } else {
                record.add("");
            }
            record.add(operateNo);
            String[] data = line.split("\t");
            //数据库长度为7 ，一行只能存7条
            for (int i = 0; i < 7; i++) {
                record.add(data[i]);
            }
            list.add(record);
        }

        // 5、插入数据，并跟新版本号

        param.clear();
        param.put("table" , "insur_check_out_des");
        param.put("operateNo" , operateNo);
        boolean flag = true;
        interfaceMapper.delTableByOperateNo(param);
        if (list.size() > 100) {
            List<List<List<String>>> groupList = ListUtils.splitList(list , 100);
            for (List<List<String>> record : groupList) {
                int total = file2TableMapper.addDataIntoTable(record , "insur_check_out_des");
                if (total != record.size()) {
                    flag = false;
                    throw new Exception("写入目录记录表失败！");
                }
            }
        } else {
            int total = file2TableMapper.addDataIntoTable(list , "insur_check_out_des");
            if (total == 0) {
                flag = false;
            }
        }

        return "1";
    }
}
