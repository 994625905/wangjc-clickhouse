# wangjc-clickhouse
过亿数据的秒级查询，不得不爱的clickhouse

### 前言
- 由于项目中的MySQL性能早已达到瓶颈，且运营那边的需求一直在不断的冲击着这个上限，一张单天能产生2000万数据的表，需要在自定义的跨日期区间内，分组，统计，汇总，分析……操作，且涉及varchar（30）的关键词字段，SQL语句的优化已调至极限，依旧无法做出满意的响应效率，于是引入了ClickHouse。

------------
#### Clickhouse的简介
- 由战斗民族2016年开源的，为了迎合IOT时代大量数据的统计分析工作，强化查询性能，以广泛应用在国内的大厂中（包括但不限于腾讯、今日头条、携程、快手……集群规模达上千节点），阿里云提供了云产品ClickHouse。但由于诞生时间晚，仍然处于不断的快速更新迭代中，我并未找到springboot有提供相关支持的jar，于是采用官方给出的clickhouse-jdbc，用它提供的数据源，自己封装了一套通用型的clickhouse-base。

#### 为什么clickhouse可以秒查
- 1、食妖蛊系列，有多少CPU，就是吃多少资源，所以clickhouse能一直保持性能；
- 2、不支持事务，不存在隔离级别。官方给出的定位是分析型数据库，与传统的关系型数据库不一样，没有严格的逻辑约束。统计数据的意义在于用大量的数据看规律，看趋势，而不是100%准确；
- 3、IO方面，MySQL是行存储，ClickHouse是列存储，后者在count()这类操作天然有优势，同时，在IO方面，MySQL需要大量随机IO，ClickHouse基本是顺序IO；
- 4、多用于大数据统计分析场景中，绝大多数请求都是用于读访问，写入基本都是另外单独线程批量或者定时执行的。
