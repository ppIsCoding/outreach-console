package com.yicheng.outreach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yicheng.outreach.common.BizException;
import com.yicheng.outreach.common.PageResult;
import com.yicheng.outreach.mapper.CallResultMapper;
import com.yicheng.outreach.mapper.LineMapper;
import com.yicheng.outreach.mapper.NameCountRow;
import com.yicheng.outreach.mapper.ScriptMapper;
import com.yicheng.outreach.mapper.StatusCountRow;
import com.yicheng.outreach.mapper.TaskEventMapper;
import com.yicheng.outreach.mapper.TaskMapper;
import com.yicheng.outreach.model.dto.LineDTO;
import com.yicheng.outreach.model.dto.ScriptDTO;
import com.yicheng.outreach.model.dto.StatusCountDTO;
import com.yicheng.outreach.model.dto.TaskCreateRequest;
import com.yicheng.outreach.model.dto.TaskDTO;
import com.yicheng.outreach.model.dto.TaskDetailDTO;
import com.yicheng.outreach.model.dto.TaskEventDTO;
import com.yicheng.outreach.model.dto.TaskQuery;
import com.yicheng.outreach.model.dto.TaskStatusRequest;
import com.yicheng.outreach.model.entity.CallResult;
import com.yicheng.outreach.model.entity.Line;
import com.yicheng.outreach.model.entity.Script;
import com.yicheng.outreach.model.entity.Task;
import com.yicheng.outreach.model.entity.TaskEvent;
import com.yicheng.outreach.model.enums.TaskAction;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务领域服务：查询、创建、状态流转、复制、删除、通话结果。
 */
@Service
public class TaskService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_PARSER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskMapper taskMapper;
    private final ScriptMapper scriptMapper;
    private final LineMapper lineMapper;
    private final CallResultMapper callResultMapper;
    private final TaskEventMapper taskEventMapper;
    private final TaskStateMachine stateMachine;

    public TaskService(TaskMapper taskMapper,
                       ScriptMapper scriptMapper,
                       LineMapper lineMapper,
                       CallResultMapper callResultMapper,
                       TaskEventMapper taskEventMapper,
                       TaskStateMachine stateMachine) {
        this.taskMapper = taskMapper;
        this.scriptMapper = scriptMapper;
        this.lineMapper = lineMapper;
        this.callResultMapper = callResultMapper;
        this.taskEventMapper = taskEventMapper;
        this.stateMachine = stateMachine;
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResult<TaskDTO> list(TaskQuery query) {
        int pageNo = query.getPage() == null ? 1 : Math.max(1, query.getPage());
        int size = query.getSize() == null
                ? DEFAULT_PAGE_SIZE
                : Math.min(MAX_PAGE_SIZE, Math.max(1, query.getSize()));

        LambdaQueryWrapper<Task> wrapper = Wrappers.lambdaQuery();
        TaskStatus status = parseStatus(query.getStatus());
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = "%" + query.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
            wrapper.and(w -> w.apply("LOWER(name) LIKE {0}", keyword)
                    .or()
                    .apply("LOWER(id) LIKE {0}", keyword));
        }
        if (StringUtils.hasText(query.getScriptId())) {
            wrapper.eq(Task::getScriptId, query.getScriptId().trim());
        }
        applyDateRange(wrapper, query);
        applySort(wrapper, query.getSort());

        Page<Task> page = new Page<>(pageNo, size);
        taskMapper.selectPage(page, wrapper);

        Map<String, Script> scripts = loadScripts();
        Map<String, Line> lines = loadLines();
        List<TaskDTO> items = page.getRecords().stream()
                .map(task -> toDTO(task, scripts.get(task.getScriptId()), lines.get(task.getLineId())))
                .toList();
        return PageResult.of(items, pageNo, size, page.getTotal());
    }

    @Transactional(readOnly = true)
    public StatusCountDTO stats() {
        StatusCountDTO dto = new StatusCountDTO();
        long all = 0L;
        for (StatusCountRow row : taskMapper.countByStatus()) {
            long count = row.getCnt() == null ? 0L : row.getCnt();
            all += count;
            TaskStatus status = parseStatusQuietly(row.getStatus());
            if (status == null) {
                continue;
            }
            switch (status) {
                case DRAFT -> dto.setDraft(count);
                case SCHEDULED -> dto.setScheduled(count);
                case RUNNING -> dto.setRunning(count);
                case PAUSED -> dto.setPaused(count);
                case COMPLETED -> dto.setCompleted(count);
                case FAILED -> dto.setFailed(count);
                case TERMINATED -> dto.setTerminated(count);
                case CANCELLED -> dto.setCancelled(count);
            }
        }
        dto.setAll(all);
        return dto;
    }

    @Transactional(readOnly = true)
    public TaskDetailDTO detail(String id) {
        Task task = getOrThrow(id);
        TaskDTO base = toDTO(task);
        TaskDetailDTO detail = new TaskDetailDTO();
        BeanUtils.copyProperties(base, detail);

        Page<TaskEvent> eventPage = new Page<>(1, 20);
        taskEventMapper.selectPage(eventPage, Wrappers.<TaskEvent>lambdaQuery()
                .eq(TaskEvent::getTaskId, id)
                .orderByDesc(TaskEvent::getCreatedAt)
                .orderByDesc(TaskEvent::getId));
        detail.setEvents(eventPage.getRecords().stream().map(this::toEventDTO).toList());

        detail.setResultDistribution(buildResultDistribution(id));
        detail.setIntentDistribution(buildIntentDistribution(id));
        detail.setAllowedActions(stateMachine.allowedActions(task.getStatus()));
        return detail;
    }

    // ------------------------------------------------------------------
    // 命令
    // ------------------------------------------------------------------

    @Transactional
    public TaskDTO create(TaskCreateRequest request) {
        String creator = StringUtils.hasText(request.getCreatorName())
                ? request.getCreatorName().trim()
                : "当前用户";
        String name = request.getName().trim();
        if (name.length() < 2 || name.length() > 30) {
            throw BizException.paramInvalid("name", "任务名称长度需为 2–30 字");
        }
        String priority = StringUtils.hasText(request.getPriority())
                ? request.getPriority().trim().toUpperCase(Locale.ROOT)
                : "NORMAL";
        if (!"NORMAL".equals(priority) && !"HIGH".equals(priority)) {
            throw BizException.paramInvalid("priority", "优先级只能为 NORMAL 或 HIGH");
        }
        String callWindow = StringUtils.hasText(request.getCallWindow())
                ? request.getCallWindow().trim()
                : "09:00-20:00";
        validateCallWindow(callWindow);

        LocalDateTime now = LocalDateTime.now(ZONE);
        if (request.getScheduledAt() != null && request.getScheduledAt().isBefore(now.plusMinutes(5))) {
            throw BizException.paramInvalid("scheduledAt", "计划执行时间需晚于当前时间 5 分钟");
        }
        if (scriptMapper.selectById(request.getScriptId()) == null) {
            throw BizException.paramInvalid("scriptId", "话术模板不存在");
        }
        if (lineMapper.selectById(request.getLineId()) == null) {
            throw BizException.paramInvalid("lineId", "外呼线路不存在");
        }
        if (existsName(creator, name, null)) {
            throw BizException.paramInvalid("name", "同一创建人下任务名称不可重复");
        }

        Task task = new Task();
        task.setId(nextTaskId());
        task.setName(name);
        task.setScriptId(request.getScriptId());
        task.setLineId(request.getLineId());
        task.setStatus(TaskStatus.DRAFT);
        task.setPriority(priority);
        task.setTotalContacts(request.getTotalContacts());
        task.setCalledCount(0);
        task.setConnectedCount(0);
        task.setIntentCount(0);
        task.setTotalDurationSec(0);
        task.setConcurrency(request.getConcurrency());
        task.setRetryLimit(request.getRetryLimit());
        task.setRetryIntervalMin(request.getRetryIntervalMin());
        task.setCallWindow(callWindow);
        task.setScheduledAt(request.getScheduledAt());
        task.setCreatorName(creator);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setVersion(0);
        taskMapper.insert(task);
        addEvent(task.getId(), "CREATED", "任务创建");
        return toDTO(task);
    }

    @Transactional
    public TaskDTO changeStatus(String id, TaskStatusRequest request) {
        Task task = getOrThrow(id);
        TaskStatus current = task.getStatus();
        TaskStatus target = stateMachine.next(current, request.getAction());

        if ((request.getAction() == TaskAction.SUBMIT || request.getAction() == TaskAction.START)
                && nvl(task.getTotalContacts()) <= 0) {
            throw BizException.conflict("名单量为 0，无法提交或启动任务");
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime scheduledAt = request.getAction() == TaskAction.SUBMIT && task.getScheduledAt() == null
                ? now
                : null;
        LocalDateTime startedAt = target == TaskStatus.RUNNING && task.getStartedAt() == null
                ? now
                : null;
        LocalDateTime finishedAt = target.isTerminal() ? now : null;

        int rows = taskMapper.updateStatusWithVersion(
                id, target, request.getVersion(), scheduledAt, startedAt, finishedAt, now);
        if (rows == 0) {
            throw BizException.conflict("任务状态已被他人修改，请刷新后重试");
        }
        addEvent(id, "STATUS_CHANGE", "状态由 " + current + " 变更为 " + target);
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public TaskDTO duplicate(String id) {
        Task source = getOrThrow(id);
        LocalDateTime now = LocalDateTime.now(ZONE);

        Task copy = new Task();
        copy.setId(nextTaskId());
        copy.setName(uniqueCopyName(source.getCreatorName(), source.getName()));
        copy.setScriptId(source.getScriptId());
        copy.setLineId(source.getLineId());
        copy.setStatus(TaskStatus.DRAFT);
        copy.setPriority(source.getPriority());
        copy.setTotalContacts(source.getTotalContacts());
        copy.setCalledCount(0);
        copy.setConnectedCount(0);
        copy.setIntentCount(0);
        copy.setTotalDurationSec(0);
        copy.setConcurrency(source.getConcurrency());
        copy.setRetryLimit(source.getRetryLimit());
        copy.setRetryIntervalMin(source.getRetryIntervalMin());
        copy.setCallWindow(source.getCallWindow());
        copy.setScheduledAt(null);
        copy.setStartedAt(null);
        copy.setFinishedAt(null);
        copy.setCreatorName(source.getCreatorName());
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        copy.setVersion(0);
        taskMapper.insert(copy);
        addEvent(copy.getId(), "CREATED", "复制自任务 " + source.getId());
        return toDTO(copy);
    }

    @Transactional
    public void delete(String id) {
        Task task = getOrThrow(id);
        if (task.getStatus() != TaskStatus.DRAFT) {
            throw BizException.conflict("仅草稿任务可删除");
        }
        taskMapper.deleteById(id);
        taskEventMapper.delete(Wrappers.<TaskEvent>lambdaQuery().eq(TaskEvent::getTaskId, id));
        callResultMapper.delete(Wrappers.<CallResult>lambdaQuery().eq(CallResult::getTaskId, id));
    }

    @Transactional(readOnly = true)
    public PageResult<CallResult> results(String id, int page, int size, String result, String intentLevel) {
        getOrThrow(id);
        int pageNo = Math.max(1, page);
        int pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        LambdaQueryWrapper<CallResult> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(CallResult::getTaskId, id);
        if (StringUtils.hasText(result)) {
            wrapper.eq(CallResult::getResult, result.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(intentLevel)) {
            wrapper.eq(CallResult::getIntentLevel, intentLevel.trim().toUpperCase(Locale.ROOT));
        }
        wrapper.orderByDesc(CallResult::getCalledAt).orderByDesc(CallResult::getId);

        Page<CallResult> pageObj = new Page<>(pageNo, pageSize);
        callResultMapper.selectPage(pageObj, wrapper);
        return PageResult.of(pageObj.getRecords(), pageNo, pageSize, pageObj.getTotal());
    }

    // ------------------------------------------------------------------
    // 元数据 / 工具
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ScriptDTO> scripts() {
        List<Task> tasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery().select(Task::getScriptId));
        Map<String, Long> counts = tasks.stream()
                .filter(task -> task.getScriptId() != null)
                .collect(Collectors.groupingBy(Task::getScriptId, Collectors.counting()));
        return scriptMapper.selectList(Wrappers.<Script>lambdaQuery().orderByAsc(Script::getId)).stream()
                .map(script -> {
                    ScriptDTO dto = new ScriptDTO();
                    BeanUtils.copyProperties(script, dto);
                    dto.setTaskCount(counts.getOrDefault(script.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LineDTO> lines() {
        List<Task> runningTasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getStatus, TaskStatus.RUNNING));
        Map<String, Integer> usedMap = runningTasks.stream()
                .filter(task -> task.getLineId() != null)
                .collect(Collectors.groupingBy(Task::getLineId,
                        Collectors.summingInt(task -> nvl(task.getConcurrency()))));
        return lineMapper.selectList(Wrappers.<Line>lambdaQuery().orderByAsc(Line::getId)).stream()
                .map(line -> {
                    LineDTO dto = new LineDTO();
                    BeanUtils.copyProperties(line, dto);
                    int capacity = nvl(line.getCapacity());
                    int used = usedMap.getOrDefault(line.getId(), 0);
                    dto.setUsed(used);
                    dto.setAvailable(Math.max(0, capacity - used));
                    dto.setOccupancyRate(capacity > 0 ? round(used * 100.0 / capacity) : 0.0);
                    if (used >= capacity && capacity > 0) {
                        dto.setStatus("FULL");
                    }
                    return dto;
                })
                .toList();
    }

    public TaskDTO toDTO(Task task) {
        return toDTO(task, scriptMapper.selectById(task.getScriptId()), lineMapper.selectById(task.getLineId()));
    }

    public TaskDTO toDTO(Task task, Script script, Line line) {
        TaskDTO dto = new TaskDTO();
        BeanUtils.copyProperties(task, dto);
        dto.setScriptName(script == null ? null : script.getName());
        dto.setLineName(line == null ? null : line.getName());

        int total = nvl(task.getTotalContacts());
        int called = nvl(task.getCalledCount());
        int connected = nvl(task.getConnectedCount());
        int intent = nvl(task.getIntentCount());
        int duration = nvl(task.getTotalDurationSec());
        int concurrency = nvl(task.getConcurrency());

        dto.setProgress(total > 0 ? round(called * 100.0 / total) : 0.0);
        dto.setConnectRate(called > 0 ? round(connected * 100.0 / called) : null);
        dto.setIntentRate(connected > 0 ? round(intent * 100.0 / connected) : null);
        dto.setAvgDurationSec(connected > 0 ? (int) Math.round(duration * 1.0 / connected) : 0);
        dto.setEstimatedRemainingSec(concurrency > 0
                ? (long) Math.ceil(Math.max(0, total - called) * 45.0 / concurrency)
                : 0L);
        return dto;
    }

    public static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Task getOrThrow(String id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw BizException.notFound("任务不存在：" + id);
        }
        return task;
    }

    private TaskStatus parseStatus(String value) {
        if (!StringUtils.hasText(value) || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        TaskStatus status = parseStatusQuietly(value);
        if (status == null) {
            throw BizException.paramInvalid("status", "status 参数不合法");
        }
        return status;
    }

    private TaskStatus parseStatusQuietly(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return TaskStatus.from(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, Script> loadScripts() {
        return scriptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Script::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, Line> loadLines() {
        return lineMapper.selectList(null).stream()
                .collect(Collectors.toMap(Line::getId, Function.identity(), (a, b) -> a));
    }

    private void applyDateRange(LambdaQueryWrapper<Task> wrapper, TaskQuery query) {
        String range = query.getDateRange();
        if (!StringUtils.hasText(range)) {
            return;
        }
        LocalDate today = LocalDate.now(ZONE);
        switch (range.trim().toLowerCase(Locale.ROOT)) {
            case "today" -> wrapper.ge(Task::getCreatedAt, today.atStartOfDay());
            case "7d", "7days", "last7days" ->
                    wrapper.ge(Task::getCreatedAt, today.minusDays(6).atStartOfDay());
            case "30d", "30days", "last30days" ->
                    wrapper.ge(Task::getCreatedAt, today.minusDays(29).atStartOfDay());
            case "all" -> {
                // 全部时间：不追加时间条件。
            }
            case "custom" -> {
                LocalDateTime start = parseBoundary(query.getStartDate(), false, "startDate");
                LocalDateTime endExclusive = parseBoundary(query.getEndDate(), true, "endDate");
                if (start != null) {
                    wrapper.ge(Task::getCreatedAt, start);
                }
                if (endExclusive != null) {
                    wrapper.lt(Task::getCreatedAt, endExclusive);
                }
            }
            default -> throw BizException.paramInvalid("dateRange", "dateRange 仅支持 today / 7d / 30d / all / custom");
        }
    }

    private LocalDateTime parseBoundary(String value, boolean end, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text);
                return end ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
            }
            LocalDateTime dateTime = LocalDateTime.parse(text, DATE_TIME_PARSER);
            return end ? dateTime.plusSeconds(1) : dateTime;
        } catch (DateTimeParseException ex) {
            throw BizException.paramInvalid(field, "时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private void applySort(LambdaQueryWrapper<Task> wrapper, String sort) {
        String value = StringUtils.hasText(sort) ? sort.trim() : "createdAt,desc";
        boolean desc = true;
        String field = value;
        if (value.startsWith("-")) {
            field = value.substring(1);
        } else if (value.contains(",")) {
            String[] parts = value.split(",", 2);
            field = parts[0];
            desc = !"asc".equalsIgnoreCase(parts[1].trim());
        } else if (value.contains(":")) {
            String[] parts = value.split(":", 2);
            field = parts[0];
            desc = !"asc".equalsIgnoreCase(parts[1].trim());
        } else if (value.contains("_")) {
            int index = value.lastIndexOf('_');
            String order = value.substring(index + 1);
            if ("asc".equalsIgnoreCase(order) || "desc".equalsIgnoreCase(order)) {
                field = value.substring(0, index);
                desc = !"asc".equalsIgnoreCase(order);
            }
        }

        String normalized = field.replace("_", "").toLowerCase(Locale.ROOT);
        SFunction<Task, ?> column = switch (normalized) {
            case "updatedat" -> Task::getUpdatedAt;
            case "name" -> Task::getName;
            case "calledcount" -> Task::getCalledCount;
            case "connectedcount" -> Task::getConnectedCount;
            case "intentcount" -> Task::getIntentCount;
            case "totalcontacts" -> Task::getTotalContacts;
            case "scheduledat" -> Task::getScheduledAt;
            case "startedat" -> Task::getStartedAt;
            case "finishedat" -> Task::getFinishedAt;
            case "status" -> Task::getStatus;
            case "priority" -> Task::getPriority;
            default -> Task::getCreatedAt;
        };
        // 注意：MyBatis-Plus 的 orderBy(condition, isAsc, column) 第二个参数是「是否升序」，
        // 不是「是否降序」，这里必须取反。
        wrapper.orderBy(true, !desc, column);
        wrapper.orderByDesc(Task::getId);
    }

    private void validateCallWindow(String callWindow) {
        String[] parts = callWindow.split("-", 2);
        if (parts.length != 2) {
            throw BizException.paramInvalid("callWindow", "呼叫时段格式应为 HH:mm-HH:mm");
        }
        try {
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            if (!start.isBefore(end)) {
                throw BizException.paramInvalid("callWindow", "呼叫时段开始时间必须早于结束时间");
            }
        } catch (DateTimeParseException ex) {
            throw BizException.paramInvalid("callWindow", "呼叫时段格式应为 HH:mm-HH:mm");
        }
    }

    private boolean existsName(String creator, String name, String excludeId) {
        LambdaQueryWrapper<Task> wrapper = Wrappers.<Task>lambdaQuery()
                .eq(Task::getCreatorName, creator)
                .eq(Task::getName, name);
        if (StringUtils.hasText(excludeId)) {
            wrapper.ne(Task::getId, excludeId);
        }
        Long count = taskMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private String uniqueCopyName(String creator, String sourceName) {
        String suffix = " 副本";
        String base = sourceName + suffix;
        if (base.length() > 30) {
            base = sourceName.substring(0, Math.max(1, 30 - suffix.length())) + suffix;
        }
        String candidate = base;
        int index = 2;
        while (existsName(creator, candidate, null)) {
            String numberSuffix = " " + index;
            int maxBaseLength = 30 - numberSuffix.length();
            candidate = (base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base) + numberSuffix;
            index++;
        }
        return candidate;
    }

    private synchronized String nextTaskId() {
        String prefix = "T" + LocalDate.now(ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        String maxId = taskMapper.selectMaxIdByPrefix(prefix);
        int sequence = 1;
        if (StringUtils.hasText(maxId) && maxId.length() > prefix.length()) {
            try {
                sequence = Integer.parseInt(maxId.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                sequence = 1;
            }
        }
        return prefix + String.format("%03d", sequence);
    }

    private void addEvent(String taskId, String type, String message) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setType(type);
        event.setMessage(message);
        event.setCreatedAt(LocalDateTime.now(ZONE));
        taskEventMapper.insert(event);
    }

    private TaskEventDTO toEventDTO(TaskEvent event) {
        TaskEventDTO dto = new TaskEventDTO();
        BeanUtils.copyProperties(event, dto);
        return dto;
    }

    private Map<String, Long> buildResultDistribution(String taskId) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("CONNECTED", 0L);
        distribution.put("NO_ANSWER", 0L);
        distribution.put("REJECTED", 0L);
        distribution.put("INVALID", 0L);
        distribution.put("POWER_OFF", 0L);
        for (NameCountRow row : callResultMapper.countByResult(taskId)) {
            distribution.put(row.getName(), row.getCnt() == null ? 0L : row.getCnt());
        }
        return distribution;
    }

    private Map<String, Long> buildIntentDistribution(String taskId) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("A", 0L);
        distribution.put("B", 0L);
        distribution.put("C", 0L);
        distribution.put("D", 0L);
        for (NameCountRow row : callResultMapper.countByIntent(taskId)) {
            distribution.put(row.getName(), row.getCnt() == null ? 0L : row.getCnt());
        }
        return distribution;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
