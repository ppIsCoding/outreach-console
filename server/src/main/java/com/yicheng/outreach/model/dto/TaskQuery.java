package com.yicheng.outreach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 任务分页查询条件。
 */
@Data
public class TaskQuery {

    /**
     * 状态筛选：ALL / DRAFT / SCHEDULED / RUNNING / PAUSED / COMPLETED / FAILED / TERMINATED / CANCELLED。
     */
    private String status;

    @Size(max = 64, message = "关键词长度不能超过 64 字")
    private String keyword;

    private String scriptId;

    /**
     * today / 7d / 30d / all / custom。
     */
    private String dateRange;

    /**
     * 自定义时间范围（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）。
     */
    private String startDate;
    private String endDate;

    @Min(value = 1, message = "页码从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数至少为 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer size = 10;

    /**
     * 排序，如 createdAt,desc / calledCount,asc / -updatedAt。
     */
    private String sort = "createdAt,desc";
}
