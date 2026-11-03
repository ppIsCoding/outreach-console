package com.yicheng.outreach.service;

import com.yicheng.outreach.common.BizException;
import com.yicheng.outreach.common.ErrorCode;
import com.yicheng.outreach.model.enums.TaskAction;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 状态机单元测试。
 */
class TaskStateMachineTest {

    private final TaskStateMachine stateMachine = new TaskStateMachine();

    @Test
    @DisplayName("合法流转：草稿 -> 待执行 -> 执行中 -> 暂停 -> 恢复")
    void shouldAllowHappyPath() {
        assertThat(stateMachine.next(TaskStatus.DRAFT, TaskAction.SUBMIT)).isEqualTo(TaskStatus.SCHEDULED);
        assertThat(stateMachine.next(TaskStatus.SCHEDULED, TaskAction.START)).isEqualTo(TaskStatus.RUNNING);
        assertThat(stateMachine.next(TaskStatus.RUNNING, TaskAction.PAUSE)).isEqualTo(TaskStatus.PAUSED);
        assertThat(stateMachine.next(TaskStatus.PAUSED, TaskAction.RESUME)).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    @DisplayName("合法流转：取消 / 终止 / 完成 / 失败")
    void shouldAllowTerminalTransitions() {
        assertThat(stateMachine.next(TaskStatus.SCHEDULED, TaskAction.CANCEL)).isEqualTo(TaskStatus.CANCELLED);
        assertThat(stateMachine.next(TaskStatus.RUNNING, TaskAction.TERMINATE)).isEqualTo(TaskStatus.TERMINATED);
        assertThat(stateMachine.next(TaskStatus.PAUSED, TaskAction.TERMINATE)).isEqualTo(TaskStatus.TERMINATED);
        assertThat(stateMachine.next(TaskStatus.RUNNING, TaskAction.COMPLETE)).isEqualTo(TaskStatus.COMPLETED);
        assertThat(stateMachine.next(TaskStatus.RUNNING, TaskAction.FAIL)).isEqualTo(TaskStatus.FAILED);
    }

    @Test
    @DisplayName("非法流转：草稿不能直接开始")
    void shouldRejectDraftStart() {
        assertThatThrownBy(() -> stateMachine.next(TaskStatus.DRAFT, TaskAction.START))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("非法流转：终态不能再次操作")
    void shouldRejectTerminalStateActions() {
        assertThatThrownBy(() -> stateMachine.next(TaskStatus.COMPLETED, TaskAction.PAUSE))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> stateMachine.next(TaskStatus.CANCELLED, TaskAction.RESUME))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> stateMachine.next(TaskStatus.TERMINATED, TaskAction.START))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("非法流转：暂停状态不能重复暂停")
    void shouldRejectInvalidPausedAction() {
        assertThatThrownBy(() -> stateMachine.next(TaskStatus.PAUSED, TaskAction.PAUSE))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("允许动作列表与状态匹配")
    void shouldExposeAllowedActions() {
        assertThat(stateMachine.allowedActions(TaskStatus.DRAFT)).containsExactly(TaskAction.SUBMIT);
        assertThat(stateMachine.allowedActions(TaskStatus.RUNNING))
                .containsExactly(TaskAction.PAUSE, TaskAction.TERMINATE,
                        TaskAction.COMPLETE, TaskAction.FAIL);
        assertThat(stateMachine.allowedActions(TaskStatus.COMPLETED)).isEmpty();
    }
}
