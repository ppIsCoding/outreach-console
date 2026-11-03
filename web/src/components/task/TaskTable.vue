<script setup lang="ts">
import { computed } from 'vue'
import { Copy, Pause, Play, Trash2 } from 'lucide-vue-next'
import type { Task, TaskAction } from '@/api/types'
import { connectRateOf, formatNumber, progressOf, relativeTime } from '@/lib/utils'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import ProgressBar from '@/components/ui/ProgressBar.vue'
import StateBlock from '@/components/ui/StateBlock.vue'

const {
  tasks = [],
  loading = false,
  error = false,
  errorCode = '',
  errorMessage = '',
  denied = false,
  selected = [],
  filtered = false,
  size = 10,
} = defineProps<{
  tasks?: Task[]
  loading?: boolean
  error?: boolean
  errorCode?: string
  errorMessage?: string
  denied?: boolean
  selected?: string[]
  filtered?: boolean
  size?: number
}>()

const emit = defineEmits<{
  toggle: [id: string]
  toggleAll: []
  detail: [id: string]
  action: [id: string, action: TaskAction]
  duplicate: [id: string]
  remove: [id: string]
  retry: []
  create: []
  clearFilters: []
}>()

const allSelected = computed(() => tasks.length > 0 && tasks.every((task) => selected.includes(task.id)))
const someSelected = computed(() => !allSelected.value && tasks.some((task) => selected.includes(task.id)))

function actionLabel(task: Task): { action: TaskAction; label: string; icon: typeof Pause } | null {
  if (task.status === 'DRAFT') return { action: 'SUBMIT', label: '提交', icon: Play }
  if (task.status === 'SCHEDULED') return { action: 'START', label: '执行', icon: Play }
  if (task.status === 'RUNNING') return { action: 'PAUSE', label: '暂停', icon: Pause }
  if (task.status === 'PAUSED') return { action: 'RESUME', label: '恢复', icon: Play }
  return null
}

function timeInfo(task: Task): { time: string; label: string } {
  switch (task.status) {
    case 'RUNNING':
      return { time: task.startedAt ?? task.createdAt, label: '开始' }
    case 'SCHEDULED':
      return { time: task.scheduledAt ?? task.createdAt, label: '计划' }
    case 'COMPLETED':
      return { time: task.finishedAt ?? task.updatedAt, label: '完成' }
    case 'FAILED':
      return { time: task.finishedAt ?? task.updatedAt, label: '失败' }
    case 'TERMINATED':
      return { time: task.finishedAt ?? task.updatedAt, label: '终止' }
    case 'PAUSED':
      return { time: task.updatedAt, label: '暂停' }
    case 'CANCELLED':
      return { time: task.updatedAt, label: '取消' }
    default:
      return { time: task.createdAt, label: '创建' }
  }
}

const showState = computed(() => !loading && (error || denied || tasks.length === 0))
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full min-w-[940px] border-separate border-spacing-0">
      <thead>
        <tr>
          <th scope="col" class="w-11 bg-surface2 px-4 py-0 text-left">
            <input
              type="checkbox"
              class="size-4 cursor-pointer accent-[#0B6B5F]"
              :checked="allSelected"
              :indeterminate.prop="someSelected"
              aria-label="全选当前页任务"
              @change="emit('toggleAll')"
            />
          </th>
          <th scope="col" class="h-10 bg-surface2 px-3 text-left text-[11.5px] font-medium tracking-wide text-ink3">任务名称</th>
          <th scope="col" class="hidden h-10 bg-surface2 px-3 text-left text-[11.5px] font-medium tracking-wide text-ink3 min-[1400px]:table-cell">话术模板</th>
          <th scope="col" class="h-10 bg-surface2 px-3 text-right text-[11.5px] font-medium tracking-wide text-ink3">名单量</th>
          <th scope="col" class="h-10 w-[150px] bg-surface2 px-3 text-left text-[11.5px] font-medium tracking-wide text-ink3">执行进度</th>
          <th scope="col" class="h-10 bg-surface2 px-3 text-right text-[11.5px] font-medium tracking-wide text-ink3">接通率</th>
          <th scope="col" class="h-10 bg-surface2 px-3 text-left text-[11.5px] font-medium tracking-wide text-ink3">状态</th>
          <th scope="col" class="h-10 bg-surface2 px-3 text-left text-[11.5px] font-medium tracking-wide text-ink3">计划 / 执行时间</th>
          <th scope="col" class="h-10 bg-surface2 px-4 text-right text-[11.5px] font-medium tracking-wide text-ink3">操作</th>
        </tr>
      </thead>

      <tbody v-if="loading">
        <tr v-for="index in size" :key="index" class="border-b border-[#EFEDE7]">
          <td class="px-4 py-4"><div class="skeleton size-3.5" /></td>
          <td class="px-3 py-4"><div class="skeleton h-3.5 w-44" /><div class="skeleton mt-1.5 h-2.5 w-24" /></td>
          <td class="hidden px-3 py-4 min-[1400px]:table-cell"><div class="skeleton h-3 w-20" /></td>
          <td class="px-3 py-4"><div class="skeleton ml-auto h-3 w-12" /></td>
          <td class="px-3 py-4"><div class="skeleton h-1.5 w-full" /></td>
          <td class="px-3 py-4"><div class="skeleton ml-auto h-3 w-10" /></td>
          <td class="px-3 py-4"><div class="skeleton h-5 w-14" /></td>
          <td class="px-3 py-4"><div class="skeleton h-3 w-20" /></td>
          <td class="px-4 py-4"><div class="skeleton ml-auto h-3 w-16" /></td>
        </tr>
      </tbody>

      <tbody v-else-if="!showState">
        <tr
          v-for="task in tasks"
          :key="task.id"
          class="border-b border-[#EFEDE7] transition-colors last:border-b-0 hover:bg-surface2"
          :class="selected.includes(task.id) && 'bg-[#F4F8F6]'"
        >
          <td class="px-4 py-0">
            <input
              type="checkbox"
              class="size-4 cursor-pointer accent-[#0B6B5F]"
              :checked="selected.includes(task.id)"
              :aria-label="`选择任务 ${task.name}`"
              @change="emit('toggle', task.id)"
            />
          </td>

          <td class="max-w-[260px] px-3 py-0">
            <div class="flex flex-col gap-0.5 py-2.5">
              <span class="truncate font-medium text-ink" :title="task.name">{{ task.name }}</span>
              <span class="num text-[10.5px] tracking-wide text-ink3">{{ task.id }}</span>
            </div>
          </td>

          <td class="hidden px-3 py-0 min-[1400px]:table-cell">
            <div class="flex flex-col gap-px">
              <span class="whitespace-nowrap text-[12.5px] text-ink2">{{ task.scriptName }}</span>
              <span class="num text-[10.5px] text-ink4">{{ task.scriptVersion }}</span>
            </div>
          </td>

          <td class="px-3 py-0 text-right">
            <span class="num text-[12.5px]">{{ formatNumber(task.totalContacts) }}</span>
          </td>

          <td class="px-3 py-0">
            <div class="flex flex-col gap-1.5 py-2.5">
              <div class="flex items-center justify-between gap-2">
                <span class="num text-[11px] font-medium text-ink">{{ progressOf(task) }}%</span>
                <span class="num text-[10.5px] text-ink4">
                  {{ formatNumber(task.calledCount) }} / {{ formatNumber(task.totalContacts) }}
                </span>
              </div>
              <ProgressBar :value="progressOf(task)" :tone="task.status === 'FAILED' ? 'warn' : 'brand'" />
            </div>
          </td>

          <td class="px-3 py-0 text-right">
            <span
              class="num text-[12.5px]"
              :class="
                connectRateOf(task) === null
                  ? 'text-ink4'
                  : connectRateOf(task)! < 20
                    ? 'text-danger'
                    : 'text-ink'
              "
            >
              {{ connectRateOf(task) === null ? '—' : `${connectRateOf(task)!.toFixed(1)}%` }}
            </span>
          </td>

          <td class="px-3 py-0">
            <StatusBadge :status="task.status" />
          </td>

          <td class="px-3 py-0">
            <div class="num whitespace-nowrap text-[11.5px] text-ink3">
              {{ relativeTime(timeInfo(task).time) }}
              <span class="mt-px block text-[10.5px] text-ink3">
                {{ timeInfo(task).label }}
              </span>
            </div>
          </td>

          <td class="px-4 py-0">
            <div class="flex items-center justify-end gap-0.5">
              <button
                type="button"
                class="rounded-[5px] px-1.5 py-1 text-xs text-ink2 transition-colors hover:bg-brand-soft hover:text-brand"
                @click="emit('detail', task.id)"
              >
                详情
              </button>

              <button
                v-if="actionLabel(task)"
                type="button"
                class="rounded-[5px] px-1.5 py-1 text-xs text-ink2 transition-colors hover:bg-[#EFEDE7] hover:text-ink"
                @click="emit('action', task.id, actionLabel(task)!.action)"
              >
                {{ actionLabel(task)!.label }}
              </button>

              <button
                type="button"
                class="grid size-6 place-items-center rounded-[5px] text-ink3 transition-colors hover:bg-[#EFEDE7] hover:text-ink"
                :disabled="task.status !== 'DRAFT'"
                :title="task.status !== 'DRAFT' ? '仅草稿可删除' : '删除任务'"
                :class="task.status !== 'DRAFT' && 'cursor-not-allowed opacity-35 hover:bg-transparent'"
                aria-label="删除任务"
                @click="emit('remove', task.id)"
              >
                <Trash2 :size="13" />
              </button>

              <button
                type="button"
                class="grid size-6 place-items-center rounded-[5px] text-ink2 transition-colors hover:bg-[#EFEDE7] hover:text-ink"
                title="复制任务"
                aria-label="复制任务"
                @click="emit('duplicate', task.id)"
              >
                <Copy :size="13" />
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <StateBlock
      v-if="showState && denied"
      variant="denied"
      title="无访问权限"
      description="你的账号没有查看外呼任务的权限，请联系管理员开通「外呼运营」角色。"
      code="40300 · FORBIDDEN"
    />

    <StateBlock
      v-else-if="showState && error"
      variant="error"
      title="任务列表加载失败"
      :description="errorMessage || '网络请求超时，已自动重试 1 次。已加载的概览数据不会丢失，可以单独重试。'"
      :code="errorCode || '50400 · GATEWAY_TIMEOUT'"
      action-label="重新加载"
      @action="emit('retry')"
    />

    <StateBlock
      v-else-if="showState && filtered"
      variant="empty"
      title="没有符合条件的任务"
      description="试试调整状态筛选或关键词，也可以清除全部筛选条件。"
      action-label="清除筛选"
      @action="emit('clearFilters')"
    />

    <StateBlock
      v-else-if="showState"
      variant="empty"
      title="还没有外呼任务"
      description="创建第一个外呼任务，导入名单后即可开始自动拨打电话。"
      action-label="新建外呼任务"
      @action="emit('create')"
    />
  </div>
</template>
