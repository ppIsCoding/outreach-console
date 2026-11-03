<script setup lang="ts">
import { computed, ref } from 'vue'
import { Copy, Pause, Play, Square } from 'lucide-vue-next'
import type { TaskAction } from '@/api/types'
import { useDuplicateTaskMutation, useStatusMutation, useTaskDetailQuery } from '@/composables/useTasks'
import { formatDuration, formatNumber, progressOf } from '@/lib/utils'
import { STATUS_META } from '@/lib/task-status'
import { useUiStore } from '@/stores/ui'
import AppDrawer from '@/components/ui/AppDrawer.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'

const open = defineModel<boolean>('open', { required: true })
const { taskId } = defineProps<{ taskId: string | null }>()

const ui = useUiStore()
const detailQuery = useTaskDetailQuery(computed(() => taskId))
const statusMutation = useStatusMutation()
const duplicateMutation = useDuplicateTaskMutation()

const task = computed(() => detailQuery.data.value)
const meta = computed(() => (task.value ? STATUS_META[task.value.status] : null))

const distribution = computed(() => {
  if (!task.value) return []
  const called = task.value.calledCount
  const connected = Math.min(task.value.connectedCount, called)
  const unanswered = Math.max(0, called - connected)

  // 未接通部分按固定比例拆分，余数补到“关机”，保证各项之和等于已呼量。
  const noAnswer = Math.round(unanswered * 0.45)
  const rejected = Math.round(unanswered * 0.25)
  const invalid = Math.round(unanswered * 0.15)
  const powerOff = Math.max(0, unanswered - noAnswer - rejected - invalid)

  return [
    { label: '接通', count: connected, color: '#0B6B5F' },
    { label: '无人接听', count: noAnswer, color: '#C2841A' },
    { label: '拒接', count: rejected, color: '#D9A441' },
    { label: '空号', count: invalid, color: '#C9644A' },
    { label: '关机', count: powerOff, color: '#B4B9C2' },
  ].map((item) => ({ ...item, percent: called ? Math.round((item.count / called) * 100) : 0 }))
})

const intents = computed(() => {
  const total = task.value?.intentCount ?? 0
  const high = Math.round(total * 0.35)
  const medium = Math.round(total * 0.4)
  const low = Math.max(0, total - high - medium)

  return [
    { label: 'A 高意向', count: high, color: '#0B6B5F' },
    { label: 'B 中意向', count: medium, color: '#15857A' },
    { label: 'C 低意向', count: low, color: '#8FBDB5' },
  ].map((item) => ({ ...item, percent: total ? Math.round((item.count / total) * 100) : 0 }))
})

const remainingText = computed(() => {
  if (!task.value) return ''
  switch (task.value.status) {
    case 'RUNNING': {
      const minutes = Math.max(
        1,
        Math.round(((task.value.totalContacts - task.value.calledCount) * 45) / 60 / Math.max(1, task.value.concurrency)),
      )
      return `剩余约 ${minutes} 分钟`
    }
    case 'PAUSED':
      return '已暂停'
    case 'SCHEDULED':
      return '等待调度'
    case 'DRAFT':
      return '尚未提交'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '执行异常'
    case 'TERMINATED':
      return '已终止'
    case 'CANCELLED':
      return '已取消'
    default:
      return ''
  }
})

const confirmOpen = ref(false)
const pendingAction = ref<TaskAction>('TERMINATE')

const confirmTitle = computed(() => {
  const name = task.value?.name ?? '任务'
  return pendingAction.value === 'CANCEL' ? `取消任务「${name}」？` : `终止任务「${name}」？`
})

const confirmDescription = computed(() =>
  pendingAction.value === 'CANCEL'
    ? '取消后任务将不再执行，已配置的名单与话术不会删除。'
    : '终止后将立即停止外呼，未拨打的号码不再执行。该操作不可撤销。',
)

const confirmLabel = computed(() => (pendingAction.value === 'CANCEL' ? '取消任务' : '终止任务'))

function askConfirm(action: 'CANCEL' | 'TERMINATE') {
  pendingAction.value = action
  confirmOpen.value = true
}

function runAction(action: TaskAction) {
  if (!task.value) return
  const labels: Record<TaskAction, string> = {
    SUBMIT: '任务已提交',
    START: '任务已开始执行',
    PAUSE: '任务已暂停',
    RESUME: '任务已恢复执行',
    TERMINATE: '任务已终止',
    CANCEL: '任务已取消',
  }
  statusMutation.mutate(
    { id: task.value.id, action, version: task.value.version },
    {
      onSuccess: () => {
        ui.toast(labels[action])
        void detailQuery.refetch()
      },
      onError: (error: Error) => ui.toast(error.message, 'error'),
    },
  )
}

function duplicate() {
  if (!task.value) return
  duplicateMutation.mutate(task.value.id, {
    onSuccess: () => {
      ui.toast('已复制为草稿任务')
      open.value = false
    },
    onError: (error: Error) => ui.toast(error.message, 'error'),
  })
}
</script>

<template>
  <AppDrawer v-model:open="open" :title="task?.name ?? '任务详情'" :subtitle="task ? `${task.id} · ${task.creatorName} 创建` : ''">
    <template #badge>
      <StatusBadge v-if="task" :status="task.status" class="mt-0.5" />
    </template>

    <div v-if="detailQuery.isPending.value" class="space-y-3">
      <div class="grid grid-cols-2 gap-3">
        <div v-for="index in 6" :key="index" class="skeleton h-[62px] rounded-[9px]" />
      </div>
      <div class="skeleton h-24 w-full" />
    </div>

    <div v-else-if="detailQuery.isError.value" class="py-16 text-center">
      <p class="text-sm text-ink2">任务详情加载失败</p>
      <p class="mt-1 text-xs text-ink4">{{ (detailQuery.error.value as Error)?.message }}</p>
      <button type="button" class="btn mt-4" @click="detailQuery.refetch()">重试</button>
    </div>

    <template v-else-if="task">
      <div class="mb-5 grid grid-cols-2 gap-3">
        <div class="rounded-[9px] border border-line bg-surface2 px-3 py-2.5">
          <div class="text-[11px] text-ink4">已呼 / 名单</div>
          <div class="num mt-1 text-[13px] font-medium">
            {{ formatNumber(task.calledCount) }} / {{ formatNumber(task.totalContacts) }}
          </div>
        </div>
        <div class="rounded-[9px] border border-line bg-surface2 px-3 py-2.5">
          <div class="text-[11px] text-ink4">接通率</div>
          <div class="num mt-1 text-[13px] font-medium">
            {{ task.calledCount ? `${((task.connectedCount / task.calledCount) * 100).toFixed(1)}%` : '—' }}
          </div>
        </div>
        <div class="rounded-[9px] border border-line bg-surface2 px-3 py-2.5">
          <div class="text-[11px] text-ink4">话术模板</div>
          <div class="mt-1 text-[13px] font-medium">{{ task.scriptName }} {{ task.scriptVersion }}</div>
        </div>
        <div class="rounded-[9px] border border-line bg-surface2 px-3 py-2.5">
          <div class="text-[11px] text-ink4">外呼线路</div>
          <div class="mt-1 text-[13px] font-medium">{{ task.lineName }}</div>
        </div>
        <div class="rounded-[9px] border border-line bg-surface2 px-3 py-2.5">
          <div class="text-[11px] text-ink4">并发数</div>
          <div class="num mt-1 text-[13px] font-medium">{{ task.concurrency }}</div>
        </div>
        <div class="rounded-[9px] border border-line bg-surface2 px-3 py-2.5">
          <div class="text-[11px] text-ink4">呼叫时段</div>
          <div class="num mt-1 text-[13px] font-medium">{{ task.callWindow }}</div>
        </div>
      </div>

      <h3 class="mb-3 flex items-center gap-2 text-xs font-semibold tracking-wide text-ink2 after:h-px after:flex-1 after:bg-line">
        进度
      </h3>
      <div class="mb-1.5 flex justify-between text-xs">
        <span class="text-ink3">已执行 {{ progressOf(task) }}%</span>
        <span class="num text-ink2">{{ remainingText }}</span>
      </div>
      <ProgressBar :value="progressOf(task)" :height="6" />

      <h3 class="mb-3 mt-6 flex items-center gap-2 text-xs font-semibold tracking-wide text-ink2 after:h-px after:flex-1 after:bg-line">
        通话结果分布
      </h3>
      <div class="space-y-2.5">
        <div v-for="item in distribution" :key="item.label" class="grid grid-cols-[64px_1fr_48px] items-center gap-2.5 text-xs">
          <span class="text-ink2">{{ item.label }}</span>
          <div class="h-1.5 overflow-hidden rounded-full bg-[#EFEDE7]">
            <div class="h-full rounded-full" :style="{ width: `${item.percent}%`, background: item.color }" />
          </div>
          <span class="num text-right text-[11.5px] text-ink3">{{ formatNumber(item.count) }}</span>
        </div>
      </div>

      <h3 class="mb-3 mt-6 flex items-center gap-2 text-xs font-semibold tracking-wide text-ink2 after:h-px after:flex-1 after:bg-line">
        意向等级
      </h3>
      <div class="space-y-2.5">
        <div v-for="item in intents" :key="item.label" class="grid grid-cols-[64px_1fr_48px] items-center gap-2.5 text-xs">
          <span class="text-ink2">{{ item.label }}</span>
          <div class="h-1.5 overflow-hidden rounded-full bg-[#EFEDE7]">
            <div class="h-full rounded-full" :style="{ width: `${item.percent}%`, background: item.color }" />
          </div>
          <span class="num text-right text-[11.5px] text-ink3">{{ formatNumber(item.count) }}</span>
        </div>
      </div>

      <h3 class="mb-3 mt-6 flex items-center gap-2 text-xs font-semibold tracking-wide text-ink2 after:h-px after:flex-1 after:bg-line">
        状态流转
      </h3>
      <ol class="relative pl-5 before:absolute before:bottom-2 before:left-[5px] before:top-1.5 before:w-px before:bg-line">
        <li v-for="event in task.events" :key="event.id" class="relative pb-4 last:pb-0">
          <span class="absolute -left-[18px] top-1 size-[7px] rounded-full border-2 border-brand bg-brand" />
          <div class="text-[12.5px]">{{ event.message }}</div>
          <div class="num mt-0.5 text-[11px] text-ink4">{{ event.createdAt }}</div>
        </li>
      </ol>

      <p class="mt-6 text-[11px] text-ink4">
        累计通话 {{ formatDuration(task.totalDurationSec) }} · 意向客户 {{ formatNumber(task.intentCount) }} 人
      </p>
    </template>

    <template #footer>
      <button
        v-if="task && meta?.actions.includes('TERMINATE')"
        type="button"
        class="btn border-[#E8CFC7] text-danger"
        :disabled="statusMutation.isPending.value"
        @click="askConfirm('TERMINATE')"
      >
        <Square :size="13" /> 终止任务
      </button>

      <button
        v-else-if="task && meta?.actions.includes('CANCEL')"
        type="button"
        class="btn border-[#E8CFC7] text-danger"
        :disabled="statusMutation.isPending.value"
        @click="askConfirm('CANCEL')"
      >
        <Square :size="13" /> 取消任务
      </button>

      <div class="ml-auto flex gap-2">
        <button type="button" class="btn" :disabled="duplicateMutation.isPending.value" @click="duplicate">
          <Copy :size="14" /> 复制任务
        </button>

        <button
          v-if="task?.status === 'DRAFT'"
          type="button"
          class="btn btn-primary"
          :disabled="statusMutation.isPending.value"
          @click="runAction('SUBMIT')"
        >
          <Play :size="14" /> 提交任务
        </button>

        <button
          v-else-if="task?.status === 'SCHEDULED'"
          type="button"
          class="btn btn-primary"
          :disabled="statusMutation.isPending.value"
          @click="runAction('START')"
        >
          <Play :size="14" /> 立即执行
        </button>

        <button
          v-else-if="task?.status === 'RUNNING'"
          type="button"
          class="btn btn-primary"
          :disabled="statusMutation.isPending.value"
          @click="runAction('PAUSE')"
        >
          <Pause :size="14" /> 暂停执行
        </button>

        <button
          v-else-if="task?.status === 'PAUSED'"
          type="button"
          class="btn btn-primary"
          :disabled="statusMutation.isPending.value"
          @click="runAction('RESUME')"
        >
          <Play :size="14" /> 恢复执行
        </button>
      </div>
    </template>
  </AppDrawer>

  <ConfirmDialog
    v-model:open="confirmOpen"
    :title="confirmTitle"
    :description="confirmDescription"
    :confirm-label="confirmLabel"
    danger
    @confirm="runAction(pendingAction)"
  />
</template>
