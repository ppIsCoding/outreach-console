package com.yicheng.outreach.service;

import com.yicheng.outreach.common.BizException;
import com.yicheng.outreach.common.ErrorCode;
import com.yicheng.outreach.model.dto.TaskCreateRequest;
import com.yicheng.outreach.model.dto.TaskDTO;
import com.yicheng.outreach.model.dto.TaskStatusRequest;
import com.yicheng.outreach.model.enums.TaskAction;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 任务服务集成测试：覆盖创建、乐观锁冲突与非法流转。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Test
    @DisplayName("创建任务后可通过状态机提交，并正确递增 version")
    void shouldCreateAndSubmitTask() {
        TaskDTO created = taskService.create(newRequest("单元测试任务-" + System.nanoTime()));
        assertThat(created.getStatus()).isEqualTo(TaskStatus.DRAFT);
        assertThat(created.getVersion()).isZero();
        assertThat(created.getScriptName()).isEqualTo("新客首访-标准版");

        TaskStatusRequest submit = statusRequest(TaskAction.SUBMIT, 0);
        TaskDTO scheduled = taskService.changeStatus(created.getId(), submit);
        assertThat(scheduled.getStatus()).isEqualTo(TaskStatus.SCHEDULED);
        assertThat(scheduled.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("过期 version 触发 40901 乐观锁冲突")
    void shouldRejectStaleVersion() {
        TaskDTO created = taskService.create(newRequest("乐观锁测试-" + System.nanoTime()));
        taskService.changeStatus(created.getId(), statusRequest(TaskAction.SUBMIT, 0));

        // START 是 SCHEDULED 的合法动作，但 version 已过期，应由 WHERE version = ? 拦截
        assertThatThrownBy(() -> taskService.changeStatus(created.getId(),
                statusRequest(TaskAction.START, 0)))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("非法状态流转返回 40901")
    void shouldRejectIllegalTransition() {
        TaskDTO created = taskService.create(newRequest("非法流转测试-" + System.nanoTime()));

        assertThatThrownBy(() -> taskService.changeStatus(created.getId(),
                statusRequest(TaskAction.PAUSE, 0)))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("同一创建人下任务名称不可重复")
    void shouldRejectDuplicateName() {
        String name = "重名校验-" + System.nanoTime();
        taskService.create(newRequest(name));

        assertThatThrownBy(() -> taskService.create(newRequest(name)))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PARAM_INVALID));
    }

    private TaskCreateRequest newRequest(String name) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setName(name);
        request.setScriptId("S001");
        request.setLineId("L001");
        request.setPriority("NORMAL");
        request.setTotalContacts(100);
        request.setConcurrency(5);
        request.setRetryLimit(1);
        request.setRetryIntervalMin(30);
        request.setCallWindow("09:00-20:00");
        request.setCreatorName("单元测试");
        return request;
    }

    private TaskStatusRequest statusRequest(TaskAction action, int version) {
        TaskStatusRequest request = new TaskStatusRequest();
        request.setAction(action);
        request.setVersion(version);
        return request;
    }
}
