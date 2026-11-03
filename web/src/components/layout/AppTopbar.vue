<script setup lang="ts">
import { Bell, CircleQuestionMark, ChevronRight } from 'lucide-vue-next'
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
      >
        <span class="relative flex size-1.5">
          <span class="absolute inline-flex size-full animate-ping rounded-full bg-[#2FB3A0] opacity-60" />
          <span class="relative inline-flex size-1.5 rounded-full bg-[#2FB3A0]" />
        </span>
        实时同步中
      </span>

      <button
        type="button"
        class="relative grid size-8 place-items-center rounded-lg text-ink2 transition-colors hover:bg-[#ECEAE4] hover:text-ink"
        aria-label="通知"
      >
        <Bell :size="17" :stroke-width="1.6" />
        <span class="absolute right-1.5 top-1.5 size-1.5 rounded-full border-[1.5px] border-canvas bg-danger" />
      </button>

      <button
        type="button"
        class="grid size-8 place-items-center rounded-lg text-ink2 transition-colors hover:bg-[#ECEAE4] hover:text-ink"
        aria-label="帮助"
      >
        <CircleQuestionMark :size="17" :stroke-width="1.6" />
      </button>
    </div>
  </header>
</template>
