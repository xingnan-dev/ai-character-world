<template>
  <AppShell class="chat-page">
    <div class="chat-layout">
      <aside class="session-sidebar app-surface--glass" aria-label="对话会话">
        <ChatSessionList :sessions="chatStore.sessions" :current-session-id="chatStore.currentSessionId" :loading="chatStore.loading" :error="chatStore.error || ''" :user="userStore.userInfo" @select="handleSelectSession" @delete="handleDeleteSession" @create="openNewChat" @reload="chatStore.fetchSessions()" />
      </aside>

      <main class="chat-main" aria-label="聊天工作区">
        <header class="chat-header app-surface--glass">
          <div class="chat-header-left">
            <el-button class="sessions-toggle" circle :icon="Menu" aria-label="打开会话列表" :aria-expanded="showSessions" @click="showSessions = true" />
            <ChatSessionAvatar v-if="chatStore.currentSession" :session="chatStore.currentSession" :user="userStore.userInfo" :size="40" />
            <div class="chat-header-info">
              <span class="workspace-eyebrow">CHAT SPACE</span>
              <h1>{{ chatStore.currentSession ? (chatStore.currentSession.title || currentSessionAvatarName) : '对话空间' }}</h1>
              <span v-if="chatStore.currentSession" class="status-online" role="status">
                <span class="status-dot" :class="{ responding: chatStore.streaming }"></span>
                {{ chatStore.streaming ? '正在回复…' : recovering ? '正在同步回复状态' : '准备好继续交流' }}
              </span>
            </div>
          </div>
          <div v-if="chatStore.currentSession" class="chat-header-actions">
            <el-button class="character-toggle" :icon="User" @click="showCharacter = true">角色资料</el-button>
            <el-tooltip content="删除当前对话"><el-button circle :icon="Delete" aria-label="删除当前对话" @click="handleDeleteCurrentSession" /></el-tooltip>
          </div>
        </header>

        <div v-if="chatStore.currentSession" class="chat-workspace">
          <section class="conversation-panel app-surface--elevated" aria-label="当前对话">
            <div class="chat-messages" ref="messagesRef" tabindex="0" role="region" aria-label="对话消息">
              <el-alert v-if="recovering" :title="recoveryExpired ? '回复状态尚未确认，请刷新状态后再发送。' : '实时连接已断开，正在同步回复状态…'" type="info" :closable="false">
                <el-button link @click="chatStore.fetchMessages(chatStore.currentSessionId)">刷新状态</el-button>
              </el-alert>
              <div v-for="msg in chatStore.messages" :key="msg.id" class="message-item" :class="msg.role">
                <div class="message-avatar"><ChatSessionAvatar :session="chatStore.currentSession" :role="msg.role" :user="userStore.userInfo" :size="32" /></div>
                <div class="message-content">
                  <div class="message-bubble" :class="{ streaming: msg.streaming, 'is-failed': msg.status === CHAT_MESSAGE_STATUS.FAILED }">
                    {{ msg.content || emptyMessageText(msg) }}
                    <span v-if="msg.streaming && msg.content" class="cursor-blink" aria-hidden="true">▊</span>
                  </div>
                  <div class="message-meta">
                    <span class="message-time">{{ msg.time }}</span>
                    <el-tag v-if="msg.role === 'assistant' && msg.status !== CHAT_MESSAGE_STATUS.COMPLETED" :type="messageStatusMeta(msg.status).type" size="small" effect="plain">{{ messageStatusMeta(msg.status).label }}</el-tag>
                    <el-button v-if="isMessageRetryable(msg)" link type="primary" size="small" :disabled="chatStore.streaming" @click="handleRetry(msg)">重新发送</el-button>
                  </div>
                </div>
              </div>
              <div v-if="chatStore.messages.length === 0" class="no-messages">
                <span class="empty-symbol"><el-icon :size="28"><ChatDotRound /></el-icon></span>
                <h2>和 {{ currentSessionAvatarName }} 打个招呼</h2><p>从一个问题或今天的心情开始。</p>
              </div>
            </div>

            <div class="chat-input-area">
              <div class="input-tools">
                <el-tooltip content="表情（开发中）"><el-button circle :icon="Avatar" aria-label="表情（开发中）" disabled /></el-tooltip>
                <el-tooltip content="图片（开发中）"><el-button circle :icon="Picture" aria-label="图片（开发中）" disabled /></el-tooltip>
                <el-tooltip content="语音（开发中）"><el-button circle :icon="Microphone" aria-label="语音（开发中）" disabled /></el-tooltip>
                <span class="keyboard-hint">Enter 发送 · Shift+Enter 换行</span>
              </div>
              <div class="input-wrapper">
                <el-input v-model="inputMessage" type="textarea" :rows="2" aria-label="聊天消息" :placeholder="chatStore.streaming ? 'AI正在回复中，请稍候...' : '输入消息，开始交流…'" resize="none" :disabled="chatStore.streaming" @keydown.enter.exact.prevent="handleSend" />
                <el-button v-if="chatStore.streaming" type="danger" circle class="send-btn" :icon="VideoPause" title="停止生成" aria-label="停止生成" @click="handleStop" />
                <el-button v-else type="primary" circle class="send-btn" :icon="Promotion" aria-label="发送消息" :disabled="!inputMessage.trim() || chatStore.streaming || recovering" @click="handleSend" />
              </div>
              <div v-if="chatStore.error" class="error-tip" role="alert"><el-icon><Warning /></el-icon><span>{{ chatStore.error }}</span></div>
            </div>
          </section>
          <ChatCharacterPanel class="desktop-character" :session="chatStore.currentSession" :user="userStore.userInfo" :streaming="chatStore.streaming" />
        </div>

        <section v-else class="no-session app-surface--glass">
          <span class="empty-symbol"><el-icon :size="36"><ChatDotRound /></el-icon></span>
          <span class="workspace-eyebrow">A LITTLE CONVERSATION, A LITTLE CONNECTION</span>
          <h2>故事，从一句问候开始</h2>
          <p>选择一位 AI Character，开启专属于你们的对话。</p>
          <el-button type="primary" size="large" :icon="Plus" @click="openNewChat">新建对话</el-button>
          <span class="empty-note">也可以从会话列表继续之前的交流</span>
        </section>
      </main>
    </div>

    <el-drawer v-model="showSessions" title="会话列表" direction="ltr" size="min(88vw, 340px)" append-to-body class="app-v2-theme chat-sessions-drawer">
      <ChatSessionList :sessions="chatStore.sessions" :current-session-id="chatStore.currentSessionId" :loading="chatStore.loading" :error="chatStore.error || ''" :user="userStore.userInfo" @select="handleSelectSession" @delete="handleDeleteSession" @create="openNewChat" @reload="chatStore.fetchSessions()" />
    </el-drawer>
    <el-drawer v-model="showCharacter" title="会话角色资料" size="min(92vw, 380px)" append-to-body class="app-v2-theme chat-character-drawer">
      <ChatCharacterPanel :session="chatStore.currentSession" :user="userStore.userInfo" :streaming="chatStore.streaming" />
    </el-drawer>

    <el-dialog v-model="showAvatarDialog" title="选择 AI Character" width="min(560px, calc(100vw - 32px))" :close-on-click-modal="true" append-to-body class="app-v2-theme avatar-dialog">
      <div class="avatar-grid">
        <button v-for="avatar in aiCharacters" :key="avatar.id" type="button" class="avatar-card" :class="{ selected: tempAvatarId === avatar.id }" :aria-pressed="tempAvatarId === avatar.id" @click="tempAvatarId = avatar.id">
          <SimpleAvatar :name="avatar.name" :image-url="avatar.imageUrl" :avatar-color="avatar.avatarColor" :entity-key="avatar.id" type="AI" :size="56" shape="circle" />
          <span class="avatar-card-info"><strong>{{ avatar.name }}</strong><span class="avatar-slogan">{{ avatar.identity || avatar.corePersonality || '暂无角色简介' }}</span><span class="avatar-type">AI Character</span></span>
        </button>
        <div v-if="aiCharacters.length === 0" class="no-avatars"><el-icon :size="40"><User /></el-icon><p>你还没有创建 AI Character</p><el-button type="primary" @click="goToCreateCharacter">去创建</el-button></div>
      </div>
      <template #footer><el-button @click="showAvatarDialog = false">取消</el-button><el-button type="primary" :disabled="!tempAvatarId" @click="handleCreateSession">开始对话</el-button></template>
    </el-dialog>
  </AppShell>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Menu,
  User,
  Delete,
  Avatar,
  Picture,
  Microphone,
  Promotion,
  ChatDotRound,
  Warning,
  VideoPause
} from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chat'
import { useUserStore } from '../stores/user'
import ChatSessionAvatar from '../components/chat/ChatSessionAvatar.vue'
import ChatCharacterPanel from '../components/chat/ChatCharacterPanel.vue'
import ChatSessionList from '../components/chat/ChatSessionList.vue'
import AppShell from '../components/layout/AppShell.vue'
import SimpleAvatar from '../components/ui/SimpleAvatar.vue'
import { getCharacterList } from '../api/character'
import {
  CHAT_MESSAGE_STATUS,
  isMessageActive,
  isMessageRetryable,
  messageStatusMeta
} from '../utils/chatMessageState'

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const messagesRef = ref(null)
const inputMessage = ref('')
const showAvatarDialog = ref(false)
const showSessions = ref(false)
const showCharacter = ref(false)
const openNewChat = () => { showSessions.value = false; showAvatarDialog.value = true }
const tempAvatarId = ref(null)
const aiCharacters = ref([])
const recoveryExpired = ref(false)
const recovering = computed(() => !chatStore.streaming && chatStore.messages.some(message => isMessageActive(message.status)))
let recoveryTimer
watch(() => `${chatStore.currentSessionId}:${recovering.value}`, (_, __, onCleanup) => {
  let disposed = false
  const sessionId = chatStore.currentSessionId
  const deadline = Date.now() + 150000
  recoveryExpired.value = false
  const poll = async () => {
    if (disposed || !recovering.value || chatStore.currentSessionId !== sessionId) return
    if (Date.now() >= deadline) { recoveryExpired.value = true; return }
    await chatStore.fetchMessages(sessionId)
    if (!disposed) recoveryTimer = setTimeout(poll, 2000)
  }
  if (recovering.value && sessionId) recoveryTimer = setTimeout(poll, 1000)
  onCleanup(() => { disposed = true; clearTimeout(recoveryTimer) })
})


const currentSessionAvatarName = computed(() => {
  if (!chatStore.currentSession) return ''
  const session = chatStore.currentSession
  return session.characterName || session.avatarName || session.title || 'AI'
})

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

onMounted(async () => {
  const characterResponse = await getCharacterList('AI')
  aiCharacters.value = characterResponse.data || characterResponse || []
  await chatStore.fetchSessions()
})

onBeforeUnmount(() => {
  chatStore.stopGeneration()
})

const goToCreateCharacter = () => { showAvatarDialog.value = false; router.push('/character/create?type=AI') }

const handleSelectSession = async (session) => {
  showSessions.value = false
  if (session.id === chatStore.currentSessionId) return
  await chatStore.selectSession(session.id)
}

const handleDeleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除与 ${session.avatarName || session.title} 的对话吗？`,
      '删除确认',
      { type: 'warning', customClass: 'app-v2-theme chat-confirm' }
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
      type: 'warning', customClass: 'app-v2-theme chat-confirm'
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
    const avatar = aiCharacters.value.find((a) => a.id === tempAvatarId.value)
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
  if (!text || chatStore.streaming || recovering.value) return

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

</script>

<style lang="scss" scoped>
.chat-page.app-shell{height:100dvh;min-height:0;overflow:hidden}
.chat-page.app-shell :deep(.app-shell__content){flex:1 1 0;min-height:0;overflow:hidden}
.chat-page .chat-layout{width:100%;min-width:0;min-height:0;flex:1 1 0;display:flex;gap:20px;padding:20px;overflow:hidden;color:var(--app-text-primary)}
.chat-page :deep(.el-button){min-height:44px;margin-left:0;border-radius:var(--app-radius-sm);font-weight:700}
.chat-page :deep(.el-button--primary){--el-button-bg-color:var(--app-accent-primary);--el-button-border-color:var(--app-accent-primary);--el-button-hover-bg-color:var(--app-accent-primary-strong);--el-button-hover-border-color:var(--app-accent-primary-strong);--el-button-active-bg-color:var(--app-accent-primary-strong);--el-button-active-border-color:var(--app-accent-primary-strong);--el-button-disabled-bg-color:var(--app-lavender-soft);--el-button-disabled-border-color:var(--app-border);--el-button-disabled-text-color:var(--app-text-secondary)}
.chat-page :deep(.el-button--primary.is-link){color:var(--app-accent-primary-strong)}
.session-sidebar{flex:0 0 248px;min-width:0;min-height:0;overflow:hidden}
.chat-main{min-width:0;min-height:0;flex:1;display:flex;flex-direction:column;gap:16px}
.chat-header{flex:none;min-width:0;min-height:86px;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px 20px}
.chat-header-left{min-width:0;display:flex;align-items:center;gap:12px}.chat-header-info{min-width:0}.workspace-eyebrow{display:block;color:var(--app-accent-primary-strong);font-size:10px;font-weight:800;letter-spacing:1.4px}.chat-header-info h1{margin:4px 0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--app-text-primary);font-size:18px;font-weight:750;letter-spacing:-.02em}
.status-online{display:flex;align-items:center;gap:6px;color:var(--app-text-secondary);font-size:12px}.status-dot{width:6px;height:6px;border-radius:50%;background:var(--app-success)}.status-dot.responding{background:var(--app-accent-primary);animation:chat-pulse 1.5s ease-in-out infinite}
.chat-header-actions{flex:none;display:flex;align-items:center;gap:8px}.chat-header-actions :deep(.el-button){color:var(--app-text-secondary);background:var(--app-surface);border-color:var(--app-border-strong)}.chat-header-actions :deep(.el-button:hover){color:var(--app-accent-primary-strong);border-color:var(--app-accent-primary)}
.sessions-toggle,.character-toggle{display:none}
.chat-workspace{min-width:0;min-height:0;flex:1;display:grid;grid-template-columns:minmax(0,1fr) 272px;gap:16px}
.conversation-panel{min-width:0;min-height:0;display:flex;flex-direction:column;overflow:hidden}
.chat-messages{min-height:0;flex:1;overflow-y:auto;overscroll-behavior:contain;scrollbar-gutter:stable;padding:28px clamp(16px,2vw,32px);display:flex;flex-direction:column;gap:22px;background:linear-gradient(180deg,var(--app-surface-glass),var(--app-surface-inset))}
.chat-messages:focus-visible{outline:2px solid var(--app-accent-primary);outline-offset:-3px}
.message-item{min-width:0;flex:none;display:flex;gap:10px;width:fit-content;max-width:min(92%,760px)}
.message-item.user{align-self:flex-end;flex-direction:row-reverse}.message-item.user .message-content{align-items:flex-end}.message-item.user .message-bubble{color:var(--app-text-inverse);background:var(--app-accent-primary);border-color:var(--app-accent-primary);border-bottom-right-radius:4px}
.message-item.assistant{align-self:flex-start}.message-item.assistant .message-bubble{border-bottom-left-radius:4px}
.message-avatar{flex:none;padding-top:4px}.message-content{min-width:0;display:flex;flex-direction:column;gap:6px}
.message-bubble{min-width:0;padding:13px 17px;border:1px solid var(--app-border);border-radius:var(--app-radius-md);background:var(--app-surface);color:var(--app-text-primary);font-size:var(--app-type-body);line-height:1.75;white-space:pre-wrap;overflow-wrap:anywhere;box-shadow:var(--app-shadow-sm)}
.message-bubble.streaming{border-color:var(--app-accent-primary);background:var(--app-lavender-soft)}.message-bubble.is-failed{border-color:var(--app-danger);background:var(--app-danger-soft);color:var(--app-danger)}
.cursor-blink{margin-left:2px;color:var(--app-accent-primary);animation:chat-blink 1s step-end infinite}
.message-meta{display:flex;align-items:center;flex-wrap:wrap;gap:8px;min-height:20px}.message-time{padding:0 3px;color:var(--app-text-secondary);font-size:11px}.message-meta :deep(.el-button){min-height:28px;font-size:12px}.message-meta :deep(.el-tag){border-radius:var(--app-radius-pill)}
.message-meta :deep(.el-tag--danger){color:var(--app-danger);border-color:var(--app-danger);background:var(--app-danger-soft)}
.chat-input-area{flex:none;padding:12px 18px 18px;border-top:1px solid var(--app-border);background:var(--app-surface-elevated)}
.input-tools{display:flex;align-items:center;gap:6px;margin-bottom:10px}.input-tools :deep(.el-button){width:30px;height:30px;min-height:30px;border:0;background:var(--app-surface-inset);color:var(--app-text-secondary)}.keyboard-hint{margin-left:auto;color:var(--app-text-secondary);font-size:11px}
.input-wrapper{min-width:0;display:flex;align-items:flex-end;gap:12px}.input-wrapper :deep(.el-textarea){min-width:0;flex:1}.input-wrapper :deep(.el-textarea__inner){min-height:76px!important;padding:12px 14px;border-radius:var(--app-radius-sm);background:var(--app-surface-inset);color:var(--app-text-primary);font-size:15px;line-height:1.6;box-shadow:0 0 0 1px var(--app-border-strong) inset}.input-wrapper :deep(.el-textarea__inner::placeholder){color:var(--app-text-secondary)}.input-wrapper :deep(.el-textarea__inner:focus){box-shadow:0 0 0 2px var(--app-accent-primary) inset,var(--app-focus-ring)}.input-wrapper :deep(.el-textarea.is-disabled .el-textarea__inner){background:var(--app-surface-inset);color:var(--app-text-secondary)}
.send-btn.el-button--danger{--el-button-bg-color:var(--app-danger);--el-button-border-color:var(--app-danger);--el-button-hover-bg-color:var(--app-danger);--el-button-hover-border-color:var(--app-danger)}
.send-btn{flex:none;width:44px;height:44px;border-radius:var(--app-radius-sm)!important}.send-btn:focus-visible{outline:2px solid var(--app-accent-primary);outline-offset:3px;box-shadow:var(--app-focus-ring)}
.error-tip{display:flex;align-items:flex-start;gap:8px;margin-top:12px;padding:10px 12px;border:1px solid var(--app-danger);border-radius:var(--app-radius-sm);background:var(--app-danger-soft);color:var(--app-danger);font-size:13px;line-height:1.5;overflow-wrap:anywhere}.error-tip .el-icon{flex:none;margin-top:2px}
.no-session{min-width:0;min-height:0;flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;padding:32px;text-align:center;overflow:auto}.no-session h2{max-width:100%;color:var(--app-text-primary);font-size:clamp(24px,3vw,34px);letter-spacing:-.03em;overflow-wrap:anywhere}.no-session p{color:var(--app-text-secondary);line-height:1.7}.no-session .workspace-eyebrow{max-width:100%;line-height:1.7;font-size:10px}.no-session :deep(.el-button){margin-top:8px}.empty-note{color:var(--app-text-secondary);font-size:12px}
.empty-symbol{flex:none;width:72px;height:72px;display:grid;place-items:center;border:1px solid var(--app-border);border-radius:var(--app-radius-lg);color:var(--app-accent-primary);background:var(--app-lavender-soft)}
.no-messages{min-height:220px;flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;padding:24px 8px;text-align:center;color:var(--app-text-secondary)}.no-messages h2{font-size:18px;font-weight:650;color:var(--app-text-primary)}.no-messages p{font-size:14px;line-height:1.7}
.avatar-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:12px;max-height:55dvh;overflow-y:auto;padding:4px}.avatar-card{display:flex;align-items:center;gap:12px;min-width:0;padding:14px;text-align:left;color:var(--app-text-primary);border:1px solid var(--app-border);border-radius:var(--app-radius-md);background:var(--app-surface-inset)}.avatar-card:hover{border-color:var(--app-accent-secondary)}.avatar-card.selected{border-color:var(--app-accent-primary);background:var(--app-lavender-soft);box-shadow:var(--app-selected-glow)}.avatar-card-info{min-width:0;display:flex;flex-direction:column;gap:6px}.avatar-card-info strong{font-size:15px;overflow-wrap:anywhere}.avatar-slogan{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--app-text-secondary);font-size:12px}.avatar-type{align-self:flex-start;padding:3px 7px;border-radius:var(--app-radius-pill);background:var(--app-lavender-soft);color:var(--app-accent-primary-strong);font-size:10px}.no-avatars{grid-column:1/-1;padding:24px;text-align:center;color:var(--app-text-secondary)}.no-avatars p{margin:12px 0 18px}
@keyframes chat-blink{50%{opacity:0}}@keyframes chat-pulse{50%{opacity:.35}}
@media(max-width:1279px){.session-sidebar{flex-basis:224px}.chat-workspace{grid-template-columns:minmax(0,1fr)}.desktop-character{display:none}.character-toggle{display:inline-flex}}
@media(max-width:1000px){.session-sidebar{display:none}.sessions-toggle{display:inline-flex}.chat-page .chat-layout{padding:16px}.chat-header{padding:12px 16px}}
@media(max-width:600px){.chat-page .chat-layout{padding:10px;gap:0}.chat-main{gap:10px}.chat-header{min-height:72px;padding:10px;gap:8px}.chat-header-left{gap:8px}.chat-header-info h1{font-size:15px}.workspace-eyebrow{font-size:9px}.chat-header-actions{gap:4px}.chat-header-actions :deep(.el-button){padding:8px}.character-toggle :deep(.el-icon){display:none}.status-online{font-size:10px}.chat-workspace{gap:0}.chat-messages{gap:18px;padding:18px 12px}.message-item{max-width:100%;gap:7px}.message-bubble{font-size:14px;padding:11px 13px}.chat-input-area{padding:10px 12px 12px}.input-wrapper{gap:8px}.input-wrapper :deep(.el-textarea__inner){font-size:16px;min-height:72px!important}.keyboard-hint{display:none}.input-tools{margin-bottom:8px}.no-session{padding:24px 16px}.no-session h2{font-size:24px}.no-session .workspace-eyebrow{display:none}.avatar-grid{grid-template-columns:1fr}}
@media(prefers-reduced-motion:reduce){.cursor-blink,.status-dot.responding{animation:none}}
@media(forced-colors:active){.message-bubble,.error-tip,.avatar-card{border:1px solid CanvasText}.avatar-card.selected{outline:2px solid Highlight}}
</style>

<style lang="scss">
.chat-sessions-drawer,.chat-character-drawer,.avatar-dialog,.chat-confirm{color:var(--app-text-primary);background:var(--app-surface-elevated);--el-color-primary:var(--app-accent-primary);--el-color-primary-light-3:var(--app-accent-primary);--el-color-primary-dark-2:var(--app-accent-primary-strong);--el-text-color-primary:var(--app-text-primary);--el-text-color-regular:var(--app-text-primary);--el-border-radius-base:var(--app-radius-sm)}
.chat-confirm{border-radius:var(--app-radius-md)}.chat-confirm .el-button{min-height:44px;border-radius:var(--app-radius-sm)}
.chat-sessions-drawer .el-drawer__header,.chat-character-drawer .el-drawer__header{margin-bottom:12px;color:var(--app-text-primary)}.chat-sessions-drawer .el-drawer__body{min-height:0;padding:0 12px 12px;overflow:hidden}.chat-character-drawer .el-drawer__body{padding:4px 16px 16px}.chat-character-drawer .chat-character-panel{min-height:100%}
.avatar-dialog{border-radius:var(--app-radius-lg)}.avatar-dialog .el-dialog__title{color:var(--app-text-primary);font-weight:750}.avatar-dialog .el-button{min-height:44px}.avatar-dialog .el-dialog__body{padding:12px 20px}
</style>
