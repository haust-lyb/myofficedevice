import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, restoreSession } from '../stores/auth'
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
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    if (to.name === 'login' && (isAuthenticated.value || await restoreSession())) return '/flow'
    return true
  }
  if (isAuthenticated.value || await restoreSession()) return true
  return { name: 'login', query: { redirect: to.fullPath } }
})

export default router
