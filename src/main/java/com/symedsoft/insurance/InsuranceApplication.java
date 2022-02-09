package com.symedsoft.insurance;

import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.service.impl.AutoUploadServiceImpl;
import com.symedsoft.insurance.utils.ReadDll;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.symedsoft.insurance.mapper")
@EnableScheduling
public class InsuranceApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsuranceApplication.class);

    public static void main(String[] args) {
        char[] c=new char[100];
        ReadDll.INSTANCE.INIT(c);
        ConfigurableApplicationContext application = SpringApplication.run(InsuranceApplication.class, args);
        Environment env = application.getEnvironment();
        String path = env.getProperty("spring.datasource.url");
        LOGGER.info("数据库地址:"+path);
    }

}
