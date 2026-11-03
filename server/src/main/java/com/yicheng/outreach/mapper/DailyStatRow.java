package com.yicheng.outreach.mapper;

import lombok.Data;

import java.time.LocalDate;

/**
 * 每日任务指标聚合行。
 */
@Data
public class DailyStatRow {
    private LocalDate statDate;
    private Long taskCount;
    private Long calledCount;
    private Long connectedCount;
    private Long intentCount;
    private Long durationSec;
}
