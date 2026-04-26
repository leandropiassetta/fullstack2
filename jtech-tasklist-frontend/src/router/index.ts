import { createRouter, createWebHistory } from 'vue-router'
import type { Pinia } from 'pinia'

import AppLayout from '@/layouts/AppLayout.vue'
import PublicLayout from '@/layouts/PublicLayout.vue'
import { appPinia } from '@/plugins/pinia'
import { registerAuthGuards } from '@/router/guards'

const routes = [
  {
    path: '/',
    redirect: '/app',
  },
  {
    path: '/login',
    component: PublicLayout,
    meta: {
      guestOnly: true,
    },
    children: [
      {
        path: '',
        name: 'login',
        component: () => import('@/modules/auth/views/LoginView.vue'),
      },
    ],
  },
  {
    path: '/app',
    component: AppLayout,
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        redirect: { name: 'app-tasklists' },
      },
      {
        path: 'tasklists',
        name: 'app-tasklists',
        component: () => import('@/modules/tasklists/views/TasklistsHomeView.vue'),
      },
      {
        path: 'tasklists/:tasklistSlug',
        name: 'app-tasklist-details',
        component: () => import('@/modules/tasklists/views/TasklistsHomeView.vue'),
      },
    ],
  },
]

export function createAppRouter(pinia: Pinia = appPinia) {
  const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
  })

  registerAuthGuards(router, pinia)

  return router
}

const router = createAppRouter()

export default router
