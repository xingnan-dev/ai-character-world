import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true, navSection: 'home' }
  },
  {
    path: '/avatar/create',
    name: 'AvatarCreate',
    component: () => import('../views/AvatarCreate.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/personality/edit/:avatarId',
    name: 'PersonalityEdit',
    component: () => import('../views/PersonalityEdit.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/character/create',
    name: 'CharacterCreate',
    component: () => import('../views/CharacterCreate.vue'),
    meta: { requiresAuth: true, navSection: 'characters' }
  },
  {
    path: '/characters',
    name: 'CharacterList',
    component: () => import('../views/CharacterList.vue'),
    meta: { requiresAuth: true, navSection: 'characters' }
  },
  {
    path: '/worlds',
    name: 'WorldList',
    component: () => import('../views/WorldList.vue'),
    meta: { requiresAuth: true, appShell: true, navSection: 'worlds' }
  },
  {
    path: '/worlds/create',
    name: 'WorldCreate',
    component: () => import('../views/WorldCreate.vue'),
    meta: { requiresAuth: true, appShell: true, navSection: 'worlds' }
  },
  {
    path: '/worlds/:worldId',
    name: 'WorldDetail',
    component: () => import('../views/WorldDetail.vue'),
    meta: { requiresAuth: true, appShell: true, navSection: 'worlds' }
  },
  {
    path: '/worlds/:worldId/interaction',
    name: 'WorldInteraction',
    component: () => import('../views/WorldInteraction.vue'),
    meta: { requiresAuth: true, appShell: true, navSection: 'worlds' }
  },
  {
    path: '/character/:id',
    name: 'CharacterDetail',
    component: () => import('../views/CharacterDetail.vue'),
    meta: { requiresAuth: true, navSection: 'characters' }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue'),
    meta: { requiresAuth: true, navSection: 'chat' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth

  if (requiresAuth !== false && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
