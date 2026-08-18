import test from 'node:test'
import assert from 'node:assert/strict'
import {
  AVATAR_BEHAVIOR_STATES,
  AvatarBehaviorController
} from '../src/avatar/AvatarBehaviorController.js'

function fakeTimers() {
  let id = 0
  const callbacks = new Map()
  return {
    setTimeout(callback) {
      id += 1
      callbacks.set(id, callback)
      return id
    },
    clearTimeout(timerId) { callbacks.delete(timerId) },
    runNext() {
      const entry = callbacks.entries().next().value
      if (!entry) return false
      callbacks.delete(entry[0])
      entry[1]()
      return true
    },
    count() { return callbacks.size }
  }
}

function blinkVrm() {
  const writes = []
  return {
    writes,
    expressionManager: {
      getExpression: (name) => name === 'blink' ? { expressionName: name } : null,
      setValue: (name, value) => writes.push([name, value])
    }
  }
}

test('controller supports all declared state transitions and rejects unknown state', () => {
  const timers = fakeTimers()
  const controller = new AvatarBehaviorController({}, {
    setTimeout: timers.setTimeout,
    clearTimeout: timers.clearTimeout
  })
  for (const state of AVATAR_BEHAVIOR_STATES) {
    assert.equal(controller.setState(state), true)
    assert.equal(controller.state, state)
  }
  assert.equal(controller.setState('walking'), false)
  controller.dispose()
})

test('unsupported blink creates no timers and never throws', () => {
  const timers = fakeTimers()
  assert.doesNotThrow(() => {
    const controller = new AvatarBehaviorController({}, {
      setTimeout: timers.setTimeout,
      clearTimeout: timers.clearTimeout
    })
    controller.dispose()
  })
  assert.equal(timers.count(), 0)
})

test('start is idempotent and does not create duplicate blink timers', () => {
  const timers = fakeTimers()
  const controller = new AvatarBehaviorController(blinkVrm(), {
    setTimeout: timers.setTimeout,
    clearTimeout: timers.clearTimeout,
    random: () => 0.5
  })
  assert.equal(timers.count(), 1)
  assert.equal(controller.start(), false)
  assert.equal(controller.start(), false)
  assert.equal(timers.count(), 1)
  controller.dispose()
})

test('blink uses transition steps and schedules the next random blink', () => {
  const timers = fakeTimers()
  const vrm = blinkVrm()
  const controller = new AvatarBehaviorController(vrm, {
    setTimeout: timers.setTimeout,
    clearTimeout: timers.clearTimeout,
    random: () => 0.25
  })
  timers.runNext()
  timers.runNext()
  timers.runNext()
  timers.runNext()
  assert.deepEqual(vrm.writes.slice(0, 4), [
    ['blink', 0.55], ['blink', 1], ['blink', 0.45], ['blink', 0]
  ])
  assert.equal(timers.count(), 1)
  controller.dispose()
})

test('dispose clears pending behavior and prevents future callbacks', () => {
  const timers = fakeTimers()
  const vrm = blinkVrm()
  const controller = new AvatarBehaviorController(vrm, {
    setTimeout: timers.setTimeout,
    clearTimeout: timers.clearTimeout
  })
  controller.dispose()
  const writesAfterDispose = vrm.writes.length
  assert.equal(timers.count(), 0)
  assert.equal(timers.runNext(), false)
  assert.equal(vrm.writes.length, writesAfterDispose)
  assert.equal(controller.setState('talking'), false)
})

test('idle update applies only subtle motion and dispose restores rotation', () => {
  const timers = fakeTimers()
  const head = { rotation: { x: 0.1, y: 0.2, z: 0.3 } }
  const vrm = {
    humanoid: { getNormalizedBoneNode: (name) => name === 'head' ? head : null }
  }
  const controller = new AvatarBehaviorController(vrm, {
    setTimeout: timers.setTimeout,
    clearTimeout: timers.clearTimeout
  })
  controller.update(0.016, 2)
  assert.ok(Math.abs(head.rotation.y - 0.2) <= 0.0121)
  controller.dispose()
  assert.deepEqual(head.rotation, { x: 0.1, y: 0.2, z: 0.3 })
})
