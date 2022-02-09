package com.symedsoft.insurance.service.impl;

import com.alibaba.fastjson.JSON;
import com.symedsoft.insurance.mapper.ParticipateMapper;
import com.symedsoft.insurance.service.ParticipateService;
import com.symedsoft.insurance.vo.ApiResultVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**********************************************************************************
 *
 *  @author pjb
 *  @Description 数据表数据写入接口
 *  @createTime 2021/12/27
 *
 * 通用的数据表 数据插入功能
 * 处理步骤:
 * 1.接收到的json字符串，转为map,方便数据插入处理
 * 2.空值检查
 * 3.通过接口号和表代码查询数据库  获得表名，日期和日期时间型字段
 * 4.给日期型加一个字段visit_date，不用在配置表中为每一个表都添加visit_date这个字段
 * 5.新增需求:对于住院相关表的 mdtrt_id，psn_no 字段，需要通过patinet_id,id + visit_id 查询得到
 * 6.为每一个字段值加上单引号
 * 7.数据里面添加上主动生成值的两个字段
 * 8.处理日期类型字段的格式转换问题
 * 9.动态封装表名和数据
 * 10.执行插入数据操作
 *********************************************************************************/

@Service
public class ParticipateServiceImpl implements ParticipateService {
    private static final Logger logger = LoggerFactory.getLogger(DictServiceImpl.class);

    @Autowired
    private ParticipateMapper participateMapper;

    @Autowired
    private UploadServiceImpl uploadService;

    @Override
    public ApiResultVo insertService(String interfaceCode, Integer tableCode, String data) {

        //接收到的json字符串，转为map,方便数据插入处理
        Map<String,Object> map = null;
        try {
            map = JSON.parseObject(data);
        } catch (Exception e) {
            logger.error("json字符串解析异常");
            return new ApiResultVo<String>("json字符串解析异常");
        }

        //空值检查
        for(String key : map.keySet()){
            String value = map.get(key).toString();
            if(StringUtils.isEmpty(value)) {
                logger.error(key+"值为空");
                return new ApiResultVo<String>("值不能为空");
            }
        }

        //通过接口号和表代码查询数据库  获得表名，日期和日期时间型字段
        Map<String,String>    result = participateMapper.getTableFields(interfaceCode,tableCode);
        if( result == null || result.isEmpty() ){                               //判断请求表是否存在
            logger.error("请求参数错误或请求表不存在");
            return new ApiResultVo<String>("请求参数错误或请求表不存在");
        }

        String fieldTableName = result.get("NODE_TABLE");                       //获取表名
        String fieldTableTime = result.get("NODE_TIME_TYPE");                   //获取表里面的日期时间型字段

        //给日期型加一个字段visit_date，不用在配置表中为每一个表都添加visit_date这个字段
        if( StringUtils.isEmpty(fieldTableTime) ){
            fieldTableTime ="visit_date";
        }else {
            fieldTableTime += ",visit_date";
        }

        //新增需求:对于住院相关表的 psn_no, mdtrt_id，setl_id 字段，需要通过 patinet_id + visit_id 查询得到
            String insurFileds = result.get("NEED_LOOKUP_FIELDS");            //查询数据表里面医保相关字段
            String[] list = null;
            if(insurFileds != null && !insurFileds.isEmpty())
                list = insurFileds.split(",");
            if( list != null && list.length != 0  ){
                String patinet_id = map.get("patient_id").toString();
                String visit_id = map.get("visit_id").toString();
                Map<String,String>  insurFieldMap=participateMapper.getInsurField(patinet_id,visit_id); //查询得到两个字段的值并写入数据
                    for (String key : list) {
                        map.put(key.toLowerCase(), insurFieldMap.get(key.toUpperCase()));
                    }
            }

        //为每一个字段值加上单引号,因为数据库里面字符类型必须用单引号，数值型加上单引号也会解析为数值
        for(String key : map.keySet()){
            String value = map.get(key).toString();
            map.put(key,"'"+value+"'");
        }

        //数据里面添加上主动生成值的两个字段
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmsss");
        String dateTime = sdf.format(new Date());
        map.put("id","sys_guid()");
        map.put("serial_no","'"+dateTime+"'");

        //处理日期类型字段的格式转换
        if(!StringUtils.isEmpty(fieldTableTime)){
            String[] fieldTableTimes=fieldTableTime.split(",");
            for(String ftt:fieldTableTimes){
                String timeStr = (String)map.get(ftt);
                if(!StringUtils.isEmpty(timeStr)){
                    map.put(ftt,"to_date('"+ timeStr.substring(1,timeStr.length()-1)+"','yyyy-mm-dd hh24:mi:ss')");
                }
            }
        }

        //下面三句话，动态封装表名和数据
        Map<String,Object> columnMap = new HashMap<>();
        columnMap.put("tableNum",fieldTableName);                   //表名
        columnMap.put("columnMap",map);                             //数据

        //执行插入数据操作
        try {
                int i = participateMapper.insertTB1(columnMap);
                logger.info("sql执行返回值"+i);
            }catch (Exception e){
                String errorMessage = e.getCause().getMessage();    //截取错误提示字符串
                logger.error("错误信息："+errorMessage);
                 return new ApiResultVo<String>(errorMessage);      //发生异常返回的错误信息提示
        }

        ApiResultVo<String> arv = new ApiResultVo<>();              //调用执行成功
        if(interfaceCode.equals("3101") && tableCode == 2){                           //对3101的调用会有返回值
            String trigScen = map.get("trig_scen").toString();
            String rv = uploadService.detailAuditBefore(dateTime, trigScen);
            arv.setData(rv);
        }

        return arv;

    }


}
