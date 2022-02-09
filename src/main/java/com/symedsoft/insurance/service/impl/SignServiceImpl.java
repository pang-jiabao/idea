package com.symedsoft.insurance.service.impl;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Maps;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.config.SignNoCache;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.LogService;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.utils.*;
import com.symedsoft.insurance.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SignServiceImpl implements SignService {
    private static final Logger logger = LoggerFactory.getLogger(SignServiceImpl.class);

    @Autowired
    private LogService logService;
    @Autowired
    private RequestParamConfig paramConfig;

    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;

    @Override
    public String signIn(String operateNo) {
        String seq = interfaceMapper.getMsgIdSequence();
        CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
        //入参数据
        HashMap<String,Object> input = new HashMap<>();
        String mac = MacUtils.getMacId();
        String ip = IPUtils.getLocalIP();
        Map<String , String> param = Maps.newHashMap();
        param.put("mac" , mac);
        param.put("ip" , ip);
        param.put("opter_no" , operateNo);
        input.put("signIn", param);
        request.setInfno("9001");

        request.setRecer_sys_code("YBXT");
        request.setInput(input);
        String inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
//        insertLog(serialNo,interfaceCode,logSaveNode,json);
//        logService.addRequestLog()
        System.out.println("入参报文为：" + inputStr);
        byte[] outputchar = new byte[1024] ;
        String outputStr = null;
        int res = 0;
        try {
            res = ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outputchar);
            outputStr = new String(outputchar,"GBK");
            System.out.println("出参为：" + outputStr);
        } catch (Exception e) {
            logger.error("调用医保接口异常：{}", e);
        }

        if (res < 0) {
            return "";
        }

        CommonResponseVO response = JSONObject.parseObject(outputStr , CommonResponseVO.class);
        if (!"0".equals(response.getInfCode())) {
            return "";
        }
        Map<String , Object> ret = response.getOutput();
        Map<String , Object> signinoutb = (Map<String , Object>) ret.get("signinoutb");
        if (MapUtil.isEmpty(signinoutb) || signinoutb.get("sign_no") == null) {
            return "";
        }
        String signNo = MapUtils.getObject2String(signinoutb , "sign_no");
        SignNoCache.cache.put(operateNo , signNo);
        return signNo;
    }

    @Override
    public String getSignNo(String operateNo) {
        if (SignNoCache.cache.containsKey(operateNo)) {
            return SignNoCache.cache.get(operateNo);
        }
        return signIn(operateNo);
    }

    @Override
    public String signOut(String operateNo) throws UnsupportedEncodingException {
        String seq = interfaceMapper.getMsgIdSequence();
        CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
        //入参数据
        HashMap<String,Object> input = new HashMap<>();
        Map<String , Object> param = Maps.newHashMap();
        param.put("sign_no" , SignNoCache.cache.get(operateNo));
        param.put("opter_no" , operateNo);
        input.put("signOut", param);
        request.setInfno("9002");
        //request.setInf_time(new Date());
        request.setInput(input);
        String inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);
//        insertLog(serialNo,interfaceCode,logSaveNode,json);
//        logService.addRequestLog()

        byte[] outputchar = new byte[1024] ;
        int res = ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes(), outputchar);
        String outputStr = new String(outputchar,"GBK");
        if (res < 0) {
            return "-1";
        }

        CommonResponseVO response = JSONObject.parseObject(outputStr , CommonResponseVO.class);
        if ("-1".equals(response.getInfCode())) {
            return "-1";
        }
        Map<String , Object> ret = response.getOutput();
        Map<String , Object> signTime = (Map<String , Object>) ret.get("sign_time");
        if (MapUtil.isEmpty(signTime) || signTime.get("sign_time") == null) {
            return "-1";
        }
        SignNoCache.cache.remove(operateNo);
        return "1";
    }
}
