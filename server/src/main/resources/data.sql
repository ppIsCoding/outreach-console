-- 智能语音外呼任务看板 · 演示种子数据（幂等：每次启动先清空再写入）
-- 时间统一使用 CURRENT_TIMESTAMP 的相对偏移，保证任何一天启动都能看到“今日”数据。

DELETE FROM call_result;
DELETE FROM task_event;
DELETE FROM task;
DELETE FROM call_line;
DELETE FROM script;

-- ---------------------------------------------------------------------------
-- 话术模板
-- ---------------------------------------------------------------------------
INSERT INTO script (id, name, industry, version, status) VALUES
('S001', '新客首访-标准版', '电商', 'v2.1', 'ACTIVE'),
('S002', '活动通知-双十一', '电商', 'v1.3', 'ACTIVE'),
('S003', '存量唤醒-沉默30天', '零售', 'v1.0', 'ACTIVE'),
('S004', '满意度回访-NPS', '服务', 'v2.0', 'ACTIVE'),
('S005', '续费提醒-到期前7天', 'SaaS', 'v1.2', 'DRAFT');

-- ---------------------------------------------------------------------------
-- 外呼线路
-- ---------------------------------------------------------------------------
INSERT INTO call_line (id, name, provider, capacity, used_concurrency, status) VALUES
('L001', '华东电信-主线路', '中国电信', 100, 0, 'ACTIVE'),
('L002', '华北联通-备用线路', '中国联通', 60, 0, 'ACTIVE'),
('L003', '全国虚拟号-测试线路', '阿里云', 30, 0, 'DEGRADED');

-- ---------------------------------------------------------------------------
-- 24 条任务
-- 状态分布：草稿 1、待执行 5、执行中 3、已暂停 2、已完成 11、异常 2
-- 说明：为满足“24 条 + 必须包含 0 名单草稿”的边界要求，已完成任务取 11 条。
-- 边界样本：
--   T20261103001 名单量为 0 的草稿
--   T20261103002 calledCount=0 且 totalContacts>0 的待执行任务
--   T20261103006 任务名超过 30 字
--   T20261103023 接通率 15%（<20%）的异常任务
-- ---------------------------------------------------------------------------
INSERT INTO task (id, name, script_id, line_id, status, priority, total_contacts, called_count,
                  connected_count, intent_count, total_duration_sec, concurrency, retry_limit,
                  retry_interval_min, call_window, scheduled_at, started_at, finished_at,
                  creator_name, created_at, updated_at, version) VALUES
('T20261103001', '双十一新客首访-待补充名单', 'S001', 'L001', 'DRAFT', 'NORMAL',
 0, 0, 0, 0, 0, 10, 1, 30, '09:00-20:00', NULL, NULL, NULL,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 0),

('T20261103002', '沉默用户唤醒-华东批', 'S003', 'L001', 'SCHEDULED', 'HIGH',
 3200, 0, 0, 0, 0, 20, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP + INTERVAL '1' HOUR, NULL, NULL,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '24' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 0),

('T20261103003', '双十一活动通知-第一批', 'S002', 'L002', 'SCHEDULED', 'HIGH',
 8000, 0, 0, 0, 0, 30, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP + INTERVAL '2' HOUR, NULL, NULL,
 '李强', CURRENT_TIMESTAMP - INTERVAL '48' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 0),

('T20261103004', '续费提醒-到期前7天', 'S005', 'L001', 'SCHEDULED', 'NORMAL',
 1200, 0, 0, 0, 0, 10, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP + INTERVAL '24' HOUR, NULL, NULL,
 '陈晨', CURRENT_TIMESTAMP - INTERVAL '72' HOUR, CURRENT_TIMESTAMP - INTERVAL '3' HOUR, 0),

('T20261103005', '会员权益升级回访', 'S001', 'L003', 'SCHEDULED', 'NORMAL',
 450, 0, 0, 0, 0, 8, 0, 30, '09:00-20:00',
 CURRENT_TIMESTAMP + INTERVAL '72' HOUR, NULL, NULL,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '96' HOUR, CURRENT_TIMESTAMP - INTERVAL '4' HOUR, 0),

('T20261103006', '2026年双十一老客唤醒暨会员权益升级专项外呼任务（华东区第二批）', 'S003', 'L002', 'SCHEDULED', 'NORMAL',
 15000, 0, 0, 0, 0, 50, 3, 120, '09:00-20:00',
 CURRENT_TIMESTAMP + INTERVAL '24' HOUR, NULL, NULL,
 '李强', CURRENT_TIMESTAMP - INTERVAL '120' HOUR, CURRENT_TIMESTAMP - INTERVAL '5' HOUR, 0),

('T20261103007', '新客首访-今日批次', 'S001', 'L001', 'RUNNING', 'HIGH',
 5000, 1240, 512, 88, 21504, 20, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR, NULL,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '24' HOUR, CURRENT_TIMESTAMP, 3),

('T20261103008', '活动通知-双十一实时', 'S002', 'L002', 'RUNNING', 'HIGH',
 3200, 860, 301, 45, 12943, 15, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '1' HOUR, CURRENT_TIMESTAMP - INTERVAL '45' MINUTE, NULL,
 '李强', CURRENT_TIMESTAMP - INTERVAL '24' HOUR, CURRENT_TIMESTAMP, 2),

('T20261103009', '存量唤醒-沉默30天', 'S003', 'L003', 'RUNNING', 'NORMAL',
 8000, 2300, 690, 40, 28980, 30, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, NULL,
 '陈晨', CURRENT_TIMESTAMP - INTERVAL '48' HOUR, CURRENT_TIMESTAMP, 5),

('T20261103010', '满意度回访-NPS调研', 'S004', 'L001', 'PAUSED', 'NORMAL',
 2600, 900, 330, 40, 13860, 12, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '4' HOUR, CURRENT_TIMESTAMP - INTERVAL '3' HOUR, NULL,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '72' HOUR, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, 2),

('T20261103011', '续费提醒-重点客户', 'S005', 'L002', 'PAUSED', 'HIGH',
 4200, 1800, 520, 60, 21840, 18, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '5' HOUR, CURRENT_TIMESTAMP - INTERVAL '5' HOUR, NULL,
 '李强', CURRENT_TIMESTAMP - INTERVAL '48' HOUR, CURRENT_TIMESTAMP - INTERVAL '20' MINUTE, 1),

('T20261103012', '新客首访-早间批次', 'S001', 'L001', 'COMPLETED', 'HIGH',
 1800, 1800, 720, 120, 30240, 20, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '7' HOUR, CURRENT_TIMESTAMP - INTERVAL '6' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '48' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 4),

('T20261103013', '活动通知-短信未触达补呼', 'S002', 'L002', 'COMPLETED', 'NORMAL',
 950, 950, 380, 55, 15960, 12, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '9' HOUR, CURRENT_TIMESTAMP - INTERVAL '8' HOUR, CURRENT_TIMESTAMP - INTERVAL '3' HOUR,
 '李强', CURRENT_TIMESTAMP - INTERVAL '72' HOUR, CURRENT_TIMESTAMP - INTERVAL '3' HOUR, 3),

('T20261103014', '沉默用户唤醒-华南批', 'S003', 'L001', 'COMPLETED', 'NORMAL',
 3000, 3000, 1050, 150, 44100, 25, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '30' HOUR, CURRENT_TIMESTAMP - INTERVAL '29' HOUR, CURRENT_TIMESTAMP - INTERVAL '25' HOUR,
 '陈晨', CURRENT_TIMESTAMP - INTERVAL '120' HOUR, CURRENT_TIMESTAMP - INTERVAL '25' HOUR, 6),

('T20261103015', '会员权益升级回访-第二批', 'S001', 'L002', 'COMPLETED', 'NORMAL',
 1200, 1200, 420, 60, 17640, 15, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '33' HOUR, CURRENT_TIMESTAMP - INTERVAL '32' HOUR, CURRENT_TIMESTAMP - INTERVAL '28' HOUR,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '96' HOUR, CURRENT_TIMESTAMP - INTERVAL '28' HOUR, 2),

('T20261103016', '续费提醒-到期前7天-批量A', 'S005', 'L001', 'COMPLETED', 'HIGH',
 2200, 2200, 880, 140, 36960, 20, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '55' HOUR, CURRENT_TIMESTAMP - INTERVAL '54' HOUR, CURRENT_TIMESTAMP - INTERVAL '49' HOUR,
 '李强', CURRENT_TIMESTAMP - INTERVAL '144' HOUR, CURRENT_TIMESTAMP - INTERVAL '49' HOUR, 5),

('T20261103017', '满意度回访-NPS-批量A', 'S004', 'L003', 'COMPLETED', 'NORMAL',
 600, 600, 240, 30, 10080, 10, 0, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '58' HOUR, CURRENT_TIMESTAMP - INTERVAL '57' HOUR, CURRENT_TIMESTAMP - INTERVAL '53' HOUR,
 '陈晨', CURRENT_TIMESTAMP - INTERVAL '144' HOUR, CURRENT_TIMESTAMP - INTERVAL '53' HOUR, 1),

('T20261103018', '新客首访-渠道A', 'S001', 'L001', 'COMPLETED', 'NORMAL',
 1600, 1600, 560, 80, 23520, 18, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '80' HOUR, CURRENT_TIMESTAMP - INTERVAL '79' HOUR, CURRENT_TIMESTAMP - INTERVAL '74' HOUR,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '168' HOUR, CURRENT_TIMESTAMP - INTERVAL '74' HOUR, 3),

('T20261103019', '活动通知-会员日', 'S002', 'L002', 'COMPLETED', 'HIGH',
 2800, 2800, 1120, 180, 47040, 25, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '79' HOUR, CURRENT_TIMESTAMP - INTERVAL '78' HOUR, CURRENT_TIMESTAMP - INTERVAL '73' HOUR,
 '李强', CURRENT_TIMESTAMP - INTERVAL '168' HOUR, CURRENT_TIMESTAMP - INTERVAL '73' HOUR, 4),

('T20261103020', '沉默用户唤醒-西南批', 'S003', 'L003', 'COMPLETED', 'NORMAL',
 900, 900, 270, 35, 11340, 12, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '105' HOUR, CURRENT_TIMESTAMP - INTERVAL '104' HOUR, CURRENT_TIMESTAMP - INTERVAL '99' HOUR,
 '陈晨', CURRENT_TIMESTAMP - INTERVAL '192' HOUR, CURRENT_TIMESTAMP - INTERVAL '99' HOUR, 2),

('T20261103021', '新客首访-渠道B', 'S001', 'L001', 'COMPLETED', 'NORMAL',
 1300, 1300, 455, 65, 19110, 15, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '128' HOUR, CURRENT_TIMESTAMP - INTERVAL '127' HOUR, CURRENT_TIMESTAMP - INTERVAL '122' HOUR,
 '王敏', CURRENT_TIMESTAMP - INTERVAL '216' HOUR, CURRENT_TIMESTAMP - INTERVAL '122' HOUR, 2),

('T20261103022', '续费提醒-到期前30天', 'S005', 'L002', 'COMPLETED', 'NORMAL',
 700, 700, 280, 40, 11760, 10, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '151' HOUR, CURRENT_TIMESTAMP - INTERVAL '150' HOUR, CURRENT_TIMESTAMP - INTERVAL '146' HOUR,
 '李强', CURRENT_TIMESTAMP - INTERVAL '240' HOUR, CURRENT_TIMESTAMP - INTERVAL '146' HOUR, 1),

('T20261103023', '外呼异常-线路质量波动', 'S002', 'L003', 'FAILED', 'NORMAL',
 1200, 1200, 180, 8, 5400, 20, 1, 30, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '6' HOUR, CURRENT_TIMESTAMP - INTERVAL '5' HOUR, CURRENT_TIMESTAMP - INTERVAL '4' HOUR,
 '陈晨', CURRENT_TIMESTAMP - INTERVAL '24' HOUR, CURRENT_TIMESTAMP - INTERVAL '4' HOUR, 3),

('T20261103024', '外呼异常-空号率过高', 'S003', 'L002', 'FAILED', 'HIGH',
 3000, 3000, 870, 30, 26100, 25, 2, 60, '09:00-20:00',
 CURRENT_TIMESTAMP - INTERVAL '32' HOUR, CURRENT_TIMESTAMP - INTERVAL '31' HOUR, CURRENT_TIMESTAMP - INTERVAL '27' HOUR,
 '李强', CURRENT_TIMESTAMP - INTERVAL '96' HOUR, CURRENT_TIMESTAMP - INTERVAL '27' HOUR, 4);

-- ---------------------------------------------------------------------------
-- 通话结果：为每个有呼出记录的任务生成 10 条样例，覆盖接通/未接/拒接/空号/关机
-- ---------------------------------------------------------------------------
INSERT INTO call_result (task_id, phone, call_status, duration_sec, hangup_cause, intent_level, called_at)
SELECT t.id,
       CONCAT('138', RIGHT(CONCAT('00000000', seq.n * 137 + 1000), 8)),
       CASE
           WHEN MOD(seq.n, 10) < 4 THEN 'CONNECTED'
           WHEN MOD(seq.n, 10) < 7 THEN 'NO_ANSWER'
           WHEN MOD(seq.n, 10) = 7 THEN 'REJECTED'
           WHEN MOD(seq.n, 10) = 8 THEN 'INVALID'
           ELSE 'POWER_OFF'
       END,
       CASE WHEN MOD(seq.n, 10) < 4 THEN 20 + MOD(seq.n * 17, 70) ELSE 0 END,
       CASE
           WHEN MOD(seq.n, 10) < 4 THEN 'NORMAL'
           WHEN MOD(seq.n, 10) < 7 THEN 'NO_ANSWER'
           WHEN MOD(seq.n, 10) = 7 THEN 'REJECTED'
           WHEN MOD(seq.n, 10) = 8 THEN 'INVALID_NUMBER'
           ELSE 'POWER_OFF'
       END,
       CASE
           WHEN MOD(seq.n, 10) < 4 THEN
               CASE
                   WHEN MOD(seq.n, 3) = 0 THEN 'A'
                   WHEN MOD(seq.n, 5) = 0 THEN 'B'
                   WHEN MOD(seq.n, 7) = 0 THEN 'C'
                   ELSE 'D'
               END
           ELSE NULL
       END,
       COALESCE(t.started_at, t.created_at) - INTERVAL '5' MINUTE
FROM task t
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) seq
WHERE t.called_count > 0;

-- ---------------------------------------------------------------------------
-- 状态事件（详情抽屉时间线）
-- ---------------------------------------------------------------------------
INSERT INTO task_event (task_id, event_type, message, created_at)
SELECT id, 'CREATED', '任务创建', created_at FROM task;

INSERT INTO task_event (task_id, event_type, message, created_at)
SELECT id, 'STATUS_CHANGE', CONCAT('状态更新为 ', status), updated_at
FROM task WHERE status <> 'DRAFT';

INSERT INTO task_event (task_id, event_type, message, created_at)
SELECT id, 'STARTED', '任务开始执行', started_at
FROM task WHERE started_at IS NOT NULL;

INSERT INTO task_event (task_id, event_type, message, created_at)
SELECT id, 'PAUSED', '任务已暂停', updated_at
FROM task WHERE status = 'PAUSED';

INSERT INTO task_event (task_id, event_type, message, created_at)
SELECT id, 'COMPLETED', '任务全部外呼完成', finished_at
FROM task WHERE status = 'COMPLETED';

INSERT INTO task_event (task_id, event_type, message, created_at)
SELECT id, 'FAILED', '任务执行异常，已自动终止', finished_at
FROM task WHERE status = 'FAILED';
