import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  //代理配置
  server: {
  proxy: {
    '/urbanova': {  // 代理所有以 /urbanova 开头的请求
      //target: 'http://47.109.73.119:8080',
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/urbanova/, '')  // ⭐ 关键：去掉 /urbanova 前缀
    }
  }
}
})