<script setup lang="ts">
import { computed } from 'vue'
import { STATUS_META } from '@/lib/task-status'
import type { TaskStatus } from '@/api/types'

const { status } = defineProps<{ status: TaskStatus }>()
const meta = computed(() => STATUS_META[status])
</script>

<template>
  <span
    class="inline-flex h-[22px] items-center gap-1.5 whitespace-nowrap rounded-[5px] px-2 text-[11.5px] font-medium"
    :class="meta.badge"
  >
    <span
      class="size-[5px] shrink-0 rounded-full"
      :class="[meta.dot, status === 'RUNNING' && 'animate-[pulse-dot_1.8s_infinite]']"
    />
    {{ meta.label }}
  </span>
</template>
