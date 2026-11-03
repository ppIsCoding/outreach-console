package com.yicheng.outreach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yicheng.outreach.mapper.DailyStatRow;
import com.yicheng.outreach.mapper.TaskMapper;
import com.yicheng.outreach.model.dto.SummaryDTO;
import com.yicheng.outreach.model.entity.Task;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板概览指标。
 * <p>指标口径：按任务的 startedAt 归集当日已呼 / 接通 / 意向 / 通话时长。</p>
 */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final double CONNECT_RATE_BENCHMARK = 35.0;

    private final TaskMapper taskMapper;

    public DashboardService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Transactional(readOnly = true)
    public SummaryDTO summary() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        DailyStatRow todayStat = firstOrEmpty(taskMapper.selectDailyStats(todayStart, tomorrowStart));
        DailyStatRow yesterdayStat = firstOrEmpty(taskMapper.selectDailyStats(yesterdayStart, todayStart));

        long todayCalled = nvl(todayStat.getCalledCount());
        long todayConnected = nvl(todayStat.getConnectedCount());
        long todayIntent = nvl(todayStat.getIntentCount());
        long todayDuration = nvl(todayStat.getDurationSec());

        long yesterdayCalled = nvl(yesterdayStat.getCalledCount());
        long yesterdayConnected = nvl(yesterdayStat.getConnectedCount());
        long yesterdayIntent = nvl(yesterdayStat.getIntentCount());
        long yesterdayDuration = nvl(yesterdayStat.getDurationSec());

        Double connectRate = rate(todayConnected, todayCalled);
        Double yesterdayConnectRate = rate(yesterdayConnected, yesterdayCalled);
        long avgDuration = todayConnected > 0 ? todayDuration / todayConnected : 0L;
        long yesterdayAvgDuration = yesterdayConnected > 0 ? yesterdayDuration / yesterdayConnected : 0L;

        long runningCount = count(Wrappers.<Task>lambdaQuery().eq(Task::getStatus, TaskStatus.RUNNING));
        List<Task> runningTasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getStatus, TaskStatus.RUNNING)
                .orderByDesc(Task::getUpdatedAt));

        int concurrencyUsed = runningTasks.stream().mapToInt(t -> nvl(t.getConcurrency())).sum();
        int concurrencyTotal = 80;

        long totalTasks = count(Wrappers.lambdaQuery());
        long completedTasks = count(Wrappers.<Task>lambdaQuery().eq(Task::getStatus, TaskStatus.COMPLETED));
        long completedToday = count(Wrappers.<Task>lambdaQuery()
                .eq(Task::getStatus, TaskStatus.COMPLETED)
                .ge(Task::getFinishedAt, todayStart)
                .lt(Task::getFinishedAt, tomorrowStart));
        long completedYesterday = count(Wrappers.<Task>lambdaQuery()
                .eq(Task::getStatus, TaskStatus.COMPLETED)
                .ge(Task::getFinishedAt, yesterdayStart)
                .lt(Task::getFinishedAt, todayStart));

        List<SummaryDTO.DailyTrendDTO> trend = buildTrend(today);

        return SummaryDTO.builder()
                .todayCalledCount(todayCalled)
                .todayConnectedCount(todayConnected)
                .todayCalledCountChangeRate(changeRate((double) todayCalled, (double) yesterdayCalled))
                .connectRate(connectRate)
                .connectRateBenchmark(CONNECT_RATE_BENCHMARK)
                .connectRateChangeRate(changeRate(connectRate, yesterdayConnectRate))
                .runningTaskCount(runningCount)
                .runningTasks(runningTasks.stream().limit(3).map(this::toRunningTask).toList())
                .concurrencyUsed(concurrencyUsed)
                .concurrencyTotal(concurrencyTotal)
                .intentCustomerCount(todayIntent)
                .intentCustomerCountChangeRate(changeRate((double) todayIntent, (double) yesterdayIntent))
                .avgCallDurationSec(avgDuration)
                .avgCallDurationChangeRate(changeRate((double) avgDuration, (double) yesterdayAvgDuration))
                .taskCompletionRate(rate(completedTasks, totalTasks))
                .taskCompletionRateChangeRate(changeRate((double) completedToday, (double) completedYesterday))
                .completedTodayCount(completedToday)
                .totalTaskCount(totalTasks)
                .completedTaskCount(completedTasks)
                .trend(trend)
                .generatedAt(LocalDateTime.now(ZONE))
                .build();
    }

    private List<SummaryDTO.DailyTrendDTO> buildTrend(LocalDate today) {
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        Map<LocalDate, DailyStatRow> rows = new HashMap<>();
        for (DailyStatRow row : taskMapper.selectDailyStats(start, end)) {
            if (row.getStatDate() != null) {
                rows.put(row.getStatDate(), row);
            }
        }

        List<SummaryDTO.DailyTrendDTO> trend = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DailyStatRow row = rows.get(date);
            long called = row == null ? 0L : nvl(row.getCalledCount());
            long connected = row == null ? 0L : nvl(row.getConnectedCount());
            trend.add(SummaryDTO.DailyTrendDTO.builder()
                    .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .calledCount(called)
                    .connectedCount(connected)
                    .connectRate(rate(connected, called))
                    .build());
        }
        return trend;
    }

    private SummaryDTO.RunningTaskDTO toRunningTask(Task task) {
        int total = nvl(task.getTotalContacts());
        int called = nvl(task.getCalledCount());
        return SummaryDTO.RunningTaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .calledCount(called)
                .totalContacts(total)
                .progress(total > 0 ? TaskService.round(called * 100.0 / total) : 0.0)
                .status(task.getStatus())
                .build();
    }

    private long count(LambdaQueryWrapper<Task> wrapper) {
        Long value = taskMapper.selectCount(wrapper);
        return value == null ? 0L : value;
    }

    private DailyStatRow firstOrEmpty(List<DailyStatRow> rows) {
        return rows == null || rows.isEmpty() ? new DailyStatRow() : rows.get(0);
    }

    private static long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static Double rate(long numerator, long denominator) {
        return denominator <= 0 ? null : TaskService.round(numerator * 100.0 / denominator);
    }

    /**
     * 环比百分比；上期为 0 或为空时返回 null。
     */
    private static Double changeRate(Double current, Double previous) {
        if (current == null || previous == null || previous == 0) {
            return null;
        }
        return TaskService.round((current - previous) / previous * 100.0);
    }
}
