<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { Search, X } from 'lucide-vue-next'
import type { Script } from '@/api/types'
import type { DateRange } from '@/composables/useTaskFilters'

const {
  keyword,
  scriptId,
  range,
  dirty = false,
  scripts = [],
} = defineProps<{
  keyword: string
  scriptId: string
  range: DateRange
  dirty?: boolean
  scripts?: Script[]
}>()

const emit = defineEmits<{
  'update:keyword': [value: string]
  'update:scriptId': [value: string]
  'update:range': [value: DateRange]
  reset: []
}>()

const RANGE_OPTIONS: Array<{ value: DateRange; label: string }> = [
  { value: 'today', label: '今日' },
  { value: '7d', label: '近 7 天' },
  { value: '30d', label: '近 30 天' },
  { value: 'all', label: '全部时间' },
]

const localKeyword = ref(keyword)
let debounceTimer: number | undefined

watch(
  () => keyword,
  (value) => {
    if (value !== localKeyword.value) localKeyword.value = value
  },
)

function handleInput() {
  window.clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => emit('update:keyword', localKeyword.value.trim()), 300)
}

function clearKeyword() {
  localKeyword.value = ''
  emit('update:keyword', '')
}

onBeforeUnmount(() => window.clearTimeout(debounceTimer))
</script>

<template>
  <div class="flex flex-wrap items-center gap-2.5 border-b border-line px-4 py-3">
    <div class="relative min-w-[220px] flex-[0_1_260px]">
      <Search :size="14" class="absolute left-2.5 top-1/2 -translate-y-1/2 text-ink4" />
      <input
        v-model="localKeyword"
        type="search"
        class="input h-8 bg-surface2 pl-8 pr-8 text-[13px]"
        placeholder="搜索任务名称或 ID"
        aria-label="搜索任务名称或 ID"
        @input="handleInput"
        @keydown.esc="clearKeyword"
      />
      <button
        v-if="localKeyword"
        type="button"
        class="absolute right-2 top-1/2 grid size-4 -translate-y-1/2 place-items-center rounded text-ink4 hover:text-ink"
        aria-label="清空搜索"
        @click="clearKeyword"
      >
        <X :size="12" :stroke-width="2.4" />
      </button>
    </div>

    <select
      :value="scriptId"
      class="select input h-8 w-auto bg-surface2 text-[12.5px] text-ink2"
      aria-label="按话术模板筛选"
      @change="emit('update:scriptId', ($event.target as HTMLSelectElement).value)"
    >
      <option value="">全部话术</option>
      <option v-for="script in scripts" :key="script.id" :value="script.id">
        {{ script.name }} {{ script.version }}
      </option>
    </select>

    <select
      :value="range"
      class="select input h-8 w-auto bg-surface2 text-[12.5px] text-ink2"
      aria-label="按时间范围筛选"
      @change="emit('update:range', ($event.target as HTMLSelectElement).value as DateRange)"
    >
      <option v-for="option in RANGE_OPTIONS" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>

    <button
      v-if="dirty"
      type="button"
      class="ml-auto rounded-md px-2 py-1 text-[12.5px] text-ink3 transition-colors hover:bg-[#F0EEE8] hover:text-ink"
      @click="emit('reset')"
    >
      重置筛选
    </button>
  </div>
</template>
