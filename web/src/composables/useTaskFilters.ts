import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { TaskQuery } from '@/api/types'

export type DateRange = NonNullable<TaskQuery['range']>

export interface TaskFilters {
  status: NonNullable<TaskQuery['status']>
  keyword: string
  scriptId: string
  range: DateRange
  page: number
  size: number
}

const DEFAULTS: TaskFilters = {
  status: 'ALL',
  keyword: '',
  scriptId: '',
  range: '7d',
  page: 1,
  size: 10,
}

const STATUSES: TaskFilters['status'][] = [
  'ALL',
  'DRAFT',
  'SCHEDULED',
  'RUNNING',
  'PAUSED',
  'COMPLETED',
  'FAILED',
  'TERMINATED',
  'CANCELLED',
]
const RANGES: DateRange[] = ['today', '7d', '30d', 'all']
const PAGE_SIZES = [10, 20, 50]

function readFilters(query: Record<string, unknown>): TaskFilters {
  const num = (value: unknown, fallback: number) => {
    const parsed = Number(value)
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
  }
  const status =
    typeof query.status === 'string' && STATUSES.includes(query.status as TaskFilters['status'])
      ? (query.status as TaskFilters['status'])
      : DEFAULTS.status
  const range =
    typeof query.range === 'string' && RANGES.includes(query.range as DateRange)
      ? (query.range as DateRange)
      : DEFAULTS.range
  const size = PAGE_SIZES.includes(num(query.size, DEFAULTS.size)) ? num(query.size, DEFAULTS.size) : DEFAULTS.size

  return {
    status,
    keyword: typeof query.q === 'string' ? query.q : '',
    scriptId: typeof query.script === 'string' ? query.script : '',
    range,
    page: Math.min(9999, num(query.page, 1)),
    size,
  }
}

/**
 * 筛选条件的唯一真源是 URL。
 * 好处：可分享、可刷新、可截图还原；使用 replace 避免筛选产生大量历史记录。
 */
export function useTaskFilters() {
  const route = useRoute()
  const router = useRouter()

  const filters = computed<TaskFilters>(() => readFilters(route.query))

  const isDirty = computed(
    () =>
      filters.value.status !== DEFAULTS.status ||
      filters.value.keyword !== '' ||
      filters.value.scriptId !== '' ||
      filters.value.range !== DEFAULTS.range,
  )

  function apply(next: Partial<TaskFilters>, options: { resetPage?: boolean } = {}) {
    const merged: TaskFilters = {
      ...filters.value,
      ...next,
      page: options.resetPage === false ? (next.page ?? filters.value.page) : (next.page ?? 1),
    }

    const query: Record<string, string> = {}
    if (merged.status !== DEFAULTS.status) query.status = merged.status
    if (merged.keyword) query.q = merged.keyword
    if (merged.scriptId) query.script = merged.scriptId
    if (merged.range !== DEFAULTS.range) query.range = merged.range
    if (merged.page > 1) query.page = String(merged.page)
    if (merged.size !== DEFAULTS.size) query.size = String(merged.size)

    void router.replace({ query })
  }

  function reset() {
    void router.replace({ query: {} })
  }

  return { filters, isDirty, apply, reset }
}
