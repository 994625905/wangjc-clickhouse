package com.wangjc.clickhouse.base.dao;

import com.wangjc.clickhouse.base.entity.ClickHouseBaseEntity;

import java.util.List;
import java.util.Map;

/**
 * dao层接口：基类，
 * @author wangjc
 * @title: ClickHouseBaseDao
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2311:00
 */
public interface ClickHouseBaseDao<T extends ClickHouseBaseEntity> {

    /**
     * 批量新增
     * @param data
     * @return
     */
    int batchInsert(List<T> data);

    /**
     * 根据构造的对象，以ID为条件来修改
     * @param entity：只针对注解声明的属性
     * @return
     */
    int updateById(T entity);

    /**
     * 指定SQL语句修改
     * @param sql
     * @return
     */
    int updateBySql(String sql);

    /**
     * 根据ID删除，要获取被ID注解声明的字段名称
     * @param id
     * @return
     */
    int deleteById(Object id);

    /**
     * 根据SQL语句删除
     * @param sql
     * @return
     */
    int deleteBySql(String sql);

    /**
     * 查询所有条目
     * @return
     */
    List<T> selectAll();

    /**
     * 获取当前实体类对应表的所有数据，并排序
     * @param orderByFieldAndIsAsc：eg：field asc/desc
     * @return
     */
    List<T> selectAllOrderBy(String orderByFieldAndIsAsc);

    /**
     * 以entity构建条件查询结果集
     * @param entity
     * @return
     */
    List<T> selectList(T entity);

    /**
     * 以entity构建条件查询结果集，并按条件排序
     * @param entity
     * @param orderByFieldIsAsc：eg：field asc/desc
     * @return
     */
    List<T> selectList(T entity,String orderByFieldIsAsc);

    /**
     * 执行指定的SQL，返回结果集
     * @param sql
     * @return
     */
    List<Map<String,Object>> selectBySql(String sql);

    /**
     * 分页查询
     * @param start
     * @param size
     * @return
     */
    List<T> selectPage(int start,int size);

    /**
     * 分页查询，排序
     * @param start
     * @param size
     * @param orderByFieldAndIsAsc：eg：field asc/desc
     * @return
     */
    List<T> selectPage(int start,int size,String orderByFieldAndIsAsc);

    /**
     * 分页查询，构造条件
     * @param start
     * @param size
     * @param entity
     * @return
     */
    List<T> selectPage(int start,int size,T entity);

    /**
     * 分页查询，构造条件，排序
     * @param start
     * @param size
     * @param entity
     * @param orderByFieldAndIsAsc：eg：field asc/desc
     * @return
     */
    List<T> selectPage(int start,int size,T entity,String orderByFieldAndIsAsc);

    /**
     * 获取总数的条目
     * @return
     */
    int selectCountAll();

    /**
     * 构建条件，获取统计数量
     * @param entity
     * @return
     */
    int selectCount(T entity);

    /**
     * 指定SQL的数据条目
     * @param sql
     * @return
     */
    int selectCount(String sql);

}
