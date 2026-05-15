import { createApp } from 'vue'
import './style.css'
import './styles/common.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { checkLoginStatus } from './stores/auth'
import './stores/websocket'

const app = createApp(App).use(router).use(ElementPlus)

checkLoginStatus().then(() => {
  app.mount('#app')
})
