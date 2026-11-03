<script setup lang="ts">
import { Check, Info, X } from 'lucide-vue-next'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
</script>

<template>
  <div class="pointer-events-none fixed left-1/2 top-4 z-80 flex -translate-x-1/2 flex-col items-center gap-2">
    <TransitionGroup
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="-translate-y-2 scale-[0.97] opacity-0"
      leave-active-class="transition duration-150 ease-in"
      leave-to-class="opacity-0"
    >
      <div
        v-for="toast in ui.toasts"
        :key="toast.id"
        class="pointer-events-auto flex items-center gap-2.5 rounded-[9px] bg-[#16191F] px-3.5 py-2.5 text-[12.5px] text-white shadow-[0_10px_30px_-8px_rgba(0,0,0,0.4)]"
        role="status"
      >
        <Check v-if="toast.type === 'success'" :size="15" class="text-[#5FD3C2]" :stroke-width="2.4" />
        <Info v-else-if="toast.type === 'info'" :size="15" class="text-[#8FB6F0]" :stroke-width="2.2" />
        <X v-else :size="15" class="text-[#F08A6E]" :stroke-width="2.4" />
        {{ toast.message }}
      </div>
    </TransitionGroup>
  </div>
</template>
