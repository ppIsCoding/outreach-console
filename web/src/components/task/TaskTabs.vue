<script setup lang="ts">
import { computed } from 'vue'
import { FILTER_TABS } from '@/lib/task-status'
import type { StatusCount, TaskStatus } from '@/api/types'

const { status, counts = [] } = defineProps<{
  status: TaskStatus | 'ALL'
  counts?: StatusCount[]
}>()

const emit = defineEmits<{ change: [status: TaskStatus | 'ALL'] }>()

const countMap = computed(() => new Map(counts.map((item) => [item.status, item.count])))
</script>

<template>
  <div class="flex items-center gap-0.5 overflow-x-auto border-b border-line px-4" role="tablist" aria-label="按状态筛选">
    <button
      v-for="tab in FILTER_TABS"
      :key="tab.key"
      type="button"
      role="tab"
      :aria-selected="status === tab.key"
      class="relative flex shrink-0 items-center gap-1.5 px-2.5 py-3 text-[13px] transition-colors"
      :class="status === tab.key ? 'font-medium text-brand' : 'text-ink2 hover:text-ink'"
      @click="emit('change', tab.key)"
    >
      {{ tab.label }}
      <span
        class="num rounded px-1.5 py-px text-[10.5px] leading-normal"
        :class="status === tab.key ? 'bg-brand-soft text-brand' : 'bg-[#F0EEE8] text-ink3'"
      >
        {{ countMap.get(tab.key) ?? 0 }}
      </span>
      <span
        v-if="status === tab.key"
        class="absolute inset-x-2 -bottom-px h-0.5 rounded-t bg-brand"
      />
    </button>
  </div>
</template>
