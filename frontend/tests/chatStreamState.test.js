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


test('multiline SSE data preserves blank paragraphs across network chunks', async () => {
  const chunks = ['data: first\r\ndata:\r\nda', 'ta:  second  \r\n\r', '\nevent: done\ndata: [DONE]\n\n']
  const reader = { read: async () => chunks.length ? { done: false, value: new TextEncoder().encode(chunks.shift()) } : { done: true } }
  const message = { content: '', status: CHAT_MESSAGE_STATUS.PENDING }
  await consumeChatStream(reader, message)
  assert.equal(message.content, 'first\n\n second  ')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.COMPLETED)
})


const encode = value => new TextEncoder().encode(value)
const readerFor = parts => ({ read: async () => parts.length ? { done: false, value: parts.shift() } : { done: true } })

test('multiple events in one read do not add separator newlines or decode text as JSON', async () => {
  const message = { content: '', status: CHAT_MESSAGE_STATUS.PENDING }
  const literal = JSON.stringify({ text: 'first\nsecond', blank: '  ' })
  await consumeChatStream(readerFor([encode('data: a\n\ndata: ' + literal + '\n\ndata: b\n\ndata: [DONE]\n\n')]), message)
  assert.equal(message.content, 'a' + literal + 'b')
})

test('JSON error escapes are decoded only by the error contract, not appended as body', async () => {
  const message = { content: 'saved prefix', status: CHAT_MESSAGE_STATUS.STREAMING }
  let error
  await consumeChatStream(readerFor([encode('event: error\ndata: ' + JSON.stringify({ message: 'first\nsecond' }) + '\n\n')]), message, value => { error = value })
  assert.equal(error, 'first\nsecond')
  assert.equal(message.content, 'saved prefix')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.FAILED)
})

test('last text remains streaming until a delayed completion event arrives', async () => {
  let finish
  let reads = 0
  const reader = { read: async () => ++reads === 1 ? { value: encode('data: body\n\n') }
    : new Promise(resolve => { finish = () => resolve({ value: encode('event: done\ndata: [DONE]\n\n') }) }) }
  const message = { content: '', status: CHAT_MESSAGE_STATUS.PENDING }
  const pending = consumeChatStream(reader, message)
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(message.content, 'body')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.STREAMING)
  finish(); await pending
  assert.equal(message.status, CHAT_MESSAGE_STATUS.COMPLETED)
})

test('network byte boundaries inside UTF8 and lines preserve exact content', async () => {
  const text = '中文\n\n  段落  '
  const wire = encode('data: ' + text.split('\n').join('\ndata: ') + '\n\ndata: [DONE]\n\n')
  const message = { content: '', status: CHAT_MESSAGE_STATUS.PENDING }
  await consumeChatStream(readerFor(Array.from(wire, byte => Uint8Array.of(byte))), message)
  assert.equal(message.content, text)
})

test('EOF discards an unterminated SSE event, retaining existing EOF status policy', async () => {
  const message = { content: '', status: CHAT_MESSAGE_STATUS.PENDING }
  await consumeChatStream(readerFor([encode('data: dispatched\n\ndata: not-dispatched')]), message)
  assert.equal(message.content, 'dispatched')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.COMPLETED)
})

test('stop interrupts pending reads without appending an incomplete event', async () => {
  const abort = new AbortController()
  const message = { role: 'assistant', content: '', status: CHAT_MESSAGE_STATUS.STREAMING }
  let reads = 0
  const reader = { read: () => ++reads === 1 ? Promise.resolve({ value: encode('data: partial') })
    : new Promise((_, reject) => abort.signal.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), {name:'AbortError'})))) }
  const pending = consumeChatStream(reader, message)
  await new Promise(resolve => setImmediate(resolve))
  cancelActiveAssistant([message], abort)
  await assert.rejects(pending, {name:'AbortError'})
  assert.equal(message.content, '')
  assert.equal(message.status, CHAT_MESSAGE_STATUS.CANCELLED)
})
