<template>
  <div class="loading-skeleton" v-if="loading">
    <div class="skeleton-overlay">
      <div class="skeleton-content">
        <div class="skeleton-logo">
          <div class="logo-icon-skull"></div>
          <div class="logo-text-skull"></div>
        </div>

        <div class="skeleton-avatar">
          <div class="avatar-ring"></div>
          <div class="avatar-model-skull">
            <div class="skull-head"></div>
            <div class="skull-body"></div>
          </div>
        </div>

        <div class="skeleton-info">
          <div class="info-line line-1"></div>
          <div class="info-line line-2"></div>
        </div>

        <div class="skeleton-buttons">
          <div class="btn-skull btn-1"></div>
          <div class="btn-skull btn-2"></div>
        </div>

        <div class="loading-text">
          <span class="text-dot dot-1">●</span>
          <span class="text-dot dot-2">●</span>
          <span class="text-dot dot-3">●</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: true
  }
})
</script>

<style lang="scss" scoped>
.loading-skeleton {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 8, 22, 0.95);
  backdrop-filter: blur(10px);
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.skeleton-overlay {
  width: 100%;
  max-width: 500px;
  padding: 40px;
  display: flex;
  justify-content: center;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  width: 100%;
}

.skeleton-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon-skull {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7C5CFF, #3B82F6);
  animation: skullPulse 1.5s ease-in-out infinite;
}

.logo-text-skull {
  width: 80px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(90deg, #1a0a3a 25%, #2a1a5a 50%, #1a0a3a 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes skullPulse {
  0%, 100% {
    opacity: 0.8;
  }
  50% {
    opacity: 1;
  }
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton-avatar {
  position: relative;
  width: 180px;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-ring {
  position: absolute;
  width: 150px;
  height: 150px;
  border: 2px solid rgba(124, 92, 255, 0.3);
  border-radius: 50%;
  border-top-color: transparent;
  border-right-color: transparent;
  animation: ringSpin 2s linear infinite;
}

@keyframes ringSpin {
  to {
    transform: rotate(360deg);
  }
}

.avatar-model-skull {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  animation: avatarFloat 2s ease-in-out infinite;
}

@keyframes avatarFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.skull-head {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(145deg, #1a0a3a 0%, #2a1a5a 100%);
  position: relative;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 35px;
    width: 10px;
    height: 12px;
    background: rgba(124, 92, 255, 0.5);
    border-radius: 50%;
    animation: eyePulse 1.5s ease-in-out infinite;
  }

  &::before {
    left: 20px;
  }

  &::after {
    right: 20px;
    animation-delay: 0.3s;
  }
}

@keyframes eyePulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.skull-body {
  width: 60px;
  height: 50px;
  background: linear-gradient(180deg, #2a1a5a 0%, #1a0a3a 100%);
  border-radius: 30px 30px 15px 15px;
}

.skeleton-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 200px;
}

.info-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #1a0a3a 25%, #2a1a5a 50%, #1a0a3a 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;

  &.line-1 {
    width: 100%;
  }

  &.line-2 {
    width: 70%;
    margin: 0 auto;
  }
}

.skeleton-buttons {
  display: flex;
  gap: 16px;
}

.btn-skull {
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(90deg, #1a0a3a 25%, #2a1a5a 50%, #1a0a3a 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;

  &.btn-1 {
    width: 140px;
  }

  &.btn-2 {
    width: 120px;
  }
}

.loading-text {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.text-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7C5CFF;
  animation: dotBounce 1.4s ease-in-out infinite;

  &.dot-1 {
    animation-delay: 0s;
  }

  &.dot-2 {
    animation-delay: 0.2s;
    background: #3B82F6;
  }

  &.dot-3 {
    animation-delay: 0.4s;
    background: #00E5C0;
  }
}

@keyframes dotBounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .skeleton-overlay {
    padding: 20px;
  }

  .skeleton-avatar {
    width: 140px;
    height: 200px;
  }

  .avatar-ring {
    width: 120px;
    height: 120px;
  }

  .skull-head {
    width: 60px;
    height: 60px;
  }

  .skull-body {
    width: 45px;
    height: 40px;
  }

  .btn-skull.btn-1 {
    width: 100px;
  }

  .btn-skull.btn-2 {
    width: 80px;
  }
}
</style>
