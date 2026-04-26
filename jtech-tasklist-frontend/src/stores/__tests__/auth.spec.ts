import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/api/auth')>()
  return {
    ...original,
    apiLogin: vi.fn(),
    apiRegister: vi.fn(),
  }
})

import { apiLogin, apiRegister } from '@/api/auth'

const mockLoginResponse = {
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-123',
  userId: 'user-id-123',
  name: 'Leandro',
  email: 'leandro@example.com',
}

describe('auth store', () => {
  let storage = new Map<string, string>()

  beforeEach(() => {
    storage = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
    })
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  describe('login', () => {
    it('calls apiLogin and sets authenticated session', async () => {
      vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)

      const store = useAuthStore()
      await store.login({ email: 'leandro@example.com', password: '123456' })

      expect(apiLogin).toHaveBeenCalledWith('leandro@example.com', '123456')
      expect(store.isAuthenticated).toBe(true)
      expect(store.user).toMatchObject({
        id: 'user-id-123',
        name: 'Leandro',
        email: 'leandro@example.com',
      })
      expect(store.accessToken).toBe('access-token-123')
      expect(store.refreshToken).toBe('refresh-token-123')
    })

    it('sets 401 error message and re-throws', async () => {
      const err = { response: { status: 401 } }
      vi.mocked(apiLogin).mockRejectedValue(err)

      const store = useAuthStore()
      await expect(store.login({ email: 'x@x.com', password: 'wrong' })).rejects.toBe(err)

      expect(store.error).toBe('E-mail ou senha incorretos.')
      expect(store.isAuthenticated).toBe(false)
    })

    it('sets generic error on non-401 failure', async () => {
      vi.mocked(apiLogin).mockRejectedValue(new Error('Network error'))

      const store = useAuthStore()
      await expect(store.login({ email: 'x@x.com', password: '123456' })).rejects.toBeDefined()

      expect(store.error).toBe('Erro ao fazer login. Tente novamente.')
    })
  })

  describe('register', () => {
    it('calls apiRegister then apiLogin, then sets session', async () => {
      vi.mocked(apiRegister).mockResolvedValue(undefined)
      vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)

      const store = useAuthStore()
      await store.register({ name: 'Leandro', email: 'leandro@example.com', password: '123456' })

      expect(apiRegister).toHaveBeenCalledWith('Leandro', 'leandro@example.com', '123456')
      expect(apiLogin).toHaveBeenCalledWith('leandro@example.com', '123456')
      expect(store.isAuthenticated).toBe(true)
      expect(store.user?.name).toBe('Leandro')
    })

    it('sets 409 error message and re-throws', async () => {
      const err = { response: { status: 409 } }
      vi.mocked(apiRegister).mockRejectedValue(err)

      const store = useAuthStore()
      await expect(
        store.register({ name: 'X', email: 'x@x.com', password: '123456' }),
      ).rejects.toBe(err)

      expect(store.error).toBe('Este e-mail já está cadastrado.')
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('setSession', () => {
    it('persists to localStorage and updates computed label', () => {
      const store = useAuthStore()

      store.setSession({ id: 'u42', name: 'Test User', email: 't@t.com' }, 'token-42', 'refresh-42')

      expect(store.currentUserLabel).toBe('Test User')
      expect(store.isAuthenticated).toBe(true)
      const persisted = JSON.parse(
        globalThis.localStorage.getItem('jtech-tasklist.auth') ?? '{}',
      )
      expect(persisted).toMatchObject({ accessToken: 'token-42', isAuthenticated: true })
    })
  })

  describe('updateTokens', () => {
    it('updates tokens but does not overwrite user', async () => {
      vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)

      const store = useAuthStore()
      await store.login({ email: 'leandro@example.com', password: '123456' })

      store.updateTokens('new-access', 'new-refresh')

      expect(store.accessToken).toBe('new-access')
      expect(store.refreshToken).toBe('new-refresh')
      expect(store.user?.name).toBe('Leandro')
    })
  })

  describe('logout', () => {
    it('clears state and removes from localStorage', async () => {
      vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)

      const store = useAuthStore()
      await store.login({ email: 'leandro@example.com', password: '123456' })
      store.logout()

      expect(store.isAuthenticated).toBe(false)
      expect(store.user).toBeNull()
      expect(store.accessToken).toBeNull()
      expect(store.refreshToken).toBeNull()
      expect(globalThis.localStorage.getItem('jtech-tasklist.auth')).toBeNull()
    })
  })

  describe('localStorage hydration', () => {
    it('hydrates session on store init', () => {
      globalThis.localStorage.setItem(
        'jtech-tasklist.auth',
        JSON.stringify({
          user: { id: 'u1', name: 'Persisted', email: 'persisted@test.com' },
          accessToken: 'persisted-access',
          refreshToken: 'persisted-refresh',
          isAuthenticated: true,
        }),
      )

      setActivePinia(createPinia())
      const store = useAuthStore()

      expect(store.isAuthenticated).toBe(true)
      expect(store.user?.email).toBe('persisted@test.com')
      expect(store.accessToken).toBe('persisted-access')
      expect(store.refreshToken).toBe('persisted-refresh')
    })
  })
})
