import client from '@/api/client'
import type { TasklistItem } from '@/types/tasklist'

export async function apiFetchTasklists(): Promise<TasklistItem[]> {
  const response = await client.get<TasklistItem[]>('/api/v1/tasklists')
  return response.data
}

export async function apiCreateTasklist(name: string): Promise<TasklistItem> {
  const response = await client.post<TasklistItem>('/api/v1/tasklists', { name })
  return response.data
}

export async function apiUpdateTasklist(id: string, name: string): Promise<TasklistItem> {
  const response = await client.put<TasklistItem>(`/api/v1/tasklists/${id}`, { name })
  return response.data
}

export async function apiDeleteTasklist(id: string): Promise<void> {
  await client.delete(`/api/v1/tasklists/${id}`)
}
