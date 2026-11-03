package com.yicheng.outreach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次通话结果。
 * <p>数据库列使用 {@code call_status} 避开保留字，对外 JSON 仍为 {@code result}。</p>
 */
@Data
@TableName("call_result")
public class CallResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String phone;

    @TableField("call_status")
    private String result;

    private Integer durationSec;
    private String hangupCause;
    private String intentLevel;
    private LocalDateTime calledAt;
}
