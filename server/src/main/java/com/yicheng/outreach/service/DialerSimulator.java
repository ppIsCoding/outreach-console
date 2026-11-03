package com.yicheng.outreach.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yicheng.outreach.mapper.CallResultMapper;
import com.yicheng.outreach.mapper.TaskEventMapper;
import com.yicheng.outreach.mapper.TaskMapper;
import com.yicheng.outreach.model.entity.CallResult;
import com.yicheng.outreach.model.entity.Task;
import com.yicheng.outreach.model.entity.TaskEvent;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 外呼进度模拟器。
 * <p>每 2 秒推进 RUNNING 任务，写入通话结果并推送 WebSocket。
 * 每次只推进并发数 / 6（至少 1 条），避免演示数据被快速跑完。</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.simulator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DialerSimulator {

    private static final Logger log = LoggerFactory.getLogger(DialerSimulator.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final TaskMapper taskMapper;
    private final CallResultMapper callResultMapper;
    private final TaskEventMapper taskEventMapper;

    public DialerSimulator(TaskMapper taskMapper,
                           CallResultMapper callResultMapper,
                           TaskEventMapper taskEventMapper) {
        this.taskMapper = taskMapper;
        this.callResultMapper = callResultMapper;
        this.taskEventMapper = taskEventMapper;
    }

    @Scheduled(fixedRateString = "${app.simulator.interval-ms:2000}")
    @Transactional
    public void tick() {
        List<Task> runningTasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getStatus, TaskStatus.RUNNING));
        if (runningTasks.isEmpty()) {
            return;
        }

        for (Task task : runningTasks) {
            advance(task);
        }
    }

    private void advance(Task task) {
        int total = nvl(task.getTotalContacts());
        if (total <= 0) {
            return;
        }
        int called = nvl(task.getCalledCount());
        int connected = nvl(task.getConnectedCount());
        int intent = nvl(task.getIntentCount());
        int duration = nvl(task.getTotalDurationSec());
        LocalDateTime now = LocalDateTime.now(ZONE);

        // 已经呼完但状态尚未落库，补一次完成状态
        if (called >= total) {
            int rows = taskMapper.updateProgressWithVersion(task.getId(), called, connected, intent, duration,
                    TaskStatus.COMPLETED, task.getVersion(), now, now);
            if (rows > 0) {
                addEvent(task.getId(), "COMPLETED", "任务全部外呼完成");
            }
            return;
        }

        int increment = Math.max(1, nvl(task.getConcurrency()) / 6);
        int newCalled = Math.min(total, called + increment);
        boolean connectedFlag = ThreadLocalRandom.current().nextDouble() < 0.38 && connected < newCalled;
        int newConnected = connectedFlag ? connected + 1 : connected;
        boolean intentFlag = connectedFlag
                && ThreadLocalRandom.current().nextDouble() < 0.08
                && intent < newConnected;
        int newIntent = intentFlag ? intent + 1 : intent;
        int newDuration = duration + (connectedFlag ? ThreadLocalRandom.current().nextInt(20, 61) : 0);
        boolean finished = newCalled >= total;
        TaskStatus nextStatus = finished ? TaskStatus.COMPLETED : TaskStatus.RUNNING;

        int rows = taskMapper.updateProgressWithVersion(task.getId(), newCalled, newConnected, newIntent,
                newDuration, nextStatus, task.getVersion(), finished ? now : null, now);
        if (rows == 0) {
            // 乐观锁冲突：本次跳过，等待下一轮
            return;
        }

        CallResult result = buildCallResult(task.getId(), connectedFlag, now);
        callResultMapper.insert(result);

        if (finished) {
            addEvent(task.getId(), "COMPLETED", "任务全部外呼完成");
        }
        log.debug("推进任务 {}: called={}, connected={}, status={}",
                task.getId(), newCalled, newConnected, nextStatus);
    }

    private CallResult buildCallResult(String taskId, boolean connected, LocalDateTime now) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        CallResult result = new CallResult();
        result.setTaskId(taskId);
        result.setPhone(String.format("139%08d", random.nextInt(100_000_000)));
        result.setCalledAt(now);

        if (connected) {
            result.setResult("CONNECTED");
            result.setDurationSec(random.nextInt(20, 91));
            result.setHangupCause("NORMAL");
            int level = random.nextInt(100);
            if (level < 12) {
                result.setIntentLevel("A");
            } else if (level < 30) {
                result.setIntentLevel("B");
            } else if (level < 55) {
                result.setIntentLevel("C");
            } else {
                result.setIntentLevel("D");
            }
        } else {
            int roll = random.nextInt(100);
            if (roll < 60) {
                result.setResult("NO_ANSWER");
                result.setHangupCause("NO_ANSWER");
            } else if (roll < 80) {
                result.setResult("REJECTED");
                result.setHangupCause("REJECTED");
            } else if (roll < 90) {
                result.setResult("INVALID");
                result.setHangupCause("INVALID_NUMBER");
            } else {
                result.setResult("POWER_OFF");
                result.setHangupCause("POWER_OFF");
            }
            result.setDurationSec(0);
        }
        return result;
    }

    private void addEvent(String taskId, String type, String message) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setType(type);
        event.setMessage(message);
        event.setCreatedAt(LocalDateTime.now(ZONE));
        taskEventMapper.insert(event);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
