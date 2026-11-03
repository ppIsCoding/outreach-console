<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const { value, tone = 'brand', height = 4 } = defineProps<{
  value: number
  tone?: 'brand' | 'warn' | 'muted'
  height?: number
}>()

const fillClass = computed(
  () =>
    ({
      brand: 'bg-gradient-to-r from-[#15857A] to-[#2FB3A0]',
      warn: 'bg-gradient-to-r from-[#C2841A] to-[#DDA53C]',
      muted: 'bg-[#C9C4B9]',
    })[tone],
)
</script>

<template>
  <div
    class="w-full overflow-hidden rounded-full bg-[#EDEAE3]"
    :style="{ height: `${height}px` }"
    role="progressbar"
    :aria-valuenow="Math.round(value)"
    aria-valuemin="0"
    aria-valuemax="100"
  >
    <div
      :class="cn('h-full rounded-full transition-[width] duration-500 ease-out', fillClass)"
      :style="{ width: `${Math.min(100, Math.max(0, value))}%` }"
    />
  </div>
</template>
