<template>
  <div class="memory-card">
    <div class="card-header">
      <h3 class="card-title">共同记忆</h3>
      <span class="memory-count">{{ memories.length }} 条</span>
    </div>

    <div class="card-body">
      <div v-if="memories.length === 0" class="empty-state">
        <div class="empty-icon">🧠</div>
        <p class="empty-text">开始与AI对话，<br />创造属于你们的共同记忆</p>
      </div>

      <div v-else class="memory-list">
        <div
          v-for="(memory, index) in memories"
          :key="index"
          class="memory-item"
          :style="{ '--delay': index * 0.1 + 's' }"
        >
          <div class="memory-timeline">
            <div class="timeline-dot" :class="memory.type"></div>
            <div v-if="index < memories.length - 1" class="timeline-line"></div>
          </div>
          <div class="memory-content">
            <div class="memory-header">
              <span class="memory-icon">{{ getIcon(memory.type) }}</span>
              <span class="memory-time">{{ memory.time }}</span>
            </div>
            <p class="memory-text">{{ memory.content }}</p>
          </div>
        </div>
      </div>

      <button v-if="memories.length > 0" class="view-more-btn" @click="$emit('viewAll')">
        查看全部记忆
        <span class="arrow">→</span>
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  memories: {
    type: Array,
    default: () => [
      {
        type: 'learning',
        content: '你正在学习 Spring Boot 后端开发',
        time: '2小时前'
      },
      {
        type: 'preference',
        content: '你喜欢未来科技风格的设计',
        time: '昨天'
      },
      {
        type: 'achievement',
        content: '完成了第一次AI角色创建',
        time: '3天前'
      }
    ]
  }
})

defineEmits(['viewAll'])

const getIcon = (type) => {
  const icons = {
    learning: '📚',
    preference: '💝',
    achievement: '🏆',
    conversation: '💬',
    emotion: '😊'
  }
  return icons[type] || '📝'
}
</script>

<style lang="scss" scoped>
.memory-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  border: 1px solid rgba(0, 229, 192, 0.2);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  overflow: hidden;
  animation: cardSlideIn 0.6s ease-out 0.1s both;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(0, 229, 192, 0.1);
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: linear-gradient(135deg, rgba(0, 229, 192, 0.1), transparent);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.memory-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.card-body {
  padding: 20px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.6;
}

.memory-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.memory-item {
  display: flex;
  gap: 12px;
  animation: fadeInUp 0.5s ease-out both;
  animation-delay: var(--delay);
}

.memory-timeline {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 20px;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.2);

  &.learning {
    background: #3B82F6;
    box-shadow: 0 0 8px #3B82F6;
  }

  &.preference {
    background: #7C5CFF;
    box-shadow: 0 0 8px #7C5CFF;
  }

  &.achievement {
    background: #00E5C0;
    box-shadow: 0 0 8px #00E5C0;
  }

  &.conversation {
    background: #f093fb;
    box-shadow: 0 0 8px #f093fb;
  }

  &.emotion {
    background: #4facfe;
    box-shadow: 0 0 8px #4facfe;
  }
}

.timeline-line {
  width: 2px;
  flex: 1;
  min-height: 20px;
  background: linear-gradient(180deg, rgba(124, 92, 255, 0.5), transparent);
  margin-top: 4px;
}

.memory-content {
  flex: 1;
  padding-bottom: 8px;
}

.memory-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.memory-icon {
  font-size: 14px;
}

.memory-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.memory-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
  margin: 0;
}

.view-more-btn {
  width: 100%;
  margin-top: 16px;
  padding: 12px;
  background: rgba(0, 229, 192, 0.1);
  border: 1px solid rgba(0, 229, 192, 0.3);
  border-radius: 12px;
  color: #00E5C0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;

  &:hover {
    background: rgba(0, 229, 192, 0.2);
    border-color: #00E5C0;
    box-shadow: 0 0 20px rgba(0, 229, 192, 0.2);
  }

  .arrow {
    transition: transform 0.3s;
  }

  &:hover .arrow {
    transform: translateX(4px);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .memory-card {
    margin-bottom: 20px;
  }
}
</style>
