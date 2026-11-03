package com.yicheng.outreach.service;

import com.yicheng.outreach.common.BizException;
import com.yicheng.outreach.model.enums.TaskAction;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 任务状态机：后端唯一权威。
 *
 * <pre>
 * DRAFT     --SUBMIT--> SCHEDULED
 * SCHEDULED --START--> RUNNING / --CANCEL--> CANCELLED
 * RUNNING   --PAUSE--> PAUSED / --TERMINATE--> TERMINATED / --COMPLETE--> COMPLETED / --FAIL--> FAILED
 * PAUSED    --RESUME--> RUNNING / --TERMINATE--> TERMINATED
 * </pre>
 */
@Component
public class TaskStateMachine {

    private static final Map<TaskStatus, Map<TaskAction, TaskStatus>> TRANSITIONS = Map.of(
            TaskStatus.DRAFT, Map.of(
                    TaskAction.SUBMIT, TaskStatus.SCHEDULED
            ),
            TaskStatus.SCHEDULED, Map.of(
                    TaskAction.START, TaskStatus.RUNNING,
                    TaskAction.CANCEL, TaskStatus.CANCELLED
            ),
            TaskStatus.RUNNING, Map.of(
                    TaskAction.PAUSE, TaskStatus.PAUSED,
                    TaskAction.TERMINATE, TaskStatus.TERMINATED,
                    TaskAction.COMPLETE, TaskStatus.COMPLETED,
                    TaskAction.FAIL, TaskStatus.FAILED
            ),
            TaskStatus.PAUSED, Map.of(
                    TaskAction.RESUME, TaskStatus.RUNNING,
                    TaskAction.TERMINATE, TaskStatus.TERMINATED
            )
    );

    /**
     * 计算目标状态，非法流转抛出 40901。
     */
    public TaskStatus next(TaskStatus current, TaskAction action) {
        TaskStatus target = current == null || action == null
                ? null
                : TRANSITIONS.getOrDefault(current, Map.of()).get(action);
        if (target == null) {
            throw BizException.conflict("任务当前状态为 " + current + "，不允许执行 " + action + " 动作");
        }
        return target;
    }

    public boolean canTransition(TaskStatus current, TaskAction action) {
        return current != null && action != null
                && TRANSITIONS.getOrDefault(current, Map.of()).containsKey(action);
    }

    /**
     * 当前状态允许的动作，供详情接口返回给前端做按钮置灰。
     */
    public List<TaskAction> allowedActions(TaskStatus current) {
        Map<TaskAction, TaskStatus> actions = TRANSITIONS.getOrDefault(current, Map.of());
        return Arrays.stream(TaskAction.values())
                .filter(actions::containsKey)
                .toList();
    }
}
