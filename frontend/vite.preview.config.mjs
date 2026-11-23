import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

const frontendDir = path.dirname(fileURLToPath(import.meta.url))

// Isolated design-preview entry. It never loads the official application
// router, stores, API clients or Element Plus theme.
export default defineConfig({
  root: path.resolve(frontendDir, 'preview'),
  base: './',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(frontendDir, 'src')
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5199,
    strictPort: true,
    open: false
  },
  preview: {
    host: '127.0.0.1',
    port: 5199,
    strictPort: true
  },
  build: {
    outDir: path.resolve(frontendDir, 'dist-design'),
    emptyOutDir: true
  }
})
