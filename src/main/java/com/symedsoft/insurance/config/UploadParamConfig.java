package com.symedsoft.insurance.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class UploadParamConfig {
    @Value("${checkAcctDettail.queryUpload}")
    private String queryUpload;
}
