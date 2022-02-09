package com.symedsoft.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestDto {

    /* 配置在服务网关中的服务接口名*/
    private String apiName;

    /* 配置在服务网关中的服务版本号*/
    private String apiVersion = "1.0.0";

    /* 配置在服务网关中的AK (accessKey)*/
    private String apiAccessKey;

    /* 配置在服务网关中的 SK(secretKey)*/
    private String apiSecretKey;

    /*时间戳*/
    private Long timeMillis = System.currentTimeMillis();

    /*调用地址*/
    private String url;

    /*请求体数据*/
    private String requestBody;



}
