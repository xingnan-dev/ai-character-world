import test from 'node:test'
import assert from 'node:assert/strict'
import {
  AVATAR_GENERATE_TIMEOUT_MS,
  createAvatarGenerationGuard
} from '../src/utils/avatarGenerationGuard.js'

test('avatar generation uses a dedicated timeout longer than the global request timeout', () => {
  assert.equal(AVATAR_GENERATE_TIMEOUT_MS, 60000)
})

test('duplicate avatar generation is blocked while a request is active', () => {
  const guard = createAvatarGenerationGuard()
  let requestCount = 0
  if (guard.tryAcquire()) requestCount += 1
  if (guard.tryAcquire()) requestCount += 1
  assert.equal(requestCount, 1)
  assert.equal(guard.isActive(), true)
})

test('avatar generation can run again after completion or failure releases the guard', () => {
  const guard = createAvatarGenerationGuard()
  assert.equal(guard.tryAcquire(), true)
  guard.release()
  assert.equal(guard.tryAcquire(), true)
})
