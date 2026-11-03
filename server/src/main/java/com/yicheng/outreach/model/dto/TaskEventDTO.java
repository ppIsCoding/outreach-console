package com.yicheng.outreach.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务状态事件。
 */
@Data
public class TaskEventDTO {

    private Long id;
    private String type;
    private String message;
    private LocalDateTime createdAt;
}
