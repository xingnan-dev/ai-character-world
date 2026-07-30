<template>
  <div class="avatar-create-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon :size="24" color="#fff"><MagicStick /></el-icon>
          <span>AI Companion</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/home" class="nav-item">
          <el-icon :size="18"><Menu /></el-icon>
          <span>首页</span>
        </router-link>
        <router-link to="/avatar/create" class="nav-item active">
          <el-icon :size="18"><User /></el-icon>
          <span>创建形象</span>
        </router-link>
        <router-link to="/chat" class="nav-item">
          <el-icon :size="18"><ChatDotRound /></el-icon>
          <span>聊天</span>
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">创建虚拟形象</h1>
          <p class="page-subtitle">自定义你的专属 AI 3D 虚拟分身</p>
        </div>
        <el-button type="primary" size="large" :icon="Check">保存形象</el-button>
      </div>

      <div class="create-body">
        <div class="preview-section">
          <div class="preview-header">
            <h3>3D 预览</h3>
            <div class="preview-actions">
              <el-button :icon="Refresh" circle size="small" title="重置视角" />
              <el-button :icon="FullScreen" circle size="small" title="全屏" />
            </div>
          </div>
          <div class="preview-area">
            <AvatarRenderer />
            <div class="preview-hint">
              <el-icon :size="16"><InfoFilled /></el-icon>
              <span>拖拽旋转 · 滚轮缩放</span>
            </div>
          </div>
        </div>

        <div class="config-section">
          <div class="config-card">
            <h3 class="config-title">基础设置</h3>
            <el-form
              ref="formRef"
              :model="form"
              label-position="top"
              class="config-form"
            >
              <el-form-item label="形象名称">
                <el-input
                  v-model="form.name"
                  placeholder="给你的形象起个名字"
                  clearable
                />
              </el-form-item>

              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio-button value="male">男</el-radio-button>
                  <el-radio-button value="female">女</el-radio-button>
                  <el-radio-button value="other">其他</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="性格">
                <el-select
                  v-model="form.personality"
                  placeholder="选择性格类型"
                >
                  <el-option label="温柔体贴" value="gentle" />
                  <el-option label="活泼开朗" value="cheerful" />
                  <el-option label="知性沉稳" value="wise" />
                  <el-option label="幽默风趣" value="humorous" />
                  <el-option label="高冷神秘" value="cool" />
                </el-select>
              </el-form-item>

              <el-form-item label="开场白">
                <el-input
                  v-model="form.greeting"
                  type="textarea"
                  :rows="3"
                  placeholder="设置形象的开场白，初次对话时会自动发送"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="config-card">
            <h3 class="config-title">模型上传</h3>
            <el-upload
              class="model-uploader"
              drag
              :auto-upload="false"
              :show-file-list="false"
              accept=".vrm,.glb,.gltf"
            >
              <el-icon class="upload-icon" :size="48"><UploadFilled /></el-icon>
              <div class="upload-text">
                将 VRM / GLB 文件拖拽到此处
              </div>
              <template #tip>
                <div class="upload-tip">
                  支持 .vrm / .glb / .gltf 格式，文件大小不超过 20MB
                </div>
              </template>
            </el-upload>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import {
  MagicStick,
  Menu,
  User,
  ChatDotRound,
  Check,
  Refresh,
  FullScreen,
  InfoFilled,
  UploadFilled
} from '@element-plus/icons-vue'
import AvatarRenderer from '../components/AvatarRenderer.vue'

const formRef = ref(null)

const form = reactive({
  name: '',
  gender: 'female',
  personality: 'gentle',
  greeting: ''
})
</script>

<style lang="scss" scoped>
.avatar-create-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: fixed;
  height: 100vh;
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #fff;
  }

  &.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  }
}

.main-content {
  flex: 1;
  margin-left: 220px;
  padding: 28px 36px;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
}

.create-body {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

.preview-section {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  min-height: 600px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #f0f0f0;

  h3 {
    font-size: 17px;
    font-weight: 600;
    color: #303133;
  }
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-area {
  flex: 1;
  position: relative;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  min-height: 500px;
}

.preview-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  font-size: 12px;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.config-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.config-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.config-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    padding-bottom: 4px;
  }
}

.model-uploader {
  width: 100%;

  :deep(.el-upload-dragger) {
    padding: 30px 20px;
    border-radius: 14px;
    border: 2px dashed #dcdfe6;
    transition: all 0.25s;

    &:hover {
      border-color: #667eea;
    }
  }
}

.upload-icon {
  color: #667eea;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 8px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 1100px) {
  .create-body {
    grid-template-columns: 1fr;
  }
}
</style>