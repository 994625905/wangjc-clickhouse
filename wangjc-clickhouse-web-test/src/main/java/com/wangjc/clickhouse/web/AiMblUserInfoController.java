package com.wangjc.clickhouse.web;

import com.wangjc.clickhouse.user.entity.AiMblUserInfo;
import com.wangjc.clickhouse.user.service.AiMblUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author wangjc
 * @title: AiMblUserInfoController
 * @projectName wangjc-clickhouse
 * @description: TODO
 * @date 2020/9/2311:22
 */
@Controller
public class AiMblUserInfoController {

    @Autowired
    private AiMblUserInfoService aiMblUserInfoService;

    @RequestMapping(value = "/test01")
    @ResponseBody
    public Object test01(){
        Map<String, Object> map = aiMblUserInfoService.selectPage(0, 10, null, null);
        return map;
    }

    /**
     * 按列排序，
     * @return
     */
    @RequestMapping(value = "/test02")
    @ResponseBody
    public Object test02(){
        Map<String, Object> map = aiMblUserInfoService.selectPage(0, 10, null, " firsttime desc");
        return map;
    }

    /**
     * 条件查询
     * @return
     */
    @RequestMapping(value = "/select")
    @ResponseBody
    public Object test03(){
        AiMblUserInfo info = new AiMblUserInfo();
        info.setUserid("12340106f6c2334792e7573a39a0863b");
        List<AiMblUserInfo> list = aiMblUserInfoService.selectList(info,null);
        return list;
    }

    /**
     * 删除和更新一样，统一是：alter table [tableName] delete where
     * @return
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public Object delete(){
        int delete = aiMblUserInfoService.getBaseDao().deleteBySql("alter table ai_mbl_user_info delete where userid = '00001daf25112a565dcf1665e6f538f4'");
        return delete;
    }

    /**
     * 新增
     * @return
     */
    @RequestMapping(value = "/insert")
    @ResponseBody
    public Object insert(){
        AiMblUserInfo info = new AiMblUserInfo();
        info.setUserid("45670106f6c2334792e7573a39a0863b");
        info.setModel("测试");
        info.setOsversion("测试");
        info.setToolversion("ceshi");
        int insert = aiMblUserInfoService.insert(info);
        return insert;
    }

    /**
     * 删除和更新一样，统一是：alter table [tableName] update……where……
     * @return
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public Object update(){
        int update = aiMblUserInfoService.getBaseDao().updateBySql("alter table ai_mbl_user_info update model = '小潮',osversion = '同学' where userid='45670106f6c2334792e7573a39a0863b'");
        return update;
    }

}
