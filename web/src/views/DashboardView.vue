<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Plus, RefreshCw } from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { IS_MOCK } from '@/api'
import type { TaskAction, TaskStatus } from '@/api/types'
import { useTaskFilters } from '@/composables/useTaskFilters'
import {
  useDeleteTaskMutation,
  useDuplicateTaskMutation,
  useScriptsQuery,
  useStatusMutation,
  useSummaryQuery,
  useTaskListQuery,
  useTaskStatsQuery,
} from '@/composables/useTasks'
import { useUiStore } from '@/stores/ui'
import StatCards from '@/components/dashboard/StatCards.vue'
import MetricStrip from '@/components/dashboard/MetricStrip.vue'
import TaskTabs from '@/components/task/TaskTabs.vue'
import TaskToolbar from '@/components/task/TaskToolbar.vue'
import TaskTable from '@/components/task/TaskTable.vue'
import TaskPagination from '@/components/task/TaskPagination.vue'
import CreateTaskDrawer from '@/components/task/CreateTaskDrawer.vue'
import TaskDetailDrawer from '@/components/task/TaskDetailDrawer.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'

const ui = useUiStore()
const { filters, isDirty, apply, reset } = useTaskFilters()

const listQuery = useTaskListQuery()
const statsQuery = useTaskStatsQuery()
const summaryQuery = useSummaryQuery()
const scriptsQuery = useScriptsQuery()

const statusMutation = useStatusMutation()
const deleteMutation = useDeleteTaskMutation()
const duplicateMutation = useDuplicateTaskMutation()

const selectedIds = ref<string[]>([])

const pageData = computed(() => listQuery.data.value)
const tasks = computed(() => pageData.value?.items ?? [])
const total = computed(() => pageData.value?.total ?? 0)
const totalPages = computed(() => pageData.value?.totalPages ?? 1)

const denied = computed(() => listQuery.error.value instanceof ApiError && listQuery.error.value.code === 40300)
const failed = computed(() => listQuery.isError.value && !denied.value)
const summaryFailed = computed(() => summaryQuery.isError.value)
const filtered = computed(() => isDirty.value || filters.value.page > 1 || (IS_MOCK && ui.scenario === 'emptyFilter'))
const refreshing = computed(
  () => listQuery.isFetching.value || summaryQuery.isFetching.value || statsQuery.isFetching.value,
)

const listError = computed(() => listQuery.error.value)
const errorCode = computed(() => {
  const error = listError.value
  if (!(error instanceof ApiError)) return ''
  const labels: Record<number, string> = {
    40300: 'FORBIDDEN',
    40401: 'NOT_FOUND',
    40901: 'CONFLICT',
    50400: 'GATEWAY_TIMEOUT',
  }
  return `${error.code} · ${labels[error.code] ?? 'REQUEST_FAILED'}`
})
const errorMessage = computed(() => (listError.value instanceof ApiError ? listError.value.message : ''))

// 接口会把越界页码夹到最后一页；把真实页码回写到 URL，避免分页器显示 981–24。
watch(
  () => pageData.value?.page,
  (page) => {
    if (page && page !== filters.value.page) {
      apply({ page }, { resetPage: false })
    }
  },
)

// 批量操作只作用于当前页，筛选/翻页后清空选择，避免“已选 11 项但只提交 1 项”。
watch(
  () => ({ ...filters.value }),
  () => {
    selectedIds.value = []
  },
  { deep: true },
)

const detailOpen = computed({
  get: () => Boolean(ui.detailId),
  set: (value: boolean) => {
    if (!value) ui.closeDetail()
  },
})

const confirmState = reactive({
  open: false,
  title: '',
  description: '',
  confirmLabel: '确认',
  danger: true,
  action: () => {},
})

function askConfirm(options: {
  title: string
  description: string
  confirmLabel?: string
  danger?: boolean
  action: () => void
}) {
  confirmState.title = options.title
  confirmState.description = options.description
  confirmState.confirmLabel = options.confirmLabel ?? '确认'
  confirmState.danger = options.danger ?? true
  confirmState.action = options.action
  confirmState.open = true
}

async function refresh() {
  const results = await Promise.all([listQuery.refetch(), summaryQuery.refetch(), statsQuery.refetch()])
  if (results.some((result) => result.isError)) {
    ui.toast('刷新失败，请稍后重试', 'error')
    return
  }
  ui.toast('数据已刷新', 'info')
}

function changeStatus(status: TaskStatus | 'ALL') {
  apply({ status })
}

function toggle(id: string) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((item) => item !== id)
    : [...selectedIds.value, id]
}

function toggleAll() {
  const ids = tasks.value.map((task) => task.id)
  const allSelected = ids.length > 0 && ids.every((id) => selectedIds.value.includes(id))
  selectedIds.value = allSelected
    ? selectedIds.value.filter((id) => !ids.includes(id))
    : [...new Set([...selectedIds.value, ...ids])]
}

function handleAction(id: string, action: TaskAction) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return

  const labels: Record<TaskAction, string> = {
    SUBMIT: '任务已提交',
    START: '任务已开始执行',
    PAUSE: '任务已暂停',
    RESUME: '任务已恢复执行',
    TERMINATE: '任务已终止',
    CANCEL: '任务已取消',
  }
  const label = labels[action]
  statusMutation.mutate(
    { id, action, version: task.version },
    {
      onSuccess: () => ui.toast(label),
      onError: (error: Error) => ui.toast(error.message, 'error'),
    },
  )
}

function handleDelete(id: string) {
  const task = tasks.value.find((item) => item.id === id)
  if (!task) return
  askConfirm({
    title: `删除任务「${task.name}」？`,
    description: '该操作不可撤销。已导入的名单不会被删除，但任务配置与执行记录将一并移除。',
    confirmLabel: '删除',
    action: () => {
      deleteMutation.mutate(id, {
        onSuccess: () => {
          selectedIds.value = selectedIds.value.filter((item) => item !== id)
          ui.toast('任务已删除')
        },
        onError: (error: Error) => ui.toast(error.message, 'error'),
      })
    },
  })
}

function handleDuplicate(id: string) {
  duplicateMutation.mutate(id, {
    onSuccess: () => ui.toast('已复制为草稿任务'),
    onError: (error: Error) => ui.toast(error.message, 'error'),
  })
}

function bulkStatus(action: TaskAction) {
  const candidates = tasks.value.filter((task) => {
    if (!selectedIds.value.includes(task.id)) return false
    if (action === 'PAUSE') return task.status === 'RUNNING'
    return task.status === 'RUNNING' || task.status === 'PAUSED'
  })

  if (candidates.length === 0) {
    ui.toast('选中的任务中没有可执行该操作的任务', 'error')
    return
  }

  const run = async () => {
    const results = await Promise.allSettled(
      candidates.map((task) => statusMutation.mutateAsync({ id: task.id, action, version: task.version })),
    )
    const succeeded = results.filter((result) => result.status === 'fulfilled').length
    const failed = results.length - succeeded
    selectedIds.value = []

    if (failed > 0) {
      ui.toast(`已处理 ${succeeded} 个，${failed} 个失败`, 'error')
      return
    }
    ui.toast(`已处理 ${succeeded} 个任务`)
  }

  if (action === 'TERMINATE') {
    askConfirm({
      title: `终止选中的 ${candidates.length} 个任务？`,
      description: '终止后将立即停止外呼，未拨打的号码不再执行。该操作不可撤销。',
      confirmLabel: '终止任务',
      action: () => void run(),
    })
    return
  }

  void run()
}

function clearFilters() {
  reset()
  selectedIds.value = []
}
</script>

<template>
  <div class="mx-auto max-w-[1440px] px-7 pb-16 pt-6.5">
    <header class="mb-5.5 flex flex-wrap items-start gap-5">
      <div>
        <h1 class="text-[22px] font-semibold leading-tight tracking-tight">外呼任务看板</h1>
        <p class="mt-1 text-[12.5px] text-ink3">监控今日外呼执行情况，及时处理异常任务</p>
      </div>

      <div class="ml-auto flex items-center gap-2">
        <button type="button" class="btn" @click="refresh">
          <RefreshCw :size="15" :class="refreshing && 'animate-spin'" />
          刷新
        </button>
        <button type="button" class="btn btn-primary" @click="ui.openCreate()">
          <Plus :size="16" :stroke-width="2" />
          新建外呼任务
        </button>
      </div>
    </header>

    <StatCards
      :summary="summaryQuery.data.value"
      :loading="summaryQuery.isPending.value"
      :failed="summaryFailed"
      @retry="summaryQuery.refetch()"
      @filter-running="changeStatus('RUNNING')"
    />

    <MetricStrip :summary="summaryQuery.data.value" :loading="summaryQuery.isPending.value" />

    <section class="card overflow-hidden">
      <TaskTabs :status="filters.status" :counts="statsQuery.data.value" @change="changeStatus" />

      <TaskToolbar
        :keyword="filters.keyword"
        :script-id="filters.scriptId"
        :range="filters.range"
        :dirty="isDirty"
        :scripts="scriptsQuery.data.value"
        @update:keyword="apply({ keyword: $event })"
        @update:script-id="apply({ scriptId: $event })"
        @update:range="apply({ range: $event })"
        @reset="clearFilters"
      />

      <TaskTable
        :tasks="tasks"
        :loading="listQuery.isPending.value"
        :error="failed"
        :error-code="errorCode"
        :error-message="errorMessage"
        :denied="denied"
        :selected="selectedIds"
        :filtered="filtered"
        :size="filters.size"
        @toggle="toggle"
        @toggle-all="toggleAll"
        @detail="ui.openDetail($event)"
        @action="handleAction"
        @duplicate="handleDuplicate"
        @remove="handleDelete"
        @retry="listQuery.refetch()"
        @create="ui.openCreate()"
        @clear-filters="clearFilters"
      />

      <TaskPagination
        v-if="!listQuery.isPending.value && !failed && !denied && tasks.length > 0"
        :page="filters.page"
        :size="filters.size"
        :total="total"
        :total-pages="totalPages"
        :selected-count="selectedIds.length"
        @update:page="apply({ page: $event }, { resetPage: false })"
        @update:size="apply({ size: $event, page: 1 })"
        @bulk-pause="bulkStatus('PAUSE')"
        @bulk-terminate="bulkStatus('TERMINATE')"
      />
    </section>

    <CreateTaskDrawer v-model:open="ui.createOpen" />
    <TaskDetailDrawer v-model:open="detailOpen" :task-id="ui.detailId" />

    <ConfirmDialog
      v-model:open="confirmState.open"
      :title="confirmState.title"
      :description="confirmState.description"
      :confirm-label="confirmState.confirmLabel"
      :danger="confirmState.danger"
      @confirm="confirmState.action()"
    />
  </div>
</template>
