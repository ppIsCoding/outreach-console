package com.yicheng.outreach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yicheng.outreach.model.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外呼任务。
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String scriptId;
    private String lineId;
    private TaskStatus status;
    private String priority;

    private Integer totalContacts;
    private Integer calledCount;
    private Integer connectedCount;
    private Integer intentCount;
    private Integer totalDurationSec;

    private Integer concurrency;
    private Integer retryLimit;
    private Integer retryIntervalMin;
    private String callWindow;

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
