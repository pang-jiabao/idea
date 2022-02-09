package com.symedsoft.insurance.common;

import com.symedsoft.insurance.service.AutoUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * @author yx
 * @version 1.0.0
 * @Description 定时任务类
 * @createTime 2021年05月19日 16:34:00
 */
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
//@EnableScheduling   // 2.开启定时任务
public class SaticScheduleTask {
//    @Autowired
//    AutoUploadService autoUploadService;
    //3.添加定时任务
//    @Scheduled(cron = "0/5 * * * * ?")
//    private void configureTasks() {
//        //System.err.println("执行静态定时任务时间: " + LocalDateTime.now());
//        autoUploadService.uploadInpFee();
//    }
}
