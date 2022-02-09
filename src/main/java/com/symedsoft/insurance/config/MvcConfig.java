package com.symedsoft.insurance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private SignInterceptor signInterceptor;

    /**
     * 注册配置的拦截器
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 这里的拦截器是new出来的，在Spring框架中可以交给IOC进行依赖注入，直接使用@Autowired注入
//        registry.addInterceptor(signInterceptor);
    }
}
