package com.symedsoft.insurance.service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/*
 *@author：LL
 *@Date:2021/5/13
 *@Description
 */
public interface InsuranceConfigService {

    String getInputJsonStr(String serialNo, String interfaceCode, boolean verify) throws UnsupportedEncodingException;

    String getUploadInputJsonStr(String serialNo, String interfaceCode, boolean verify) throws UnsupportedEncodingException;

    String getUploadInputJsonStrById(String serialNo, String interfaceCode, boolean verify) throws UnsupportedEncodingException;

    String saveOutputStr(String serialNo, String interfaceCode, String outpStr, boolean delOldData) throws Exception;

    String saveOutputStr5402(String serialNo, String interfaceCode, String outpStr, boolean delOldData) throws Exception;

    String saveReadCardOutputStr(String serialNo, String interfaceCode, String outpStr, boolean delOldData, String newborn) throws Exception;

    Map<String , Object> getInputJsonStrByDownload (String interfaceCode) throws Exception;

    String getCommonJsonStr(String serialNo, String interfaceCode) throws UnsupportedEncodingException;

    String getInp1162(String serialNo, String interfaceCode) throws UnsupportedEncodingException;

    String call2601(String serialNo,String interfaceCode) throws Exception;

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
