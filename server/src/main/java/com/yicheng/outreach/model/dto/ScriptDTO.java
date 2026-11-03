package com.yicheng.outreach.model.dto;

import lombok.Data;

/**
 * 话术模板。
 */
@Data
public class ScriptDTO {

    private String id;
    private String name;
    private String industry;
    private String version;
    private String status;
    private long taskCount;
}
