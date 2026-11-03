import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatNumber(n: number | null | undefined): string {
  if (n === null || n === undefined) return '—'
  return n.toLocaleString('en-US')
}

export function formatPercent(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined) return '—'
  return `${value.toFixed(digits)}%`
}

/** 秒 → 1′42″ */
export function formatDuration(seconds: number | null | undefined): string {
  if (!seconds) return '—'
  const m = Math.floor(seconds / 60)
  const s = Math.round(seconds % 60)
  return `${m}′${String(s).padStart(2, '0')}″`
}

/** 秒 → 约 1 小时 30 分 */
export function formatEstimate(seconds: number): string {
  if (seconds < 60) return '不到 1 分钟'
  const totalMinutes = Math.round(seconds / 60)
  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  if (h === 0) return `约 ${m} 分钟`
  if (m === 0) return `约 ${h} 小时`
  return `约 ${h} 小时 ${m} 分`
}

export function progressOf(task: { calledCount: number; totalContacts: number }): number {
  if (!task.totalContacts) return 0
  return Math.min(100, Math.round((task.calledCount / task.totalContacts) * 100))
}

export function connectRateOf(task: {
  calledCount: number
  connectedCount: number
}): number | null {
  if (!task.calledCount) return null
  return (task.connectedCount / task.calledCount) * 100
}

/** 相对时间：刚刚 / 12 分钟前 / 昨天 14:30 */
export function relativeTime(iso: string): string {
  const then = new Date(iso.replace(' ', 'T'))
  const now = new Date()
  const diff = (now.getTime() - then.getTime()) / 1000
  if (Number.isNaN(diff)) return iso
  if (diff < 0) {
    const ahead = -diff
    if (ahead < 60) return '即将开始'
    if (ahead < 3600) return `${Math.ceil(ahead / 60)} 分钟后`
    if (ahead < 86400) return `${Math.ceil(ahead / 3600)} 小时后`
    return iso.slice(5, 16)
  }
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 172800) return `昨天 ${iso.slice(11, 16)}`
  return iso.slice(5, 16)
}

export function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
