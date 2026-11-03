package com.yicheng.outreach.controller;

import com.yicheng.outreach.common.ApiResponse;
import com.yicheng.outreach.model.dto.SummaryDTO;
import com.yicheng.outreach.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板概览接口。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "看板概览")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "概览指标（含环比与近 7 日趋势）")
    public ApiResponse<SummaryDTO> summary() {
        return ApiResponse.success(dashboardService.summary());
    }
}
