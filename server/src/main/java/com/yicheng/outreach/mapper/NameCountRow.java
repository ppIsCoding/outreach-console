package com.yicheng.outreach.mapper;

import lombok.Data;

/**
 * 通用“名称 - 数量”聚合行。
 */
@Data
public class NameCountRow {
    private String name;
    private Long cnt;
}
