package com.symedsoft.insurance.service.impl;

import com.symedsoft.insurance.exception.CustomException;
import com.symedsoft.insurance.mapper.LogMapper;
import com.symedsoft.insurance.service.LogService;
import com.symedsoft.insurance.utils.AssertUtils;
import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.LogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private LogMapper logMapper;

    @Override
    @Transactional
    public int addRequestLog(String serialNo, String interfaceCode, Map<String, Object> node, String json,String msgId) throws UnsupportedEncodingException {
        String patientId = node.get("PATIENT_ID") == null? "" : node.get("PATIENT_ID").toString();
        Date visitDate = node.get("VISIT_DATE") == null ? null : (Date)node.get("VISIT_DATE");
        String visitId = node.get("VISIT_ID") == null ? "":node.get("VISIT_ID").toString();
        String operateNo = node.get("OPERATE_NO")== null? "" : node.get("OPERATE_NO").toString();
        //记录入参日志
        int n = logMapper.insertInsuranceBusinessLog(serialNo,interfaceCode,patientId
                ,visitDate,visitId,operateNo,json,msgId);
        if(n == 0){
            throw new CustomException("接口" + interfaceCode + "-" + serialNo + "：入参日志记录失败");
        }
        return n;
    }

    @Override
    @Transactional
    public int updateResponseLog(String serialNo, String interfaceCode, CommonResponseVO response, String json) throws UnsupportedEncodingException {
        String err_msg = response.getErr_msg();
        if(!AssertUtils.isBlank(err_msg) && err_msg.getBytes().length > 1600 ){
            response.setErr_msg(err_msg.substring(0, 1000));
        }
        int n = logMapper.UpdateInsuranceBusinessLog(serialNo,interfaceCode,response,json);
        return n;
    }

    @Override
    @Transactional
    public int updateJsonLog(String serialNo, String interfaceCode, CommonResponseVO response, String inJson,String outJson) throws UnsupportedEncodingException {
        String err_msg = response.getErr_msg();
        if(!AssertUtils.isBlank(err_msg) && err_msg.getBytes().length > 1600 ){
            response.setErr_msg(err_msg.substring(0, 1000));
        }
        int n = logMapper.UpdateInsuranceBusinessJsonLog(serialNo,interfaceCode,response,inJson,outJson);
        return n;
    }
    @Override
    public String queryOperateNoFromLog(String serialNo, String interfaceCode) {
        return logMapper.queryOperateNoFromLog(serialNo , interfaceCode);
    }

    @Override
    public List<HashMap<String, Object>> queryLogBySerialNo(String serialNo, String interfaceCode) {
        return logMapper.queryLogBySerialNo(serialNo,interfaceCode);
    }

    @Override
    public int addRequestLog(LogVO logVO) throws Exception {
        String patientId = logVO.getPatientId() == null ? ""   : logVO.getPatientId();
        Date visitDate   = logVO.getVisitDate() == null ? null : logVO.getVisitDate();
        String visitId   = logVO.getVisitId()   == null ? ""   : logVO.getVisitId();
        String operateNo = logVO.getOperateNo() == null ? ""   : logVO.getOperateNo();
        //记录入参日志
        int n = logMapper.insertInsuranceBusinessLog(logVO.getSerialNo(),logVO.getInterfaceCode(),patientId
                ,visitDate,visitId,operateNo,logVO.getInputJson(),logVO.getMsgid());
        if(n == 0){
            throw new CustomException("接口" + logVO.getInterfaceCode() + "-" + logVO.getSerialNo() + "：入参日志记录失败");
        }
        return n;
    }

    @Override
    public int updateResponseLog(LogVO logVO) throws Exception {
        return 0;
    }

}
