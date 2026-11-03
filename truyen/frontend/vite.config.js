import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Proxy /api về Spring Boot để cookie refresh token hoạt động mà không vướng CORS
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Backend chỉ cho phép trang ở cổng 5173: cổng đã bị dùng thì báo lỗi, không tự nhảy sang cổng khác
    strictPort: true,
    proxy: {
      '/api': process.env.API_TARGET ?? 'http://localhost:8080',
    },
  },
})
