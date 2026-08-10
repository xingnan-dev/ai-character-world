export const CHAT_MESSAGE_STATUS = Object.freeze({
  PENDING: 0,
  STREAMING: 1,
  COMPLETED: 2,
  FAILED: 3,
  CANCELLED: 4,
  INTERRUPTED: 5
})

const STATUS_META = Object.freeze({
  0: { label: '等待回复', type: 'info' },
  1: { label: '正在回复', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '回复失败', type: 'danger' },
  4: { label: '已停止', type: 'warning' },
  5: { label: '回复中断', type: 'warning' }
})

export function createChatRequestId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `chat-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function messageStatusMeta(status) {
  return STATUS_META[status] || { label: '状态未知', type: 'info' }
}

export function isMessageActive(status) {
  return status === CHAT_MESSAGE_STATUS.PENDING || status === CHAT_MESSAGE_STATUS.STREAMING
}

export function isMessageRetryable(message) {
  return message?.role === 'assistant' && [
    CHAT_MESSAGE_STATUS.FAILED,
    CHAT_MESSAGE_STATUS.CANCELLED,
    CHAT_MESSAGE_STATUS.INTERRUPTED
  ].includes(message.status)
}

export function mapChatMessage(message, formatTime = (value) => value || '') {
  const status = message.status ?? CHAT_MESSAGE_STATUS.COMPLETED
  return {
    id: message.id,
    sessionId: message.sessionId,
    requestId: message.requestId,
    role: message.role === 2 ? 'assistant' : 'user',
    content: message.content || '',
    emotion: message.emotion,
    status,
    errorCode: message.errorCode,
    errorMessage: message.errorMessage,
    streaming: status === CHAT_MESSAGE_STATUS.STREAMING,
    time: formatTime(message.createTime),
    rawTime: message.createTime
  }
}

export function findRetryContent(messages, assistantMessage) {
  if (!isMessageRetryable(assistantMessage) || !assistantMessage.requestId) return null
  return messages.find((message) =>
    message.role === 'user' && message.requestId === assistantMessage.requestId
  )?.content || null
}
