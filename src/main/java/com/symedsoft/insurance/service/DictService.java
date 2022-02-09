package com.symedsoft.insurance.service;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public interface DictService {

    /**
     * 根据insur_dict_item 配置表 刷新 字典数据 <br/>
     * 先删除数据，然后再遍历 配置表，写 字典数据
     * @return
     */
    boolean refresh () throws Exception;

    /**
     * 根据 字典名 和 项名 查询 字典代码
     * @param dictName
     * @param itemName
     * @return
     */
    String queryDictCodeByItem(String dictName , String itemName) ;

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
}
