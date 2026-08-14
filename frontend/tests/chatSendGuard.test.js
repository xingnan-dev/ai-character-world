import test from 'node:test'
import assert from 'node:assert/strict'
import { createChatSendGuard } from '../src/utils/chatSendGuard.js'

test('blocks a second send while the same session is active', () => {
  const guard = createChatSendGuard()
  assert.equal(guard.tryAcquire(11), true)
  assert.equal(guard.tryAcquire(11), false)
  assert.equal(guard.isActive(11), true)
})

test('allows sending again after completion or failure releases the session', () => {
  const guard = createChatSendGuard()
  assert.equal(guard.tryAcquire(11), true)
  guard.release(11)
  assert.equal(guard.tryAcquire(11), true)
})

test('tracks different sessions independently', () => {
  const guard = createChatSendGuard()
  assert.equal(guard.tryAcquire(11), true)
  assert.equal(guard.tryAcquire(12), true)
})
