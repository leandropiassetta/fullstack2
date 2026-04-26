import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useTasklistsStore } from '@/stores/tasklists'

const { mockRemoveTasksByTasklist } = vi.hoisted(() => ({
  mockRemoveTasksByTasklist: vi.fn(),
}))

vi.mock('@/api/tasklists')
vi.mock('@/stores/tasks', () => ({
  useTasksStore: () => ({ removeTasksByTasklist: mockRemoveTasksByTasklist }),
}))

import {
  apiFetchTasklists,
  apiCreateTasklist,
  apiUpdateTasklist,
  apiDeleteTasklist,
} from '@/api/tasklists'

const tl = (id: string, name: string) => ({ id, name })

describe('tasklists store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  describe('fetchTasklists', () => {
    it('populates items on success', async () => {
      const items = [tl('tl-1', 'Casa'), tl('tl-2', 'Trabalho')]
      vi.mocked(apiFetchTasklists).mockResolvedValue(items)

      const store = useTasklistsStore()
      await store.fetchTasklists()

      expect(store.items).toEqual(items)
      expect(store.isLoading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('sets error on failure', async () => {
      vi.mocked(apiFetchTasklists).mockRejectedValue(new Error('Network'))

      const store = useTasklistsStore()
      await store.fetchTasklists()

      expect(store.items).toEqual([])
      expect(store.error).toBe('Não foi possível carregar as listas.')
    })
  })

  describe('createTasklist', () => {
    it('creates tasklist, pushes to items, and sets active', async () => {
      const created = tl('new-id', 'Pessoal')
      vi.mocked(apiCreateTasklist).mockResolvedValue(created)

      const store = useTasklistsStore()
      const result = await store.createTasklist('  Pessoal  ')

      expect(apiCreateTasklist).toHaveBeenCalledWith('Pessoal')
      expect(result).toEqual(created)
      expect(store.items).toContainEqual(created)
      expect(store.activeTasklistId).toBe('new-id')
    })

    it('throws if name is empty', async () => {
      const store = useTasklistsStore()
      await expect(store.createTasklist('   ')).rejects.toThrow('Informe um nome para a lista.')
    })

    it('sets 409 as duplicate name message', async () => {
      const err = { response: { status: 409 } }
      vi.mocked(apiCreateTasklist).mockRejectedValue(err)

      const store = useTasklistsStore()
      const result = await store.createTasklist('Casa')

      expect(result).toBeNull()
      expect(store.error).toBe('Já existe uma lista com esse nome.')
    })

    it('sets generic error on non-409 failure', async () => {
      vi.mocked(apiCreateTasklist).mockRejectedValue(new Error('Network'))

      const store = useTasklistsStore()
      const result = await store.createTasklist('Casa')

      expect(result).toBeNull()
      expect(store.error).toBe('Erro ao criar lista.')
    })
  })

  describe('renameTasklist', () => {
    it('renames tasklist and updates items', async () => {
      const original = tl('tl-1', 'Casa')
      const updated = tl('tl-1', 'Casa Nova')
      vi.mocked(apiFetchTasklists).mockResolvedValue([original])
      vi.mocked(apiUpdateTasklist).mockResolvedValue(updated)

      const store = useTasklistsStore()
      await store.fetchTasklists()
      const result = await store.renameTasklist('tl-1', '  Casa Nova  ')

      expect(apiUpdateTasklist).toHaveBeenCalledWith('tl-1', 'Casa Nova')
      expect(result).toEqual(updated)
      expect(store.items[0]?.name).toBe('Casa Nova')
    })

    it('throws if name is empty', async () => {
      const store = useTasklistsStore()
      await expect(store.renameTasklist('tl-1', '')).rejects.toThrow(
        'Informe um nome para a lista.',
      )
    })

    it('sets 409 as duplicate name message', async () => {
      const err = { response: { status: 409 } }
      vi.mocked(apiUpdateTasklist).mockRejectedValue(err)

      const store = useTasklistsStore()
      const result = await store.renameTasklist('tl-1', 'Nova')

      expect(result).toBeNull()
      expect(store.error).toBe('Já existe uma lista com esse nome.')
    })
  })

  describe('deleteTasklist', () => {
    it('removes tasklist and updates activeTasklistId to next item', async () => {
      const items = [tl('tl-1', 'Casa'), tl('tl-2', 'Trabalho')]
      vi.mocked(apiFetchTasklists).mockResolvedValue(items)
      vi.mocked(apiDeleteTasklist).mockResolvedValue(undefined)

      const store = useTasklistsStore()
      await store.fetchTasklists()
      store.setActiveTasklist('tl-1')

      const success = await store.deleteTasklist('tl-1')

      expect(success).toBe(true)
      expect(store.items).toHaveLength(1)
      expect(store.items[0]?.id).toBe('tl-2')
      expect(store.activeTasklistId).toBe('tl-2')
    })

    it('calls removeTasksByTasklist on tasks store after delete', async () => {
      vi.mocked(apiFetchTasklists).mockResolvedValue([tl('tl-1', 'Casa')])
      vi.mocked(apiDeleteTasklist).mockResolvedValue(undefined)

      const store = useTasklistsStore()
      await store.fetchTasklists()
      await store.deleteTasklist('tl-1')

      expect(mockRemoveTasksByTasklist).toHaveBeenCalledWith('tl-1')
    })

    it('sets 404 as "Lista não encontrada" message', async () => {
      const err = { response: { status: 404 } }
      vi.mocked(apiDeleteTasklist).mockRejectedValue(err)

      const store = useTasklistsStore()
      const success = await store.deleteTasklist('tl-1')

      expect(success).toBe(false)
      expect(store.error).toBe('Lista não encontrada.')
    })

    it('sets generic error on non-409 failure', async () => {
      vi.mocked(apiDeleteTasklist).mockRejectedValue(new Error('Network'))

      const store = useTasklistsStore()
      const success = await store.deleteTasklist('tl-1')

      expect(success).toBe(false)
      expect(store.error).toBe('Erro ao excluir lista.')
    })

    it('prefers backend message when delete fails with API payload', async () => {
      vi.mocked(apiDeleteTasklist).mockRejectedValue({
        response: { status: 500, data: { message: 'violates foreign key constraint' } },
      })

      const store = useTasklistsStore()
      const success = await store.deleteTasklist('tl-1')

      expect(success).toBe(false)
      expect(store.error).toBe('violates foreign key constraint')
    })
  })

  describe('setActiveTasklist', () => {
    it('sets active to given id if it exists in items', async () => {
      vi.mocked(apiFetchTasklists).mockResolvedValue([tl('tl-1', 'Casa')])

      const store = useTasklistsStore()
      await store.fetchTasklists()
      store.setActiveTasklist('tl-1')

      expect(store.activeTasklistId).toBe('tl-1')
    })

    it('falls back to first item if id not found', async () => {
      vi.mocked(apiFetchTasklists).mockResolvedValue([tl('tl-1', 'Casa')])

      const store = useTasklistsStore()
      await store.fetchTasklists()
      store.setActiveTasklist('nonexistent')

      expect(store.activeTasklistId).toBe('tl-1')
    })

    it('sets null if items is empty', () => {
      const store = useTasklistsStore()
      store.setActiveTasklist('any')

      expect(store.activeTasklistId).toBeNull()
    })
  })

  describe('getListBySlug', () => {
    it('finds a tasklist by normalized route slug', async () => {
      vi.mocked(apiFetchTasklists).mockResolvedValue([
        tl('tl-1', 'Casa'),
        tl('tl-2', 'Área Pessoal'),
      ])

      const store = useTasklistsStore()
      await store.fetchTasklists()

      expect(store.getListBySlug('area-pessoal')).toEqual(tl('tl-2', 'Área Pessoal'))
    })
  })
})
