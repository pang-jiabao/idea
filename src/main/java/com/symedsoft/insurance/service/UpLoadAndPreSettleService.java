package com.symedsoft.insurance.service;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

/*
 *@author：LL
 *@Date:2021-08-18
 *@Description
 */
public interface UpLoadAndPreSettleService {
    String preSettle(String patientId,int visitId,String serialNo) throws Exception;
}
