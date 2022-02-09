package com.symedsoft.insurance.task;

import com.symedsoft.insurance.mapper.ScheduleMapper;
import com.symedsoft.insurance.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author yx
 * @version 1.0.0
 * @Description
 * @createTime 2020年11月24日 17:14:00
 */
@Service
public class SysJobRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(SysJobRunner.class);

    @Autowired
    private ScheduleMapper mapper;

    @Autowired
    private CronTaskRegistrar cronTaskRegistrar;

    @Override
    public void run(String... args) {
        System.err.println("线程启动");
        // 初始加载数据库里状态为正常的定时任务
        List<Map<String , Object>> list = mapper.queryTaskTable(1);
        if (list == null || list.size() == 0) {
            logger.info("没有需要执行的任务。");
            return;
        }

        for (Map<String , Object> map : list) {
            String beanName = MapUtils.getObject2String(map, "beanName");
            String methodName = MapUtils.getObject2String(map, "methodName");
            String methodParams = MapUtils.getObject2String(map, "methodParams");
            String cronExpression = MapUtils.getObject2String(map, "cronExpression");
            SchedulingRunnable task = new SchedulingRunnable(beanName, methodName, methodParams);
            cronTaskRegistrar.addCronTask(task, cronExpression);
        }
        System.err.println("定时任务已加载完毕...");
    }
}
