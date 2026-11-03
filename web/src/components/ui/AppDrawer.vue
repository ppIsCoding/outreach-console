<script setup lang="ts">
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { X } from 'lucide-vue-next'

const open = defineModel<boolean>('open', { required: true })

const { title, subtitle = '', width = '620px' } = defineProps<{
  title: string
  subtitle?: string
  width?: string
}>()
</script>

<template>
  <DialogRoot v-model:open="open">
    <DialogPortal>
      <DialogOverlay class="drawer-overlay" />
      <DialogContent class="drawer-content" :style="{ width }">
        <header class="flex shrink-0 items-start gap-3 border-b border-line px-6 pb-4 pt-5">
          <div class="min-w-0 flex-1">
            <DialogTitle class="text-base font-semibold leading-tight">{{ title }}</DialogTitle>
            <DialogDescription :class="subtitle ? 'mt-1 text-xs text-ink3' : 'sr-only'">
              {{ subtitle || '对话框' }}
            </DialogDescription>
          </div>
          <slot name="badge" />
          <button
            type="button"
            class="grid size-8 shrink-0 place-items-center rounded-lg text-ink2 transition-colors hover:bg-[#ECEAE4] hover:text-ink"
            aria-label="关闭"
            @click="open = false"
          >
            <X :size="17" :stroke-width="1.8" />
          </button>
        </header>

        <div class="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          <slot />
        </div>

        <footer
          v-if="$slots.footer"
          class="flex shrink-0 items-center gap-2.5 border-t border-line bg-surface2 px-6 py-3.5"
        >
          <slot name="footer" />
        </footer>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgb(18 22 28 / 0.34);
  backdrop-filter: blur(1.5px);
}
.drawer-overlay[data-state='open'] {
  animation: overlay-in 0.22s var(--ease-out-soft);
}
.drawer-overlay[data-state='closed'] {
  animation: overlay-out 0.18s var(--ease-out-soft);
}

.drawer-content {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 41;
  display: flex;
  max-width: 96vw;
  flex-direction: column;
  background: var(--color-surface);
  box-shadow: var(--shadow-float);
  outline: none;
}
.drawer-content[data-state='open'] {
  animation: drawer-in 0.26s var(--ease-out-soft);
}
.drawer-content[data-state='closed'] {
  animation: drawer-out 0.2s var(--ease-out-soft);
}

@keyframes overlay-in {
  from {
    opacity: 0;
  }
}
@keyframes overlay-out {
  to {
    opacity: 0;
  }
}
@keyframes drawer-in {
  from {
    transform: translateX(100%);
  }
}
@keyframes drawer-out {
  to {
    transform: translateX(100%);
  }
}
</style>
