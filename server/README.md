# 智能语音外呼任务看板 · 后端

Spring Boot 3.5 + Java 21 + MyBatis-Plus 3.5.9 + H2（默认）/ MySQL 8（可选）。

零配置启动：首次运行自动建表并写入 24 条演示任务，无需安装数据库。

---

## 1. 启动

环境要求：JDK 21、Maven 3.9+。

```bash
cd server

# 开发模式
mvn spring-boot:run

# 或打包后运行
mvn -DskipTests package
java -jar target/outreach-server-1.0.0.jar
```

启动后：

| 地址 | 说明 |
|---|---|
| `http://localhost:8080/api/v1` | REST API 前缀 |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/h2-console` | H2 控制台 |

H2 控制台连接信息：

```text
JDBC URL: jdbc:h2:file:./data/outreach;MODE=MySQL;DATABASE_TO_LOWER=TRUE
User Name: sa
Password:  （留空）
```

数据文件位于 `server/data/outreach.mv.db`。`schema.sql` / `data.sql` 在每次启动时执行，`data.sql` 会先清空再写入种子数据，因此重启会重置演示进度。

---

## 2. 切换到 MySQL 8

先创建数据库：

```sql
CREATE DATABASE outreach DEFAULT CHARACTER SET utf8mb4;
```

使用 `mysql` profile 启动：

```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="mysql"; mvn spring-boot:run

# 或直接指定
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

连接参数可用环境变量覆盖：

| 环境变量 | 默认值 |
|---|---|
| `MYSQL_HOST` | `localhost` |
| `MYSQL_PORT` | `3306` |
| `MYSQL_DATABASE` | `outreach` |
| `MYSQL_USERNAME` | `root` |
| `MYSQL_PASSWORD` | `root` |

MySQL profile 同样会执行 `schema.sql` + `data.sql`（含清空逻辑），仅建议用于本地演示。生产环境请将 `spring.sql.init.mode` 改为 `never` 并使用正式迁移工具。

---

## 3. 统一响应与错误码

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "a1b2c3d4e5f6"
}
```

| code | HTTP | 含义 |
|---|---|---|
| `0` | 200 / 201 | 成功 |
| `40001` | 400 | 参数校验失败，`data` 为 `{字段: 提示}` |
| `40401` | 404 | 任务不存在 / 接口不存在 |
| `40901` | 409 | 状态冲突（非法流转或乐观锁失败） |
| `50000` | 500 | 服务端异常 |

请求头 `X-Trace-Id` 可传入自定义链路 ID；未传时服务端生成，并同时写入响应头与响应体。

---

## 4. 接口速查

所有接口前缀：`/api/v1`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/dashboard/summary` | 概览指标（含环比与近 7 日趋势） |
| GET | `/tasks` | 任务分页查询 |
| GET | `/tasks/stats` | 各状态计数（含 `all`） |
| GET | `/tasks/{id}` | 任务详情（含最近 20 条状态事件） |
| POST | `/tasks` | 创建任务，成功返回 201 |
| PATCH | `/tasks/{id}/status` | 状态流转 + 乐观锁 |
| POST | `/tasks/{id}/duplicate` | 复制任务，成功返回 201 |
| DELETE | `/tasks/{id}` | 删除任务（仅 `DRAFT`） |
| GET | `/tasks/{id}/results` | 通话结果分页 |
| GET | `/meta/scripts` | 话术模板列表 |
| GET | `/meta/lines` | 线路及实时并发占用 |
| WS | `/ws/tasks` | 任务进度推送 |

### 4.1 任务分页

```http
GET /api/v1/tasks?status=running&keyword=双十一&scriptId=S002&dateRange=7d&page=1&size=10&sort=createdAt,desc
```

| 参数 | 说明 |
|---|---|
| `status` | `ALL` / `DRAFT` / `SCHEDULED` / `RUNNING` / `PAUSED` / `COMPLETED` / `FAILED` / `TERMINATED` / `CANCELLED`，大小写不敏感 |
| `keyword` | 匹配任务名称或任务 ID |
| `scriptId` | 话术模板 ID |
| `dateRange` | `today` / `7d` / `30d` / `custom` |
| `startDate` / `endDate` | `dateRange=custom` 时使用，格式 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `page` | 从 1 开始，默认 1 |
| `size` | 默认 10，上限 100 |
| `sort` | 如 `createdAt,desc`、`calledCount,asc`、`-updatedAt` |

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [],
    "page": 1,
    "size": 10,
    "total": 24,
    "totalPages": 3
  },
  "traceId": "..."
}
```

### 4.2 状态计数

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "all": 24,
    "draft": 1,
    "scheduled": 5,
    "running": 3,
    "paused": 2,
    "completed": 11,
    "failed": 2,
    "terminated": 0,
    "cancelled": 0
  },
  "traceId": "..."
}
```

### 4.3 创建任务

```http
POST /api/v1/tasks
Content-Type: application/json

{
  "name": "双十一新客首访",
  "scriptId": "S001",
  "lineId": "L001",
  "priority": "HIGH",
  "totalContacts": 500,
  "concurrency": 10,
  "retryLimit": 1,
  "retryIntervalMin": 30,
  "callWindow": "09:00-20:00",
  "scheduledAt": "2026-11-03 18:00:00",
  "creatorName": "王敏"
}
```

校验规则：

| 字段 | 规则 |
|---|---|
| `name` | 2–30 字，同一创建人下不可重名 |
| `totalContacts` | 1–50000 |
| `concurrency` | 1–50 |
| `retryLimit` | 0–3 |
| `retryIntervalMin` | 5–1440 |
| `callWindow` | `HH:mm-HH:mm`，开始必须早于结束 |
| `scheduledAt` | 可空；非空时需晚于当前时间 5 分钟 |

创建后状态为 `DRAFT`，`version=0`。

### 4.4 状态流转

```http
PATCH /api/v1/tasks/T20261103007/status
Content-Type: application/json

{ "action": "PAUSE", "version": 3 }
```

状态机：

```text
DRAFT     --SUBMIT--> SCHEDULED
SCHEDULED --START--> RUNNING    --CANCEL--> CANCELLED
RUNNING   --PAUSE--> PAUSED     --TERMINATE--> TERMINATED
RUNNING   --COMPLETE--> COMPLETED
RUNNING   --FAIL--> FAILED
PAUSED    --RESUME--> RUNNING   --TERMINATE--> TERMINATED
```

- 非法流转返回 `40901`。
- `WHERE id = ? AND version = ?`，影响行数为 0 返回 `40901`。
- 删除仅允许 `DRAFT`，否则返回 `40901`。

### 4.5 通话结果分页

```http
GET /api/v1/tasks/T20261103007/results?page=1&size=10&result=CONNECTED&intentLevel=A
```

`result` 可选值：`CONNECTED` / `NO_ANSWER` / `REJECTED` / `INVALID` / `POWER_OFF`；`intentLevel` 可选值：`A` / `B` / `C` / `D`。

### 4.6 概览指标

`GET /api/v1/dashboard/summary` 主要字段：

| 字段 | 口径 |
|---|---|
| `todayCalledCount` | 今日启动任务的 `calledCount` 合计 |
| `todayConnectedCount` | 今日启动任务的 `connectedCount` 合计 |
| `todayCalledCountChangeRate` | 相对昨日的变化百分比 |
| `connectRate` | 今日接通数 / 今日已呼数 |
| `connectRateBenchmark` | 行业基准，当前固定 35.0 |
| `runningTaskCount` / `runningTasks` | 执行中任务数 + 前 3 个任务进度 |
| `intentCustomerCount` | 今日启动任务的 `intentCount` 合计 |
| `avgCallDurationSec` | 今日累计通话秒数 / 今日接通数 |
| `taskCompletionRate` | 已完成任务数 / 全部任务数 |
| `completedTodayCount` | 今日完成的任务数 |
| `trend` | 近 7 日 `{date, calledCount, connectedCount, connectRate}` |
| `*ChangeRate` | 环比百分比，分母为 0 时返回 `null` |

### 4.7 WebSocket

连接：

```js
const ws = new WebSocket('ws://localhost:8080/ws/tasks')

ws.onopen = () => {
  ws.send(JSON.stringify({
    type: 'subscribe',
    taskIds: ['T20261103007', 'T20261103008']
  }))
}

ws.onmessage = (event) => {
  const message = JSON.parse(event.data)
  // { type: 'task.progress', taskId: 'T20261103007',
  //   payload: { calledCount: 1240, connectedCount: 512, status: 'RUNNING' } }
}
```

- 订阅成功返回 `{"type":"subscribed","taskIds":[...]}`。
- 未订阅时默认推送执行中的前 10 个任务，方便零配置联调。
- 支持 `{"type":"ping"}`，返回 `{"type":"pong"}`。
- 发送异常会自动移除连接。

---

## 5. 定时模拟与种子数据

- `DialerSimulator` 每 2 秒推进一次 `RUNNING` 任务：
  - `calledCount += max(1, concurrency / 6)`
  - 约 38% 概率 `connectedCount += 1`
  - 约 8% 概率 `intentCount += 1`
  - 接通时累加 20–60 秒通话时长
  - 同时写入一条 `call_result`
  - `calledCount >= totalContacts` 时置为 `COMPLETED` 并写 `finished_at`
- 测试 profile 下通过 `app.simulator.enabled=false` 关闭模拟器。

`data.sql` 预置：

- 24 条任务：草稿 1、待执行 5、执行中 3、已暂停 2、已完成 11、异常 2。
- 5 条话术、3 条线路、约 230 条通话结果、若干状态事件。
- 边界样本：
  - `T20261103001`：名单量为 0 的草稿。
  - `T20261103002`：`calledCount=0` 且 `totalContacts>0` 的待执行任务。
  - `T20261103006`：超过 30 字的超长任务名。
  - `T20261103023`：接通率 15% 的异常任务。

> 说明：文档要求“24 条任务”且“包含 0 名单草稿”，两者相加会使已完成任务数少 1 条，因此种子数据中已完成任务为 11 条（其余状态数量与文档一致）。`TERMINATED` / `CANCELLED` 可通过状态机接口从执行中/待执行任务演示产生。

---

## 6. 测试

```bash
cd server
mvn test
```

- `TaskStateMachineTest`：合法流转、非法流转、允许动作列表。
- `TaskServiceTest`：创建任务、乐观锁冲突、非法流转、重名校验。

当前结果：`Tests run: 10, Failures: 0, Errors: 0`。

---

## 7. 工程说明

```text
src/main/java/com/yicheng/outreach
├── OutreachApplication.java
├── common/      统一响应、错误码、异常处理、traceId、分页
├── config/      CORS、MyBatis-Plus、OpenAPI、WebSocket、Jackson
├── controller/  TaskController、DashboardController、MetaController
├── mapper/      MyBatis-Plus Mapper 与聚合行
├── model/       entity / enums / dto
├── service/     TaskService、TaskStateMachine、DashboardService、DialerSimulator
└── ws/          TaskProgressHandler
```

- 时间字段统一 `LocalDateTime`，JSON 序列化为 `yyyy-MM-dd HH:mm:ss`；反序列化兼容 ISO-8601。
- 数据库列避开保留字：`call_line`、`call_status`、`event_type`、`used_concurrency`。
- CORS 放行 `http://localhost:5173` 与 `http://127.0.0.1:5173`。
- `springdoc` 2.6.x 在 Spring Framework 6.2 下关闭了 `override-with-generic-response`，以保证 `/v3/api-docs` 正常生成。
