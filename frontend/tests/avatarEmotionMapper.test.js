import test from 'node:test'
import assert from 'node:assert/strict'
import { mapReplyToReaction } from '../src/avatar/AvatarEmotionMapper.js'

test('empty and non-string replies have no reaction', () => {
  assert.equal(mapReplyToReaction('  '), null)
  assert.equal(mapReplyToReaction(null), null)
  assert.equal(mapReplyToReaction({}), null)
})

test('maps happy, sad and surprised replies deterministically', () => {
  assert.equal(mapReplyToReaction('哈哈，太好了！').emotion, 'happy')
  assert.equal(mapReplyToReaction('抱歉，这确实很遗憾。').emotion, 'sad')
  const surprised = mapReplyToReaction('哇，居然是真的！')
  assert.equal(surprised.emotion, 'surprised')
  assert.equal(surprised.action, 'nod')
})

test('maps affirmation and rejection actions', () => {
  assert.equal(mapReplyToReaction('好的，没问题。').action, 'nod')
  assert.equal(mapReplyToReaction('不行，这样做是错误的。').action, 'shakeHead')
})

test('emotion priority is stable and sad wins over surprise', () => {
  const reaction = mapReplyToReaction('抱歉，没想到居然发生了这种情况。')
  assert.equal(reaction.emotion, 'sad')
})

test('unmatched and mixed-case text is safe', () => {
  assert.equal(mapReplyToReaction('今天气温二十度。'), null)
  assert.equal(mapReplyToReaction('OK，哈哈 HAPPY DAY').emotion, 'happy')
})
