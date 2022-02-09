package com.symedsoft.insurance.controller;

import com.symedsoft.insurance.common.OpenLog;
import com.symedsoft.insurance.mapper.ParticipateMapper;
import com.symedsoft.insurance.service.impl.ParticipateServiceImpl;
import com.symedsoft.insurance.vo.ApiResultVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author pjb
 * @Description 数据表数据写入接口
 * @createTime 2021/12/27
 */
@RestController  //@Controller + @ResponseBody
@RequestMapping
public class ParticipateController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsuranceController.class);

    @Autowired
    private ParticipateServiceImpl participateService;


    @OpenLog
    @PostMapping(value = "/participate")
    public ApiResultVo participate(@RequestParam(defaultValue = "") String interfaceCode,
                              @RequestParam(defaultValue = "") Integer tableCode ,
                              @RequestParam(defaultValue = "") String data){
        //判断必要的三个参数
        if( StringUtils.isEmpty(interfaceCode)){
            return new ApiResultVo<String>("传入接口号interfaceCode为空");
        }else if( tableCode == null ){
            return new ApiResultVo<String>("传入数据表代码tableCode为空");
        }else  if( StringUtils.isEmpty(data) ){
            return new ApiResultVo<String>("传入数据data为空");
        }

        LOGGER.info(interfaceCode+"---"+tableCode+"--"+"数据写入服务处理开始--------------");
        LOGGER.info("--------传入数据:"+data+"------------");
        //调用服务处理
        ApiResultVo rv = participateService.insertService(interfaceCode, tableCode, data);
        LOGGER.info("-------数据写入服务处理结束结果:"+rv+"-----------");

        //输出操作结果
        return  rv;

    }
}
