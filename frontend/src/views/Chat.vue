<template>
  <div class="chat-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo" @click="goHome">
          <el-icon :size="24" color="#fff"><MagicStick /></el-icon>
          <span>AI Companion</span>
        </div>
        <el-tooltip content="新建对话">
          <el-button
            circle
            class="new-chat-btn"
            :icon="Plus"
            @click="showAvatarDialog = true"
          />
        </el-tooltip>
      </div>

      <div class="chat-list">
        <div class="chat-list-header">
          <span>最近对话</span>
          <span class="session-count">{{ chatStore.sessions.length }}</span>
        </div>

        <div v-if="chatStore.loading && chatStore.sessions.length === 0" class="loading-state">
          <el-icon :size="32" class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-else-if="chatStore.sessions.length === 0" class="empty-state">
          <el-icon :size="48" color="rgba(255,255,255,0.3)"><ChatLineRound /></el-icon>
          <p>暂无对话</p>
          <span>点击上方 + 开始新对话</span>
        </div>

        <div v-else class="chat-items">
          <div
            v-for="session in chatStore.sessions"
            :key="session.id"
            class="chat-item"
            :class="{ active: session.id === chatStore.currentSessionId }"
            @click="handleSelectSession(session)"
          >
            <div class="chat-avatar">
              <el-avatar :size="40" :style="{ background: getAvatarColor(session.avatarName) }">
                {{ (session.avatarName || '?').charAt(0) }}
              </el-avatar>
            </div>
            <div class="chat-info">
              <span class="chat-name">{{ session.title || session.avatarName }}</span>
              <span class="chat-preview">{{ session.avatarName ? '与 ' + session.avatarName + ' 的对话' : '对话' }}</span>
            </div>
            <el-icon
              class="delete-icon"
              :size="14"
              @click.stop="handleDeleteSession(session)"
            >
              <Close />
            </el-icon>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <el-avatar :size="36" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          {{ userStore.nickname.charAt(0).toUpperCase() }}
        </el-avatar>
        <div class="user-info">
          <span class="user-name">{{ userStore.nickname }}</span>
          <span class="user-status">在线</span>
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <template v-if="chatStore.currentSession">
        <header class="chat-header">
          <div class="chat-header-left">
            <el-avatar
              :size="44"
              :style="{ background: getAvatarColor(currentSessionAvatarName) }"
            >
              {{ (currentSessionAvatarName || '?').charAt(0) }}
            </el-avatar>
            <div class="chat-header-info">
              <h3>{{ chatStore.currentSession.title || currentSessionAvatarName }}</h3>
              <span class="status-online">
                <span class="status-dot"></span>
                {{ chatStore.streaming ? '正在回复...' : '在线' }}
              </span>
            </div>
          </div>
          <div class="chat-header-actions">
            <el-tooltip content="返回首页">
              <el-button circle :icon="HomeFilled" @click="goHome" />
            </el-tooltip>
            <el-tooltip content="删除对话">
              <el-button circle :icon="Delete" @click="handleDeleteCurrentSession" />
            </el-tooltip>
          </div>
        </header>

        <div class="chat-avatar-area">
          <div class="avatar-stage">
            <AvatarRenderer
              v-if="currentAvatarModelUrl"
              :key="`${sessionAvatar.id}:${currentAvatarModelUrl}`"
              :model-url="currentAvatarModelUrl"
              :behavior-state="avatarBehaviorState"
              @loaded="onAvatarLoaded"
              @error="onAvatarError"
            />
            <div v-else-if="sessionAvatarLoading" class="avatar-model-state">
              <el-icon class="is-loading" :size="24"><Loading /></el-icon>
              <span>正在加载会话形象...</span>
            </div>
            <div v-else-if="sessionAvatarError" class="avatar-model-state avatar-model-error">
              <el-icon :size="24"><Warning /></el-icon>
              <span>{{ sessionAvatarError }}</span>
            </div>
            <div v-else class="avatar-model-state">
              <el-icon :size="24"><Warning /></el-icon>
              <span>当前会话没有可加载的3D模型</span>
            </div>
          </div>
        </div>

        <div class="chat-messages" ref="messagesRef">
          <div
            v-for="msg in chatStore.messages"
            :key="msg.id"
            class="message-item"
            :class="msg.role"
          >
            <div class="message-avatar">
              <el-avatar
                v-if="msg.role === 'assistant'"
                :size="36"
                :style="{ background: getAvatarColor(currentSessionAvatarName) }"
              >
                {{ (currentSessionAvatarName || '?').charAt(0) }}
              </el-avatar>
              <el-avatar
                v-else
                :size="36"
                style="background: linear-gradient(135deg, #667eea, #764ba2)"
              >
                {{ userStore.nickname.charAt(0).toUpperCase() }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-bubble" :class="{ streaming: msg.streaming }">
                {{ msg.content || emptyMessageText(msg) }}
                <span v-if="msg.streaming && msg.content" class="cursor-blink">▊</span>
              </div>
              <div class="message-meta">
                <span class="message-time">{{ msg.time }}</span>
                <el-tag
                  v-if="msg.role === 'assistant' && msg.status !== CHAT_MESSAGE_STATUS.COMPLETED"
                  :type="messageStatusMeta(msg.status).type"
                  size="small"
                  effect="plain"
                >
                  {{ messageStatusMeta(msg.status).label }}
                </el-tag>
                <el-button
                  v-if="isMessageRetryable(msg)"
                  link
                  type="primary"
                  size="small"
                  :disabled="chatStore.streaming"
                  @click="handleRetry(msg)"
                >
                  重新发送
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="chatStore.messages.length === 0" class="no-messages">
            <el-icon :size="48" color="rgba(0,0,0,0.2)"><ChatDotRound /></el-icon>
            <p>开始与 {{ currentSessionAvatarName }} 对话吧！</p>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="input-tools">
            <el-tooltip content="表情（开发中）">
              <el-button circle :icon="Avatar" disabled />
            </el-tooltip>
            <el-tooltip content="图片（开发中）">
              <el-button circle :icon="Picture" disabled />
            </el-tooltip>
            <el-tooltip content="语音（开发中）">
              <el-button circle :icon="Microphone" disabled />
            </el-tooltip>
          </div>
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              :placeholder="chatStore.streaming ? 'AI正在回复中，请稍候...' : '输入消息，Enter 发送，Shift+Enter 换行'"
              resize="none"
              :disabled="chatStore.streaming"
              @keydown.enter.exact.prevent="handleSend"
            />
            <el-button
              v-if="chatStore.streaming"
              type="danger"
              circle
              class="send-btn"
              :icon="VideoPause"
              title="停止生成"
              @click="handleStop"
            />
            <el-button
              v-else
              type="primary"
              circle
              class="send-btn"
              :icon="Promotion"
              :disabled="!inputMessage.trim() || chatStore.streaming"
              @click="handleSend"
            />
          </div>
          <div v-if="chatStore.error" class="error-tip">
            <el-icon><Warning /></el-icon>
            <span>{{ chatStore.error }}</span>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="no-session">
          <div class="no-session-content">
            <el-icon :size="80" color="rgba(102,126,234,0.6)"><ChatDotRound /></el-icon>
            <h2>开始一段新对话</h2>
            <p>选择一个AI形象，开启你的虚拟陪伴之旅</p>
            <el-button type="primary" size="large" @click="showAvatarDialog = true">
              <el-icon style="margin-right: 8px"><Plus /></el-icon>
              新建对话
            </el-button>
          </div>
        </div>
      </template>
    </main>

    <el-dialog
      v-model="showAvatarDialog"
      title="选择AI形象"
      width="560px"
      :close-on-click-modal="true"
      class="avatar-dialog"
    >
      <div class="avatar-grid">
        <div
          v-for="avatar in avatarList"
          :key="avatar.id"
          class="avatar-card"
          :class="{ selected: tempAvatarId === avatar.id }"
          @click="tempAvatarId = avatar.id"
        >
          <div class="avatar-card-preview" :style="{ background: getAvatarColor(avatar.name) }">
            {{ avatar.name.charAt(0) }}
          </div>
          <div class="avatar-card-info">
            <h4>{{ avatar.name }}</h4>
            <p class="avatar-slogan">{{ avatar.slogan || '暂无个性签名' }}</p>
            <span class="avatar-type">{{ getTypeLabel(avatar.type) }}</span>
          </div>
        </div>

        <div v-if="avatarList.length === 0" class="no-avatars">
          <el-icon :size="48" color="rgba(0,0,0,0.2)"><User /></el-icon>
          <p>你还没有创建任何AI形象</p>
          <el-button type="primary" @click="goToCreateAvatar">去创建</el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!tempAvatarId"
          @click="handleCreateSession"
        >
          开始对话
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick,
  Plus,
  User,
  Delete,
  Avatar,
  Picture,
  Microphone,
  Promotion,
  Loading,
  Close,
  ChatLineRound,
  ChatDotRound,
  HomeFilled,
  Warning,
  VideoPause
} from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chat'
import { useAvatarStore } from '../stores/avatar'
import { useUserStore } from '../stores/user'
import AvatarRenderer from '../components/AvatarRenderer.vue'
import { getAvatarById } from '../api/avatar'
import { resolveChatAvatarBehaviorState } from '../avatar/chatAvatarBehaviorState'
import {
  CHAT_MESSAGE_STATUS,
  isMessageRetryable,
  messageStatusMeta
} from '../utils/chatMessageState'

const router = useRouter()
const chatStore = useChatStore()
const avatarStore = useAvatarStore()
const userStore = useUserStore()

const messagesRef = ref(null)
const inputMessage = ref('')
const showAvatarDialog = ref(false)
const tempAvatarId = ref(null)
const sessionAvatar = ref(null)
const sessionAvatarLoading = ref(false)
const sessionAvatarError = ref('')
const sessionAvatarCache = new Map()
let sessionAvatarRequestVersion = 0

const avatarList = computed(() => avatarStore.avatarList)

const currentSessionAvatarName = computed(() => {
  if (!chatStore.currentSession) return ''
  const session = chatStore.currentSession
  return session.avatarName || session.title || 'AI'
})

const currentAvatarModelUrl = computed(() => {
  return sessionAvatar.value?.modelUrl?.trim() || ''
})

const avatarBehaviorState = computed(() => {
  return resolveChatAvatarBehaviorState(chatStore.messages)
})

const loadSessionAvatar = async (avatarId) => {
  const requestVersion = ++sessionAvatarRequestVersion
  sessionAvatarError.value = ''

  if (!avatarId) {
    sessionAvatar.value = null
    sessionAvatarLoading.value = false
    return
  }

  const cacheKey = String(avatarId)
  const cachedAvatar = sessionAvatarCache.get(cacheKey)
  if (cachedAvatar) {
    sessionAvatar.value = cachedAvatar
    sessionAvatarLoading.value = false
    return
  }

  sessionAvatar.value = null
  sessionAvatarLoading.value = true
  try {
    const res = await getAvatarById(avatarId)
    const avatar = res.data || res
    if (
      requestVersion !== sessionAvatarRequestVersion
      || String(chatStore.currentSession?.avatarId ?? '') !== cacheKey
    ) return

    sessionAvatarCache.set(cacheKey, avatar)
    sessionAvatar.value = avatar
  } catch (err) {
    if (requestVersion !== sessionAvatarRequestVersion) return
    sessionAvatar.value = null
    sessionAvatarError.value = err.message || '会话3D形象加载失败'
  } finally {
    if (requestVersion === sessionAvatarRequestVersion) {
      sessionAvatarLoading.value = false
    }
  }
}

const colorPalette = [
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #43e97b)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #a8edea, #fed6e3)',
  'linear-gradient(135deg, #ff9a9e, #fecfef)',
  'linear-gradient(135deg, #5ee7df, #b490ca)',
  'linear-gradient(135deg, #c471f5, #fa71cd)'
]

const getAvatarColor = (name) => {
  if (!name) return colorPalette[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  const index = Math.abs(hash) % colorPalette.length
  return colorPalette[index]
}

const getTypeLabel = (type) => {
  const map = { 1: '人类', 2: '动物', 3: '幻想', 4: '其他' }
  return map[type] || '未知'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(
  () => chatStore.messages.length,
  () => scrollToBottom()
)

watch(
  () => chatStore.messages,
  () => scrollToBottom(),
  { deep: true }
)

watch(
  () => chatStore.currentSession?.avatarId ?? null,
  (avatarId) => loadSessionAvatar(avatarId),
  { immediate: true }
)

onMounted(async () => {
  if (avatarStore.avatarList.length === 0) {
    await avatarStore.fetchAvatarList()
  }
  await chatStore.fetchSessions()
})

onBeforeUnmount(() => {
  sessionAvatarRequestVersion += 1
  chatStore.stopGeneration()
})

const goHome = () => {
  router.push('/home')
}

const goToCreateAvatar = () => {
  showAvatarDialog.value = false
  router.push('/avatar/create')
}

const handleSelectSession = async (session) => {
  if (session.id === chatStore.currentSessionId) return
  await chatStore.selectSession(session.id)
}

const handleDeleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除与 ${session.avatarName || session.title} 的对话吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await chatStore.deleteSession(session.id)
    ElMessage.success('对话已删除')
  } catch (e) {
    // cancelled
  }
}

const handleDeleteCurrentSession = async () => {
  if (!chatStore.currentSessionId) return
  try {
    await ElMessageBox.confirm('确定要删除当前对话吗？', '删除确认', {
      type: 'warning'
    })
    await chatStore.deleteSession(chatStore.currentSessionId)
    ElMessage.success('对话已删除')
  } catch (e) {
    // cancelled
  }
}

const handleCreateSession = async () => {
  if (!tempAvatarId.value) return
  try {
    const avatar = avatarStore.avatarList.find((a) => a.id === tempAvatarId.value)
    const session = await chatStore.createSession(
      tempAvatarId.value,
      avatar ? avatar.name : '新对话'
    )
    showAvatarDialog.value = false
    tempAvatarId.value = null
    ElMessage.success('对话已创建')
  } catch (err) {
    ElMessage.error(err.message || '创建对话失败')
  }
}

const handleSend = async () => {
  const text = inputMessage.value.trim()
  if (!text || chatStore.streaming) return

  if (!chatStore.currentSessionId) {
    ElMessage.warning('请先选择或创建一个对话')
    return
  }

  inputMessage.value = ''
  await chatStore.sendMessage(text)
}

const handleStop = () => {
  if (chatStore.stopGeneration()) ElMessage.info('已停止生成')
}

const handleRetry = async (message) => {
  await chatStore.retryMessage(message)
}

const emptyMessageText = (message) => {
  if (message.status === CHAT_MESSAGE_STATUS.PENDING) return '等待 AI 回复...'
  if (message.status === CHAT_MESSAGE_STATUS.STREAMING) return '思考中...'
  if (message.status === CHAT_MESSAGE_STATUS.FAILED) return '本次回复失败'
  if (message.status === CHAT_MESSAGE_STATUS.CANCELLED) return '本次回复已停止'
  if (message.status === CHAT_MESSAGE_STATUS.INTERRUPTED) return '回复因服务中断未完成'
  return ''
}

const onAvatarLoaded = () => {
  // 3D avatar loaded successfully
}

const onAvatarError = (err) => {
  console.warn('3D avatar load error:', err)
}
</script>

<style lang="scss" scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  overflow: hidden;
}

.sidebar {
  width: 280px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}

.new-chat-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
}

.chat-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;

  .session-count {
    background: rgba(255, 255, 255, 0.1);
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 11px;
  }
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.4);
  gap: 10px;

  p {
    font-size: 14px;
    margin: 0;
  }

  span {
    font-size: 12px;
  }
}

.chat-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
  }

  &.active {
    background: rgba(255, 255, 255, 0.15);
    box-shadow: 0 0 20px rgba(102, 126, 234, 0.3);
  }

  .chat-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .chat-name {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .chat-preview {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .delete-icon {
    opacity: 0;
    color: rgba(255, 255, 255, 0.5);
    transition: all 0.2s;

    &:hover {
      color: #ff6b6b;
    }
  }

  &:hover .delete-icon {
    opacity: 1;
  }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 12px;

  .user-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .user-name {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
  }

  .user-status {
    font-size: 12px;
    color: #67c23a;
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #ebeef5;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chat-header-info {
  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 2px;
  }
}

.status-online {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #67c23a;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.chat-header-actions {
  display: flex;
  gap: 10px;
}

.chat-avatar-area {
  padding: 16px 24px 0;
}

.avatar-stage {
  height: 140px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(
      circle at center,
      rgba(102, 126, 234, 0.2) 0%,
      transparent 70%
    );
  }
}

.avatar-model-state {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.avatar-model-error {
  color: #ffb4c0;
}

.chat-messages {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 70%;

  &.user {
    align-self: flex-end;
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-bubble {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
    }
  }

  &.assistant {
    align-self: flex-start;

    .message-bubble {
      background: #fff;
      color: #303133;
      border: 1px solid #ebeef5;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;

  &.streaming {
    background: linear-gradient(135deg, #f0f4ff, #e8edff);
    border: 1px dashed #667eea;
  }
}

.cursor-blink {
  animation: blink 1s step-end infinite;
  color: #667eea;
  margin-left: 2px;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
  padding: 0 4px;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 20px;
}

.no-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(0, 0, 0, 0.3);

  p {
    margin-top: 12px;
    font-size: 14px;
  }
}

.chat-input-area {
  padding: 16px 24px 24px;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;

  :deep(.el-textarea__inner) {
    border-radius: 12px;
    padding: 12px 16px;
    font-size: 14px;
  }
}

.send-btn {
  height: 40px;
  width: 40px;
  flex-shrink: 0;
}

.error-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 8px;
  font-size: 12px;
  color: #f56c6c;
}

.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  .no-session-content {
    text-align: center;
    padding: 60px;

    h2 {
      margin: 16px 0 8px;
      color: #303133;
    }

    p {
      color: #909399;
      margin-bottom: 24px;
    }
  }
}

.avatar-dialog {
  :deep(.el-dialog__body) {
    padding: 10px 20px;
  }
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  max-height: 420px;
  overflow-y: auto;
  padding: 10px 0;
}

.avatar-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #c4d0ff;
    background: #f5f7ff;
  }

  &.selected {
    border-color: #667eea;
    background: #f0f4ff;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  }
}

.avatar-card-preview {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.avatar-card-info {
  flex: 1;
  min-width: 0;

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 4px;
  }
}

.avatar-slogan {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.avatar-type {
  font-size: 11px;
  color: #667eea;
  background: #eef1ff;
  padding: 2px 8px;
  border-radius: 10px;
}

.no-avatars {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: rgba(0, 0, 0, 0.4);

  p {
    margin: 12px 0 16px;
  }
}
</style>
