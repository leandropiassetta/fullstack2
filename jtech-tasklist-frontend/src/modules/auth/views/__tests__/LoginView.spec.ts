// @vitest-environment jsdom

import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import LoginView from '@/modules/auth/views/LoginView.vue'
import { vuetify } from '@/plugins/vuetify'
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
  accessToken: 'access-token-test',
  refreshToken: 'refresh-token-test',
  userId: 'user-id-test',
  name: 'Leandro',
  email: 'leandro@example.com',
}

const pushMock = vi.fn()
const routeMock = {
  query: {} as Record<string, string>,
}

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
  useRoute: () => routeMock,
}))

describe('LoginView', () => {
  beforeEach(() => {
    pushMock.mockReset()
    routeMock.query = {}
    vi.clearAllMocks()
    vi.stubGlobal(
      'ResizeObserver',
      class {
        observe() {}
        unobserve() {}
        disconnect() {}
      },
    )

    const storage = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
    })

    setActivePinia(createPinia())
  })

  it('keeps submit disabled while fields are empty', () => {
    const wrapper = mount(LoginView, {
      attachTo: document.body,
      global: { plugins: [vuetify] },
    })

    const submitButton = wrapper.find('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()

    wrapper.unmount()
  })

  it('authenticates and redirects after valid login', async () => {
    vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)

    const wrapper = mount(LoginView, {
      attachTo: document.body,
      global: { plugins: [vuetify] },
    })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('leandro@example.com')
    await inputs[1].setValue('123456')
    await wrapper.vm.$nextTick()

    const submitButton = wrapper.find('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeUndefined()

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    const authStore = useAuthStore()
    expect(authStore.isAuthenticated).toBe(true)
    expect(authStore.user).toMatchObject({
      id: 'user-id-test',
      email: 'leandro@example.com',
    })
    expect(pushMock).toHaveBeenCalledWith({ name: 'app-tasklists' })

    wrapper.unmount()
  })

  it('redirects to the originally requested route after login', async () => {
    vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)
    routeMock.query = { redirectTo: '/app/tasklists/tasklist-1' }

    const wrapper = mount(LoginView, {
      attachTo: document.body,
      global: { plugins: [vuetify] },
    })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('leandro@example.com')
    await inputs[1].setValue('123456')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith('/app/tasklists/tasklist-1')

    wrapper.unmount()
  })

  it('registers, auto-logins and redirects after valid signup', async () => {
    vi.mocked(apiRegister).mockResolvedValue(undefined)
    vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse)

    const wrapper = mount(LoginView, {
      attachTo: document.body,
      global: { plugins: [vuetify] },
    })

    const tabs = wrapper.findAll('[role="tab"]')
    await tabs[1].trigger('click')
    await flushPromises()

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('Leandro')
    await inputs[1].setValue('leandro@example.com')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('123456')
    await wrapper.vm.$nextTick()

    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(apiRegister).toHaveBeenCalledWith('Leandro', 'leandro@example.com', '123456')
    expect(apiLogin).toHaveBeenCalledWith('leandro@example.com', '123456')

    const authStore = useAuthStore()
    expect(authStore.isAuthenticated).toBe(true)
    expect(pushMock).toHaveBeenCalledWith({ name: 'app-tasklists' })

    wrapper.unmount()
  })
})
