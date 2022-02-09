package com.symedsoft.insurance.utils;


import com.symedsoft.insurance.exception.CustomException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/*
 *@author：LL
 *@Date:2021/4/8
 *@Description obj、字符串校验
 */
public class AssertUtils {

    /**
     * 多字符串非空校验
     * @param msg 异常提示信息，多字段时，隔开
     * @param ss 校验字符串
     * @example NotBlank("姓名,地址,电话",name,adress,tel)
     */
    public static void notBlank(String msg,String...ss)  {
        for (int i =0 ;i< ss.length;i++){
             if(isBlank(ss[i])) {
                 msg = msg.replace("，", ",");
                 String val = msg.split(",")[i];
                 throw new CustomException(val + "不能为空");
             }
        }
    }

    /**
     * 多obj非空校验
     * @param objs 待校验对象
     * @param msg 异常信息
     */
    public static void notBlank( String msg,Object...objs){
        for (int i =0 ;i< objs.length;i++){
            if(objs[i] == null || StringUtils.isBlank(objs[i].toString())) {
                msg = msg.replace("，", ",");
                String val = msg.split(",")[i];
                throw new CustomException(val + "不能为空");
            }
        }
    }

    /**
     * 单字符串非空校验
     * @param s 校验字符串
     * @param msg 为空时的异常信息
     */
    public static void notBlank(String s , String msg)  {
        if(isBlank(s)) {
            throw new CustomException(msg + "不能为空");
        }
    }

    /**
     * 是否空list
     * @param list 列表
     */
    public static boolean isEmptyList(List<?> list)  {
       return list ==null || list.isEmpty();
    }

    /**
     * 多obj非空校验
     * @param objs 待校验对象
     * @param msg 异常信息
     */
    public static void notNull( String msg,Object...objs){
        for (int i =0 ;i< objs.length;i++){
            if(objs[i] == null) {
                msg = msg.replace("，", ",");
                String val = msg.split(",")[i];
                throw new CustomException(val + "不能为空");
            }
        }
    }

    /**
     * obj非空校验
     * @param obj 待校验对象
     * @param msg 异常信息
     */
    public static void notNull(Object obj, String msg){
        if (obj == null) {
            throw new CustomException(msg);
        }
    }

    /**
     * 校验字符串是否是正确的时间格式
     * @param format 转换格式
     * @param dateStr 待转换字符串
     * @param msg 转换错误提示
     */
    public static Date StrToDate(String format,String dateStr,String msg)  {
        Date date = null;
        try {
            date = DateUtils.parseDate(dateStr, format);
        }catch(ParseException e){
            throw new CustomException(msg + "时间格式错误："+ format);
        }
        return date;
    }

    /**
     *  校验double非空并且非负
     * @param dd 参数
     * @param msg  参数的意义
     * @return double 保留两位小数
     */
    public static double verifyDouble(Double dd,String msg){
        if(dd == null){
            throw new CustomException(msg + "不能为空");
        }
        if(dd<0.0){
            throw new CustomException(msg + "不能为负数");
        }
        return doubleByTwo(dd);
    }

    /**
     * 校验非空字符串（包括"null"）
     * @param s
     * @return
     */
    public static boolean isBlank(String s){
        return StringUtils.isBlank(s) || "null".equals(s.trim());
    }

    /**
     * double保留2位小数
     * @param d
     * @return
     */
    public static double doubleByTwo(Double d){
        return new BigDecimal(d).setScale(2,   BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * list非空验证
     * @param list
     * @param msg
     */
    public static void notEmptyList(List<?> list, String msg) {
        if(isEmptyList(list)){
            throw new CustomException(msg);
        }
    }
}
