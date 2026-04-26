// @vitest-environment jsdom

import { computed, defineComponent, reactive } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TasklistsHomeView from '@/modules/tasklists/views/TasklistsHomeView.vue'
import { useTasklistsStore } from '@/stores/tasklists'

const { mockRemoveTasksByTasklist } = vi.hoisted(() => ({
  mockRemoveTasksByTasklist: vi.fn(),
}))

vi.mock('@/api/tasklists')
vi.mock('@/stores/tasks', () => ({
  useTasksStore: () => ({
    items: [],
    isLoading: false,
    error: null,
    tasksByTasklist: () => [],
    fetchTasks: vi.fn(),
    createTask: vi.fn(),
    updateTask: vi.fn(),
    toggleTask: vi.fn(),
    removeTask: vi.fn(),
    removeTasksByTasklist: mockRemoveTasksByTasklist,
  }),
}))

import {
  apiFetchTasklists,
  apiCreateTasklist,
  apiDeleteTasklist,
} from '@/api/tasklists'

const pushMock = vi.fn()
const replaceMock = vi.fn()
const routeMock = reactive({
  params: {} as Record<string, string>,
})

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, replace: replaceMock }),
  useRoute: () => routeMock,
}))

const PassThroughStub = defineComponent({
  template: '<div><slot /></div>',
})

const SurfacePanelStub = defineComponent({
  props: {
    label: { type: String, default: '' },
    title: { type: String, default: '' },
    description: { type: String, default: '' },
  },
  template: '<section><p>{{ label }}</p><h2>{{ title }}</h2><p>{{ description }}</p><slot /></section>',
})

const VFormStub = defineComponent({
  template: '<form @submit.prevent="$emit(\'submit\', $event)"><slot /></form>',
  emits: ['submit'],
})

const VTextFieldStub = defineComponent({
  props: {
    modelValue: { type: String, default: '' },
    errorMessages: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  setup(props, { attrs, emit }) {
    const stringifiedErrors = computed(() =>
      (props.errorMessages as string[]).filter(Boolean).join(' '),
    )
    return {
      attrs,
      stringifiedErrors,
      emitUpdate: (event: Event) =>
        emit('update:modelValue', (event.target as HTMLInputElement).value),
    }
  },
  template:
    '<div><input v-bind="attrs" :value="modelValue" @input="emitUpdate" /><span v-if="stringifiedErrors">{{ stringifiedErrors }}</span></div>',
})

const VBtnStub = defineComponent({
  props: { type: { type: String, default: 'button' } },
  setup(_, { attrs }) {
    return { attrs }
  },
  template:
    '<button v-bind="attrs" :type="type" @click="$emit(\'click\', $event)"><slot /></button>',
  emits: ['click'],
})

const VListItemStub = defineComponent({
  setup(_, { attrs }) {
    return { attrs }
  },
  template:
    '<div v-bind="attrs" @click="$emit(\'click\', $event)"><slot name="prepend" /><slot /><slot name="append" /></div>',
  emits: ['click'],
})

const VDialogStub = defineComponent({
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue"><slot /></div>',
})

const VAlertStub = defineComponent({
  props: { text: { type: String, default: '' } },
  template: '<div>{{ text }}</div>',
})

describe('TasklistsHomeView', () => {
  beforeEach(() => {
    pushMock.mockReset()
    replaceMock.mockReset()
    routeMock.params = {}
    mockRemoveTasksByTasklist.mockReset()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  function mountView() {
    return mount(TasklistsHomeView, {
      global: {
        stubs: {
          SurfacePanel: SurfacePanelStub,
          VContainer: PassThroughStub,
          VRow: PassThroughStub,
          VCol: PassThroughStub,
          VForm: VFormStub,
          VTextField: VTextFieldStub,
          VTextarea: VTextFieldStub,
          VBtn: VBtnStub,
          VList: PassThroughStub,
          VListItem: VListItemStub,
          VListItemTitle: PassThroughStub,
          VListItemSubtitle: PassThroughStub,
          VIcon: PassThroughStub,
          VChip: PassThroughStub,
          VSheet: PassThroughStub,
          VSwitch: VTextFieldStub,
          VCheckboxBtn: VBtnStub,
          ExpandTransition: PassThroughStub,
          VDialog: VDialogStub,
          VCard: PassThroughStub,
          VCardTitle: PassThroughStub,
          VCardText: PassThroughStub,
          VCardActions: PassThroughStub,
          VSpacer: PassThroughStub,
          VProgressLinear: PassThroughStub,
          VAlert: VAlertStub,
        },
      },
    })
  }

  it('creates a tasklist from the UI', async () => {
    vi.mocked(apiFetchTasklists).mockResolvedValue([])
    const created = { id: 'tasklist-ui-1', name: 'Trabalho' }
    vi.mocked(apiCreateTasklist).mockResolvedValue(created)

    const wrapper = mountView()
    await flushPromises()

    const createInput = wrapper.find('[data-testid="tasklist-create-input"] input')
    await createInput.setValue('Trabalho')
    await wrapper.find('[data-testid="tasklist-create-submit"]').trigger('click')
    await flushPromises()

    const store = useTasklistsStore()
    expect(store.items).toHaveLength(1)
    expect(store.items[0]?.name).toBe('Trabalho')
    expect(pushMock).toHaveBeenCalledWith({
      name: 'app-tasklist-details',
      params: { tasklistSlug: 'trabalho' },
    })
  })

  it('shows duplicate-name feedback in the UI', async () => {
    vi.mocked(apiFetchTasklists).mockResolvedValue([])
    vi.mocked(apiCreateTasklist)
      .mockResolvedValueOnce({ id: 'tl-1', name: 'Trabalho' })
      .mockRejectedValueOnce({ response: { status: 409 } })

    const wrapper = mountView()
    await flushPromises()

    const createInput = wrapper.find('[data-testid="tasklist-create-input"] input')

    await createInput.setValue('Trabalho')
    await wrapper.find('[data-testid="tasklist-create-submit"]').trigger('click')
    await flushPromises()

    await createInput.setValue(' trabalho ')
    await wrapper.find('[data-testid="tasklist-create-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Já existe uma lista com esse nome.')
  })

  it('confirms deletion from the UI', async () => {
    const items = [
      { id: 'tasklist-ui-1', name: 'Casa' },
      { id: 'tasklist-ui-2', name: 'Trabalho' },
    ]
    vi.mocked(apiFetchTasklists).mockResolvedValue(items)
    vi.mocked(apiDeleteTasklist).mockResolvedValue(undefined)

    const wrapper = mountView()
    await flushPromises()

    const deleteButtons = wrapper.findAll('[data-testid="tasklist-delete-button"]')
    await deleteButtons[0].trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="tasklist-delete-confirm"]').trigger('click')
    await flushPromises()

    const store = useTasklistsStore()
    expect(store.items).toHaveLength(1)
    expect(store.items[0]?.name).toBe('Trabalho')
  })
})
