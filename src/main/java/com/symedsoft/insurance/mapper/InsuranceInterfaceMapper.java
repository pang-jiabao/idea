package com.symedsoft.insurance.mapper;

import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.InsurBusinessConfigVO;
import org.apache.ibatis.annotations.Param;

import java.util.*;

/*
 *@author：LL
 *@Date:2021/5/13
 *@Description
 */
public interface InsuranceInterfaceMapper {
    /**
     * 获取医保接口的入参配置
     * @param code 接口编码
     * @param io I-入参 O-出参
     * @return
     */
    List<InsurBusinessConfigVO> getInterfaceConfig(@Param("code") String code, @Param("io") String io);

    /**
     * 获取医保接口的入参配置根据项目节点
     * @param code 接口编码
     * @param io I-入参 O-出参
     * @return
     */
    InsurBusinessConfigVO getInterfaceConfigByComfigItem(@Param("code") String code, @Param("io") String io,@Param("itemName") String itemName);
    /**
     * 查询对应入参表的入参
     * @param table 表名
     * @param serialNo 入参序号
     * @return
     */
    List<Map<String,Object>> getInputByTableNameAndSerialNo(@Param("table") String table,@Param("serialNo") String serialNo);

    /**
     * 查询对应入参表的入参
     * @param table 表名
     * @param id 入参序号
     * @return
     */
    List<Map<String,Object>> getInputByTableNameAndId(@Param("table") String table,@Param("id") String id);

    /**
     * 插入结果表
     * @param table 数据库表名
     * @param serialNo 入参序号
     * @param listData  多行数据的vallist
     * @param log  patientId、operateNo、visitDate、visitId
     * @return
     */
    int insertOut(@Param("table")String table,@Param("serialNo")String serialNo
            , @Param("listData")List<Map<String,Object>> listData,@Param("log")HashMap<String,Object> log);

    int insertOutWithOutBase(@Param("table")String table,@Param("serialNo")String serialNo
            , @Param("listData")List<Map<String,Object>> listData);

    /**
     * 查询对应入参表的入参
     * @param param
     * @return
     */
    List<Map<String , Object>> getInputByTableNameAndCase (Map<String , String> param) ;

    /**
     * 删除该员工编号的
     * @param param
     * @return
     */
    boolean delTableByOperateNo(Map<String , Object> param);

    /**
     * 查询操作人姓名
     * @param operateNo
     * @return
     */
    String selectOperateNameByNo(String operateNo);

    /**
     * 获取医保接口的入参的扩展字段
     * @param code 接口编码
     * @param io I-入参 O-出参
     * @return
     */
    List<Map<String,String>> selectExpProperty(@Param("code") String code, @Param("io") String io);

    /**
     * 获取报文序列号
     * @return
     */
    String getMsgIdSequence ();

    /**
     * 查询病人的参保区划
     * @param patientId
     * @return
     */
    List<Map<String, String>> getInsuplcAdmdvs(@Param("patientId") String patientId);

    //获取配置表节点的字段
    List<String> getNodeColumn(@Param("code") String interfaceCode, @Param("IO") String o, @Param("node") String node);

    Map<String, Object> getPatientCardecinfoIn(@Param("serialNo") String serialNo);

    Map<String,Object> getPatientIdforInp(@Param("table") String table,@Param("serialNo") String serialNo);


    int updateGetPatientBaseinfoOut(@Param("serialNo") String serialNo);

    /**
     * 根据psnNo查询 patientId、serialNo
     * @param psnNo
     * @return
     */
    List<Map<String , String>> queryReadCardBaseInfoByPsnNo(@Param("psnNo") String psnNo , @Param("newborn") String newborn);

    /**
     * 根据id查询姓名
     * @param patientId
     * @return
     */
    String getNameToPatMasterIndex(String patientId);

    /**
     * 修改门诊结算出参状态表
     * @param serialNo
     * @return
     */
    int updateOutpSetterOutFlag(String serialNo);

    /**
     * 修改门诊结算入参状态表
     * @param serialNo
     * @return
     */
    int updateOutpSetterInFlag(String serialNo);

    /**
     * 修改住院结算出参状态表
     * @param serialNo
     * @return
     */
    int updateInpSetterOutFlag(String serialNo);

    /**
     *修改住院结算入参状态表
     * @param serialNo
     * @return
     */
    int updateInpSetterInFlag(String serialNo);


}
