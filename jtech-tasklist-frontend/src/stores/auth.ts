import { defineStore } from 'pinia'

import { apiLogin, apiRegister, loginResponseToUser } from '@/api/auth'
import { AUTH_STORAGE_KEY } from '@/config/storageKeys'
import type { AuthSession, AuthUser, LoginCredentials, RegisterCredentials } from '@/types/auth'

function createEmptySession(): AuthSession {
  return {
    user: null,
    accessToken: null,
    refreshToken: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
  }
}

function loadPersistedSession(): AuthSession {
  if (typeof window === 'undefined') {
    return createEmptySession()
  }

  const storedSession = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!storedSession) {
    return createEmptySession()
  }

  try {
    const parsed = JSON.parse(storedSession) as Partial<AuthSession>

    if (
      parsed.user &&
      typeof parsed.user.id === 'string' &&
      typeof parsed.accessToken === 'string' &&
      parsed.isAuthenticated === true
    ) {
      return {
        user: {
          id: parsed.user.id,
          name: parsed.user.name,
          email: parsed.user.email,
        },
        accessToken: parsed.accessToken,
        refreshToken: parsed.refreshToken ?? null,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      }
    }
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
  }

  return createEmptySession()
}

function persistSession(user: AuthUser, accessToken: string, refreshToken: string | null) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({ user, accessToken, refreshToken, isAuthenticated: true }),
  )
}

function clearSession() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthSession => loadPersistedSession(),
  getters: {
    currentUserLabel: (state) =>
      state.user?.name ?? state.user?.email ?? state.user?.id ?? 'sem sessão',
  },
  actions: {
    setSession(user: AuthUser, accessToken: string, refreshToken: string | null) {
      this.user = user
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      this.isAuthenticated = true
      this.error = null
      persistSession(user, accessToken, refreshToken)
    },
    updateTokens(accessToken: string, refreshToken: string | null) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      if (this.user && this.accessToken) {
        persistSession(this.user, accessToken, refreshToken)
      }
    },
    async login(credentials: LoginCredentials) {
      this.isLoading = true
      this.error = null
      try {
        const response = await apiLogin(credentials.email, credentials.password)
        const user = loginResponseToUser(response)
        this.setSession(user, response.accessToken, response.refreshToken)
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status
        this.error = status === 401 ? 'E-mail ou senha incorretos.' : 'Erro ao fazer login. Tente novamente.'
        throw err
      } finally {
        this.isLoading = false
      }
    },
    async register(credentials: RegisterCredentials) {
      this.isLoading = true
      this.error = null
      try {
        await apiRegister(credentials.name, credentials.email, credentials.password)
        const response = await apiLogin(credentials.email, credentials.password)
        const user = loginResponseToUser(response)
        this.setSession(user, response.accessToken, response.refreshToken)
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status
        this.error = status === 409 ? 'Este e-mail já está cadastrado.' : 'Erro ao cadastrar. Tente novamente.'
        throw err
      } finally {
        this.isLoading = false
      }
    },
    logout() {
      this.user = null
      this.accessToken = null
      this.refreshToken = null
      this.isAuthenticated = false
      this.isLoading = false
      this.error = null
      clearSession()
    },
  },
})
