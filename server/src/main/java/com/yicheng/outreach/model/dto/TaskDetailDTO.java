package com.yicheng.outreach.model.dto;

import com.yicheng.outreach.model.enums.TaskAction;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 任务详情：基础信息 + 最近状态事件 + 通话结果分布 + 当前可执行动作。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDetailDTO extends TaskDTO {

    private List<TaskEventDTO> events;

    /**
     * 通话结果分布，如 {@code {"CONNECTED": 512, "NO_ANSWER": 320}}。
     */
    private Map<String, Long> resultDistribution;

    /**
     * 意向等级分布，如 {@code {"A": 32, "B": 56}}。
     */
    private Map<String, Long> intentDistribution;

    /**
     * 当前状态允许的动作，前端据此置灰按钮。
     */
    private List<TaskAction> allowedActions;
}
