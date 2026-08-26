import test from 'node:test'
import assert from 'node:assert/strict'
import {
  applyCharacterDraft,
  buildCharacterCreatePayload,
  buildCharacterUpdatePayload,
  createCharacterForm
} from '../src/utils/characterDraft.js'

test('AI draft populates every editable character field', () => {
  const form = createCharacterForm()
  applyCharacterDraft(form, {
    characterType: 'USER',
    name: '林澈',
    age: 28,
    identity: '研究员',
    corePersonality: '冷静',
    currentGoal: '探索未知',
    biography: '角色背景',
    relationshipToUser: '伙伴',
    speakingStyle: '简洁自然',
    profile: {
      values: ['诚实'], likes: ['阅读'], dislikes: ['欺骗'], interests: ['宇宙'],
      fears: ['失败'], secrets: ['秘密'], behaviorTendencies: ['先分析']
    }
  })

  assert.equal(form.characterType, 'USER')
  assert.equal(form.name, '林澈')
  assert.equal(form.profile.values, '诚实')
  assert.equal(form.profile.behaviorTendencies, '先分析')
})

test('create payload uses user edited form instead of original LLM draft', () => {
  const originalDraft = {
    characterType: 'AI', name: '原始名字', identity: '原始身份',
    profile: { values: ['原始价值观'] }
  }
  const form = createCharacterForm(originalDraft)
  form.name = '用户修改后的名字'
  form.identity = '用户修改后的身份'
  form.profile.values = '诚实\n成长'

  const payload = buildCharacterCreatePayload(form, '原始自然语言描述')

  assert.equal(payload.name, '用户修改后的名字')
  assert.equal(payload.identity, '用户修改后的身份')
  assert.deepEqual(payload.profile.values, ['诚实', '成长'])
  assert.equal(payload.sourceDescription, '原始自然语言描述')
})

test('character creation remains independent from Avatar and VRM', () => {
  const payload = buildCharacterCreatePayload(createCharacterForm({ name: '无模型角色' }))

  assert.equal(payload.visualType, 'INITIAL')
  assert.equal('avatarId' in payload, false)
  assert.equal('modelUrl' in payload, false)
  assert.equal('imageUrl' in payload, false)
})

test('profile text is normalized and limited to ten entries', () => {
  const form = createCharacterForm({ name: '测试角色' })
  form.profile.interests = Array.from({ length: 12 }, (_, index) => ` 兴趣${index + 1} `).join('\n')

  const payload = buildCharacterCreatePayload(form)

  assert.equal(payload.profile.interests.length, 10)
  assert.equal(payload.profile.interests[0], '兴趣1')
  assert.equal(payload.profile.interests[9], '兴趣10')
})

test('update payload contains only editable fields and excludes server ownership fields', () => {
  const form = createCharacterForm({ name: '可编辑角色', identity: '旧身份' })
  form.identity = '新身份'
  form.profile.likes = '咖啡\n音乐'

  const payload = buildCharacterUpdatePayload(form)

  assert.equal(payload.identity, '新身份')
  assert.deepEqual(payload.profile.likes, ['咖啡', '音乐'])
  assert.equal('id' in payload, false)
  assert.equal('userId' in payload, false)
  assert.equal('status' in payload, false)
  assert.equal('deleted' in payload, false)
})
