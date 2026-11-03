<script setup lang="ts">
import { computed } from 'vue'
import { Ban, Inbox, TriangleAlert } from 'lucide-vue-next'

const {
  variant = 'empty',
  title,
  description,
  actionLabel,
  code,
} = defineProps<{
  variant?: 'empty' | 'error' | 'denied'
  title: string
  description: string
  actionLabel?: string
  code?: string
}>()

defineEmits<{ action: [] }>()

const icon = computed(() => (variant === 'empty' ? Inbox : variant === 'denied' ? Ban : TriangleAlert))
const iconClass = computed(() =>
  variant === 'empty' ? 'bg-surface2 border-line text-ink4' : 'bg-danger-soft border-[#F0D5CD] text-danger',
)
</script>

<template>
  <div
    class="flex flex-col items-center justify-center gap-3.5 px-6 py-14 text-center"
    :role="variant === 'empty' ? 'status' : 'alert'"
  >
    <div class="grid size-[52px] place-items-center rounded-[14px] border" :class="iconClass">
      <component :is="icon" :size="24" :stroke-width="1.4" />
    </div>

    <div class="text-[14.5px] font-medium">{{ title }}</div>
    <p class="max-w-[340px] text-[12.5px] leading-relaxed text-ink3">{{ description }}</p>

    <span v-if="code" class="rounded bg-[#F2F0EA] px-1.5 py-0.5 font-mono text-[11px] text-ink2">
      {{ code }}
    </span>

    <button v-if="actionLabel" type="button" class="btn btn-primary mt-1" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>
