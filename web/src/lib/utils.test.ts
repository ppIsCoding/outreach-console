import { describe, expect, it } from 'vitest'
import { connectRateOf, formatDuration, formatEstimate, formatNumber, formatPercent, progressOf } from './utils'

describe('formatNumber', () => {
  it('千分位分隔', () => {
    expect(formatNumber(12480)).toBe('12,480')
  })

  it('空值返回占位符', () => {
    expect(formatNumber(null)).toBe('—')
    expect(formatNumber(undefined)).toBe('—')
  })

  it('0 是有效值，不应被当成空', () => {
    expect(formatNumber(0)).toBe('0')
  })
})

describe('formatPercent', () => {
  it('默认保留一位小数', () => {
    expect(formatPercent(38.24)).toBe('38.2%')
  })

  it('空值返回占位符', () => {
    expect(formatPercent(null)).toBe('—')
  })
})

describe('formatDuration', () => {
  it('秒转分秒', () => {
    expect(formatDuration(102)).toBe('1′42″')
  })

  it('不足一分钟补零', () => {
    expect(formatDuration(9)).toBe('0′09″')
  })

  it('0 秒返回占位符', () => {
    expect(formatDuration(0)).toBe('—')
  })
})

describe('formatEstimate', () => {
  it('不足一分钟', () => {
    expect(formatEstimate(30)).toBe('不到 1 分钟')
  })

  it('小时 + 分钟', () => {
    expect(formatEstimate(5400)).toBe('约 1 小时 30 分')
  })

  it('整小时不显示 0 分', () => {
    expect(formatEstimate(7200)).toBe('约 2 小时')
  })
})

describe('progressOf', () => {
  it('计算百分比并四舍五入', () => {
    expect(progressOf({ calledCount: 1632, totalContacts: 2400 })).toBe(68)
  })

  it('名单为 0 时返回 0，不产生 NaN', () => {
    expect(progressOf({ calledCount: 0, totalContacts: 0 })).toBe(0)
  })

  it('超出名单量时封顶 100', () => {
    expect(progressOf({ calledCount: 3000, totalContacts: 2400 })).toBe(100)
  })
})

describe('connectRateOf', () => {
  it('已呼为 0 时返回 null（UI 显示 —）', () => {
    expect(connectRateOf({ calledCount: 0, connectedCount: 0 })).toBeNull()
  })

  it('正常计算接通率', () => {
    expect(connectRateOf({ calledCount: 1000, connectedCount: 412 })).toBeCloseTo(41.2, 5)
  })
})
