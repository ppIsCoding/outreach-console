package com.yicheng.outreach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务状态事件，用于详情页时间线。
 * <p>数据库列使用 {@code event_type} 避开保留字，对外 JSON 仍为 {@code type}。</p>
 */
@Data
@TableName("task_event")
public class TaskEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    @TableField("event_type")
    private String type;

    private String message;
    private LocalDateTime createdAt;
}
