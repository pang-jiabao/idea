package com.symedsoft.insurance.service;

import java.text.ParseException;
import java.util.Map;

/**
 * 【4101】医疗保障基金结算清单信息上传
 */
public interface UploadService {

    Object callProcedure(String procedureName) throws ParseException;

    String callInterfaceBySerialNo(String interfaceCode);
    String callInterfaceById(String interfaceCode);
    String callInterfaceById(String interfaceCode,String id);
    void autoUpload4101();

    void autoUpload4401();
    String upload4101ByPatient(String patientId,int visitId) ;

    String detailAuditBefore(String serialNo,String trig_scen);

    String detailAuditAfter(String serialNo,String trig_scen);

    int insertDetailAuditBfResultOut(Map<String,Object> map);

    int insertDetailAuditBfDetailOut(Map<String,Object> map);

    int insertDetailAuditAfterResultOut(Map<String,Object> map);

    int insertDetailAuditAfterDetailOut(Map<String,Object> map);
}
