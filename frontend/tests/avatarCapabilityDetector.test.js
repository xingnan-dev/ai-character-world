import test from 'node:test'
import assert from 'node:assert/strict'
import { AvatarCapabilityDetector } from '../src/avatar/AvatarCapabilityDetector.js'

function createVrm(expressions = [], bones = []) {
  return {
    expressionManager: {
      getExpression: (name) => expressions.includes(name) ? { expressionName: name } : null
    },
    humanoid: {
      getNormalizedBoneNode: (name) => bones.includes(name) ? { name } : null
    }
  }
}

test('detects common VRM expression and humanoid capabilities', () => {
  const capabilities = new AvatarCapabilityDetector().detect(
    createVrm(['blink', 'aa', 'happy', 'sad', 'surprised'], ['head', 'neck'])
  )
  assert.deepEqual(capabilities, {
    expressionManager: true,
    blink: true,
    mouth: true,
    happy: true,
    sad: true,
    surprised: true,
    head: true,
    neck: true
  })
})

test('missing and throwing VRM capabilities safely return false', () => {
  const detector = new AvatarCapabilityDetector()
  assert.equal(detector.detect(null).blink, false)
  const capabilities = detector.detect({
    expressionManager: { getExpression: () => { throw new Error('unsupported') } },
    humanoid: { getNormalizedBoneNode: () => { throw new Error('unsupported') } }
  })
  assert.equal(capabilities.blink, false)
  assert.equal(capabilities.mouth, false)
  assert.equal(capabilities.head, false)
})
