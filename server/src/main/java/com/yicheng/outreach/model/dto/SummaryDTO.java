package com.yicheng.outreach.model.dto;

import com.yicheng.outreach.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 看板概览指标。
 * <p>环比字段均为百分比（如 12.5 表示 +12.5%），分母为 0 时返回 null。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO {

    private long todayCalledCount;
    private long todayConnectedCount;
    private Double todayCalledCountChangeRate;

    private Double connectRate;
    private Double connectRateBenchmark;
    private Double connectRateChangeRate;

    private long runningTaskCount;
    private List<RunningTaskDTO> runningTasks;

    /** 执行中任务占用的并发总数 */
    private int concurrencyUsed;
    /** 线路可用并发总量 */
    private int concurrencyTotal;

    private long intentCustomerCount;
    private Double intentCustomerCountChangeRate;

    private long avgCallDurationSec;
    private Double avgCallDurationChangeRate;

    private Double taskCompletionRate;
    private Double taskCompletionRateChangeRate;
    private long completedTodayCount;

    private long totalTaskCount;
    private long completedTaskCount;

    /**
     * 近 7 日趋势（按任务 startedAt 聚合）。
     */
    private List<DailyTrendDTO> trend;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunningTaskDTO {
        private String id;
        private String name;
        private int calledCount;
        private int totalContacts;
        private double progress;
        private TaskStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrendDTO {
        private String date;
        private long calledCount;
        private long connectedCount;
        private Double connectRate;
    }
}
