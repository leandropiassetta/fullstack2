export interface TasklistItem {
  id: string
  name: string
}

export interface TasklistsSession {
  items: TasklistItem[]
  activeTasklistId: string | null
  isLoading: boolean
  error: string | null
}
