import { describe, expect, it } from 'vitest'
import { ACTION_META, FILTER_TABS, STATUS_META } from './task-status'
import type { TaskStatus } from '@/api/types'

const ALL_STATUSES: TaskStatus[] = [
  'DRAFT',
  'SCHEDULED',
  'RUNNING',
  'PAUSED',
  'COMPLETED',
  'FAILED',
  'TERMINATED',
  'CANCELLED',
]

describe('STATUS_META', () => {
  it('覆盖全部状态', () => {
    expect(Object.keys(STATUS_META).sort()).toEqual([...ALL_STATUSES].sort())
  })

  it('每个状态都有中文标签与样式', () => {
    for (const status of ALL_STATUSES) {
      expect(STATUS_META[status].label.length).toBeGreaterThan(0)
      expect(STATUS_META[status].badge.length).toBeGreaterThan(0)
      expect(STATUS_META[status].dot.length).toBeGreaterThan(0)
    }
  })
})

describe('状态机（前端只读副本）', () => {
  it('执行中只能暂停或终止', () => {
    expect(STATUS_META.RUNNING.actions).toEqual(['PAUSE', 'TERMINATE'])
  })

  it('已暂停可以恢复或终止', () => {
    expect(STATUS_META.PAUSED.actions).toEqual(['RESUME', 'TERMINATE'])
  })

  it('草稿只能提交，不能直接执行', () => {
    expect(STATUS_META.DRAFT.actions).toEqual(['SUBMIT'])
  })

  it('终态不允许任何操作', () => {
    for (const status of ['COMPLETED', 'FAILED', 'TERMINATED', 'CANCELLED'] as TaskStatus[]) {
      expect(STATUS_META[status].actions).toEqual([])
    }
  })

  it('每个动作都有中文文案', () => {
    for (const meta of Object.values(STATUS_META)) {
      for (const action of meta.actions) {
        expect(ACTION_META[action].label.length).toBeGreaterThan(0)
      }
    }
  })
})

describe('FILTER_TABS', () => {
  it('第一个 Tab 是「全部」', () => {
    expect(FILTER_TABS[0]?.key).toBe('ALL')
  })

  it('覆盖需求文档约定的 6 个筛选态', () => {
    expect(FILTER_TABS.map((tab) => tab.key)).toEqual([
      'ALL',
      'RUNNING',
      'SCHEDULED',
      'PAUSED',
      'COMPLETED',
      'FAILED',
    ])
  })
})
