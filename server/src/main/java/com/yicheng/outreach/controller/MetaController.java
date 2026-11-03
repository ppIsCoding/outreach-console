package com.yicheng.outreach.controller;

import com.yicheng.outreach.common.ApiResponse;
import com.yicheng.outreach.model.dto.LineDTO;
import com.yicheng.outreach.model.dto.ScriptDTO;
import com.yicheng.outreach.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 元数据接口：话术模板、外呼线路。
 */
@RestController
@RequestMapping("/api/v1/meta")
@Tag(name = "元数据")
public class MetaController {

    private final TaskService taskService;

    public MetaController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/scripts")
    @Operation(summary = "话术模板列表")
    public ApiResponse<List<ScriptDTO>> scripts() {
        return ApiResponse.success(taskService.scripts());
    }

    @GetMapping("/lines")
    @Operation(summary = "外呼线路及并发占用")
    public ApiResponse<List<LineDTO>> lines() {
        return ApiResponse.success(taskService.lines());
    }
}
