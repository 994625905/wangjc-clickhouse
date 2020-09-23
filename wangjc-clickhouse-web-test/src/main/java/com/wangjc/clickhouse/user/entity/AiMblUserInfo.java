package com.wangjc.clickhouse.user.entity;

import com.wangjc.clickhouse.base.annotation.ClickHouseColumn;
import com.wangjc.clickhouse.base.annotation.ClickHouseID;
import com.wangjc.clickhouse.base.annotation.ClickHouseTable;
import com.wangjc.clickhouse.base.entity.ClickHouseBaseEntity;

/**
 * @author wangjc
 * @title: AiMblUserInfo
 * @projectName i4-data-platform
 * @description: TODO
 * @date 2020/9/2219:03
 */
@ClickHouseTable(name = "ai_mbl_user_info")
public class AiMblUserInfo extends ClickHouseBaseEntity {

    @ClickHouseColumn(name = "userid")
    @ClickHouseID
    private String userid;

    @ClickHouseColumn(name = "model")
    private String model;

    @ClickHouseColumn(name = "osversion")
    private String osversion;

    @ClickHouseColumn(name = "toolversion")
    private String toolversion;

    @ClickHouseColumn(name = "firsttime")
    private String firsttime;

    @ClickHouseColumn(name = "lasttime")
    private String lasttime;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOsversion() {
        return osversion;
    }

    public void setOsversion(String osversion) {
        this.osversion = osversion;
    }

    public String getToolversion() {
        return toolversion;
    }

    public void setToolversion(String toolversion) {
        this.toolversion = toolversion;
    }

    public String getFirsttime() {
        return firsttime;
    }

    public void setFirsttime(String firsttime) {
        this.firsttime = firsttime;
    }

    public String getLasttime() {
        return lasttime;
    }

    public void setLasttime(String lasttime) {
        this.lasttime = lasttime;
    }
}
