package com.symedsoft.insurance.exception;

import com.symedsoft.insurance.common.ResponseCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.sql.SQLException;

/*
 *@author：LL
 *@Date:2021/4/7
 *@Description 异常处理
 */
@RestControllerAdvice
public class ExceptionHandle {
    /**
     * 通用异常处理
     * @param e 异常
     * @return 错误码+信息
     */
    @ExceptionHandler(value=Exception.class)
    @ResponseBody
    public String handleException(Exception e){
        return "-1";
    }

}
