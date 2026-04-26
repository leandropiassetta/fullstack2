export interface TaskItem {
  id: string
  title: string
  description: string | null
  completed: boolean
  tasklistId: string
  createdAt: string
}

export interface TasksSession {
  items: TaskItem[]
  isLoading: boolean
  error: string | null
}
