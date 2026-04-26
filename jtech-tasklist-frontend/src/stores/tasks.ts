import { defineStore } from 'pinia'

import {
  apiFetchTasks,
  apiCreateTask,
  apiUpdateTask,
  apiDeleteTask,
} from '@/api/tasks'
import type { TaskItem, TasksSession } from '@/types/task'

const TASK_DESCRIPTION_LIMIT = 500

function sanitizeTaskTitle(title: string) {
  return title.trim()
}

function sanitizeTaskDescription(description: string | null | undefined) {
  const sanitizedDescription = description?.trim()
  return sanitizedDescription ? sanitizedDescription : null
}

function createEmptyState(): TasksSession {
  return {
    items: [],
    isLoading: false,
    error: null,
  }
}

export const useTasksStore = defineStore('tasks', {
  state: (): TasksSession => createEmptyState(),
  getters: {
    tasksByTasklist: (state) => (tasklistId: string) =>
      state.items.filter((task) => task.tasklistId === tasklistId),
    hasTasksForTasklist: (state) => (tasklistId: string) =>
      state.items.some((task) => task.tasklistId === tasklistId),
  },
  actions: {
    async fetchTasks(tasklistId: string) {
      this.isLoading = true
      this.error = null
      try {
        const tasks = await apiFetchTasks(tasklistId)
        this.items = this.items.filter((task) => task.tasklistId !== tasklistId)
        this.items.push(...tasks)
      } catch {
        this.error = 'Não foi possível carregar as tasks.'
      } finally {
        this.isLoading = false
      }
    },
    async createTask(task: {
      title: string
      description?: string | null
      tasklistId: string
    }): Promise<TaskItem | null> {
      const sanitizedTitle = sanitizeTaskTitle(task.title)
      const sanitizedDescription = sanitizeTaskDescription(task.description)

      if (!task.tasklistId) {
        throw new Error('Selecione uma lista válida para a task.')
      }

      if (!sanitizedTitle) {
        throw new Error('Informe um título para a task.')
      }

      if (sanitizedDescription && sanitizedDescription.length > TASK_DESCRIPTION_LIMIT) {
        throw new Error(`A descrição pode ter no máximo ${TASK_DESCRIPTION_LIMIT} caracteres.`)
      }

      this.isLoading = true
      this.error = null
      try {
        const created = await apiCreateTask({
          title: sanitizedTitle,
          description: sanitizedDescription,
          tasklistId: task.tasklistId,
        })
        this.items.unshift(created)
        return created
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status
        this.error = status === 409 ? 'Já existe uma task com esse título nesta lista.' : 'Erro ao criar task.'
        return null
      } finally {
        this.isLoading = false
      }
    },
    async updateTask(
      taskId: string,
      updates: { title: string; description?: string | null; completed: boolean; tasklistId: string },
    ): Promise<TaskItem | null> {
      const sanitizedTitle = sanitizeTaskTitle(updates.title)
      const sanitizedDescription = sanitizeTaskDescription(updates.description)

      if (!sanitizedTitle) {
        throw new Error('Informe um título para a task.')
      }

      if (sanitizedDescription && sanitizedDescription.length > TASK_DESCRIPTION_LIMIT) {
        throw new Error(`A descrição pode ter no máximo ${TASK_DESCRIPTION_LIMIT} caracteres.`)
      }

      this.isLoading = true
      this.error = null
      try {
        const updated = await apiUpdateTask(taskId, {
          title: sanitizedTitle,
          description: sanitizedDescription,
          completed: updates.completed,
          tasklistId: updates.tasklistId,
        })
        const index = this.items.findIndex((item) => item.id === taskId)
        if (index !== -1) {
          this.items[index] = updated
        }
        return updated
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } })?.response?.status
        this.error = status === 409 ? 'Já existe uma task com esse título nesta lista.' : 'Erro ao atualizar task.'
        return null
      } finally {
        this.isLoading = false
      }
    },
    async toggleTask(taskId: string): Promise<TaskItem | null> {
      const task = this.items.find((item) => item.id === taskId)

      if (!task) {
        throw new Error('Task não encontrada.')
      }

      this.isLoading = true
      this.error = null
      try {
        const updated = await apiUpdateTask(taskId, {
          title: task.title,
          description: task.description,
          completed: !task.completed,
          tasklistId: task.tasklistId,
        })
        const index = this.items.findIndex((item) => item.id === taskId)
        if (index !== -1) {
          this.items[index] = updated
        }
        return updated
      } catch {
        this.error = 'Erro ao atualizar task.'
        return null
      } finally {
        this.isLoading = false
      }
    },
    async removeTask(taskId: string): Promise<boolean> {
      this.isLoading = true
      this.error = null
      try {
        await apiDeleteTask(taskId)
        this.items = this.items.filter((item) => item.id !== taskId)
        return true
      } catch {
        this.error = 'Erro ao excluir task.'
        return false
      } finally {
        this.isLoading = false
      }
    },
    removeTasksByTasklist(tasklistId: string) {
      this.items = this.items.filter((task) => task.tasklistId !== tasklistId)
    },
  },
})
