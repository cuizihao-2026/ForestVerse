import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8020,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8010',
        changeOrigin: true,
        rewrite: (path) => path
      },
      '/ws': {
        target: 'ws://localhost:8010',
        ws: true
      },
      '/uploads': {
        target: 'http://localhost:8010',
        changeOrigin: true
      }
    }
  }
})
