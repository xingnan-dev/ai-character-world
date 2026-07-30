import { defineStore } from 'pinia'
import {
  createSession as createSessionApi,
  getSessionList,
  deleteSession as deleteSessionApi,
  getMessageList,
  streamChat as streamChatApi
} from '../api/chat'
import { useAvatarStore } from './avatar'

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],
    currentSessionId: null,
    messages: [],
    loading: false,
    streaming: false,
    error: null,
    currentAvatar: null
  }),
  getters: {
    currentSession: (state) =>
      state.sessions.find((s) => s.id === state.currentSessionId) || null,
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
        const list = res.data || res || []
        this.sessions = list
      } catch (err) {
        this.sessions = []
      } finally {
        this.loading = false
      }
    },
    async createSession(avatarId, title) {
      this.loading = true
      this.error = null
      try {
        const res = await createSessionApi({ avatarId, title })
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
        this.messages = list.map((m) => ({
          id: m.id,
          role: m.role === 2 ? 'assistant' : 'user',
          content: m.content,
          time: this.formatTime(m.createTime),
          rawTime: m.createTime
        }))
      } catch (err) {
        this.error = err.message || '获取消息失败'
      } finally {
        this.loading = false
      }
    },
    async deleteSession(sessionId) {
      try {
        await deleteSessionApi(sessionId)
        this.sessions = this.sessions.filter((s) => s.id !== sessionId)
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
      if (!this.currentSessionId || this.streaming) return
      this.streaming = true
      this.error = null

      const userMessage = {
        id: Date.now(),
        role: 'user',
        content,
        time: this.formatTime(new Date().toISOString())
      }
      this.messages.push(userMessage)

      const assistantMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: '',
        time: this.formatTime(new Date().toISOString()),
        streaming: true
      }
      this.messages.push(assistantMessage)

      try {
        const reader = await streamChatApi(this.currentSessionId, content)
        await this.consumeStream(reader, assistantMessage)
      } catch (err) {
        this.error = err.message || '发送消息失败'
        assistantMessage.content = '抱歉，出现了错误：' + err.message
      } finally {
        this.streaming = false
        assistantMessage.streaming = false
      }
    },
    async consumeStream(reader, messageObj) {
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = 'message'

      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break

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
              messageObj.content = '抱歉，AI服务出错了：' + data
              return
            }

            if (data === '[DONE]') return

            messageObj.content += data
            currentEvent = 'message'
          }
        }
      } catch (err) {
        this.error = err.message || '流读取错误'
        messageObj.content += '\n[连接错误: ' + err.message + ']'
      }
    },
    formatTime(isoString) {
      if (!isoString) return ''
      const date = new Date(isoString)
      if (isNaN(date.getTime())) {
        const parts = isoString.split('T')
        if (parts.length === 2) return parts[1].substring(0, 5)
        return isoString
      }
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    },
    resetState() {
      this.sessions = []
      this.currentSessionId = null
      this.messages = []
      this.loading = false
      this.streaming = false
      this.error = null
      this.currentAvatar = null
    }
  }
})