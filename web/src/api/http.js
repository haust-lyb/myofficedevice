import axios from 'axios'

export const TOKEN_KEY = 'netdesk_access_token'

const http = axios.create({
  baseURL: '/mod/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem('netdesk_user')
      const loginPath = `${import.meta.env.BASE_URL}login`.replace(/\/+/g, '/')
      if (window.location.pathname !== loginPath) window.location.assign(loginPath)
    }
    return Promise.reject(error)
  },
)

export default http
