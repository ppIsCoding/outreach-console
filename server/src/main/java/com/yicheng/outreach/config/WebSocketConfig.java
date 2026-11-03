package com.yicheng.outreach.config;

import com.yicheng.outreach.ws.TaskProgressHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 原生 WebSocket 配置：路径 {@code /ws/tasks}。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskProgressHandler taskProgressHandler;

    public WebSocketConfig(TaskProgressHandler taskProgressHandler) {
        this.taskProgressHandler = taskProgressHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskProgressHandler, "/ws/tasks")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
