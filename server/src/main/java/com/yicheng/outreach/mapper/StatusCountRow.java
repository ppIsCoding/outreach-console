package com.yicheng.outreach.mapper;

import lombok.Data;

/**
 * 状态计数聚合行。
 */
@Data
public class StatusCountRow {
    private String status;
    private Long cnt;
}
