import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getMatchMessage,
  getParseSourceMessage,
  mapUnmatchedAttributes
} from '../src/utils/avatarMatchResult.js'

test('matched asset reports an exact model match', () => {
  assert.equal(getMatchMessage('MATCHED'), '模型完全匹配')
})

test('nearest asset reports the fallback model and maps unmatched fields', () => {
  assert.equal(
    getMatchMessage('NEAREST'),
    '当前资产库暂无完全匹配模型，已选择最接近模型'
  )
  assert.deepEqual(
    mapUnmatchedAttributes(['gender', 'hairColor', 'wingType', 'accessories']),
    ['性别', '发色', '翅膀', '配饰']
  )
})

test('rule fallback is disclosed while normal LLM parsing has no warning', () => {
  assert.equal(getParseSourceMessage('RULE_FALLBACK'), 'AI解析失败，本次使用规则解析')
  assert.equal(getParseSourceMessage('LLM'), '')
})

test('unmatched field mapping is null-safe and removes duplicates', () => {
  assert.deepEqual(mapUnmatchedAttributes(null), [])
  assert.deepEqual(mapUnmatchedAttributes(['eyeColor', 'eyeColor', 'unknown']), ['瞳色', 'unknown'])
})
