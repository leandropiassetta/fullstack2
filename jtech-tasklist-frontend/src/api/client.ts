import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'

import { AUTH_STORAGE_KEY } from '@/config/storageKeys'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

let isRefreshing = false
let pendingQueue: Array<(token: string) => void> = []
let rejectQueue: Array<(reason: unknown) => void> = []

function flushQueue(token: string) {
  pendingQueue.forEach((resolve) => resolve(token))
  pendingQueue = []
  rejectQueue = []
}

function rejectPendingQueue(reason: unknown) {
  rejectQueue.forEach((reject) => reject(reason))
  pendingQueue = []
  rejectQueue = []
}

function readStoredToken(): { accessToken: string | null; refreshToken: string | null } {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return { accessToken: null, refreshToken: null }
    const session = JSON.parse(raw)
    return {
      accessToken: session?.accessToken ?? null,
      refreshToken: session?.refreshToken ?? null,
    }
  } catch {
    return { accessToken: null, refreshToken: null }
  }
}

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { accessToken } = readStoredToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    const url = originalRequest.url ?? ''
    const isAuthEndpoint =
      url.includes('/auth/login') ||
      url.includes('/auth/refresh') ||
      url.includes('/auth/register')

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(client(originalRequest))
          })
          rejectQueue.push(reject)
        })
      }

      isRefreshing = true

      try {
        const { refreshToken } = readStoredToken()
        if (!refreshToken) throw new Error('No refresh token available')

        const response = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
          { refreshToken },
        )

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data

        const { useAuthStore } = await import('@/stores/auth')
        useAuthStore().updateTokens(newAccessToken, newRefreshToken)

        flushQueue(newAccessToken)
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return client(originalRequest)
      } catch (refreshError) {
        rejectPendingQueue(refreshError)

        const { useAuthStore } = await import('@/stores/auth')
        useAuthStore().logout()

        const { default: router } = await import('@/router')
        router.push('/login')

        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)

export default client
