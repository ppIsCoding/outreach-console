package com.yicheng.outreach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 外呼线路。
 */
@Data
@TableName("call_line")
public class Line {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String provider;
    private Integer capacity;

    @TableField("used_concurrency")
    private Integer used;

    private String status;
}
