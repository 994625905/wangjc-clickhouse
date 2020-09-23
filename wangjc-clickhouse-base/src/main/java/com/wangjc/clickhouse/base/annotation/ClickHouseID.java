package com.wangjc.clickhouse.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明实体类中与表id（主键）对应的属性，区分普通列，并无法自增
 * @author wangjc
 * @title: ClickHouseID
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2310:46
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClickHouseID {

}
