import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import '@/styles/tokens.css'
import '@/styles/admin.css'
import '@/styles/skins/minimal.css'
import '@/styles/skins/codenow.css'
import '@/styles/skins/cohere.css'
import '@/styles/skins/dell.css'

import i18n from '@/i18n'

import '@/access'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(i18n)

app.mount('#app')
