package com.yicheng.outreach.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI outreachOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能语音外呼任务看板 API")
                        .version("v1")
                        .description("任务查询、创建、状态流转、通话结果与实时进度推送"))
                .servers(List.of(new Server().url("http://localhost:8080").description("本地开发")));
    }
}
