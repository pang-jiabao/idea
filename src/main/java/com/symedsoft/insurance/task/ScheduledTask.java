package com.symedsoft.insurance.task;

import java.util.concurrent.ScheduledFuture;

/**
 * @author yx
 * @version 1.0.0
 * @Description
 * @createTime 2020年11月24日 17:06:00
 */

public final class ScheduledTask {

    volatile ScheduledFuture<?> future;

    /**
     * 取消定时任务
     */
    public void cancel() {
        ScheduledFuture<?> future = this.future;
        if (future != null) {
            future.cancel(true);
        }
    }
}
