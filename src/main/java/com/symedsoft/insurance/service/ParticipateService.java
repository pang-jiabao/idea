package com.symedsoft.insurance.service;


import com.symedsoft.insurance.vo.ApiResultVo;

public interface ParticipateService {
    //数据写入入参表通用方法
    ApiResultVo insertService(String interfaceCode, Integer tableCode , String data);
}
