import { createRouter, createWebHistory } from 'vue-router'
import { currentUser, restoreSession } from '../stores/auth'
const routes = [
  {
    path: '/',
    redirect: '/flow',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/flow',
    name: 'flow',
    component: () => import('../views/FlowView.vue'),
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { superAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  const authenticated = await restoreSession()
  if (to.meta.public) {
    if (to.name === 'login' && authenticated) return '/flow'
    return true
  }
  if (authenticated) {
    if (to.meta.superAdmin && !currentUser.value?.canManageSystem) return '/flow'
    return true
  }
  return { name: 'login', query: { redirect: to.fullPath } }
})

export default router
