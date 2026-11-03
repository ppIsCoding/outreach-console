<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Check, Info, TriangleAlert, Upload } from 'lucide-vue-next'
import type { CreateTaskPayload, TaskPriority } from '@/api/types'
import { useCreateTaskMutation, useLinesQuery, useScriptsQuery } from '@/composables/useTasks'
import { formatEstimate, formatNumber } from '@/lib/utils'
import { useUiStore } from '@/stores/ui'
import AppDrawer from '@/components/ui/AppDrawer.vue'

const open = defineModel<boolean>('open', { required: true })

const ui = useUiStore()
const { data: scripts } = useScriptsQuery()
const { data: lines } = useLinesQuery()
const createMutation = useCreateTaskMutation()

const STEP_TITLES = ['基础信息', '呼叫对象', '外呼策略'] as const
const TOTAL_STEPS = STEP_TITLES.length

const step = ref(1)
const errors = reactive<Record<string, string>>({})

const form = reactive({
  name: '',
  scriptId: '',
  lineId: '',
  priority: 'NORMAL' as TaskPriority,
  source: 'UPLOAD' as 'UPLOAD' | 'POOL',
  totalContacts: 2400,
  dedupe: true,
  skipRecent: true,
  scheduleMode: 'NOW' as 'NOW' | 'SCHEDULED' | 'RECURRING',
  scheduledAt: '',
  concurrency: 20,
  retryLimit: 1,
  retryIntervalMin: 30,
  callWindowStart: '09:00',
  callWindowEnd: '20:00',
})

const selectedLine = computed(() => lines.value?.find((line) => line.id === form.lineId))
const lineCapacity = computed(() => selectedLine.value?.capacity ?? 50)
const lineAvailable = computed(() =>
  selectedLine.value ? selectedLine.value.capacity - selectedLine.value.used : 50,
)

const estimate = computed(() => {
  const remaining = Math.max(0, form.totalContacts)
  return formatEstimate((remaining * 45) / Math.max(1, form.concurrency))
})

watch(open, (value) => {
  if (!value) return
  step.value = 1
  Object.keys(errors).forEach((key) => delete errors[key])
  form.name = ''
  form.scriptId = scripts.value?.[0]?.id ?? ''
  form.lineId = lines.value?.[0]?.id ?? ''
  form.priority = 'NORMAL'
  form.source = 'UPLOAD'
  form.totalContacts = 2400
  form.dedupe = true
  form.skipRecent = true
  form.scheduleMode = 'NOW'
  form.concurrency = 20
  form.retryLimit = 1
  form.retryIntervalMin = 30
})

function validateStep(current: number): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])

  if (current === 1) {
    if (form.name.trim().length < 2) errors.name = '任务名称至少 2 个字符'
    else if (form.name.trim().length > 30) errors.name = '任务名称不能超过 30 个字符'
    if (!form.scriptId) errors.scriptId = '请选择话术模板'
    if (!form.lineId) errors.lineId = '请选择外呼线路'
  }

  if (current === 2) {
    if (form.totalContacts < 1) errors.totalContacts = '名单数量至少为 1'
    else if (form.totalContacts > 50000) errors.totalContacts = '单次最多导入 50,000 条'
  }

  if (current === 3) {
    if (form.concurrency < 1 || form.concurrency > 50) errors.concurrency = '并发数需在 1–50 之间'
    else if (form.concurrency > lineAvailable.value) errors.concurrency = `超出线路剩余并发（${lineAvailable.value}）`
    if (form.retryLimit < 0 || form.retryLimit > 3) errors.retryLimit = '重呼次数需在 0–3 之间'
    if (form.callWindowStart >= form.callWindowEnd) errors.callWindow = '开始时间需早于结束时间'
    if (form.scheduleMode === 'SCHEDULED' && !form.scheduledAt) errors.scheduledAt = '请选择执行时间'
  }

  return Object.keys(errors).length === 0
}

function next() {
  if (!validateStep(step.value)) return
  if (step.value < TOTAL_STEPS) step.value += 1
  else submit()
}

function back() {
  if (step.value > 1) step.value -= 1
}

function submit() {
  const payload: CreateTaskPayload = {
    name: form.name.trim(),
    scriptId: form.scriptId,
    lineId: form.lineId,
    priority: form.priority,
    source: form.source,
    totalContacts: form.totalContacts,
    dedupe: form.dedupe,
    skipRecent: form.skipRecent,
    scheduleMode: form.scheduleMode,
    scheduledAt: form.scheduleMode === 'SCHEDULED' ? form.scheduledAt : null,
    concurrency: form.concurrency,
    retryLimit: form.retryLimit,
    retryIntervalMin: form.retryIntervalMin,
    callWindowStart: form.callWindowStart,
    callWindowEnd: form.callWindowEnd,
  }

  createMutation.mutate(payload, {
    onSuccess: () => {
      ui.toast('任务已创建，等待调度执行')
      open.value = false
    },
    onError: (error: Error) => ui.toast(error.message || '创建失败，请重试', 'error'),
  })
}
</script>

<template>
  <AppDrawer v-model:open="open" title="新建外呼任务" subtitle="配置完成后将按策略自动调度执行">
    <div class="mb-6 flex items-center">
      <template v-for="(title, index) in STEP_TITLES" :key="title">
        <div
          class="flex items-center gap-2 text-[12.5px]"
          :class="step === index + 1 ? 'text-ink' : step > index + 1 ? 'text-ink2' : 'text-ink3'"
        >
          <span
            class="num grid size-[22px] place-items-center rounded-full border-[1.5px] text-[11px] transition-colors"
            :class="
              step > index + 1
                ? 'border-brand-line bg-brand-soft text-brand'
                : step === index + 1
                  ? 'border-brand bg-brand text-white'
                  : 'border-line-strong bg-white text-ink3'
            "
          >
            <Check v-if="step > index + 1" :size="12" :stroke-width="3" />
            <template v-else>{{ index + 1 }}</template>
          </span>
          {{ title }}
        </div>
        <div v-if="index < STEP_TITLES.length - 1" class="mx-2.5 h-px min-w-4 flex-1 bg-line" />
      </template>
    </div>

    <!-- Step 1 -->
    <div v-show="step === 1" class="space-y-4">
      <div>
        <label for="task-name" class="mb-1.5 flex items-center gap-1 text-[12.5px] font-medium text-ink2">
          任务名称 <span class="text-[11px] text-danger">*</span>
        </label>
        <input
          id="task-name"
          v-model="form.name"
          class="input"
          placeholder="例如：618大促-新客首呼"
          :aria-invalid="Boolean(errors.name)"
          aria-describedby="task-name-hint"
          maxlength="30"
        />
        <p id="task-name-hint" class="mt-1.5 text-[11.5px]" :class="errors.name ? 'text-danger' : 'text-ink4'">
          {{ errors.name || `${form.name.length} / 30 字符，同一创建人下不可重名` }}
        </p>
      </div>

      <div>
        <label for="task-script" class="mb-1.5 flex items-center gap-1 text-[12.5px] font-medium text-ink2">
          话术模板 <span class="text-[11px] text-danger">*</span>
        </label>
        <select id="task-script" v-model="form.scriptId" class="input select">
          <option v-for="script in scripts" :key="script.id" :value="script.id">
            {{ script.name }} {{ script.version }} · {{ script.industry }}
          </option>
        </select>
        <p class="mt-1.5 text-[11.5px] text-ink4">话术模板决定机器人的开场白与对话分支</p>
      </div>

      <div>
        <label for="task-line" class="mb-1.5 flex items-center gap-1 text-[12.5px] font-medium text-ink2">
          外呼线路 <span class="text-[11px] text-danger">*</span>
        </label>
        <select id="task-line" v-model="form.lineId" class="input select">
          <option v-for="line in lines" :key="line.id" :value="line.id">
            {{ line.name }}（可用 {{ line.capacity - line.used }} / {{ line.capacity }} 并发）
          </option>
        </select>
      </div>

      <div>
        <span class="mb-1.5 block text-[12.5px] font-medium text-ink2">优先级</span>
        <div class="grid grid-cols-2 gap-2">
          <button
            v-for="option in [
              { value: 'NORMAL', label: '普通', desc: '按创建顺序排队' },
              { value: 'HIGH', label: '高', desc: '优先占用线路并发' },
            ]"
            :key="option.value"
            type="button"
            class="rounded-[9px] border px-3 py-2.5 text-left transition-colors"
            :class="
              form.priority === option.value
                ? 'border-brand bg-brand-soft shadow-[inset_0_0_0_1px_var(--color-brand)]'
                : 'border-line bg-white hover:bg-surface2'
            "
            :aria-pressed="form.priority === option.value"
            @click="form.priority = option.value as TaskPriority"
          >
            <span class="block text-[12.5px] font-medium">{{ option.label }}</span>
            <span class="mt-0.5 block text-[11px] text-ink3">{{ option.desc }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Step 2 -->
    <div v-show="step === 2" class="space-y-4">
      <div>
        <span class="mb-1.5 block text-[12.5px] font-medium text-ink2">名单来源</span>
        <div class="grid grid-cols-2 gap-2">
          <button
            v-for="option in [
              { value: 'UPLOAD', label: '上传名单文件', desc: '支持 xlsx / csv' },
              { value: 'POOL', label: '选择号码池', desc: '从已有号码池圈选' },
            ]"
            :key="option.value"
            type="button"
            class="rounded-[9px] border px-3 py-2.5 text-left transition-colors"
            :class="
              form.source === option.value
                ? 'border-brand bg-brand-soft shadow-[inset_0_0_0_1px_var(--color-brand)]'
                : 'border-line bg-white hover:bg-surface2'
            "
            :aria-pressed="form.source === option.value"
            @click="form.source = option.value as 'UPLOAD' | 'POOL'"
          >
            <span class="flex items-center gap-1.5 text-[12.5px] font-medium">
              <Upload v-if="option.value === 'UPLOAD'" :size="14" />
              {{ option.label }}
            </span>
            <span class="mt-0.5 block text-[11px] text-ink3">{{ option.desc }}</span>
          </button>
        </div>
      </div>

      <div class="rounded-[10px] border-[1.5px] border-dashed border-line-strong bg-surface2 px-4 py-6 text-center">
        <p class="text-[13px] text-ink2">
          拖拽文件到此处，或 <span class="cursor-pointer font-medium text-brand">点击选择</span>
        </p>
        <p class="mt-1.5 text-[11.5px] text-ink4">需包含「手机号」列，单次上限 50,000 条</p>
      </div>

      <div>
        <label for="task-contacts" class="mb-1.5 block text-[12.5px] font-medium text-ink2">号码数量</label>
        <input
          id="task-contacts"
          v-model.number="form.totalContacts"
          type="number"
          class="input"
          min="1"
          max="50000"
          :aria-invalid="Boolean(errors.totalContacts)"
        />
        <p class="mt-1.5 text-[11.5px]" :class="errors.totalContacts ? 'text-danger' : 'text-ok'">
          <template v-if="errors.totalContacts">{{ errors.totalContacts }}</template>
          <template v-else>
            已解析 {{ formatNumber(form.totalContacts + 446) }} 条 · 去重后
            {{ formatNumber(form.totalContacts) }} 条
          </template>
        </p>
      </div>

      <div class="pt-1">
        <label class="flex cursor-pointer items-center gap-3 border-b border-dashed border-line py-3">
          <span class="flex-1">
            <span class="block text-[12.5px]">自动去重</span>
            <span class="block text-[11px] text-ink4">按手机号去重，保留最新记录</span>
          </span>
          <input v-model="form.dedupe" type="checkbox" class="peer sr-only" />
          <span
            class="relative h-[19px] w-[34px] shrink-0 rounded-full bg-[#D9D5CB] transition-colors peer-checked:bg-brand peer-focus-visible:ring-2 peer-focus-visible:ring-brand peer-focus-visible:ring-offset-2 after:absolute after:left-0.5 after:top-0.5 after:size-[15px] after:rounded-full after:bg-white after:shadow after:transition-transform peer-checked:after:translate-x-[15px]"
          />
        </label>

        <label class="flex cursor-pointer items-center gap-3 py-3">
          <span class="flex-1">
            <span class="block text-[12.5px]">过滤近 7 天已呼号码</span>
            <span class="block text-[11px] text-ink4">避免重复骚扰，提升客户体验</span>
          </span>
          <input v-model="form.skipRecent" type="checkbox" class="peer sr-only" />
          <span
            class="relative h-[19px] w-[34px] shrink-0 rounded-full bg-[#D9D5CB] transition-colors peer-checked:bg-brand peer-focus-visible:ring-2 peer-focus-visible:ring-brand peer-focus-visible:ring-offset-2 after:absolute after:left-0.5 after:top-0.5 after:size-[15px] after:rounded-full after:bg-white after:shadow after:transition-transform peer-checked:after:translate-x-[15px]"
          />
        </label>
      </div>

      <div class="flex gap-2.5 rounded-lg bg-info-soft px-3 py-2.5 text-xs leading-relaxed text-info">
        <Info :size="15" class="mt-px shrink-0" />
        <span>
          本次将外呼 <b class="num">{{ formatNumber(form.totalContacts) }}</b> 个号码，其中 446 个因近期已呼被过滤。
        </span>
      </div>
    </div>

    <!-- Step 3 -->
    <div v-show="step === 3" class="space-y-4">
      <div>
        <span class="mb-1.5 block text-[12.5px] font-medium text-ink2">执行方式</span>
        <div class="grid grid-cols-3 gap-2">
          <button
            v-for="option in [
              { value: 'NOW', label: '立即执行', desc: '提交后排队' },
              { value: 'SCHEDULED', label: '定时执行', desc: '指定开始时间' },
              { value: 'RECURRING', label: '周期执行', desc: '每天 / 每周' },
            ]"
            :key="option.value"
            type="button"
            class="rounded-[9px] border px-2.5 py-2.5 text-left transition-colors"
            :class="
              form.scheduleMode === option.value
                ? 'border-brand bg-brand-soft shadow-[inset_0_0_0_1px_var(--color-brand)]'
                : 'border-line bg-white hover:bg-surface2'
            "
            :aria-pressed="form.scheduleMode === option.value"
            @click="form.scheduleMode = option.value as typeof form.scheduleMode"
          >
            <span class="block text-[12.5px] font-medium">{{ option.label }}</span>
            <span class="mt-0.5 block text-[11px] text-ink3">{{ option.desc }}</span>
          </button>
        </div>
      </div>

      <div v-if="form.scheduleMode === 'SCHEDULED'">
        <label for="task-scheduled" class="mb-1.5 block text-[12.5px] font-medium text-ink2">开始时间</label>
        <input
          id="task-scheduled"
          v-model="form.scheduledAt"
          type="datetime-local"
          class="input"
          :aria-invalid="Boolean(errors.scheduledAt)"
        />
        <p v-if="errors.scheduledAt" class="mt-1.5 text-[11.5px] text-danger">{{ errors.scheduledAt }}</p>
      </div>

      <div class="grid grid-cols-2 gap-3.5">
        <div>
          <label for="task-concurrency" class="mb-1.5 block text-[12.5px] font-medium text-ink2">并发数</label>
          <input
            id="task-concurrency"
            v-model.number="form.concurrency"
            type="number"
            class="input"
            min="1"
            :max="lineCapacity"
            :aria-invalid="Boolean(errors.concurrency)"
          />
          <p class="mt-1.5 text-[11.5px]" :class="errors.concurrency ? 'text-danger' : 'text-ink4'">
            {{ errors.concurrency || `上限 ${lineAvailable}，受线路剩余并发限制` }}
          </p>
        </div>

        <div>
          <label for="task-retry" class="mb-1.5 block text-[12.5px] font-medium text-ink2">未接通重呼</label>
          <select id="task-retry" v-model.number="form.retryLimit" class="input select">
            <option :value="0">不重呼</option>
            <option :value="1">重呼 1 次</option>
            <option :value="2">重呼 2 次</option>
            <option :value="3">重呼 3 次</option>
          </select>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3.5">
        <div>
          <label for="task-interval" class="mb-1.5 block text-[12.5px] font-medium text-ink2">重呼间隔</label>
          <select id="task-interval" v-model.number="form.retryIntervalMin" class="input select">
            <option :value="15">15 分钟</option>
            <option :value="30">30 分钟</option>
            <option :value="60">1 小时</option>
            <option :value="120">2 小时</option>
          </select>
        </div>

        <div>
          <span class="mb-1.5 block text-[12.5px] font-medium text-ink2">呼叫时段</span>
          <div class="flex items-center gap-1.5">
            <input v-model="form.callWindowStart" type="time" class="input" aria-label="呼叫时段开始" />
            <span class="text-ink4">–</span>
            <input v-model="form.callWindowEnd" type="time" class="input" aria-label="呼叫时段结束" />
          </div>
          <p v-if="errors.callWindow" class="mt-1.5 text-[11.5px] text-danger">{{ errors.callWindow }}</p>
        </div>
      </div>

      <div class="rounded-[10px] border border-line bg-surface2 p-4">
        <div class="space-y-1.5 text-[12.5px]">
          <div class="flex justify-between gap-3">
            <span class="text-ink3">任务名称</span>
            <span class="truncate text-right">{{ form.name || '—' }}</span>
          </div>
          <div class="flex justify-between gap-3">
            <span class="text-ink3">话术模板</span>
            <span>{{ scripts?.find((item) => item.id === form.scriptId)?.name ?? '—' }}</span>
          </div>
          <div class="flex justify-between gap-3">
            <span class="text-ink3">外呼线路</span>
            <span>{{ selectedLine?.name ?? '—' }}</span>
          </div>
          <div class="flex justify-between gap-3">
            <span class="text-ink3">号码数量</span>
            <span class="num">{{ formatNumber(form.totalContacts) }}</span>
          </div>
        </div>

        <div class="my-2.5 h-px bg-line" />

        <div class="flex items-baseline gap-2">
          <span class="text-[11.5px] text-ink3">预计耗时</span>
          <span class="num text-lg font-semibold text-brand">{{ estimate }}</span>
        </div>
        <p class="mt-1 text-[11px] text-ink4">按平均通话 45s、并发 {{ form.concurrency }} 估算</p>
      </div>

      <div class="flex gap-2.5 rounded-lg bg-warn-soft px-3 py-2.5 text-xs leading-relaxed text-warn">
        <TriangleAlert :size="15" class="mt-px shrink-0" />
        <span>超出呼叫时段（{{ form.callWindowStart }}–{{ form.callWindowEnd }}）的号码将自动顺延至次日继续执行。</span>
      </div>
    </div>

    <template #footer>
      <span class="text-xs text-ink4">共 {{ TOTAL_STEPS }} 步，当前第 {{ step }} 步</span>
      <div class="ml-auto flex gap-2">
        <button v-if="step > 1" type="button" class="btn" @click="back">上一步</button>
        <button type="button" class="btn btn-primary" :disabled="createMutation.isPending.value" @click="next">
          {{ step === TOTAL_STEPS ? (createMutation.isPending.value ? '提交中…' : '提交并开始') : '下一步' }}
        </button>
      </div>
    </template>
  </AppDrawer>
</template>
