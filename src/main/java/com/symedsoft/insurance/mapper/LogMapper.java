package com.symedsoft.insurance.mapper;

import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.LogVO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 日志mapper
 */
public interface LogMapper {

    /**
     * 插入日志
     * @param serialNo
     * @param interfaceCode
     * @param patientId
     * @param visitDate
     * @param visitId
     * @param operateNo
     * @return
     */
    int insertInsuranceBusinessLog(@Param("serialNo")String serialNo, @Param("interfaceCode")String interfaceCode
            , @Param("patientId")String patientId, @Param("visitDate") Date visitDate
            , @Param("visitId")String visitId, @Param("operateNo")String operateNo, @Param("json")String json,@Param("msgId") String msgId);

    /**
     * 更新日志
     * @param serialNo
     * @param interfaceCode
     * @param response
     * @return
     */
    int UpdateInsuranceBusinessLog(@Param("serialNo")String serialNo, @Param("interfaceCode")String interfaceCode
            , @Param("res") CommonResponseVO response, @Param("json")String json);
    /**
     * 清空日志大文本数据
     * @param serialNo
     * @param interfaceCode
     * @param response
     * @return
     */
    int UpdateInsuranceBusinessJsonLog(@Param("serialNo")String serialNo, @Param("interfaceCode")String interfaceCode
            , @Param("res") CommonResponseVO response, @Param("inJson")String inJson,@Param("outJson")String outJson);

    /**
     * 查询日志中记录的操作人员编号
     * @param serialNo
     * @return
     */
    String queryOperateNoFromLog(@Param("serialNo")String serialNo, @Param("interfaceCode") String interfaceCode);

    /**
     * 查询日志中记录的信息
     * @param serialNo
     * @return
     */
    List<HashMap<String,Object>> queryLogBySerialNo(@Param("serialNo")String serialNo, @Param("interfaceCode") String interfaceCode);

    /**
     * 使用实体类修改记录
     * @param logVO
     * @return
     */
    int UpdateBusinessLogByLog (@Param("log") LogVO logVO);

    int insertUploadLog(@Param("serialNo")String serialNo,@Param("interfaceCode")String interfaceCode,@Param("patientId")String patientId,@Param("errMsg")String errMsg);
}
