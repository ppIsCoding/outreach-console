import { ApiError, httpRequest } from './client'
import * as db from './mock/db'
import {
  mapLines,
  mapScripts,
  mapStatusCounts,
  mapSummary,
  mapTask,
  mapTaskDetail,
  mapTaskPage,
  toBackendCreate,
  toBackendQuery,
  type BackendLine,
  type BackendScript,
  type BackendStatusCount,
  type BackendSummary,
  type BackendTask,
  type BackendTaskDetail,
} from './http-mappers'
import type {
  CallLine,
  CallResult,
  CreateTaskPayload,
  PageResult,
  Script,
  StatusCount,
  Summary,
  Task,
  TaskAction,
  TaskDetail,
  TaskQuery,
} from './types'

const MODE = import.meta.env.VITE_API_MODE ?? 'mock'
export const IS_MOCK = MODE === 'mock'

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function mapError(error: unknown): never {
  const message = error instanceof Error ? error.message : ''
  if (message === 'TASK_NOT_FOUND') throw new ApiError(40401, '任务不存在或已被删除')
  if (message === 'VERSION_CONFLICT') throw new ApiError(40901, '任务状态已被他人修改，请刷新后重试')
  if (message === 'ILLEGAL_TRANSITION') throw new ApiError(40901, '当前状态不允许该操作')
  throw error
}

interface MockOptions {
  /** 在「请求失败」演示场景下仍然成功（用于制造"统计成功 / 列表失败"的部分失败） */
  allowInError?: boolean
}

async function mockCall<T>(producer: () => T, options: MockOptions = {}): Promise<T> {
  const scenario = db.getScenario()

  if (scenario === 'loading') {
    await new Promise<never>(() => {})
  }

  await delay(160 + Math.random() * 200)

  if (scenario === 'denied') throw new ApiError(40300, '你的账号没有查看外呼任务的权限')
  if (scenario === 'error' && !options.allowInError) {
    throw new ApiError(50400, '请求超时，请稍后重试')
  }

  try {
    return producer()
  } catch (error) {
    return mapError(error)
  }
}

function toQuery(params: Record<string, unknown>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue
    search.set(key, String(value))
  }
  const query = search.toString()
  return query ? `?${query}` : ''
}

function emptyPage<T>(page = 1, size = 10): PageResult<T> {
  return { items: [], page, size, total: 0, totalPages: 1 }
}

export const api = {
  /* ---------------- dashboard ---------------- */
  summary(): Promise<Summary> {
    if (!IS_MOCK) {
      return httpRequest<BackendSummary>('/api/v1/dashboard/summary').then(mapSummary)
    }
    return mockCall(() => db.taskSummary(), { allowInError: true })
  },

  stats(): Promise<StatusCount[]> {
    if (!IS_MOCK) {
      return httpRequest<BackendStatusCount>('/api/v1/tasks/stats').then(mapStatusCounts)
    }
    return mockCall(() => db.taskStats())
  },

  /* ---------------- tasks ---------------- */
  listTasks(query: TaskQuery): Promise<PageResult<Task>> {
    if (!IS_MOCK) {
      const search = toQuery(toBackendQuery(query as unknown as Record<string, unknown>))
      return httpRequest<PageResult<BackendTask>>(`/api/v1/tasks${search}`).then(mapTaskPage)
    }
    return mockCall(() => {
      const scenario = db.getScenario()
      if (scenario === 'empty' || scenario === 'emptyFilter') {
        return emptyPage<Task>(query.page ?? 1, query.size ?? 10)
      }
      return db.listTasks(query)
    })
  },

  getTask(id: string): Promise<TaskDetail> {
    if (!IS_MOCK) {
      return httpRequest<BackendTaskDetail>(`/api/v1/tasks/${id}`).then(mapTaskDetail)
    }
    return mockCall(() => db.getTask(id))
  },

  createTask(payload: CreateTaskPayload): Promise<Task> {
    if (!IS_MOCK) {
      return httpRequest<BackendTask>('/api/v1/tasks', {
        method: 'POST',
        body: JSON.stringify(toBackendCreate(payload)),
      }).then(mapTask)
    }
    return mockCall(() => db.createTask(payload))
  },

  patchStatus(id: string, action: TaskAction, version: number): Promise<Task> {
    if (!IS_MOCK) {
      return httpRequest<BackendTask>(`/api/v1/tasks/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ action, version }),
      }).then(mapTask)
    }
    return mockCall(() => db.patchStatus(id, action, version))
  },

  duplicateTask(id: string): Promise<Task> {
    if (!IS_MOCK) {
      return httpRequest<BackendTask>(`/api/v1/tasks/${id}/duplicate`, { method: 'POST' }).then(mapTask)
    }
    return mockCall(() => db.duplicateTask(id))
  },

  removeTask(id: string): Promise<void> {
    if (!IS_MOCK) return httpRequest<void>(`/api/v1/tasks/${id}`, { method: 'DELETE' })
    return mockCall(() => db.removeTask(id))
  },

  listResults(id: string, page = 1, size = 8): Promise<PageResult<CallResult>> {
    if (!IS_MOCK) {
      return httpRequest<PageResult<CallResult>>(`/api/v1/tasks/${id}/results${toQuery({ page, size })}`)
    }
    return mockCall(() => db.listResults(id, page, size))
  },

  /* ---------------- meta ---------------- */
  scripts(): Promise<Script[]> {
    if (!IS_MOCK) return httpRequest<BackendScript[]>('/api/v1/meta/scripts').then(mapScripts)
    return mockCall(() => db.scripts())
  },

  lines(): Promise<CallLine[]> {
    if (!IS_MOCK) return httpRequest<BackendLine[]>('/api/v1/meta/lines').then(mapLines)
    return mockCall(() => db.lines())
  },
}
