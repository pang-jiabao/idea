package com.symedsoft.insurance.mapper;

import java.util.List;
import java.util.Map;

/**
 * 字典服务
 */
public interface DictMapper {

    /**
     * 查询所有字典项
     * @return
     */
    List<Map<String , String>> queryDictItem();

    /**
     * 删除字典表中所有数据
     * @return
     */
    boolean delDict(String dictName);

    /**
     *批量插入字典数据
     * @param list
     * @return
     */
    boolean addBatchDict (Map<String , Object> list);

    /**
     * 根据 字典名 和 项名 查询 字典代码
     * @param param
     * @return
     */
    String queryDictCodeByItem(Map <String , String> param);

    /**
     * 通过 字典名 查询 对应的所有字典代码
     * @param dictName
     * @return
     */
    List<Map<String , String>> queryDictCodeByName(String dictName);

    /**
     * 根据 item_code 和 dict_name 查询 item_name
     * @param dictName
     * @param itemCode
     * @return
     */
    String queryItemNameByItemCode (String dictName , String itemCode);

    int mergeDict();
}
