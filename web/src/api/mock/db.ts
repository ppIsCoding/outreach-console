import type {
  CallResult,
  CreateTaskPayload,
  PageResult,
  StatusCount,
  Summary,
  Task,
  TaskAction,
  TaskDetail,
  TaskEvent,
  TaskQuery,
  TaskStatus,
} from '../types'
import { buildSeedTasks, LINES, SCRIPTS } from './seed'

export type Scenario = 'normal' | 'loading' | 'empty' | 'emptyFilter' | 'error' | 'denied'

const SCENARIO_KEY = 'outreach:scenario'
let scenario: Scenario = 'normal'

/**
 * 场景状态同时写入 sessionStorage：
 * 1. 保证不同模块实例读到的值一致（避免打包/别名导致的模块副本）
 * 2. 刷新页面后仍停留在当前演示场景，方便截图
 */
export function setScenario(next: Scenario) {
  scenario = next
  try {
    window.sessionStorage.setItem(SCENARIO_KEY, next)
  } catch {
    /* 隐私模式下忽略 */
  }
}

export function getScenario(): Scenario {
  try {
    const stored = window.sessionStorage.getItem(SCENARIO_KEY) as Scenario | null
    return stored ?? scenario
  } catch {
    return scenario
  }
}

let tasks: Task[] = buildSeedTasks()
let seq = tasks.length

/** 本地时间 `yyyy-MM-dd HH:mm:ss`（不能用 toISOString，那是 UTC，会比本地慢 8 小时） */
function nowStamp(): string {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return (
    `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  )
}

export function resetDb() {
  tasks = buildSeedTasks()
  seq = tasks.length
}

/* ------------------------------------------------------------------ */
/* 查询                                                                */
/* ------------------------------------------------------------------ */

function matches(task: Task, query: TaskQuery): boolean {
  if (query.status && query.status !== 'ALL' && task.status !== query.status) return false
  if (query.scriptId && task.scriptId !== query.scriptId) return false
  const kw = query.keyword?.trim().toLowerCase()
  if (kw && !task.name.toLowerCase().includes(kw) && !task.id.toLowerCase().includes(kw)) return false

  // 时间范围按本地自然日计算，避免 UTC 在东八区凌晨把“今天”算错。
  if (query.range && query.range !== 'all') {
    const createdAt = new Date(task.createdAt.replace(' ', 'T'))
    if (Number.isNaN(createdAt.getTime())) return true

    const start = new Date()
    start.setHours(0, 0, 0, 0)
    if (query.range === '7d') start.setDate(start.getDate() - 6)
    if (query.range === '30d') start.setDate(start.getDate() - 29)
    if (createdAt < start) return false
  }

  return true
}

export function listTasks(query: TaskQuery): PageResult<Task> {
  const page = Math.max(1, query.page ?? 1)
  const size = Math.min(100, Math.max(1, query.size ?? 10))
  const filtered = tasks.filter((task) => matches(task, query))
  const totalPages = Math.max(1, Math.ceil(filtered.length / size))
  const safePage = Math.min(page, totalPages)
  const start = (safePage - 1) * size
  return {
    items: filtered.slice(start, start + size),
    page: safePage,
    size,
    total: filtered.length,
    totalPages,
  }
}

export function scripts() {
  return SCRIPTS
}

export function lines() {
  return LINES
}

export function taskStats(): StatusCount[] {
  if (getScenario() === 'empty') return [{ status: 'ALL', count: 0 }]

  const counts = new Map<TaskStatus, number>()
  for (const task of tasks) counts.set(task.status, (counts.get(task.status) ?? 0) + 1)
  const result: StatusCount[] = [{ status: 'ALL', count: tasks.length }]
  for (const [status, count] of counts) result.push({ status, count })
  return result
}

export function getTask(id: string): TaskDetail {
  const task = tasks.find((item) => item.id === id)
  if (!task) throw new Error('TASK_NOT_FOUND')
  return { ...task, events: buildEvents(task) }
}

function buildEvents(task: Task): TaskEvent[] {
  const events: TaskEvent[] = [{ id: 1, type: 'CREATED', message: '任务创建', createdAt: task.createdAt }]
  if (task.startedAt) {
    events.push({
      id: 2,
      type: 'SCHEDULED',
      message: `调度成功 · 分配并发 ${task.concurrency}`,
      createdAt: task.startedAt,
    })
    events.push({ id: 3, type: 'STARTED', message: '开始执行', createdAt: task.startedAt })
  }
  if (task.status === 'PAUSED') {
    events.push({ id: 4, type: 'PAUSED', message: '任务暂停', createdAt: task.updatedAt })
  }
  if (task.finishedAt) {
    events.push({
      id: 5,
      type: task.status,
      message: task.status === 'COMPLETED' ? '任务执行完成' : '任务异常终止',
      createdAt: task.finishedAt,
    })
  }
  return events.reverse()
}

export function taskSummary(): Summary {
  if (getScenario() === 'empty') {
    return {
      todayCalls: 0,
      todayCallsDelta: 0,
      connectRate: 0,
      connectRateBaseline: 34.6,
      connectedCount: 0,
      unansweredCount: 0,
      runningCount: 0,
      concurrencyUsed: 0,
      concurrencyTotal: 80,
      runningTasks: [],
      intentCount: 0,
      intentDelta: 0,
      avgDurationSec: 0,
      completionRate: 0,
      sparkline: [0, 0, 0, 0, 0, 0, 0],
    }
  }

  const running = tasks.filter((task) => task.status === 'RUNNING')
  return {
    todayCalls: 12480,
    todayCallsDelta: 8.2,
    connectRate: 38.2,
    connectRateBaseline: 34.6,
    connectedCount: 4768,
    unansweredCount: 7712,
    runningCount: running.length,
    concurrencyUsed: 46,
    concurrencyTotal: 80,
    runningTasks: running.slice(0, 3).map((task) => ({
      id: task.id,
      name: task.name,
      progress: task.totalContacts ? Math.round((task.calledCount / task.totalContacts) * 100) : 0,
    })),
    intentCount: 126,
    intentDelta: 12,
    avgDurationSec: 102,
    completionRate: 78,
    sparkline: [42, 38, 47, 35, 24, 28, 32, 44, 39, 34, 18, 21, 24, 33, 27, 21, 13, 10],
  }
}

/* ------------------------------------------------------------------ */
/* 变更                                                                */
/* ------------------------------------------------------------------ */

const TRANSITIONS: Partial<Record<TaskStatus, Partial<Record<TaskAction, TaskStatus>>>> = {
  DRAFT: { SUBMIT: 'SCHEDULED' },
  SCHEDULED: { START: 'RUNNING', CANCEL: 'CANCELLED' },
  RUNNING: { PAUSE: 'PAUSED', TERMINATE: 'TERMINATED' },
  PAUSED: { RESUME: 'RUNNING', TERMINATE: 'TERMINATED' },
}

export function patchStatus(id: string, action: TaskAction, version: number): Task {
  const task = tasks.find((item) => item.id === id)
  if (!task) throw new Error('TASK_NOT_FOUND')
  if (task.version !== version) throw new Error('VERSION_CONFLICT')
  const next = TRANSITIONS[task.status]?.[action]
  if (!next) throw new Error('ILLEGAL_TRANSITION')

  task.status = next
  task.version += 1
  task.updatedAt = nowStamp()
  if (next === 'RUNNING') task.startedAt ??= task.updatedAt
  if (next === 'COMPLETED' || next === 'TERMINATED') task.finishedAt = task.updatedAt
  return { ...task }
}

export function createTask(payload: CreateTaskPayload): Task {
  const script = SCRIPTS.find((item) => item.id === payload.scriptId) ?? SCRIPTS[0]!
  seq += 1
  const now = new Date()
  const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const createdAt = nowStamp()
  const task: Task = {
    id: `T${stamp}${String(seq).padStart(3, '0')}`,
    name: payload.name,
    scriptId: script.id,
    scriptName: script.name,
    scriptVersion: script.version,
    lineId: payload.lineId,
    lineName: '联通-沈阳主线路',
    status: 'SCHEDULED',
    priority: payload.priority,
    totalContacts: payload.totalContacts,
    calledCount: 0,
    connectedCount: 0,
    intentCount: 0,
    totalDurationSec: 0,
    concurrency: payload.concurrency,
    retryLimit: payload.retryLimit,
    retryIntervalMin: payload.retryIntervalMin,
    callWindow: `${payload.callWindowStart}-${payload.callWindowEnd}`,
    scheduledAt: payload.scheduledAt ?? createdAt,
    startedAt: null,
    finishedAt: null,
    creatorName: '李思远',
    createdAt,
    updatedAt: createdAt,
    version: 0,
  }
  tasks = [task, ...tasks]
  return { ...task }
}

export function duplicateTask(id: string): Task {
  const source = tasks.find((item) => item.id === id)
  if (!source) throw new Error('TASK_NOT_FOUND')
  seq += 1
  const now = new Date()
  const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const createdAt = nowStamp()
  const copy: Task = {
    ...source,
    id: `T${stamp}${String(seq).padStart(3, '0')}`,
    name: `${source.name}（副本）`,
    status: 'DRAFT',
    calledCount: 0,
    connectedCount: 0,
    intentCount: 0,
    totalDurationSec: 0,
    startedAt: null,
    finishedAt: null,
    createdAt,
    updatedAt: createdAt,
    version: 0,
  }
  tasks = [copy, ...tasks]
  return { ...copy }
}

export function removeTask(id: string) {
  const task = tasks.find((item) => item.id === id)
  if (!task) throw new Error('TASK_NOT_FOUND')
  if (task.status !== 'DRAFT') throw new Error('ILLEGAL_TRANSITION')
  tasks = tasks.filter((item) => item.id !== id)
}

export function listResults(id: string, page = 1, size = 8): PageResult<CallResult> {
  const task = tasks.find((item) => item.id === id)
  if (!task) throw new Error('TASK_NOT_FOUND')
  const total = Math.min(task.calledCount, 240)
  const items: CallResult[] = Array.from({ length: Math.min(size, Math.max(0, total - (page - 1) * size)) }, (_, i) => {
    const seed = (id.charCodeAt(6) * 7 + (page - 1) * size + i) % 100
    const result =
      seed < 38 ? 'CONNECTED' : seed < 70 ? 'NO_ANSWER' : seed < 85 ? 'REJECTED' : seed < 95 ? 'INVALID' : 'POWER_OFF'
    return {
      id: (page - 1) * size + i + 1,
      phone: `13${String(seed).padStart(2, '0')}****${String(1000 + seed * 7).slice(-4)}`,
      result,
      durationSec: result === 'CONNECTED' ? 20 + (seed % 90) : 0,
      intentLevel: result === 'CONNECTED' ? (seed % 3 === 0 ? 'A' : seed % 3 === 1 ? 'B' : 'C') : null,
      calledAt: task.updatedAt,
    }
  })
  return { items, page, size, total, totalPages: Math.max(1, Math.ceil(total / size)) }
}

/* ------------------------------------------------------------------ */
/* 实时推进（模拟外呼调度器）                                            */
/* ------------------------------------------------------------------ */

type ProgressListener = (payload: {
  taskId: string
  calledCount: number
  connectedCount: number
  status: TaskStatus
}) => void
const listeners = new Set<ProgressListener>()
let timer: ReturnType<typeof setInterval> | null = null

export function subscribeProgress(listener: ProgressListener) {
  listeners.add(listener)
  if (!timer) {
    timer = setInterval(tick, 2500)
  }
  return () => {
    listeners.delete(listener)
    if (listeners.size === 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }
}

function tick() {
  for (const task of tasks) {
    if (task.status !== 'RUNNING') continue
    if (task.calledCount >= task.totalContacts) {
      task.status = 'COMPLETED'
      task.finishedAt = nowStamp()
      listeners.forEach((listener) =>
        listener({
          taskId: task.id,
          calledCount: task.calledCount,
          connectedCount: task.connectedCount,
          status: task.status,
        }),
      )
      continue
    }
    const step = Math.max(1, Math.round(task.concurrency / 6))
    task.calledCount = Math.min(task.totalContacts, task.calledCount + step)
    if (Math.random() < 0.38) task.connectedCount += 1
    if (Math.random() < 0.08) task.intentCount += 1
    listeners.forEach((listener) =>
      listener({
        taskId: task.id,
        calledCount: task.calledCount,
        connectedCount: task.connectedCount,
        status: task.status,
      }),
    )
  }
}
