package com.symedsoft.insurance.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.symedsoft.insurance.config.RequestParamConfig;
import com.symedsoft.insurance.config.SignNoCache;
import com.symedsoft.insurance.mapper.DictMapper;
import com.symedsoft.insurance.mapper.InsuranceInterfaceMapper;
import com.symedsoft.insurance.service.DictService;
import com.symedsoft.insurance.service.SignService;
import com.symedsoft.insurance.utils.MapUtils;
import com.symedsoft.insurance.utils.ReadDll;
import com.symedsoft.insurance.vo.CommonRequestVO;
import com.symedsoft.insurance.vo.CommonResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictServiceImpl implements DictService {
    private static final Logger logger = LoggerFactory.getLogger(DictServiceImpl.class);

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private SignService signService;

    @Autowired
    private RequestParamConfig paramConfig;

    @Autowired
    private InsuranceInterfaceMapper interfaceMapper;

    @Override
    @Transactional
    public boolean refresh() throws Exception {
        logger.info("*************开始刷新字典*********");
        // 1、查询 需要更新的字典项
        List<Map<String , String>> dictItemList = dictMapper.queryDictItem();
        logger.info("*************刷新字典数量"+dictItemList.size()+"条*********");
        // 2、循环调用接口查询字典数据
        for (Map<String , String> dictItem : dictItemList) {
            String seq = interfaceMapper.getMsgIdSequence();
            CommonRequestVO request = new CommonRequestVO(paramConfig , seq);
            HashMap<String,Object> input = new HashMap<>();
            Map<String , Object> param = Maps.newHashMap();
            param.put("type" , dictItem.get("dictName"));
//            param.put("type" , "list_attr_code");

            String date = DateUtil.format(new Date(),"yyyy-MM-dd");
            param.put("admdvs" , paramConfig.getMdtrtarea_admvs());
            param.put("valiFlag" , "1");
            param.put("vali_flag" , "1");
            param.put("date" , date);
            input.put("data" , param);

            request.setRecer_sys_code("YBXT");
            request.setInfno("1901");
            request.setSign_no(signService.getSignNo(paramConfig.getOperateNo()));
            //request.setInf_time(new Date());
            request.setInput(input);

            String inputStr = JSONObject.toJSONString(request, SerializerFeature.WriteMapNullValue,
                    SerializerFeature.WriteNullStringAsEmpty,SerializerFeature.WriteNullNumberAsZero);

            byte[] outputchar = new byte[1024*1024];
            logger.info("1901********调用开始**************");
            logger.info("1901**入参"+inputStr);
            int res = ReadDll.INSTANCE.BUSINESS_HANDLE(inputStr.getBytes("GBK"), outputchar);
            String outputStr = null;
            outputStr = new String(outputchar,"gbk");
            logger.info("1901**出参"+outputStr.trim());
            logger.info("1901********调用完成**************");
            if (res < 0) {
                return false;
            }

            CommonResponseVO response = JSONObject.parseObject(outputStr , CommonResponseVO.class);
            if (!"0".equals(response.getInfCode())) {
                return false;
            }
            Map <String , Object> ret = response.getOutput();
            List<Map<String , Object>> retList = (List<Map<String , Object>>) ret.get("list");
            if (retList == null || retList.size() == 0) {
                continue;
            }
            ret.put("dictDesc" , dictItem.get("dictDesc"));
//            List<Map<String , Object>> list = (List<Map<String , Object>>) ret.get("list");
            logger.info("字典结果集有值，值为：{}" , dictItem.get("dictDesc"));

            // 3、删除字典原数据
            dictMapper.delDict(dictItem.get("dictName"));
            logger.info("删除字典{}" , dictItem.get("dictName"));


            // 4、批量插入字典数据
            boolean flag = dictMapper.addBatchDict(ret);
            logger.info("插入字典{}" , dictItem.get("dictName"));



            if (!flag) {
                logger.info("插入字典{}失败！" , dictItem.get("dictName"));
                return false;
            }
        }
        dictMapper.mergeDict();
        return true;
    }

    @Override
    public String queryDictCodeByItem(String dictName, String itemName) {
        return null;
    }

    @Override
    public List<Map<String, String>> queryDictCodeByName(String dictName) {
        return null;
    }

    @Override
    public String queryItemNameByItemCode(String dictName, String itemCode) {
        return null;
    }
}
