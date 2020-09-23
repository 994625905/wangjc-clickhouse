package com.wangjc.clickhouse.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * clickHouse表名注解，声明实体
 * @author wangjc
 * @title: ClickHouseTable
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2310:47
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClickHouseTable {

    /**
     * 名称
     * @return
     */
    String name() default "";

}
