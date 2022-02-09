package com.symedsoft.insurance.vo;


import lombok.Data;

/**
 * @author pjb
 * @Description 封装返回数据 code + msg + data
 * @createTime 2022/01/20
 */
@Data
public class ApiResultVo<T>{
    private int code;
    private String message;
    private T data;
    public ApiResultVo() {                  //成功调用的构造方法
        this.code = 200;
        this.message = "调用执行成功!";
    }

    public ApiResultVo(String message) {    //失败调用的构造方法
        this.code = 400;
        this.message = message;
    }

}
