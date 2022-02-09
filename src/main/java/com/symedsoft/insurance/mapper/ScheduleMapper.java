package com.symedsoft.insurance.mapper;

import java.util.List;
import java.util.Map;

public interface ScheduleMapper {
    /**
     * 查询定时任务
     * @param jobStatus
     * @return
     */
    List<Map<String , Object>> queryTaskTable(int jobStatus);
}
