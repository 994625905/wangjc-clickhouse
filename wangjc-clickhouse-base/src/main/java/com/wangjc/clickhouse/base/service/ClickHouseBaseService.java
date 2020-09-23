package com.wangjc.clickhouse.base.service;

import com.wangjc.clickhouse.base.dao.ClickHouseBaseDao;
import com.wangjc.clickhouse.base.entity.ClickHouseBaseEntity;

import java.util.List;
import java.util.Map;

/**
 * service：基类，提供可直接调用的方法。
 * @author wangjc
 * @title: ClickHouseBaseService
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2311:06
 */
public interface ClickHouseBaseService<T extends ClickHouseBaseEntity> {

    /**
     * 虽然不允许直接在service中写SQL语句，但我还是提供基础的baseDao，
     * 测试的时候可以用到，有的时候也考虑到简洁方便，
     * @return
     */
    ClickHouseBaseDao<T> getBaseDao();

    /**
     * 批量新增
     * @param data
     * @return
     */
    int batchInsert(List<T> data);

    /**
     * 新增
     * @param entity
     * @return
     */
    int insert(T entity);

    /**
     * 根据构造的对象，以ID为条件来修改
     * @param entity：只针对注解声明的属性
     * @return
     */
    int updateById(T entity);

    /**
     * 根据ID删除，要获取被ID注解声明的字段名称
     * @param id
     * @return
     */
    int deleteById(Object id);

    /**
     * 获取当前实体类对应表的所有数据，并排序
     * @param orderByFieldAndIsAsc：eg：field asc/desc，可为null
     * @return
     */
    List<T> selectAllOrderBy(String orderByFieldAndIsAsc);

    /**
     * 以entity构建条件查询结果集，并按条件排序
     * @param entity
     * @param orderByFieldIsAsc：eg：field asc/desc，可为null
     * @return
     */
    List<T> selectList(T entity,String orderByFieldIsAsc);

    /**
     * 分页查询，构造条件，排序
     * @param start
     * @param size
     * @param entity：构造条件，可为null
     * @param orderByFieldAndIsAsc：eg：field asc/desc，可为null
     * @return: 返回结果为 {rows：查询的数据[……]，total：总数条目}
     */
    Map<String,Object> selectPage(int start, int size, T entity, String orderByFieldAndIsAsc);

}
