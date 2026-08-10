import { computed, reactive } from 'vue'
import http, { TOKEN_KEY } from '../api/http'

const savedUser = sessionStorage.getItem('netdesk_user')
const state = reactive({
  user: savedUser ? JSON.parse(savedUser) : null,
  checking: false,
})

export const currentUser = computed(() => state.user)
export const isAuthenticated = computed(() => Boolean(sessionStorage.getItem(TOKEN_KEY) && state.user))

export async function login(username, password) {
  const result = await http.post('/auth/login', { username, password })
  sessionStorage.setItem(TOKEN_KEY, result.data.token)
  setUser(result.data.user)
  return result.data.user
}

export async function restoreSession() {
  if (!sessionStorage.getItem(TOKEN_KEY)) return false
  if (state.checking) return Boolean(state.user)
  state.checking = true
  try {
    const result = await http.get('/auth/me')
    setUser(result.data)
    return true
  } catch {
    clearSession()
    return false
  } finally {
    state.checking = false
  }
}

export async function logout() {
  try { await http.post('/auth/logout') } catch { /* 本地会话仍需清理 */ }
  clearSession()
}

function setUser(user) {
  state.user = user
  sessionStorage.setItem('netdesk_user', JSON.stringify(user))
}

function clearSession() {
  state.user = null
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem('netdesk_user')
}
