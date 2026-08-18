<template>
  <div class="avatar-show" :key="currentAvatarIndex">
    <div class="welcome-section">
      <h1 class="welcome-title">欢迎回来，{{ userName }}</h1>
      <p class="welcome-subtitle">
        <span class="slogan-text">{{ slogan }}</span>
        <span class="slogan-cursor">|</span>
      </p>
    </div>

    <div class="avatar-container">
      <div class="avatar-stage">
        <div class="energy-wave wave-1"></div>
        <div class="energy-wave wave-2"></div>
        <div class="energy-wave wave-3"></div>
        
        <div class="stage-ring"></div>
        <div class="stage-ring ring-2"></div>
        <div class="stage-ring ring-3"></div>
        <div class="stage-ring ring-4"></div>
        
        <div class="avatar-3d-scene">
          <div class="avatar-light light-1"></div>
          <div class="avatar-light light-2"></div>

          <AvatarRenderer
            v-if="avatar?.modelUrl"
            class="home-avatar-renderer"
            :model-url="avatar.modelUrl"
            presentation="home"
            @loaded="handleAvatarLoaded"
            @error="handleAvatarError"
          />
          <div v-else class="avatar-model-state">
            <span class="model-state-icon">◇</span>
            <span>当前形象没有可加载的3D模型</span>
          </div>
          <div v-if="avatarLoadError" class="avatar-model-state avatar-model-error">
            <span class="model-state-icon">!</span>
            <span>3D模型加载失败，请稍后重试</span>
          </div>
          <div class="floating-particles">
            <span v-for="i in 12" :key="i" class="particle" :style="getParticleStyle(i)"></span>
          </div>
        </div>

        <div class="avatar-info">
          <div class="info-item">
            <span class="info-icon">🏷️</span>
            <div class="info-content">
              <span class="info-label">名字</span>
              <span class="info-value">{{ avatar.name }}</span>
            </div>
          </div>
          <div class="info-item">
            <span class="info-icon">💫</span>
            <div class="info-content">
              <span class="info-label">身份</span>
              <span class="info-value">{{ avatar.identity }}</span>
            </div>
          </div>
          <div class="info-item status-online">
            <span class="info-icon">⚡</span>
            <div class="info-content">
              <span class="info-label">状态</span>
              <span class="status-indicator">
                <span class="status-dot online"></span>
                在线
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="action-buttons">
      <button class="btn btn-create" @click="$emit('create')">
        <span class="btn-glow"></span>
        <span class="btn-icon">✨</span>
        <span class="btn-text">创造AI形象</span>
      </button>
      <button class="btn btn-chat" @click="$emit('chat')">
        <span class="btn-glow"></span>
        <span class="btn-icon">💬</span>
        <span class="btn-text">开始聊天</span>
      </button>
    </div>

    <div v-if="avatars.length > 1" class="avatar-switcher">
      <span class="switcher-label">切换形象：</span>
      <div class="switcher-list">
        <div
          v-for="(av, index) in avatars"
          :key="av.id"
          class="switcher-item"
          :class="{ active: currentAvatarIndex === index }"
          @click="$emit('switch', av)"
        >
          <span class="switcher-icon">{{ av.emoji || '🤖' }}</span>
          <span class="switcher-name">{{ av.name }}</span>
          <span v-if="currentAvatarIndex === index" class="switcher-ring"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AvatarRenderer from '../AvatarRenderer.vue'

const props = defineProps({
  userName: {
    type: String,
    default: '创造者'
  },
  slogan: {
    type: String,
    default: '所有没有你的日子，都存在缺陷'
  },
  avatar: {
    type: Object,
    default: () => ({
      name: 'Luna',
      identity: '未来AI伙伴',
      status: 'online',
      emoji: '🤖'
    })
  },
  avatars: {
    type: Array,
    default: () => []
  },
  currentAvatarIndex: {
    type: Number,
    default: 0
  }
})

defineEmits(['create', 'chat', 'switch'])

const avatarLoadError = ref(false)

const handleAvatarLoaded = () => {
  avatarLoadError.value = false
}

const handleAvatarError = () => {
  avatarLoadError.value = true
}

const getParticleStyle = (i) => {
  const angle = (i / 12) * 360
  const radius = 100 + (i % 4) * 25
  const x = Math.cos(angle * Math.PI / 180) * radius
  const y = Math.sin(angle * Math.PI / 180) * radius
  const delay = i * 0.2
  const duration = 6 + (i % 3) * 2
  return {
    transform: `translate(${x}px, ${y}px)`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

</script>

<style lang="scss" scoped>
.avatar-show {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  animation: avatarFadeIn 0.8s ease-out;
}

@keyframes avatarFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.welcome-section {
  text-align: center;
  margin-bottom: 32px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #fff 0%, #7C5CFF 50%, #00E5C0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: titleShimmer 3s ease-in-out infinite;
  background-size: 200% 200%;
}

@keyframes titleShimmer {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.welcome-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  font-style: italic;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.slogan-cursor {
  animation: blinkCursor 1s infinite;
  color: #00E5C0;
}

@keyframes blinkCursor {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

.avatar-container {
  width: 100%;
  max-width: 520px;
  margin-bottom: 32px;
}

.avatar-stage {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background: linear-gradient(180deg, rgba(124, 92, 255, 0.05) 0%, rgba(6, 8, 22, 0.8) 100%);
  border-radius: 32px;
  border: 1px solid rgba(124, 92, 255, 0.25);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  overflow: hidden;
}

.energy-wave {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(124, 92, 255, 0.3);
  pointer-events: none;

  &.wave-1 {
    width: 180px;
    height: 180px;
    animation: energyPulse 3s ease-out infinite;
  }

  &.wave-2 {
    width: 180px;
    height: 180px;
    animation: energyPulse 3s ease-out infinite 1s;
  }

  &.wave-3 {
    width: 180px;
    height: 180px;
    animation: energyPulse 3s ease-out infinite 2s;
  }
}

@keyframes energyPulse {
  0% {
    transform: scale(0.5);
    opacity: 1;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

.stage-ring {
  position: absolute;
  border: 1px solid rgba(124, 92, 255, 0.25);
  border-radius: 50%;
  pointer-events: none;

  &.ring-1 {
    width: 280px;
    height: 280px;
    animation: ringRotate 20s linear infinite;
    border-style: dashed;
  }

  &.ring-2 {
    width: 340px;
    height: 340px;
    border-color: rgba(59, 130, 246, 0.2);
    animation: ringRotate 30s linear infinite reverse;
    border-style: dotted;
  }

  &.ring-3 {
    width: 400px;
    height: 400px;
    border-color: rgba(0, 229, 192, 0.15);
    animation: ringRotate 40s linear infinite;
  }

  &.ring-4 {
    width: 460px;
    height: 460px;
    border-color: rgba(240, 147, 251, 0.1);
    animation: ringRotate 50s linear infinite reverse;
    border-style: double;
  }
}

@keyframes ringRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.avatar-3d-scene {
  position: relative;
  width: min(380px, 78vw);
  height: 410px;
  perspective: 1000px;
  z-index: 2;
}

.home-avatar-renderer {
  position: absolute;
  inset: 0;
  z-index: 2;
  border-radius: 50% 50% 24px 24px;
  filter: drop-shadow(0 20px 32px rgba(0, 0, 0, 0.3));
}

.avatar-model-state {
  position: absolute;
  inset: 0;
  z-index: 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px;
  border: 1px dashed rgba(124, 92, 255, 0.35);
  border-radius: 20px;
  background: rgba(6, 8, 22, 0.72);
  color: rgba(255, 255, 255, 0.65);
  text-align: center;
  font-size: 13px;
}

.avatar-model-error {
  border-color: rgba(255, 107, 129, 0.4);
  color: rgba(255, 180, 190, 0.9);
}

.model-state-icon {
  font-size: 28px;
  line-height: 1;
}

.avatar-light {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(20px);

  &.light-1 {
    width: 60px;
    height: 60px;
    background: rgba(124, 92, 255, 0.4);
    top: 20%;
    left: -20px;
    animation: lightMove 5s ease-in-out infinite;
  }

  &.light-2 {
    width: 40px;
    height: 40px;
    background: rgba(0, 229, 192, 0.3);
    bottom: 30%;
    right: -15px;
    animation: lightMove 5s ease-in-out infinite 2.5s;
  }
}

@keyframes lightMove {
  0%, 100% {
    transform: translate(0, 0);
    opacity: 0.5;
  }
  50% {
    transform: translate(10px, -10px);
    opacity: 1;
  }
}

.avatar-3d {
  position: absolute;
  width: 100%;
  height: 100%;
  animation: avatarFloat 5s ease-in-out infinite;
  transform-style: preserve-3d;
}

@keyframes avatarFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-15px);
  }
}

.avatar-shadow {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 20px;
  background: radial-gradient(ellipse, rgba(124, 92, 255, 0.3) 0%, transparent 70%);
  animation: shadowPulse 5s ease-in-out infinite;
}

@keyframes shadowPulse {
  0%, 100% {
    transform: translateX(-50%) scale(1);
    opacity: 0.5;
  }
  50% {
    transform: translateX(-50%) scale(0.8);
    opacity: 0.3;
  }
}

.avatar-model {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  transform-style: preserve-3d;
}

.avatar-head {
  position: relative;
  width: 100px;
  height: 110px;
  margin: 0 auto;
}

.face-glow {
  position: absolute;
  inset: -15px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.4) 0%, transparent 70%);
  animation: faceGlow 3s ease-in-out infinite;
  filter: blur(10px);
}

@keyframes faceGlow {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.face {
  width: 100%;
  height: 100%;
  background: linear-gradient(145deg, #8B7BFF 0%, #7C5CFF 50%, #3B82F6 100%);
  border-radius: 50% 50% 45% 45%;
  position: relative;
  box-shadow: 
    0 0 50px rgba(124, 92, 255, 0.6),
    inset 0 -15px 30px rgba(0, 0, 0, 0.3),
    inset 0 10px 20px rgba(255, 255, 255, 0.1);
  animation: faceBreathe 3s ease-in-out infinite;
}

@keyframes faceBreathe {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.03);
  }
}

.face-highlight {
  position: absolute;
  top: 15%;
  left: 20%;
  width: 30%;
  height: 25%;
  background: radial-gradient(ellipse, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  border-radius: 50%;
}

.cheek {
  position: absolute;
  top: 55%;
  width: 15px;
  height: 10px;
  background: rgba(255, 150, 200, 0.3);
  border-radius: 50%;
  filter: blur(3px);
  animation: cheekGlow 4s ease-in-out infinite;

  &.cheek-left {
    left: 10%;
  }

  &.cheek-right {
    right: 10%;
    animation-delay: 0.5s;
  }
}

@keyframes cheekGlow {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.6;
  }
}

.hair {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 120px;
  height: 50px;
  background: linear-gradient(180deg, #1a0a3a 0%, #2a1a5a 50%, transparent 100%);
  border-radius: 60px 60px 20px 20px;
  overflow: hidden;
}

.hair-shine {
  position: absolute;
  top: 10px;
  left: 20%;
  width: 60%;
  height: 15px;
  background: linear-gradient(90deg, transparent, rgba(124, 92, 255, 0.3), transparent);
  animation: hairShine 3s ease-in-out infinite;
}

@keyframes hairShine {
  0%, 100% {
    transform: translateX(-20px);
    opacity: 0.5;
  }
  50% {
    transform: translateX(20px);
    opacity: 1;
  }
}

.eyes {
  position: absolute;
  top: 38%;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 18px;
  z-index: 2;
}

.eye {
  width: 22px;
  height: 22px;
  background: radial-gradient(circle at 40% 40%, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.8) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: eyeBlink 4s infinite;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.eye-shine {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 6px;
  height: 6px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  z-index: 2;
}

.pupil {
  width: 10px;
  height: 10px;
  background: radial-gradient(circle at 30% 30%, #1a0a3a 0%, #060816 100%);
  border-radius: 50%;
  animation: pupilMove 6s ease-in-out infinite;
  position: relative;
  z-index: 1;
}

@keyframes eyeBlink {
  0%, 40%, 60%, 100% {
    transform: scaleY(1);
  }
  45%, 55% {
    transform: scaleY(0.1);
  }
  50% {
    transform: scaleY(0.1);
  }
}

@keyframes pupilMove {
  0%, 90%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(2px, -1px);
  }
  50% {
    transform: translate(-1px, 1px);
  }
  75% {
    transform: translate(1px, 0);
  }
}

.mouth {
  position: absolute;
  bottom: 22%;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 6px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 0 0 12px 12px;
  animation: mouthTalk 4s ease-in-out infinite;
  overflow: hidden;
}

.mouth-inner {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: rgba(255, 100, 150, 0.3);
  border-radius: 0 0 12px 12px;
}

@keyframes mouthTalk {
  0%, 80%, 100% {
    height: 6px;
  }
  85%, 95% {
    height: 8px;
  }
}

.avatar-neck {
  width: 30px;
  height: 15px;
  margin: -5px auto 0;
  background: linear-gradient(180deg, #6a4acc 0%, #4a2a8c 100%);
  border-radius: 0 0 10px 10px;
}

.avatar-body {
  width: 90px;
  height: 80px;
  margin: -5px auto 0;
  position: relative;
}

.body-armor {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #7C5CFF 0%, #3B82F6 50%, #1a0a3a 100%);
  border-radius: 45px 45px 25px 25px;
  box-shadow: 
    0 4px 20px rgba(124, 92, 255, 0.4),
    inset 0 2px 10px rgba(255, 255, 255, 0.1);
}

.body-core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  background: radial-gradient(circle, #00E5C0 0%, #7C5CFF 100%);
  border-radius: 50%;
  animation: corePulse 2s ease-in-out infinite;
  box-shadow: 0 0 20px #00E5C0;
}

@keyframes corePulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    box-shadow: 0 0 20px #00E5C0;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    box-shadow: 0 0 30px #00E5C0, 0 0 40px #7C5CFF;
  }
}

.body-glow {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 8px;
  background: linear-gradient(90deg, transparent, #00E5C0, transparent);
  filter: blur(4px);
  animation: bodyGlow 2s ease-in-out infinite;
}

@keyframes bodyGlow {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.body-lines {
  position: absolute;
  inset: 0;
  background: 
    linear-gradient(90deg, transparent 45%, rgba(255, 255, 255, 0.1) 50%, transparent 55%),
    linear-gradient(0deg, transparent 45%, rgba(255, 255, 255, 0.1) 50%, transparent 55%);
  background-size: 20px 20px;
  border-radius: 45px 45px 25px 25px;
  opacity: 0.5;
}

.floating-particles {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: #00E5C0;
  border-radius: 50%;
  box-shadow: 0 0 8px #00E5C0;
  animation: particleOrbit 8s linear infinite;
}

@keyframes particleOrbit {
  0% {
    transform: rotate(0deg) translateX(100px) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: rotate(360deg) translateX(100px) rotate(-360deg);
    opacity: 0;
  }
}

.avatar-info {
  margin-top: 40px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  animation: infoSlideUp 0.6s ease-out 0.3s both;
}

@keyframes infoSlideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: linear-gradient(135deg, rgba(124, 92, 255, 0.1), rgba(59, 130, 246, 0.05));
  border-radius: 14px;
  border: 1px solid rgba(124, 92, 255, 0.2);
  transition: all 0.3s;

  &:hover {
    background: linear-gradient(135deg, rgba(124, 92, 255, 0.2), rgba(59, 130, 246, 0.1));
    transform: translateY(-2px);
  }

  &.status-online {
    border-color: rgba(0, 229, 192, 0.3);
    background: linear-gradient(135deg, rgba(0, 229, 192, 0.1), transparent);
  }
}

.info-icon {
  font-size: 16px;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #00E5C0;
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00E5C0;
  box-shadow: 0 0 10px #00E5C0;
  animation: statusPulse 2s infinite;
}

@keyframes statusPulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 10px #00E5C0;
  }
  50% {
    opacity: 0.6;
    box-shadow: 0 0 20px #00E5C0;
  }
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  animation: buttonsSlideUp 0.6s ease-out 0.5s both;
}

@keyframes buttonsSlideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-3px) scale(1.02);
  }

  &:active {
    transform: translateY(0) scale(0.98);
  }
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.btn:hover .btn-glow {
  left: 100%;
}

.btn-icon {
  font-size: 18px;
  position: relative;
  z-index: 1;
}

.btn-text {
  position: relative;
  z-index: 1;
}

.btn-create {
  background: linear-gradient(135deg, #7C5CFF 0%, #3B82F6 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(124, 92, 255, 0.4);

  &:hover {
    box-shadow: 0 8px 40px rgba(124, 92, 255, 0.6);
  }
}

.btn-chat {
  background: linear-gradient(135deg, rgba(0, 229, 192, 0.1) 0%, rgba(59, 130, 246, 0.1) 100%);
  color: #fff;
  border: 2px solid rgba(0, 229, 192, 0.5);

  &:hover {
    background: linear-gradient(135deg, rgba(0, 229, 192, 0.2), rgba(59, 130, 246, 0.15));
    border-color: #00E5C0;
    box-shadow: 0 8px 40px rgba(0, 229, 192, 0.3);
  }
}

.avatar-switcher {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  animation: switcherFadeIn 0.6s ease-out 0.7s both;
}

@keyframes switcherFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.switcher-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
}

.switcher-list {
  display: flex;
  gap: 10px;
}

.switcher-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 55px;

  &:hover {
    background: rgba(124, 92, 255, 0.2);
    transform: translateY(-3px);
  }

  &.active {
    background: linear-gradient(135deg, #7C5CFF, #3B82F6);
    box-shadow: 0 4px 20px rgba(124, 92, 255, 0.5);
    transform: translateY(-3px);
  }
}

.switcher-icon {
  font-size: 24px;
}

.switcher-name {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.switcher-ring {
  position: absolute;
  inset: -2px;
  border-radius: 16px;
  border: 2px solid #00E5C0;
  animation: ringPulse 2s ease-in-out infinite;
  pointer-events: none;
}

@keyframes ringPulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 10px #00E5C0;
  }
  50% {
    opacity: 0.5;
    box-shadow: 0 0 20px #00E5C0;
  }
}

@media (max-width: 768px) {
  .avatar-show {
    padding: 20px 16px;
  }

  .welcome-title {
    font-size: 24px;
  }

  .avatar-stage {
    padding: 30px 16px;
  }

  .avatar-3d-scene {
    width: min(300px, 82vw);
    height: 340px;
  }

  .avatar-head {
    width: 80px;
    height: 90px;
  }

  .face {
    width: 80px;
    height: 90px;
  }

  .eye {
    width: 18px;
    height: 18px;
  }

  .pupil {
    width: 8px;
    height: 8px;
  }

  .hair {
    width: 100px;
    height: 40px;
  }

  .avatar-body {
    width: 70px;
    height: 60px;
  }

  .body-armor {
    border-radius: 35px 35px 20px 20px;
  }

  .avatar-info {
    gap: 8px;
  }

  .info-item {
    padding: 8px 12px;
  }

  .action-buttons {
    flex-direction: column;
    width: 100%;
    max-width: 280px;
  }

  .btn {
    justify-content: center;
  }
}
</style>
