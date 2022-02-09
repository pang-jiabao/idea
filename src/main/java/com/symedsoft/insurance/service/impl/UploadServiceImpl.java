package com.symedsoft.insurance.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.dto.RequestDto;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.mapper.LogMapper;
import com.symedsoft.insurance.mapper.UploadMapper;
import com.symedsoft.insurance.service.InsuranceConfigService;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.service.UploadService;
import com.symedsoft.insurance.utils.InsurBusinessHandle;
import com.symedsoft.insurance.utils.MapUtils;
import com.symedsoft.insurance.utils.ReadDll;
import com.symedsoft.insurance.vo.CommonRequestVO;
import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.InsurBusinessConfigVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author yx
 * @version 1.0.0
 * @Description 上传相关service
 * @createTime 2021年10月11日
 */
@Service
public class UploadServiceImpl implements UploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadServiceImpl.class);
    @Autowired
    private UploadMapper uploadMapper;

    @Autowired
    private RequestParamConfig requestParamConfig;

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private InsuranceConfigService insuranceConfigService;
    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;
    @Autowired
    private SignService signService;
    @Autowired
    private RequestParamConfig paramConfig;
    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    TransactionDefinition transactionDefinition;
    /** 初始上传时间 */
    @Value("${upload.startdate}")
    private String startdate;
    @Value("${upload.enddate}")
    private String enddate;
    @Override
    public Object callProcedure(String procedureName) throws ParseException {
        int ret;
        Map<String,Object> map=new HashMap<>();
        map.put("procedureName",procedureName);
        map.put("hospCode",requestParamConfig.getFixmedins_code());
        map.put("operateNo",requestParamConfig.getOperateNo());
        map.put("ret",-1);
        Map<String, Object> procedureRECORD = uploadMapper.getProcedureRecord(procedureName);
        if (procedureRECORD==null){
            map.put("stDate",startdate);
            map.put("edDate",enddate);
        }else {
            map.put("stDate",procedureRECORD.get("START_DATE").toString());
            map.put("edDate",procedureRECORD.get("END_DATE").toString());
        }
        uploadMapper.callProcedure(map);
        if (map.get("ret")==null){
            map.put("ret","1");
        }
        if (Integer.parseInt(map.get("ret").toString())==1){
            Map<String,Object> procdeure=new HashMap<>();
            Date currentdate=new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentdate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            currentdate = calendar.getTime();
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dayTime=sdf.format(currentdate);
            if (procedureRECORD==null){
                procdeure.put("procedureName",procedureName);
                procdeure.put("stDate",enddate);
                procdeure.put("edDate",dayTime);
                int i = uploadMapper.insertProcedureRecord(map);
            }else {
                procdeure.put("procedureName",procedureName);
                procdeure.put("stDate",procedureRECORD.get("END_DATE").toString());
                //把调用记录设值回表里面(下一次调用开始时间取上一次的结束时间)
                procdeure.put("edDate",dayTime);
                int i = uploadMapper.updateProcedureRecord(map);
            }
        }else {
            Map<String,Object> procdeure=new HashMap<>();
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (procedureRECORD==null){
                procdeure.put("procedureName",procedureName);
                procdeure.put("stDate",startdate);
                procdeure.put("edDate",enddate);
                int i = uploadMapper.insertProcedureRecord(map);
            }else {
                procdeure.put("procedureName",procedureName);
                procdeure.put("stDate",procedureRECORD.get("START_DATE").toString());
                //把调用记录设值回表里面(下一次调用开始时间取上一次的结束时间)
                procdeure.put("edDate",procedureRECORD.get("END_DATE").toString());
                int i = uploadMapper.updateProcedureRecord(map);
            }
        }
        return map.get("ret");
    }

    @Override
    public String callInterfaceBySerialNo(String interfaceCode) {
        //事务手动提交
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        TransactionStatus transactionele = dataSourceTransactionManager.getTransaction(transactionDefinition);
        String ret="1";
        //获取接口对应的表名
        String configTableName = uploadMapper.getConfig(interfaceCode);
        //查询接口未上传的serial_no
        List<String> serialNos=uploadMapper.getInterfaceSerialNo(configTableName);
        if (serialNos.size()==0){
            return "入参表无需要上传记录";
        }
        for (int i=0;i<serialNos.size();i++){
            String inputJsonStr = null;
            try {
                System.out.println("serialNos.get(i):"+serialNos.get(i));
                    //获取入参
                    inputJsonStr = insuranceConfigService.getUploadInputJsonStr(serialNos.get(i), interfaceCode, false);
                    //将上传状态改为已取参
                    uploadMapper.updateInterfaceTableBySerialNo(configTableName,serialNos.get(i),"2");
                    dataSourceTransactionManager.commit(transactionStatus);
                    System.out.println(inputJsonStr);
                    RequestDto requestDto=new RequestDto();
                    requestDto.setRequestBody(inputJsonStr);
                    //调用医保接口
//                String outJson = InsurBusinessHandle.businessHandle(requestDto);
                    byte[] outpchar = new byte[1024*1024];
                    String outpStr = "";
                    LOGGER.info(interfaceCode+"接口入参:"+inputJsonStr);
                    int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(inputJsonStr.getBytes("GBK"), outpchar);
                    outpStr=new String(outpchar,"GBK");
                    LOGGER.info(interfaceCode+"接口出参:"+outpStr.trim());
                    if(res < 0){
                        LOGGER.error("****************callInsuranceService医保接口调用失败****************");
                    }
                if ("-1".equals(outpStr)){
                    ret="-1";
                    break;
                }
                //保存出参
//                ret = insuranceConfigService.saveOutputStr(serialNos.get(i), interfaceCode, outpStr, false);
                if ("1".equals(ret)){
                    //修改更新状态
                    uploadMapper.updateInterfaceTableBySerialNo(configTableName,serialNos.get(i),"1");
                }else {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                dataSourceTransactionManager.rollback(transactionele);
                //todo
                break;
            }
            //todo
            break;
        }
        dataSourceTransactionManager.commit(transactionele);
        return ret;

    }
    @Override
    public String callInterfaceById(String interfaceCode) {
        //事务手动提交
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        TransactionStatus transactionele = dataSourceTransactionManager.getTransaction(transactionDefinition);
        String ret="1";
        //获取接口对应的表名
        String configTableName = uploadMapper.getConfig(interfaceCode);
        //查询接口未上传的serial_no
        List<String> serialNos=uploadMapper.getInterfaceId(configTableName);
        if (serialNos.size()==0){
            return "入参表无需要上传记录";
        }
        for (int i=0;i<serialNos.size();i++){
            String inputJsonStr = null;
            try {
                System.out.println("serialNos.get(i):"+serialNos.get(i));
                //获取入参
                inputJsonStr = insuranceConfigService.getUploadInputJsonStrById(serialNos.get(i), interfaceCode, false);
                uploadMapper.updateInterfaceTableById(configTableName,serialNos.get(i),"2");
                dataSourceTransactionManager.commit(transactionStatus);
                System.out.println(inputJsonStr);
                RequestDto requestDto=new RequestDto();
                requestDto.setRequestBody(inputJsonStr);
                //调用医保接口
//                String outJson = InsurBusinessHandle.businessHandle(requestDto);
                byte[] outpchar = new byte[1024*1024];
                String outpStr = "";
                LOGGER.info(interfaceCode+"接口入参:"+inputJsonStr);
                int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(inputJsonStr.getBytes("GBK"), outpchar);
                outpStr=new String(outpchar,"GBK").trim();
                LOGGER.info(interfaceCode+"接口出参:"+outpStr);
                if(res < 0){
                    LOGGER.error("****************callInsuranceService医保接口调用失败****************");
                }
                if ("-1".equals(outpStr)){
                    ret="-1";
                    break;
                }
                //保存出参
//                ret = insuranceConfigService.saveOutputStr(serialNos.get(i), interfaceCode, outpStr, false);
                if ("1".equals(ret)){
                    //修改更新状态
                    uploadMapper.updateInterfaceTableById(configTableName,serialNos.get(i),"1");
                }else {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                dataSourceTransactionManager.rollback(transactionele);
                //todo
                break;
            }
            //todo
            break;
        }
        dataSourceTransactionManager.commit(transactionele);
        return ret;

    }

    @Override
    public String callInterfaceById(String interfaceCode,String id) {
        //事务手动提交
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        TransactionStatus transactionele = dataSourceTransactionManager.getTransaction(transactionDefinition);
        String ret="1";
        //获取接口对应的表名
        String configTableName = uploadMapper.getConfig(interfaceCode);
        //查询接口未上传的serial_no
        List<String> serialNos=uploadMapper.getInterfaceId(configTableName);
        if (serialNos.size()==0){
            return "入参表无需要上传记录";
        }
//        for (int i=0;i<serialNos.size();i++){
            String inputJsonStr = null;
            try {
                System.out.println("serialNos.get(i):"+id);
                //获取入参
                inputJsonStr = insuranceConfigService.getUploadInputJsonStrById(id, interfaceCode, false);
                uploadMapper.updateInterfaceTableById(configTableName,id,"2");
                dataSourceTransactionManager.commit(transactionStatus);
                System.out.println(inputJsonStr);
                RequestDto requestDto=new RequestDto();
                requestDto.setRequestBody(inputJsonStr);
                //调用医保接口
//                String outJson = InsurBusinessHandle.businessHandle(requestDto);
                byte[] outpchar = new byte[1024*1024];
                String outpStr = "";
                LOGGER.info(interfaceCode+"接口入参:"+inputJsonStr);
                int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(inputJsonStr.getBytes("GBK"), outpchar);
                outpStr=new String(outpchar,"GBK").trim();
                LOGGER.info(interfaceCode+"接口出参:"+outpStr);
                if(res < 0){
                    LOGGER.error("****************callInsuranceService医保接口调用失败****************");
                }
                if ("-1".equals(outpStr)){
                    ret="-1";
//                    break;
                }
                //保存出参
//                ret = insuranceConfigService.saveOutputStr(serialNos.get(i), interfaceCode, outpStr, false);
                if ("1".equals(ret)){
                    //修改更新状态
                    uploadMapper.updateInterfaceTableById(configTableName,id,"1");
                }else {
//                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                dataSourceTransactionManager.rollback(transactionele);
                //todo
//                break;
            }
            //todo
//            break;
//        }
        dataSourceTransactionManager.commit(transactionele);
        return ret;

    }
    @Override
    public void autoUpload4101() {
        List<Map<String, Object>> maps = uploadMapper.selectInpSetlePatient();
        for (Map<String,Object> map:maps){
            upload4101ByPatient(map.get("PATIENT_ID").toString(),Integer.parseInt(map.get("VISIT_ID").toString()));
        }
    }

    @Override
    public void autoUpload4401() {
        int ret;
        Map<String,Object> map=new HashMap<>();
        map.put("hospCode","H50015501017");
        map.put("operateNo","123");
        map.put("stDate","2021-08-01 00:00:00");
        map.put("edDate","2021-10-10 00:00:00");
        map.put("ret",-1);
        uploadMapper.insert4401procedure(map);
        System.out.println(map.get("ret"));
        System.out.println("aaa");
    }

    @Override
    public String upload4101ByPatient(String patientId, int visitId) {
        String serialno=uploadMapper.getSerialNo();
        Map<String, Object> sinfo = uploadMapper.selectSettlInfo(patientId, visitId);
        List<Map<String, Object>> insurPatienInfo = uploadMapper.selectInsurPatienInfo(patientId);
        Map<String, Object> inpAdmintInfo = uploadMapper.selectInpAdmintInfo(patientId, visitId);
        List<Map<String, Object>> inpLeaveDiseinfo = uploadMapper.selectInpLeaveDiseinfo(patientId, visitId);

        Map<String,Object> setlinfo=new HashMap<>();
        setlinfo.put("SERIAL_NO",serialno);
        setlinfo.put("PATIENT_ID",patientId);
        setlinfo.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        setlinfo.put("OPERATE_NO",requestParamConfig.getOperateNo());
        setlinfo.put("MDTRT_ID",sinfo.get("MDTRT_ID"));
        setlinfo.put("SETL_ID",sinfo.get("SETL_ID"));
        setlinfo.put("FIXMEDINS_NAME",requestParamConfig.getFixmedins_name());
        setlinfo.put("FIXMEDINS_CODE",requestParamConfig.getFixmedins_code());
        setlinfo.put("MEDCASNO",sinfo.get("INP_NO"));
        setlinfo.put("PSN_NAME",sinfo.get("PSN_NAME"));
        setlinfo.put("GEND",sinfo.get("GEND"));
        setlinfo.put("BRDY",sinfo.get("BRDY"));
        setlinfo.put("AGE",sinfo.get("AGE"));
        //先默认中国
        setlinfo.put("NTLY","CHN");
        if (sinfo.get("AGE_DAY")!=null){
            if (Integer.valueOf(sinfo.get("AGE_DAY").toString())<365){
                setlinfo.put("NWB_AGE",sinfo.get("AGE_DAY"));
            }
        }

        setlinfo.put("NATY",sinfo.get("NATY"));
        setlinfo.put("PATN_CERT_TYPE",sinfo.get("PSN_CERT_TYPE"));
        setlinfo.put("CERTNO",sinfo.get("CERTNO"));
        switch (sinfo.get("PSN_TYPE").toString()){
            case "1101" : setlinfo.put("PRFS","17");  break;
            case "1102" : setlinfo.put("PRFS","11");  break;
            case "1300" : setlinfo.put("PRFS","80");  break;
            case "1403" : setlinfo.put("PRFS","31");  break;
            case "1404" : setlinfo.put("PRFS","31");  break;
            default :setlinfo.put("PRFS","90");
        }
        setlinfo.put("CONER_NAME",sinfo.get("PSN_NAME"));
        setlinfo.put("PATN_RLTS","1");
        setlinfo.put("CONER_ADDR",sinfo.get("MAILING_ADDRESS"));
        setlinfo.put("CONER_TEL",sinfo.get("PHONE_NUMBER_HOME"));
        setlinfo.put("HI_TYPE",sinfo.get("INSUTYPE"));

        setlinfo.put("INSUPLC",insurPatienInfo.get(0).get("INSUPLC_ADMDVS"));
//        setlinfo.put("IPT_MED_TYPE",sinfo.get("MED_TYPE"));
        setlinfo.put("IPT_MED_TYPE","1");
        setlinfo.put("ADM_CATY",inpAdmintInfo.get("ADM_DEPT_CODG"));
        setlinfo.put("DSCG_CATY",inpAdmintInfo.get("ADM_DEPT_CODG"));
        setlinfo.put("MAINDIAG_FLAG","1");

        //票据相关
        setlinfo.put("BILL_CODE","1");
        setlinfo.put("BILL_NO","1");
        setlinfo.put("BIZ_SN","1");

        setlinfo.put("SETL_BEGN_DATE",sinfo.get("SETL_TIME"));
        setlinfo.put("SETL_END_DATE",sinfo.get("SETL_TIME"));
        setlinfo.put("PSN_SELFPAY",sinfo.get("PSN_PART_AMT"));
        setlinfo.put("PSN_OWNPAY",sinfo.get("PSN_CASH_PAY"));
        setlinfo.put("ACCT_PAY",sinfo.get("ACCT_PAY"));
        setlinfo.put("PSN_CASHPAY",sinfo.get("PSN_CASH_PAY"));
        setlinfo.put("HI_PAYMTD","1");

        setlinfo.put("HSORG","");
        setlinfo.put("hsorg_opter","");
        setlinfo.put("medins_fill_dept","");
        setlinfo.put("medins_fill_psn"," ");

        uploadMapper.insertSetlInfo(setlinfo);

        //医保结算信息
        Map<String,Object> payinfo=new HashMap<>();
        payinfo.put("SERIAL_NO",serialno);
        payinfo.put("PATIENT_ID",patientId);
        payinfo.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        payinfo.put("OPERATE_NO",requestParamConfig.getOperateNo());
        //HIFP_PAY,CVLSERV_PAY,HIFES_PAY+HIFMI_PAY+HIFOB_PAY,MAF_PAY,ACCT_PAY
        if(Double.parseDouble(sinfo.get("HIFP_PAY").toString())>0){
            if ("390".equals(sinfo.get("INSUTYPE"))){
                payinfo.put("FUND_PAY_TYPE","390101");
                payinfo.put("FUND_PAYAMT",sinfo.get("HIFP_PAY"));
            }else {
                payinfo.put("FUND_PAY_TYPE","310101");
                payinfo.put("FUND_PAYAMT",sinfo.get("HIFP_PAY"));
            }
            uploadMapper.insertPayInfo(payinfo);
        }
        if(Double.parseDouble(sinfo.get("CVLSERV_PAY").toString())>0){
            payinfo.put("FUND_PAY_TYPE","320101");
            payinfo.put("FUND_PAYAMT",sinfo.get("CVLSERV_PAY"));
            uploadMapper.insertPayInfo(payinfo);
        }
        if(Double.parseDouble(sinfo.get("DLLP").toString())>0){
            payinfo.put("FUND_PAY_TYPE","390201");
            payinfo.put("FUND_PAYAMT",sinfo.get("DLLP"));
            uploadMapper.insertPayInfo(payinfo);
        }
        if(Double.parseDouble(sinfo.get("MAF_PAY").toString())>0){
            payinfo.put("FUND_PAY_TYPE","610100");
            payinfo.put("FUND_PAYAMT",sinfo.get("MAF_PAY"));
            uploadMapper.insertPayInfo(payinfo);
        }
        if(Double.parseDouble(sinfo.get("ACCT_PAY").toString())>0){
            payinfo.put("FUND_PAY_TYPE","310201");
            payinfo.put("FUND_PAYAMT",sinfo.get("ACCT_PAY"));
            uploadMapper.insertPayInfo(payinfo);
        }
        //住院诊断信息 diag_type,maindiag_flag,diag_code,diag_name
        Map<String,Object> diseinfo=new HashMap<>();
        diseinfo.put("SERIAL_NO",serialno);
        diseinfo.put("PATIENT_ID",patientId);
        diseinfo.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        diseinfo.put("OPERATE_NO",requestParamConfig.getOperateNo());
        for (Map<String,Object> leaveDiseinfo:inpLeaveDiseinfo) {
            diseinfo.put("DIAG_TYPE",leaveDiseinfo.get("DIAG_TYPE"));
            diseinfo.put("DIAG_CODE",leaveDiseinfo.get("DIAG_CODE"));
            diseinfo.put("DIAG_NAME",leaveDiseinfo.get("DIAG_NAME"));
            diseinfo.put("MAINDIAG_FLAG",leaveDiseinfo.get("MAINDIAG_FLAG"));
            uploadMapper.insertDiseInfo(diseinfo);
        }
        //费用信息
        Map<String,Object> iteminfo=new HashMap<>();
        iteminfo.put("SERIAL_NO",serialno);
        iteminfo.put("PATIENT_ID",patientId);
        iteminfo.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        iteminfo.put("OPERATE_NO",requestParamConfig.getOperateNo());
        List<Map<String, Object>> detailInfo = uploadMapper.selectDetailInfo(patientId, visitId);
        for (Map<String,Object> detail:detailInfo){
            iteminfo.put("MED_CHRGITM",detail.get("MED_CHRGITM_TYPE"));
            iteminfo.put("AMT",detail.get("COSTS"));
            iteminfo.put("CLAA_SUMFEE",detail.get("JAY"));
            iteminfo.put("CLAB_AMT",detail.get("YI"));
            iteminfo.put("FULAMT_OWNPAY_AMT",detail.get("ZI"));
            iteminfo.put("OTH_AMT",detail.get("OTH"));
            uploadMapper.insertItemInfo(iteminfo);
        }
        //门诊慢特病信息
        Map<String,Object> opspdise=new HashMap<>();
        opspdise.put("SERIAL_NO",serialno);
        opspdise.put("PATIENT_ID",patientId);
        opspdise.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        opspdise.put("OPERATE_NO",requestParamConfig.getOperateNo());
        opspdise.put("DIAG_NAME","*");
        opspdise.put("DIAG_CODE","*");
        opspdise.put("OPRN_OPRT_NAME","*");
        opspdise.put("OPRN_OPRT_CODE","*");
        uploadMapper.insertOpspDise(opspdise);

        //手术信息
        Map<String,Object> oprninfo=new HashMap<>();
        oprninfo.put("SERIAL_NO",serialno);
        oprninfo.put("PATIENT_ID",patientId);
        oprninfo.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        oprninfo.put("OPERATE_NO",requestParamConfig.getOperateNo());
        List<Map<String, Object>> operationInfo = uploadMapper.selectOperationInfo(patientId, visitId);
        if (operationInfo.size()<=0){
            oprninfo.put("OPRN_OPRT_TYPE","1");
            oprninfo.put("OPRN_OPRT_NAME","*");
            oprninfo.put("OPRN_OPRT_CODE","*");
            //
            oprninfo.put("OPRN_OPRT_DATE",sinfo.get("SETL_TIME"));
            oprninfo.put("OPER_DR_NAME","*");
            oprninfo.put("OPER_DR_CODE","*");
            uploadMapper.insertOprnInfo(oprninfo);
        }
        for (Map<String,Object> operation:operationInfo){
            oprninfo.put("OPRN_OPRT_TYPE",operation.get("OPRN_OPRT_TYPE"));
            oprninfo.put("OPRN_OPRT_NAME",operation.get("OPRN_OPRT_NAME"));
            oprninfo.put("OPRN_OPRT_CODE",operation.get("OPRN_OPRT_CODE"));
            oprninfo.put("OPRN_OPRT_DATE",operation.get("OPRN_OPRT_DATE"));
            oprninfo.put("OPER_DR_NAME",operation.get("OPER_DR_NAME"));
            oprninfo.put("OPER_DR_CODE",operation.get("OPER_DR_NAME"));
            uploadMapper.insertOprnInfo(oprninfo);
        }
        Map<String,Object> icuinfo=new HashMap<>();
        icuinfo.put("SERIAL_NO",serialno);
        icuinfo.put("PATIENT_ID",patientId);
        icuinfo.put("VISIT_ID",visitId);
        //setlinfo.put("VISIT_DATE",null);
        icuinfo.put("OPERATE_NO",requestParamConfig.getOperateNo());
        icuinfo.put("SCS_CUTD_WARD_TYPE","*");
        icuinfo.put("SCS_CUTD_INPOOL_TIME",sinfo.get("SETL_TIME"));
        icuinfo.put("SCS_CUTD_EXIT_TIME",sinfo.get("SETL_TIME"));
        icuinfo.put("SCS_CUTD_SUM_DURA","0/0/0");
        uploadMapper.insertIcuInfo(icuinfo);

        uploadMapper.updateInpSetFlag(patientId,visitId);
        return null;
    }

    @Override
    public String detailAuditBefore(String serialNo,String trig_scen) {
        //取出明细审核事前分析主表的入参，根据UPLOAD_FLAG来查
        List<Map<String, Object>> detailAuditBfPatientIn = uploadMapper.getDetailAuditBfPatientIn(serialNo);
        if (detailAuditBfPatientIn.size()<=0){
            return "serialNo:"+serialNo+"无数据";
        }
        InsurBusinessConfigVO patient = interfaceMapper.getInterfaceConfigByComfigItem("3101", "I", "patient_dtos");

        Map<String, Object> dataIns = detailAuditBfPatientIn.get(0);
        /*List<Map<String, Object>> master = uploadMapper.get3101Master();*/
        //根据主表的serial_no取出所有节点的数
        Object serial_no = dataIns.get("SERIAL_NO");
        Object patient_id = dataIns.get("PATIENT_ID");
        Object curr_mdtrt_id = dataIns.get("CURR_MDTRT_ID");
        Object visit_id = dataIns.get("visit_id");
        try {
            if(curr_mdtrt_id==null || "".equals(curr_mdtrt_id)){
                LOGGER.info("没有curr_mdtrt_id,{}",dataIns);
                return "-1";
            }
            if(serial_no==null || "".equals(serial_no)){
                LOGGER.info("没有SERIALNO,{}",serial_no);
                return "-1";
            }
            detailAuditBfPatientIn=MapUtils.transKeyToLower(detailAuditBfPatientIn,patient.getNode_date_type(),
                    patient.getNode_time_type(),patient.getNode_number_type());
            dataIns=detailAuditBfPatientIn.get(0);
            List<Map<String, Object>> detailAuditBfEncounterIn = uploadMapper.getDetailAuditBfEncounterIn(serial_no.toString());
            InsurBusinessConfigVO encounter = interfaceMapper.getInterfaceConfigByComfigItem("3101", "I", "fsi_encounter_dtos");
            detailAuditBfEncounterIn=MapUtils.transKeyToLower(detailAuditBfEncounterIn,encounter.getNode_date_type(),
                    encounter.getNode_time_type(),encounter.getNode_number_type());
//            if(detailAuditBfEncounterIn.get(0).get("adm_date")==null || "".equals(detailAuditBfEncounterIn.get(0).get("adm_date"))){
//                detailAuditBfEncounterIn.get(0).put("adm_date","1900-01-01 00:00:00");
//            }
//            if(detailAuditBfEncounterIn.get(0).get("dscg_date")==null || "".equals(detailAuditBfEncounterIn.get(0).get("dscg_date"))){
//                detailAuditBfEncounterIn.get(0).put("dscg_date","1900-01-01 00:00:00");
//            }
            List<Map<String, Object>> detailAuditBfDiagnoseIn = uploadMapper.getDetailAuditBfDiagnoseIn(serial_no.toString());
            InsurBusinessConfigVO diagnose = interfaceMapper.getInterfaceConfigByComfigItem("3101", "I", "fsi_diagnose_dtos");
            detailAuditBfDiagnoseIn=MapUtils.transKeyToLower(detailAuditBfDiagnoseIn,diagnose.getNode_date_type(),
                    diagnose.getNode_time_type(),diagnose.getNode_number_type());
            List<Map<String, Object>> detailAuditBfOperationIn = uploadMapper.getDetailAuditBfOperationIn(serial_no.toString());
            InsurBusinessConfigVO operation = interfaceMapper.getInterfaceConfigByComfigItem("3101", "I", "fsi_operation_dtos");
            detailAuditBfOperationIn=MapUtils.transKeyToLower(detailAuditBfOperationIn,operation.getNode_date_type(),
                    operation.getNode_time_type(),operation.getNode_number_type());
            List<Map<String, Object>> detailAuditBfOrderIn = uploadMapper.getDetailAuditBfOrderIn(serial_no.toString());
            InsurBusinessConfigVO order = interfaceMapper.getInterfaceConfigByComfigItem("3101", "I", "fsi_order_dtos");
            detailAuditBfOrderIn=MapUtils.transKeyToLower(detailAuditBfOrderIn,order.getNode_date_type(),
                order.getNode_time_type(),order.getNode_number_type());

            if(detailAuditBfEncounterIn==null){
                throw new Exception("没有找到该serialNo的就诊信息,此条记录无法上传。");
            }
            if(detailAuditBfDiagnoseIn==null || detailAuditBfDiagnoseIn.size() <= 0){
                throw new Exception("没有找到该serialNo的诊断信息,此条记录无法上传。");
            }
            if(detailAuditBfOrderIn==null || detailAuditBfOrderIn.size() <= 0){
                throw new Exception("没有找到该serialNo的医嘱处方信息,此条记录无法上传。");
            }
            if(detailAuditBfOperationIn!=null){
                detailAuditBfEncounterIn.get(0).put("fsi_operation_dtos",detailAuditBfOperationIn);
            }
            detailAuditBfEncounterIn.get(0).put("fsi_diagnose_dtos",detailAuditBfDiagnoseIn);
            detailAuditBfEncounterIn.get(0).put("fsi_order_dtos",detailAuditBfOrderIn);
            dataIns.put("fsi_encounter_dtos",detailAuditBfEncounterIn);
            List<Map<String,Object>> fsi_his_data_dto=new ArrayList<>();
            dataIns.put("fsi_his_data_dto",fsi_his_data_dto);
            String recer_sys_code="YBXT";
            String seq = interfaceMapper.getMsgIdSequence();
            CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
            request.setInfno("3101");
            String operateNo = "9999";
            String operateName = "自动上传";
            request.setOpter_type("1");
            request.setOpter(operateNo);
            request.setOpter_name(operateName);
            request.setSign_no(signService.getSignNo(operateNo));
            request.setRecer_sys_code(recer_sys_code);
            HashMap<String,Object> inputMaster = new HashMap<>();
            HashMap<String,Object> input = new HashMap<>();
            inputMaster.put("patient_dtos",dataIns);
            inputMaster.put("syscode","SYMEDSOFT");
            inputMaster.put("task_id",request.getMsgid());
            inputMaster.put("trig_scen",trig_scen);
            input.put("data",inputMaster);
            request.setInput(input);
            String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
            byte[] outchar = new byte[1024*1024] ;
            LOGGER.info("3101接口入参:"+json);
            int result = ReadDll.INSTANCE.BUSINESS_HANDLE(json.getBytes("GBK") , outchar);
            String outStr = new String(outchar,"gbk").trim();
            LOGGER.info("3101接口出参:"+outStr);
            if (result != 0){
                throw new Exception("调用动态库返回值不为0");
                //修改状态
                /*uploadMapper.update3101Status(patient_id.toString(),"2",serial_no.toString());
                continue;
    */              }
            // 2、解析返回报文
            CommonResponseVO response = JSONObject.parseObject(outStr , CommonResponseVO.class);
            if ("-1".equals(response.getInfCode())) {
                //修改状态
                throw new Exception("调用接口解析InfCode等于-1");
                /*uploadMapper.update3101Status(patient_id.toString(),"2",serial_no.toString());
                continue;*/
            }
            if (StringUtils.isNotEmpty(response.getErr_msg())) {
                //修改状态
                throw new Exception(response.getErr_msg());
                /*uploadMapper.update3101Status(patient_id.toString(),"2",serial_no.toString());
                continue;*/
            }
            Map <String , Object> ret = response.getOutput();
            if (ret != null && ret.size() > 0) {
                JSONObject jsonObject = JSONObject.parseObject(json);
                JSONObject output=jsonObject.getJSONObject("output");
                JSONArray jsonresult=output.getJSONArray("result");
                for(int i=0;i<jsonresult.size();i++){
                    String jsonret=jsonresult.get(i).toString();
                    Map mapObj = JSONObject.parseObject(jsonret,Map.class);
                    mapObj.put("serial_no",serialNo);
                    mapObj.put("patient_id",patient_id);
                    mapObj.put("operate_no","9999");
                    insertDetailAuditBfResultOut(mapObj);
                    JSONArray judge_result_detail_dtos=jsonresult.getJSONObject(i).getJSONArray("judge_result_detail_dtos");
                    for (int j=0;j<judge_result_detail_dtos.size();j++){
                        String detail=judge_result_detail_dtos.get(j).toString();
                        Map detailMap=JSONObject.parseObject(detail,Map.class);
                        detailMap.put("serial_no",serialNo);
                        detailMap.put("patient_id",patient_id);
                        detailMap.put("operate_no","111");
                        insertDetailAuditBfDetailOut(detailMap);
                    }
                }
            }
            //修改状态
            uploadMapper.update3101Status(patient_id.toString(),"1",serial_no.toString());
            LOGGER.info("serialNo 为 {} 的信息采集上传成功！" , serial_no);
        } catch (Exception e) {
            //修改状态
            int i = uploadMapper.update3101Status(patient_id.toString(),"2",serial_no.toString());
            logMapper.insertUploadLog(serial_no.toString(),"3101",patient_id.toString(),e.getMessage());
            LOGGER.error("定时任务信息采集上传调用接口：{} 中的serialNo: {}发生异常，保存信息为：{}"
                    ,"3101", serial_no, e.getMessage());
        }
        return "1";
    }



    @Override
    public String detailAuditAfter(String serialNo,String trig_scen) {
        //取出明细审核事前分析主表的入参，根据UPLOAD_FLAG来查
        List<Map<String, Object>> detailAuditBfPatientIns = uploadMapper.getDetailAuditBfPatientInAfter(serialNo);
        /*List<Map<String, Object>> master = uploadMapper.get3101Master();*/
        //根据主表的serial_no取出所有节点的数据
        if (detailAuditBfPatientIns.size()<=0){
            return "serialNo:"+serialNo+"无数据";
        }
        InsurBusinessConfigVO patient = interfaceMapper.getInterfaceConfigByComfigItem("3102", "I", "patient_dtos");

        Map<String, Object> dataIns = detailAuditBfPatientIns.get(0);
        Object serial_no = dataIns.get("SERIAL_NO");
        Object patient_id = dataIns.get("PATIENT_ID");
        Object curr_mdtrt_id = dataIns.get("CURR_MDTRT_ID");
        Object visit_id = dataIns.get("VISIT_ID");
        try {
            if(curr_mdtrt_id==null || "".equals(curr_mdtrt_id)){
                LOGGER.info("没有curr_mdtrt_id,{}",dataIns);
                return "-1";
            }
            if(serial_no==null || "".equals(serial_no)){
                LOGGER.info("没有SERIALNO,{}",serial_no);
                return "-1";
            }
            detailAuditBfPatientIns=MapUtils.transKeyToLower(detailAuditBfPatientIns,patient.getNode_date_type(),
                    patient.getNode_time_type(),patient.getNode_number_type());
            dataIns=detailAuditBfPatientIns.get(0);

            List<Map<String, Object>> detailAuditAfterEncounterIn = uploadMapper.getDetailAuditBfEncounterAfter(serial_no.toString(), patient_id.toString(),curr_mdtrt_id.toString());
            InsurBusinessConfigVO encounter = interfaceMapper.getInterfaceConfigByComfigItem("3102", "I", "fsi_encounter_dtos");
            detailAuditAfterEncounterIn=MapUtils.transKeyToLower(detailAuditAfterEncounterIn,encounter.getNode_date_type(),
                    encounter.getNode_time_type(),encounter.getNode_number_type());
//            if(detailAuditBfEncounterIn.get(0).get("adm_date")==null || "".equals(detailAuditBfEncounterIn.get(0).get("adm_date"))){
//            detailAuditAfterEncounterIn.get(0).put("adm_date","1900-01-01 00:00:00");
//            }
//            if(detailAuditBfEncounterIn.get(0).get("dscg_date")==null || "".equals(detailAuditBfEncounterIn.get(0).get("dscg_date"))){
//            detailAuditAfterEncounterIn.get(0).put("dscg_date","1900-01-01 00:00:00");
//            }
            List<Map<String, Object>> detailAuditAfterDiagnoseIn = uploadMapper.getDetailAuditAfterDiagnoseIn(serial_no.toString(), patient_id.toString(),Integer.parseInt(visit_id.toString()));
            InsurBusinessConfigVO diagnose = interfaceMapper.getInterfaceConfigByComfigItem("3102", "I", "fsi_diagnose_dtos");
            detailAuditAfterDiagnoseIn=MapUtils.transKeyToLower(detailAuditAfterDiagnoseIn,diagnose.getNode_date_type(),
                    diagnose.getNode_time_type(),diagnose.getNode_number_type());
            List<Map<String, Object>> detailAuditAfterOperationIn = uploadMapper.getDetailAuditAfterOperationIn(serial_no.toString(), patient_id.toString(),Integer.parseInt(visit_id.toString()));
            InsurBusinessConfigVO operation = interfaceMapper.getInterfaceConfigByComfigItem("3102", "I", "fsi_operation_dtos");
            detailAuditAfterOperationIn=MapUtils.transKeyToLower(detailAuditAfterOperationIn,operation.getNode_date_type(),
                    operation.getNode_time_type(),operation.getNode_number_type());
            List<Map<String, Object>> detailAuditAfterOrderIn = uploadMapper.getDetailAuditAfterOrderIn(serial_no.toString(), patient_id.toString(),Integer.parseInt(visit_id.toString()));
            InsurBusinessConfigVO order = interfaceMapper.getInterfaceConfigByComfigItem("3102", "I", "fsi_order_dtos");
            detailAuditAfterOrderIn=MapUtils.transKeyToLower(detailAuditAfterOrderIn,order.getNode_date_type(),
                    order.getNode_time_type(),order.getNode_number_type());
            if(detailAuditAfterEncounterIn==null){
                throw new Exception("没有找到该serialNo的就诊信息,此条记录无法上传。");
            }
            if(detailAuditAfterDiagnoseIn==null || detailAuditAfterDiagnoseIn.size() <= 0){
                throw new Exception("没有找到该serialNo的诊断信息,此条记录无法上传。");
            }
            if(detailAuditAfterOrderIn==null || detailAuditAfterOrderIn.size() <= 0){
                throw new Exception("没有找到该serialNo的医嘱处方信息,此条记录无法上传。");
            }
            if(detailAuditAfterOperationIn!=null){
                detailAuditAfterEncounterIn.get(0).put("fsi_operation_dtos",detailAuditAfterOperationIn);
            }
            detailAuditAfterEncounterIn.get(0).put("fsi_diagnose_dtos",detailAuditAfterDiagnoseIn);
            detailAuditAfterEncounterIn.get(0).put("fsi_order_dtos",detailAuditAfterOrderIn);
            dataIns.put("fsi_encounter_dtos",detailAuditAfterEncounterIn);
            List<Map<String,Object>> fsi_his_data_dto=new ArrayList<>();
            dataIns.put("fsi_his_data_dto",fsi_his_data_dto);
            String recer_sys_code="YBXT";
            String seq = interfaceMapper.getMsgIdSequence();
            CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
            request.setInfno("3102");
            String operateNo = "9999";
            String operateName = "自动上传";
            request.setOpter_type("1");
            request.setOpter(operateNo);
            request.setOpter_name(operateName);
            request.setSign_no(signService.getSignNo(operateNo));
            request.setRecer_sys_code(recer_sys_code);
            HashMap<String,Object> inputMaster = new HashMap<>();
            HashMap<String,Object> input = new HashMap<>();
            inputMaster.put("patient_dtos",dataIns);
            inputMaster.put("syscode","SYMEDSOFT");
            inputMaster.put("task_id",request.getMsgid());
            inputMaster.put("trig_scen",trig_scen);
            input.put("data",inputMaster);
            request.setInput(input);
            String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
            LOGGER.info("3102接口返回上传入参为：" + json);
            byte[] outchar = new byte[1024*1024] ;
            int result = ReadDll.INSTANCE.BUSINESS_HANDLE(json.getBytes("GBK") , outchar);
            String outStr = new String(outchar,"gbk").trim();
            LOGGER.info("3102接口返回上传出参为：" + outStr.trim());
            if (result != 0){
                return outStr;
         }
            // 2、解析返回报文
            CommonResponseVO response = JSONObject.parseObject(outStr , CommonResponseVO.class);
            if ("-1".equals(response.getInfCode())) {
                return outStr;
            }
            if (StringUtils.isNotEmpty(response.getErr_msg())) {
                //修改状态
                return outStr;
            }
            Map <String , Object> ret = response.getOutput();
            if (ret != null && ret.size() > 0) {
                JSONObject jsonObject = JSONObject.parseObject(json);
                JSONObject output=jsonObject.getJSONObject("output");
                JSONArray jsonresult=output.getJSONArray("result");
                for(int i=0;i<jsonresult.size();i++){
                    String jsonret=jsonresult.get(i).toString();
                    Map mapObj = JSONObject.parseObject(jsonret,Map.class);
                    mapObj.put("serial_no",serialNo);
                    mapObj.put("patient_id",patient_id);
                    mapObj.put("operate_no","9999");
                    insertDetailAuditAfterResultOut(mapObj);
                    JSONArray judge_result_detail_dtos=jsonresult.getJSONObject(i).getJSONArray("judge_result_detail_dtos");
                    for (int j=0;j<judge_result_detail_dtos.size();j++){
                        String detail=judge_result_detail_dtos.get(j).toString();
                        Map detailMap=JSONObject.parseObject(detail,Map.class);
                        detailMap.put("serial_no",serialNo);
                        detailMap.put("patient_id",patient_id);
                        detailMap.put("operate_no","111");
                        insertDetailAuditAfterDetailOut(detailMap);
                    }
                }
            }

            //修改状态
            uploadMapper.update3102Status(patient_id.toString(),"1",serial_no.toString());
            LOGGER.info("serialNo 为 {} 的信息采集上传成功！" , serial_no);
        } catch (Exception e) {
            //修改状态
            int i = uploadMapper.update3102Status(patient_id.toString(),"2",serial_no.toString());
            logMapper.insertUploadLog(serial_no.toString(),"3102",patient_id.toString(),e.getMessage());
            LOGGER.error("定时任务信息采集上传调用接口：{} 中的serialNo: {}发生异常，保存信息为：{}"
                    ,"3102", serial_no, e.getMessage());
            //todo
            return e.getMessage();
        }
        return null;
    }

    @Override
    public int insertDetailAuditBfResultOut(Map<String, Object> map) {
        return uploadMapper.insertDetailAuditBfResultOut(map);
    }

    @Override
    public int insertDetailAuditBfDetailOut(Map<String, Object> map) {
        return uploadMapper.insertDetailAuditBfDetailOut(map);
    }

    @Override
    public int insertDetailAuditAfterResultOut(Map<String, Object> map) {
        return uploadMapper.insertDetailAuditAfterResultOut(map);
    }

    @Override
    public int insertDetailAuditAfterDetailOut(Map<String, Object> map) {
        return uploadMapper.insertDetailAuditAfterDetailOut(map);
    }
}
