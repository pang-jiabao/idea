package com.symedsoft.insurance.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Maps;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.mapper.File2TableMapper;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.CatalogService;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.utils.*;
import com.symedsoft.insurance.vo.CommonRequestVO;
import com.symedsoft.insurance.vo.CommonResponseVO;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CatalogServiceImpl implements CatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceImpl.class);

    private static final String url =
            "http://localhost:8097/fsi/api/rsfComIfsService/callService";
    /** 按照报文要求传入 JSON 格式字符串 */
    private static final String downInput = "{…}";

    @Autowired
    private File2TableMapper file2TableMapper;

    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;

    /** 获取请求地址 */
    @Autowired
    private RequestParamConfig paramConfig;

    @Autowired
    private SignService signService;

    @Override
    public boolean doInsuranceAndParse (Map<String , Object> param) throws Exception {
        String type = MapUtils.getObject2String(param , "tableType");
        // 1、调用医保接口
        String jsonStr = MapUtils.getObject2String(param , "jsonStr");


        if (!"file".equals(type)) {
            return false;
        }

        byte[] outchar = new byte[1024] ;
        int result = ReadDll.INSTANCE.BUSINESS_HANDLE(jsonStr.getBytes("GBK") , outchar);
        String outStr = null;
        try {
            outStr = new String(outchar,"gbk").trim();
        } catch (UnsupportedEncodingException e) {
            logger.error("解析查询目录文件信息出参报错，{}" , e);
            throw e;
        }
        if (result != 0){
            logger.error("调用医保查询目录文件信息接口失败!");
            return false;
        }
        logger.info("调用目录下载的医保返回报文是: {}" , outStr.trim());


        // 2、解析返回报文
        CommonResponseVO response = JSONObject.parseObject(outStr , CommonResponseVO.class);
        if (!"0".equals(response.getInfCode())) {
            logger.error("调用医保查询目录文件信息接口返回失败,{}" , outStr);
            return false;
        }
        Map <String , Object> ret = response.getOutput();
        if (ret == null || ret.size() == 0) {
            logger.error("解析调用医保查询目录文件信息接口返回Output失败,{}" ,outStr);
            return false;
        }
        String fileQuryNo = MapUtils.getObject2String(ret , "file_qury_no");
        String filename = MapUtils.getObject2String(ret , "filename");
        Date dldEndTime = (Date) ret.get("dld_end_time");
        Integer dataCnt = (Integer) ret.get("data_cnt");

        // 3、组装 下载接口的入参报文
        String seq = interfaceMapper.getMsgIdSequence();
        CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
        HashMap<String,Object> input = new HashMap<>();
        Map<String , Object> downloadParam = Maps.newHashMap();
        downloadParam.put("file_qury_no" , fileQuryNo);
        downloadParam.put("filename" , filename);
        downloadParam.put("fixmedins_code" , "plc");
        input.put("fsDownloadIn" , downloadParam);


        request.setRecer_sys_code("YBXT");
        request.setInput(input);
        request.setInfno("9102");
        request.setSign_no(signService.getSignNo(paramConfig.getOperateNo()));

        // 4、将入参报文转换为 json字符串，并放入 param 中
        String inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        param.put("jsonStr" , inputStr);
        param.put("filename" , filename);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveFileIntoTable (Map<String , Object> param) throws Exception {

        logger.info("保存文件开始"+param);
        // 1、根据系统解析入参信息
        String jsonStr = MapUtils.getObject2String(param , "jsonStr");
        String tableName = MapUtils.getObject2String(param , "tableName");
        String filename = MapUtils.getObject2String(param , "filename").replace(".zip","");

        //获取字段长度
        int columnCount = Integer.valueOf(param.get("columnCount").toString());
        int jpNum = Integer.valueOf(param.get("jpNum").toString());
        // 2、走下载逻辑
        //先生成zip文件
        String path = "c:" + File.separator + "insur" + File.separator + "YBDLOAD" + File.separator;

        //通过读取配置中的url地址
//        DownLoadUtils.downloadFileFromInsurance(paramConfig.getUrl() , jsonStr, filename , path);
        //通过读取配置中的url地址
//        DownLoadUtils.downloadFileFromInsurance(paramConfig.getUrl() , jsonStr, filename , path);
        byte[] outChar =  new byte[1024] ;
        logger.info("下载文件入参-----"+jsonStr);
        int result = ReadDll.INSTANCE.BUSINESS_HANDLE(jsonStr.getBytes("GBK"),outChar);
        logger.info("下载文件出参-----"+new String(outChar,"gbk").trim());
        if (result < 0) {
            return "-1";
        }
        String outStr = null;
        try {
            outStr = new String(outChar,"gbk").trim();
        } catch (UnsupportedEncodingException e) {
            logger.error("解析调用下载目录文件出参报错：{}" , e);
            throw e;
        }

        String ret = "1";
        File file = new File(path + filename + File.separator + filename);
        logger.info("----目录下载，文件下载成功！执行取文件写入表操作。");
        try (FileInputStream fileInputStream = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
             BufferedReader br = new BufferedReader(inputStreamReader)) {
            Map<String , Object> response = JSONObject.parseObject(outStr , Map.class);
            if ("-1".equals(MapUtils.getObject2String(response , "infcode"))) {
                return "-1";
            }

            // 4、读取数据文件
//            BufferedReader br = new BufferedReader(new FileReader());


            String line = null;
            List<List<String>> list = new ArrayList<>();

            //匹配 Mon Dec 28 14:04:04 CST 2020
            String regEx = "[A-Z][a-z]{2}\\s[A-Z][a-z]{2}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\sCST\\s\\d{4}";
            Pattern pattern = Pattern.compile(regEx);

            //不是删除全部，而是增量更新
            //取list中每一行的第一列记录组成 主键list，然后删除目录表中有这个主键记录的数据
            String primaryKey = file2TableMapper.queryPrimaryKeyByTableName(tableName);
            logger.info("表名为：{}，主键为：{}", tableName , primaryKey);

            // 5、插入数据，并跟新版本号
            boolean flag = true;

            while((line = br.readLine()) != null) {
                List<String> record = new ArrayList<>();

                String[] data = line.split("\t");
                if (data.length != columnCount) {
                    logger.info("目录下载出错，文本文件长度与目录表长度不一致！文件长度为：{}，表长度为：{}" , data.length , columnCount);
                }
                int jpCount = 1;
                String jp = "";
                for (int i=0;i<columnCount;i++) {
                    String s = null;
                    if (i<data.length){
                        s = data[i];
                        if ("null".equals(s)) {
                            s = null;
                        } else if (s.indexOf("CST") > -1) {
                            Matcher matcher = pattern.matcher(s);
                            if (matcher.matches()) {
                                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
                                Date date = (Date) sdf.parse(s);
                                s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                            }
                        }
                    }
                    //logger.info("===答应出来的文本内容为：{}" , s);
                    record.add(s);

                    if (jpCount == jpNum) {
                        if (StringUtils.isNotEmpty(s)) {
                            jp = PinyinUtils.toFirstChar(s , HanyuPinyinCaseType.LOWERCASE);
                        }
                    }

                    jpCount ++;
                    if(jpCount>columnCount){
                        break;
                    }
                }
                logger.info("list的长度" +record.size());
                logger.info("表的长度" +columnCount);
                //增加简拼字段的值
                record.add(jp);
//                list.add(record);
                int i = file2TableMapper.delTableDataById(tableName , primaryKey , record.get(0));
                logger.info("删除返回值：{}" , i);
                i = file2TableMapper.addDataIntoTableByOne(record , tableName);
                logger.info("插入记录返回值：{}" , i);
                if (i < 1) {
                    flag = false;
                    ret = "-1";
                    break;
                }
            }



            //避免oracle批量只能插入1000个参数
//            int maxCountSize = 1000/columnCount ;
//            logger.info("tablename为{}" , tableName);
//            if (list.size() > maxCountSize) {
//                List<List<List<String>>> groupList = ListUtils.splitList(list , maxCountSize);
//                for (List<List<String>> record : groupList) {
//
//                    List<String> ids = record.stream().map(p -> p.get(0)).collect(Collectors.toList());
////                    int i = file2TableMapper.delAllTableData(tableName , primaryKey , record.get(0));
////                    logger.info("删除返回值：{}" , i);
//
//                    int total = file2TableMapper.addDataIntoTable(record , tableName);
//                    if (total != record.size()) {
//                        flag = false;
//                        throw new Exception("写入目录记录表失败！");
//                    }
//                }
//            } else {
//                List<String> ids = list.stream().map(p -> p.get(0)).collect(Collectors.toList());
//                int i = file2TableMapper.delAllTableData(tableName , primaryKey , ids);
//                logger.info("删除返回值：{}" , i);
//                int total = file2TableMapper.addDataIntoTable(list , tableName);
//                if (total == 0) {
//                    flag = false;
//                    throw new Exception("写入目录记录表失败！");
//                }
//            }

            if (flag) {
                String version = file2TableMapper.queryVersionByTable(tableName);
                file2TableMapper.modifyVerByTable(version , tableName);
            }
        } catch (Exception e) {
            logger.error("解析调用下载目录文件出参报错：{}" , e);
            throw e;
        }

        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String test() throws Exception {
        List<String> record = new ArrayList<>();
        record.add("1");
        record.add("测试回滚");
        record.add("测试回滚");
        record.add("2");
        record.add("1");
        List<List<String>> records = new ArrayList<>();
        records.add(record);
        int i = file2TableMapper.addDataIntoTable(records ,"insur_vs_clinic_bak");
        throw new UnsupportedEncodingException("走回滚");

    }
}
