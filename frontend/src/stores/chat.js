import { defineStore } from 'pinia'
import { markRaw } from 'vue'
import {
  createSession as createSessionApi,
  getSessionList,
  deleteSession as deleteSessionApi,
  getMessageList,
  streamChat as streamChatApi
} from '../api/chat'
import {
  CHAT_MESSAGE_STATUS,
  createChatRequestId,
  findRetryContent,
  isMessageActive,
  mapChatMessage
} from '../utils/chatMessageState'
import { createChatSendGuard } from '../utils/chatSendGuard'

const sendGuard = createChatSendGuard()

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],
    currentSessionId: null,
    messages: [],
    loading: false,
    streaming: false,
    error: null,
    currentAvatar: null,
    activeAbortController: null
  }),
  getters: {
    currentSession: (state) =>
      state.sessions.find((session) => session.id === state.currentSessionId) || null,
    sortedSessions: (state) => state.sessions
  },
  actions: {
    setCurrentAvatar(avatar) {
      this.currentAvatar = avatar
    },
    async fetchSessions() {
      this.loading = true
      this.error = null
      try {
        const res = await getSessionList()
        this.sessions = res.data || res || []
      } catch (err) {
        this.sessions = []
        this.error = err.message || '获取会话列表失败'
      } finally {
        this.loading = false
      }
    },
    async createSession(characterId, title) {
      this.loading = true
      this.error = null
      try {
        const res = await createSessionApi({ characterId, title })
        const session = res.data || res
        this.sessions.unshift(session)
        this.currentSessionId = session.id
        this.messages = []
        return session
      } catch (err) {
        this.error = err.message || '创建会话失败'
        throw err
      } finally {
        this.loading = false
      }
    },
    async selectSession(sessionId) {
      this.stopGeneration()
      this.currentSessionId = sessionId
      this.messages = []
      await this.fetchMessages(sessionId)
    },
    async fetchMessages(sessionId) {
      this.loading = true
      this.error = null
      try {
        const res = await getMessageList(sessionId)
        const list = res.data || res || []
        this.messages = list.map((message) => mapChatMessage(message, this.formatTime))
      } catch (err) {
        this.error = err.message || '获取消息记录失败'
      } finally {
        this.loading = false
      }
    },
    async deleteSession(sessionId) {
      try {
        if (this.currentSessionId === sessionId) this.stopGeneration()
        await deleteSessionApi(sessionId)
        this.sessions = this.sessions.filter((session) => session.id !== sessionId)
        if (this.currentSessionId === sessionId) {
          this.currentSessionId = null
          this.messages = []
        }
      } catch (err) {
        this.error = err.message || '删除会话失败'
        throw err
      }
    },
    async sendMessage(content) {
      const sessionId = this.currentSessionId
      if (!sessionId || this.streaming || !sendGuard.tryAcquire(sessionId)) return false
      const requestId = createChatRequestId()
      const abortController = markRaw(new AbortController())
      this.streaming = true
      this.error = null
      this.activeAbortController = abortController

      const now = new Date().toISOString()
      const userMessage = {
        id: `local-user-${requestId}`,
        sessionId,
        requestId,
        role: 'user',
        content,
        status: CHAT_MESSAGE_STATUS.COMPLETED,
        time: this.formatTime(now)
      }
      const assistantMessage = {
        id: `local-assistant-${requestId}`,
        sessionId,
        requestId,
        role: 'assistant',
        content: '',
        status: CHAT_MESSAGE_STATUS.PENDING,
        time: this.formatTime(now),
        streaming: false
      }
      this.messages.push(userMessage, assistantMessage)

      try {
        const reader = await streamChatApi(sessionId, content, requestId, abortController.signal)
        await this.consumeStream(reader, assistantMessage)
      } catch (err) {
        if (err.name === 'AbortError') {
          assistantMessage.status = CHAT_MESSAGE_STATUS.CANCELLED
        } else {
          this.error = err.message || '发送消息失败'
          assistantMessage.status = CHAT_MESSAGE_STATUS.FAILED
          assistantMessage.errorMessage = this.error
        }
      } finally {
        this.streaming = false
        assistantMessage.streaming = false
        if (this.activeAbortController === abortController) this.activeAbortController = null
        await this.syncMessagesAfterStream(sessionId, requestId)
        sendGuard.release(sessionId)
      }
      return true
    },
    async consumeStream(reader, message) {
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = 'message'

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          if (message.status !== CHAT_MESSAGE_STATUS.FAILED) {
            message.status = CHAT_MESSAGE_STATUS.COMPLETED
          }
          return
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmedLine = line.trim()
          if (!trimmedLine) continue
          if (trimmedLine.startsWith('event:')) {
            currentEvent = trimmedLine.substring(6).trim()
            continue
          }
          if (!trimmedLine.startsWith('data:')) continue

          const data = trimmedLine.substring(5).trim()
          if (currentEvent === 'error') {
            this.error = data
            message.status = CHAT_MESSAGE_STATUS.FAILED
            message.errorMessage = data
            return
          }
          if (data === '[DONE]') {
            message.status = CHAT_MESSAGE_STATUS.COMPLETED
            return
          }

          message.status = CHAT_MESSAGE_STATUS.STREAMING
          message.streaming = true
          message.content += data
          currentEvent = 'message'
        }
      }
    },
    stopGeneration() {
      if (!this.activeAbortController) return false
      this.activeAbortController.abort()
      const activeMessage = [...this.messages].reverse().find((message) =>
        message.role === 'assistant' && [
          CHAT_MESSAGE_STATUS.PENDING,
          CHAT_MESSAGE_STATUS.STREAMING
        ].includes(message.status)
      )
      if (activeMessage) {
        activeMessage.status = CHAT_MESSAGE_STATUS.CANCELLED
        activeMessage.streaming = false
      }
      return true
    },
    async retryMessage(message) {
      if (this.streaming) return
      const content = findRetryContent(this.messages, message)
      if (!content) {
        this.error = '未找到需要重试的用户消息'
        return
      }
      await this.sendMessage(content)
    },
    async syncMessagesAfterStream(sessionId, requestId) {
      for (let attempt = 0; attempt < 3; attempt += 1) {
        await new Promise((resolve) => setTimeout(resolve, 150))
        if (this.currentSessionId !== sessionId) return
        await this.fetchMessages(sessionId)
        const assistant = this.messages.find((message) =>
          message.role === 'assistant' && message.requestId === requestId
        )
        if (!assistant || !isMessageActive(assistant.status)) return
      }
    },
    formatTime(isoString) {
      if (!isoString) return ''
      const date = new Date(isoString)
      if (Number.isNaN(date.getTime())) {
        const parts = isoString.split('T')
        return parts.length === 2 ? parts[1].substring(0, 5) : isoString
      }
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    },
    resetState() {
      this.stopGeneration()
      sendGuard.clear()
      this.sessions = []
      this.currentSessionId = null
      this.messages = []
      this.loading = false
      this.streaming = false
      this.error = null
      this.currentAvatar = null
      this.activeAbortController = null
    }
  }
})
