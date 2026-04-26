import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

export function registerAuthGuards(router: Router, pinia: Pinia) {
  router.beforeEach((to) => {
    const authStore = useAuthStore(pinia)

    if (to.matched.some((record) => record.meta.requiresAuth) && !authStore.isAuthenticated) {
      return {
        name: 'login',
        query: {
          redirectTo: to.fullPath,
        },
      }
    }

    if (to.matched.some((record) => record.meta.guestOnly) && authStore.isAuthenticated) {
      return { name: 'app-tasklists' }
    }

    return true
  })
}
