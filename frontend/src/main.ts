import { createApp } from 'vue'
import './style.css'
import './styles/common.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { checkLoginStatus } from './stores/auth'

// 先检查登录状态，再挂载应用
checkLoginStatus()

const app = createApp(App).use(router).use(ElementPlus)

app.mount('#app')
