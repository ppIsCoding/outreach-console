package com.yicheng.outreach.model.dto;

import com.yicheng.outreach.model.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务列表 / 基础信息。
 */
@Data
public class TaskDTO {

    private String id;
    private String name;
    private String scriptId;
    private String scriptName;
    private String lineId;
    private String lineName;

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

    /**
     * 进度百分比（0–100，保留 1 位小数）。
     */
    private Double progress;

    /**
     * 接通率百分比（0–100）；calledCount 为 0 时为 null。
     */
    private Double connectRate;

    /**
     * 意向率百分比（0–100）；connectedCount 为 0 时为 null。
     */
    private Double intentRate;

    private Integer avgDurationSec;

    /**
     * 按“剩余号码 × 45 秒 ÷ 并发数”估算的剩余耗时（秒）。
     */
    private Long estimatedRemainingSec;
}
