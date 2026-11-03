<script setup lang="ts">
import {
  ChartLine,
  ListChecks,
  MessageSquareText,
  PhoneCall,
  Settings,
  Users,
  LayoutGrid,
} from 'lucide-vue-next'

const groups = [
  {
    label: '工作台',
    items: [
      { key: 'overview', label: '概览', icon: LayoutGrid, badge: '' },
      { key: 'tasks', label: '任务看板', icon: ListChecks, badge: '3' },
      { key: 'scripts', label: '话术管理', icon: MessageSquareText, badge: '' },
      { key: 'contacts', label: '名单管理', icon: Users, badge: '' },
      { key: 'records', label: '通话记录', icon: PhoneCall, badge: '' },
    ],
  },
  {
    label: '分析',
    items: [
      { key: 'reports', label: '数据报表', icon: ChartLine, badge: '' },
      { key: 'settings', label: '系统设置', icon: Settings, badge: '' },
    ],
  },
]
</script>

<template>
  <aside class="hidden w-[232px] shrink-0 flex-col bg-sidebar text-white md:flex">
    <div class="flex items-center gap-2.5 px-4.5 pb-5.5 pt-5">
      <div
        class="grid size-[30px] place-items-center rounded-lg bg-gradient-to-br from-[#15857A] to-brand shadow-[0_2px_10px_rgba(11,107,95,0.45)]"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round">
          <path d="M3 12h2M19 12h2M7 8v8M11 5v14M15 9v6" />
        </svg>
      </div>
      <div>
        <div class="text-sm font-semibold tracking-wide">Outreach</div>
        <div class="text-[10.5px] uppercase tracking-[0.14em] text-[#6E7887]">Voice Console</div>
      </div>
    </div>

    <nav class="flex-1 overflow-y-auto px-3 pb-3">
      <template v-for="group in groups" :key="group.label">
        <div class="px-2.5 pb-2 pt-3.5 text-[10.5px] uppercase tracking-[0.12em] text-[#5A6371]">
          {{ group.label }}
        </div>
        <button
          v-for="item in group.items"
          :key="item.key"
          type="button"
          class="relative flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-[13.5px] transition-colors"
          :class="
            item.key === 'tasks'
              ? 'bg-[#1D252E] text-white'
              : 'text-[#8E97A6] hover:bg-sidebar2 hover:text-[#D6DBE3]'
          "
        >
          <span
            v-if="item.key === 'tasks'"
            class="absolute -left-3 top-1/2 h-[18px] w-[3px] -translate-y-1/2 rounded-r-[3px] bg-[#2FB3A0]"
          />
          <component :is="item.icon" :size="16" :stroke-width="1.6" class="shrink-0 opacity-90" />
          <span>{{ item.label }}</span>
          <span
            v-if="item.badge"
            class="ml-auto rounded-full px-1.5 py-px font-mono text-[10.5px]"
            :class="item.key === 'tasks' ? 'bg-[#2FB3A0]/18 text-[#5FD3C2]' : 'bg-[#2A323D] text-[#9AA4B2]'"
          >
            {{ item.badge }}
          </span>
        </button>
      </template>
    </nav>

    <div class="border-t border-[#212831] p-3">
      <button type="button" class="flex w-full items-center gap-2.5 rounded-[9px] p-2 text-left hover:bg-sidebar2">
        <span
          class="grid size-[30px] shrink-0 place-items-center rounded-full bg-gradient-to-br from-[#3C4654] to-[#2A323D] text-[11.5px] font-semibold text-[#C9D2DE]"
        >
          李
        </span>
        <span>
          <span class="block text-[12.5px] leading-tight text-[#DDE2E9]">李思远</span>
          <span class="block text-[11px] text-[#6E7887]">外呼运营</span>
        </span>
      </button>
    </div>
  </aside>
</template>
