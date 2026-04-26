import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useTasksStore } from '@/stores/tasks'
import type { TaskItem } from '@/types/task'

vi.mock('@/api/tasks')

import { apiFetchTasks, apiCreateTask, apiUpdateTask, apiDeleteTask } from '@/api/tasks'

const makeTask = (overrides: Partial<TaskItem> = {}): TaskItem => ({
  id: 'task-id-1',
  title: 'Task teste',
  description: null,
  completed: false,
  tasklistId: 'tl-1',
  createdAt: '2024-01-15T10:30:00',
  ...overrides,
})

describe('tasks store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  describe('fetchTasks', () => {
    it('loads tasks for the given tasklist', async () => {
      const tasks = [makeTask({ id: 't1' }), makeTask({ id: 't2' })]
      vi.mocked(apiFetchTasks).mockResolvedValue(tasks)

      const store = useTasksStore()
      await store.fetchTasks('tl-1')

      expect(apiFetchTasks).toHaveBeenCalledWith('tl-1')
      expect(store.items).toHaveLength(2)
      expect(store.isLoading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('replaces previous tasks for the same tasklist on re-fetch', async () => {
      vi.mocked(apiFetchTasks).mockResolvedValueOnce([makeTask({ id: 't1' })])
      vi.mocked(apiFetchTasks).mockResolvedValueOnce([makeTask({ id: 't2', title: 'Updated' })])

      const store = useTasksStore()
      await store.fetchTasks('tl-1')
      await store.fetchTasks('tl-1')

      expect(store.items).toHaveLength(1)
      expect(store.items[0]?.id).toBe('t2')
    })

    it('sets error on failure', async () => {
      vi.mocked(apiFetchTasks).mockRejectedValue(new Error('Network'))

      const store = useTasksStore()
      await store.fetchTasks('tl-1')

      expect(store.error).toBe('Não foi possível carregar as tasks.')
      expect(store.items).toHaveLength(0)
    })
  })

  describe('createTask', () => {
    it('creates task and prepends to items', async () => {
      const created = makeTask({ id: 'new-t', title: 'Comprar pão' })
      vi.mocked(apiCreateTask).mockResolvedValue(created)

      const store = useTasksStore()
      const result = await store.createTask({ title: '  Comprar pão  ', tasklistId: 'tl-1' })

      expect(apiCreateTask).toHaveBeenCalledWith({
        title: 'Comprar pão',
        description: null,
        tasklistId: 'tl-1',
      })
      expect(result).toEqual(created)
      expect(store.items[0]).toEqual(created)
    })

    it('throws if title is empty', async () => {
      const store = useTasksStore()
      await expect(store.createTask({ title: '  ', tasklistId: 'tl-1' })).rejects.toThrow(
        'Informe um título para a task.',
      )
    })

    it('throws if tasklistId is missing', async () => {
      const store = useTasksStore()
      await expect(store.createTask({ title: 'Test', tasklistId: '' })).rejects.toThrow(
        'Selecione uma lista válida para a task.',
      )
    })

    it('throws if description exceeds 500 characters', async () => {
      const store = useTasksStore()
      await expect(
        store.createTask({ title: 'Test', description: 'x'.repeat(501), tasklistId: 'tl-1' }),
      ).rejects.toThrow('A descrição pode ter no máximo 500 caracteres.')
    })

    it('sets 409 as duplicate title message', async () => {
      const err = { response: { status: 409 } }
      vi.mocked(apiCreateTask).mockRejectedValue(err)

      const store = useTasksStore()
      const result = await store.createTask({ title: 'Task', tasklistId: 'tl-1' })

      expect(result).toBeNull()
      expect(store.error).toBe('Já existe uma task com esse título nesta lista.')
    })
  })

  describe('updateTask', () => {
    it('updates task in items on success', async () => {
      const original = makeTask({ id: 't1', title: 'Old' })
      const updated = makeTask({ id: 't1', title: 'New', completed: true })
      vi.mocked(apiFetchTasks).mockResolvedValue([original])
      vi.mocked(apiUpdateTask).mockResolvedValue(updated)

      const store = useTasksStore()
      await store.fetchTasks('tl-1')
      const result = await store.updateTask('t1', {
        title: 'New',
        description: null,
        completed: true,
        tasklistId: 'tl-1',
      })

      expect(result).toEqual(updated)
      expect(store.items[0]).toEqual(updated)
    })

    it('throws if title is empty', async () => {
      const store = useTasksStore()
      await expect(
        store.updateTask('t1', { title: '', description: null, completed: false, tasklistId: 'tl-1' }),
      ).rejects.toThrow('Informe um título para a task.')
    })

    it('sets 409 as duplicate title message', async () => {
      const err = { response: { status: 409 } }
      vi.mocked(apiUpdateTask).mockRejectedValue(err)

      const store = useTasksStore()
      const result = await store.updateTask('t1', {
        title: 'Dup',
        description: null,
        completed: false,
        tasklistId: 'tl-1',
      })

      expect(result).toBeNull()
      expect(store.error).toBe('Já existe uma task com esse título nesta lista.')
    })
  })

  describe('toggleTask', () => {
    it('sends full body with inverted completed and tasklistId', async () => {
      const task = makeTask({
        id: 't1',
        title: 'T',
        description: 'Desc',
        completed: false,
        tasklistId: 'tl-1',
      })
      const toggled = { ...task, completed: true }
      vi.mocked(apiFetchTasks).mockResolvedValue([task])
      vi.mocked(apiUpdateTask).mockResolvedValue(toggled)

      const store = useTasksStore()
      await store.fetchTasks('tl-1')
      await store.toggleTask('t1')

      expect(apiUpdateTask).toHaveBeenCalledWith('t1', {
        title: 'T',
        description: 'Desc',
        completed: true,
        tasklistId: 'tl-1',
      })
      expect(store.items[0]?.completed).toBe(true)
    })

    it('throws if task not found', async () => {
      const store = useTasksStore()
      await expect(store.toggleTask('nonexistent')).rejects.toThrow('Task não encontrada.')
    })

    it('sets error on API failure', async () => {
      const task = makeTask({ id: 't1' })
      vi.mocked(apiFetchTasks).mockResolvedValue([task])
      vi.mocked(apiUpdateTask).mockRejectedValue(new Error('Network'))

      const store = useTasksStore()
      await store.fetchTasks('tl-1')
      const result = await store.toggleTask('t1')

      expect(result).toBeNull()
      expect(store.error).toBe('Erro ao atualizar task.')
    })
  })

  describe('removeTask', () => {
    it('removes task from items on success', async () => {
      const task = makeTask({ id: 't1' })
      vi.mocked(apiFetchTasks).mockResolvedValue([task])
      vi.mocked(apiDeleteTask).mockResolvedValue(undefined)

      const store = useTasksStore()
      await store.fetchTasks('tl-1')
      const success = await store.removeTask('t1')

      expect(success).toBe(true)
      expect(store.items).toHaveLength(0)
      expect(apiDeleteTask).toHaveBeenCalledWith('t1')
    })

    it('sets error on API failure', async () => {
      vi.mocked(apiDeleteTask).mockRejectedValue(new Error('Network'))

      const store = useTasksStore()
      const success = await store.removeTask('t1')

      expect(success).toBe(false)
      expect(store.error).toBe('Erro ao excluir task.')
    })
  })

  describe('removeTasksByTasklist', () => {
    it('removes all tasks for the given tasklist without calling API', () => {
      const store = useTasksStore()
      store.items = [
        makeTask({ id: 't1', tasklistId: 'tl-1' }),
        makeTask({ id: 't2', tasklistId: 'tl-2' }),
        makeTask({ id: 't3', tasklistId: 'tl-1' }),
      ]

      store.removeTasksByTasklist('tl-1')

      expect(store.items).toHaveLength(1)
      expect(store.items[0]?.id).toBe('t2')
      expect(apiDeleteTask).not.toHaveBeenCalled()
    })
  })
})
