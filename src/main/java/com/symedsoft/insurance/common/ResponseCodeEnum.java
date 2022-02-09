package com.symedsoft.insurance.common;

/*
 * @author LL
 * @Date 2020-04-07
 * @Decripetion 响应状态码
 */
public enum ResponseCodeEnum {

    SUCCESS(0,"成功"),
    FAIL(-1,"失败"),
    EXCEPTION_SQL(506,"数据库操作错误"),
    EXCEPTION_NULLPOINT(507,"空指针错误"),
    EXCEPTION_IO(508,"IO错误"),
    EXCEPTION_ARITH(509,"算数错误"),
    EXCEPTION_ARRAY(510,"数组越界"),
    EXCEPTION_RUNTIME(511,"运行异常"),
    EXCEPTION_TRANS(512,"事务异常"),
    EXCEPTION_CUSTOM(555,"自定义异常"),

    ;

    private Integer code;
    private String msg;


    ResponseCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
