package com.symedsoft.insurance.vo;

import lombok.Data;

import java.util.Map;

/*
 *@author：LL
 *@Date:2021/5/15
 *@Description 响应VO
 */
@Data
public class CommonResponseVO {
    private String infCode;       //交易状态码
    private String inf_refmsgid;       //接收方报文ID
    private String refmsg_time;       //接收报文时间
    private String respond_time;       //响应报文时间
    private String err_msg;       //错误信息
    private Map<String,Object> output;       //交易输出
}
