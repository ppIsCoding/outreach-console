<script setup lang="ts">
import type { Summary } from '@/api/types'
import { formatDuration, formatNumber } from '@/lib/utils'

const { summary, loading = false } = defineProps<{ summary?: Summary; loading?: boolean }>()
</script>

<template>
  <section class="mb-4.5 grid grid-cols-1 overflow-hidden rounded-[14px] border border-line bg-surface sm:grid-cols-3">
    <div class="flex items-center gap-3.5 px-5 py-3.5">
      <span class="text-xs text-ink3">意向客户</span>
      <span class="num text-[11px] text-ink3">+{{ summary?.intentDelta ?? 0 }} 今日</span>
      <span v-if="loading" class="skeleton ml-auto h-4 w-12" />
      <span v-else class="num ml-auto text-base font-semibold">{{ formatNumber(summary?.intentCount) }}</span>
    </div>

    <div class="flex items-center gap-3.5 border-line px-5 py-3.5 sm:border-l">
      <span class="text-xs text-ink3">平均通话时长</span>
      <span class="text-[11px] text-ink3">接通口径</span>
      <span v-if="loading" class="skeleton ml-auto h-4 w-12" />
      <span v-else class="num ml-auto text-base font-semibold">{{ formatDuration(summary?.avgDurationSec) }}</span>
    </div>

    <div class="flex items-center gap-3.5 border-line px-5 py-3.5 sm:border-l">
      <span class="text-xs text-ink3">任务完成率</span>
      <span class="text-[11px] text-ink3">近 7 日</span>
      <span v-if="loading" class="skeleton ml-auto h-4 w-12" />
      <span v-else class="num ml-auto text-base font-semibold">{{ summary?.completionRate ?? 0 }}%</span>
    </div>
  </section>
</template>
