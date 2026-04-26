import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import { registerAuthGuards } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'

describe('router guards', () => {
  beforeEach(() => {
    const storage = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => {
        storage.set(key, value)
      },
      removeItem: (key: string) => {
        storage.delete(key)
      },
      clear: () => {
        storage.clear()
      },
    })
  })

  it('redirects guest users to login when accessing protected routes', async () => {
    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', name: 'login', component: { template: '<div>login</div>' }, meta: { guestOnly: true } },
        {
          path: '/app/tasklists',
          name: 'app-tasklists',
          component: { template: '<div>tasklists</div>' },
          meta: { requiresAuth: true },
        },
      ],
    })
    registerAuthGuards(router, pinia)

    await router.push('/app/tasklists')

    expect(router.currentRoute.value.fullPath).toBe('/login?redirectTo=/app/tasklists')
  })

  it('redirects authenticated users away from login', async () => {
    const pinia = createPinia()
    const authStore = useAuthStore(pinia)
    authStore.setSession(
      { id: 'user-1', name: 'User 1', email: 'user1@test.com' },
      'token-1',
      'refresh-1',
    )

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', name: 'login', component: { template: '<div>login</div>' }, meta: { guestOnly: true } },
        {
          path: '/app/tasklists',
          name: 'app-tasklists',
          component: { template: '<div>tasklists</div>' },
          meta: { requiresAuth: true },
        },
      ],
    })
    registerAuthGuards(router, pinia)
    await router.push('/login')

    expect(router.currentRoute.value.fullPath).toBe('/app/tasklists')
  })
})
