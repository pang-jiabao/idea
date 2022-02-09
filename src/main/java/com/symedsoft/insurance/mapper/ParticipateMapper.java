package com.symedsoft.insurance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface ParticipateMapper {

    //封装了表名和数据
    int insertTB1(Map<String,Object> columnMap);

    //获取表名,日期，日期时间
    HashMap<String,String> getTableFields(@Param("interfaceCode") String interfaceCode, @Param("tableCode") Integer tableCode);


    //查询插入通用数据表的医保参数
    HashMap<String,String> getInsurField(@Param("patient_id") String patient_id, @Param("visit_id") String visit_id);

  
}
