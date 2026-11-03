import { computed, onMounted, onUnmounted, type Ref } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { api } from '@/api'
import { subscribeTaskProgress } from '@/api/realtime'
import type { CreateTaskPayload, PageResult, Task, TaskAction, TaskStatus } from '@/api/types'
import { useTaskFilters } from './useTaskFilters'

export const taskKeys = {
  all: ['tasks'] as const,
  list: (filters: unknown) => ['tasks', 'list', filters] as const,
  stats: ['tasks', 'stats'] as const,
  detail: (id: string) => ['tasks', 'detail', id] as const,
  summary: ['dashboard', 'summary'] as const,
  scripts: ['meta', 'scripts'] as const,
  lines: ['meta', 'lines'] as const,
}

export function useTaskListQuery() {
  const { filters } = useTaskFilters()
  return useQuery({
    queryKey: computed(() => taskKeys.list(filters.value)),
    queryFn: () => api.listTasks(filters.value),
  })
}

export function useTaskStatsQuery() {
  return useQuery({ queryKey: taskKeys.stats, queryFn: () => api.stats() })
}

export function useSummaryQuery() {
  return useQuery({
    queryKey: taskKeys.summary,
    queryFn: () => api.summary(),
    refetchInterval: 20_000,
  })
}

export function useScriptsQuery() {
  return useQuery({ queryKey: taskKeys.scripts, queryFn: () => api.scripts(), staleTime: Infinity })
}

export function useLinesQuery() {
  return useQuery({ queryKey: taskKeys.lines, queryFn: () => api.lines(), staleTime: Infinity })
}

export function useTaskDetailQuery(id: Ref<string | null>) {
  return useQuery({
    queryKey: computed(() => taskKeys.detail(id.value ?? 'none')),
    queryFn: () => api.getTask(id.value as string),
    enabled: computed(() => Boolean(id.value)),
  })
}

/* ------------------------------------------------------------------ */
/* 状态流转：乐观更新 + 失败回滚                                        */
/* ------------------------------------------------------------------ */

const OPTIMISTIC_STATUS: Partial<Record<TaskAction, TaskStatus>> = {
  SUBMIT: 'SCHEDULED',
  START: 'RUNNING',
  PAUSE: 'PAUSED',
  RESUME: 'RUNNING',
  TERMINATE: 'TERMINATED',
  CANCEL: 'CANCELLED',
}

export function useStatusMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: { id: string; action: TaskAction; version: number }) =>
      api.patchStatus(input.id, input.action, input.version),

    onMutate: async (input) => {
      await queryClient.cancelQueries({ queryKey: taskKeys.all })

      const snapshots = queryClient.getQueriesData<PageResult<Task>>({ queryKey: taskKeys.all })
      const optimistic = OPTIMISTIC_STATUS[input.action]

      for (const [key, data] of snapshots) {
        if (!data || !Array.isArray((data as PageResult<Task>).items)) continue
        queryClient.setQueryData<PageResult<Task>>(key, {
          ...(data as PageResult<Task>),
          items: (data as PageResult<Task>).items.map((task) =>
            task.id === input.id && optimistic ? { ...task, status: optimistic } : task,
          ),
        })
      }

      return { snapshots }
    },

    onError: (_error, _input, context) => {
      context?.snapshots.forEach(([key, data]) => queryClient.setQueryData(key, data))
    },

    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: taskKeys.all })
      void queryClient.invalidateQueries({ queryKey: taskKeys.summary })
    },
  })
}

export function useCreateTaskMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateTaskPayload) => api.createTask(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: taskKeys.all })
      void queryClient.invalidateQueries({ queryKey: taskKeys.summary })
    },
  })
}

export function useDuplicateTaskMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.duplicateTask(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: taskKeys.all })
      void queryClient.invalidateQueries({ queryKey: taskKeys.summary })
    },
  })
}

export function useDeleteTaskMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.removeTask(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: taskKeys.all })
      void queryClient.invalidateQueries({ queryKey: taskKeys.summary })
    },
  })
}

/* ------------------------------------------------------------------ */
/* WebSocket 进度 → 直接写入查询缓存                                     */
/* ------------------------------------------------------------------ */

export function useTaskProgressSync() {
  const queryClient = useQueryClient()
  let unsubscribe: (() => void) | null = null

  onMounted(() => {
    unsubscribe = subscribeTaskProgress((payload) => {
      queryClient.setQueriesData<PageResult<Task>>({ queryKey: taskKeys.all }, (previous) => {
        if (!previous || !Array.isArray(previous.items)) return previous
        return {
          ...previous,
          items: previous.items.map((task) =>
            task.id === payload.taskId
              ? {
                  ...task,
                  calledCount: payload.calledCount,
                  connectedCount: payload.connectedCount,
                  status: payload.status,
                }
              : task,
          ),
        }
      })
    })
  })

  onUnmounted(() => unsubscribe?.())
}
