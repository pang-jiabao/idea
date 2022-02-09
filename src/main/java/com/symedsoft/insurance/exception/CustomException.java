package com.symedsoft.insurance.exception;

/*
 *@author：LL
 *@Date:2021/4/8
 *@Description 自定义异常
 */
public class CustomException extends RuntimeException {
    public CustomException() {
        super();
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomException(String message) {
        super(message);
    }

    public CustomException(Throwable cause) {
        super(cause);
    }
}
