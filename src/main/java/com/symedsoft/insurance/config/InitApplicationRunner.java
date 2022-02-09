package com.symedsoft.insurance.config;

import com.symedsoft.insurance.service.SignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitApplicationRunner implements ApplicationRunner {
    @Autowired
    private SignService signService;

    @Autowired
    private RequestParamConfig requestParamConfig;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("第一次执行，获取管理员信息，并对其签到");
        // todo 临时注释
        System.err.println("管理员的Sign_no为：" + signService.signIn(requestParamConfig.getOperateNo()));
    }
}
