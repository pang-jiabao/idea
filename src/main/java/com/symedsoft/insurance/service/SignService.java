package com.symedsoft.insurance.service;

import java.io.UnsupportedEncodingException;

public interface SignService {

    /**
     * 签到
     * @param operateNo
     * @return
     */
    String signIn(String operateNo) throws UnsupportedEncodingException;

    /**
     * 获取SignNo2
     * @param operateNo
     * @return
     */
    String getSignNo(String operateNo) ;

    /**
     * 签退
     * @param operateNo
     * @return
     */
    String signOut(String operateNo) throws UnsupportedEncodingException;
}
