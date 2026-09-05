import test from 'node:test'
import assert from 'node:assert/strict'
import { buildWorldTimelineViewModel } from '../src/utils/worldTimelineViewModel.js'

const userSnapshot = (patch = {}) => ({
  snapshotVersion: 1,
  sourceCharacterId: 41,
  characterType: 'USER',
  name: '历史中的我',
  avatarColor: '#123abc',
  imageUrl: '/avatars/me.png',
  ...patch
})

const participant = (patch = {}) => ({
  id: 7,
  participantType: 'AI',
  sourceCharacterId: 99,
  displayOrder: 0,
  character: {
    snapshotVersion: 1,
    sourceCharacterId: 99,
    characterType: 'AI',
    name: '快照角色',
    avatarColor: '#abcdef',
    imageUrl: null
  },
  ...patch
})

const item = (events, roundPatch = {}) => ({
  round: {
    id: 10,
    status: 'COMPLETED',
    userCharacterId: 41,
    userCharacter: userSnapshot(),
    ...roundPatch
  },
  events
})

test('USER_MESSAGE uses only the same round userCharacter snapshot', () => {
  const input = item([{ id: 1, eventType: 'USER_MESSAGE', content: 'hello' }])
  input.userCharacter = userSnapshot({ name: '外层实时身份' })
  const [view] = buildWorldTimelineViewModel([input], [])

  assert.equal(view.events[0].kind, 'USER')
  assert.equal(view.events[0].actor.name, '历史中的我')
  assert.equal(view.events[0].actor.initial, '历')
  assert.equal(view.events[0].actor.snapshotSource, 'ROUND')
})

test('missing or incorrectly typed user snapshot uses field-safe fallback', () => {
  for (const invalid of [null, 'serialized-json', [], { characterType: 'AI', name: 'not-user' }]) {
    const [view] = buildWorldTimelineViewModel([
      item([{ id: 1, eventType: 'USER_MESSAGE' }], { userCharacter: invalid, userCharacterId: 88 })
    ], [])
    assert.deepEqual(view.events[0].actor, {
      kind: 'USER',
      name: '用户角色',
      initial: '我',
      avatarColor: '#f4b293',
      imageUrl: null,
      snapshotSource: 'FALLBACK',
      sourceCharacterId: null,
      participantId: null
    })
  }
})

test('valid user snapshot falls back per missing display field', () => {
  const [view] = buildWorldTimelineViewModel([
    item([{ id: 1, eventType: 'USER_MESSAGE' }], {
      userCharacter: userSnapshot({ name: ' ', avatarColor: '#12345', imageUrl: '' })
    })
  ], [])
  const actor = view.events[0].actor
  assert.equal(actor.name, '用户角色')
  assert.equal(actor.initial, '我')
  assert.equal(actor.avatarColor, '#f4b293')
  assert.equal(actor.imageUrl, null)
  assert.equal(actor.snapshotSource, 'ROUND')
})

test('AI_MESSAGE maps participantId to participant.id and never to sourceCharacterId', () => {
  const events = [
    { id: 2, eventType: 'AI_MESSAGE', participantId: 7, status: 'COMPLETED' },
    { id: 3, eventType: 'AI_MESSAGE', participantId: 99, status: 'COMPLETED' }
  ]
  const [view] = buildWorldTimelineViewModel([item(events)], [participant()])

  assert.equal(view.events[0].actor.name, '快照角色')
  assert.equal(view.events[0].actor.participantId, 7)
  assert.equal(view.events[0].actor.sourceCharacterId, 99)
  assert.equal(view.events[1].actor.name, '未知角色')
  assert.equal(view.events[1].actor.snapshotSource, 'FALLBACK')
})

test('missing participant and partial participant snapshot fall back per field', () => {
  const events = [
    { id: 2, eventType: 'AI_MESSAGE', participantId: 404 },
    { id: 3, eventType: 'AI_MESSAGE', participantId: 7 }
  ]
  const partial = participant({ character: { avatarColor: '#0af', imageUrl: '' } })
  const [view] = buildWorldTimelineViewModel([item(events)], [partial])

  assert.equal(view.events[0].actor.name, '未知角色')
  assert.equal(view.events[0].actor.initial, '角')
  assert.equal(view.events[0].actor.avatarColor, '#83cdf3')
  assert.equal(view.events[1].actor.name, '未知角色')
  assert.equal(view.events[1].actor.initial, '角')
  assert.equal(view.events[1].actor.avatarColor, '#0af')
  assert.equal(view.events[1].actor.imageUrl, null)
  assert.equal(view.events[1].actor.snapshotSource, 'PARTICIPANT')
})

test('persisted participant snapshot remains displayable without a live source Character', () => {
  const deletedSourceSnapshot = participant({
    sourceCharacterId: 501,
    character: { name: '已删除源角色的快照', avatarColor: '#334455' }
  })
  const [view] = buildWorldTimelineViewModel([
    item([{ id: 2, eventType: 'AI_MESSAGE', participantId: 7 }])
  ], [deletedSourceSnapshot])

  assert.equal(view.events[0].actor.name, '已删除源角色的快照')
  assert.equal(view.events[0].actor.snapshotSource, 'PARTICIPANT')
})

test('unknown event type is not presented as AI', () => {
  const [view] = buildWorldTimelineViewModel([
    item([{ id: 4, eventType: 'SYSTEM_MESSAGE', participantId: 7, content: 'notice' }])
  ], [participant()])

  assert.equal(view.events[0].kind, 'UNKNOWN')
  assert.equal(view.events[0].actor.kind, 'UNKNOWN')
  assert.equal(view.events[0].actor.name, '未知事件')
})

test('failed AI event retains identity, status and error information', () => {
  const [view] = buildWorldTimelineViewModel([
    item([{ id: 5, eventType: 'AI_MESSAGE', participantId: 7, status: 'FAILED', errorCode: 'LLM_TIMEOUT' }])
  ], [participant()])

  assert.equal(view.events[0].actor.name, '快照角色')
  assert.equal(view.events[0].status, 'FAILED')
  assert.equal(view.events[0].errorCode, 'LLM_TIMEOUT')
})

test('WORLD_NO_AI_PARTICIPANT with no AI events is safe', () => {
  const [view] = buildWorldTimelineViewModel([
    item([], { status: 'FAILED', errorCode: 'WORLD_NO_AI_PARTICIPANT' })
  ], [])

  assert.equal(view.round.status, 'FAILED')
  assert.equal(view.round.errorCode, 'WORLD_NO_AI_PARTICIPANT')
  assert.deepEqual(view.events, [])
})

test('view model does not mutate DTOs and preserves round and event order', () => {
  const input = [
    item([
      { id: 9, sequenceNo: 3, eventType: 'AI_MESSAGE', participantId: 7 },
      { id: 8, sequenceNo: 2, eventType: 'AI_MESSAGE', participantId: 7 }
    ], { id: 20 }),
    item([{ id: 1, sequenceNo: 1, eventType: 'USER_MESSAGE' }], { id: 10 })
  ]
  const participants = [participant()]
  const beforeItems = structuredClone(input)
  const beforeParticipants = structuredClone(participants)

  const view = buildWorldTimelineViewModel(input, participants)

  assert.deepEqual(input, beforeItems)
  assert.deepEqual(participants, beforeParticipants)
  assert.deepEqual(view.map(entry => entry.round.id), [20, 10])
  assert.deepEqual(view[0].events.map(event => event.id), [9, 8])
  assert.notEqual(view[0], input[0])
  assert.notEqual(view[0].events[0], input[0].events[0])
})
