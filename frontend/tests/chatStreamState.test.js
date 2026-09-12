import test from 'node:test'
import assert from 'node:assert/strict'
import { parseSseErrorPayload, cancelActiveAssistant, consumeChatStream } from '../src/utils/chatStreamState.js'
import { CHAT_MESSAGE_STATUS } from '../src/utils/chatMessageState.js'

test('parseSseErrorPayload extracts a human-readable JSON message', () => {
  assert.equal(parseSseErrorPayload('{"code":"AI_ERROR","message":"服务暂时不可用"}'), '服务暂时不可用')
})

test('parseSseErrorPayload preserves non-JSON and missing-message payloads', () => {
  assert.equal(parseSseErrorPayload('网络错误'), '网络错误')
  assert.equal(parseSseErrorPayload('{"code":"ERR"}'), '{"code":"ERR"}')
  assert.equal(parseSseErrorPayload('{"message":""}'), '{"message":""}')
})

test('cancelActiveAssistant aborts and cancels the active assistant', () => {
  let aborted = false
  const messages = [
    { role: 'assistant', status: CHAT_MESSAGE_STATUS.COMPLETED },
    { role: 'assistant', status: CHAT_MESSAGE_STATUS.STREAMING, streaming: true }
  ]
  assert.equal(cancelActiveAssistant(messages, { abort: () => { aborted = true } }), true)
  assert.equal(aborted, true)
  assert.equal(messages[1].status, CHAT_MESSAGE_STATUS.CANCELLED)
  assert.equal(messages[1].streaming, false)
})

test('cancelActiveAssistant returns false without a controller', () => {
  assert.equal(cancelActiveAssistant([], null), false)
})

test('consumeChatStream preserves leading whitespace in text chunks', async () => {
  const chunks = ['event: message\ndata:  world\n\n', 'data: [DONE]\n\n']
  const reader = { read: async () => chunks.length ? { done: false, value: new TextEncoder().encode(chunks.shift()) } : { done: true } }
  const message = { role: 'assistant', status: CHAT_MESSAGE_STATUS.PENDING, content: '' }
  await consumeChatStream(reader, message)
  assert.equal(message.content, ' world')
})
