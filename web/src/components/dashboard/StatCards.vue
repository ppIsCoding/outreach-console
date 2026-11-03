<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDownRight, ArrowUpRight, Info, RefreshCw } from 'lucide-vue-next'
import type { Summary } from '@/api/types'
import { formatNumber, formatPercent } from '@/lib/utils'
import ProgressBar from '@/components/ui/ProgressBar.vue'

const {
  summary,
  loading = false,
  failed = false,
} = defineProps<{
  summary?: Summary
  loading?: boolean
  failed?: boolean
}>()

const emit = defineEmits<{ retry: []; filterRunning: [] }>()

const RING_RADIUS = 35
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS

const ringOffset = computed(() => {
  const rate = summary?.connectRate ?? 0
  return RING_CIRCUMFERENCE * (1 - Math.min(100, Math.max(0, rate)) / 100)
})

const sparkline = computed(() => {
  const values = summary?.sparkline ?? []
  if (values.length < 2 || values.every((value) => value === 0)) return { line: '', area: '' }

  const width = 320
  const height = 74
  const padding = 8
  const max = Math.max(...values)
  const min = Math.min(...values)
  const range = max - min || 1

  const points = values.map((value, index) => {
    const x = (index / (values.length - 1)) * width
    const y = padding + (1 - (value - min) / range) * (height - padding * 2)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  })

  const line = `M${points.join(' L')}`
  return { line, area: `${line} L${width},${height} L0,${height} Z` }
})

const deltaUp = computed(() => (summary?.todayCallsDelta ?? 0) >= 0)
</script>

<template>
  <section class="@container mb-3.5 grid grid-cols-12 gap-3.5" aria-label="今日外呼概览">
    <!-- 主指标：今日外呼量 -->
    <div
      class="card col-span-12 flex flex-col overflow-hidden px-5 pb-[18px] pt-4.5 @min-[800px]:col-span-5"
    >
      <div class="flex items-center gap-1.5 text-xs text-ink3">
        今日外呼量
        <Info
          :size="13"
          class="opacity-60"
          title="统计口径：今日 00:00 起已拨出通话数"
          aria-label="统计口径：今日 00:00 起已拨出通话数"
        />
      </div>

      <div v-if="loading" class="mt-3 space-y-3">
        <div class="skeleton h-8 w-40" />
        <div class="skeleton h-[74px] w-full" />
      </div>

      <div v-else-if="failed" class="mt-4 flex items-center gap-2 text-[13px] text-ink3">
        <span class="font-mono text-2xl text-ink4">--</span>
        <button type="button" class="btn btn-sm" @click="emit('retry')"><RefreshCw :size="13" /> 重试</button>
      </div>

      <template v-else>
        <div class="mt-2 flex items-end gap-3">
          <div class="num text-[30px] font-semibold leading-none">{{ formatNumber(summary?.todayCalls) }}</div>
          <div
            class="num mb-0.5 inline-flex items-center gap-0.5 rounded-[5px] px-1.5 py-0.5 text-[11.5px] font-medium"
            :class="deltaUp ? 'bg-ok-soft text-ok' : 'bg-danger-soft text-danger'"
          >
            <component :is="deltaUp ? ArrowUpRight : ArrowDownRight" :size="11" :stroke-width="2.5" />
            {{ Math.abs(summary?.todayCallsDelta ?? 0).toFixed(1) }}%
          </div>
          <div class="mb-1 text-[11.5px] text-ink3">较昨日</div>
        </div>

        <div class="-mx-5 -mb-[18px] mt-auto pt-2.5">
          <svg viewBox="0 0 320 74" preserveAspectRatio="none" class="block h-[74px] w-full" aria-hidden="true">
            <defs>
              <linearGradient id="spark-fill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#0B6B5F" stop-opacity="0.18" />
                <stop offset="100%" stop-color="#0B6B5F" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path :d="sparkline.area" fill="url(#spark-fill)" />
            <path :d="sparkline.line" fill="none" stroke="#0B6B5F" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>
      </template>
    </div>

    <!-- 接通率 -->
    <div class="card col-span-12 px-5 py-4.5 @min-[560px]:col-span-6 @min-[800px]:col-span-3">
      <div class="text-xs text-ink3">今日接通率</div>

      <div v-if="loading" class="mt-3 flex items-center gap-3">
        <div class="skeleton size-[86px] rounded-full" />
        <div class="flex-1 space-y-2">
          <div class="skeleton h-3 w-full" />
          <div class="skeleton h-3 w-3/4" />
        </div>
      </div>

      <div v-else-if="failed" class="mt-4 flex items-center gap-2 text-[13px] text-ink3">
        <span class="font-mono text-2xl text-ink4">--</span>
        <button type="button" class="btn btn-sm" @click="emit('retry')"><RefreshCw :size="13" /> 重试</button>
      </div>

      <div v-else class="mt-2.5 flex items-center gap-3.5">
        <div class="relative size-[86px] shrink-0">
          <svg width="86" height="86" viewBox="0 0 86 86" class="-rotate-90" aria-hidden="true">
            <circle cx="43" cy="43" :r="RING_RADIUS" fill="none" stroke="#EFEDE7" stroke-width="7" />
            <circle
              cx="43"
              cy="43"
              :r="RING_RADIUS"
              fill="none"
              stroke="#0B6B5F"
              stroke-width="7"
              stroke-linecap="round"
              :stroke-dasharray="RING_CIRCUMFERENCE"
              :stroke-dashoffset="ringOffset"
              class="transition-[stroke-dashoffset] duration-700 ease-out"
            />
          </svg>
          <div class="absolute inset-0 grid place-content-center text-center">
            <div class="num text-lg font-semibold leading-none">
              {{ (summary?.connectRate ?? 0).toFixed(1) }}<span class="text-[11px]">%</span>
            </div>
            <div class="mt-0.5 text-[9.5px] tracking-wider text-ink3">接通 / 已呼</div>
          </div>
        </div>

        <div class="flex min-w-0 flex-col gap-1.5 text-[11.5px] text-ink2">
          <div class="flex items-center gap-1.5 whitespace-nowrap">
            <span class="size-[7px] rounded-[2px] bg-brand" />
            接通
            <span class="num ml-auto pl-2 font-medium text-ink">{{ formatNumber(summary?.connectedCount) }}</span>
          </div>
          <div class="flex items-center gap-1.5 whitespace-nowrap">
            <span class="size-[7px] rounded-[2px] bg-[#D9D5CB]" />
            未接通
            <span class="num ml-auto pl-2 font-medium text-ink">{{ formatNumber(summary?.unansweredCount) }}</span>
          </div>
          <div class="mt-0.5 whitespace-nowrap text-[11px] text-ink3">
            行业均值 {{ formatPercent(summary?.connectRateBaseline) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 执行中任务 -->
    <div class="card col-span-12 px-5 py-4.5 @min-[560px]:col-span-6 @min-[800px]:col-span-4">
      <div class="text-xs text-ink3">执行中任务</div>

      <div v-if="loading" class="mt-3 space-y-3">
        <div class="skeleton h-7 w-16" />
        <div class="skeleton h-3 w-full" />
        <div class="skeleton h-3 w-full" />
      </div>

      <div v-else-if="failed" class="mt-4 flex items-center gap-2 text-[13px] text-ink3">
        <span class="font-mono text-2xl text-ink4">--</span>
        <button type="button" class="btn btn-sm" @click="emit('retry')"><RefreshCw :size="13" /> 重试</button>
      </div>

      <template v-else>
        <div class="mt-1.5 flex items-baseline gap-2.5">
          <div class="num text-2xl font-semibold leading-none">{{ summary?.runningCount ?? 0 }}</div>
          <div class="text-[11.5px] text-ink3">
            占用并发 <span class="num">{{ summary?.concurrencyUsed ?? 0 }}</span> /
            <span class="num">{{ summary?.concurrencyTotal ?? 0 }}</span>
          </div>
          <button type="button" class="ml-auto text-[11.5px] text-brand hover:underline" @click="emit('filterRunning')">
            查看全部
          </button>
        </div>

        <div class="mt-3 flex flex-col gap-2.5">
          <div v-for="task in summary?.runningTasks ?? []" :key="task.id" class="flex flex-col gap-1.5">
            <div class="flex items-center gap-2 text-xs">
              <span class="min-w-0 flex-1 truncate text-ink2">{{ task.name }}</span>
              <span class="num shrink-0 text-[11px] text-ink3">{{ task.progress }}%</span>
            </div>
            <ProgressBar :value="task.progress" />
          </div>

          <div v-if="!summary?.runningTasks?.length" class="py-3 text-center text-xs text-ink3">
            当前没有正在执行的任务
          </div>
        </div>
      </template>
    </div>
  </section>
</template>
