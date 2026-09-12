import test from 'node:test'
import assert from 'node:assert/strict'
import {
  imageAfterFailure,
  resolveAssistantPresentation,
  resolveMessagePresentation,
  resolveUserPresentation,
  resolveCharacterDetails
} from '../src/utils/chatSessionPresentation.js'

const oldSession = { id: 1, characterId: 10, characterName: '星澜', imageUrl: '/old.png', avatarColor: '#123456' }
const newSession = { id: 2, characterId: 10, characterName: '星澜', imageUrl: '/new.png', avatarColor: '#654321' }

test('character session and assistant messages use the frozen snapshot name and image', () => {
  assert.deepEqual(resolveAssistantPresentation(oldSession), {
    name: '星澜', imageUrl: '/old.png', avatarColor: '#123456', type: 'AI', entityKey: 'character-session-1'
  })
  assert.equal(resolveMessagePresentation(oldSession, 'assistant', {}).imageUrl, '/old.png')
})

test('old and new sessions keep their own image snapshots after a character image change', () => {
  assert.equal(resolveAssistantPresentation(oldSession).imageUrl, '/old.png')
  assert.equal(resolveAssistantPresentation(newSession).imageUrl, '/new.png')
  assert.equal(resolveAssistantPresentation(oldSession).imageUrl, '/old.png')
})

test('user messages use a frozen USER snapshot when present and otherwise fall back to the nickname', () => {
  const frozen = resolveUserPresentation({ userCharacter: {
    characterType: 'USER', sourceCharacterId: 20, name: '旅人', imageUrl: '/user-old.png', avatarColor: '#abcdef'
  } }, { id: 7, nickname: '当前用户' })
  assert.equal(frozen.imageUrl, '/user-old.png')
  assert.equal(frozen.name, '旅人')
  assert.equal(frozen.avatarColor, '#abcdef')
  assert.equal(frozen.entityKey, 20)

  const fallback = resolveUserPresentation(oldSession, { id: 7, nickname: '当前用户' })
  assert.equal(fallback.imageUrl, '')
  assert.equal(fallback.name, '当前用户')
  assert.equal(fallback.type, 'USER')
})

test('a non-USER userCharacter snapshot is ignored and falls back to the nickname', () => {
  const result = resolveUserPresentation({ userCharacter: {
    characterType: 'AI', sourceCharacterId: 99, name: 'AI角色', imageUrl: '/ai.png', avatarColor: '#000000'
  } }, { id: 7, nickname: '当前用户' })
  assert.equal(result.name, '当前用户')
  assert.equal(result.imageUrl, '')
  assert.equal(result.avatarColor, '')
})

test('failed images are removed so the avatar component renders its name-and-color fallback', () => {
  assert.equal(imageAfterFailure('/old.png'), '/old.png')
  assert.equal(imageAfterFailure('/old.png', '/old.png'), '')
})

test('legacy Avatar sessions degrade to their frozen name without requiring an image', () => {
  const legacy = resolveAssistantPresentation({ id: 9, avatarId: 3, avatarName: '旧人格', title: '历史会话' })
  assert.equal(legacy.name, '旧人格')
  assert.equal(legacy.imageUrl, '')
  assert.equal(legacy.entityKey, 'avatar-session-9')
})

test('character details keep only non-empty snapshot fields', () => {
  assert.deepEqual(resolveCharacterDetails({ identity: '侦探', corePersonality: '冷静', currentGoal: null, speakingStyle: '', relationshipToUser: '伙伴' }), [
    ['身份', '侦探'], ['性格', '冷静'], ['与你的关系', '伙伴']
  ])
  assert.deepEqual(resolveCharacterDetails({ identity: null, corePersonality: '', currentGoal: '  ', speakingStyle: undefined, relationshipToUser: null }), [])
  assert.deepEqual(resolveCharacterDetails(null), [])
})
