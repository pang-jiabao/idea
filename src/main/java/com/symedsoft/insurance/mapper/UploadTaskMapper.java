package com.symedsoft.insurance.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface UploadTaskMapper {

    /**
     * 查询目标表中符合条件的SerialNo
     * @return
     */
    List<String> querySerialNoByTableName(@Param("tableName")String tableName , @Param("isFlag") int isFlag);

    /**
     * 根据 SerialNo 修改上传状态
     * @param param
     * @return
     */
    int modifyStatusBySerialNo(Map<String , Object> param);

    /**
     * 通过interfaceCode获取定时任务执行时所需入参
     * @param queryParamByCode
     * @return
     */
    String queryParamByCode(String queryParamByCode);
}
