import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import ChatView from './views/ChatView.vue'

// Preview-only hash router. The official application router and routes are
// untouched.
export default createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/home', name: 'preview-home', component: HomeView },
    { path: '/chat', name: 'preview-chat', component: ChatView },
    { path: '/:pathMatch(.*)*', redirect: '/home' }
  ]
})
