import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    //vueDevTools()
  ],
  define: {
    // vue-i18n esm-bundler 特性开关（仅 Composition 模式），消除开发环境 console 警告
    __VUE_I18N_FULL_INSTALL__: true,
    __INTLIFY_PROD_DEVTOOLS__: false,
    __VUE_I18N_LEGACY_API__: false,
    __INTLIFY_JIT_COMPILATION__: true,
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
