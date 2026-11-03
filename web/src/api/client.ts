import type { ApiEnvelope } from './types'

export class ApiError extends Error {
  code: number
  fields?: Record<string, string>

  constructor(code: number, message: string, fields?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.fields = fields
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE ?? ''

export async function httpRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 15_000)

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(init.headers ?? {}),
      },
      signal: controller.signal,
    })

    const payload = (await response.json()) as ApiEnvelope<T>
    if (!response.ok || payload.code !== 0) {
      throw new ApiError(payload.code ?? response.status, payload.message ?? '请求失败')
    }
    return payload.data
  } catch (error) {
    if (error instanceof ApiError) throw error
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError(50400, '请求超时，请检查网络后重试')
    }
    throw new ApiError(50000, '网络异常，请稍后重试')
  } finally {
    clearTimeout(timeout)
  }
}
