<template>
  <div class="chat-view" :class="`accent-${currentSession.accent}`">
    <div class="chat-atmosphere" aria-hidden="true">
      <span class="atmosphere-glow atmosphere-glow--left"></span>
      <span class="atmosphere-glow atmosphere-glow--right"></span>
      <span class="atmosphere-orbit atmosphere-orbit--one"></span>
      <span class="atmosphere-orbit atmosphere-orbit--two"></span>
      <DuskStar class="atmosphere-mark atmosphere-mark--one" :size="14" tone="rose" />
      <DuskStar class="atmosphere-mark atmosphere-mark--two" :size="10" tone="ice" :spark="false" />
    </div>

    <div class="chat-layout">
      <aside class="rail-panel" aria-label="窄会话列表">
        <SessionRail
          :sessions="SESSIONS"
          :current-session-id="currentSessionId"
          @select="selectSession"
        />
      </aside>

      <section class="chat-room" aria-label="聊天工作区">
        <header class="room-head">
          <button
            type="button"
            class="rail-toggle"
            aria-label="打开会话列表抽屉"
            :aria-expanded="sessionsOpen"
            @click="openSessions"
          >
            <span></span><span></span><span></span>
          </button>

          <div class="head-identity">
            <span class="head-avatar" aria-hidden="true">
              <img :src="currentSession.imageUrl" alt="" />
              <i class="head-presence"></i>
            </span>
            <div class="head-text">
              <span class="head-kicker">{{ currentSession.characterName }} · 最近一次谈话</span>
              <h1 class="head-title">{{ currentSession.topic }}</h1>
            </div>
          </div>

          <div class="head-meta">
            <span class="head-status"><i aria-hidden="true"></i>在线</span>
            <span class="head-time">{{ currentSession.time }}</span>
          </div>

          <button
            type="button"
            class="profile-toggle"
            :aria-expanded="profileOpen"
            aria-controls="character-profile"
            @click="toggleProfile"
          >
            角色资料
            <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
              <path d="M3 5.5 8 10.5 13 5.5" fill="none" stroke="currentColor" stroke-width="1.4" />
            </svg>
          </button>
        </header>

        <div
          ref="messagesRef"
          class="messages"
          role="region"
          aria-label="对话消息"
          tabindex="0"
        >
          <div class="messages-inner">
            <div class="day-line" aria-hidden="true">
              <span></span>
              <time>11 月 23 日 · 星期一</time>
              <span></span>
            </div>
            <p class="conversation-caption">与 {{ currentSession.characterName }} 的最近一次谈话</p>

            <div
              v-for="msg in visibleMessages"
              :key="msg.id"
              class="message"
              :class="[msg.role, { 'is-long': msg.long, 'is-pending': msg.pending }]"
              :data-long-message="msg.long ? 'true' : undefined"
            >
              <span class="msg-avatar">
                <ChatSessionAvatar
                  :session="currentSession"
                  :role="msg.role"
                  :user="PREVIEW_USER"
                  :size="32"
                />
              </span>
              <div class="msg-body">
                <span class="msg-name">
                  {{ msg.role === 'assistant' ? currentSession.characterName : '你' }}
                  <time v-if="msg.time">{{ msg.time }}</time>
                </span>

                <div
                  v-if="msg.pending"
                  class="msg-typing"
                  :aria-label="`${currentSession.characterName}正在输入`"
                  role="status"
                >
                  <span class="typing-dots" aria-hidden="true"><i></i><i></i><i></i></span>
                </div>
                <div
                  v-else
                  class="msg-bubble"
                  :class="{ 'is-streaming': msg.streaming, 'is-long': msg.long }"
                >
                  {{ formatContent(msg.content) }}<span v-if="msg.streaming" class="stream-caret" aria-hidden="true"></span>
                </div>
              </div>
            </div>

            <div class="messages-end" aria-hidden="true">
              <span></span><b>·</b><span></span>
            </div>
          </div>
        </div>

        <footer class="composer">
          <div class="composer-inner">
            <div class="composer-line">
              <span class="composer-to">
                <i aria-hidden="true"></i>
                写给 {{ currentSession.characterName }}
              </span>
              <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
            </div>
            <div class="composer-bar">
              <textarea
                ref="inputRef"
                v-model="draft"
                class="composer-input"
                rows="1"
                maxlength="4000"
                :placeholder="`写下此刻想说的话…`"
                aria-label="聊天消息（样稿输入，不写入真实数据）"
                @input="growInput"
                @keydown="handleInputKeydown"
              ></textarea>
              <button
                type="button"
                class="send-button"
                :disabled="!canSend"
                aria-label="发送样稿消息"
                @click="sendMessage"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path d="M5 12h13M13 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="1.5" />
                </svg>
              </button>
            </div>
            <div class="composer-meta">
              <span>样稿输入 · 不写入真实数据</span>
              <span class="composer-preview">PREVIEW ONLY</span>
            </div>
          </div>
        </footer>
      </section>
    </div>

    <Transition name="drawer-fade">
      <div v-if="sessionsOpen" class="sessions-layer">
        <button
          type="button"
          class="sessions-scrim"
          aria-label="关闭会话抽屉"
          @click="sessionsOpen = false"
        ></button>
        <Transition name="drawer-slide" appear>
          <aside
            v-if="sessionsOpen"
            class="session-drawer"
            aria-label="会话抽屉"
          >
            <button
              ref="drawerCloseRef"
              type="button"
              class="drawer-close"
              aria-label="关闭会话抽屉"
              @click="sessionsOpen = false"
            >×</button>
            <SessionRail
              :sessions="SESSIONS"
              :current-session-id="currentSessionId"
              @select="selectSession"
            />
          </aside>
        </Transition>
      </div>
    </Transition>

    <ProfilePanel
      :session="currentSession"
      :open="profileOpen"
      @close="profileOpen = false"
    />

    <PreviewDock
      v-if="!profileOpen && !sessionsOpen"
      :state="previewState"
      :profile-open="profileOpen"
      :sessions-open="sessionsOpen"
      @update:state="setPreviewState"
      @toggle-profile="toggleProfile"
      @toggle-sessions="toggleSessions"
      @reset="resetPreview"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatSessionAvatar from '../../src/components/chat/ChatSessionAvatar.vue'
import SessionRail from '../components/SessionRail.vue'
import ProfilePanel from '../components/ProfilePanel.vue'
import PreviewDock from '../components/PreviewDock.vue'
import DuskStar from '../components/DuskStar.vue'
import {
  GENERATING_TEXT,
  PREVIEW_USER,
  RECENT_SESSION_ID,
  SESSIONS,
  WAITING_USER_TEXT
} from '../data.js'

const route = useRoute()
const router = useRouter()

const sessionIds = new Set(SESSIONS.map((item) => item.id))
const initialId = sessionIds.has(route.query.session) ? route.query.session : RECENT_SESSION_ID

const currentSessionId = ref(initialId)
const previewState = ref('normal')
const sentText = ref('')
const draft = ref('')
const profileOpen = ref(false)
const sessionsOpen = ref(false)
const messagesRef = ref(null)
const inputRef = ref(null)
const drawerCloseRef = ref(null)

const currentSession = computed(
  () => SESSIONS.find((item) => item.id === currentSessionId.value) || SESSIONS[0]
)

const visibleMessages = computed(() => {
  const base = currentSession.value.messages
  if (previewState.value === 'waiting') {
    return [
      ...base,
      {
        id: 'preview-waiting-user',
        role: 'user',
        time: '现在',
        content: sentText.value || WAITING_USER_TEXT
      },
      {
        id: 'preview-waiting-assistant',
        role: 'assistant',
        pending: true
      }
    ]
  }
  if (previewState.value === 'generating') {
    return [
      ...base,
      {
        id: 'preview-generating-user',
        role: 'user',
        time: '现在',
        content: sentText.value || WAITING_USER_TEXT
      },
      {
        id: 'preview-generating-assistant',
        role: 'assistant',
        time: '现在',
        streaming: true,
        content: GENERATING_TEXT
      }
    ]
  }
  return base
})

const canSend = computed(() => draft.value.trim().length > 0)

function formatContent(content = '') {
  // Keep a single line break between paragraphs so long replies stay compact.
  return content.replace(/\r\n/g, '\n').replace(/\n{2,}/g, '\n')
}

function scrollToBottom() {
  nextTick(() => {
    const element = messagesRef.value
    if (!element) return
    element.scrollTop = element.scrollHeight
  })
}

function scrollToLongMessage() {
  nextTick(() => {
    const container = messagesRef.value
    if (!container) return
    const target = container.querySelector('[data-long-message="true"]')
    if (!target) {
      container.scrollTop = container.scrollHeight
      return
    }
    const offset =
      target.getBoundingClientRect().top -
      container.getBoundingClientRect().top +
      container.scrollTop
    container.scrollTop = Math.max(0, offset - 18)
  })
}

function selectSession(id) {
  if (!sessionIds.has(id)) return
  sessionsOpen.value = false
  profileOpen.value = false
  if (id === currentSessionId.value) return
  currentSessionId.value = id
  previewState.value = 'normal'
  sentText.value = ''
  router.replace({ path: '/chat', query: { session: id } })
  scrollToBottom()
}

function setPreviewState(value) {
  previewState.value = value
  profileOpen.value = false
  if (value === 'long') {
    // The preview control should always be able to demonstrate a long reply.
    // If the current room only has short messages, move to the room that does.
    if (!currentSession.value.messages.some((msg) => msg.long)) {
      const longSession = SESSIONS.find((item) => item.messages.some((msg) => msg.long))
      if (longSession) {
        currentSessionId.value = longSession.id
        sentText.value = ''
        router.replace({ path: '/chat', query: { session: longSession.id } })
      }
    }
    scrollToLongMessage()
  } else {
    scrollToBottom()
  }
}

function toggleProfile() {
  sessionsOpen.value = false
  profileOpen.value = !profileOpen.value
}

function openSessions() {
  profileOpen.value = false
  sessionsOpen.value = true
}

function toggleSessions() {
  profileOpen.value = false
  sessionsOpen.value = !sessionsOpen.value
}

function resetPreview() {
  previewState.value = 'normal'
  sentText.value = ''
  draft.value = ''
  sessionsOpen.value = false
  profileOpen.value = false
  nextTick(() => resetInputHeight())
  scrollToBottom()
}

function growInput() {
  const element = inputRef.value
  if (!element) return
  element.style.height = 'auto'
  const nextHeight = Math.min(Math.max(element.scrollHeight, 46), 128)
  element.style.height = `${nextHeight}px`
  element.style.overflowY = element.scrollHeight > 128 ? 'auto' : 'hidden'
}

function resetInputHeight() {
  const element = inputRef.value
  if (!element) return
  element.style.height = '46px'
  element.style.overflowY = 'hidden'
}

function sendMessage() {
  const text = draft.value.trim()
  if (!text) return
  sentText.value = text.replace(/\r\n/g, '\n')
  previewState.value = 'waiting'
  draft.value = ''
  nextTick(() => {
    resetInputHeight()
    scrollToBottom()
  })
  inputRef.value?.focus()
}

function handleInputKeydown(event) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  sendMessage()
}

function handleGlobalKeydown(event) {
  if (event.key !== 'Escape') return
  if (sessionsOpen.value) {
    sessionsOpen.value = false
    return
  }
  if (profileOpen.value) profileOpen.value = false
}

watch(
  () => route.query.session,
  (id) => {
    if (!id || id === currentSessionId.value || !sessionIds.has(id)) return
    currentSessionId.value = id
    previewState.value = 'normal'
    sentText.value = ''
    scrollToBottom()
  }
)

watch(sessionsOpen, (open) => {
  if (open) nextTick(() => drawerCloseRef.value?.focus())
})

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
  scrollToBottom()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<style lang="scss" scoped>
.chat-view {
  --session-accent: var(--dream-rose);
  position: fixed;
  z-index: 1;
  top: var(--dream-nav-h);
  right: 0;
  bottom: 0;
  left: 0;
  overflow: hidden;
  isolation: isolate;
  color: var(--dream-ink);
}

.chat-view.accent-sun {
  --session-accent: var(--dream-apricot);
}

.chat-view.accent-ice {
  --session-accent: var(--dream-ice);
}

.chat-view.accent-violet {
  --session-accent: var(--dream-violet);
}

.chat-view.accent-rose {
  --session-accent: var(--dream-rose);
}

.chat-atmosphere {
  position: absolute;
  z-index: 0;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.atmosphere-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(30px);
}

.atmosphere-glow--left {
  bottom: -16%;
  left: -7%;
  width: 42%;
  height: 48%;
  background: radial-gradient(circle, rgba(237, 187, 199, 0.34), transparent 68%);
}

.atmosphere-glow--right {
  top: -20%;
  right: -8%;
  width: 44%;
  height: 50%;
  background: radial-gradient(circle, rgba(156, 194, 215, 0.3), transparent 68%);
}

.atmosphere-orbit {
  position: absolute;
  border: 1px solid rgba(92, 127, 151, 0.15);
  border-radius: 50%;
}

.atmosphere-orbit--one {
  top: 10%;
  right: -12%;
  width: 34%;
  height: 42%;
  transform: rotate(-17deg);
}

.atmosphere-orbit--two {
  bottom: -24%;
  left: -7%;
  width: 27%;
  height: 42%;
  border-color: rgba(188, 122, 141, 0.14);
  transform: rotate(12deg);
}

.atmosphere-mark {
  position: absolute;
  opacity: 0.46;
}

.atmosphere-mark--one {
  top: 12%;
  right: 4%;
}

.atmosphere-mark--two {
  bottom: 12%;
  left: 3%;
}

.chat-layout {
  position: relative;
  z-index: 1;
  width: min(1220px, calc(100% - 32px));
  height: 100%;
  min-height: 0;
  margin: 0 auto;
  display: flex;
  gap: 13px;
  padding: 14px 0 16px;
}

.rail-panel {
  flex: 0 0 236px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--dream-line-soft);
  border-radius: 20px;
  background: linear-gradient(168deg, rgba(255, 251, 245, 0.76), rgba(244, 239, 238, 0.58));
  box-shadow: var(--dream-shadow-inset), 0 14px 34px rgba(75, 69, 89, 0.09);
  backdrop-filter: blur(14px);
}

.chat-room {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--dream-line-soft);
  border-radius: 20px;
  background:
    radial-gradient(ellipse 56% 30% at 88% 0%, rgba(172, 200, 215, 0.17), transparent 68%),
    radial-gradient(ellipse 60% 32% at 4% 100%, rgba(231, 189, 199, 0.13), transparent 72%),
    linear-gradient(180deg, rgba(255, 252, 247, 0.9) 0%, rgba(251, 246, 240, 0.82) 58%, rgba(248, 241, 236, 0.86) 100%);
  box-shadow: var(--dream-shadow-inset), 0 18px 44px rgba(75, 69, 89, 0.1);
}

.room-head {
  flex: none;
  min-height: 70px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px 10px 18px;
  border-bottom: 1px solid var(--dream-line-soft);
  background: linear-gradient(90deg, rgba(255, 252, 247, 0.42), transparent 60%);
}

.rail-toggle {
  display: none;
  width: 44px;
  height: 44px;
  flex: none;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  border-radius: 10px;
  color: var(--dream-ink-2);
}

.rail-toggle span {
  width: 16px;
  height: 1px;
  background: currentColor;
  opacity: 0.76;
}

.rail-toggle span:nth-child(2) {
  width: 12px;
}

.rail-toggle:hover {
  background: rgba(199, 121, 144, 0.08);
}

.head-identity {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 11px;
}

.head-avatar {
  position: relative;
  width: 40px;
  height: 40px;
  flex: none;
  overflow: visible;
  border: 1px solid rgba(73, 69, 89, 0.2);
  border-radius: 50%;
  background: #e9e1e5;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.52);
}

.head-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.head-presence {
  position: absolute;
  right: -1px;
  bottom: 1px;
  width: 8px;
  height: 8px;
  border: 1px solid #fffaf4;
  border-radius: 50%;
  background: var(--dream-success);
}

.head-text {
  min-width: 0;
}

.head-kicker {
  display: block;
  overflow: hidden;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.12em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-title {
  overflow: hidden;
  margin-top: 2px;
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 19px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.02em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-meta {
  flex: none;
  display: flex;
  align-items: center;
  gap: 11px;
  color: var(--dream-ink-3);
}

.head-status {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 8px;
  border: 1px solid var(--dream-line-soft);
  border-radius: var(--dream-radius-pill);
  font-size: 9px;
  letter-spacing: 0.04em;
  background: rgba(255, 252, 247, 0.46);
}

.head-status i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dream-success);
  box-shadow: 0 0 0 3px rgba(91, 147, 122, 0.12);
}

.head-time {
  font-family: var(--dream-font-annotation);
  font-size: 9px;
  letter-spacing: 0.05em;
}

.profile-toggle {
  min-height: 44px;
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid var(--dream-line-soft);
  border-radius: var(--dream-radius-pill);
  color: var(--dream-ink-2);
  font-size: 10.5px;
  background: rgba(255, 252, 247, 0.56);
  transition:
    border-color var(--dream-motion) var(--dream-ease),
    color var(--dream-motion) var(--dream-ease),
    background var(--dream-motion) var(--dream-ease);
}

.profile-toggle:hover {
  color: var(--dream-ink);
  border-color: rgba(184, 112, 133, 0.32);
  background: rgba(255, 253, 249, 0.8);
}

.profile-toggle svg {
  transition: transform var(--dream-motion) var(--dream-ease);
}

.profile-toggle[aria-expanded="true"] {
  color: var(--dream-rose);
  border-color: rgba(184, 112, 133, 0.36);
}

.profile-toggle[aria-expanded="true"] svg {
  transform: rotate(180deg);
}

.messages {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  background: transparent;
}

.messages:focus-visible {
  outline: 2px solid var(--dream-focus);
  outline-offset: -4px;
}

.messages-inner {
  width: min(100%, 720px);
  min-height: 100%;
  margin: 0 auto;
  padding: 20px clamp(18px, 3vw, 32px) 26px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.day-line {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--dream-ink-3);
}

.day-line span {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(58, 62, 91, 0.16));
}

.day-line span:last-child {
  background: linear-gradient(90deg, rgba(58, 62, 91, 0.16), transparent);
}

.day-line time {
  flex: none;
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.1em;
}

.conversation-caption {
  margin: -5px 0 4px;
  color: var(--dream-ink-3);
  font-size: 8.5px;
  letter-spacing: 0.06em;
  text-align: center;
}

.message {
  min-width: 0;
  max-width: 94%;
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.message.assistant {
  align-self: flex-start;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  flex: none;
  padding-top: 17px;
}

.msg-avatar :deep(.simple-avatar) {
  border-width: 1px;
  border-color: rgba(72, 68, 88, 0.17);
  box-shadow: 0 4px 14px rgba(71, 67, 91, 0.13);
}

.msg-body {
  min-width: 0;
  max-width: 628px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message.user .msg-body {
  align-items: flex-end;
}

.msg-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 0 4px;
  color: var(--dream-ink-3);
  font-size: 9px;
  letter-spacing: 0.04em;
}

.msg-name time {
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  opacity: 0.8;
}

.message.user .msg-name {
  flex-direction: row-reverse;
}

.msg-bubble {
  width: fit-content;
  min-width: 44px;
  max-width: 100%;
  padding: 11px 15px;
  border: 1px solid rgba(58, 62, 91, 0.1);
  border-radius: 5px 16px 16px 16px;
  color: var(--dream-ink);
  font-size: 14.5px;
  line-height: 1.78;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: rgba(255, 252, 247, 0.8);
  box-shadow: 0 6px 18px rgba(72, 67, 88, 0.055);
}

.msg-bubble.is-long {
  padding: 13px 16px;
  line-height: 1.72;
}

.message.user .msg-bubble {
  color: #3e3341;
  border-color: rgba(188, 132, 147, 0.2);
  border-radius: 16px 5px 16px 16px;
  background: linear-gradient(135deg, rgba(246, 224, 229, 0.84), rgba(241, 226, 226, 0.62));
}

.msg-typing {
  min-height: 38px;
  display: flex;
  align-items: center;
  padding: 6px 4px;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 10px;
}

.typing-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--session-accent);
  opacity: 0.28;
  animation: typing-wave 1.4s var(--dream-ease) infinite;
}

.typing-dots i:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dots i:nth-child(3) {
  animation-delay: 0.32s;
}

.stream-caret {
  display: inline-block;
  width: 6px;
  height: 14px;
  margin-left: 3px;
  vertical-align: -2px;
  border-radius: 1px;
  background: var(--session-accent);
  opacity: 0.72;
  animation: caret-breathe 1.1s step-end infinite;
}

.is-streaming {
  border-color: rgba(184, 112, 133, 0.22);
}

.messages-end {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  color: var(--dream-ink-3);
  opacity: 0.64;
}

.messages-end span {
  width: 22px;
  height: 1px;
  background: var(--dream-line);
}

.messages-end b {
  font-size: 8px;
  font-weight: 400;
}

.composer {
  flex: none;
  padding: 9px clamp(14px, 3vw, 22px) 10px;
  border-top: 1px solid var(--dream-line-soft);
  background: linear-gradient(180deg, rgba(251, 246, 240, 0.58), rgba(255, 252, 247, 0.84));
}

.composer-inner {
  width: min(100%, 720px);
  margin: 0 auto;
}

.composer-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 5px;
}

.composer-to {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--dream-ink-3);
  font-size: 9px;
  letter-spacing: 0.05em;
}

.composer-to i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--session-accent);
  opacity: 0.76;
}

.composer-hint {
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.04em;
}

.composer-bar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 5px 6px 5px 13px;
  border: 1px solid var(--dream-line);
  border-radius: 14px;
  background: rgba(255, 253, 249, 0.86);
  box-shadow: var(--dream-shadow-inset);
  transition:
    border-color var(--dream-motion) var(--dream-ease),
    box-shadow var(--dream-motion) var(--dream-ease);
}

.composer-bar:focus-within {
  border-color: rgba(85, 143, 168, 0.54);
  box-shadow:
    var(--dream-shadow-inset),
    0 0 0 3px rgba(125, 176, 199, 0.14);
}

.composer-input {
  min-width: 0;
  flex: 1;
  height: 46px;
  min-height: 46px;
  max-height: 128px;
  padding: 11px 0;
  overflow-y: hidden;
  border: 0;
  outline: none;
  resize: none;
  color: var(--dream-ink);
  font-family: var(--dream-font-body);
  font-size: 14px;
  line-height: 1.6;
  background: transparent;
}

.composer-input:focus-visible {
  outline: none;
  box-shadow: none;
}

.composer-input::placeholder {
  color: var(--dream-ink-3);
  opacity: 0.76;
}

.send-button {
  width: 44px;
  height: 44px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--dream-ink-inverse);
  background: linear-gradient(135deg, #3b4062, #282c49);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16), 0 7px 16px rgba(47, 50, 78, 0.2);
  transition:
    opacity var(--dream-motion) var(--dream-ease),
    transform var(--dream-motion) var(--dream-ease),
    box-shadow var(--dream-motion) var(--dream-ease);
}

.send-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow:
    0 0 0 4px rgba(199, 121, 144, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    0 10px 20px rgba(47, 50, 78, 0.24);
}

.send-button:disabled {
  opacity: 0.32;
  box-shadow: none;
}

.composer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 7px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.composer-preview {
  color: rgba(174, 105, 126, 0.7);
}

.sessions-layer {
  position: fixed;
  z-index: 72;
  top: var(--dream-nav-h);
  right: 0;
  bottom: 0;
  left: 0;
}

.sessions-scrim {
  position: absolute;
  inset: 0;
  cursor: default;
  background: rgba(49, 48, 66, 0.2);
  backdrop-filter: blur(2px);
}

.session-drawer {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: min(332px, 86vw);
  overflow: hidden;
  color: var(--dream-ink);
  border-right: 1px solid rgba(58, 62, 91, 0.14);
  background:
    radial-gradient(ellipse at 0% 0%, rgba(235, 188, 200, 0.28), transparent 38%),
    linear-gradient(168deg, #fcf8f3, #f2ecec);
  box-shadow: 20px 0 58px rgba(61, 55, 75, 0.18);
}

.session-drawer :deep(.rail-head) {
  padding-top: 20px;
  padding-right: 54px;
}

.session-drawer :deep(.rail-scroll) {
  padding-bottom: 16px;
}

.drawer-close {
  position: absolute;
  z-index: 3;
  top: 10px;
  right: 9px;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--dream-ink-2);
  font-size: 18px;
}

.drawer-close:hover {
  color: var(--dream-rose);
  background: rgba(199, 121, 144, 0.08);
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity var(--dream-motion) var(--dream-ease);
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 300ms var(--dream-ease);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-100%);
}

@keyframes typing-wave {
  0%, 100% { opacity: 0.2; transform: translateY(0); }
  40% { opacity: 0.88; transform: translateY(-2px); }
}

@keyframes caret-breathe {
  50% { opacity: 0.16; }
}

@media (max-width: 1080px) {
  .rail-panel {
    flex-basis: 214px;
  }

  .chat-layout {
    gap: 11px;
    padding-right: 16px;
    padding-left: 16px;
  }

  .head-meta {
    display: none;
  }
}

@media (max-width: 960px) {
  .rail-panel {
    display: none;
  }

  .rail-toggle {
    display: flex;
  }

  .chat-layout {
    width: min(100% - 24px, 920px);
    padding-top: 12px;
    padding-bottom: 64px;
  }
}

@media (max-width: 640px) {
  .chat-layout {
    width: 100%;
    padding: 8px 8px 64px;
  }

  .chat-room {
    border-radius: 16px;
  }

  .room-head {
    min-height: 60px;
    gap: 7px;
    padding: 7px 8px 7px 6px;
  }

  .rail-toggle {
    width: 40px;
    height: 44px;
  }

  .head-avatar {
    width: 36px;
    height: 36px;
  }

  .head-kicker {
    font-size: 7.5px;
  }

  .head-title {
    font-size: 17px;
  }

  .profile-toggle {
    gap: 4px;
    padding: 0 10px;
    font-size: 9.5px;
  }

  .messages-inner {
    padding: 17px 12px 23px;
    gap: 13px;
  }

  .conversation-caption {
    display: none;
  }

  .message {
    max-width: 100%;
    gap: 7px;
  }

  .msg-avatar {
    padding-top: 16px;
  }

  .msg-bubble {
    padding: 10px 13px;
    font-size: 13.5px;
    line-height: 1.76;
  }

  .msg-bubble.is-long {
    padding: 12px 14px;
    line-height: 1.74;
  }

  .msg-name {
    font-size: 8.5px;
  }

  .composer {
    padding: 7px 10px 9px;
  }

  .composer-hint {
    display: none;
  }

  .composer-bar {
    padding-left: 11px;
  }

  .composer-input {
    font-size: 16px;
  }

  .composer-meta {
    font-size: 6.5px;
  }

  .session-drawer {
    width: min(330px, 86vw);
  }
}

@media (max-width: 390px) {
  .head-kicker {
    display: none;
  }

  .profile-toggle {
    padding: 0 9px;
  }

  .profile-toggle svg {
    display: none;
  }

  .composer-preview {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .typing-dots i,
  .stream-caret {
    animation: none;
  }

  .typing-dots i {
    opacity: 0.62;
  }

  .drawer-slide-enter-active,
  .drawer-slide-leave-active,
  .drawer-fade-enter-active,
  .drawer-fade-leave-active {
    transition: none;
  }
}

@media (forced-colors: active) {
  .chat-room,
  .rail-panel,
  .msg-bubble,
  .composer-bar,
  .session-drawer {
    border: 1px solid CanvasText;
  }

  .atmosphere-glow,
  .atmosphere-orbit,
  .atmosphere-mark {
    display: none;
  }
}
</style>
