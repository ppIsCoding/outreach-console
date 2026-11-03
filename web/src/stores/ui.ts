import { defineStore } from 'pinia'
import { setScenario, type Scenario } from '@/api/mock/db'

export type ToastType = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  message: string
  type: ToastType
}

let toastId = 0

export const useUiStore = defineStore('ui', {
  state: () => ({
    createOpen: false,
    detailId: null as string | null,
    toasts: [] as Toast[],
    scenario: 'normal' as Scenario,
  }),

  actions: {
    toast(message: string, type: ToastType = 'success') {
      const id = ++toastId
      this.toasts.push({ id, message, type })
      window.setTimeout(() => this.dismiss(id), 2800)
    },

    dismiss(id: number) {
      this.toasts = this.toasts.filter((toast) => toast.id !== id)
    },

    openCreate() {
      this.createOpen = true
    },

    closeCreate() {
      this.createOpen = false
    },

    openDetail(id: string) {
      this.detailId = id
    },

    closeDetail() {
      this.detailId = null
    },

    switchScenario(next: Scenario) {
      this.scenario = next
      setScenario(next)
    },
  },
})
