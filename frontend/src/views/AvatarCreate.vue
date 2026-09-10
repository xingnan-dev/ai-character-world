<template>
  <div class="avatar-create-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <el-icon :size="24" color="#fff"><MagicStick /></el-icon>
          <span>AIworld</span>
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
          <p class="page-subtitle">用 AI 一键生成，或自定义专属 3D 虚拟分身</p>
        </div>
      </div>

      <div class="create-body">
        <!-- Left: Preview Section -->
        <div class="preview-section">
          <div class="preview-header">
            <h3>3D 预览</h3>
            <div class="preview-actions">
              <el-button :icon="Refresh" circle size="small" title="重置视角" />
              <el-button :icon="FullScreen" circle size="small" title="全屏" />
            </div>
          </div>
          <div class="preview-area">
            <AvatarRenderer
              v-if="previewModelUrl"
              :model-url="previewModelUrl"
              :avatar-name="generatedAvatar?.name"
            />
            <div v-else class="preview-placeholder">
              <el-icon :size="80" color="#4a4a6a"><User /></el-icon>
              <p>生成形象后将在此处预览</p>
            </div>
            <div class="preview-hint">
              <el-icon :size="16"><InfoFilled /></el-icon>
              <span>拖拽旋转 · 滚轮缩放</span>
            </div>
          </div>
        </div>

        <!-- Right: Config Section -->
        <div class="config-section">
          <!-- Mode Tabs -->
          <div class="mode-tabs">
            <div 
              class="mode-tab" 
              :class="{ active: mode === 'ai' }"
              @click="mode = 'ai'"
            >
              <el-icon :size="20"><MagicStick /></el-icon>
              <span>AI 生成</span>
            </div>
            <div 
              class="mode-tab" 
              :class="{ active: mode === 'manual' }"
              @click="mode = 'manual'"
            >
              <el-icon :size="20"><EditPen /></el-icon>
              <span>手动创建</span>
            </div>
          </div>

          <!-- AI Generation Mode -->
          <div v-if="mode === 'ai'" class="config-card">
            <h3 class="config-title">AI 智能生成</h3>
            
            <div class="ai-generate-area">
              <el-input
                v-model="aiDescription"
                type="textarea"
                :rows="6"
                placeholder="描述你想要创建的角色，例如：&#10;创建一个银色长发、蓝色眼睛、猫耳、机械翅膀、黑色战甲、性格温柔但高冷的AI少女"
                maxlength="500"
                show-word-limit
                class="ai-textarea"
              />
              
              <div class="quick-tags">
                <span class="quick-tags-label">快速示例：</span>
                <el-tag 
                  v-for="tag in quickExamples" 
                  :key="tag" 
                  class="quick-tag"
                  @click="applyExample(tag)"
                >
                  {{ tag }}
                </el-tag>
              </div>

              <el-button
                type="primary"
                size="large"
                :icon="MagicStick"
                :loading="generating"
                :disabled="generating || !aiDescription.trim()"
                class="generate-btn"
                @click="handleGenerate"
              >
                {{ generating ? 'AI 分析中...' : 'AI 生成形象' }}
              </el-button>
            </div>

            <!-- Generated Result -->
            <div v-if="generatedAvatar && !generating" class="result-panel">
              <h4 class="result-title">
                <el-icon color="#67c23a"><CircleCheck /></el-icon>
                生成结果
              </h4>

              <div v-if="parseSourceMessage" class="parse-source-warning">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ parseSourceMessage }}</span>
              </div>

              <h5 class="result-section-title">角色设定</h5>

              <div class="result-info">
                <div class="info-item">
                  <span class="info-label">名称</span>
                  <span class="info-value">{{ generatedAvatar.name }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">性别</span>
                  <span class="info-value">{{ genderLabel }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">性格</span>
                  <span class="info-value">{{ generatedPersonality?.corePersonality || '-' }}</span>
                </div>
              </div>

              <div v-if="generatedAttributes?.length" class="attributes-list">
                <span 
                  v-for="attr in generatedAttributes" 
                  :key="attr.category + attr.attrKey"
                  class="attr-tag"
                >
                  {{ getAttrLabel(attr.category) }}: {{ attr.attrValue }}
                </span>
              </div>

              <h5 class="result-section-title">模型匹配</h5>
              <div class="model-match-panel" :class="assetMatchType?.toLowerCase()">
                <div class="model-match-message">{{ assetMatchMessage }}</div>
                <div class="model-file">模型：{{ generatedAvatar.modelUrl }}</div>
                <div v-if="unmatchedAttributeLabels.length" class="unmatched-attributes">
                  <span class="unmatched-label">未满足属性：</span>
                  <el-tag
                    v-for="attribute in unmatchedAttributeLabels"
                    :key="attribute"
                    type="warning"
                    size="small"
                  >
                    {{ attribute }}
                  </el-tag>
                </div>
              </div>

              <div class="result-actions">
                <el-button type="primary" :icon="Check" @click="saveGenerated">保存形象</el-button>
                <el-button :icon="Refresh" @click="resetGenerate">重新生成</el-button>
              </div>
            </div>
          </div>

          <!-- Manual Creation Mode -->
          <div v-if="mode === 'manual'" class="config-card">
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

            <el-button 
              type="primary" 
              size="large" 
              :icon="Check" 
              class="save-btn"
              @click="handleSaveManual"
            >
              保存形象
            </el-button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  MagicStick,
  Menu,
  User,
  ChatDotRound,
  Check,
  Refresh,
  FullScreen,
  InfoFilled,
  EditPen,
  CircleCheck
} from '@element-plus/icons-vue'
import { generateAvatar, createAvatar } from '../api/avatar'
import AvatarRenderer from '../components/AvatarRenderer.vue'
import { useAvatarStore } from '../stores/avatar'
import { createAvatarGenerationGuard } from '../utils/avatarGenerationGuard'
import {
  getMatchMessage,
  getParseSourceMessage,
  mapUnmatchedAttributes
} from '../utils/avatarMatchResult'

const avatarStore = useAvatarStore()

// Mode switching
const mode = ref('ai')

// AI Generation
const aiDescription = ref('')
const generating = ref(false)
const generationGuard = createAvatarGenerationGuard()
const generatedAvatar = ref(null)
const generatedPersonality = ref(null)
const generatedAttributes = ref([])
const previewModelUrl = ref('')
const parseSource = ref('')
const assetMatchType = ref('')
const unmatchedAttributes = ref([])

const quickExamples = [
  '创建一个银色长发、蓝色眼睛、猫耳、机械翅膀、黑色战甲、性格温柔但高冷的AI少女',
  '创建一个紫色短发、异瞳、优雅外套的神秘高冷少女',
  '创建一个黑色长发、温柔眼睛、休闲服装的治愈系温柔少女'
]

const formRef = ref(null)
const form = reactive({
  name: '',
  gender: 'female',
  personality: 'gentle',
  greeting: ''
})

const genderLabel = computed(() => {
  if (!generatedAvatar.value) return '-'
  const g = generatedAvatar.value.gender
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '其他'
})

const parseSourceMessage = computed(() => getParseSourceMessage(parseSource.value))
const assetMatchMessage = computed(() => getMatchMessage(assetMatchType.value))
const unmatchedAttributeLabels = computed(() => mapUnmatchedAttributes(unmatchedAttributes.value))

function getAttrLabel(category) {
  const labels = {
    hair: '发型',
    eye: '眼睛',
    body: '体型',
    ear: '耳朵',
    wing: '翅膀',
    outfit: '服装',
    accessory: '配饰'
  }
  return labels[category] || category
}

function applyExample(text) {
  aiDescription.value = text
}

async function handleGenerate() {
  if (generating.value || !generationGuard.tryAcquire()) return

  if (!aiDescription.value.trim()) {
    generationGuard.release()
    ElMessage.warning('请输入角色描述')
    return
  }

  generating.value = true
  generatedAvatar.value = null
  generatedPersonality.value = null
  generatedAttributes.value = []
  previewModelUrl.value = ''
  parseSource.value = ''
  assetMatchType.value = ''
  unmatchedAttributes.value = []

  try {
    const response = await generateAvatar({
      description: aiDescription.value,
      createPersonality: true
    })

    if (response.data) {
      generatedAvatar.value = response.data.avatar
      generatedPersonality.value = response.data.personality
      generatedAttributes.value = response.data.attributes || []
      parseSource.value = response.data.parseSource || ''
      assetMatchType.value = response.data.assetMatchType || ''
      unmatchedAttributes.value = response.data.unmatchedAttributes || []
      
      // Set the model URL for preview
      if (response.data.avatar.modelUrl) {
        previewModelUrl.value = response.data.avatar.modelUrl
      }
      
      ElMessage.success(response.data.message || '形象生成成功！')
    }
  } catch (error) {
    console.error('Generate failed:', error)
  } finally {
    generating.value = false
    generationGuard.release()
  }
}

async function saveGenerated() {
  if (!generatedAvatar.value) return
  
  try {
    // AI 生成时后端已自动保存，这里刷新列表即可
    ElMessage.success('形象已保存！')
    
    // 重新获取头像列表
    await avatarStore.fetchAvatarList()
    
    // 重置表单，准备生成下一个
    resetGenerate()
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error('保存失败，请重试')
  }
}

function resetGenerate() {
  generatedAvatar.value = null
  generatedPersonality.value = null
  generatedAttributes.value = []
  previewModelUrl.value = ''
  parseSource.value = ''
  assetMatchType.value = ''
  unmatchedAttributes.value = []
  aiDescription.value = ''
}

async function handleSaveManual() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入形象名称')
    return
  }

  try {
    const data = {
      name: form.name,
      type: 1,
      baseModel: 'nova',
      modelUrl: '/models/avatars/nova.vrm',
      slogan: form.greeting,
      appearanceConfig: JSON.stringify({
        gender: form.gender
      }),
      personality: buildPersonalityRequest(form.personality, form.name)
    }

    const response = await createAvatar(data)
    if (response.data) {
      ElMessage.success('形象创建成功！')
      form.name = ''
      form.greeting = ''
    }
  } catch (error) {
    console.error('Create failed:', error)
    ElMessage.error('创建失败，请重试')
  }
}

function buildPersonalityRequest(type, avatarName) {
  const profiles = {
    gentle: {
      corePersonality: '温柔体贴、善解人意',
      languageStyle: '温柔、自然、耐心'
    },
    cheerful: {
      corePersonality: '活泼开朗、积极乐观',
      languageStyle: '轻松、活泼、富有感染力'
    },
    wise: {
      corePersonality: '知性沉稳、善于思考',
      languageStyle: '清晰、理性、循循善诱'
    },
    humorous: {
      corePersonality: '幽默风趣、乐于互动',
      languageStyle: '轻松、诙谐、自然'
    },
    cool: {
      corePersonality: '冷静克制、神秘独立',
      languageStyle: '简洁、冷静、言简意赅'
    }
  }
  const profile = profiles[type] || profiles.gentle
  return {
    name: `${avatarName}的人格`,
    templateType: 1,
    corePersonality: profile.corePersonality,
    identity: 'AI虚拟伴侣',
    languageStyle: profile.languageStyle,
    hobbies: '陪伴、交流',
    relationship: '朋友'
  }
}
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

.preview-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.4);

  p {
    margin-top: 16px;
    font-size: 14px;
  }
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

.mode-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: -12px;
}

.mode-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  border-radius: 16px 16px 0 0;
  background: #e8eaed;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  color: #606266;
  transition: all 0.25s;

  &:hover {
    background: #dcdfe6;
  }

  &.active {
    background: #fff;
    color: #667eea;
    box-shadow: 0 -2px 8px rgba(102, 126, 234, 0.15);
  }
}

.config-card {
  background: #fff;
  border-radius: 0 20px 20px 20px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.config-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.ai-generate-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-textarea {
  :deep(.el-textarea__inner) {
    border-radius: 12px;
    border: 2px solid #e4e7ed;
    transition: all 0.25s;

    &:focus {
      border-color: #667eea;
    }
  }
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.quick-tags-label {
  font-size: 13px;
  color: #909399;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #667eea;
    border-color: #667eea;
  }
}

.generate-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;

  &:hover:not(:disabled) {
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  }
}

.result-panel {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.parse-source-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 16px;
  color: #b26a00;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  font-size: 13px;
}

.result-section-title {
  margin: 0 0 10px;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
}

.result-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 10px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.attributes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.model-match-panel {
  padding: 12px;
  margin-bottom: 16px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 10px;

  &.matched {
    background: #f0f9eb;
    border-color: #e1f3d8;
  }

  &.nearest {
    background: #fdf6ec;
    border-color: #faecd8;
  }
}

.model-match-message {
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.model-file {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.unmatched-attributes {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
}

.unmatched-label {
  color: #606266;
  font-size: 12px;
}

.attr-tag {
  padding: 4px 10px;
  background: linear-gradient(135deg, #667eea20 0%, #764ba220 100%);
  color: #667eea;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.result-actions {
  display: flex;
  gap: 12px;
}

.save-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 16px;
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
