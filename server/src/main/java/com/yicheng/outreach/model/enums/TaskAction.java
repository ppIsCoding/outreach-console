package com.yicheng.outreach.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 状态流转动作。
 */
public enum TaskAction {
    SUBMIT,
    START,
    PAUSE,
    RESUME,
    TERMINATE,
    CANCEL,
    COMPLETE,
    FAIL;

    @JsonCreator
    public static TaskAction from(String value) {
        return value == null ? null : TaskAction.valueOf(value.trim().toUpperCase());
    }
}
