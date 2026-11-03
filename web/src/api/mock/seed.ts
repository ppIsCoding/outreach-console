import type { CallLine, Script, Task, TaskStatus } from '../types'

export const SCRIPTS: Script[] = [
  { id: 'S01', name: '新客回访', industry: '电商零售', version: 'v3.2' },
  { id: 'S02', name: '续费提醒', industry: 'SaaS', version: 'v1.8' },
  { id: 'S03', name: '活动通知', industry: '通用', version: 'v4.0' },
  { id: 'S04', name: 'NPS回访', industry: '通用', version: 'v1.2' },
  { id: 'S05', name: '唤醒话术', industry: '本地生活', version: 'v2.1' },
  { id: 'S06', name: '信息核验', industry: '通用', version: 'v2.0' },
]

export const LINES: CallLine[] = [
  { id: 'L01', name: '联通-沈阳主线路', provider: '中国联通', capacity: 50, used: 16 },
  { id: 'L02', name: '移动-全国线路', provider: '中国移动', capacity: 30, used: 18 },
  { id: 'L03', name: '电信-华东线路', provider: '中国电信', capacity: 20, used: 12 },
]

type Row = [
  name: string,
  scriptIdx: number,
  total: number,
  called: number,
  rate: number | null,
  status: TaskStatus,
  minutesAgo: number,
]

/**
 * 24 条种子数据，覆盖全部状态与边界：
 * - 超长任务名（第 6 条）
 * - 名单量为 0（第 24 条）
 * - 接通率 < 20% 的异常任务（第 9、23 条）
 * - calledCount = 0 且名单非空（第 5、11 条）
 */
const ROWS: Row[] = [
  ['618大促-新客首呼', 0, 2400, 1632, 41.2, 'RUNNING', 26],
  ['双11预售通知-高意向', 2, 3200, 980, 36.5, 'RUNNING', 18],
  ['新客首呼-华东区', 0, 3600, 1120, 39.8, 'RUNNING', 12],
  ['会员到期续费提醒-11月批次', 1, 1860, 1860, 52.7, 'COMPLETED', 1580],
  ['沉默用户唤醒-A组', 4, 5000, 0, null, 'SCHEDULED', 4],
  ['双十一大促期间高价值会员专属权益升级通知外呼任务（第二批）', 2, 2800, 0, null, 'DRAFT', 40],
  ['满意度回访-10月批次', 3, 1200, 1200, 61.3, 'COMPLETED', 2900],
  ['金融产品老客加推', 5, 800, 312, 18.4, 'PAUSED', 72],
  ['空号清洗-华北区', 5, 6500, 6500, 12.8, 'FAILED', 1700],
  ['课程试听邀约', 3, 1500, 640, 27.3, 'PAUSED', 58],
  ['老客生日关怀', 4, 960, 0, null, 'SCHEDULED', 95],
  ['企业客户续约提醒', 1, 420, 0, null, 'DRAFT', 22],
  ['双11尾款提醒-B组', 2, 2800, 2800, 44.9, 'COMPLETED', 1260],
  ['会员积分到期提醒', 1, 2100, 2100, 48.1, 'COMPLETED', 2100],
  ['新品上市调研', 3, 1600, 1600, 57.6, 'COMPLETED', 2600],
  ['沉睡商户激活', 4, 3000, 3000, 31.2, 'COMPLETED', 3300],
  ['售后服务满意度', 3, 900, 900, 63.4, 'COMPLETED', 3600],
  ['双11预售加购提醒', 2, 4200, 4200, 40.7, 'COMPLETED', 1500],
  ['高价值客户专属回访', 0, 380, 380, 68.2, 'COMPLETED', 4200],
  ['老客复购邀约', 3, 1100, 1100, 35.9, 'COMPLETED', 4600],
  ['续费到期预警-12月', 1, 1700, 0, null, 'SCHEDULED', -120],
  ['流失用户召回', 4, 2600, 2600, 22.6, 'COMPLETED', 5100],
  ['空号清洗-华南区', 5, 4800, 4800, 9.4, 'FAILED', 3800],
  ['名单为空的测试任务', 3, 0, 0, null, 'DRAFT', 8],
]

function pad(n: number) {
  return String(n).padStart(2, '0')
}

export function fmtDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function shiftMinutes(base: Date, minutes: number): string {
  return fmtDate(new Date(base.getTime() + minutes * 60_000))
}

export function buildSeedTasks(): Task[] {
  const now = new Date()
  return ROWS.map((row, index) => {
    const [name, scriptIdx, total, called, rate, status, minutesAgo] = row
    const script = SCRIPTS[scriptIdx]!
    const line = LINES[index % LINES.length]!
    const connected = rate === null ? 0 : Math.round((called * rate) / 100)
    const createdAt = shiftMinutes(now, -minutesAgo)
    return {
      id: `T20261103${String(index + 1).padStart(3, '0')}`,
      name,
      scriptId: script.id,
      scriptName: script.name,
      scriptVersion: script.version,
      lineId: line.id,
      lineName: line.name,
      status,
      priority: index % 7 === 0 ? 'HIGH' : 'NORMAL',
      totalContacts: total,
      calledCount: called,
      connectedCount: connected,
      intentCount: Math.round(connected * 0.18),
      totalDurationSec: connected * 43,
      concurrency: [20, 16, 24, 12, 10][index % 5]!,
      retryLimit: index % 3,
      retryIntervalMin: 30,
      callWindow: '09:00-20:00',
      scheduledAt: status === 'SCHEDULED' ? shiftMinutes(now, minutesAgo) : null,
      startedAt: ['RUNNING', 'PAUSED', 'COMPLETED', 'FAILED'].includes(status)
        ? shiftMinutes(now, -minutesAgo - 1)
        : null,
      finishedAt: status === 'COMPLETED' || status === 'FAILED' ? shiftMinutes(now, -minutesAgo + 30) : null,
      creatorName: ['李思远', '王雨桐', '陈默'][index % 3]!,
      createdAt,
      updatedAt: createdAt,
      version: 0,
    }
  })
}
