import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/mod/',
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/mod': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8765',
        changeOrigin: true,
      },
    },
  },
})
