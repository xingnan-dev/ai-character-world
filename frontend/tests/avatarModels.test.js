import test from 'node:test'
import assert from 'node:assert/strict'
import { DEFAULT_MODEL, getModelUrlByName } from '../src/config/avatarModels.js'

test('only real base model identifiers map to bundled VRM files', () => {
  assert.equal(getModelUrlByName('nova'), '/models/avatars/nova.vrm')
  assert.equal(getModelUrlByName('NOVA'), '/models/avatars/nova.vrm')
  assert.equal(getModelUrlByName('sky'), '/models/avatars/sky.vrm')
})

test('generated character names and fuzzy names do not guess a model', () => {
  assert.equal(getModelUrlByName('星瑶'), DEFAULT_MODEL)
  assert.equal(getModelUrlByName('阿岚'), DEFAULT_MODEL)
  assert.equal(getModelUrlByName('nova assistant'), DEFAULT_MODEL)
  assert.equal(getModelUrlByName('luna'), DEFAULT_MODEL)
})
