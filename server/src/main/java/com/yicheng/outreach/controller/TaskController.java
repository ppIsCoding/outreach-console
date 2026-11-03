package com.yicheng.outreach.controller;

import com.yicheng.outreach.common.ApiResponse;
import com.yicheng.outreach.common.PageResult;
import com.yicheng.outreach.model.dto.StatusCountDTO;
import com.yicheng.outreach.model.dto.TaskCreateRequest;
import com.yicheng.outreach.model.dto.TaskDTO;
import com.yicheng.outreach.model.dto.TaskDetailDTO;
import com.yicheng.outreach.model.dto.TaskQuery;
import com.yicheng.outreach.model.dto.TaskStatusRequest;
import com.yicheng.outreach.model.entity.CallResult;
import com.yicheng.outreach.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理接口。
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "任务管理")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "分页查询任务")
    public ApiResponse<PageResult<TaskDTO>> list(@Valid @ModelAttribute TaskQuery query) {
        return ApiResponse.success(taskService.list(query));
    }

    @GetMapping("/stats")
    @Operation(summary = "各状态任务计数")
    public ApiResponse<StatusCountDTO> stats() {
        return ApiResponse.success(taskService.stats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    public ApiResponse<TaskDetailDTO> detail(@PathVariable String id) {
        return ApiResponse.success(taskService.detail(id));
    }

    @PostMapping
    @Operation(summary = "创建任务")
    public ResponseEntity<ApiResponse<TaskDTO>> create(@Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(taskService.create(request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "状态流转（乐观锁）")
    public ApiResponse<TaskDTO> changeStatus(@PathVariable String id,
                                             @Valid @RequestBody TaskStatusRequest request) {
        return ApiResponse.success(taskService.changeStatus(id, request));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "复制任务")
    public ResponseEntity<ApiResponse<TaskDTO>> duplicate(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(taskService.duplicate(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务（仅草稿）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        taskService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "通话结果分页")
    public ApiResponse<PageResult<CallResult>> results(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String intentLevel) {
        return ApiResponse.success(taskService.results(id, page, size, result, intentLevel));
    }
}
