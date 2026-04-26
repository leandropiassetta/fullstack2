import { defineStore } from 'pinia'

import {
  apiFetchTasklists,
  apiCreateTasklist,
  apiUpdateTasklist,
  apiDeleteTasklist,
} from '@/api/tasklists'
import type { TasklistItem, TasklistsSession } from '@/types/tasklist'
import { useTasksStore } from '@/stores/tasks'

function sanitizeTasklistName(name: string) {
  return name.trim()
}

function extractApiErrorMessage(err: unknown) {
  const response = (err as { response?: { status?: number; data?: { message?: string } } })?.response
  return {
    status: response?.status,
    message: response?.data?.message?.trim() || null,
  }
}

export function toTasklistSlug(name: string) {
  return sanitizeTasklistName(name)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

function sortTasklistsByName(items: TasklistItem[]) {
  return [...items].sort((left, right) => left.name.localeCompare(right.name, 'pt-BR'))
}

function createEmptyState(): TasklistsSession {
  return {
    items: [],
    activeTasklistId: null,
    isLoading: false,
    error: null,
  }
}

export const useTasklistsStore = defineStore('tasklists', {
  state: (): TasklistsSession => createEmptyState(),
  getters: {
    activeTasklist: (state) =>
      state.items.find((tasklist) => tasklist.id === state.activeTasklistId) ?? null,
    getListBySlug: (state) => (slug: string) =>
      state.items.find((tasklist) => toTasklistSlug(tasklist.name) === slug) ?? null,
    hasItems: (state) => state.items.length > 0,
  },
  actions: {
    async fetchTasklists() {
      this.isLoading = true
      this.error = null
      try {
        this.items = sortTasklistsByName(await apiFetchTasklists())
      } catch {
        this.error = 'Não foi possível carregar as listas.'
      } finally {
        this.isLoading = false
      }
    },
    async createTasklist(name: string): Promise<TasklistItem | null> {
      const sanitizedName = sanitizeTasklistName(name)
      if (!sanitizedName) {
        throw new Error('Informe um nome para a lista.')
      }

      this.isLoading = true
      this.error = null
      try {
        const tasklist = await apiCreateTasklist(sanitizedName)
        this.items = sortTasklistsByName([...this.items, tasklist])
        this.activeTasklistId = tasklist.id
        return tasklist
      } catch (err: unknown) {
        const { status } = extractApiErrorMessage(err)
        this.error = status === 409 ? 'Já existe uma lista com esse nome.' : 'Erro ao criar lista.'
        return null
      } finally {
        this.isLoading = false
      }
    },
    async renameTasklist(tasklistId: string, nextName: string): Promise<TasklistItem | null> {
      const sanitizedName = sanitizeTasklistName(nextName)
      if (!sanitizedName) {
        throw new Error('Informe um nome para a lista.')
      }

      this.isLoading = true
      this.error = null
      try {
        const updated = await apiUpdateTasklist(tasklistId, sanitizedName)
        const index = this.items.findIndex((item) => item.id === tasklistId)
        if (index !== -1) {
          const nextItems = [...this.items]
          nextItems[index] = updated
          this.items = sortTasklistsByName(nextItems)
        }
        return updated
      } catch (err: unknown) {
        const { status } = extractApiErrorMessage(err)
        this.error = status === 409 ? 'Já existe uma lista com esse nome.' : 'Erro ao renomear lista.'
        return null
      } finally {
        this.isLoading = false
      }
    },
    async deleteTasklist(tasklistId: string): Promise<boolean> {
      this.isLoading = true
      this.error = null
      try {
        await apiDeleteTasklist(tasklistId)
        this.items = this.items.filter((item) => item.id !== tasklistId)
        if (this.activeTasklistId === tasklistId) {
          this.activeTasklistId = this.items[0]?.id ?? null
        }
        useTasksStore().removeTasksByTasklist(tasklistId)
        return true
      } catch (err: unknown) {
        const { status, message } = extractApiErrorMessage(err)
        this.error =
          status === 404 ? 'Lista não encontrada.' : message || 'Erro ao excluir lista.'
        return false
      } finally {
        this.isLoading = false
      }
    },
    setActiveTasklist(tasklistId: string | null) {
      if (tasklistId === null) {
        this.activeTasklistId = null
        return
      }

      if (tasklistId && this.items.some((item) => item.id === tasklistId)) {
        this.activeTasklistId = tasklistId
      } else {
        this.activeTasklistId = this.items[0]?.id ?? null
      }
    },
  },
})
