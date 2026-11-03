package com.yicheng.outreach.model.dto;

import lombok.Data;

/**
 * 各状态任务计数（供状态 Tab 使用）。
 */
@Data
public class StatusCountDTO {

    private long all;
    private long draft;
    private long scheduled;
    private long running;
    private long paused;
    private long completed;
    private long failed;
    private long terminated;
    private long cancelled;
}
