package com.yicheng.outreach;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能语音外呼任务看板后端启动类。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.yicheng.outreach.mapper")
public class OutreachApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutreachApplication.class, args);
    }
}
