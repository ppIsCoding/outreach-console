<script setup lang="ts">
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { formatNumber } from '@/lib/utils'

const { page, size, total, totalPages, selectedCount = 0 } = defineProps<{
  page: number
  size: number
  total: number
  totalPages: number
  selectedCount?: number
}>()

const emit = defineEmits<{
  'update:page': [page: number]
  'update:size': [size: number]
  'bulk-pause': []
  'bulk-terminate': []
}>()

const pages = computed(() => {
  const windowSize = 5
  const start = Math.max(1, Math.min(page - 2, totalPages - windowSize + 1))
  const end = Math.min(totalPages, start + windowSize - 1)
  return Array.from({ length: Math.max(0, end - start + 1) }, (_, index) => start + index)
})

const rangeText = computed(() => {
  if (total === 0) return '0 条'
  const from = (page - 1) * size + 1
  const to = Math.min(page * size, total)
  return `${from}–${to} / ${formatNumber(total)}`
})
</script>

<template>
  <div class="flex flex-wrap items-center gap-3 border-t border-line px-4 py-3">
    <template v-if="selectedCount > 0">
      <span class="text-[12.5px] text-ink2">
        已选 <b class="num text-brand">{{ selectedCount }}</b> 项
      </span>
      <button type="button" class="btn btn-sm ml-1" @click="emit('bulk-pause')">批量暂停</button>
      <button type="button" class="btn btn-sm" @click="emit('bulk-terminate')">批量终止</button>
    </template>

    <span v-else class="text-xs text-ink3">
      共 <b class="num font-medium text-ink2">{{ formatNumber(total) }}</b> 个任务
    </span>

    <div class="ml-auto flex items-center gap-1">
      <span class="num mr-2 text-xs text-ink3">{{ rangeText }}</span>

      <select
        :value="size"
        class="select input mr-1 h-7 w-auto bg-surface2 px-2 pr-6 text-[11.5px] text-ink2"
        aria-label="每页条数"
        @change="emit('update:size', Number(($event.target as HTMLSelectElement).value))"
      >
        <option :value="10">10 / 页</option>
        <option :value="20">20 / 页</option>
        <option :value="50">50 / 页</option>
      </select>

      <button
        type="button"
        class="grid size-7 place-items-center rounded-md text-ink2 transition-colors hover:bg-[#F0EEE8] disabled:cursor-not-allowed disabled:opacity-35"
        :disabled="page <= 1"
        aria-label="上一页"
        @click="emit('update:page', page - 1)"
      >
        <ChevronLeft :size="14" />
      </button>

      <button
        v-for="item in pages"
        :key="item"
        type="button"
        class="num grid h-7 min-w-7 place-items-center rounded-md px-1.5 text-xs transition-colors"
        :class="item === page ? 'bg-brand text-white' : 'text-ink2 hover:bg-[#F0EEE8]'"
        :aria-current="item === page ? 'page' : undefined"
        @click="emit('update:page', item)"
      >
        {{ item }}
      </button>

      <button
        type="button"
        class="grid size-7 place-items-center rounded-md text-ink2 transition-colors hover:bg-[#F0EEE8] disabled:cursor-not-allowed disabled:opacity-35"
        :disabled="page >= totalPages"
        aria-label="下一页"
        @click="emit('update:page', page + 1)"
      >
        <ChevronRight :size="14" />
      </button>
    </div>
  </div>
</template>
