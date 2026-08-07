import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildPersonalityUpdatePayload,
  createPersonalityEditForm
} from '../src/utils/personalityForm.js'

test('edit form exposes only the five allowed personality fields', () => {
  const form = createPersonalityEditForm({
    id: 99,
    avatarId: 54,
    corePersonality: '温柔',
    identity: '虚拟伴侣',
    languageStyle: '自然',
    hobbies: '音乐',
    relationship: '朋友',
    personalitySnapshot: 'forbidden'
  })

  assert.deepEqual(Object.keys(form), [
    'corePersonality', 'identity', 'languageStyle', 'hobbies', 'relationship'
  ])
})

test('update payload locks avatar id and never sends personality or snapshot ids', () => {
  const payload = buildPersonalityUpdatePayload(
    '54',
    { id: 99, avatarId: 999, name: '小学的人格', templateType: 1 },
    {
      avatarId: 888,
      personalityId: 777,
      corePersonality: ' 活泼开朗 ',
      identity: 'AI虚拟伴侣',
      languageStyle: '自然',
      hobbies: '音乐',
      relationship: '朋友',
      personalitySnapshot: 'forbidden'
    }
  )

  assert.equal(payload.avatarId, 54)
  assert.equal(payload.corePersonality, '活泼开朗')
  assert.equal('personalityId' in payload, false)
  assert.equal('personalitySnapshot' in payload, false)
})
