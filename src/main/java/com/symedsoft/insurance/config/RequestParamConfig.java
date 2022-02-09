package com.symedsoft.insurance.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class RequestParamConfig {

    /** 就医地医保区划 */
    @Value("${requestVO.mdtrtarea_admvs}")
    private String mdtrtarea_admvs;

    /** 接口版本号 */
    @Value("${requestVO.infver}")
    private String infver;

    /** 定点医药机构编号 */
    @Value("${requestVO.fixmedins_code}")
    private String fixmedins_code;

    /** 定点医药机构名称 */
    @Value("${requestVO.fixmedins_name}")
    private String fixmedins_name;

    @Value("${requestVO.url}")
    private String url;

    @Value("${requestVO.operateNo}")
    private String operateNo;

    @Value("${requestVO.operateName}")
    private String operateName;

    @Value("${requestVO.operateType}")
    private String operateType;

    @Value("${requestVO.serv_code}")
    private String serv_code;

    @Value("${requestVO.serv_sign}")
    private String serv_sign;

    @Value("${doctorCode}")
    private String doctorCode;

    @Value("${selfFlag}")
    private String selfFlag;

    @Value("${newBornFlag}")
    private String newBornFlag;


}
