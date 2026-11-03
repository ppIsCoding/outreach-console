package com.yicheng.outreach.ws;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yicheng.outreach.mapper.TaskMapper;
import com.yicheng.outreach.model.entity.Task;
import com.yicheng.outreach.model.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 任务进度 WebSocket 处理器。
 * <p>客户端发送 {@code {"type":"subscribe","taskIds":["T..."]}} 订阅任务；
 * 服务端每 2 秒推送 {@code {"type":"task.progress","taskId":"...","payload":{...}}}。</p>
 */
@Component
public class TaskProgressHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskProgressHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public TaskProgressHandler(TaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            if ("subscribe".equals(type)) {
                Set<String> taskIds = new CopyOnWriteArraySet<>();
                JsonNode array = root.path("taskIds");
                if (array.isArray()) {
                    array.forEach(node -> {
                        if (node.isTextual() && !node.asText().isBlank()) {
                            taskIds.add(node.asText());
                        }
                    });
                }
                subscriptions.put(session.getId(), taskIds);

                Map<String, Object> ack = new LinkedHashMap<>();
                ack.put("type", "subscribed");
                ack.put("taskIds", taskIds);
                send(session, ack);
            } else if ("ping".equals(type)) {
                Map<String, Object> pong = new LinkedHashMap<>();
                pong.put("type", "pong");
                send(session, pong);
            } else {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("type", "error");
                error.put("message", "不支持的消息类型: " + type);
                send(session, error);
            }
        } catch (Exception ex) {
            log.debug("WebSocket 消息解析失败: {}", ex.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("type", "error");
            error.put("message", "消息格式不正确，应为 JSON");
            send(session, error);
        }
    }

    @Scheduled(fixedRateString = "${app.websocket.push-interval-ms:2000}")
    public void pushProgress() {
        if (sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                remove(session);
                continue;
            }
            Set<String> taskIds = subscriptions.get(session.getId());
            List<Task> tasks;
            if (taskIds == null || taskIds.isEmpty()) {
                // 未订阅时默认推送执行中任务，方便前端零配置联调
                tasks = taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                        .eq(Task::getStatus, TaskStatus.RUNNING)
                        .last("LIMIT 10"));
            } else {
                tasks = taskMapper.selectBatchIds(taskIds);
            }
            for (Task task : tasks) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("calledCount", nvl(task.getCalledCount()));
                payload.put("connectedCount", nvl(task.getConnectedCount()));
                payload.put("status", task.getStatus());

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("type", "task.progress");
                message.put("taskId", task.getId());
                message.put("payload", payload);
                send(session, message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
        log.debug("WebSocket 连接关闭: {}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WebSocket 传输异常: {}", exception.getMessage());
        remove(session);
    }

    private void send(WebSocketSession session, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception ex) {
            log.debug("WebSocket 发送失败，移除连接 {}: {}", session.getId(), ex.getMessage());
            remove(session);
        }
    }

    private void remove(WebSocketSession session) {
        sessions.remove(session);
        subscriptions.remove(session.getId());
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception ignored) {
            // 连接已不可用，忽略
        }
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
