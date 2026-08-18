import test from 'node:test'
import assert from 'node:assert/strict'
import { AvatarExpressionAdapter } from '../src/avatar/AvatarExpressionAdapter.js'

function manager(names) {
  const values = new Map()
  return {
    values,
    getExpression: (name) => names.includes(name) ? { expressionName: name } : null,
    setValue: (name, value) => values.set(name, value)
  }
}

test('adapter safely controls blink, mouth and emotion expressions', () => {
  const expressionManager = manager(['blink', 'aa', 'happy'])
  const adapter = new AvatarExpressionAdapter({ expressionManager })
  assert.equal(adapter.setBlink(2), true)
  assert.equal(adapter.setMouthOpen(0.4), true)
  assert.equal(adapter.setEmotion('happy', 0.8), true)
  assert.equal(expressionManager.values.get('blink'), 1)
  assert.equal(expressionManager.values.get('aa'), 0.4)
  assert.equal(expressionManager.values.get('happy'), 0.8)
})

test('unsupported expressions and missing manager never throw', () => {
  const adapter = new AvatarExpressionAdapter({})
  assert.doesNotThrow(() => adapter.setBlink(1))
  assert.equal(adapter.setBlink(1), false)
  assert.equal(adapter.setMouthOpen(1), false)
  assert.equal(adapter.setEmotion('sad', 1), false)
  assert.equal(adapter.resetExpressions(), false)
})

test('dispose resets supported expressions and blocks later writes', () => {
  const expressionManager = manager(['blink'])
  const adapter = new AvatarExpressionAdapter({ expressionManager })
  adapter.setBlink(1)
  adapter.dispose()
  assert.equal(expressionManager.values.get('blink'), 0)
  assert.equal(adapter.setBlink(1), false)
})
