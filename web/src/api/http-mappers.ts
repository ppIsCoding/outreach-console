/**
 * 后端 DTO → 前端领域模型 的映射层。
 *
 * 后端（Spring Boot）使用 todayCalledCount / dateRange 等命名，
 * 前端使用 todayCalls / range。两者通过这一层解耦：
 * - 后端字段调整时只改这里
 * - 前端组件永远只认 `types.ts`
 */

import type {
  CallLine,
  PageResult,
  Script,
  StatusCount,
  Summary,
  Task,
  TaskDetail,
  TaskEvent,
  TaskPriority,
  TaskStatus,
} from './types'

interface BackendRunningTask {
  id: string
  name: string
  calledCount: number
  totalContacts: number
  progress: number
  status: TaskStatus
}

interface BackendTrendPoint {
  date: string
  calledCount: number
  connectedCount: number
  connectRate: number | null
}

export interface BackendSummary {
  todayCalledCount: number
  todayConnectedCount: number
  todayCalledCountChangeRate: number | null
  connectRate: number | null
  connectRateBenchmark: number | null
  runningTaskCount: number
  runningTasks: BackendRunningTask[]
  concurrencyUsed: number
  concurrencyTotal: number
  intentCustomerCount: number
  intentCustomerCountChangeRate: number | null
  avgCallDurationSec: number
  taskCompletionRate: number | null
  trend: BackendTrendPoint[]
}

export interface BackendStatusCount {
  all: number
  draft: number
  scheduled: number
  running: number
  paused: number
  completed: number
  failed: number
  terminated: number
  cancelled: number
}

export interface BackendTask {
  id: string
  name: string
  scriptId: string
  scriptName: string
  lineId: string
  lineName: string
  status: TaskStatus
  priority: string
  totalContacts: number
  calledCount: number
  connectedCount: number
  intentCount: number
  totalDurationSec: number
  concurrency: number
  retryLimit: number
  retryIntervalMin: number
  callWindow: string
  scheduledAt: string | null
  startedAt: string | null
  finishedAt: string | null
  creatorName: string
  createdAt: string
  updatedAt: string
  version: number
}

export interface BackendTaskDetail extends BackendTask {
  events: TaskEvent[]
}

export interface BackendScript {
  id: string
  name: string
  industry: string
  version: string
}

export interface BackendLine {
  id: string
  name: string
  provider: string
  capacity: number
  used: number
}

const num = (value: number | null | undefined): number => (typeof value === 'number' ? value : 0)

export function mapSummary(raw: BackendSummary): Summary {
  const called = num(raw.todayCalledCount)
  const connected = num(raw.todayConnectedCount)

  return {
    todayCalls: called,
    todayCallsDelta: num(raw.todayCalledCountChangeRate),
    connectRate: num(raw.connectRate),
    connectRateBaseline: num(raw.connectRateBenchmark),
    connectedCount: connected,
    unansweredCount: Math.max(0, called - connected),
    runningCount: num(raw.runningTaskCount),
    concurrencyUsed: num(raw.concurrencyUsed),
    concurrencyTotal: num(raw.concurrencyTotal) || 80,
    runningTasks: (raw.runningTasks ?? []).map((task) => ({
      id: task.id,
      name: task.name,
      progress: Math.round(num(task.progress)),
    })),
    intentCount: num(raw.intentCustomerCount),
    intentDelta: Math.round(num(raw.intentCustomerCountChangeRate)),
    avgDurationSec: num(raw.avgCallDurationSec),
    completionRate: Math.round(num(raw.taskCompletionRate)),
    sparkline: (raw.trend ?? []).map((point) => num(point.calledCount)),
  }
}

export function mapStatusCounts(raw: BackendStatusCount): StatusCount[] {
  return [
    { status: 'ALL', count: num(raw.all) },
    { status: 'DRAFT', count: num(raw.draft) },
    { status: 'SCHEDULED', count: num(raw.scheduled) },
    { status: 'RUNNING', count: num(raw.running) },
    { status: 'PAUSED', count: num(raw.paused) },
    { status: 'COMPLETED', count: num(raw.completed) },
    { status: 'FAILED', count: num(raw.failed) },
    { status: 'TERMINATED', count: num(raw.terminated) },
    { status: 'CANCELLED', count: num(raw.cancelled) },
  ]
}

export function mapTask(raw: BackendTask): Task {
  return {
    id: raw.id,
    name: raw.name,
    scriptId: raw.scriptId,
    scriptName: raw.scriptName,
    // 后端暂未返回话术版本，留空由 UI 自动省略
    scriptVersion: '',
    lineId: raw.lineId,
    lineName: raw.lineName,
    status: raw.status,
    priority: (raw.priority === 'HIGH' ? 'HIGH' : 'NORMAL') as TaskPriority,
    totalContacts: num(raw.totalContacts),
    calledCount: num(raw.calledCount),
    connectedCount: num(raw.connectedCount),
    intentCount: num(raw.intentCount),
    totalDurationSec: num(raw.totalDurationSec),
    concurrency: num(raw.concurrency),
    retryLimit: num(raw.retryLimit),
    retryIntervalMin: num(raw.retryIntervalMin),
    callWindow: raw.callWindow,
    scheduledAt: raw.scheduledAt,
    startedAt: raw.startedAt,
    finishedAt: raw.finishedAt,
    creatorName: raw.creatorName,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
    version: num(raw.version),
  }
}

export function mapTaskPage(raw: PageResult<BackendTask>): PageResult<Task> {
  return { ...raw, items: (raw.items ?? []).map(mapTask) }
}

export function mapTaskDetail(raw: BackendTaskDetail): TaskDetail {
  return { ...mapTask(raw), events: raw.events ?? [] }
}

export function mapScripts(raw: BackendScript[]): Script[] {
  return raw.map((item) => ({
    id: item.id,
    name: item.name,
    industry: item.industry,
    version: item.version,
  }))
}

export function mapLines(raw: BackendLine[]): CallLine[] {
  return raw.map((item) => ({
    id: item.id,
    name: item.name,
    provider: item.provider,
    capacity: num(item.capacity),
    used: num(item.used),
  }))
}

/** 前端筛选条件 → 后端查询参数 */
export function toBackendQuery(query: {
  status?: string
  keyword?: string
  scriptId?: string
  range?: string
  page?: number
  size?: number
  sort?: string
}): Record<string, unknown> {
  return {
    status: query.status && query.status !== 'ALL' ? query.status : undefined,
    keyword: query.keyword,
    scriptId: query.scriptId,
    dateRange: query.range === 'all' ? undefined : query.range,
    page: query.page,
    size: query.size,
    sort: query.sort,
  }
}

/** 前端创建表单 → 后端创建请求 */
export function toBackendCreate(payload: {
  name: string
  scriptId: string
  lineId: string
  priority: string
  totalContacts: number
  concurrency: number
  retryLimit: number
  retryIntervalMin: number
  callWindowStart: string
  callWindowEnd: string
  scheduledAt?: string | null
}): Record<string, unknown> {
  return {
    name: payload.name,
    scriptId: payload.scriptId,
    lineId: payload.lineId,
    priority: payload.priority,
    totalContacts: payload.totalContacts,
    concurrency: payload.concurrency,
    retryLimit: payload.retryLimit,
    retryIntervalMin: payload.retryIntervalMin,
    callWindow: `${payload.callWindowStart}-${payload.callWindowEnd}`,
    scheduledAt: payload.scheduledAt ? payload.scheduledAt.replace('T', ' ') + ':00' : null,
    creatorName: '李思远',
  }
}
