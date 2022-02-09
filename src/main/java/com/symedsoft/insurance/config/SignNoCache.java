package com.symedsoft.insurance.config;

import java.util.HashMap;
import java.util.Map;

public class SignNoCache {

    /**
     * sign_no 全局缓存，默认去这里读取和写入 sign_no<br/>
     * 里面有 operateNo 和 sign_no
     */
    public static final Map<String , String> cache = new HashMap<>();
}
