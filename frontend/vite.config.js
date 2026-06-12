import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'

export default defineConfig({
  plugins: [vue()],
  resolve:{
    alias:{
      '@': path.resolve(__dirname,'./src')
    }
  },
  // 生产构建：输出到 Spring Boot 静态资源目录，由后端统一提供服务
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
    sourcemap: true,
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        // 合理的分包策略，减少请求数（Vite 8 Rolldown 需要函数形式）
        manualChunks(id) {
          if (id.includes('node_modules/element-plus')) return 'element-plus'
          if (id.includes('node_modules/@element-plus/icons-vue')) return 'element-icons'
        }
      }
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',  // 允许局域网内其他设备访问
    allowedHosts: true,  // 允许 Cpolar/ngrok 等隧道域名访问
    // https 交给 Cpolar 处理，本地用 HTTP 避免证书冲突
    // https: {
    //   key: fs.readFileSync(path.resolve(__dirname, 'certs/localhost-key.pem')),
    //   cert: fs.readFileSync(path.resolve(__dirname, 'certs/localhost.pem'))
    // },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 不再 rewrite：后端 ApiPrefixConfig 已为控制器统一添加 /api 前缀
      }
    }
  }
})