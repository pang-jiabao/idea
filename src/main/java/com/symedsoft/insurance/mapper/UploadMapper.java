package com.symedsoft.insurance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UploadMapper {

    Map<String,Object> selectSettlInfo(@Param("patientid") String patientId,@Param("visitId") int visitId);

    List<Map<String,Object>> selectInsurPatienInfo(@Param("patientid") String patientId);

    Map<String,Object> selectInpAdmintInfo(@Param("patientid") String patientId,@Param("visitId") int visitId);

    List<Map<String,Object>> selectInpLeaveDiseinfo(@Param("patientid") String patientId,@Param("visitId") int visitId);

    List<Map<String,Object>> selectDetailInfo(@Param("patientid") String patientId,@Param("visitId") int visitId);


    List<Map<String,Object>> selectOperationInfo(@Param("patientid") String patientId,@Param("visitId") int visitId);

    int insertSetlInfo(Map<String,Object> map);

    int insertPayInfo(Map<String,Object> map);

    int insertDiseInfo(Map<String,Object> map);

    int insertItemInfo(Map<String,Object> map);

    int insertOpspDise(Map<String,Object> map);

    int insertOprnInfo(Map<String,Object> map);

    int insertIcuInfo(Map<String,Object> map);

    String getSerialNo();

    void insert4401procedure(Map<String,Object> map);

    void callProcedure(Map<String,Object> map);

    String getConfig(String interfaceCode);

    List<String> getInterfaceSerialNo(String configTableName);

    int updateInterfaceTableBySerialNo(@Param("configTableName") String configTableName,@Param("serialNo") String serialNo,@Param("flag") String flag);

    int updateInterfaceTableById(@Param("configTableName") String configTableName,@Param("id") String id,@Param("flag") String flag);

    List<Map<String,Object>> get3101Master();

    List<Map<String,Object>> getDetailAuditBfPatientIn(@Param("serialNo") String serialNo);

    List<Map<String,Object>> getDetailAuditBfPatientInAfter(@Param("serialNo") String serialNo);

    List<Map<String,Object>> getDetailAuditBfEncounterIn(@Param("serialNo")String serialNo);

    List<Map<String,Object>> getDetailAuditBfEncounterAfter(@Param("serialNo")String serialNo,@Param("patientId")String patientId,@Param("mdtrtId") String mdtrtId);


    List<Map<String,Object>> getDetailAuditBfDiagnoseIn(@Param("serialNo")String serialNo);

    List<Map<String,Object>> getDetailAuditAfterDiagnoseIn(@Param("serialNo")String serialNo,@Param("patientId")String patientId,@Param("visitId")Integer visitId);


    List<Map<String,Object>> getDetailAuditBfOrderIn(@Param("serialNo")String serialNo);

    List<Map<String,Object>> getDetailAuditAfterOrderIn(@Param("serialNo")String serialNo,@Param("patientId")String patientId,@Param("visitId")Integer visitId);

    List<Map<String,Object>> getDetailAuditBfOperationIn(@Param("serialNo")String serialNo);

    List<Map<String,Object>> getDetailAuditAfterOperationIn(@Param("serialNo")String serialNo,@Param("patientId")String patientId,@Param("visitId")Integer visitId);


    int update3101Status(@Param("patientId")String patientId,@Param("uploadFlag")String uploadFlag,@Param("serialNo")String serialNo);

    int update3102Status(@Param("patientId")String patientId,@Param("uploadFlag")String uploadFlag,@Param("serialNo")String serialNo);


    Map<String,Object> getProcedureRecord(String procedureName);

    int insertProcedureRecord(Map<String,Object> map);

    int updateProcedureRecord(Map<String,Object> map);

    List<String> getInterfaceId(String configTableName);

    int updateInpSetFlag(@Param("patientId") String patientId,@Param("visitId") int visitId);

    List<Map<String,Object>> selectInpSetlePatient();

    //保存事前result
    int insertDetailAuditBfResultOut(Map<String,Object> map);
    //保存事前明细
    int insertDetailAuditBfDetailOut(Map<String,Object> map);

    //保存事中result
    int insertDetailAuditAfterResultOut(Map<String,Object> map);
    //保存事中明细
    int insertDetailAuditAfterDetailOut(Map<String,Object> map);
}
