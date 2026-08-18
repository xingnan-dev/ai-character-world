import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveChatAvatarBehaviorState } from '../src/avatar/chatAvatarBehaviorState.js'
import { CHAT_MESSAGE_STATUS } from '../src/utils/chatMessageState.js'

const assistant = (status) => ({ role: 'assistant', status })

test('empty and user-only messages resolve to idle', () => {
  assert.equal(resolveChatAvatarBehaviorState([]), 'idle')
  assert.equal(resolveChatAvatarBehaviorState([{ role: 'user', status: CHAT_MESSAGE_STATUS.COMPLETED }]), 'idle')
})

test('pending assistant resolves to thinking', () => {
  assert.equal(resolveChatAvatarBehaviorState([assistant(CHAT_MESSAGE_STATUS.PENDING)]), 'thinking')
})

test('streaming assistant resolves to talking', () => {
  assert.equal(resolveChatAvatarBehaviorState([assistant(CHAT_MESSAGE_STATUS.STREAMING)]), 'talking')
})

test('terminal assistant states resolve to idle', () => {
  for (const status of [
    CHAT_MESSAGE_STATUS.COMPLETED,
    CHAT_MESSAGE_STATUS.FAILED,
    CHAT_MESSAGE_STATUS.CANCELLED,
    CHAT_MESSAGE_STATUS.INTERRUPTED
  ]) {
    assert.equal(resolveChatAvatarBehaviorState([assistant(status)]), 'idle')
  }
})

test('latest assistant message determines behavior', () => {
  assert.equal(resolveChatAvatarBehaviorState([
    assistant(CHAT_MESSAGE_STATUS.STREAMING),
    { role: 'user', status: CHAT_MESSAGE_STATUS.COMPLETED },
    assistant(CHAT_MESSAGE_STATUS.COMPLETED)
  ]), 'idle')
  assert.equal(resolveChatAvatarBehaviorState([
    assistant(CHAT_MESSAGE_STATUS.COMPLETED),
    assistant(CHAT_MESSAGE_STATUS.PENDING)
  ]), 'thinking')
})
