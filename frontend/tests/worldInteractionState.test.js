import test from 'node:test'
import assert from 'node:assert/strict'
import {
  clearPendingInteraction,
  createWorldRequestId,
  currentParticipantId,
  dataOf,
  errorCodeOf,
  interactionStorageKey,
  isTerminalRound,
  mergeEvents,
  mergeTimeline,
  normalizeWorldInput,
  participantForEvent,
  pollDelay,
  readPendingInteraction,
  worldInteractionErrorMessage,
  writePendingInteraction
} from '../src/utils/worldInteraction.js'

function memoryStorage() {
  const values = new Map()
  return {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key)
  }
}

test('request id is stable, UUID based and input is normalized', () => {
  const crypto = { randomUUID: () => '123e4567-e89b-12d3-a456-426614174000' }
  assert.equal(createWorldRequestId(crypto), '123e4567-e89b-12d3-a456-426614174000')
  assert.equal(normalizeWorldInput('  hello world  '), 'hello world')
})

test('Result data is unwrapped by property presence without replacing falsy values', () => {
  assert.equal(dataOf({ code: 200, msg: 'success', data: null }), null)
  assert.equal(dataOf({ code: 200, msg: 'success', data: false }), false)
  assert.equal(dataOf({ code: 200, msg: 'success', data: 0 }), 0)
  assert.equal(dataOf({ code: 200, msg: 'success', data: '' }), '')
  assert.deepEqual(dataOf({ code: 200, data: { id: 7 } }), { id: 7 })
  assert.deepEqual(dataOf({ code: 200, data: [] }), [])
})

test('non-Result values remain compatible and are returned unchanged', () => {
  const direct = { id: 9, status: 'PENDING' }
  assert.equal(dataOf(direct), direct)
  assert.equal(dataOf(null), null)
  assert.equal(dataOf(false), false)
  assert.equal(dataOf(0), 0)
  assert.equal(dataOf(''), '')
})

test('pending submission survives refresh with the same request id and round id', () => {
  const storage = memoryStorage()
  const key = interactionStorageKey(7, 11)
  const descriptor = { requestId: 'stable-id', userInput: 'hello', roundId: 31, phase: 'DISPATCHING', createdAt: 10 }
  writePendingInteraction(storage, key, descriptor)
  assert.deepEqual(readPendingInteraction(storage, key), descriptor)
  clearPendingInteraction(storage, key)
  assert.equal(readPendingInteraction(storage, key), null)
})

test('timeline merges rounds and events by id and displays old to new', () => {
  const current = [{ round: { id: 20, status: 'RUNNING' }, events: [{ id: 2, sequenceNo: 2, content: 'old' }] }]
  const incoming = [
    { round: { id: 10, status: 'COMPLETED' }, events: [{ id: 1, sequenceNo: 1 }] },
    { round: { id: 20, status: 'COMPLETED' }, events: [{ id: 2, sequenceNo: 2, content: 'new' }, { id: 3, sequenceNo: 1 }] }
  ]
  const merged = mergeTimeline(current, incoming)
  assert.deepEqual(merged.map(item => item.round.id), [10, 20])
  assert.equal(merged[1].round.status, 'COMPLETED')
  assert.deepEqual(merged[1].events.map(event => event.id), [3, 2])
  assert.equal(merged[1].events[1].content, 'new')
})

test('events deduplicate by id and sort by sequenceNo then id', () => {
  const events = mergeEvents(
    [{ id: 4, sequenceNo: 3 }, { id: 2, sequenceNo: 2, content: 'before' }],
    [{ id: 3, sequenceNo: 2 }, { id: 2, sequenceNo: 2, content: 'after' }]
  )
  assert.deepEqual(events.map(event => event.id), [2, 3, 4])
  assert.equal(events[0].content, 'after')
})

test('AI event participant maps by WorldParticipant id, not sourceCharacterId', () => {
  const participants = [{ id: 8, sourceCharacterId: 99, character: { name: 'A' } }]
  assert.equal(participantForEvent(participants, { participantId: 8 })?.character.name, 'A')
  assert.equal(participantForEvent(participants, { participantId: 99 }), null)
})

test('current participant advances as sequential AI events appear', () => {
  const participants = [{ id: 8, displayOrder: 0 }, { id: 9, displayOrder: 1 }]
  assert.equal(currentParticipantId(participants, [], 'RUNNING'), 8)
  assert.equal(currentParticipantId(participants, [{ participantId: 8 }], 'RUNNING'), 9)
  assert.equal(currentParticipantId(participants, [], 'COMPLETED'), null)
})

test('only backend-defined error codes receive stable Chinese mappings', () => {
  assert.equal(worldInteractionErrorMessage('WORLD_EXECUTION_BUSY').includes('本轮已保留'), true)
  assert.equal(worldInteractionErrorMessage('LLM_RATE_LIMIT').includes('AI服务繁忙'), true)
  assert.equal(worldInteractionErrorMessage('NOT_A_REAL_CODE', 'fallback'), 'fallback')
  assert.equal(errorCodeOf({ response: { data: { msg: 'WORLD_ROUND_ACTIVE' } } }), 'WORLD_ROUND_ACTIVE')
})

test('terminal states and polling backoff follow the interaction lifecycle', () => {
  assert.equal(isTerminalRound('COMPLETED'), true)
  assert.equal(isTerminalRound('PARTIAL_FAILED'), true)
  assert.equal(isTerminalRound('RUNNING'), false)
  assert.equal(pollDelay(0, 0, false), 1000)
  assert.equal(pollDelay(70000, 0, false), 5000)
  assert.equal(pollDelay(0, 9, false), 15000)
  assert.equal(pollDelay(0, 0, true), 15000)
})
