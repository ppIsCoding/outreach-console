<script setup lang="ts">
import { ref } from 'vue'
import { FlaskConical, X } from 'lucide-vue-next'
import { useUiStore } from '@/stores/ui'
import type { Scenario } from '@/api/mock/db'

const ui = useUiStore()
const open = ref(false)

const options: Array<{ key: Scenario; label: string; hint: string }> = [
  { key: 'normal', label: '正常', hint: '默认数据' },
  { key: 'loading', label: '加载中', hint: '骨架屏' },
  { key: 'empty', label: '空数据', hint: '无任务' },
  { key: 'emptyFilter', label: '筛选空', hint: '无匹配结果' },
  { key: 'error', label: '请求失败', hint: '列表报错' },
  { key: 'denied', label: '无权限', hint: '403' },
]

function pick(key: Scenario) {
  ui.switchScenario(key)
  // 场景已持久化到 sessionStorage。整页刷新可以确保所有请求重新发起，
  // 比逐个 invalidate 更可靠（缓存、重试、并发请求都会一并重置）。
  window.setTimeout(() => window.location.reload(), 140)
}
</script>

<template>
  <div class="fixed bottom-4 right-4 z-30 print:hidden">
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="translate-y-2 opacity-0"
      leave-active-class="transition duration-100 ease-in"
      leave-to-class="translate-y-2 opacity-0"
    >
      <div
        v-if="open"
        class="mb-2 w-[228px] rounded-xl border border-white/10 bg-[#12161C]/95 p-2.5 text-white shadow-[0_12px_40px_-12px_rgba(0,0,0,0.5)] backdrop-blur-md"
      >
        <div class="flex items-center gap-1.5 px-1 pb-2.5 text-[10.5px] uppercase tracking-[0.08em] text-[#7C8798]">
          <FlaskConical :size="13" />
          演示状态
          <button
            type="button"
            class="ml-auto rounded p-0.5 text-[#7C8798] hover:text-white"
            aria-label="收起演示状态面板"
            @click="open = false"
          >
            <X :size="13" />
          </button>
        </div>

        <div class="grid grid-cols-2 gap-1.5">
          <button
            v-for="option in options"
            :key="option.key"
            type="button"
            class="rounded-md px-2 py-1.5 text-left transition-colors"
            :class="
              ui.scenario === option.key
                ? 'bg-[#2FB3A0] text-[#06231F]'
                : 'bg-white/5 text-[#AEB7C4] hover:bg-white/10 hover:text-white'
            "
            @click="pick(option.key)"
          >
            <span class="block text-[11.5px] font-medium leading-tight">{{ option.label }}</span>
            <span
              class="block text-[10px] leading-tight"
              :class="ui.scenario === option.key ? 'text-[#0B4A42]' : 'text-[#6E7887]'"
            >
              {{ option.hint }}
            </span>
          </button>
        </div>

        <p class="px-1 pt-2 text-[10px] leading-relaxed text-[#5A6371]">
          仅 mock 模式可见，用于向评审演示状态完整性
        </p>
      </div>
    </Transition>

    <button
      type="button"
      class="ml-auto flex items-center gap-1.5 rounded-full border border-white/10 bg-[#12161C]/92 px-3 py-2 text-[11.5px] text-[#C9D2DE] shadow-lg backdrop-blur-md transition-colors hover:bg-[#1A1F27]"
      :aria-expanded="open"
      aria-label="打开演示状态面板"
      @click="open = !open"
    >
      <FlaskConical :size="14" class="text-[#2FB3A0]" />
      演示状态
    </button>
  </div>
</template>
