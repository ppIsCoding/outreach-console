import type { TaskAction, TaskStatus } from '../api/types'

export interface StatusMeta {
  label: string
  /** tailwind 类：背景 / 文字 / 圆点 */
  badge: string
  dot: string
  /** 状态机：该状态下可执行的动作 */
  actions: TaskAction[]
}

export const STATUS_META: Record<TaskStatus, StatusMeta> = {
  RUNNING: {
    label: '执行中',
    badge: 'bg-brand-soft text-brand',
    dot: 'bg-[#2FB3A0]',
    actions: ['PAUSE', 'TERMINATE'],
  },
  SCHEDULED: {
    label: '待执行',
    badge: 'bg-info-soft text-info',
    dot: 'bg-[#5B85C7]',
    actions: ['START', 'CANCEL'],
  },
  PAUSED: {
    label: '已暂停',
    badge: 'bg-warn-soft text-warn',
    dot: 'bg-[#D9A441]',
    actions: ['RESUME', 'TERMINATE'],
  },
  COMPLETED: {
    label: '已完成',
    badge: 'bg-ok-soft text-ok',
    dot: 'bg-[#4E9B76]',
    actions: [],
  },
  FAILED: {
    label: '异常',
    badge: 'bg-danger-soft text-danger',
    dot: 'bg-[#C9644A]',
    actions: [],
  },
  TERMINATED: {
    label: '已终止',
    badge: 'bg-[#F0EEE8] text-ink2',
    dot: 'bg-[#A9AFB9]',
    actions: [],
  },
  CANCELLED: {
    label: '已取消',
    badge: 'bg-[#F0EEE8] text-ink2',
    dot: 'bg-[#A9AFB9]',
    actions: [],
  },
  DRAFT: {
    label: '草稿',
    badge: 'bg-[#F0EEE8] text-ink2',
    dot: 'bg-[#A9AFB9]',
    actions: ['SUBMIT'],
  },
}

export const ACTION_META: Record<TaskAction, { label: string; danger?: boolean }> = {
  SUBMIT: { label: '提交' },
  START: { label: '立即执行' },
  PAUSE: { label: '暂停' },
  RESUME: { label: '恢复' },
  TERMINATE: { label: '终止', danger: true },
  CANCEL: { label: '取消', danger: true },
}

export const FILTER_TABS: Array<{ key: TaskStatus | 'ALL'; label: string }> = [
  { key: 'ALL', label: '全部' },
  { key: 'RUNNING', label: '执行中' },
  { key: 'SCHEDULED', label: '待执行' },
  { key: 'PAUSED', label: '已暂停' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'FAILED', label: '异常' },
]
