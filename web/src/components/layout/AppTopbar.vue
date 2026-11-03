<script setup lang="ts">
import { computed } from 'vue'
import { Bell, CircleQuestionMark, ChevronRight } from 'lucide-vue-next'
import { IS_MOCK } from '@/api'
import { connectionState } from '@/api/realtime'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()

const state = computed(() => {
  if (IS_MOCK) {
    return ui.scenario === 'error' || ui.scenario === 'denied' ? 'offline' : 'mock'
  }
  return connectionState.value
})

const stateMeta = computed(() => {
  if (state.value === 'offline') return { label: '同步中断', color: '#B4472F', ping: false }
  if (state.value === 'connecting') return { label: '连接中', color: '#C2841A', ping: false }
  return { label: '实时同步中', color: '#2FB3A0', ping: true }
})
</script>

<template>
  <header
    class="flex h-14 shrink-0 items-center gap-4 border-b border-line bg-canvas/85 px-7 backdrop-blur-md"
  >
    <div class="flex items-center gap-2 text-[13px] text-ink3">
      <span>外呼管理</span>
      <ChevronRight :size="14" class="opacity-50" />
      <span class="font-medium text-ink">任务看板</span>
    </div>

    <div class="ml-auto flex items-center gap-1.5">
      <span
        class="flex items-center gap-1.5 rounded-full border border-line bg-surface px-2.5 py-1 text-[11.5px] text-ink2"
        :title="stateMeta.label"
      >
        <span class="relative flex size-1.5">
          <span
            v-if="stateMeta.ping"
            class="absolute inline-flex size-full animate-ping rounded-full opacity-60"
            :style="{ backgroundColor: stateMeta.color }"
          />
          <span
            class="relative inline-flex size-1.5 rounded-full"
            :style="{ backgroundColor: stateMeta.color }"
          />
        </span>
        {{ stateMeta.label }}
      </span>

      <button
        type="button"
        class="relative grid size-8 place-items-center rounded-lg text-ink2 transition-colors hover:bg-[#ECEAE4] hover:text-ink"
        aria-label="通知"
        @click="ui.toast('通知中心暂未开放', 'info')"
      >
        <Bell :size="17" :stroke-width="1.6" />
        <span class="absolute right-1.5 top-1.5 size-1.5 rounded-full border-[1.5px] border-canvas bg-danger" />
      </button>

      <button
        type="button"
        class="grid size-8 place-items-center rounded-lg text-ink2 transition-colors hover:bg-[#ECEAE4] hover:text-ink"
        aria-label="帮助"
        @click="ui.toast('帮助中心暂未开放', 'info')"
      >
        <CircleQuestionMark :size="17" :stroke-width="1.6" />
      </button>
    </div>
  </header>
</template>
