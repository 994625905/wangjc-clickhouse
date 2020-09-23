package com.wangjc.clickhouse.base.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import ru.yandex.clickhouse.BalancedClickhouseDataSource;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author wangjc
 * @title: ClickHouseConfig
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2310:48
 */
public class ClickHouseConfig {

    private final static Logger logger = LoggerFactory.getLogger(ClickHouseConfig.class);

    private static ClickHouseProperties clickHouseProperties;
    private static DataSource dataSource;

    /** 初始化 */
    static{
        try {
            /** 加载配置文件 */
            Resource resource = new ClassPathResource("application.properties");
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);

            /** 设置clickHouse连接属性 */
            clickHouseProperties = new ClickHouseProperties();
            clickHouseProperties.setUser(properties.getProperty("clickhouse.user"));
            clickHouseProperties.setPassword(properties.getProperty("clickhouse.password"));
            clickHouseProperties.setDatabase(properties.getProperty("clickhouse.database"));

            //集群地址，以逗号隔开；"jdbc:clickhouse://127.0.0.1:8121,127.0.0.1:8122,127.0.0.1:8121,127.0.0.1:8122";
            String address = properties.getProperty("clickhouse.address");
            logger.debug("clickHouse初始化数据源地址[{}]",address);

            if(address.indexOf(",") > 0){
                dataSource = new BalancedClickhouseDataSource(address,clickHouseProperties);//集群
            }else{
                dataSource = new ClickHouseDataSource(address,clickHouseProperties);//单机
            }

            if(dataSource != null){
                logger.debug("clickHouse数据源初始化成功");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 获取数据源
     * @return
     */
    public static DataSource getDataSource(){
        if(dataSource == null){
            logger.error("获取数据源失败，dataSource：[{}]",dataSource);
            throw new RuntimeException(" clickHouseDataSource is null ");
        }
        return dataSource;
    }

}
