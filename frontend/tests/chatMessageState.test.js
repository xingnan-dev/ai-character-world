import test from 'node:test'
import assert from 'node:assert/strict'
import {
  CHAT_MESSAGE_STATUS,
  createChatRequestId,
  findRetryContent,
  isMessageActive,
  isMessageRetryable,
  mapChatMessage,
  messageStatusMeta
} from '../src/utils/chatMessageState.js'

test('request ids are present and unique', () => {
  const first = createChatRequestId()
  const second = createChatRequestId()
  assert.ok(first)
  assert.notEqual(first, second)
})

test('server lifecycle status is restored into frontend messages', () => {
  const message = mapChatMessage({
    id: 10, sessionId: 2, requestId: 'request-1', role: 2, content: 'partial',
    status: CHAT_MESSAGE_STATUS.INTERRUPTED,
    errorCode: 'SERVICE_RESTART_INTERRUPTED', createTime: '2026-08-10T10:00:00'
  }, () => '10:00')
  assert.equal(message.role, 'assistant')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.INTERRUPTED)
  assert.equal(message.errorCode, 'SERVICE_RESTART_INTERRUPTED')
  assert.equal(message.streaming, false)
  assert.equal(messageStatusMeta(message.status).label, '回复中断')
})

test('active and retryable lifecycle states are classified correctly', () => {
  assert.equal(isMessageActive(CHAT_MESSAGE_STATUS.PENDING), true)
  assert.equal(isMessageActive(CHAT_MESSAGE_STATUS.STREAMING), true)
  assert.equal(isMessageActive(CHAT_MESSAGE_STATUS.COMPLETED), false)
  assert.equal(isMessageRetryable({ role: 'assistant', status: CHAT_MESSAGE_STATUS.FAILED }), true)
  assert.equal(isMessageRetryable({ role: 'assistant', status: CHAT_MESSAGE_STATUS.COMPLETED }), false)
})

test('retry content comes from the user message with the same request id', () => {
  const messages = [
    { role: 'user', requestId: 'request-1', content: 'hello' },
    { role: 'assistant', requestId: 'request-1', status: CHAT_MESSAGE_STATUS.FAILED }
  ]
  assert.equal(findRetryContent(messages, messages[1]), 'hello')
})
