package com.symedsoft.insurance.service;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * 下载 service
 *
 */
public interface CatalogService {

    /**
     * 调用医保接口，并解析医保接口的返回数据
     * @param param
     * @return
     */
    boolean doInsuranceAndParse (Map<String , Object> param) throws Exception;

    /**
     * 保存文件并将文件内容写入表中
     * @param param
     * @return
     */
    String saveFileIntoTable (Map<String , Object> param) throws Exception;



    /**
     * 测试共呢个
     * @return
     */
    String test() throws Exception;
}
