import test from 'node:test'
import assert from 'node:assert/strict'
import {
  personalityIdentity,
  personalityInterests,
  personalityTags,
  personalityText
} from '../src/utils/avatarProfile.js'

test('missing backend personality is shown as unset instead of a fabricated default', () => {
  assert.equal(personalityText(null), '未设置人格')
  assert.equal(personalityIdentity(undefined), '未设置人格')
  assert.equal(personalityInterests({}), '未设置人格')
  assert.deepEqual(personalityTags(null), ['未设置人格'])
})

test('profile display uses personality data returned by the backend', () => {
  const personality = {
    corePersonality: '冷静、理性',
    identity: '研究助手',
    hobbies: '阅读、分析'
  }
  assert.equal(personalityText(personality), '冷静、理性')
  assert.equal(personalityIdentity(personality), '研究助手')
  assert.equal(personalityInterests(personality), '阅读、分析')
  assert.deepEqual(personalityTags(personality), ['冷静', '理性'])
})
