package com.symedsoft.insurance.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface File2TableMapper {

    /**
     * 批量插入 目录表
     * @param list
     * @return
     */
    int addDataIntoTable(@Param("recordList") List<List<String>> list, @Param("tableName") String tableName);

    int addDataIntoTableByOne(@Param("record") List<String> list, @Param("tableName") String tableName);

    /**
     * 查询对应 目录表 中的最大版本
     * @param tableName
     * @return
     */
    String queryVersionByTable(String tableName);

    /**
     * 修改版本号
     * @param version
     * @return
     */
    boolean modifyVerByTable(@Param("version") String version, @Param("tableName") String tableName);

    /**
     * 删除表数据
     * @return
     */
    int delAllTableData(@Param("tableName") String tableName,
                        @Param("primaryKey") String primaryKey,
                        @Param("ids") List<String> ids);

    /**
     * 根据主键删除数据
     * @param tableName
     * @param primaryKey
     * @param id
     * @return
     */
    int delTableDataById(@Param("tableName") String tableName,
                         @Param("primaryKey") String primaryKey,
                         @Param("id") String id);

    /**
     * 根据tableName查询表主键名
     * @param tableName
     * @return
     */
    String queryPrimaryKeyByTableName(String tableName);
}
