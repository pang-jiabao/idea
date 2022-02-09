package com.symedsoft.insurance.utils;

import com.symedsoft.insurance.controller.InsuranceController;
import com.symedsoft.insurance.dto.RequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * 通用调用医保服务方法
 */
public class InsurBusinessHandle {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsurBusinessHandle.class);

    public static String businessHandle(RequestDto dto){
        byte[] outpchar = new byte[1024*1024];
        String outpStr = "";
        try {
            int res =  ReadDll.INSTANCE.BUSINESS_HANDLE(dto.getRequestBody().getBytes("GBK"), outpchar);
            outpStr=new String(outpchar,"GBK");
            if(res < 0){
                LOGGER.error("****************callInsuranceService医保接口调用失败****************");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "-1";
        }
        return outpStr.trim();
    }
}
