import test from 'node:test'
import assert from 'node:assert/strict'
import {
  accentForWorld,
  applyWorldDraft,
  buildParticipantPayload,
  buildWorldSemanticPayload,
  createWorldForm,
  eligibleAiCharacters,
  moveRosterItem,
  snapshotSummary,
  visibleWorldParticipants
} from '../src/utils/worldBuilder.js'

test('draft is editable and sourceDescription always comes from user input', () => {
  const form = createWorldForm()
  applyWorldDraft(form, {
    name: 'AI名字', background: '背景', rules: '规则', atmosphere: '神秘', scene: '雨夜',
    sourceDescription: '伪造描述'
  }, '用户原始描述')
  form.name = '用户修改后的名字'
  const payload = buildWorldSemanticPayload(form)
  assert.equal(payload.name, '用户修改后的名字')
  assert.equal(payload.sourceDescription, '用户原始描述')
})

test('only available AI Characters can be selected', () => {
  const result = eligibleAiCharacters([
    { id: 1, characterType: 'AI', status: 1 },
    { id: 2, characterType: 'USER', status: 1 },
    { id: 3, characterType: 'AI', status: 0 },
    { id: 4, characterType: 'AI', deleted: true }
  ])
  assert.deepEqual(result.map(item => item.id), [1])
})

test('roster movement and payload produce continuous displayOrder', () => {
  const alpha = { id: 10, name: 'Alpha' }
  const beta = { id: 20, name: 'Beta' }
  const moved = moveRosterItem([alpha, beta], 1, -1)
  assert.deepEqual(moved.map(item => item.id), [20, 10])
  assert.deepEqual(buildParticipantPayload(moved), [
    { characterId: 20, participantType: 'AI', displayOrder: 0 },
    { characterId: 10, participantType: 'AI', displayOrder: 1 }
  ])
})

test('duplicate roster entries are removed and snapshots have safe text fallback', () => {
  const character = { id: 1, name: 'A', identity: '', corePersonality: '冷静' }
  assert.equal(buildParticipantPayload([character, character]).length, 1)
  assert.equal(snapshotSummary(character), '冷静')
  assert.equal(snapshotSummary(null), '暂无角色摘要')
})

test('World visual accent is stable and limited to local CSS themes', () => {
  assert.equal(accentForWorld(7), accentForWorld(7))
  assert.match(accentForWorld(7), /^accent-(sky|mint|peach)$/)
})

test('World presentation never renders more than four participants', () => {
  const participants = Array.from({ length: 12 }, (_, index) => ({ id: index + 1 }))
  assert.deepEqual(visibleWorldParticipants({ participants }).map(item => item.id), [1, 2, 3, 4])
})
