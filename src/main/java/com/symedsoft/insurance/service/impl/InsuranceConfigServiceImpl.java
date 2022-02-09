package com.symedsoft.insurance.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.mapper.File2TableMapper;
import com.symedsoft.insurance.service.LogService;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.utils.ListUtils;
import com.symedsoft.insurance.utils.ReadDll;
import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.InsurBusinessConfigVO;
import com.symedsoft.insurance.exception.CustomException;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.InsuranceConfigService;
import com.symedsoft.insurance.utils.AssertUtils;
import com.symedsoft.insurance.utils.MapUtils;
import com.symedsoft.insurance.vo.CommonRequestVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

/*
 *@author：LL
 *@Date:2021/5/13
 *@Description
 */
@Service
public class InsuranceConfigServiceImpl implements InsuranceConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsuranceConfigServiceImpl.class);
    /**
     * 出参保存分批次保存的size
     */
    public static int BATCH_SIZE = 100;

    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;

    @Autowired
    private LogService logService;

    @Autowired
    private RequestParamConfig paramConfig;

    @Autowired
    private SignService signService;

    @Autowired
    private File2TableMapper file2TableMapper;

    /**
     * 获取入参json串
     * @param serialNo  入参编号
     * @param interfaceCode 接口编码
     * @param verify 是否校验 操作者等信息
     * @return
     */
    @Transactional
    @Override
    public String getInputJsonStr(String serialNo, String interfaceCode, boolean verify) throws UnsupportedEncodingException {
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"I");
        AssertUtils.notEmptyList(configList,"接口" + interfaceCode + "-" + serialNo + "：无此接口入参配置");


        HashMap<String,Object> input = new HashMap<>();
        //logSaveNode用于存日志
        Map<String,Object> logSaveNode = null;
        String nodePatientid="";
        //遍历该接口的节点配置信息，获取每个节点对应入参表的数据
        for(InsurBusinessConfigVO config : configList){
            //入参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table  = config.getNode_table();
            if (AssertUtils.isBlank(table) || AssertUtils.isBlank(node)) {
                throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：无节点或对应入参表的配置信息");
            }
            short nodeType = config.getMulti_line();
            /*
            *该节点入参字段中：时间、数值类型的字段
            * 用于后面 1.解析成对应的时间格式  2.入参为空时的默认值设置
            */
            String dateType = config.getNode_date_type();
            String timeType = config.getNode_time_type();
            String numberType =  config.getNode_number_type();
            //获取该节点入参表的数据
            List<Map<String,Object>> inputList= interfaceMapper.getInputByTableNameAndSerialNo(table,serialNo);
            nodePatientid=table;
            AssertUtils.notEmptyList(inputList,interfaceCode +"-" +"serialNo" +"：无此入参数据");

            //保存接口发请求日志时，只需要取公共字段，只取一次节点信息
            if(logSaveNode == null ){
                logSaveNode = inputList.get(0);
            }
            /*
            *将节点入参数据转为符合医保接口入参的格式
            * 1.map的key转为小写
            * 2.时间字符串格式转换
            * 3.为空时的默认值设置：空串默认"",数值默认0
            * 4.去掉入参表的6个公共字段："ID","SERIAL_NO","PATIENT_ID","VISIT_ID","VISIT_DATE","OPERATE_NO"
            */
//            LOGGER.info("需要转换的入参表为{}，以及需要转换的dateType为{}，timeType为{}，numberType为{}",
//                    table , dateType , timeType , numberType);
            inputList = MapUtils.transKeyToLower(inputList,dateType,timeType,numberType);
            //判断此节点的单行/多行入参
            if(nodeType == 0){
                //单行
                if("3503".equals(interfaceCode)){
                    inputList.get(0).put("prodentp_name",inputList.get(0).get("prdr_name"));
                }
                input.put(node,inputList.get(0));

            }else{
                //多行
                input.put(node,inputList);
            }
        }

        //st:查询此接口的扩展字段

        List<Map<String,String>> expPropertyList = interfaceMapper.selectExpProperty(interfaceCode, "I");
        for(Map<String,String> property :expPropertyList){
            //获取该扩展字段的标识，对应入参表，该扩展字段对应的node节点
            String proName = property.get("property");
            String table = property.get("table");
            String propertyType = property.get("propertyType");
            String node = property.get("node");
            String nodeType = property.get("nodeType");
            //获取扩展字段入参表的数据
            List<Map<String,Object>> expPropertyDataList = interfaceMapper.getInputByTableNameAndSerialNo(table,serialNo);
            String jsonstr = "";
            if(AssertUtils.isEmptyList(expPropertyDataList)){
                //扩展字段入参表无数据
                jsonstr = "0".equals(propertyType) ? "{}" : "[]";
                if("0".equals(nodeType)){
                    //扩展字段父节点为单行输入
                    ((Map<String, Object>) input.get(node)).put(proName,jsonstr);
                }else{
                    //扩展字段父节点为多行输入
                    List<Map<String,Object>> nodeDataList = (List<Map<String, Object>>) input.get(node);
                    for(Map<String,Object> m : nodeDataList){
                        m.put(proName,jsonstr);
                    }
                }
            }else{
                //扩展字段入参表有数据：输入转换key大小写
                expPropertyDataList = MapUtils.transKeyToLower(expPropertyDataList,property.get("dateType"),
                        property.get("timeType"),property.get("numberType"));

                //判断扩展字段的父node是单行输入或多行输入
                if("0".equals(nodeType)){    //父节点单行输入
                    //去除扩展入参的node_num
                    for(Map<String,Object> data : expPropertyDataList){
                        data.remove("node_num");
                    }
                    //扩展字段json字符串:根据扩展字段的类型是单行或数组
                    jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? expPropertyDataList.get(0) : expPropertyDataList
                            , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                            ,SerializerFeature.WriteNullNumberAsZero);
                    //设置扩展字段json串
                    Map<String,Object> nodeData = (Map<String, Object>) input.get(node);
                    nodeData.put(proName,jsonstr);
                }else{   //扩展字段父节点为多行输入
                    //获取父节点的多行入参数据
                    List<Map<String,Object>> nodeDataList = (List<Map<String, Object>>) input.get(node);
                    /*
                    * 父node为多行输入时：查找与该行输入对应的扩展字段入参数据（1...1或1...*）
                    * 扩展字段入参表的NODE_NUM与node入参表的 EXP_CONTENT对应
                    */
                    for(Map<String,Object> m : nodeDataList){
                        /*
                        当前处理：扩展字段父节点为多行输入，且每行的扩展字段数据都不一致，通过node_num一一对应每行各自的扩展字段数据
                        String exp_content = (String) m.get(proName);
                        //节点入参表exp_content无数据，赋值空json串
                        if(AssertUtils.isBlank(exp_content)){
                            m.put(proName,"0".equals(propertyType) ? "{}" : "[]");
                            continue;
                        }
                        //获取扩展入参数据中与node行数据对应的数据
                        List<Map<String,Object>> nodeExpDataList = expPropertyDataList.stream()
                                    .filter(exp -> exp_content.equals(exp.get("node_num")))
                                    .collect(Collectors.toList());
                        //该node入参数据行无扩展入参
                        if(AssertUtils.isEmptyList(nodeExpDataList)){
                            m.put(proName,"0".equals(propertyType) ? "{}" : "[]");
                            continue;
                        }
                        for(Map<String,Object> data : expPropertyDataList){
                            data.remove("node_num");
                        }
                        jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? nodeExpDataList.get(0) : nodeExpDataList
                                , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                                ,SerializerFeature.WriteNullNumberAsZero);
                        m.put(proName,jsonstr);
                        */

                        //默认多行输入时，每行的扩展字段参数一致
                        //去除扩展入参的node_num
                        for(Map<String,Object> data : expPropertyDataList){
                            data.remove("node_num");
                        }
                        jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? expPropertyDataList.get(0) : expPropertyDataList
                                , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                                ,SerializerFeature.WriteNullNumberAsZero);
                        m.put(proName,jsonstr);
                    }
                }
            }
        }
        //ed:查询此接口的扩展字段

        //开始组织医保请求入参requestVO ，入参节点数据input
        String seq = interfaceMapper.getMsgIdSequence();
        //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
        CommonRequestVO request = new CommonRequestVO(paramConfig,seq);
        request.setInfno(interfaceCode);
        //查询经办人信息
        if (verify){
            AssertUtils.notNull(logSaveNode.get("OPERATE_NO"), "操作人不能为空");
            String operateNo = logSaveNode.get("OPERATE_NO").toString();
            String operateName = interfaceMapper.selectOperateNameByNo(operateNo);
            request.setOpter_type("1");
            request.setOpter(operateNo);
            request.setOpter_name(operateName);
        }

        request.setRecer_sys_code("YBXT");
        // todo 临时注释
        request.setSign_no(signService.getSignNo(request.getOpter()));

        //添加入参节点信息
        request.setInput(input);
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        //若人员编号交易输入中含有人员编号，insuplc_admdvs必填，可查人员信息获取交易出参表取得
        if(json.indexOf("\"psn_no\":") > 0){
            Object patientIdobj =null;
            if (logSaveNode.get("PATIENT_ID")!=null){
                patientIdobj=logSaveNode.get("PATIENT_ID");
            }else {
                Map<String,Object> expPropertyDataList = interfaceMapper.getPatientIdforInp(nodePatientid,serialNo);
                patientIdobj=expPropertyDataList.get("PATIENT_ID");
            }
            String patientId="";
            if (patientIdobj!=null){
                patientId=patientIdobj.toString();
            }

            List<Map<String,String>> m = interfaceMapper.getInsuplcAdmdvs(patientId);
            String insuplc_admdvsr="";
            if (m.size()==0){
                insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
            }else {
                //System.out.println(patientId);
                //AssertUtils.notEmptyList(m,"病人无参保信息");
                if (m.get(0).get("psn_type").equals("1300")){
                    request.setRecer_sys_code("LXXT");
                }
                if (m.get(0).get("insuplc_admdvs")==null){
                    insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
                }else {
                    insuplc_admdvsr=m.get(0).get("insuplc_admdvs");
                }
            }

            request.setInsuplc_admdvs(insuplc_admdvsr);
            json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        }
        //新增医保接口请求log
        logService.addRequestLog(serialNo,interfaceCode,logSaveNode,json,request.getMsgid());
        return json;
    }

    /**
     * 保存医保出参
     * @param serialNo 入参序列号
     * @param interfaceCode 接口编号
     * @param outpStr 接口出参
     * @param delOldData 删除老记录
     */
    @Transactional
    @Override
    public String saveOutputStr(String serialNo, String interfaceCode, String outpStr , boolean delOldData) throws Exception{
        AssertUtils.notBlank(outpStr, "出参");
        JSONObject jsonObject=JSONObject.parseObject(outpStr);
        Object output = jsonObject.get("output");
        System.out.println(output);
        if ("null".equals(output)){
            jsonObject.put("output",null);
        }
        //将医保出参解析为responseVO
        CommonResponseVO response = JSONObject.parseObject(jsonObject.toJSONString(),CommonResponseVO.class);
        if (response == null || AssertUtils.isBlank(response.getInfCode())) {
           throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：response 无公共响应信息");
        }
        //医保接口调用失败-1，保存日志，直接return -1
        if (!"0".equals(response.getInfCode())) {
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            //throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            return "-1";
        }else{
            //接口调用成功不保存出参json
            logService.updateJsonLog(serialNo,interfaceCode,response,"","");
        }

        /*医保接口调用成功时保存逻辑
         * 保存响应的外层数据：infno、inf_refmsgid、refmsg_time、respond_time、err_msg
         *  update log表（入参解析时新增的日志记录）
         */
        //查询patientId、operateNo、visitDate、visitId
        HashMap<String,Object> log = logService.queryLogBySerialNo(serialNo, interfaceCode).get(0);

        /*保存输出的节点信息：output
        * 1.查询config出参节点配置
        * 2.遍历配置的节点信息，保存数据
        */
        //当前无出参配置，无需保存output，直接返回成功：1
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"O");
        if(AssertUtils.isEmptyList(configList)) {
            return "1";
        }

        //有出参配置，返回却无output节点信息，保存此异常信息，返回失败：-1
        Map<String,Object> out = response.getOutput();
        if (out == null || out.isEmpty()) {
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：response无output信息");
            response.setErr_msg("response无output信息");
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
//            throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：response无output信息");
            return "-1";
        }

        //遍历出参节点配置信息
        for(InsurBusinessConfigVO config : configList){
            //出参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table  = config.getNode_table();
            if (StringUtils.isEmpty(table)) {
                LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：没有对应的出参表");
                response.setErr_msg("config表中找不到出参");
                logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
//                throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：没有对应的出参表");
                return "-1";
            }
            short nodeType = config.getMulti_line();
            //该出参节点为时间类型（天、时分秒）的字段，用于后面转换为对应的date，存入出参表
            String dateType = config.getNode_date_type() == null ? "" : config.getNode_date_type();
            String timeType = config.getNode_time_type() == null ? "" : config.getNode_time_type();
            //该出参节点的出参数据（单行、多行批量保存）
            List<Map<String,Object>> nodeDataList = new ArrayList<>();
            if(AssertUtils.isBlank(node)){
                //出参只有output，没有节点信息
                if (out.containsKey("size")) {
                    int size = (Integer) out.get("size");
                    int recordCounts = (Integer) out.get("recordCounts");
                    if (size == 0) {
                        return "1";
                    }
                    List<Map<String , Object>> data = (List<Map<String , Object>>) out.get("data");
                    nodeDataList.addAll(data);
                }


            }else{
                //判断出参节点的类型：单行/多行
                if(nodeType == 0){
                    //单行
                    Map<String,Object> nodeJson = (Map<String,Object>) out.get(node);
                    //该出参节点，在出参json串中找不到，记录异常，返回-1
                    if (nodeJson == null){
                        LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                        response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
                        logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
//                        throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                        return "-1";
                    }
                    if(nodeJson.isEmpty()){
                        continue;
                    }
                    nodeDataList.add(nodeJson);
                }else{
                    //多行
                    nodeDataList = (List<Map<String,Object>>) out.get(node);
                    //该出参节点，在出参json串中找不到，记录异常，返回-1
                    if (nodeDataList == null){
                        LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                        response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
                        logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
//                        throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                        return "-1";
                    }
                    if(nodeDataList.isEmpty()){
                        continue;
                    }
                }
            }


            /*
             * 1.获取此节点在数据库中的出参配置字段
             * 2.获取此节点出参数据的key的顺序，批量保存foreach时，出参表字段顺序
             * 3.剔除医保出参字段（该字段在数据库中没有配置）
             */

            List<String> nodeColumnList = interfaceMapper.getNodeColumn(interfaceCode,"O",node);
            /*
            * 修正：存在出参节点为多行，但每行字段不一致的情况
            Set<String> keySet = nodeDataList.get(0).keySet();
            //剔除无配置的出参
            Iterator<String> it = keySet.iterator();
            while(it.hasNext()){
                String key = it.next();
                if(!nodeColumnList.contains(key)){
                    it.remove();
                }
            }*/

            /*
             * 转换此节点的出参数据 List<Map<String,Object>>  —— nodeDataList
             * 1.将每个Map<String,Object>的val值转换为list<Object>
             * 2.将为时间类型的出参数据根据格式转换为date
             */
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "---"+nodeDataList);
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "---"+nodeColumnList);
            List<Map<String,Object>> listData = MapUtils.tranInsertObject(nodeDataList,nodeColumnList,dateType,timeType);

            //3、判断是否需要删除出参表中该操作人员写入的老数据
            if (delOldData) {
                String operateNo = logService.queryOperateNoFromLog(serialNo , interfaceCode);
                Map<String , Object> param = Maps.newHashMap();
                param.put("operateNo" , operateNo);
                param.put("table" , table);
                interfaceMapper.delTableByOperateNo(param);
            }

            /*
            * 保存该节点的出参数据 List<List<Object>> ——listData
            * listData数据量大时分批次上传
            */
            int n = 0 ;
            if("2204".equals(interfaceCode)){//门诊费用上传一次最多保存40条
                BATCH_SIZE=40;
            }
            if(listData.size() >= BATCH_SIZE){
                List<List<Map<String,Object>>> group = ListUtils.splitList2(listData,BATCH_SIZE);
                for(List<Map<String,Object>> dataList : group){
                    int i = delOldData ? interfaceMapper.insertOutWithOutBase(table , serialNo , dataList) :
                            interfaceMapper.insertOut(table,serialNo,dataList,log);
                    n = n + i;
                }
            } else {
                int i = delOldData ? interfaceMapper.insertOutWithOutBase(table , serialNo , listData) :
                        interfaceMapper.insertOut(table,serialNo,listData,log);
                 n = n + i;
            }

            if(n != listData.size()){
                LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：保存" + node + "节点出参失败");
                response.setErr_msg("保存" + node + "节点出参失败");
                logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
//                throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：保存" + node + "节点出参失败");
                return "-1";
            }

        }

        return "1";
    }

    @Override
    public String saveOutputStr5402(String serialNo, String interfaceCode, String outpStr, boolean delOldData) throws Exception {
        AssertUtils.notBlank(outpStr, "出参");
        Map<String , Object> map = JSONObject.parseObject(outpStr , Map.class);
        if (map == null || AssertUtils.isBlank(MapUtils.getObject2String(map , "infcode"))) {
            throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：response 无公共响应信息");
        }
        CommonResponseVO  response = new CommonResponseVO();
        response.setInfCode(MapUtils.getObject2String(map , "infcode"));
        response.setInf_refmsgid(MapUtils.getObject2String(map , "inf_refmsgid"));
        response.setRefmsg_time(MapUtils.getObject2String(map , "refmsg_time"));
        response.setRespond_time(MapUtils.getObject2String(map , "respond_time"));
        response.setErr_msg(MapUtils.getObject2String(map , "err_msg"));

        //医保接口调用失败-1，保存日志，直接return -1
        if (!"0".equals(response.getInfCode())) {
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            return "-1";
        }else{
            //接口调用成功不保存出参json
            logService.updateJsonLog(serialNo,interfaceCode,response,"","");
        }

        /*医保接口调用成功时保存逻辑
         * 保存响应的外层数据：infno、inf_refmsgid、refmsg_time、respond_time、err_msg
         *  update log表（入参解析时新增的日志记录）
         */
        //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);

        //查询patientId、operateNo、visitDate、visitId
        HashMap<String,Object> log = logService.queryLogBySerialNo(serialNo, interfaceCode).get(0);

        /*保存输出的节点信息：output
         * 1.查询config出参节点配置
         * 2.遍历配置的节点信息，保存数据
         */
        //当前无出参配置，无需保存output，直接返回成功：1
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"O");
        InsurBusinessConfigVO config = configList.get(0);
        if(AssertUtils.isEmptyList(configList)) {
            return "1";
        }

        List<Map<String , Object>> out = (List<Map<String , Object>>) map.get("output");
        if (out == null || out.size() == 0) {
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：response无output信息");
            response.setErr_msg("response无output信息");
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            return "1";
        }
        String node = config.getNode();
        String table  = config.getNode_table();
        if (StringUtils.isEmpty(table)) {
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：没有对应的出参表");
            response.setErr_msg("config表中找不到出参");
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            return "-1";
        }

        short nodeType = config.getMulti_line();
        //该出参节点为时间类型（天、时分秒）的字段，用于后面转换为对应的date，存入出参表
        String dateType = config.getNode_date_type() == null ? "" : config.getNode_date_type();
        String timeType = config.getNode_time_type() == null ? "" : config.getNode_time_type();

        /*
         * 1.获取此节点在数据库中的出参配置字段
         * 2.获取此节点出参数据的key的顺序，批量保存foreach时，出参表字段顺序
         * 3.剔除医保出参字段（该字段在数据库中没有配置）
         */
        List<String> nodeColumnList = interfaceMapper.getNodeColumn(interfaceCode,"O",node);
            /*
            * 修正：存在出参节点为多行，但每行字段不一致的情况
            Set<String> keySet = nodeDataList.get(0).keySet();
            //剔除无配置的出参
            Iterator<String> it = keySet.iterator();
            while(it.hasNext()){
                String key = it.next();
                if(!nodeColumnList.contains(key)){
                    it.remove();
                }
            }*/

        /*
         * 转换此节点的出参数据 List<Map<String,Object>>  —— nodeDataList
         * 1.将每个Map<String,Object>的val值转换为list<Object>
         * 2.将为时间类型的出参数据根据格式转换为date
         */
        List<Map<String,Object>> listData = MapUtils.tranInsertObject(out,nodeColumnList,dateType,timeType);

        //3、判断是否需要删除出参表中该操作人员写入的老数据
        if (delOldData) {
            String operateNo = logService.queryOperateNoFromLog(serialNo , interfaceCode);
            Map<String , Object> param = Maps.newHashMap();
            param.put("operateNo" , operateNo);
            param.put("table" , table);
            interfaceMapper.delTableByOperateNo(param);
        }

        /*
         * 保存该节点的出参数据 List<List<Object>> ——listData
         * listData数据量大时分批次上传
         */
        int n = 0 ;
        if(listData.size() >= BATCH_SIZE){
            List<List<Map<String,Object>>> group = ListUtils.splitList2(listData,BATCH_SIZE);
            for(List<Map<String,Object>> dataList : group){
                n = n + interfaceMapper.insertOut(table,serialNo,dataList,log);
            }
        }else{
            n = n + interfaceMapper.insertOut(table,serialNo,listData,log);
        }

        if(n != listData.size()){
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：保存" + node + "节点出参失败");
            response.setErr_msg("保存" + node + "节点出参失败");
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            return "-1";
        }

        return "1";
    }

    @Override
    @Transactional
    public String saveReadCardOutputStr(String serialNo,
                                        String interfaceCode,
                                        String outpStr,
                                        boolean delOldData,
                                        String newborn) throws Exception {
        AssertUtils.notBlank(outpStr, "出参");
        //将医保出参解析为responseVO
        JSONObject jsonObject=JSONObject.parseObject(outpStr);
        Object output = jsonObject.get("output");
        System.out.println(output);
        if ("null".equals(output)){
            jsonObject.put("output",null);
        }
        CommonResponseVO response = JSONObject.parseObject(jsonObject.toJSONString(),CommonResponseVO.class);
        if (response == null || AssertUtils.isBlank(response.getInfCode())) {
            throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：response 无公共响应信息");
        }
        //医保接口调用失败-1，保存日志，直接return -1
        if (!"0".equals(response.getInfCode())) {
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            return "-1";
        }else{
            //接口调用成功不保存出参json
            logService.updateJsonLog(serialNo,interfaceCode,response,"","");
        }

        /*医保接口调用成功时保存逻辑
         * 保存响应的外层数据：infno、inf_refmsgid、refmsg_time、respond_time、err_msg
         *  update log表（入参解析时新增的日志记录）
         */
        //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);

        /*保存输出的节点信息：output
         * 1.查询config出参节点配置
         * 2.遍历配置的节点信息，保存数据
         */
        //当前无出参配置，无需保存output，直接返回成功：1
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"O");
        if(AssertUtils.isEmptyList(configList)) {
            return "1";
        }

        //有出参配置，返回却无output节点信息，保存此异常信息，返回失败：-1
        Map<String,Object> out = response.getOutput();
        if (out == null || out.isEmpty()) {
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：response无output信息");
            response.setErr_msg("response无output信息");
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            return "-1";
        }

        Map<String,Object> baseInfoNode = (Map<String,Object>) out.get("baseinfo");
        //该出参节点，在出参json串中找不到，记录异常，返回-1
        if (baseInfoNode == null || baseInfoNode.isEmpty()){
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
            response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
            logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            return "-1";
        }

        String psnNo = MapUtils.getObject2String(baseInfoNode , "psn_no");
        String psnName = MapUtils.getObject2String(baseInfoNode , "psn_name");
        HashMap<String , Object> log = new HashMap<>();

        List<Map<String , String>> oldReadCardInfo = interfaceMapper.queryReadCardBaseInfoByPsnNo(psnNo , newborn);

        if (oldReadCardInfo != null && oldReadCardInfo.size() > 0) {
            List<String> serialNos = new ArrayList<>();
                //List<String> serialNos = oldReadCardInfo.stream().map(p -> p.get("serialNo")).collect(Collectors.toList());
            for(Map<String , String> readCardInfo : oldReadCardInfo) {
                String patientId = readCardInfo.get("patientId");
                //查询pat_master_index 有相同名字才删(新生儿随母住院名字不同)
                String nameToPatMasterIndex = interfaceMapper.getNameToPatMasterIndex(patientId);
                if (psnName.equals(nameToPatMasterIndex) || nameToPatMasterIndex == null) {
                    serialNos.add(readCardInfo.get("serialNo"));
                    log.put("patientId", patientId);
                }
            }

            if (serialNos.size() > 0) {
                file2TableMapper.delAllTableData("GET_PATIENT_BASEINFO_OUT","serial_no", serialNos);
                file2TableMapper.delAllTableData("GET_PATIENT_INSUINFO_OUT","serial_no", serialNos);
                file2TableMapper.delAllTableData("GET_PATIENT_IDETINFO_OUT","serial_no", serialNos);
                file2TableMapper.delAllTableData("GET_PATIENT_CARDECINFO_OUT","serial_no", serialNos);
            }

        }

        //遍历出参节点配置信息
        for(InsurBusinessConfigVO config : configList){
            //出参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table  = config.getNode_table();
            if (StringUtils.isEmpty(table)) {
                LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：没有对应的出参表");
                response.setErr_msg("config表中找不到出参");
                logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                return "-1";
            }
            short nodeType = config.getMulti_line();
            //该出参节点为时间类型（天、时分秒）的字段，用于后面转换为对应的date，存入出参表
            String dateType = config.getNode_date_type() == null ? "" : config.getNode_date_type();
            String timeType = config.getNode_time_type() == null ? "" : config.getNode_time_type();
            //该出参节点的出参数据（单行、多行批量保存）
            List<Map<String,Object>> nodeDataList = new ArrayList<>();

            //判断出参节点的类型：单行/多行
            if(nodeType == 0){
                //单行
                Map<String,Object> nodeJson = (Map<String,Object>) out.get(node);
                //该出参节点，在出参json串中找不到，记录异常，返回-1
                if (nodeJson == null){
                    LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                    response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
                    logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                    return "-1";
                }
                if(nodeJson.isEmpty()){
                    continue;
                }
                nodeDataList.add(nodeJson);
            } else {
                //多行
                nodeDataList = (List<Map<String,Object>>) out.get(node);
                //该出参节点，在出参json串中找不到，记录异常，返回-1
                if (nodeDataList == null){
                    LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                    response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
                    logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                    return "-1";
                }
                if(nodeDataList.isEmpty()){
                    continue;
                }
            }
            /*
             * 1.获取此节点在数据库中的出参配置字段
             * 2.获取此节点出参数据的key的顺序，批量保存foreach时，出参表字段顺序
             * 3.剔除医保出参字段（该字段在数据库中没有配置）
             */
            List<String> nodeColumnList = interfaceMapper.getNodeColumn(interfaceCode,"O",node);

            /*
             * 转换此节点的出参数据 List<Map<String,Object>>  —— nodeDataList
             * 1.将每个Map<String,Object>的val值转换为list<Object>
             * 2.将为时间类型的出参数据根据格式转换为date
             */
            List<Map<String,Object>> listData = MapUtils.tranInsertObject(nodeDataList,nodeColumnList,dateType,timeType);

            /*
             * 保存该节点的出参数据 List<List<Object>> ——listData
             * listData数据量大时分批次上传
             */
            int n = 0 ;
            if(listData.size() >= BATCH_SIZE){
                List<List<Map<String,Object>>> group = ListUtils.splitList2(listData,BATCH_SIZE);
                for(List<Map<String,Object>> dataList : group){
                    n = n + interfaceMapper.insertOut(table,serialNo,dataList,log);
                }
            } else {
                n = n + interfaceMapper.insertOut(table,serialNo,listData,log);
            }

            if(n != listData.size()){
                LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：保存" + node + "节点出参失败");
                response.setErr_msg("保存" + node + "节点出参失败");
                logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                return "-1";
            }
        }

        //如果是新生儿标志打上
        if ("1".equals(newborn)){
            interfaceMapper.updateGetPatientBaseinfoOut(serialNo);
        }

        return "1";
    }

    @Override
    @Transactional
    public Map<String , Object> getInputJsonStrByDownload(String interfaceCode) throws Exception {
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"I");
        AssertUtils.notEmptyList(configList,interfaceCode +"：无此接口入参配置");

        String seq = interfaceMapper.getMsgIdSequence();
        CommonRequestVO request = new CommonRequestVO(paramConfig , seq);

        //入参数据
        HashMap<String,Object> input = new HashMap<>();
        String dateType = "" ;
        String timeType = "" ;
        String numberType = "" ;
        Map<String , Object> ret = Maps.newHashMap();
        if (configList.size() > 1) {
            throw new Exception("关系表错误");
        }
        InsurBusinessConfigVO config = configList.get(0);

        //入参节点名、节点对应的表、节点类型（单行、多行）
        String node = config.getNode();
        String table  = config.getNode_table();
        if (StringUtils.isEmpty(table)) {
            throw new Exception("没有对应的表关系");
        }

        short nodeType = config.getMulti_line();
        dateType = config.getNode_date_type();
        timeType = config.getNode_time_type();
        numberType =  config.getNode_number_type();
        Map<String , String> param = Maps.newHashMap();
        param.put("table", table);
        param.put("interfaceCode" , interfaceCode);
        List<Map<String,Object>> inputList = interfaceMapper.getInputByTableNameAndCase(param);
        AssertUtils.notEmptyList(inputList,interfaceCode +"-表中无此数据");
        //将map的key转为小写
        inputList = MapUtils.transKeyToLower(inputList,dateType,timeType,numberType);
        ret.put("jpNum", inputList.get(0).get("jp_num"));
        ret.put("tableName" , inputList.get(0).get("table_name"));
        //有可能是查询，有可能是文件
        ret.put("tableType" , inputList.get(0).get("table_type"));
        //这个表的字段数量  需要在表 INSUR_TABLE_MASTER 中加入column_count 字段
        ret.put("columnCount", inputList.get(0).get("column_count"));
        Map <String , Object> mapNode = Maps.newHashMap();
        mapNode.put("ver" , inputList.get(0).get("ver"));
        if(nodeType == 0) {
            //单行数据
            input.put(node,mapNode);
        } else {
            //多行数据
            throw new Exception("下载配置出错");
        }

        request.setInfno(interfaceCode);
        request.setInput(input);
        request.setRecer_sys_code("YBXT");
        request.setSign_no(signService.getSignNo(paramConfig.getOperateNo()));

        ret.put("jsonStr" , JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero));
        return ret;
    }

    /**
     * 无入参节点，获取公共入参json串
     * @param serialNo
     * @param interfaceCode
     * @return
     */
    @Override
    public String getCommonJsonStr(String serialNo, String interfaceCode) throws UnsupportedEncodingException {
        String seq = interfaceMapper.getMsgIdSequence();
        //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
        CommonRequestVO request = new CommonRequestVO(paramConfig,seq);
        request.setInfno(interfaceCode);
        request.setRecer_sys_code("YBXT");
        request.setSign_no(signService.getSignNo(request.getOpter()));
        //添加入参节点信息
        request.setInput(new HashMap<>());
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        Map<String, Object> logSaveNode = new HashMap<>();
        logSaveNode.put("PATIENT_ID","");
        logSaveNode.put("OPERATE_NO","");
        logService.addRequestLog(serialNo,interfaceCode,logSaveNode,json,request.getMsgid());
        return json;
    }

    @Override
    public String getInp1162(String serialNo, String interfaceCode) throws UnsupportedEncodingException {
        String seq = interfaceMapper.getMsgIdSequence();
        //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
        CommonRequestVO request = new CommonRequestVO(paramConfig,seq);
        request.setInfno(interfaceCode);
        request.setRecer_sys_code("YBXT");
        //添加入参节点信息
        Map<String,Object> data=new HashMap<>();
        Map<String,Object> map=interfaceMapper.getPatientCardecinfoIn(serialNo);
        request.setSign_no(signService.getSignNo(request.getOpter()));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue()==null){
                map.put(entry.getKey(),"");
            }
        }
        data.put("data",map);
        request.setInput(data);
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        Map<String, Object> logSaveNode = new HashMap<>();
        logSaveNode.put("PATIENT_ID","");
        logSaveNode.put("OPERATE_NO","");
        logService.addRequestLog(serialNo,interfaceCode,logSaveNode,json,request.getMsgid());
        return json;
    }



    @Transactional
    @Override
    public String getUploadInputJsonStrById(String id, String interfaceCode, boolean verify) throws UnsupportedEncodingException {
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"I");
        AssertUtils.notEmptyList(configList,"接口" + interfaceCode + "-" + id + "：无此接口入参配置");
        HashMap<String,Object> input = new HashMap<>();
        //logSaveNode用于存日志
        Map<String,Object> logSaveNode = null;
        String nodePatientid="";
        //遍历该接口的节点配置信息，获取每个节点对应入参表的数据
        for(InsurBusinessConfigVO config : configList){
            //入参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table  = config.getNode_table();
            if (AssertUtils.isBlank(table) || AssertUtils.isBlank(node)) {
                throw new CustomException("接口" + interfaceCode + "-" + id + "：无节点或对应入参表的配置信息");
            }
            short nodeType = config.getMulti_line();
            /*
             *该节点入参字段中：时间、数值类型的字段
             * 用于后面 1.解析成对应的时间格式  2.入参为空时的默认值设置
             */
            String dateType = config.getNode_date_type();
            String timeType = config.getNode_time_type();
            String numberType =  config.getNode_number_type();
            //获取该节点入参表的数据
            List<Map<String,Object>> inputList= interfaceMapper.getInputByTableNameAndId(table,id);
            nodePatientid=table;
            if (inputList.size()==0){
                continue;
            }

            //AssertUtils.notEmptyList(inputList,interfaceCode +"-" +"serialNo" +"：无此入参数据");

            //保存接口发请求日志时，只需要取公共字段，只取一次节点信息
            if(logSaveNode == null ){
                logSaveNode = inputList.get(0);
            }
            /*
             *将节点入参数据转为符合医保接口入参的格式
             * 1.map的key转为小写
             * 2.时间字符串格式转换
             * 3.为空时的默认值设置：空串默认"",数值默认0
             * 4.去掉入参表的6个公共字段："ID","SERIAL_NO","PATIENT_ID","VISIT_ID","VISIT_DATE","OPERATE_NO"
             */
//            LOGGER.info("需要转换的入参表为{}，以及需要转换的dateType为{}，timeType为{}，numberType为{}",
//                    table , dateType , timeType , numberType);
            inputList = MapUtils.transKeyToLower(inputList,dateType,timeType,numberType);
            //判断此节点的单行/多行入参
            if(nodeType == 0){
                //单行
                if("3503".equals(interfaceCode)){
                    inputList.get(0).put("prodentp_name",inputList.get(0).get("prdr_name"));
                }
                input.put(node,inputList.get(0));

            }else{
                //多行
                input.put(node,inputList);
            }
        }

        //st:查询此接口的扩展字段

        List<Map<String,String>> expPropertyList = interfaceMapper.selectExpProperty(interfaceCode, "I");
        for(Map<String,String> property :expPropertyList){
            //获取该扩展字段的标识，对应入参表，该扩展字段对应的node节点
            String proName = property.get("property");
            String table = property.get("table");
            String propertyType = property.get("propertyType");
            String node = property.get("node");
            String nodeType = property.get("nodeType");
            //获取扩展字段入参表的数据
            List<Map<String,Object>> expPropertyDataList = interfaceMapper.getInputByTableNameAndId(table,id);
            String jsonstr = "";
            if(AssertUtils.isEmptyList(expPropertyDataList)){
                //扩展字段入参表无数据
                jsonstr = "0".equals(propertyType) ? "{}" : "[]";
                if("0".equals(nodeType)){
                    //扩展字段父节点为单行输入
                    ((Map<String, Object>) input.get(node)).put(proName,jsonstr);
                }else{
                    //扩展字段父节点为多行输入
                    List<Map<String,Object>> nodeDataList = (List<Map<String, Object>>) input.get(node);
                    for(Map<String,Object> m : nodeDataList){
                        m.put(proName,jsonstr);
                    }
                }
            }else{
                //扩展字段入参表有数据：输入转换key大小写
                expPropertyDataList = MapUtils.transKeyToLower(expPropertyDataList,property.get("dateType"),
                        property.get("timeType"),property.get("numberType"));

                //判断扩展字段的父node是单行输入或多行输入
                if("0".equals(nodeType)){    //父节点单行输入
                    //去除扩展入参的node_num
                    for(Map<String,Object> data : expPropertyDataList){
                        data.remove("node_num");
                    }
                    //扩展字段json字符串:根据扩展字段的类型是单行或数组
                    jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? expPropertyDataList.get(0) : expPropertyDataList
                            , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                            ,SerializerFeature.WriteNullNumberAsZero);
                    //设置扩展字段json串
                    Map<String,Object> nodeData = (Map<String, Object>) input.get(node);
                    nodeData.put(proName,jsonstr);
                }else{   //扩展字段父节点为多行输入
                    //获取父节点的多行入参数据
                    List<Map<String,Object>> nodeDataList = (List<Map<String, Object>>) input.get(node);
                    /*
                     * 父node为多行输入时：查找与该行输入对应的扩展字段入参数据（1...1或1...*）
                     * 扩展字段入参表的NODE_NUM与node入参表的 EXP_CONTENT对应
                     */
                    for(Map<String,Object> m : nodeDataList){

                        //默认多行输入时，每行的扩展字段参数一致
                        //去除扩展入参的node_num
                        for(Map<String,Object> data : expPropertyDataList){
                            data.remove("node_num");
                        }
                        jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? expPropertyDataList.get(0) : expPropertyDataList
                                , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                                ,SerializerFeature.WriteNullNumberAsZero);
                        m.put(proName,jsonstr);
                    }
                }
            }
        }
        //ed:查询此接口的扩展字段

        //开始组织医保请求入参requestVO ，入参节点数据input
        String seq = interfaceMapper.getMsgIdSequence();
        //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
        CommonRequestVO request = new CommonRequestVO(paramConfig,seq);
        request.setInfno(interfaceCode);
        //查询经办人信息
        if (verify){
            AssertUtils.notNull(logSaveNode.get("OPERATE_NO"), "操作人不能为空");
            String operateNo = logSaveNode.get("OPERATE_NO").toString();
            String operateName = interfaceMapper.selectOperateNameByNo(operateNo);
            request.setOpter_type("1");
            request.setOpter(operateNo);
            request.setOpter_name(operateName);
        }

        request.setRecer_sys_code("YBXT");
        // todo 临时注释
        request.setSign_no(signService.getSignNo(request.getOpter()));

        //添加入参节点信息
        request.setInput(input);
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        //若人员编号交易输入中含有人员编号，insuplc_admdvs必填，可查人员信息获取交易出参表取得
        if(json.indexOf("\"psn_no\":") > 0){
            Object patientIdobj =null;
            if (logSaveNode.get("PATIENT_ID")!=null){
                patientIdobj=logSaveNode.get("PATIENT_ID");
            }else {
                //Map<String,Object> expPropertyDataList = interfaceMapper.getPatientIdforInp(nodePatientid,id);
               // patientIdobj=expPropertyDataList.get("PATIENT_ID");
            }
            String patientId="";
            if (patientIdobj!=null){
                patientId=patientIdobj.toString();
            }

            List<Map<String,String>> m = interfaceMapper.getInsuplcAdmdvs(patientId);
            String insuplc_admdvsr="";
            if (m.size()==0){
                insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
            }else {
                //System.out.println(patientId);
                //AssertUtils.notEmptyList(m,"病人无参保信息");
                if (m.get(0).get("psn_type").equals("1300")){
                    request.setRecer_sys_code("LXXT");
                }
                if (m.get(0).get("insuplc_admdvs")==null){
                    insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
                }else {
                    insuplc_admdvsr=m.get(0).get("insuplc_admdvs");
                }
            }

            request.setInsuplc_admdvs(insuplc_admdvsr);
            json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        }
        //新增医保接口请求log
        //logService.addRequestLog(serialNo,interfaceCode,logSaveNode,json,request.getMsgid());
        return json;
    }

    @Transactional
    @Override
    public String getUploadInputJsonStr(String serialNo, String interfaceCode, boolean verify) throws UnsupportedEncodingException {
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"I");
        AssertUtils.notEmptyList(configList,"接口" + interfaceCode + "-" + serialNo + "：无此接口入参配置");


        HashMap<String,Object> input = new HashMap<>();
        //logSaveNode用于存日志
        Map<String,Object> logSaveNode = null;
        String nodePatientid="";
        //遍历该接口的节点配置信息，获取每个节点对应入参表的数据
        for(InsurBusinessConfigVO config : configList){
            //入参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table  = config.getNode_table();
            if (AssertUtils.isBlank(table) || AssertUtils.isBlank(node)) {
                throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：无节点或对应入参表的配置信息");
            }
            short nodeType = config.getMulti_line();
            /*
             *该节点入参字段中：时间、数值类型的字段
             * 用于后面 1.解析成对应的时间格式  2.入参为空时的默认值设置
             */
            String dateType = config.getNode_date_type();
            String timeType = config.getNode_time_type();
            String numberType =  config.getNode_number_type();
            //获取该节点入参表的数据
            List<Map<String,Object>> inputList= interfaceMapper.getInputByTableNameAndSerialNo(table,serialNo);
            nodePatientid=table;
            if (inputList.size()==0){
                continue;
            }
            //AssertUtils.notEmptyList(inputList,interfaceCode +"-" +"serialNo" +"：无此入参数据");

            //保存接口发请求日志时，只需要取公共字段，只取一次节点信息
            if(logSaveNode == null ){
                logSaveNode = inputList.get(0);
            }
            /*
             *将节点入参数据转为符合医保接口入参的格式
             * 1.map的key转为小写
             * 2.时间字符串格式转换
             * 3.为空时的默认值设置：空串默认"",数值默认0
             * 4.去掉入参表的6个公共字段："ID","SERIAL_NO","PATIENT_ID","VISIT_ID","VISIT_DATE","OPERATE_NO"
             */
//            LOGGER.info("需要转换的入参表为{}，以及需要转换的dateType为{}，timeType为{}，numberType为{}",
//                    table , dateType , timeType , numberType);
            inputList = MapUtils.transKeyToLower(inputList,dateType,timeType,numberType);
            //判断此节点的单行/多行入参
            if(nodeType == 0){
                //单行
                if("3503".equals(interfaceCode)){
                    inputList.get(0).put("prodentpName",inputList.get(0).get("prdr_name"));
                }
                input.put(node,inputList.get(0));

            }else{
                //多行
                input.put(node,inputList);
            }
        }

        //st:查询此接口的扩展字段

        List<Map<String,String>> expPropertyList = interfaceMapper.selectExpProperty(interfaceCode, "I");
        for(Map<String,String> property :expPropertyList){
            //获取该扩展字段的标识，对应入参表，该扩展字段对应的node节点
            String proName = property.get("property");
            String table = property.get("table");
            String propertyType = property.get("propertyType");
            String node = property.get("node");
            String nodeType = property.get("nodeType");
            //获取扩展字段入参表的数据
            List<Map<String,Object>> expPropertyDataList = interfaceMapper.getInputByTableNameAndSerialNo(table,serialNo);
            String jsonstr = "";
            if(AssertUtils.isEmptyList(expPropertyDataList)){
                //扩展字段入参表无数据
                jsonstr = "0".equals(propertyType) ? "{}" : "[]";
                if("0".equals(nodeType)){
                    //扩展字段父节点为单行输入
                    ((Map<String, Object>) input.get(node)).put(proName,jsonstr);
                }else{
                    //扩展字段父节点为多行输入
                    List<Map<String,Object>> nodeDataList = (List<Map<String, Object>>) input.get(node);
                    for(Map<String,Object> m : nodeDataList){
                        m.put(proName,jsonstr);
                    }
                }
            }else{
                //扩展字段入参表有数据：输入转换key大小写
                expPropertyDataList = MapUtils.transKeyToLower(expPropertyDataList,property.get("dateType"),
                        property.get("timeType"),property.get("numberType"));

                //判断扩展字段的父node是单行输入或多行输入
                if("0".equals(nodeType)){    //父节点单行输入
                    //去除扩展入参的node_num
                    for(Map<String,Object> data : expPropertyDataList){
                        data.remove("node_num");
                    }
                    //扩展字段json字符串:根据扩展字段的类型是单行或数组
                    jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? expPropertyDataList.get(0) : expPropertyDataList
                            , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                            ,SerializerFeature.WriteNullNumberAsZero);
                    //设置扩展字段json串
                    Map<String,Object> nodeData = (Map<String, Object>) input.get(node);
                    nodeData.put(proName,jsonstr);
                }else{   //扩展字段父节点为多行输入
                    //获取父节点的多行入参数据
                    List<Map<String,Object>> nodeDataList = (List<Map<String, Object>>) input.get(node);
                    /*
                     * 父node为多行输入时：查找与该行输入对应的扩展字段入参数据（1...1或1...*）
                     * 扩展字段入参表的NODE_NUM与node入参表的 EXP_CONTENT对应
                     */
                    for(Map<String,Object> m : nodeDataList){
                        /*
                        当前处理：扩展字段父节点为多行输入，且每行的扩展字段数据都不一致，通过node_num一一对应每行各自的扩展字段数据
                        String exp_content = (String) m.get(proName);
                        //节点入参表exp_content无数据，赋值空json串
                        if(AssertUtils.isBlank(exp_content)){
                            m.put(proName,"0".equals(propertyType) ? "{}" : "[]");
                            continue;
                        }
                        //获取扩展入参数据中与node行数据对应的数据
                        List<Map<String,Object>> nodeExpDataList = expPropertyDataList.stream()
                                    .filter(exp -> exp_content.equals(exp.get("node_num")))
                                    .collect(Collectors.toList());
                        //该node入参数据行无扩展入参
                        if(AssertUtils.isEmptyList(nodeExpDataList)){
                            m.put(proName,"0".equals(propertyType) ? "{}" : "[]");
                            continue;
                        }
                        for(Map<String,Object> data : expPropertyDataList){
                            data.remove("node_num");
                        }
                        jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? nodeExpDataList.get(0) : nodeExpDataList
                                , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                                ,SerializerFeature.WriteNullNumberAsZero);
                        m.put(proName,jsonstr);
                        */

                        //默认多行输入时，每行的扩展字段参数一致
                        //去除扩展入参的node_num
                        for(Map<String,Object> data : expPropertyDataList){
                            data.remove("node_num");
                        }
                        jsonstr = JSONObject.toJSONString("0".equals(propertyType) ? expPropertyDataList.get(0) : expPropertyDataList
                                , SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty
                                ,SerializerFeature.WriteNullNumberAsZero);
                        m.put(proName,jsonstr);
                    }
                }
            }
        }
        //ed:查询此接口的扩展字段

        //开始组织医保请求入参requestVO ，入参节点数据input
        String seq = interfaceMapper.getMsgIdSequence();
        //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
        CommonRequestVO request = new CommonRequestVO(paramConfig,seq);
        request.setInfno(interfaceCode);
        //查询经办人信息
        if (verify){
            AssertUtils.notNull(logSaveNode.get("OPERATE_NO"), "操作人不能为空");
            String operateNo = logSaveNode.get("OPERATE_NO").toString();
            String operateName = interfaceMapper.selectOperateNameByNo(operateNo);
            request.setOpter_type("1");
            request.setOpter(operateNo);
            request.setOpter_name(operateName);
        }

        request.setRecer_sys_code("YBXT");
        // todo 临时注释
        request.setSign_no(signService.getSignNo(request.getOpter()));

        //添加入参节点信息
        request.setInput(input);
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        //若人员编号交易输入中含有人员编号，insuplc_admdvs必填，可查人员信息获取交易出参表取得
        if(json.indexOf("\"psn_no\":") > 0){
            Object patientIdobj =null;
            if (logSaveNode.get("PATIENT_ID")!=null){
                patientIdobj=logSaveNode.get("PATIENT_ID");
            }else {
                Map<String,Object> expPropertyDataList = interfaceMapper.getPatientIdforInp(nodePatientid,serialNo);
                patientIdobj=expPropertyDataList.get("PATIENT_ID");
            }
            String patientId="";
            if (patientIdobj!=null){
                patientId=patientIdobj.toString();
            }

            List<Map<String,String>> m = interfaceMapper.getInsuplcAdmdvs(patientId);
            String insuplc_admdvsr="";
            if (m.size()==0){
                insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
            }else {
                //System.out.println(patientId);
                //AssertUtils.notEmptyList(m,"病人无参保信息");
                if (m.get(0).get("psn_type").equals("1300")){
                    request.setRecer_sys_code("LXXT");
                }
                if (m.get(0).get("insuplc_admdvs")==null){
                    insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
                }else {
                    insuplc_admdvsr=m.get(0).get("insuplc_admdvs");
                }
            }

            request.setInsuplc_admdvs(insuplc_admdvsr);
            json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        }
        //新增医保接口请求log
        logService.addRequestLog(serialNo,interfaceCode,logSaveNode,json,request.getMsgid());
        return json;
    }

    /**
     * 2601冲正接口
     * @param serialNo
     * @param interfaceCode
     * @return
     * @throws Exception
     */
    @Override
    public String call2601(String serialNo, String interfaceCode) throws Exception {
        Map<String,Object> log = logService.queryLogBySerialNo(serialNo, interfaceCode).get(0);
        String seq = interfaceMapper.getMsgIdSequence();
        //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
        CommonRequestVO request = new CommonRequestVO(paramConfig,seq);
        request.setInfno("2601");
        request.setRecer_sys_code("YBXT");
        request.setSign_no(signService.getSignNo(request.getOpter()));
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"I");
        Object psn_no=null;
        if (configList.size()>0){
            String table  = configList.get(0).getNode_table();
            List<Map<String,Object>> inputList= interfaceMapper.getInputByTableNameAndSerialNo(table,serialNo);
            if (inputList.size()>0){
                psn_no=inputList.get(0).get("PSN_NO");
            }else {
                return "-1";
            }
        }else {
            return "-1";
        }
        if (psn_no==null){
            return "-1";
        }
        //添加入参节点信息
        Map<String,Object> input=new HashMap<>();
        Map<String,Object> data=new HashMap<>();
        data.put("psn_no",psn_no);
        data.put("omsgid",log.get("msgid"));
        data.put("oinfno",interfaceCode);
        input.put("data",data);
        request.setInput(input);
        String patientId="";
        if (log.get("patientId")!=null){
            patientId=log.get("patientId").toString();
        }
        List<Map<String,String>> m = interfaceMapper.getInsuplcAdmdvs(patientId);
        String insuplc_admdvsr="";
        if (m.size()==0){
            insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
        }else {
            if (m.get(0).get("psn_type").equals("1300")){
                request.setRecer_sys_code("LXXT");
            }
            if (m.get(0).get("insuplc_admdvs")==null){
                insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
            }else {
                insuplc_admdvsr=m.get(0).get("insuplc_admdvs");
            }
        }
        String operateNo="";
        if (log.get("operateNo")!=null){
            operateNo=log.get("operateNo").toString();
        }else{
            operateNo=request.getOpter();
        }
        // todo 临时注释
        request.setSign_no(signService.getSignNo(operateNo));
        request.setOpter(operateNo);
        request.setInsuplc_admdvs(insuplc_admdvsr);
        String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
        Map<String, Object> logSaveNode = new HashMap<>();
        logSaveNode.put("PATIENT_ID",log.get("patientId"));
        logSaveNode.put("OPERATE_NO",log.get("operateNo"));
        logService.addRequestLog(serialNo,"2601",logSaveNode,json,request.getMsgid());
        byte[] outpchar = new byte[1024*1024];
        LOGGER.info(interfaceCode +"---"+serialNo +"*************************医保调用开始*************************" );
        LOGGER.info(interfaceCode +"---"+serialNo+"**************入参内容:*********:"+json);
        int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(json.getBytes("GBK"), outpchar);
        String outputStr = new String(outpchar,"GBK").trim();
        LOGGER.info(interfaceCode +"---"+serialNo+"**************出参内容:*********:"+outputStr.trim());
        LOGGER.info(interfaceCode +"---"+serialNo + "*************************医保调用结束*************************");
        if(res < 0){
            LOGGER.error(interfaceCode +"---"+serialNo +"****************callInsuranceService医保接口调用失败****************");
            return "-1";
        }
        JSONObject jsonObject=JSONObject.parseObject(outputStr);
        Object output = jsonObject.get("output");
        System.out.println(output);
        if ("null".equals(output)){
            jsonObject.put("output",null);
        }
        //将医保出参解析为responseVO
        CommonResponseVO response = JSONObject.parseObject(jsonObject.toJSONString(),CommonResponseVO.class);
        if (response == null || AssertUtils.isBlank(response.getInfCode())) {
            return "-1";
        }
        //医保接口调用失败-1，保存日志，直接return -1
        if (!"0".equals(response.getInfCode())) {
            logService.updateResponseLog(serialNo,"2601",response,outputStr);
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            //throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            return "-1";
        }else{
            //接口调用成功不保存出参json
            logService.updateJsonLog(serialNo,"2601",response,"","");
        }
        return "1";
    }

    @Override
    public int updateOutpSetterOutFlag(String serialNo) {
        return interfaceMapper.updateOutpSetterOutFlag(serialNo);
    }

    @Override
    public int updateOutpSetterInFlag(String serialNo) {
        return interfaceMapper.updateOutpSetterInFlag(serialNo);
    }

    @Override
    public int updateInpSetterOutFlag(String serialNo) {
        return interfaceMapper.updateInpSetterOutFlag(serialNo);
    }

    @Override
    public int updateInpSetterInFlag(String serialNo) {
        return interfaceMapper.updateInpSetterInFlag(serialNo);
    }
    public static void main(String[] args) {
        List<String> list = Lists.newArrayList();
        System.out.println("开始测试");
        for (String s : list) {
            System.out.println(s);
        }
        System.out.println("结束");
    }

}
