package com.symedsoft.insurance.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.symedsoft.insurance.mapper.UploadTaskMapper;
import com.symedsoft.insurance.service.CollectionUploadService;
import com.symedsoft.insurance.service.InsuranceConfigService;
import com.symedsoft.insurance.utils.ReadDll;
import com.symedsoft.insurance.vo.CommonResponseVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CollectionUploadServiceImpl implements CollectionUploadService {

    private static final Logger logger = LoggerFactory.getLogger(CollectionUploadServiceImpl.class);

    @Autowired
    private UploadTaskMapper uploadTaskMapper;

    @Autowired
    private InsuranceConfigService insuranceConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runTask(String tableNameAndInterfaceCode) {
        String[] args = tableNameAndInterfaceCode.split(",");
        String tableName = args[0];
        String interfaceCode = args[1];
        logger.info("定时任务信息采集上传开始执行！执行的接口为 {},主表名为 {}" , interfaceCode , tableName);
        //查询待上传的信息
        List<String> serialNoList = uploadTaskMapper.querySerialNoByTableName(tableName , 0);
        logger.info("需要上传的新数据有 {} 条。" , serialNoList.size());

        //查询曾经上传失败的信息
        List<String> failSerialNoList = uploadTaskMapper.querySerialNoByTableName(tableName , 3);
        logger.info("需要上传的失败新数据有 {} 条。" , failSerialNoList.size());

        //将所有待上传的信息放在一起去重
        Set<String> serialNos = new HashSet<>(serialNoList.size() + failSerialNoList.size());
        serialNos.addAll(serialNoList);
        serialNos.addAll(failSerialNoList);

        if (serialNos.size() == 0) {
            logger.info("接口：{} 没有需要采集上传的信息！" , interfaceCode);
            return;
        }

        //修改状态为正在上传
        Map<String , Object> param = Maps.newHashMap();
        param.put("serialNos" , serialNos);
        param.put("multiline" , true);
        param.put("flag" , 1);
        param.put("tableName" , tableName);
        uploadTaskMapper.modifyStatusBySerialNo(param);

        //遍历执行解析报文，并执行上传
        for (String serialNo : serialNos) {
            logger.info("执行 serialNo 为 {} 的信息采集上传。" , serialNo);

            try  {
                // 1、查询入参信息，并调医保
                String inputJson = insuranceConfigService.getInputJsonStr(serialNo , interfaceCode, true);
                logger.info("接口返回上传入参为：" + inputJson);
                byte[] outchar = new byte[1024*8] ;
                int result = ReadDll.INSTANCE.BUSINESS_HANDLE(inputJson.getBytes("GBK") , outchar);
                String outStr = new String(outchar,"gbk");

                logger.info("接口返回上传出参为：" + outStr);

                if (result != 0){
                    param.put("serialNo" , serialNo);
                    param.put("multiline" , false);
                    param.put("flag" , 3);
                    uploadTaskMapper.modifyStatusBySerialNo(param);
                    continue;
                }

                // 2、解析返回报文
                CommonResponseVO response = JSONObject.parseObject(outStr , CommonResponseVO.class);
                if ("-1".equals(response.getInfCode())) {
                    param.put("serialNo" , serialNo);
                    param.put("multiline" , false);
                    param.put("flag" , 3);
                    uploadTaskMapper.modifyStatusBySerialNo(param);
                    continue;
                }

                if (StringUtils.isNotEmpty(response.getErr_msg())) {
                    param.put("serialNo" , serialNo);
                    param.put("multiline" , false);
                    param.put("flag" , 3);
                    uploadTaskMapper.modifyStatusBySerialNo(param);
                    continue;
                }

                Map <String , Object> ret = response.getOutput();
                if (ret != null && ret.size() > 0) {
                    insuranceConfigService.saveOutputStr(serialNo , interfaceCode , outStr , false);
                }

                param.put("serialNo" , serialNo);
                param.put("multiline" , false);
                param.put("flag" , 2);
                uploadTaskMapper.modifyStatusBySerialNo(param);
                logger.info("serialNo 为 {} 的信息采集上传成功！" , serialNo);

            } catch (Exception e) {
                param.put("serialNo" , serialNo);
                param.put("multiline" , false);
                param.put("flag" , 3);
                uploadTaskMapper.modifyStatusBySerialNo(param);
                logger.error("定时任务信息采集上传调用接口：{} 中的serialNo: {}发生异常，保存信息为：{}"
                        ,interfaceCode, serialNo, e);

            }
        }
    }

    @Override
    public String getParamByCode(String interfaceCode) {
        return uploadTaskMapper.queryParamByCode(interfaceCode);
    }
}
