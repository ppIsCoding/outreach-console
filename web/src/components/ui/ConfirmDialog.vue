<script setup lang="ts">
import { AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogOverlay, AlertDialogPortal, AlertDialogRoot, AlertDialogTitle } from 'reka-ui'

const open = defineModel<boolean>('open', { required: true })

const {
  title,
  description,
  confirmLabel = '确认',
  danger = false,
} = defineProps<{
  title: string
  description: string
  confirmLabel?: string
  danger?: boolean
}>()

const emit = defineEmits<{ confirm: [] }>()

function confirm() {
  emit('confirm')
  open.value = false
}
</script>

<template>
  <AlertDialogRoot v-model:open="open">
    <AlertDialogPortal>
      <AlertDialogOverlay class="fixed inset-0 z-50 bg-[#12161C]/40 backdrop-blur-[1.5px]" />
      <AlertDialogContent
        class="fixed left-1/2 top-1/2 z-51 w-[400px] max-w-[92vw] -translate-x-1/2 -translate-y-1/2 rounded-[14px] border border-line bg-surface p-5 shadow-[var(--shadow-float)]"
      >
        <AlertDialogTitle class="text-[15px] font-semibold">{{ title }}</AlertDialogTitle>
        <AlertDialogDescription class="mt-2 text-[12.5px] leading-relaxed text-ink3">
          {{ description }}
        </AlertDialogDescription>

        <div class="mt-5 flex justify-end gap-2">
          <AlertDialogCancel class="btn">取消</AlertDialogCancel>
          <AlertDialogAction
            class="btn"
            :class="danger ? 'border-danger bg-danger text-white hover:bg-[#9C3A26]' : 'btn-primary'"
            @click="confirm"
          >
            {{ confirmLabel }}
          </AlertDialogAction>
        </div>
      </AlertDialogContent>
    </AlertDialogPortal>
  </AlertDialogRoot>
</template>
