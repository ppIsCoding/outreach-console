<script setup lang="ts">
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppTopbar from '@/components/layout/AppTopbar.vue'
import AppToaster from '@/components/ui/AppToaster.vue'
import ScenarioSwitcher from '@/components/dev/ScenarioSwitcher.vue'
import { IS_MOCK } from '@/api'
import { useTaskProgressSync } from '@/composables/useTasks'

useTaskProgressSync()
</script>

<template>
  <div class="flex h-full overflow-hidden bg-canvas text-ink">
    <AppSidebar />

    <div class="flex min-w-0 flex-1 flex-col">
      <AppTopbar />
      <main class="flex-1 overflow-y-auto">
        <RouterView />
      </main>
    </div>

    <AppToaster />
    <ScenarioSwitcher v-if="IS_MOCK" />
  </div>
</template>
