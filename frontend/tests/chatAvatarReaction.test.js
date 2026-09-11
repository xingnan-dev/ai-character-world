import test from 'node:test'
import assert from 'node:assert/strict'
import { CHAT_MESSAGE_STATUS } from '../src/utils/chatMessageState.js'
import { findRetryContent } from '../src/utils/chatMessageState.js'
import { cancelActiveAssistant, consumeChatStream } from '../src/utils/chatStreamState.js'

test('SSE consumption still streams and completes assistant messages without an avatar renderer', async () => {
  const chunks = [
    new TextEncoder().encode('data: 你好\n\n'),
    new TextEncoder().encode('data: [DONE]\n\n')
  ]
  const reader = { read: async () => chunks.length ? { done: false, value: chunks.shift() } : { done: true } }
  const message = { role: 'assistant', content: '', status: CHAT_MESSAGE_STATUS.PENDING }

  await consumeChatStream(reader, message)
  assert.equal(message.content, '你好')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.COMPLETED)
})

test('stop and retry behavior remains session-based for legacy Avatar conversations', async () => {
  let aborted = false
  const messages = [
    { role: 'user', requestId: 'legacy-request', content: '继续聊' },
    { role: 'assistant', requestId: 'legacy-request', status: CHAT_MESSAGE_STATUS.STREAMING, streaming: true }
  ]
  assert.equal(cancelActiveAssistant(messages, { abort: () => { aborted = true } }), true)
  assert.equal(aborted, true)
  assert.equal(messages[1].status, CHAT_MESSAGE_STATUS.CANCELLED)
  assert.equal(findRetryContent(messages, messages[1]), '继续聊')
})
