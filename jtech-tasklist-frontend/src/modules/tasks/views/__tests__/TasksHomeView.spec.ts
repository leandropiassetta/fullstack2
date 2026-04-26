// @vitest-environment jsdom

import { computed, defineComponent, reactive } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TasksHomeView from '@/modules/tasks/views/TasksHomeView.vue'
import { useTasklistsStore } from '@/stores/tasklists'
import { useTasksStore } from '@/stores/tasks'

vi.mock('@/api/tasks')
vi.mock('@/api/tasklists')

import { apiFetchTasks, apiCreateTask, apiUpdateTask } from '@/api/tasks'

const pushMock = vi.fn()
const replaceMock = vi.fn()
const routeMock = reactive({
  params: { tasklistId: 'tasklist-1' } as Record<string, string>,
})

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, replace: replaceMock }),
  useRoute: () => routeMock,
}))

const PassThroughStub = defineComponent({
  template: '<div><slot /></div>',
})

const SurfacePanelStub = defineComponent({
  props: { label: String, title: String, description: String },
  template:
    '<section><p>{{ label }}</p><h2>{{ title }}</h2><p>{{ description }}</p><slot /></section>',
})

const VFormStub = defineComponent({
  emits: ['submit'],
  template: '<form @submit.prevent="$emit(\'submit\', $event)"><slot /></form>',
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

const VTextareaStub = VTextFieldStub

const VBtnStub = defineComponent({
  props: { type: { type: String, default: 'button' } },
  setup(_, { attrs }) {
    return { attrs }
  },
  emits: ['click'],
  template:
    '<button v-bind="attrs" :type="type" @click="$emit(\'click\', $event)"><slot /></button>',
})

const VListItemStub = defineComponent({
  setup(_, { attrs }) {
    return { attrs }
  },
  emits: ['click'],
  template:
    '<div v-bind="attrs" @click="$emit(\'click\', $event)"><slot name="prepend" /><slot /><slot name="append" /></div>',
})

const VCheckboxBtnStub = defineComponent({
  props: { modelValue: { type: Boolean, default: false } },
  setup(_, { attrs }) {
    return { attrs }
  },
  emits: ['click'],
  template: '<button v-bind="attrs" @click="$emit(\'click\', $event)">toggle</button>',
})

const VDialogStub = defineComponent({
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue"><slot /></div>',
})

const VAlertStub = defineComponent({
  props: { text: { type: String, default: '' } },
  template: '<div>{{ text }}</div>',
})

describe('TasksHomeView', () => {
  beforeEach(() => {
    pushMock.mockReset()
    replaceMock.mockReset()
    routeMock.params = { tasklistId: 'tasklist-1' }
    vi.clearAllMocks()

    vi.mocked(apiFetchTasks).mockResolvedValue([])

    setActivePinia(createPinia())

    const tasklistsStore = useTasklistsStore()
    tasklistsStore.$patch({
      items: [{ id: 'tasklist-1', name: 'Trabalho' }],
      activeTasklistId: 'tasklist-1',
    })
  })

  function mountView() {
    return mount(TasksHomeView, {
      global: {
        stubs: {
          SurfacePanel: SurfacePanelStub,
          VContainer: PassThroughStub,
          VRow: PassThroughStub,
          VCol: PassThroughStub,
          VForm: VFormStub,
          VTextField: VTextFieldStub,
          VTextarea: VTextareaStub,
          VBtn: VBtnStub,
          VList: PassThroughStub,
          VListItem: VListItemStub,
          VListItemTitle: PassThroughStub,
          VListItemSubtitle: PassThroughStub,
          VIcon: PassThroughStub,
          VChip: PassThroughStub,
          VSheet: PassThroughStub,
          VDialog: VDialogStub,
          VCard: PassThroughStub,
          VCardTitle: PassThroughStub,
          VCardText: PassThroughStub,
          VCardActions: PassThroughStub,
          VSpacer: PassThroughStub,
          VCheckboxBtn: VCheckboxBtnStub,
          VSwitch: VTextFieldStub,
          VProgressLinear: PassThroughStub,
          VAlert: VAlertStub,
        },
      },
    })
  }

  it('creates a task from the UI', async () => {
    const created = {
      id: 'task-ui-1',
      title: 'Entregar proposta',
      description: null,
      completed: false,
      tasklistId: 'tasklist-1',
      createdAt: '2024-01-15T10:30:00',
    }
    vi.mocked(apiCreateTask).mockResolvedValue(created)

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="task-create-title"] input').setValue('Entregar proposta')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    const tasksStore = useTasksStore()
    expect(tasksStore.items).toHaveLength(1)
    expect(tasksStore.items[0]?.title).toBe('Entregar proposta')
  })

  it('shows duplicate feedback when API returns 409', async () => {
    const existingTask = {
      id: 'task-ui-1',
      title: 'Entregar proposta',
      description: null,
      completed: false,
      tasklistId: 'tasklist-1',
      createdAt: '2024-01-15T10:30:00',
    }
    vi.mocked(apiFetchTasks).mockResolvedValue([existingTask])
    vi.mocked(apiCreateTask).mockRejectedValue({ response: { status: 409 } })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="task-create-title"] input').setValue(' entregar proposta ')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Já existe uma task com esse título nesta lista.')
  })

  it('toggles completion from the UI', async () => {
    const task = {
      id: 'task-ui-1',
      title: 'Revisar contrato',
      description: null,
      completed: false,
      tasklistId: 'tasklist-1',
      createdAt: '2024-01-15T10:30:00',
    }
    vi.mocked(apiFetchTasks).mockResolvedValue([task])
    vi.mocked(apiUpdateTask).mockResolvedValue({ ...task, completed: true })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="task-toggle"]').trigger('click')
    await flushPromises()

    const tasksStore = useTasksStore()
    expect(tasksStore.items[0]?.completed).toBe(true)
  })
})
