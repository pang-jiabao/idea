package com.symedsoft.insurance.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.exception.CustomException;
import com.symedsoft.insurance.mapper.AutoUploadMapper;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.service.UpLoadAndPreSettleService;
import com.symedsoft.insurance.utils.AssertUtils;
import com.symedsoft.insurance.utils.ListUtils;
import com.symedsoft.insurance.utils.MapUtils;
import com.symedsoft.insurance.utils.ReadDll;
import com.symedsoft.insurance.vo.CommonRequestVO;
import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.InsurBusinessConfigVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/*
 *@author：LL
 *@Date:2021-08-18
 *@Description
 */
@Service
public class UpLoadAndPreSettleServiceImpl implements UpLoadAndPreSettleService {
    public static final int BATCH_SIZE = 40;
    @Autowired
    private AutoUploadMapper autoUploadMapper;
    @Autowired
    private RequestParamConfig paramConfig;
    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;
    @Autowired
    private SignService signService;

    /**
     * 预结算
     * @param patientId
     * @param visitId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String preSettle(String patientId, int visitId,String serialNo) throws Exception {
        autoUploadMapper.deletePreSettleDataIn(patientId,visitId);
        autoUploadMapper.deletePreSettleSetldetailOut(patientId, visitId);
        autoUploadMapper.deletePreSettleSetlinfoOut(patientId,visitId);
        List<Map<String,Object>> inputList = autoUploadMapper.getPreSettleInput(patientId,visitId);
        if(AssertUtils.isEmptyList(inputList) || inputList.size() > 1 ){
            throw new CustomException("预结算-获取预结算入参错误");
        }
        CommonRequestVO request = getInputStr(patientId,"2303");
        HashMap<String, Object> input = new HashMap<>();
        input.put("data", inputList.get(0));
        //添加入参节点信息
        request.setInput(input);
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero);
        callinsur("2303", serialNo, json,patientId,visitId);
        return "1";
    }

    /**
     * 调用拼接医保入参
     * @param patientId
     */
    private CommonRequestVO getInputStr(String patientId,String interfaceCode) {
        try{
            //开始组织医保请求入参requestVO ，入参节点数据input
            String seq = interfaceMapper.getMsgIdSequence();
            CommonRequestVO request = new CommonRequestVO(paramConfig, seq);
            //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
            request.setInfno(interfaceCode);
            request.setRecer_sys_code("YBXT");
            List<Map<String, String>> m = interfaceMapper.getInsuplcAdmdvs(patientId);
            String insuplc_admdvsr = "";
            if (m == null || m.size() == 0) {
                insuplc_admdvsr =paramConfig.getMdtrtarea_admvs();
            } else {
                if (m.get(0).get("psn_type").equals("1300")){
                    request.setRecer_sys_code("LXXT");
                }
                if (m.get(0).get("insuplc_admdvs") == null) {
                    insuplc_admdvsr = paramConfig.getMdtrtarea_admvs();
                } else {
                    insuplc_admdvsr = m.get(0).get("insuplc_admdvs");
                }
            }
            request.setInsuplc_admdvs(insuplc_admdvsr);
            //查询经办人信息
            String operateNo = "9999";
            String operateName = "自动上传";
            request.setOpter_type("1");
            request.setOpter(operateNo);
            request.setOpter_name(operateName);
            request.setSign_no(signService.getSignNo(operateNo));
            return request;
        }catch (Exception e){
            throw e;
        }
    }

    /**
     * 调医保并保存
     * @param serialNo
     * @param json
     * @return 出参的output节点
     */
    public Map<String, Object> callinsur(String interfaceCode, String serialNo, String json,
                                         String patientId,int visitId) throws Exception {
        byte[] outpchar = new byte[1024*1024];
        int result = ReadDll.INSTANCE.BUSINESS_HANDLE(json.getBytes("gbk"), outpchar);
        String outpStr = new String(outpchar, "gbk");
        if (result != 0) {
            throw new CustomException(interfaceCode + "医保调用失败:" + outpStr.trim());
        }
        //保存出参
        AssertUtils.notBlank(outpStr, interfaceCode + "************-医保调用失败-出参");
        //将医保出参解析为responseVO
        CommonResponseVO response = JSONObject.parseObject(outpStr, CommonResponseVO.class);
        if (response == null || AssertUtils.isBlank(response.getInfCode())) {
            throw new CustomException(interfaceCode + "医保调用失败" + outpStr.trim());
        }
        if (!"0".equals(response.getInfCode())) {
            throw new CustomException(interfaceCode + "医保调用失败-" + response.getErr_msg());
        }
        HashMap<String, Object> log = new HashMap<>();
        log.put("patientId", patientId);
        log.put("visitId", visitId);
        log.put("visitDate", null);
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode, "O");
        if (AssertUtils.isEmptyList(configList)) {
            throw new CustomException(interfaceCode + "医保保存失败-无insur_business_config");
        }
        Map<String, Object> out = response.getOutput();
        if (out == null || out.isEmpty()) {
            throw new CustomException(interfaceCode + "医保保存失败-医保出参output无数据");
        }

        for (InsurBusinessConfigVO config : configList) {
            //出参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table = config.getNode_table();
            if (StringUtils.isEmpty(table) || StringUtils.isEmpty(node)) {
                throw new CustomException(interfaceCode + "医保保存失败-无出参表和出参节点配置");
            }
            short nodeType = config.getMulti_line();
            //该出参节点为时间类型（天、时分秒）的字段，用于后面转换为对应的date，存入出参表
            String dateType = config.getNode_date_type() == null ? "" : config.getNode_date_type();
            String timeType = config.getNode_time_type() == null ? "" : config.getNode_time_type();
            //该出参节点的出参数据（单行、多行批量保存）
            List<Map<String, Object>> nodeDataList = new ArrayList<>();
            //判断出参节点的类型：单行/多行
            if (nodeType == 0) {
                //单行
                Map<String, Object> nodeJson = (Map<String, Object>) out.get(node);
                //该出参节点，在出参json串中找不到，记录异常，返回
                if (nodeJson == null || nodeJson.isEmpty()) {
                    throw new CustomException(interfaceCode + "医保保存失败-无" + node + "节点出参");
                }
                nodeDataList.add(nodeJson);
            } else {
                //多行
                nodeDataList = (List<Map<String, Object>>) out.get(node);
                //该出参节点，在出参json串中找不到，记录异常，返回
                if (nodeDataList == null || nodeDataList.isEmpty()) {
                    throw new CustomException(interfaceCode + "医保保存失败-无" + node + "节点出参");
                }
            }
            /*
             * 1.获取此节点在数据库中的出参配置字段
             * 2.获取此节点出参数据的key的顺序，批量保存foreach时，出参表字段顺序
             * 3.剔除医保出参字段（该字段在数据库中没有配置）
             */
            List<String> nodeColumnList = interfaceMapper.getNodeColumn(interfaceCode, "O", node);
            Set<String> keySet = nodeDataList.get(0).keySet();
            //剔除无配置的出参
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (!nodeColumnList.contains(key)) {
                    it.remove();
                }
            }
            /*
             * 转换此节点的出参数据 List<Map<String,Object>>  —— nodeDataList
             * 1.将每个Map<String,Object>的val值转换为list<Object>
             * 2.将为时间类型的出参数据根据格式转换为date
             */
            //LOGGER.info("=============转换出参信息开始==============");
            List<Map<String, Object>> listData = MapUtils.tranInsertObject(nodeDataList, nodeColumnList, dateType, timeType);
            /*
             * 保存该节点的出参数据 List<List<Object>> ——listData
             * listData数据量大时分批次上传
             */
            //LOGGER.info("=============保存出参信息开始==============");
            int n = 0;
            if (listData.size() > BATCH_SIZE) {
                List<List<Map<String, Object>>> group = ListUtils.splitList2(listData, BATCH_SIZE);
                for (List<Map<String, Object>> dataList : group) {
                    n = n + interfaceMapper.insertOut(table, serialNo, dataList, log);
                }
            } else {
                n = n + interfaceMapper.insertOut(table, serialNo, listData, log);
            }
            if (n != listData.size()) {
                throw new CustomException(interfaceCode + "医保保存失败-insert数量不等于出参返回数量");
            }
        }
        return out;
    }
}
