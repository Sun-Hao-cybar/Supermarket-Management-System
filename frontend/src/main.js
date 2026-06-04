import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import * as Icons from '@element-plus/icons-vue'
import router from './router'

const app = createApp(App)
for (const [k, c] of Object.entries(Icons)) app.component(k, c)
app.use(ElementPlus)
app.use(router)
app.mount('#app')