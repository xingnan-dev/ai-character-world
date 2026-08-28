<template>
  <div class="home-container">
    <LoadingSkeleton :loading="isLoading" />
    
    <SpaceBackground />

    <transition name="fade" appear>
      <SideMenu
        v-show="!isLoading"
        :items="menuItems"
        :class="{ 'mobile-open': menuOpen }"
        @navigate="handleNavigate"
      />
    </transition>

    <div 
      v-if="menuOpen" 
      class="mobile-overlay" 
      @click="menuOpen = false"
    ></div>

    <button 
      v-if="!isLoading" 
      class="menu-toggle"
      @click="menuOpen = !menuOpen"
      aria-label="切换菜单"
    >
      <span class="toggle-icon">{{ menuOpen ? '✕' : '☰' }}</span>
    </button>

    <main class="main-layout" :class="{ 'sidebar-open': menuOpen }">
      <div class="center-panel">
        <transition name="fade-slide" appear mode="out-in">
          <AIAvatarShow
            v-if="!isLoading"
            :key="currentAvatarIndex"
            :userName="userStore.nickname || '创造者'"
            :slogan="currentSlogan"
            :avatar="currentAvatar"
            :avatars="avatarList"
            :currentAvatarIndex="currentAvatarIndex"
            @create="handleCreate"
            @chat="handleChat"
            @switch="handleSwitchAvatar"
          />
        </transition>
      </div>

      <aside class="right-panel">
        <transition name="fade-slide" appear delay="100" mode="out-in">
          <AIStatusCard v-if="!isLoading" :data="currentAvatar" @edit="handleEditPersonality" />
        </transition>
        <transition name="fade-slide" appear delay="200" mode="out-in">
          <MemoryCard
            v-if="!isLoading"
            :memories="memories"
            @viewAll="handleViewMemories"
          />
        </transition>
        <transition name="fade-slide" appear delay="300" mode="out-in">
          <QuickAction
            v-if="!isLoading"
            @create="handleCreate"
            @chat="handleChat"
            @memory="handleViewMemories"
            @warehouse="handleWarehouse"
          />
        </transition>
      </aside>
    </main>

    <Transition name="toast-fade" appear>
      <div v-if="toast.show" class="toast-notification" :class="toast.type">
        <span class="toast-icon">{{ toast.icon }}</span>
        <span class="toast-message">{{ toast.message }}</span>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useAvatarStore } from '../stores/avatar'
import { useMemoryStore } from '../stores/memory'
import SpaceBackground from '../components/home/SpaceBackground.vue'
import SideMenu from '../components/home/SideMenu.vue'
import AIAvatarShow from '../components/home/AIAvatarShow.vue'
import AIStatusCard from '../components/home/AIStatusCard.vue'
import MemoryCard from '../components/home/MemoryCard.vue'
import QuickAction from '../components/home/QuickAction.vue'
import LoadingSkeleton from '../components/home/LoadingSkeleton.vue'
import {
  personalityIdentity,
  personalityInterests,
  personalityTags,
  personalityText
} from '../utils/avatarProfile'

const router = useRouter()
const userStore = useUserStore()
const avatarStore = useAvatarStore()
const memoryStore = useMemoryStore()

const isLoading = ref(true)
const menuOpen = ref(false)

const toast = ref({
  show: false,
  message: '',
  icon: '✨',
  type: 'success'
})

const showToast = (message, icon = '✨', type = 'success') => {
  toast.value = { show: true, message, icon, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

const menuItems = [
  { key: 'space', label: 'AI空间', icon: '🌌', path: '/home' },
  { key: 'create', label: '创造生命', icon: '✨', path: '/avatar/create' },
  { key: 'characters', label: '角色空间', icon: '🎭', path: '/characters' },
  { key: 'worlds', label: '我的世界', icon: '🌍', path: '/worlds' },
  { key: 'chat', label: '与TA聊天', icon: '💬', path: '/chat', badge: 'New' },
  { key: 'memory', label: '记忆空间', icon: '🧠', path: '/memory' },
  { key: 'warehouse', label: '形象仓库', icon: '🎨', path: '/avatars' },
  { key: 'settings', label: '设置', icon: '⚙', path: '/settings' }
]

const avatarList = computed(() => {
  return avatarStore.avatarList.map(avatar => ({
    id: avatar.id,
    name: avatar.name,
    modelUrl: avatar.modelUrl,
    baseModel: avatar.baseModel,
    identity: personalityIdentity(avatar.personality),
    personality: personalityText(avatar.personality),
    interests: personalityInterests(avatar.personality),
    status: 'online',
    emoji: getAvatarEmoji(avatar),
    tags: personalityTags(avatar.personality),
    growth: 50 + (avatar.id * 15),
    level: avatar.personalityId || 1,
    slogan: avatar.slogan || '所有没有你的日子，都存在缺陷'
  }))
})

const currentAvatarIndex = computed(() => avatarStore.currentAvatarIndex)

const currentAvatar = computed(() => avatarList.value[currentAvatarIndex.value])
const currentSlogan = computed(() => currentAvatar.value?.slogan || '')

const memories = computed(() => {
  return memoryStore.memories.map(memory => ({
    id: memory.id,
    type: getMemoryType(memory.category),
    content: memory.value,
    time: formatTime(memory.createTime)
  }))
})

function getIdentityText(avatar) {
  const types = { 1: '虚拟人', 2: '动物伙伴', 3: '创意物品' }
  return types[avatar.type] || 'AI伙伴'
}

function getAvatarEmoji(avatar) {
  const emojis = { 1: '🤖', 2: '🐱', 3: '✨' }
  return emojis[avatar.type] || '🤖'
}

function getMemoryType(category) {
  const types = { 1: 'learning', 2: 'preference', 3: 'achievement', 4: 'event' }
  return types[category] || 'event'
}

function formatTime(dateStr) {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const handleNavigate = (item) => {
  menuOpen.value = false
  if (item.path && item.path !== '/home') {
    router.push(item.path)
  }
}

const handleCreate = () => {
  showToast('正在打开创造空间...', '✨')
  setTimeout(() => router.push('/avatar/create'), 300)
}

const handleChat = () => {
  showToast('正在连接AI...', '💬')
  setTimeout(() => router.push('/chat'), 300)
}

const handleEditPersonality = () => {
  if (!currentAvatar.value?.id) {
    showToast('请先选择一个形象', '⚠', 'warning')
    return
  }
  router.push(`/personality/edit/${currentAvatar.value.id}`)
}

const handleSwitchAvatar = (avatar) => {
  const success = avatarStore.switchAvatar(avatar)
  if (success) {
    showToast(`已切换到 ${avatar.name}`, avatar.emoji || '🤖', 'info')
  }
}

const handleViewMemories = () => {
  showToast('正在打开记忆空间...', '🧠')
  setTimeout(() => router.push('/memory'), 300)
}

const handleWarehouse = () => {
  showToast('正在打开形象仓库...', '🎨')
  setTimeout(() => router.push('/avatars'), 300)
}

const handleKeydown = (e) => {
  if (e.key === 'Escape') {
    menuOpen.value = false
  }
}

let loadingTimer = null

onMounted(async () => {
  try {
    await Promise.all([
      avatarStore.fetchAvatarList(),
      memoryStore.fetchMemories()
    ])
  } catch (err) {
    console.warn('数据加载失败，使用默认数据:', err.message)
  }
  
  loadingTimer = setTimeout(() => {
    isLoading.value = false
  }, 800)
  
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (loadingTimer) {
    clearTimeout(loadingTimer)
  }
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  background: #060816;
  position: relative;
  overflow-x: hidden;
  animation: pageFadeIn 0.8s ease-out;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

@keyframes pageFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.main-layout {
  margin-left: 220px;
  display: flex;
  gap: 24px;
  padding: 32px;
  position: relative;
  z-index: 1;
  transition: margin-left 0.3s ease;
}

.center-panel {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: calc(100vh - 64px);
  animation: panelSlideIn 0.8s ease-out 0.1s both;
}

@keyframes panelSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.right-panel {
  width: 340px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  flex-shrink: 0;
  animation: panelSlideIn 0.8s ease-out 0.2s both;
}

.mobile-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(6, 8, 22, 0.7);
  backdrop-filter: blur(4px);
  z-index: 9;
  animation: overlayFadeIn 0.3s ease-out;
}

@keyframes overlayFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.menu-toggle {
  display: none;
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 20;
  width: 44px;
  height: 44px;
  background: rgba(124, 92, 255, 0.8);
  border: 1px solid rgba(124, 92, 255, 0.5);
  border-radius: 12px;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  backdrop-filter: blur(10px);
  transition: all 0.3s;

  &:hover {
    background: rgba(124, 92, 255, 1);
    box-shadow: 0 4px 20px rgba(124, 92, 255, 0.4);
  }
}

.toast-notification {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(10, 14, 39, 0.9);
  border: 1px solid rgba(124, 92, 255, 0.3);
  border-radius: 14px;
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  color: white;
  font-size: 14px;

  &.success {
    border-color: rgba(0, 229, 192, 0.4);
    .toast-icon {
      color: #00E5C0;
    }
  }

  &.info {
    border-color: rgba(59, 130, 246, 0.4);
    .toast-icon {
      color: #3B82F6;
    }
  }

  &.warning {
    border-color: rgba(255, 193, 7, 0.4);
    .toast-icon {
      color: #ffc107;
    }
  }
}

.toast-icon {
  font-size: 20px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

@media (max-width: 1200px) {
  .right-panel {
    width: 300px;
  }
}

@media (max-width: 1024px) {
  .main-layout {
    flex-direction: column;
  }

  .center-panel {
    width: 100%;
  }

  .right-panel {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .main-layout {
    margin-left: 0;
    padding: 16px;
    gap: 16px;
    padding-top: 72px;
  }

  .menu-toggle {
    display: flex;
  }

  .mobile-overlay {
    display: block;
  }

  .toast-notification {
    top: 16px;
    right: 16px;
    left: 16px;
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
