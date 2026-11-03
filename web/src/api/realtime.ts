import { ref } from 'vue'
import { IS_MOCK } from './index'
import { subscribeProgress } from './mock/db'
import type { TaskStatus } from './types'

export type ConnectionState = 'mock' | 'connecting' | 'live' | 'offline'

/** 顶栏“实时同步”指示器的真实连接状态。 */
export const connectionState = ref<ConnectionState>(IS_MOCK ? 'mock' : 'connecting')

export interface ProgressPayload {
  taskId: string
  calledCount: number
  connectedCount: number
  status: TaskStatus
}

/**
 * 订阅任务进度。
 * - mock 模式：本地定时器模拟调度器推进
 * - http 模式：连接 Spring Boot 的 /ws/tasks，断线指数退避重连
 */
export function subscribeTaskProgress(onMessage: (payload: ProgressPayload) => void): () => void {
  if (IS_MOCK) {
    connectionState.value = 'mock'
    return subscribeProgress(onMessage)
  }

  let socket: WebSocket | null = null
  let closed = false
  let attempt = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  const connect = () => {
    connectionState.value = 'connecting'
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    socket = new WebSocket(`${protocol}://${window.location.host}/ws/tasks`)

    socket.onopen = () => {
      attempt = 0
      connectionState.value = 'live'
      socket?.send(JSON.stringify({ type: 'subscribe', taskIds: [] }))
    }

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data as string)
        if (message.type !== 'task.progress') return
        onMessage({
          taskId: message.taskId,
          calledCount: message.payload.calledCount,
          connectedCount: message.payload.connectedCount,
          status: message.payload.status,
        })
      } catch {
        /* 忽略无法解析的消息 */
      }
    }

    socket.onclose = () => {
      if (closed) return
      connectionState.value = 'offline'
      const wait = Math.min(30_000, 1000 * 2 ** attempt)
      attempt += 1
      reconnectTimer = setTimeout(connect, wait)
    }
  }

  connect()

  return () => {
    closed = true
    connectionState.value = 'offline'
    if (reconnectTimer) clearTimeout(reconnectTimer)
    socket?.close()
  }
}
