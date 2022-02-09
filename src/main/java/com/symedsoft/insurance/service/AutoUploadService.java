package com.symedsoft.insurance.service;

import java.io.UnsupportedEncodingException;

/**
 * @author yx
 * @version 1.0.0
 * @Description
 * @createTime 2021年05月24日 09:45:00
 */
public interface AutoUploadService {

    void autoUploadInpFee() throws Exception;

    String uploadinpFee(String patientId,int visitId) throws Exception;
}
