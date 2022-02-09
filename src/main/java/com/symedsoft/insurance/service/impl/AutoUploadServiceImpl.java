package com.symedsoft.insurance.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.sun.javafx.collections.MappingChange;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.exception.CustomException;
import com.symedsoft.insurance.mapper.AutoUploadMapper;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.AutoUploadService;
import com.symedsoft.insurance.service.LogService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yx
 * @version 1.0.0
 * @Description 自动上传服务
 * @createTime 2021年05月24日 09:45:00
 */
@Service
public class AutoUploadServiceImpl implements AutoUploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoUploadServiceImpl.class);
    @Autowired
    private AutoUploadMapper autoUploadMapper;

    @Autowired
    private RequestParamConfig paramConfig;
//    @Autowired
//    private LogService logService;
    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;
    @Autowired
    private SignService signService;
    @Autowired
    private UpLoadAndPreSettleService upLoadAndPreSettleService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    TransactionDefinition transactionDefinition;
    public static int BATCH_SIZE = 100;
    /**
     * 住院费用医保上传
     */
    public void autoUploadInpFee() {
        List<Map<String, Object>> inpAdmit = autoUploadMapper.getInpAdmit();
        for (Map<String,Object> map:inpAdmit){
            String patientId = map.get("PATIENT_ID").toString();
            int visitId = Integer.parseInt(map.get("VISIT_ID").toString());
            String serialNo = map.get("SERIAL_NO").toString();
            try {
                String s = uploadFee(patientId, visitId);
                if ("-1".equals(s)){
                    continue;
                }
                s="2";
                int i=0;
                while ("2".equals(s)){
                    //第一次执行正记录上传
                    s = callInterface(patientId, visitId , false);
                    if ("-1".equals(s)) {
                        break;
                    }
                    //查询正费用是否全部上传完,全部传完才传输负费用
                    int countPositiveFee = autoUploadMapper.getCountPositiveFee(patientId, visitId);
                    if(countPositiveFee > 0){}else{
                        s = callInterface(patientId, visitId, true);
                    }
                    i++;
                    if(i>20){
                        break;
                    }
                }
                //自动预结算
                if("1".equals(s)){
                    upLoadAndPreSettleService.preSettle(patientId,visitId,serialNo);
                }
                //7点钟停止定时任务，防止争抢业务系统线程
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour>6){
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("住院费用医保上传中 patientId 为 {} visitId 为 {} 异常，异常信息为：{}" ,
                        patientId , visitId , e);
            }

        }
    }

    public String uploadinpFee(String patientId, int visitId) throws Exception {
        String ret = "";
        try {
            String s = uploadFee(patientId, visitId);

            //第一次执行正记录上传
            ret = callInterface(patientId,visitId ,false);
            if ("-1".equals(ret)) {
                return ret;
            }
            //查询正费用是否全部上传完,全部传完才传输负费用
            int countPositiveFee = autoUploadMapper.getCountPositiveFee(patientId, visitId);
            if(countPositiveFee > 0){
            }else {
                //第二次执行负记录上传
                ret = compare(ret , callInterface(patientId, visitId, true));
            }
//            Map<String, Object> inpAdmitByPatientId = autoUploadMapper.getInpAdmitByPatientId(patientId, visitId);
//            String serialNo = inpAdmitByPatientId.get("SERIAL_NO").toString();
//            upLoadAndPreSettleService.preSettle(patientId,visitId,serialNo);
        } catch (Exception e) {
            LOGGER.error("自动上传异常，{}" , e);
            throw new Exception(e);
        }
        return ret;
    }

    public String compare(String a , String b) {
        if ("2".equals(a)) {
            return a;
        } else if ("2".equals(b)) {
            return b;
        } else {
            return b;
        }
    }

    /**
     * 将指定的 病人某次住院的账单写入入参表
     * @param patientId
     * @param visitId
     * @return
     * @throws Exception
     */
    public String uploadFee(String patientId, int visitId) throws Exception {
        //事务手动提交
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try{
            LOGGER.info("开始写入参表，patient_id:{} visit_id:{}", patientId, visitId);
            String newBornFlag = paramConfig.getNewBornFlag();
            List<Map<String, Object>> inpDetailFeedetails ;
            if ("1".equals(newBornFlag)){ //1 支持母亲新生儿费用合并上传结算 ，0 不支持
                //获取新生儿信息随母亲费用一起上传
                List<String> bornInfo = autoUploadMapper.getBronInfo(patientId, visitId);

                if (bornInfo != null && bornInfo.size() > 0) {
                    //有新生儿
                    inpDetailFeedetails = autoUploadMapper.getInpDetailFeedetailAndBron(
                            patientId, visitId, bornInfo , paramConfig.getDoctorCode(),paramConfig.getSelfFlag());
                } else {
                    inpDetailFeedetails = autoUploadMapper.getInpDetailFeedetail(patientId, visitId, paramConfig.getDoctorCode(),paramConfig.getSelfFlag());
                }
            }else {
                inpDetailFeedetails = autoUploadMapper.getInpDetailFeedetail(patientId, visitId, paramConfig.getDoctorCode(),paramConfig.getSelfFlag());
            }
            LOGGER.info("查寻入参表完成，patient_id:{} visit_id:{}", patientId, visitId);
            //提前结束抽取数据入入参表的流程
            if (inpDetailFeedetails == null || inpDetailFeedetails.size() == 0) {
                dataSourceTransactionManager.rollback(transactionStatus);
                LOGGER.info("正在执行自动上传费用，patient_id:{} visit_id:{}" +
                        "没有需要写入入参表inp_detail_up_feedetail_in的数据。", patientId, visitId);
                return "1";
            }
            Map<String, Object> inpAdmitByPatientId = autoUploadMapper.getInpAdmitByPatientId(patientId, visitId);
            for (Map<String, Object> inpDetail : inpDetailFeedetails){
                inpDetail.put("PATIENT_ID", patientId);
                inpDetail.put("VISIT_ID", visitId);
                inpDetail.put("VISIT_DATE", null);
                inpDetail.put("OPERATE_NO", "9999");
                inpDetail.put("SERIAL_NO", inpAdmitByPatientId.get("SERIAL_NO"));
                //医院转自费标志
                Object selfflag = inpDetail.get("SELFFLAG");
                if (selfflag!=null && "1".equals(selfflag.toString())){
                    inpDetail.put("HOSP_APPR_FLAG","2");
                }
                selfflag = inpDetail.get("PAY_SELF_FLAG");
                if ("1".equals(selfflag)){
                    inpDetail.put("HOSP_APPR_FLAG","2");
                }
                //中药颗粒转自费
                int medins_list_codg = autoUploadMapper.getZyCount(inpDetail.get("MEDINS_LIST_CODG").toString());
                if (medins_list_codg>0){
                    inpDetail.put("HOSP_APPR_FLA","2");
                }
                //插入入参表
                if (inpDetail.containsKey("BILG_DR_CODG") && inpDetail.get("BILG_DR_CODG") != null) {
                    int i = autoUploadMapper.insertInpDetailFeedetail(inpDetail);
                }
//                Map<String, Object> m=new HashMap<>();
//                m.put("PATIENT_ID",patientId);
//                m.put("VISIT_ID",visitId);
//                m.put("VISIT_DATE",null);
//                m.put("OPERATE_NO","9999");
//                m.put("SERIAL_NO",inpAdmitByPatientId.get("SERIAL_NO"));
//                m.put("NODE_NUM",inpDetail.get("ID"));
//                Object med_type = inpDetail.get("MED_TYPE");//急诊转住院
//                if (med_type!=null &&"24".equals(med_type.toString())){
//                    m.put("ER_FLAG","1");//急诊
//                }else {
//                    m.put("ER_FLAG","0");//非急诊
//                }
//                //插入入参扩展表
//                int j = autoUploadMapper.insertInpDetailFeedetailExp(m);
            }
            LOGGER.info("写入入参表完成，patient_id:{} visit_id:{}", patientId, visitId);
            //修改上传状态
            int uploadCount = 0;
            List<String> itemNos = inpDetailFeedetails.stream()
                    .filter(p -> p.containsKey("BILG_DR_CODG") && !AssertUtils.isBlank(MapUtils.getObject2String(p,"BILG_DR_CODG")))
                    .map(p ->MapUtils.getObject2String(p,"ITEM_NO")).collect(Collectors.toList());
            if (itemNos != null && itemNos.size() > 0) {
                if(itemNos.size()>500){
                    List<List<String>> group = ListUtils.splitList3(itemNos,BATCH_SIZE);
                    for(List<String> oneGroup : group){
                        int count = autoUploadMapper.updateDetailFlag(patientId , visitId , oneGroup);
                        uploadCount = uploadCount + count;
                    }
                }else{
                    uploadCount = autoUploadMapper.updateDetailFlag(patientId , visitId , itemNos);
                }
            }
            LOGGER.info("修改inpbilldetail状态完成，patient_id:{} visit_id:{}", patientId, visitId);
            dataSourceTransactionManager.commit(transactionStatus);
        }catch (Exception e){
            dataSourceTransactionManager.rollback(transactionStatus);
            return "-1";
        }
        return "1";
    }


    /**
     * 调用医保接口上传正记录或者负记录(退回某个账单)费用信息
     * @param patientId
     * @param visitId
     * @param isRollBack    负记录标志
     * @return
     * @throws Exception
     */
    public String callInterface(String patientId, int visitId, boolean isRollBack) throws Exception {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        String retStr = "1";
        try{
            LOGGER.info("开始取入参表数据，patient_id:{} visit_id:{}", patientId, visitId);
            //查询入参表未上传状态的序列号
            List<String> uploadSerialNo = autoUploadMapper.getUploadSerialNo(patientId, visitId);
            if (uploadSerialNo == null || uploadSerialNo.size() == 0) {
                LOGGER.info("执行住院费用信息上传，" +
                        "未在INP_DETAIL_UP_FEEDETAIL_IN表中查询到 " +
                        "patient_id:{} visit_id:{}的数据记录！", patientId, visitId);
                dataSourceTransactionManager.rollback(transactionStatus);
                return retStr;
            }
            for (String serialNo : uploadSerialNo){
                //根据序列号查询前100调费用明细(一次只能传40条)
                //生成json
                List<Map<String, Object>> inpDetailUpFeedetailIn = autoUploadMapper.getInpDetailUpFeedetailIn(serialNo, isRollBack);
                if (inpDetailUpFeedetailIn == null || inpDetailUpFeedetailIn.size() == 0){
                    continue;
                }
                LOGGER.info("入参数据取完，patient_id:{} visit_id:{}", patientId, visitId);
                //入日志表的信息
                Map<String, Object> patientInfo = autoUploadMapper.getPatientInfo(serialNo);
                //统计需要执行的总个数
                LOGGER.info("统计需要执行的总个数开始，patient_id:{} visit_id:{}", patientId, visitId);
                int count = autoUploadMapper.queryInpDetailUpFeedetailInCount(serialNo, isRollBack);
                LOGGER.info("统计需要执行的总个数完成，patient_id:{} visit_id:{}", patientId, visitId);
                //构建入参
                List<Map<String, Object>> inpList = new ArrayList<>();
                List<Object> idList = new ArrayList<>();

                for (Map<String, Object> inpDetailIn : inpDetailUpFeedetailIn) {
                    LOGGER.info("查询扩展字段，patient_id:{} visit_id:{}", patientId, visitId);
                    //Map<String, Object> id = autoUploadMapper.getInpDetailUpFeedetailInExp(inpDetailIn.get("ID"));
                    String medType = autoUploadMapper.getMedType(patientId, visitId);
                    Map<String, Object> id = new HashMap<>();
                    if ("24".equals(medType)){
                        id.put("er_flag","1");//急诊
                    }else {
                        id.put("er_flag","0");//非急诊
                    }
                    LOGGER.info("查询扩展字段，patient_id:{} visit_id:{}", patientId, visitId);
                    idList.add(inpDetailIn.get("ID"));
                    inpDetailIn.remove("ID");
                    inpDetailIn.put("exp_content",id);
                    //获取ITemNo
                    int itemNo=Integer.parseInt(inpDetailIn.get("FEEDETL_SN").toString().replace(patientId,""));
                    Map<String, Object> inpBillDetailOne = autoUploadMapper.getInpBillDetailOne(patientId, visitId, itemNo);
                    //查下该项目最小单位
                    BigDecimal packPrice=null;
                    String firmId="",minSpec = "";
                    Map<String, String> itemUnitsFromInsurVsClinic = null;
                    LOGGER.info("包装转换开始，patient_id:{} visit_id:{}", patientId, visitId);
                    if("A".equals(inpBillDetailOne.get("ITEM_CLASS")) || "B".equals(inpBillDetailOne.get("ITEM_CLASS"))){
                        Map<String,String> minunitSpec = autoUploadMapper.getMinUnits(inpDetailIn.get("MEDINS_LIST_CODG"), inpBillDetailOne.get("ITEM_SPEC"),inpBillDetailOne.get("BILLING_DATE_TIME"));
                        firmId =minunitSpec.get("FIRM_ID");
                        minSpec =minunitSpec.get("MIN_SPEC");
                        //查下该项目的对码标准包装
                        itemUnitsFromInsurVsClinic = autoUploadMapper.getItemUnitsFromInsurVsClinic(inpBillDetailOne.get("ITEM_CODE").toString(), inpBillDetailOne.get("ITEM_SPEC").toString(),
                                inpBillDetailOne.get("UNITS").toString(), inpBillDetailOne.get("ITEM_CLASS").toString());
                        if(itemUnitsFromInsurVsClinic!=null){ //未对标准包装
                            //查寻该包装标志单价
                            packPrice = autoUploadMapper.getPackPrice(inpDetailIn.get("MEDINS_LIST_CODG"), itemUnitsFromInsurVsClinic.get("ITEM_UNIT"),
                                    firmId,inpBillDetailOne.get("BILLING_DATE_TIME"),minSpec);
                        }
                    }
                    LOGGER.info("包装转换完成，patient_id:{} visit_id:{}", patientId, visitId);
                    LOGGER.info("负记录传输开始，patient_id:{} visit_id:{}", patientId, visitId);
                    if (isRollBack) {
                        //查询正记录的主键
//                    LOGGER.info("进入负记录处理方法中");
                        //正记录的价格
                        BigDecimal prePackPrice = null;
                        BigDecimal pric = (BigDecimal) inpDetailIn.get("PRIC");
                        BigDecimal totalCosts = (BigDecimal) inpDetailIn.get("DET_ITEM_FEE_SUMAMT");
                        BigDecimal fcnt = (BigDecimal) inpDetailIn.get("CNT");
                        String medinsListCodg = MapUtils.getObject2String(inpDetailIn, "MEDINS_LIST_CODG");
//                    LOGGER.info("查看对应参数：{} ，{} ，{}" , pric.toString(),medinsListCodg, serialNo);
                        //总价单价数量代码完全一样的项目优先退
                        List<Map<String, Object>> cutDownRecords = autoUploadMapper.queryCutDownEqualsInpDetail(totalCosts,fcnt,pric,medinsListCodg,serialNo);
                        if(cutDownRecords.size()<=0){ //(总价，数量，一致认为是同一个项目)
                            cutDownRecords=autoUploadMapper.queryCutDownOneInpDetail(totalCosts,fcnt,medinsListCodg,serialNo);
                        }
                        if(cutDownRecords.size()<=0){ //(单价，代码一致认为是同一个项目)
                            cutDownRecords=autoUploadMapper.queryCutDownInpDetail(pric,medinsListCodg,serialNo);
                        }

                        if(cutDownRecords.size()<=0){ //调价后找不到正记录时不修改传输状态
                            idList.remove(inpDetailIn.get("ID"));
                        }

                        //LOGGER.info("查询到符合覆盖反记录的正记录有{}条", cutDownRecords.size());
                        Integer seq = 0;

                        //拆分之前负的总费用
                        BigDecimal oldcost=new BigDecimal(inpDetailIn.get("DET_ITEM_FEE_SUMAMT").toString());
                        LOGGER.info("拆分之前负的总费用："+oldcost);
                        BigDecimal oldFee=BigDecimal.ZERO;
                        for (Map<String , Object> cutDownRecord : cutDownRecords) {

                            BigDecimal rollBackAmount = (BigDecimal) inpDetailIn.get("CNT");
                            BigDecimal cutDownAmount = (BigDecimal) cutDownRecord.get("ROLLBACK_REMAINING");
                            BigDecimal difference = cutDownAmount.add(rollBackAmount);
                            LOGGER.info("负记录金额是：{}，正记录的金额是：{}，冲正为：{}", rollBackAmount,cutDownAmount,difference);
                            String cutDownKey = MapUtils.getObject2String(cutDownRecord , "ID");
                            String cutDownFeedetlSn = MapUtils.getObject2String(cutDownRecord,"FEEDETL_SN");

                            //负记录时需以正记录的标准价格为准,期间有可能调价
                            if("A".equals(inpBillDetailOne.get("ITEM_CLASS")) || "B".equals(inpBillDetailOne.get("ITEM_CLASS"))){
                                if(itemUnitsFromInsurVsClinic!=null){
                                    prePackPrice =  autoUploadMapper.getPackPrice(inpDetailIn.get("MEDINS_LIST_CODG"), itemUnitsFromInsurVsClinic.get("ITEM_UNIT"),
                                            firmId,cutDownRecord.get("FEE_OCUR_TIME"),minSpec);
                                }
                            }

                            //正记录能够完全覆盖反记录
                            if (difference.signum() == 1 || difference.signum() == 0) {
                                inpDetailIn.put("INIT_FEEDETL_SN", cutDownFeedetlSn);
                                int i = autoUploadMapper.modifyInpDetailRecord(difference, cutDownKey);
//                            LOGGER.info("修改成功！正记录被冲！{}" , i);
                                //标准包装单价、
                                if(prePackPrice!=null && prePackPrice.doubleValue()>0){
                                    //获取传输总价不变
                                    BigDecimal cost=new BigDecimal(inpDetailIn.get("DET_ITEM_FEE_SUMAMT").toString());
                                    //除过后的数量改变
                                    BigDecimal cnt=cost.divide(prePackPrice).setScale(4, BigDecimal.ROUND_HALF_UP);
                                    //单价位标准单价
                                    inpDetailIn.put("PRIC",prePackPrice);
                                    inpDetailIn.put("CNT",cnt);
                                }
                                LOGGER.info("负记录拆分总费用："+oldFee);
                                inpDetailIn.put("DET_ITEM_FEE_SUMAMT", oldcost.subtract(oldFee));
                                inpList.add(inpDetailIn);
                                break;
                            } else {
                                //正记录不能完全覆盖反记录，需要拆分负记录
                                HashMap<String , Object> newInpDetailIn = new HashMap();
                                newInpDetailIn.putAll(inpDetailIn);
                                //将拆分的第一个与正记录完全冲销
                                BigDecimal negate = cutDownAmount.negate();
                                inpDetailIn.put("CNT", negate);
                                //设置新的总价(单价乘以数量)
                                BigDecimal totalCostNew=cutDownAmount.negate().multiply(new BigDecimal(inpDetailIn.get("PRIC").toString())).setScale(2, BigDecimal.ROUND_HALF_UP);
                                inpDetailIn.put("DET_ITEM_FEE_SUMAMT",totalCostNew);
                                LOGGER.info("拆分后新费用："+totalCostNew);
                                oldFee=oldFee.add(totalCostNew).setScale(2, BigDecimal.ROUND_HALF_UP);
                                LOGGER.info("拆分总新费用："+oldFee);
                                inpDetailIn.put("INIT_FEEDETL_SN", cutDownFeedetlSn);
                                String newFeedetlSn = MapUtils.getObject2String(inpDetailIn,"FEEDETL_SN") +"_"+seq.toString();
                                seq ++;
                                inpDetailIn.put("FEEDETL_SN" , newFeedetlSn);

                                //标准包装单价
                                if(prePackPrice!=null && prePackPrice.doubleValue()>0){
                                    //获取传输总价不变
                                    BigDecimal cost=new BigDecimal(inpDetailIn.get("DET_ITEM_FEE_SUMAMT").toString());
                                    //除过后的数量改变
                                    BigDecimal cnt=cost.divide(prePackPrice).setScale(4, BigDecimal.ROUND_HALF_UP);;
                                    //单价位标准单价
                                    inpDetailIn.put("PRIC",prePackPrice);
                                    inpDetailIn.put("CNT",cnt);
                                }

                                inpList.add(inpDetailIn);
                                //
                                autoUploadMapper.modifyInpDetailRecord(BigDecimal.ZERO , cutDownKey);

                                //将拆分的另一个附上剩余的值
                                newInpDetailIn.put("CNT", difference);
                                BigDecimal totalCost=difference.multiply(new BigDecimal(inpDetailIn.get("PRIC").toString())).setScale(2, BigDecimal.ROUND_HALF_UP);;
                                newInpDetailIn.put("DET_ITEM_FEE_SUMAMT",totalCost);
                                inpDetailIn = newInpDetailIn;
                            }
                        }
                    } else {
                        //标准包装单价
                        if(packPrice!=null && packPrice.doubleValue()>0){
                            //获取传输总价不变
                            BigDecimal cost=new BigDecimal(inpDetailIn.get("DET_ITEM_FEE_SUMAMT").toString());
                            //除过后的数量改变
                            BigDecimal cnt=cost.divide(packPrice).setScale(4, BigDecimal.ROUND_HALF_UP);;
                            //单价位标准单价
                            inpDetailIn.put("PRIC",packPrice);
                            inpDetailIn.put("CNT",cnt);
                        }
                        inpList.add(inpDetailIn);
                    }
                }
                LOGGER.info("负记录传输完成，patient_id:{} visit_id:{}", patientId, visitId);
//            LOGGER.info("反记录写入完成，一共{}条", inpList.size());
                if (inpList.size() == 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    return "没有找到相应的收费记录，不能退费。";
                }

                InsurBusinessConfigVO config = autoUploadMapper.getInpFeeInterfaceConfig();
                String dateType = config.getNode_date_type();
                String timeType = config.getNode_time_type();
                String numberType =  config.getNode_number_type();
                inpList = MapUtils.transKeyToLower(inpList,dateType,timeType,numberType);
                //开始组织医保请求入参requestVO ，入参节点数据input
                String seq = interfaceMapper.getMsgIdSequence();
                CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
                //配置医保接口请求公共入参：infno、msgid、mdtrtarea_admvs...
                request.setInfno("2301");
                List<Map<String,String>> m = interfaceMapper.getInsuplcAdmdvs(patientId);
                String insuplc_admdvsr="";
                String recer_sys_code="YBXT";
                if (m.size()==0){
                    insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
                }else{
                    if (m.get(0).get("psn_type").equals("1300")){
                        recer_sys_code="LXXT";
                    }
                    if (m.get(0).get("insuplc_admdvs")==null){
                        insuplc_admdvsr=paramConfig.getMdtrtarea_admvs();
                    }else {
                        insuplc_admdvsr=m.get(0).get("insuplc_admdvs");
                    }
                }
                request.setInsuplc_admdvs(insuplc_admdvsr);
                //request.setInf_time(new Date());
                //查询经办人信息
                String operateNo = "9999";
                String operateName = "自动上传";
                request.setOpter_type("1");
                request.setOpter(operateNo);
                request.setOpter_name(operateName);
                request.setSign_no(signService.getSignNo(operateNo));
                request.setRecer_sys_code(recer_sys_code);
                HashMap<String,Object> input = new HashMap<>();
                input.put("feedetail",inpList);
                //添加入参节点信息
                request.setInput(input);
                String json = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
                //logService.addRequestLog(serialNo,"2301",patientInfo,json,request.getMsgid());
                byte[] outpchar = new byte[1024*1024];
                LOGGER.info("2301"+"---"+serialNo +"*************************医保调用开始*************************" );
                LOGGER.info("2301"+"---"+serialNo +"**************入参内容:*********:"+json);
                int result = ReadDll.INSTANCE.BUSINESS_HANDLE(json.getBytes("gbk") , outpchar);
                String outpStr = new String(outpchar,"gbk");
                LOGGER.info("2301"+"---"+serialNo+"**************出参内容:*********:"+outpStr.trim());
                LOGGER.info("2301"+"---"+serialNo + "*************************医保调用结束*************************");
                if (result != 0){
                    dataSourceTransactionManager.rollback(transactionStatus);
                    LOGGER.error("2301" +"****************callInsuranceService医保接口调用失败****************");
                    return outpStr.trim();
                }
                try {
                    //保存出参
                    String ret = saveOutputStr(serialNo, "2301" , outpStr , false,patientId,visitId,new Date());
                    LOGGER.info("2301" +"*************************出参保存完成:"+ret+"*************************");
                    //修改传输状态
                    if ("1".equals(ret)){
                        LOGGER.info("修改上传状态，待修改的主键为：{}", idList.toString());
                        int i = autoUploadMapper.updateTransFlag(idList);
                        LOGGER.info("修改上传状态，执行了{}条" , i);
                    }else {
                        dataSourceTransactionManager.rollback(transactionStatus);
                        LOGGER.error("2301" +"*************************出参保存失败:"+ret+"*************************");
                        return outpStr.trim();
                    }
                    retStr = (retStr.equals("2") || count > inpDetailUpFeedetailIn.size()) ? "2" : "1";
                } catch (Exception e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    LOGGER.error("自动上传费用信息接口出错：" + e);
                    retStr = e.getMessage();
                }
            }
            dataSourceTransactionManager.commit(transactionStatus);
        }catch (Exception e){
            dataSourceTransactionManager.rollback(transactionStatus);
            return e.getMessage();
        }
        return retStr;
    }

    /**
     * 保存医保出参
     * @param serialNo 入参序列号
     * @param interfaceCode 接口编号
     * @param outpStr 接口出参
     * @param delOldData 删除老记录
     */
    public String saveOutputStr(String serialNo, String interfaceCode, String outpStr , boolean delOldData,String patientId,int visitId,Date visitDate) throws Exception{
        AssertUtils.notBlank(outpStr, "出参");
        //将医保出参解析为responseVO
        CommonResponseVO response = JSONObject.parseObject(outpStr,CommonResponseVO.class);
        if (response == null || AssertUtils.isBlank(response.getInfCode())) {
            throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：response 无公共响应信息");
        }
        //医保接口调用失败-1，保存日志，直接return -1
        if (!"0".equals(response.getInfCode())) {
            //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：医保接口调用失败：" + response.getErr_msg());
            return "-1";
        }else{
            //接口调用成功不保存出参json
            //logService.updateJsonLog(serialNo,interfaceCode,response,"","");
        }

        /*医保接口调用成功时保存逻辑
         * 保存响应的外层数据：infno、inf_refmsgid、refmsg_time、respond_time、err_msg
         *  update log表（入参解析时新增的日志记录）
         */
        LOGGER.info("=============保存出参信息开始===============");
        LOGGER.info("=============修改日志表===============");
        //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
        LOGGER.info("=============日志表修改完成==============");

        //查询patientId、operateNo、visitDate、visitId
        //HashMap<String,Object> log = logService.queryLogBySerialNo(serialNo, interfaceCode).get(0);
        HashMap<String,Object> log=new HashMap<>();
        log.put("patientId",patientId);
        log.put("visitId",visitId);
        log.put("visitDate",visitDate);
        /*保存输出的节点信息：output
         * 1.查询config出参节点配置
         * 2.遍历配置的节点信息，保存数据
         */
        //当前无出参配置，无需保存output，直接返回成功：1
        LOGGER.info("=============查询config表，并解析节点信息 开始==============");
        List<InsurBusinessConfigVO> configList = interfaceMapper.getInterfaceConfig(interfaceCode,"O");
        if(AssertUtils.isEmptyList(configList)) {
            return "1";
        }

        //有出参配置，返回却无output节点信息，保存此异常信息，返回失败：-1
        Map<String,Object> out = response.getOutput();
        if (out == null || out.isEmpty()) {
            LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：response无output信息");
            response.setErr_msg("response无output信息");
            //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
            return "-1";
        }

        //遍历出参节点配置信息
        for(InsurBusinessConfigVO config : configList){
            //出参节点名、节点对应的表、节点类型（单行、多行）
            String node = config.getNode();
            String table  = config.getNode_table();
            if (StringUtils.isEmpty(table) || StringUtils.isEmpty(node)) {
                LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：没有对应的表或节点信息");
                response.setErr_msg("config表中找不到表或节点信息");
                //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
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
                if (nodeJson == null || nodeJson.isEmpty()){
                    LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                    response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
                    //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                    return "-1";
                }
                nodeDataList.add(nodeJson);
            }else{
                //多行
                nodeDataList = (List<Map<String,Object>>) out.get(node);
                //该出参节点，在出参json串中找不到，记录异常，返回-1
                if (nodeDataList == null || nodeDataList.isEmpty()){
                    LOGGER.error("接口" + interfaceCode + "-" + serialNo + "：output节点标识与配置的节点标识不一致或节点输出数据为空");
                    response.setErr_msg("output节点标识与配置的节点标识不一致或节点输出数据为空");
                    //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                    return "-1";
                }
            }

            /*
             * 1.获取此节点在数据库中的出参配置字段
             * 2.获取此节点出参数据的key的顺序，批量保存foreach时，出参表字段顺序
             * 3.剔除医保出参字段（该字段在数据库中没有配置）
             */
            List<String> nodeColumnList = interfaceMapper.getNodeColumn(interfaceCode,"O",node);
            Set<String> keySet = nodeDataList.get(0).keySet();
            //剔除无配置的出参
            Iterator<String> it = keySet.iterator();
            while(it.hasNext()){
                String key = it.next();
                if(!nodeColumnList.contains(key)){
                    it.remove();
                }
            }
            /*
             * 转换此节点的出参数据 List<Map<String,Object>>  —— nodeDataList
             * 1.将每个Map<String,Object>的val值转换为list<Object>
             * 2.将为时间类型的出参数据根据格式转换为date
             */
            LOGGER.info("=============转换出参信息开始==============");
            List<Map<String,Object>> listData = MapUtils.tranInsertObject(nodeDataList, nodeColumnList, dateType,timeType);
            LOGGER.info("=============转换出参信息结束==============");

            //3、判断是否需要删除出参表中该操作人员写入的老数据
            if (delOldData) {
                //String operateNo = logService.queryOperateNoFromLog(serialNo , interfaceCode);
//                Map<String , Object> param = Maps.newHashMap();
//                param.put("operateNo" , operateNo);
//                param.put("table" , table);
//                interfaceMapper.delTableByOperateNo(param);
            }
            /*
             * 保存该节点的出参数据 List<List<Object>> ——listData
             * listData数据量大时分批次上传
             */
            LOGGER.info("=============保存出参信息开始==============");
            int n = 0 ;
            BATCH_SIZE=40;
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
                //logService.updateResponseLog(serialNo,interfaceCode,response,outpStr);
                return "-1";
            }
            LOGGER.info("=============保存出参信息结束==============");
        }
        return "1";
    }
}
