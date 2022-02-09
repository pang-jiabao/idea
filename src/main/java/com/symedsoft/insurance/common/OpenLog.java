package com.symedsoft.insurance.common;

/*
 *@author：LL
 *@Date:2021/4/20
 *@Description
 */

import java.lang.annotation.*;

@Target(ElementType.METHOD) //注解放置的目标位置,METHOD是可注解在方法级别上
@Retention(RetentionPolicy.RUNTIME) //注解在哪个阶段执行
@Documented
public @interface OpenLog {
}
