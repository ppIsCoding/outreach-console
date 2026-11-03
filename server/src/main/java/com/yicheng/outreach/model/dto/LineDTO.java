package com.yicheng.outreach.model.dto;

import lombok.Data;

/**
 * 外呼线路及并发占用。
 */
@Data
public class LineDTO {

    private String id;
    private String name;
    private String provider;
    private Integer capacity;
    private Integer used;
    private Integer available;
    private String status;
    private Double occupancyRate;
}
