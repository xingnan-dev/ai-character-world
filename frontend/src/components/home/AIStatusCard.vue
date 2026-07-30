<template>
  <div class="status-card">
    <div class="card-header">
      <h3 class="card-title">AI 档案</h3>
      <span class="card-badge">Active</span>
    </div>

    <div class="card-body">
      <div class="profile-section">
        <div class="section-label">基本信息</div>
        <div class="info-list">
          <div class="info-row">
            <span class="row-icon">🏷️</span>
            <span class="row-label">名字</span>
            <span class="row-value">{{ data.name }}</span>
          </div>
          <div class="info-row">
            <span class="row-icon">💫</span>
            <span class="row-label">性格</span>
            <span class="row-value">{{ data.personality }}</span>
          </div>
          <div class="info-row">
            <span class="row-icon">🌍</span>
            <span class="row-label">身份</span>
            <span class="row-value">{{ data.identity }}</span>
          </div>
          <div class="info-row">
            <span class="row-icon">🎨</span>
            <span class="row-label">兴趣</span>
            <span class="row-value">{{ data.interests }}</span>
          </div>
          <div class="info-row">
            <span class="row-icon">📊</span>
            <span class="row-label">状态</span>
            <span class="row-value status">
              <span class="status-dot"></span>
              {{ data.status === 'online' ? '在线' : data.status === 'offline' ? '离线' : '休眠中' }}
            </span>
          </div>
        </div>
      </div>

      <div class="profile-section">
        <div class="section-label">能力标签</div>
        <div class="tags-list">
          <span v-for="(tag, i) in data.tags" :key="i" class="tag" :style="{ '--delay': i * 0.1 + 's' }">
            {{ tag }}
          </span>
        </div>
      </div>

      <div class="profile-section">
        <div class="section-label">成长值</div>
        <div class="progress-section">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: data.growth + '%' }"></div>
          </div>
          <div class="progress-info">
            <span class="progress-value">{{ data.growth }}%</span>
            <span class="progress-label">Lv.{{ data.level }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: {
    type: Object,
    default: () => ({
      name: 'Luna',
      personality: '温柔、幽默、善解人意',
      identity: '未来AI伙伴',
      interests: '科技、艺术、哲学',
      status: 'online',
      tags: ['温柔', '幽默', '知识渊博', '倾听者'],
      growth: 65,
      level: 3
    })
  }
})
</script>

<style lang="scss" scoped>
.status-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  border: 1px solid rgba(124, 92, 255, 0.2);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  overflow: hidden;
  animation: cardSlideIn 0.6s ease-out;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(124, 92, 255, 0.15);
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
  background: linear-gradient(135deg, rgba(124, 92, 255, 0.1), transparent);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.card-badge {
  padding: 4px 12px;
  background: linear-gradient(135deg, #00E5C0, #3B82F6);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 20px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.card-body {
  padding: 20px 24px;
}

.profile-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
  font-weight: 600;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  transition: background 0.3s;

  &:hover {
    background: rgba(124, 92, 255, 0.1);
  }
}

.row-icon {
  font-size: 16px;
  width: 24px;
  text-align: center;
}

.row-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  width: 50px;
}

.row-value {
  font-size: 13px;
  color: #fff;
  font-weight: 500;
  flex: 1;

  &.status {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #00E5C0;
  }
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00E5C0;
  box-shadow: 0 0 8px #00E5C0;
  animation: pulse 2s infinite;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(124, 92, 255, 0.2), rgba(59, 130, 246, 0.1));
  border: 1px solid rgba(124, 92, 255, 0.3);
  color: #fff;
  font-size: 12px;
  border-radius: 20px;
  animation: tagFadeIn 0.5s ease-out both;
  animation-delay: var(--delay);
}

.progress-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #7C5CFF 0%, #00E5C0 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
  box-shadow: 0 0 10px rgba(124, 92, 255, 0.5);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-value {
  font-size: 14px;
  font-weight: 600;
  color: #00E5C0;
}

.progress-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes tagFadeIn {
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
  .status-card {
    margin-bottom: 20px;
  }
}
</style>
