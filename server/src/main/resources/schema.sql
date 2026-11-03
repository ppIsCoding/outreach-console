-- 智能语音外呼任务看板 · H2 / MySQL 通用建表脚本
-- 约定：表名与列名均避开 MySQL / H2 保留字（line、result、type 已分别改名为 call_line、call_status、event_type）

CREATE TABLE IF NOT EXISTS script (
    id       VARCHAR(32) PRIMARY KEY,
    name     VARCHAR(64) NOT NULL,
    industry VARCHAR(32),
    version  VARCHAR(16),
    status   VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS call_line (
    id               VARCHAR(32) PRIMARY KEY,
    name             VARCHAR(64) NOT NULL,
    provider         VARCHAR(32),
    capacity         INT         NOT NULL DEFAULT 0,
    used_concurrency INT         NOT NULL DEFAULT 0,
    status           VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS task (
    id                 VARCHAR(32) PRIMARY KEY,
    name               VARCHAR(64)  NOT NULL,
    script_id          VARCHAR(32)  NOT NULL,
    line_id            VARCHAR(32)  NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    priority           VARCHAR(8)   NOT NULL DEFAULT 'NORMAL',
    total_contacts     INT          NOT NULL DEFAULT 0,
    called_count       INT          NOT NULL DEFAULT 0,
    connected_count    INT          NOT NULL DEFAULT 0,
    intent_count       INT          NOT NULL DEFAULT 0,
    total_duration_sec INT          NOT NULL DEFAULT 0,
    concurrency        TINYINT      NOT NULL DEFAULT 10,
    retry_limit        TINYINT      NOT NULL DEFAULT 1,
    retry_interval_min INT          NOT NULL DEFAULT 30,
    call_window        VARCHAR(16)  NOT NULL DEFAULT '09:00-20:00',
    scheduled_at       TIMESTAMP    NULL,
    started_at         TIMESTAMP    NULL,
    finished_at        TIMESTAMP    NULL,
    creator_name       VARCHAR(32)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    version            INT          NOT NULL DEFAULT 0,
    INDEX idx_task_status_created (status, created_at DESC),
    INDEX idx_task_name (name),
    INDEX idx_task_script (script_id),
    INDEX idx_task_created (created_at DESC),
    INDEX idx_task_started (started_at)
);

CREATE TABLE IF NOT EXISTS call_result (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id      VARCHAR(32) NOT NULL,
    phone        VARCHAR(32),
    call_status  VARCHAR(16),
    duration_sec INT,
    hangup_cause VARCHAR(32),
    intent_level VARCHAR(4),
    called_at    TIMESTAMP,
    INDEX idx_result_task (task_id, called_at DESC),
    INDEX idx_result_called_at (called_at),
    INDEX idx_result_intent (task_id, intent_level)
);

CREATE TABLE IF NOT EXISTS task_event (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    VARCHAR(32) NOT NULL,
    event_type VARCHAR(32),
    message    VARCHAR(255),
    created_at TIMESTAMP,
    INDEX idx_event_task (task_id, created_at DESC)
);
