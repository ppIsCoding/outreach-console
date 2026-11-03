package com.yicheng.outreach.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 任务状态。
 */
public enum TaskStatus {
    DRAFT,
    SCHEDULED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    TERMINATED,
    CANCELLED;

    @JsonCreator
    public static TaskStatus from(String value) {
        return value == null ? null : TaskStatus.valueOf(value.trim().toUpperCase());
    }

    /**
     * 是否为终态（终态任务不再被定时模拟器推进）。
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == TERMINATED || this == CANCELLED;
    }
}
