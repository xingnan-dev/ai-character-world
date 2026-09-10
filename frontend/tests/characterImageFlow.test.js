import test from 'node:test'
import assert from 'node:assert/strict'
import { createCharacterImageFlow } from '../src/utils/characterImageFlow.js'

function deferred() { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
function flow(overrides = {}) {
  return createCharacterImageFlow({
    generateImage: async () => ({ data: { id: 7, status: 'SUCCEEDED', imageUrl: '/new.png' } }),
    confirmImage: async () => {}, createCharacter: async () => ({ data: { id: 11 } }),
    createRequestId: () => 'request-1', ...overrides
  })
}

test('blocks duplicate generation while active and recovers after failure', async () => {
  const pending = deferred(); let calls = 0
  const subject = flow({ generateImage: async () => { calls++; return pending.promise } })
  const first = subject.generate('portrait')
  assert.equal(subject.generating, true)
  assert.equal(await subject.generate('portrait'), null)
  assert.equal(calls, 1)
  pending.reject(new Error('timeout'))
  await assert.rejects(first, /timeout/)
  assert.equal(subject.generating, false)
})

test('candidate image does not overwrite an already saved image', async () => {
  const savedImageUrl = '/saved.png'
  const subject = flow()
  await subject.generate('portrait')
  assert.equal(savedImageUrl, '/saved.png')
  assert.equal(subject.candidateImageUrl, '/new.png')
})

test('confirms by generation record id', async () => {
  let confirmation
  const subject = flow({ confirmImage: async payload => { confirmation = payload } })
  await subject.generate('portrait')
  await subject.createAndConfirm({ name: 'A' })
  assert.deepEqual(confirmation, { generationId: 7, characterId: 11 })
})

test('retry after binding failure does not create the character twice', async () => {
  let creates = 0, confirms = 0
  const subject = flow({
    createCharacter: async () => { creates++; return { data: { id: 11 } } },
    confirmImage: async () => { confirms++; if (confirms === 1) throw new Error('bind failed') }
  })
  await subject.generate('portrait')
  await assert.rejects(subject.createAndConfirm({ name: 'A' }), /bind failed/)
  assert.equal(await subject.createAndConfirm({ name: 'A' }), 11)
  assert.equal(creates, 1)
  assert.equal(confirms, 2)
})
