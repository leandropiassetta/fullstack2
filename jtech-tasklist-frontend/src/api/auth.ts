import client from '@/api/client'
import type { AuthUser } from '@/types/auth'

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: string
  name: string
  email: string
}

export function loginResponseToUser(response: LoginResponse): AuthUser {
  return {
    id: response.userId,
    name: response.name,
    email: response.email,
  }
}

export async function apiRegister(name: string, email: string, password: string): Promise<void> {
  await client.post('/auth/register', { name, email, password })
}

export async function apiLogin(email: string, password: string): Promise<LoginResponse> {
  const response = await client.post<LoginResponse>('/auth/login', { email, password })
  return response.data
}

export async function apiRefresh(
  refreshToken: string,
): Promise<{ accessToken: string; refreshToken: string }> {
  const response = await client.post<{ accessToken: string; refreshToken: string }>(
    '/auth/refresh',
    { refreshToken },
  )
  return response.data
}
