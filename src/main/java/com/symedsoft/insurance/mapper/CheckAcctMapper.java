package com.symedsoft.insurance.mapper;

import java.util.List;
import java.util.Map;

/**
 * 对账
 */
public interface CheckAcctMapper {

    List<Map<String , Object>> queryDetailDataByTime (Map<String , Object> param);

    Map<String , Object> queryCheckDetail (String serialNo);

    List<String> queryListSortFromConfig(String interfaceCode);
}
