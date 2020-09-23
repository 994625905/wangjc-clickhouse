package com.wangjc.clickhouse.base.dao.impl;

import cn.hutool.core.date.DateUtil;
import com.wangjc.clickhouse.base.annotation.ClickHouseColumn;
import com.wangjc.clickhouse.base.annotation.ClickHouseID;
import com.wangjc.clickhouse.base.annotation.ClickHouseTable;
import com.wangjc.clickhouse.base.config.ClickHouseConfig;
import com.wangjc.clickhouse.base.dao.ClickHouseBaseDao;
import com.wangjc.clickhouse.base.entity.ClickHouseBaseEntity;
import com.wangjc.clickhouse.base.util.ClickHouseBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * dao层实现类，基类，封装单表的增删改查，支持使用原生SQL丰富操作
 * @author wangjc
 * @title: ClickHouseBaseDaoImpl
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2311:01
 */
public class ClickHouseBaseDaoImpl<T extends ClickHouseBaseEntity> implements ClickHouseBaseDao<T> {

    private static final Logger logger = LoggerFactory.getLogger(ClickHouseBaseDaoImpl.class);//采集每一次执行的SQL日志，方便单独调试

    private Class<T> tClass;// 当前实体类对应泛型
    private Field[] allFieldList;// 当前实体类属性数组
    private Map<String,Field> allFieldMap;// key-->字段名（数据表）, value-->属性名（实体类）
    private Lock lock = new ReentrantLock();// 可重入锁

    private static final String CONDITION_EQUAL = "equal";// 相等条件
    private static final String CONDITION_LIKE = "like";// 匹配条件

    private Connection conn;//数据库连接

    /**
     * 获取connection，关闭自动提交，其实clickhouse不支持回滚的
     * 但我还是这样做了（处理成功commit，异常捕获rollback），假如后面新的版本又支持了呢。。
     * @return
     */
    private Connection getConnection(){
        if(ClickHouseConfig.getDataSource() == null){
            logger.error("获取连接池失败，dataSource：[{}]", ClickHouseConfig.getDataSource());
            throw new RuntimeException(" clickHouseDataSource is null ");
        }
        try {
            lock.lock();
            if( conn == null || conn.isClosed() ) {
                conn = ClickHouseConfig.getDataSource().getConnection();
                conn.setAutoCommit(false);//关闭自动提交，有异常直接回滚
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return conn;
    }

    /**
     * 获取T的class实例
     * @return
     */
    private Class<T> getTClass(){
        if(tClass == null){
            lock.lock();
            try {
                if(tClass == null){
                    tClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
                }
            }finally {
                lock.unlock();
            }
        }
        return tClass;
    }

    /**
     * 获取当前T所有的属性数组
     * @return
     */
    private Field[] getAllFieldList(){
        if(allFieldList == null){
            lock.lock();
            try {
                if(allFieldList == null){
                    allFieldList = this.getTClass().getDeclaredFields();
                }
            }finally {
                lock.unlock();
            }
        }
        return allFieldList;
    }

    /**
     * 获取map，key为表字段名，value为实体类属性名
     * @return
     */
    private Map<String,Field> getAllFieldMap(){
        if(allFieldMap == null){
            lock.lock();
            try {
                Field[] fieldList = getAllFieldList();
                allFieldMap = new HashMap<>(fieldList.length);
                for(Field field:fieldList){
                    ClickHouseColumn column = field.getAnnotation(ClickHouseColumn.class);
                    allFieldMap.put(column.name(),field);
                }
            }finally {
                lock.unlock();
            }
        }
        return allFieldMap;
    }

    /**
     * 获取当前实体类对应的表名
     * @return
     */
    private String getTableName(){
        ClickHouseTable table = this.getTClass().getAnnotation(ClickHouseTable.class);
        if(table == null){
            throw new RuntimeException(" no Annotation 'javax.persistence.Table' in clazz  ");
        }
        return table.name();
    }

    /**
     * 反射通过getter获取值
     * @param fieldName
     * @param t
     * @return
     */
    private Object getMethodGetValue(String fieldName,T t){
        try {
            return t.getClass().getMethod("get"+ ClickHouseBaseUtil.capitalize(fieldName)).invoke(t);
        } catch (Exception e) {
            e.printStackTrace();
            // 把这个异常转为运行时异常再抛出
            throw new RuntimeException("tClazz=" + t.getClass().getName(), e);
        }
    }

    /**
     * 反射通过setter设置值
     * @param fieldName
     * @param t
     * @param value
     * @param valueType
     */
    private void setMethodSetValue(String fieldName,T t,Object value,Class<?> valueType){
        try {
            Method method = t.getClass().getMethod("set"+ClickHouseBaseUtil.capitalize(fieldName),valueType);
            method.invoke(t,valueType.cast(value));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("tClazz=" + t.getClass().getName(), e);
        }
    }

    /**
     * 执行查询语句
     * @param sql
     * @return
     */
    private ResultSet executeQuery(String sql){
        ResultSet resultSet = null;
        try {
            Statement statement = getConnection().createStatement();
            resultSet = statement.executeQuery(sql);
        }catch (Exception e){
            e.printStackTrace();
        }
        return resultSet;
    }

    /**
     * 预编译SQL
     * @param sql
     * @param values
     * @return
     */
    private PreparedStatement createPreparedStatement(String sql, Object[] values){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getConnection().prepareStatement(sql);
            for(int i=0;i<values.length;i++){
                preparedStatement.setObject(i+1,values[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return preparedStatement;
    }

    /**
     * 将ResultSet结果集装到List对象
     * @param resultSet
     * @return
     */
    private List<T> convertSetToEntity(ResultSet resultSet){
        List<T> result = new ArrayList<>();
        Map<String, Field> fieldMap = getAllFieldMap();
        try {
            while (resultSet.next()){
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                /** 通过反射创建装载条目的实体对象 */
                T instance = getTClass().newInstance();
                for(int i=1;i<=resultSetMetaData.getColumnCount();i++){
                    String columnName = resultSetMetaData.getColumnName( i );
                    if(fieldMap.containsKey(columnName)){
                        Field field = fieldMap.get(columnName);
                        this.setMethodSetValue(field.getName(),instance,resultSet.getObject(i),field.getType());// 设置值
                    }
                }
                result.add(instance);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 将resultSet结果集装到map
     * @param resultSet
     * @return
     */
    private List<Map<String,Object>> convertSetToMap(ResultSet resultSet){
        List<Map<String,Object>> result = new ArrayList<>();
        Map<String,Field> fieldMap = getAllFieldMap();
        try {
            while (resultSet.next()){
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                /** 创建装载条目的map */
                Map<String,Object> map = new HashMap<>(resultSetMetaData.getColumnCount());
                for(int i=1;i<=resultSetMetaData.getColumnCount();i++){
                    String columnName = resultSetMetaData.getColumnName(i);
                    if(fieldMap.containsKey(columnName)){
                        map.put(columnName,resultSet.getObject(i));// put值
                    }
                }
                result.add(map);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 批量新增
     * @param data
     * @return
     */
    @Override
    public int batchInsert(List<T> data){
        if(data == null || data.size() < 1){
            throw new RuntimeException(" data is null ");
        }
        // 构建插入语句sql，占位符value
        StringBuffer sql_insert = new StringBuffer(" insert into ").append(this.getTableName()).append("(");
        StringBuffer sql_value = new StringBuffer("(");

        // 获取类的所有字段，并逐一填充sql和value
        Field[] fieldList = this.getAllFieldList();
        for(Field field:fieldList){
            ClickHouseColumn column = field.getAnnotation(ClickHouseColumn.class);
            if(column != null){
                sql_insert.append(column.name()).append(",");
                sql_value.append("?").append(",");
            }
        }

        // 完成sql和value的构建
        sql_insert.deleteCharAt(sql_insert.length()-1).append(")");
        sql_value.deleteCharAt(sql_value.length()-1).append(")");
        sql_insert.append(" values ").append(sql_value);

        logger.info(sql_insert.toString());

        //开始批量插入
        Connection connection = getConnection();
        try {
            PreparedStatement pst = connection.prepareStatement(sql_insert.toString());
            for(T t:data){
                int index = 1;//preparedStatement的占位
                for(Field field:fieldList){
                    // 获取当前字段的值
                    Object getValue = this.getMethodGetValue(field.getName(), t);

                    // 日期格式的单独处理
                    if(field.getName().indexOf("java.util.Date") != -1){
                        ClickHouseColumn column = field.getAnnotation(ClickHouseColumn.class);
                        String columnType = column.format();
                        if(columnType == null || columnType == ""){
                            columnType = "yyyy-MM-dd HH:mm:ss";
                        }
                        if(getValue != null){
                            getValue = DateUtil.format((java.util.Date) getValue,columnType);
                        }else{
                            getValue = DateUtil.format(new Date(),columnType);//默认当前时间
                        }
                    }
                    pst.setObject(index,getValue);
                    index++;
                }
                pst.addBatch();// 添加到批次
            }
            int[] res = pst.executeBatch();// 提交批处理
            connection.commit();// 执行
            logger.info("新增结果：[{}]",res);
            return res.length;
        } catch (Exception e) {
            try {
                connection.rollback();//异常回滚
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 不同于普通SQL的修改和删除，alter table [tableName] update……where……
     * 根据构造的对象，以ID为条件来修改
     * @param entity：只针对注解声明的属性
     * @return
     */
    @Override
    public int updateById(T entity){
        if(entity == null){
            throw new RuntimeException(" entity is null ");
        }
        // 构建修改语句sql
        StringBuffer sql = new StringBuffer("alter table ").append(this.getTableName()).append(" update ");
        String where = "";

        // 获取类的所有字段，并逐一填充sql，只针对注解声明的属性,
        Field[] fieldList = this.getAllFieldList();
        List<Object> setValue = new ArrayList<>();
        for(Field field:fieldList){
            ClickHouseColumn column = field.getAnnotation(ClickHouseColumn.class);
            if(column != null){
                Object getValue = this.getMethodGetValue(field.getName(), entity);//反射获取到属性的值

                // ID条件判断
                ClickHouseID id = field.getAnnotation(ClickHouseID.class);
                if(id != null){
                    where = " where "+column.name()+" = "+getValue;
                }else{
                    sql.append(column.name()).append(" = ?,");
                    setValue.add(getValue);
                }
            }
        }

        // 完成SQL的构建
        sql.deleteCharAt(sql.length()-1).append(where);

        logger.info(sql.toString());

        // 开始修改
        Connection connection = getConnection();
        try {
            PreparedStatement pst = connection.prepareStatement(sql.toString());
            for(Object value:setValue){
                int index = 1;//preparedStatement的占位
                pst.setObject(index,value);
                index++;
            }
            int res = pst.executeUpdate();//提交修改
            connection.commit();// 执行
            logger.info("修改结果：[{}]",res);
            return res;
        } catch (Exception e) {
            try {
                connection.rollback();//异常回滚
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 指定的SQL语句修改，一般是范围修改
     * @param sql
     * @return
     */
    @Override
    public int updateBySql(String sql){
        logger.info(sql);

        Connection connection = getConnection();
        try {
            Statement statement = getConnection().createStatement();
            int update = statement.executeUpdate(sql);
            connection.commit();// 执行
            return update;
        } catch (Exception e) {
            try {
                connection.rollback();//异常回滚
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 不同于普通SQL的修改和删除，alter table [tableName] delete where……
     * 根据ID删除，要获取被ID注解声明的字段名称
     * @param id
     * @return
     */
    @Override
    public int deleteById(Object id){
        String columnID = "";//字段ID
        Field[] fieldList = this.getAllFieldList();
        for(Field field:fieldList){
            ClickHouseID annotationID = field.getAnnotation(ClickHouseID.class);
            if(annotationID != null){
                ClickHouseColumn annotationColumn = field.getAnnotation(ClickHouseColumn.class);
                columnID = annotationColumn.name();
            }
        }
        String sql = "alter table "+this.getTableName()+" delete where "+columnID+" = ?";

        logger.info(sql);

        Connection connection = getConnection();
        try {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setObject(1,id);
            int res = pst.executeUpdate();//提交删除
            connection.commit();// 执行
            return res;
        } catch (Exception e) {
            try {
                connection.rollback();//异常回滚
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 根据SQL语句来删除
     * @param sql
     * @return
     */
    @Override
    public int deleteBySql(String sql){
        logger.info(sql);

        Connection connection = getConnection();
        try {
            Statement statement = getConnection().createStatement();
            int update = statement.executeUpdate(sql);
            connection.commit();// 执行
            return update;
        } catch (Exception e) {
            try {
                connection.rollback();//异常回滚
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取当前实体类对应表的所有数据，使用前请注意表的大小
     * @return
     */
    @Override
    public List<T> selectAll(){
        String sql = "select * from "+getTableName();

        logger.info(sql);

        ResultSet resultSet = this.executeQuery(sql);
        return this.convertSetToEntity(resultSet);
    }

    /**
     * 获取当前实体类对应表的所有数据，并排序
     * @param orderByFieldAndIsAsc：eg：field asc/desc
     * @return
     */
    @Override
    public List<T> selectAllOrderBy(String orderByFieldAndIsAsc){
        if(orderByFieldAndIsAsc == null || orderByFieldAndIsAsc == ""){
            return this.selectAll();
        }
        String sql = "select * from "+getTableName()+" order by "+orderByFieldAndIsAsc;

        logger.info(sql);

        ResultSet resultSet = this.executeQuery(sql);
        return this.convertSetToEntity(resultSet);
    }

    /**
     * 以entity构建条件查询结果集
     * @param entity
     * @return
     */
    @Override
    public List<T> selectList(T entity){
        return this.selectList(entity,null);
    }

    /**
     * 以entity构建条件查询结果集，并按条件排序
     * @param entity
     * @param orderByFieldIsAsc：eg：field asc/desc
     * @return
     */
    @Override
    public List<T> selectList(T entity,String orderByFieldIsAsc){
        if(entity == null){
            return this.selectAllOrderBy(orderByFieldIsAsc);
        }
        // 获取构建条件实体
        ConditionEntity conditionEntity = this.getWhereSqlAndValue(entity, CONDITION_EQUAL);

        StringBuffer sql = new StringBuffer("select * from ").append(this.getTableName()).append(" where ").append(conditionEntity.getSql());
        if(orderByFieldIsAsc != null && orderByFieldIsAsc != ""){
            sql.append(" order by ").append(orderByFieldIsAsc);
        }

        logger.info(sql.toString());

        try {
            PreparedStatement preparedStatement = this.createPreparedStatement(sql.toString(), conditionEntity.getValues());
            ResultSet resultSet = preparedStatement.executeQuery();
            return convertSetToEntity(resultSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<T>(0);//直接返回空list，防止NullPointException
    }

    /**
     * 分页查询
     * @param start
     * @param size
     * @return
     */
    @Override
    public List<T> selectPage(int start,int size){
        String sql = "select * from "+this.getTableName()+" limit "+start+","+size;

        logger.info(sql);

        ResultSet resultSet = executeQuery( sql);
        return convertSetToEntity(resultSet);
    }

    /**
     * 分页查询，排序
     * @param start
     * @param size
     * @param orderByFieldAndIsAsc：eg：field asc/desc
     * @return
     */
    @Override
    public List<T> selectPage(int start,int size,String orderByFieldAndIsAsc){
        if(orderByFieldAndIsAsc == null || orderByFieldAndIsAsc == ""){
            return this.selectPage(start,size);
        }
        String sql = "select * from "+this.getTableName()+" order by "+orderByFieldAndIsAsc+" limit "+start+","+size;

        logger.info(sql);

        ResultSet resultSet = executeQuery( sql);
        return convertSetToEntity(resultSet);
    }

    /**
     * 分页查询，构造条件
     * @param start
     * @param size
     * @param entity
     * @return
     */
    @Override
    public List<T> selectPage(int start,int size,T entity){
        return this.selectPage(start,size,entity,null);
    }

    /**
     * 分页查询，构造条件，排序
     * @param start
     * @param size
     * @param entity
     * @param orderByFieldAndIsAsc：eg：field asc/desc
     * @return
     */
    @Override
    public List<T> selectPage(int start,int size,T entity,String orderByFieldAndIsAsc){
        if(entity == null){
            return this.selectPage(start,size,orderByFieldAndIsAsc);
        }
        // 构造条件对象
        ConditionEntity conditionEntity = this.getWhereSqlAndValue(entity, CONDITION_EQUAL);

        // 构造SQL语句
        StringBuffer sql = new StringBuffer("select * from ").append(this.getTableName());
        sql.append(" where ").append(conditionEntity.getSql());
        if(orderByFieldAndIsAsc != null && orderByFieldAndIsAsc != ""){
            sql.append(" order by ").append(orderByFieldAndIsAsc);
        }
        sql.append(" limit ").append(start).append(",").append(size);

        logger.info(sql.toString());

        try {
            PreparedStatement preparedStatement = this.createPreparedStatement(sql.toString(), conditionEntity.getValues());
            ResultSet resultSet = preparedStatement.executeQuery();
            return this.convertSetToEntity(resultSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<T>(0);//直接返回空list，防止NullPointException
    }

    /**
     * 获取总数的条目
     * @return
     */
    @Override
    public int selectCountAll(){
        String sql = "select count(*) as count from "+this.getTableName();

        logger.info(sql);

        ResultSet resultSet = this.executeQuery(sql);
        try {
            if(resultSet.next()){
                return resultSet.getInt("count");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 构建条件，获取统计数量
     * @param entity
     * @return
     */
    @Override
    public int selectCount(T entity){
        if(entity == null){
            return this.selectCountAll();
        }
        // 构建条件对象
        ConditionEntity conditionEntity = this.getWhereSqlAndValue(entity, CONDITION_EQUAL);

        // 构建SQL
        StringBuffer sql = new StringBuffer("select count(*) as count from ").append(this.getTableName());
        sql.append(" where ").append(conditionEntity.getSql());

        logger.info(sql.toString());

        try {
            PreparedStatement preparedStatement = this.createPreparedStatement(sql.toString(), conditionEntity.getValues());
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return resultSet.getInt("count");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int selectCount(String sql) {
        sql = "select count(t.*) as count from ("+sql+") t";//手动构造统计

        logger.info(sql);

        ResultSet resultSet = this.executeQuery(sql);
        if(resultSet != null){
            try {
                if(resultSet.next()){
                    return resultSet.getInt("count");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * 执行指定的SQL，返回结果集
     * @param sql
     * @return
     */
    public List<Map<String,Object>> selectBySql(String sql){
        List<Map<String,Object>> list = new ArrayList<>();

        logger.info(sql);

        ResultSet resultSet = this.executeQuery(sql);
        if(resultSet != null){
            try {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                while (resultSet.next()){
                    Map<String,Object> map = new HashMap<>(resultSetMetaData.getColumnCount());
                    for(int i=1;i<= resultSetMetaData.getColumnCount();i++){
                        map.put(resultSetMetaData.getColumnName(i),resultSet.getObject(i));
                    }
                    list.add(map);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 根据传递的对象，构建查询条件
     * @param entity
     * @param condition
     * @return
     */
    private ConditionEntity getWhereSqlAndValue(T entity,String condition){
        // 获取所有字段的属性
        Field[] fieldList = this.getAllFieldList();

        // 创建一些容器用于后续计算,条件SQL，条件值,
        StringBuffer whereSql = new StringBuffer();
        List<Object> whereValue = new ArrayList<>();

        // 只针对column注解的列做处理，仅对内容不为null的属性生效
        for(Field field:fieldList){
            ClickHouseColumn column = field.getAnnotation(ClickHouseColumn.class);
            if(column != null){
                Object getValue = this.getMethodGetValue(field.getName(), entity);
                if(getValue != null){
                    if(CONDITION_EQUAL.equals(condition)){
                        whereSql.append(" "+column.name()+" = ? and");
                        whereValue.add(getValue);
                    }else if(CONDITION_LIKE.equals(condition)){
                        whereSql.append(" "+column.name()+" like ? and");
                        whereValue.add("%"+getValue+"%");
                    }
                }
            }
        }
        // 获取最终的SQL
        String sql = whereSql.toString();
        if(sql.endsWith("and")){
            sql = sql.substring(0, (sql.length() - 3));
        }
        // 返回结果
        return new ConditionEntity(sql,whereValue.toArray());
    }

    /**
     * 构建查询条件的实体类
     */
    private class ConditionEntity{
        private String sql;
        private Object[] values;

        public ConditionEntity(String sql,Object[] values){
            this.sql = sql;
            this.values = values;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public Object[] getValues() {
            return values;
        }

        public void setValues(Object[] values) {
            this.values = values;
        }
    }

}
