import { CHAT_MESSAGE_STATUS } from '../utils/chatMessageState.js'

export function resolveChatAvatarBehaviorState(messages) {
  if (!Array.isArray(messages) || messages.length === 0) return 'idle'

  const latestAssistant = [...messages]
    .reverse()
    .find((message) => message?.role === 'assistant')

  if (latestAssistant?.status === CHAT_MESSAGE_STATUS.PENDING) return 'thinking'
  if (latestAssistant?.status === CHAT_MESSAGE_STATUS.STREAMING) return 'talking'
  return 'idle'
}

export default resolveChatAvatarBehaviorState
