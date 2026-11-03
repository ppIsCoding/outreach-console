/** 与后端 OpenAPI 契约保持一致的领域类型 */

export type TaskStatus =
  | 'DRAFT'
  | 'SCHEDULED'
  | 'RUNNING'
  | 'PAUSED'
  | 'COMPLETED'
  | 'FAILED'
  | 'TERMINATED'
  | 'CANCELLED'

export type TaskAction = 'SUBMIT' | 'START' | 'PAUSE' | 'RESUME' | 'TERMINATE' | 'CANCEL'

export type TaskPriority = 'NORMAL' | 'HIGH'

export interface Task {
  id: string
  name: string
  scriptId: string
  scriptName: string
  scriptVersion: string
  lineId: string
  lineName: string
  status: TaskStatus
  priority: TaskPriority
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

export interface TaskDetail extends Task {
  events: TaskEvent[]
}

export interface TaskEvent {
  id: number
  type: string
  message: string
  createdAt: string
}

export interface CallResult {
  id: number
  phone: string
  result: 'CONNECTED' | 'NO_ANSWER' | 'REJECTED' | 'INVALID' | 'POWER_OFF'
  durationSec: number
  intentLevel: 'A' | 'B' | 'C' | 'D' | null
  calledAt: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface StatusCount {
  status: TaskStatus | 'ALL'
  count: number
}

export interface RunningTaskBrief {
  id: string
  name: string
  progress: number
}

export interface Summary {
  todayCalls: number
  todayCallsDelta: number
  connectRate: number
  connectRateBaseline: number
  connectedCount: number
  unansweredCount: number
  runningCount: number
  concurrencyUsed: number
  concurrencyTotal: number
  runningTasks: RunningTaskBrief[]
  intentCount: number
  intentDelta: number
  avgDurationSec: number
  completionRate: number
  sparkline: number[]
}

export interface Script {
  id: string
  name: string
  industry: string
  version: string
}

export interface CallLine {
  id: string
  name: string
  provider: string
  capacity: number
  used: number
}

export interface TaskQuery {
  status?: TaskStatus | 'ALL'
  keyword?: string
  scriptId?: string
  range?: 'today' | '7d' | '30d' | 'all'
  page?: number
  size?: number
  sort?: string
}

export interface CreateTaskPayload {
  name: string
  scriptId: string
  lineId: string
  priority: TaskPriority
  source: 'UPLOAD' | 'POOL'
  totalContacts: number
  dedupe: boolean
  skipRecent: boolean
  scheduleMode: 'NOW' | 'SCHEDULED' | 'RECURRING'
  scheduledAt?: string | null
  concurrency: number
  retryLimit: number
  retryIntervalMin: number
  callWindowStart: string
  callWindowEnd: string
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  traceId: string
}
