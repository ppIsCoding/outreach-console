package com.yicheng.outreach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建任务请求。
 */
@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    @Size(min = 2, max = 30, message = "任务名称长度需为 2–30 字")
    private String name;

    @NotBlank(message = "话术模板不能为空")
    private String scriptId;

    @NotBlank(message = "外呼线路不能为空")
    private String lineId;

    @Pattern(regexp = "NORMAL|HIGH", message = "优先级只能为 NORMAL 或 HIGH")
    private String priority;

    @NotNull(message = "名单量不能为空")
    @Min(value = 1, message = "名单量至少为 1")
    @Max(value = 50000, message = "名单量不能超过 50000")
    private Integer totalContacts;

    @NotNull(message = "并发数不能为空")
    @Min(value = 1, message = "并发数至少为 1")
    @Max(value = 50, message = "并发数不能超过 50")
    private Integer concurrency;

    @NotNull(message = "重呼次数不能为空")
    @Min(value = 0, message = "重呼次数不能小于 0")
    @Max(value = 3, message = "重呼次数不能超过 3")
    private Integer retryLimit;

    @NotNull(message = "重呼间隔不能为空")
    @Min(value = 5, message = "重呼间隔至少为 5 分钟")
    @Max(value = 1440, message = "重呼间隔不能超过 1440 分钟")
    private Integer retryIntervalMin;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$",
            message = "呼叫时段格式应为 HH:mm-HH:mm")
    private String callWindow;

    private LocalDateTime scheduledAt;

    @Size(max = 32, message = "创建人名称不能超过 32 字")
    private String creatorName;
}
