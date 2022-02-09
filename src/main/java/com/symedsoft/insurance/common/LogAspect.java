package com.symedsoft.insurance.common;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 *@author：LL
 *@Date:2021/4/20
 *@Description 日志切面
 */
@Aspect
@Component
@Slf4j
public class LogAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAspect.class);

    //定义切入点
    @Pointcut("@annotation(com.symedsoft.insurance.common.OpenLog)")
    public void logMethod() {
    }


    @AfterThrowing(pointcut = "logMethod()",throwing = "e")
    public void LogExceptionInfo(JoinPoint joinPoint,Throwable e){
        Object[] paramValues = joinPoint.getArgs();
        LOGGER.error("***********异常日志***********\n params:{}\n exception:{}",JSONObject.toJSONString(paramValues),e);

    }


}
