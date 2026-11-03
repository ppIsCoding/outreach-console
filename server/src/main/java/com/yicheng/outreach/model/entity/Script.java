package com.yicheng.outreach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 话术模板。
 */
@Data
@TableName("script")
public class Script {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String industry;
    private String version;
    private String status;
}
