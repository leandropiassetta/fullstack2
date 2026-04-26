import client from '@/api/client'
import type { TaskItem } from '@/types/task'

export async function apiFetchTasks(tasklistId: string): Promise<TaskItem[]> {
  const response = await client.get<TaskItem[]>('/api/v1/tasks', {
    params: { tasklistId },
  })
  return response.data
}

export async function apiCreateTask(task: {
  title: string
  description?: string | null
  tasklistId: string
}): Promise<TaskItem> {
  const response = await client.post<TaskItem>('/api/v1/tasks', task)
  return response.data
}

export async function apiUpdateTask(
  id: string,
  task: {
    title: string
    description: string | null
    completed: boolean
    tasklistId: string
  },
): Promise<TaskItem> {
  const response = await client.put<TaskItem>(`/api/v1/tasks/${id}`, task)
  return response.data
}

export async function apiDeleteTask(id: string): Promise<void> {
  await client.delete(`/api/v1/tasks/${id}`)
}
