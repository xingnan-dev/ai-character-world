import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig(({ mode }) => {
  const backend = mode === 'acceptance'
    ? (process.env.VITE_ACCEPTANCE_API_TARGET || 'http://127.0.0.1:18080')
    : 'http://localhost:8080'
  const imageBackend = mode === 'acceptance'
    ? (process.env.VITE_ACCEPTANCE_IMAGE_TARGET || backend)
    : backend
  return {
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: backend,
        changeOrigin: true
      },
      '/generated-images': {
        target: imageBackend,
        changeOrigin: true
      }
    }
  }
  }
})
