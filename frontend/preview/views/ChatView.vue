<template>
  <div class="chat-view">
    <div class="chat-atmosphere" aria-hidden="true">
      <span class="atmosphere-glow atmosphere-glow--left"></span>
      <span class="atmosphere-glow atmosphere-glow--right"></span>
      <span class="atmosphere-orbit"></span>
      <DuskStar class="atmosphere-star atmosphere-star--one" :size="20" tone="rose" />
      <DuskStar class="atmosphere-star atmosphere-star--two" :size="12" tone="ice" :spark="false" />
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
            </span>
            <div class="head-text">
              <span class="head-kicker">{{ currentSession.characterName }} · 当前对话</span>
              <h1 class="head-title">{{ currentSession.topic }}</h1>
            </div>
          </div>

          <div class="head-meta">
            <span class="head-status"><i aria-hidden="true"></i>在线</span>
            <span class="head-time">最近 {{ currentSession.time }}</span>
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
              <time>12 月 17 日 · 星期四</time>
              <span></span>
            </div>
            <p class="conversation-caption">窗边的雪 · 第 12 次谈话</p>

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
                  class="msg-bubble is-pending"
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
                  {{ msg.content }}<span v-if="msg.streaming" class="stream-caret" aria-hidden="true"></span>
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
                <DuskStar :size="12" tone="rose" :spark="false" />
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
  position: fixed;
  z-index: 1;
  top: var(--dusk-nav-h);
  right: 0;
  bottom: 0;
  left: 0;
  overflow: hidden;
  isolation: isolate;
  color: var(--dusk-text);
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
  filter: blur(26px);
}

.atmosphere-glow--left {
  bottom: -18%;
  left: -8%;
  width: 48%;
  height: 56%;
  background: radial-gradient(circle, rgba(238, 157, 184, 0.14), transparent 68%);
}

.atmosphere-glow--right {
  top: -22%;
  right: -10%;
  width: 46%;
  height: 54%;
  background: radial-gradient(circle, rgba(93, 125, 176, 0.2), transparent 66%);
}

.atmosphere-orbit {
  position: absolute;
  bottom: -26%;
  left: -6%;
  width: 44%;
  height: 50%;
  border: 1px solid rgba(232, 182, 199, 0.12);
  border-radius: 50%;
  transform: rotate(-12deg);
}

.atmosphere-star {
  position: absolute;
  opacity: 0.5;
}

.atmosphere-star--one {
  top: 13%;
  right: 5%;
}

.atmosphere-star--two {
  bottom: 14%;
  left: 5%;
}

.chat-layout {
  position: relative;
  z-index: 1;
  width: min(1180px, calc(100% - 32px));
  height: 100%;
  min-height: 0;
  margin: 0 auto;
  display: flex;
  gap: 14px;
  padding: 14px 0 16px;
}

.rail-panel {
  flex: 0 0 224px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--dusk-line-soft);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(19, 28, 55, 0.88), rgba(11, 18, 36, 0.9));
  box-shadow: var(--dusk-shadow-soft);
}

.chat-room {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--dusk-line);
  border-radius: 20px;
  background: linear-gradient(180deg, #121c37 0%, #0e1730 62%, #0b1225 100%);
  box-shadow: var(--dusk-shadow-soft);
}

.room-head {
  flex: none;
  min-height: 68px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px 10px 18px;
  border-bottom: 1px solid var(--dusk-line-soft);
  background:
    linear-gradient(90deg, rgba(238, 157, 184, 0.03), transparent 38%),
    rgba(10, 16, 32, 0.34);
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
  color: var(--dusk-text-2);
}

.rail-toggle span {
  width: 16px;
  height: 1px;
  background: currentColor;
  opacity: 0.8;
}

.rail-toggle span:nth-child(2) {
  width: 12px;
}

.rail-toggle:hover {
  background: rgba(232, 182, 199, 0.06);
}

.head-identity {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 11px;
}

.head-avatar {
  width: 40px;
  height: 40px;
  flex: none;
  overflow: hidden;
  border: 1px solid rgba(244, 239, 229, 0.3);
  border-radius: 50%;
  background: #111a33;
  box-shadow: 0 0 0 3px rgba(244, 239, 229, 0.03);
}

.head-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.head-text {
  min-width: 0;
}

.head-kicker {
  display: block;
  overflow: hidden;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.11em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-title {
  overflow: hidden;
  margin-top: 2px;
  color: var(--dusk-text);
  font-family: var(--dusk-font-display);
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
  gap: 12px;
  color: var(--dusk-text-3);
}

.head-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.head-status i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dusk-success);
  box-shadow: 0 0 7px rgba(143, 201, 172, 0.4);
}

.head-time {
  font-family: var(--dusk-font-annotation);
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
  border: 1px solid var(--dusk-line-soft);
  border-radius: var(--dusk-radius-pill);
  color: var(--dusk-text-2);
  font-size: 11px;
  background: rgba(22, 31, 56, 0.38);
  transition:
    border-color var(--dusk-motion) var(--dusk-ease),
    color var(--dusk-motion) var(--dusk-ease),
    background var(--dusk-motion) var(--dusk-ease);
}

.profile-toggle:hover {
  color: var(--dusk-text);
  border-color: rgba(232, 182, 199, 0.32);
  background: rgba(232, 182, 199, 0.05);
}

.profile-toggle svg {
  transition: transform var(--dusk-motion) var(--dusk-ease);
}

.profile-toggle[aria-expanded="true"] {
  color: var(--dusk-rose-soft);
  border-color: rgba(232, 182, 199, 0.36);
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
  outline: 2px solid var(--dusk-focus);
  outline-offset: -4px;
}

.messages-inner {
  width: min(100%, 720px);
  min-height: 100%;
  margin: 0 auto;
  padding: 24px clamp(18px, 3vw, 30px) 30px;
  display: flex;
  flex-direction: column;
  gap: 17px;
}

.day-line {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--dusk-text-3);
}

.day-line span {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(216, 224, 248, 0.14));
}

.day-line span:last-child {
  background: linear-gradient(90deg, rgba(216, 224, 248, 0.14), transparent);
}

.day-line time {
  flex: none;
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.conversation-caption {
  margin: -5px 0 4px;
  color: var(--dusk-text-3);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-align: center;
}

.message {
  min-width: 0;
  max-width: 92%;
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
  padding-top: 18px;
}

.msg-avatar :deep(.simple-avatar) {
  border-width: 1px;
  border-color: rgba(244, 239, 229, 0.26);
  box-shadow: 0 6px 16px rgba(3, 8, 20, 0.3);
}

.msg-body {
  min-width: 0;
  max-width: 620px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.message.user .msg-body {
  align-items: flex-end;
}

.msg-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 0 3px;
  color: var(--dusk-text-3);
  font-size: 9.5px;
  letter-spacing: 0.05em;
}

.msg-name time {
  font-family: var(--dusk-font-annotation);
  font-size: 8.5px;
  opacity: 0.8;
}

.message.user .msg-name {
  flex-direction: row-reverse;
}

.msg-bubble {
  width: fit-content;
  min-width: 44px;
  max-width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(216, 224, 248, 0.11);
  border-radius: 4px 14px 14px 14px;
  color: var(--dusk-text);
  font-size: 14.5px;
  line-height: 1.88;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: rgba(236, 239, 255, 0.056);
  box-shadow: 0 8px 24px rgba(1, 4, 12, 0.12);
}

.msg-bubble.is-long {
  padding: 16px 18px;
  line-height: 1.95;
}

.message.user .msg-bubble {
  color: #f3e9ec;
  border-color: rgba(232, 182, 199, 0.26);
  border-radius: 14px 4px 14px 14px;
  background: linear-gradient(135deg, rgba(232, 182, 199, 0.15), rgba(196, 155, 179, 0.09));
}

.msg-bubble.is-pending {
  padding: 15px 18px;
  background: rgba(236, 239, 255, 0.05);
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
  background: var(--dusk-rose);
  opacity: 0.3;
  animation: typing-wave 1.4s var(--dusk-ease) infinite;
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
  background: var(--dusk-rose);
  opacity: 0.72;
  animation: caret-breathe 1.1s step-end infinite;
}

.is-streaming {
  border-color: rgba(232, 182, 199, 0.22);
}

.messages-end {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  color: var(--dusk-text-3);
  opacity: 0.55;
}

.messages-end span {
  width: 22px;
  height: 1px;
  background: var(--dusk-line);
}

.messages-end b {
  font-size: 8px;
  font-weight: 400;
}

.composer {
  flex: none;
  padding: 9px clamp(14px, 3vw, 22px) 11px;
  border-top: 1px solid var(--dusk-line-soft);
  background:
    linear-gradient(180deg, rgba(17, 26, 49, 0.74), rgba(11, 18, 36, 0.96));
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
  color: var(--dusk-text-3);
  font-size: 9.5px;
  letter-spacing: 0.05em;
}

.composer-hint {
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.04em;
}

.composer-bar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 5px 6px 5px 13px;
  border: 1px solid var(--dusk-line);
  border-radius: 14px;
  background: rgba(20, 29, 53, 0.66);
  transition:
    border-color var(--dusk-motion) var(--dusk-ease),
    box-shadow var(--dusk-motion) var(--dusk-ease);
}

.composer-bar:focus-within {
  border-color: rgba(184, 216, 232, 0.52);
  box-shadow: 0 0 0 3px rgba(184, 216, 232, 0.1);
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
  color: var(--dusk-text);
  font-family: var(--dusk-font-body);
  font-size: 14.5px;
  line-height: 1.6;
  background: transparent;
}

.composer-input::placeholder {
  color: var(--dusk-text-3);
  opacity: 0.84;
}

.send-button {
  width: 44px;
  height: 44px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--dusk-text-inverse);
  background: linear-gradient(135deg, #f7f1e6, #ddd6dd);
  box-shadow: inset 0 1px 0 #fffdf8;
  transition:
    opacity var(--dusk-motion) var(--dusk-ease),
    transform var(--dusk-motion) var(--dusk-ease),
    box-shadow var(--dusk-motion) var(--dusk-ease);
}

.send-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow:
    0 0 0 4px rgba(232, 182, 199, 0.12),
    inset 0 1px 0 #fffdf8;
}

.send-button:disabled {
  opacity: 0.34;
  box-shadow: none;
}

.composer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 5px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 7.5px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.composer-preview {
  color: rgba(232, 182, 199, 0.62);
}

.sessions-layer {
  position: fixed;
  z-index: 72;
  top: var(--dusk-nav-h);
  right: 0;
  bottom: 0;
  left: 0;
}

.sessions-scrim {
  position: absolute;
  inset: 0;
  cursor: default;
  background: rgba(4, 8, 18, 0.56);
  backdrop-filter: blur(2px);
}

.session-drawer {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: min(322px, 86vw);
  overflow: hidden;
  color: var(--dusk-text);
  border-right: 1px solid rgba(232, 182, 199, 0.2);
  background:
    radial-gradient(ellipse at 0% 0%, rgba(238, 157, 184, 0.1), transparent 36%),
    linear-gradient(180deg, #121c38, #0a1124);
  box-shadow: 24px 0 70px rgba(1, 4, 12, 0.42);
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
  color: var(--dusk-text-2);
  font-size: 18px;
}

.drawer-close:hover {
  color: var(--dusk-rose);
  background: rgba(232, 182, 199, 0.06);
}

.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity var(--dusk-motion) var(--dusk-ease);
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 300ms var(--dusk-ease);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-100%);
}

@keyframes typing-wave {
  0%, 100% { opacity: 0.22; transform: translateY(0); }
  40% { opacity: 0.9; transform: translateY(-2px); }
}

@keyframes caret-breathe {
  50% { opacity: 0.18; }
}

@media (max-width: 1080px) {
  .rail-panel {
    flex-basis: 204px;
  }

  .chat-layout {
    gap: 12px;
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
    font-size: 10px;
  }

  .messages-inner {
    padding: 18px 12px 24px;
    gap: 15px;
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
    font-size: 14px;
    line-height: 1.82;
  }

  .msg-bubble.is-long {
    padding: 13px 14px;
    line-height: 1.9;
  }

  .msg-name {
    font-size: 9px;
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
    font-size: 7px;
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
    opacity: 0.64;
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
  .atmosphere-star {
    display: none;
  }
}
</style>
