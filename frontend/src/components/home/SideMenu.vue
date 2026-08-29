<template>
  <nav class="side-menu">
    <div class="menu-header">
      <div class="logo">
        <span class="logo-icon">🌌</span>
        <span class="logo-text">AI Space</span>
        <span class="logo-glow"></span>
      </div>
    </div>

    <div class="menu-list">
      <div
        v-for="(item, index) in items"
        :key="item.key"
        class="menu-item"
        :class="{ active: currentKey === item.key }"
        :style="{ '--item-delay': `${index * 0.05}s` }"
        role="link"
        tabindex="0"
        @click="handleClick(item, $event)"
        @keydown.enter.prevent="handleClick(item)"
        @keydown.space.prevent="handleClick(item)"
      >
        <span class="item-glow"></span>
        <span class="item-icon">{{ item.icon }}</span>
        <span class="item-label">{{ item.label }}</span>
        <span v-if="item.badge" class="item-badge">
          <span class="badge-text">{{ item.badge }}</span>
        </span>
        <span class="item-arrow">→</span>
        <span class="ripple" v-for="ripple in ripples" :key="ripple.id" :style="ripple.style"></span>
      </div>
    </div>

    <div class="menu-footer">
      <div class="user-card">
        <div class="user-avatar">
          {{ userStore.nickname?.charAt(0)?.toUpperCase() || 'U' }}
          <span class="avatar-ring"></span>
          <span class="avatar-glow"></span>
        </div>
        <div class="user-detail">
          <span class="user-name">{{ userStore.nickname || '用户' }}</span>
          <span class="user-status">
            <span class="status-dot"></span>
            在线
          </span>
        </div>
      </div>
      <button class="logout-btn" @click="handleLogout" title="退出登录">
        <span class="logout-icon">⏻</span>
        <span class="logout-text">退出</span>
      </button>
    </div>

    <div class="menu-decoration">
      <div class="deco-circle circle-1"></div>
      <div class="deco-circle circle-2"></div>
    </div>
  </nav>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'

const props = defineProps({
  items: {
    type: Array,
    default: () => [
      { key: 'space', label: 'AI空间', icon: '🌌', path: '/home' },
      { key: 'create', label: '创造生命', icon: '✨', path: '/avatar/create' },
      { key: 'characters', label: '角色空间', icon: '🎭', path: '/characters' },
      { key: 'worlds', label: '我的世界', icon: '🌍', path: '/worlds' },
      { key: 'chat', label: '与TA聊天', icon: '💬', path: '/chat', badge: 'New' },
      { key: 'memory', label: '记忆空间', icon: '🧠', path: '/memory' },
      { key: 'warehouse', label: '形象仓库', icon: '🎨', path: '/avatars' },
      { key: 'settings', label: '设置', icon: '⚙', path: '/settings' }
    ]
  }
})

const emit = defineEmits(['navigate'])
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const ripples = ref([])
let rippleId = 0

const currentKey = computed(() => {
  const currentPath = route.path
  const item = props.items.find(i => currentPath.startsWith(i.path))
  return item?.key || 'space'
})

const handleClick = (item, e) => {
  const target = e?.currentTarget
  if (target && typeof target.getBoundingClientRect === 'function') {
    const rect = target.getBoundingClientRect()
    const x = Number.isFinite(e.clientX) ? e.clientX - rect.left : rect.width / 2
    const y = Number.isFinite(e.clientY) ? e.clientY - rect.top : rect.height / 2

    const newRipple = {
      id: rippleId++,
      style: {
        left: `${x}px`,
        top: `${y}px`
      }
    }
    ripples.value.push(newRipple)
    setTimeout(() => {
      ripples.value.shift()
    }, 600)
  }

  emit('navigate', item)
  if (item.path) {
    router.push(item.path)
  }
}

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.side-menu {
  width: 220px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  background: linear-gradient(180deg, rgba(10, 14, 39, 0.85) 0%, rgba(6, 8, 22, 0.9) 100%);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid rgba(124, 92, 255, 0.15);
  overflow: hidden;
}

.menu-header {
  margin-bottom: 32px;
  padding: 0 8px;
  animation: slideInDown 0.6s ease-out;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.logo-icon {
  font-size: 28px;
  filter: drop-shadow(0 0 10px rgba(124, 92, 255, 0.6));
  animation: pulse 3s ease-in-out infinite;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #7C5CFF, #00E5C0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.logo-glow {
  position: absolute;
  bottom: -4px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #7C5CFF, #00E5C0, transparent);
  opacity: 0.5;
}

.menu-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
  z-index: 1;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  color: rgba(255, 255, 255, 0.5);
  overflow: hidden;
  animation: fadeInLeft 0.5s ease-out both;
  animation-delay: var(--item-delay);

  &:hover {
    background: rgba(124, 92, 255, 0.08);
    color: rgba(255, 255, 255, 0.95);
    transform: translateX(4px);

    .item-arrow {
      opacity: 1;
      transform: translateX(0);
    }

    .item-glow {
      opacity: 1;
    }
  }

  &.active {
    background: linear-gradient(135deg, rgba(124, 92, 255, 0.25), rgba(59, 130, 246, 0.15));
    color: #fff;
    box-shadow: 
      0 4px 20px rgba(124, 92, 255, 0.25),
      inset 0 1px 0 rgba(255, 255, 255, 0.1);

    .item-glow {
      opacity: 1;
      background: linear-gradient(90deg, rgba(124, 92, 255, 0.3), transparent);
    }

    .item-arrow {
      opacity: 1;
      transform: translateX(0);
      color: #00E5C0;
    }

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 65%;
      background: linear-gradient(180deg, #7C5CFF, #00E5C0);
      border-radius: 2px;
      box-shadow: 0 0 10px rgba(124, 92, 255, 0.5);
    }
  }
}

.item-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(124, 92, 255, 0.2), transparent);
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
}

.item-icon {
  font-size: 20px;
  width: 24px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.item-label {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  position: relative;
  z-index: 1;
}

.item-badge {
  position: relative;
  z-index: 1;
  padding: 2px 8px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border-radius: 10px;
  font-weight: 600;
  font-size: 10px;
  animation: badgePulse 2s ease-in-out infinite;
}

.badge-text {
  position: relative;
  z-index: 1;
}

.item-arrow {
  font-size: 14px;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

.ripple {
  position: absolute;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(124, 92, 255, 0.4);
  transform: translate(-50%, -50%);
  animation: rippleEffect 0.6s ease-out forwards;
  pointer-events: none;
  z-index: 2;
}

.menu-footer {
  padding: 14px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  animation: fadeInUp 0.6s ease-out 0.3s both;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7C5CFF, #3B82F6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: white;
  position: relative;
  box-shadow: 0 4px 20px rgba(124, 92, 255, 0.4);
}

.avatar-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: linear-gradient(135deg, #7C5CFF, #00E5C0) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: rotate 4s linear infinite;
}

.avatar-glow {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.3) 0%, transparent 70%);
  animation: pulse 2s ease-in-out infinite;
}

.user-detail {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.user-status {
  font-size: 11px;
  color: rgba(0, 229, 192, 0.8);
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00E5C0;
  box-shadow: 0 0 8px #00E5C0;
  animation: pulse 2s infinite;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  color: rgba(255, 255, 255, 0.6);

  &:hover {
    background: rgba(255, 107, 107, 0.2);
    border-color: rgba(255, 107, 107, 0.4);
    color: #ff6b6b;
  }
}

.logout-icon {
  font-size: 14px;
}

.logout-text {
  font-size: 12px;
}

.menu-decoration {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 200px;
  pointer-events: none;
  opacity: 0.3;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(124, 92, 255, 0.2);
}

.circle-1 {
  width: 150px;
  height: 150px;
  bottom: 20px;
  left: -50px;
  animation: rotate 30s linear infinite;
}

.circle-2 {
  width: 100px;
  height: 100px;
  bottom: 60px;
  right: -30px;
  animation: rotate 20s linear infinite reverse;
  border-color: rgba(0, 229, 192, 0.2);
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes rippleEffect {
  0% {
    width: 0;
    height: 0;
    opacity: 0.6;
  }
  100% {
    width: 200px;
    height: 200px;
    opacity: 0;
  }
}

@keyframes badgePulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .side-menu {
    transform: translateX(-100%);
    transition: transform 0.3s;

    &.mobile-open {
      transform: translateX(0);
    }
  }
}
</style>
