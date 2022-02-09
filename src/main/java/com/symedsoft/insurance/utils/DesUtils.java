package com.symedsoft.insurance.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;

public class DesUtils {
    private static Logger logger = LoggerFactory.getLogger(DesUtils.class);

    /**
     * 加密
     * @param str
     * @return
     */
    public static String encoder(String str) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(str.getBytes());
    }

    /**
     * 解密
     * @param str
     * @return
     */
    public static String decoder(String str) {
        BASE64Decoder decoder = new BASE64Decoder();
        String decode = null;
        try {
            decode = new String(decoder.decodeBuffer(str));
            System.out.println("decode: " + decode);
        } catch (IOException e) {
            logger.error("IO异常：{}" , e);
        }
        return decode;
    }

    public static void main(String[] args) {
        String src = "A6B3720544049E1397";
        BASE64Encoder encoder = new BASE64Encoder();
        String encode = encoder.encode(src.getBytes());
        System.out.println("加密: "+encode);

        BASE64Decoder decoder = new BASE64Decoder();
        try {
            String decode = new String(decoder.decodeBuffer(encode));
            System.out.println("解密: " + decode);
        } catch (IOException e) {
            logger.error("IO异常：{}" , e);
            e.printStackTrace();
        }
    }

}
