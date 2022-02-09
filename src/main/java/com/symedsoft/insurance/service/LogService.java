package com.symedsoft.insurance.service;

import com.symedsoft.insurance.vo.CommonResponseVO;
import com.symedsoft.insurance.vo.LogVO;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface LogService {

    /**
     * 新增请求日志 <br/>
     * 获取入参时，插入日志
     * @param serialNo
     * @param interfaceCode
     * @param node
     * @param json
     * @return
     */
    int addRequestLog (String serialNo, String interfaceCode,
                       Map<String,Object> node, String json,String msgId) throws UnsupportedEncodingException;

    /**
     * 更新接收日志
     * 保存出参时，更新日志
     * @param serialNo
     * @param interfaceCode
     * @param response
     * @param json
     * @return
     */
    int updateResponseLog (String serialNo, String interfaceCode,
                           CommonResponseVO response, String json) throws UnsupportedEncodingException;

    /**
     * 更新接收日志
     * 保存出参时，更新日志
     * @param serialNo
     * @param interfaceCode
     * @param response
     * @param inJson
     * @return
     */
    int updateJsonLog (String serialNo, String interfaceCode,
                           CommonResponseVO response, String inJson,String outJson) throws UnsupportedEncodingException;
    /**
     * 查询操作人编号
     * @param serialNo
     * @param interfaceCode
     * @return
     */
    String queryOperateNoFromLog (String serialNo,String interfaceCode);

    /**
     * 查询patientId、operateNo、visitDate、visitId
     * @param serialNo
     * @param interfaceCode
     * @return
     */
    List<HashMap<String,Object>> queryLogBySerialNo (String serialNo, String interfaceCode);

    /**
     * 新增请求日志
     * @param logVO
     * @return
     * @throws Exception
     */
    int addRequestLog (LogVO logVO) throws Exception;

    /**
     * 新增接收日志
     * @param logVO
     * @return
     * @throws Exception
     */
    int updateResponseLog (LogVO logVO) throws Exception;

}
