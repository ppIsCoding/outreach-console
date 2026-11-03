package com.yicheng.outreach.model.dto;

import com.yicheng.outreach.model.enums.TaskAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 状态流转请求。
 */
@Data
public class TaskStatusRequest {

    @NotNull(message = "action 不能为空")
    private TaskAction action;

    @NotNull(message = "version 不能为空")
    @Min(value = 0, message = "version 不能小于 0")
    private Integer version;
}
