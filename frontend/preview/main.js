import { createApp } from 'vue'
import App from './App.vue'
import router from './router.js'
import './preview.scss'

createApp(App).use(router).mount('#app')
