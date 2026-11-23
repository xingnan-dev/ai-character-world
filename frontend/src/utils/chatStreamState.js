import { CHAT_MESSAGE_STATUS } from './chatMessageState.js'

export function parseSseErrorPayload(data) {
  const raw = typeof data === 'string' ? data : String(data ?? '')
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed.message === 'string' && parsed.message.trim()) return parsed.message.trim()
  } catch {
    // Keep non-JSON SSE errors as-is.
  }
  return raw
}

export async function consumeChatStream(reader, message, onError = () => {}) {
  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = 'message'
  let dataLines = []

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      if (message.status !== CHAT_MESSAGE_STATUS.FAILED) message.status = CHAT_MESSAGE_STATUS.COMPLETED
      return
    }
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    for (let line of lines) {
      if (line.endsWith('\r')) line = line.slice(0, -1)
      if (line !== '') {
        if (line.startsWith('event:')) currentEvent = line.slice(6).trim()
        if (line.startsWith('data:')) {
          const value = line.slice(5)
          dataLines.push(value.startsWith(' ') ? value.slice(1) : value)
        }
        continue
      }
      if (!dataLines.length) { currentEvent = 'message'; continue }
      const data = dataLines.join('\n')
      dataLines = []
      if (currentEvent === 'error') {
        const messageText = parseSseErrorPayload(data)
        onError(messageText)
        message.status = CHAT_MESSAGE_STATUS.FAILED
        message.errorMessage = messageText
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
}

export function cancelActiveAssistant(messages, abortController) {
  if (!abortController) return false
  abortController.abort()
  const activeMessage = [...messages].reverse().find((message) =>
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
}
