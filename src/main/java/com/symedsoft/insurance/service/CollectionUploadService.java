package com.symedsoft.insurance.service;

import java.util.List;
import java.util.Map;

public interface CollectionUploadService {


    /**
     * 执行任务
     * @return
     */
    void runTask (String interfaceCodeAndInterfaceCode);

    /**
     * 通过interfaceCode获取定时任务执行时所需入参
     * @param interfaceCode
     * @return
     */
    String getParamByCode(String interfaceCode);
}
