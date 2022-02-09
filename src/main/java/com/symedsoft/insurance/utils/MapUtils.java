package com.symedsoft.insurance.utils;

import cn.hutool.core.date.DateUtil;
import org.apache.commons.lang3.time.DateUtils;
import java.text.ParseException;
import java.util.*;

/*
 *@author：LL
 *@Date:2021/5/14
 *@Description map转换
 */
public class MapUtils {
    private final static  List<String> COMMON_PROPERTY =  new ArrayList<>(Arrays.asList
            ("ID","SERIAL_NO","PATIENT_ID","VISIT_ID","VISIT_DATE","OPERATE_NO","CANCEL_FLAG"));

    /**
     * 入参listmap key转小写，时间类型转换，number默认值设置
     * @param list  数据库节点入参表数据
     * @param dateType 此节点传参类型为yyyy-MM-dd的字段 ，逗号隔开
     * @param timeType 此节点传参类型为yyyy-MM-dd hh:mm:ss的字段 ，逗号隔开
     * @param numberType 此节点传参类型为数值的字段 ，逗号隔开
     * @return
     */
    public static List<Map<String, Object>> transKeyToLower(List<Map<String,Object>> list,String dateType,String timeType,String numberType) {
        List<Map<String, Object>> newList = new ArrayList<>();
        List<String> dateList = dateType == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(dateType.split(",")));
        List<String> timeList = timeType == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(timeType.split(",")));
        List<String> numberList = numberType == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(numberType.split(",")));

        Iterator<Map<String, Object>> iterator = list.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> m = iterator.next();
            Map<String, Object> newMap = new HashMap<>();
            for (String key : m.keySet()) {
                //去掉入参表的公共字段
                if(COMMON_PROPERTY.contains(key)){
                    continue;
                }
                String lowKey = key.toLowerCase();
                //医保接口入参为空时：字符串默认"" 数值类型默认0
                if( m.get(key) == null){
                    newMap.put(lowKey, "");
                    //数值类型为空时，默认0
                    if(numberList.contains(lowKey)){
                        newMap.put(lowKey, 0);
                    }
                }else{
                    //入参非空时：时间类型入参转为对应格式的字符串
                    newMap.put(key.toLowerCase(), m.get(key));
                    //传参为日期的类型
                    if(dateList.contains(lowKey)){
                        Date date = (Date) m.get(key);
                        newMap.put(lowKey, DateUtil.format(date, "yyyy-MM-dd"));
                    }
                    //传参为时分秒的类型
                    if(timeList.contains(lowKey)){
                        Date date = (Date) m.get(key);
                        newMap.put(lowKey, DateUtil.format(date, "yyyy-MM-dd HH:mm:ss"));
                    }
                }
            }
            newList.add(newMap);
            iterator.remove();//使用迭代器的删除方法删除
        }
        return newList;
    }

    /**
     * 处理出参，时间类型转换
     * @param nodeDataList 医保接口出参节点信息
     * @param nodeColumnList 数据库中出参节点对应的字段（医保接口出参比db配置的多）
     * @param dateType 该出参节点类型是yyyy-MM-dd时间类型的字段 逗号隔开
     * @param timeType 该出参节点类型是yyyy-MM-dd hh:mm:ss 时间类型的字段 逗号隔开
     * @return List<List<Object>>
     * @throws ParseException
     */
    public static List<Map<String,Object>> tranInsertObject(List<Map<String, Object>> nodeDataList, List<String> nodeColumnList, String dateType, String timeType) throws ParseException {
        List<String> dateColumnList = dateType == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(dateType.split(",")));
        List<String> timeColumnList = timeType == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(timeType.split(",")));
        List<Map<String,Object>> nodeValList = new ArrayList<>();

        for(Map<String,Object> json : nodeDataList){
            Map<String,Object> valMap = new HashMap<>();
            for(String key : json.keySet() ){
                //医保出参的字段，在数据库中无配置，剔除此数据，否则保存出参会有无效字段异常
                if (!nodeColumnList.contains(key.toLowerCase())){
                    continue;
                }
                if(json.get(key) != null && !"".equals(json.get(key)) ){
                    //医保出参非空时：时间类型字符串需转为对应的date类型存入数据库
                    if(dateColumnList.contains(key)){
                        Date date = DateUtils.parseDate(json.get(key).toString(), "yyyy-MM-dd");
                        valMap.put(key,date);
                    }else if (timeColumnList.contains(key)){
                        Date time = DateUtils.parseDate(json.get(key).toString(), "yyyy-MM-dd HH:mm:ss");
                        valMap.put(key,time);
                    }else{
                        valMap.put(key,json.get(key));
                    }
                }else{
                    valMap.put(key,json.get(key));
                }
            }

            nodeValList.add(valMap);
        }
        return nodeValList;
    }

    /**
     * 从map中取出出局并转换成String
     * @param map
     * @param key
     * @return
     */
    public static String getObject2String (Map<String , Object> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key) == null ? "" : map.get(key).toString();
        } else {
            return null;
        }
    }


}
