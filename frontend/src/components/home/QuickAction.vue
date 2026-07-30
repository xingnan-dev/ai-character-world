<template>
  <div class="quick-actions">
    <div class="card-header">
      <h3 class="card-title">快捷入口</h3>
    </div>

    <div class="actions-grid">
      <div class="action-item action-create" @click="$emit('create')">
        <div class="action-icon-wrap">
          <span class="action-icon">✨</span>
        </div>
        <div class="action-info">
          <span class="action-name">创造AI形象</span>
          <span class="action-desc">定制专属角色</span>
        </div>
        <span class="action-arrow">→</span>
      </div>

      <div class="action-item action-chat" @click="$emit('chat')">
        <div class="action-icon-wrap">
          <span class="action-icon">💬</span>
        </div>
        <div class="action-info">
          <span class="action-name">开始聊天</span>
          <span class="action-desc">与TA深度交流</span>
        </div>
        <span class="action-arrow">→</span>
      </div>

      <div class="action-item action-memory" @click="$emit('memory')">
        <div class="action-icon-wrap">
          <span class="action-icon">🧠</span>
        </div>
        <div class="action-info">
          <span class="action-name">记忆空间</span>
          <span class="action-desc">查看共同记忆</span>
        </div>
        <span class="action-arrow">→</span>
      </div>

      <div class="action-item action-warehouse" @click="$emit('warehouse')">
        <div class="action-icon-wrap">
          <span class="action-icon">🎨</span>
        </div>
        <div class="action-info">
          <span class="action-name">形象仓库</span>
          <span class="action-desc">管理所有角色</span>
        </div>
        <span class="action-arrow">→</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineEmits(['create', 'chat', 'memory', 'warehouse'])
</script>

<style lang="scss" scoped>
.quick-actions {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  overflow: hidden;
  animation: cardSlideIn 0.6s ease-out 0.2s both;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 12px 40px rgba(59, 130, 246, 0.1);
  }
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), transparent);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  background: rgba(255, 255, 255, 0.05);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: rgba(10, 14, 39, 0.6);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
    transition: left 0.5s ease;
  }

  &:hover {
    background: rgba(124, 92, 255, 0.15);
    transform: translateX(4px);

    &::before {
      left: 100%;
    }

    .action-arrow {
      transform: translateX(4px);
      opacity: 1;
    }
  }

  &:nth-child(2):hover {
    background: rgba(0, 229, 192, 0.15);
  }

  &:nth-child(3):hover {
    background: rgba(59, 130, 246, 0.15);
  }

  &:nth-child(4):hover {
    background: rgba(240, 147, 251, 0.15);
  }
}

.action-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-create .action-icon-wrap {
  background: linear-gradient(135deg, #7C5CFF, #3B82F6);
  box-shadow: 0 4px 15px rgba(124, 92, 255, 0.4);
}

.action-chat .action-icon-wrap {
  background: linear-gradient(135deg, #00E5C0, #3B82F6);
  box-shadow: 0 4px 15px rgba(0, 229, 192, 0.4);
}

.action-memory .action-icon-wrap {
  background: linear-gradient(135deg, #3B82F6, #7C5CFF);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.action-warehouse .action-icon-wrap {
  background: linear-gradient(135deg, #f093fb, #7C5CFF);
  box-shadow: 0 4px 15px rgba(240, 147, 251, 0.4);
}

.action-icon {
  font-size: 22px;
}

.action-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.action-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.action-arrow {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.3);
  transition: all 0.3s;
  opacity: 0.5;
}

@media (max-width: 768px) {
  .actions-grid {
    grid-template-columns: 1fr;
  }
}
</style>
