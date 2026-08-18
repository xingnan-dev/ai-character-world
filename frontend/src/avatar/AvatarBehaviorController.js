import AvatarCapabilityDetector from './AvatarCapabilityDetector.js'
import AvatarExpressionAdapter from './AvatarExpressionAdapter.js'

export const AVATAR_BEHAVIOR_STATES = Object.freeze([
  'idle', 'thinking', 'talking', 'listening', 'error'
])

const DEFAULT_TIMING = Object.freeze({
  minBlinkDelay: 2500,
  maxBlinkDelay: 6500,
  blinkCloseDuration: 80,
  blinkHoldDuration: 45,
  blinkOpenDuration: 110
})

export class AvatarBehaviorController {
  constructor(vrm, options = {}) {
    this.vrm = vrm ?? null
    this.detector = options.detector ?? new AvatarCapabilityDetector()
    this.capabilities = this.detector.detect(this.vrm)
    this.expressionAdapter = options.expressionAdapter
      ?? new AvatarExpressionAdapter(this.vrm, this.detector)
    this.setTimer = options.setTimeout ?? globalThis.setTimeout.bind(globalThis)
    this.clearTimer = options.clearTimeout ?? globalThis.clearTimeout.bind(globalThis)
    this.random = options.random ?? Math.random
    this.timing = { ...DEFAULT_TIMING, ...options.timing }
    this.state = 'idle'
    this.stateElapsed = 0
    this.disposed = false
    this.blinkTimer = null
    this.blinkStepTimers = new Set()
    this.head = this.#getBone('head')
    this.neck = this.#getBone('neck')
    this.initialHeadRotation = this.#copyRotation(this.head)
    this.initialNeckRotation = this.#copyRotation(this.neck)
    this.start()
  }

  start() {
    if (this.disposed || this.blinkTimer !== null || !this.capabilities.blink) return false
    this.#scheduleBlink()
    return true
  }

  setState(state) {
    if (this.disposed || !AVATAR_BEHAVIOR_STATES.includes(state)) return false
    if (this.state === state) return true
    this.state = state
    this.stateElapsed = 0
    return true
  }

  playReaction(reaction) {
    if (this.disposed) return false
    if (reaction === 'happy' || reaction === 'sad' || reaction === 'surprised') {
      return this.expressionAdapter.setEmotion(reaction, 1)
    }
    return false
  }

  update(delta, elapsed = 0) {
    if (this.disposed) return
    const safeDelta = Math.max(0, Math.min(Number(delta) || 0, 0.1))
    this.stateElapsed += safeDelta
    const offset = this.#stateOffset(elapsed)
    const blend = 1 - Math.exp(-safeDelta * 10)
    this.#applyRotation(this.head, this.initialHeadRotation, offset, blend)
    this.#applyRotation(this.neck, this.initialNeckRotation, {
      x: offset.x * 0.45,
      y: offset.y * 0.45,
      z: offset.z * 0.45
    }, blend)
  }

  dispose() {
    if (this.disposed) return
    this.disposed = true
    this.#clearAllTimers()
    this.expressionAdapter.dispose()
    this.#restoreRotation(this.head, this.initialHeadRotation)
    this.#restoreRotation(this.neck, this.initialNeckRotation)
    this.vrm = null
    this.head = null
    this.neck = null
  }

  #scheduleBlink() {
    if (this.disposed || !this.capabilities.blink || this.blinkTimer !== null) return
    const range = Math.max(0, this.timing.maxBlinkDelay - this.timing.minBlinkDelay)
    const delay = this.timing.minBlinkDelay + this.random() * range
    this.blinkTimer = this.setTimer(() => {
      this.blinkTimer = null
      this.#performBlink()
    }, delay)
  }

  #performBlink() {
    if (this.disposed) return
    this.expressionAdapter.setBlink(0.55)
    this.#scheduleBlinkStep(this.timing.blinkCloseDuration, () => {
      this.expressionAdapter.setBlink(1)
      this.#scheduleBlinkStep(this.timing.blinkHoldDuration, () => {
        this.expressionAdapter.setBlink(0.45)
        this.#scheduleBlinkStep(this.timing.blinkOpenDuration, () => {
          this.expressionAdapter.setBlink(0)
          this.#scheduleBlink()
        })
      })
    })
  }

  #scheduleBlinkStep(delay, callback) {
    if (this.disposed) return
    const timer = this.setTimer(() => {
      this.blinkStepTimers.delete(timer)
      if (!this.disposed) callback()
    }, delay)
    this.blinkStepTimers.add(timer)
  }

  #clearAllTimers() {
    if (this.blinkTimer !== null) {
      this.clearTimer(this.blinkTimer)
      this.blinkTimer = null
    }
    for (const timer of this.blinkStepTimers) this.clearTimer(timer)
    this.blinkStepTimers.clear()
  }

  #getBone(name) {
    if (!this.vrm?.humanoid) return null
    try {
      return this.vrm.humanoid.getNormalizedBoneNode?.(name)
        ?? this.vrm.humanoid.getRawBoneNode?.(name)
        ?? null
    } catch {
      return null
    }
  }

  #copyRotation(node) {
    if (!node?.rotation) return null
    return { x: node.rotation.x, y: node.rotation.y, z: node.rotation.z }
  }

  #stateOffset(elapsed) {
    if (this.state === 'thinking') {
      return {
        x: -0.008 + Math.sin(this.stateElapsed * 0.8) * 0.003,
        y: Math.sin(this.stateElapsed * 0.65) * 0.005,
        z: 0.014 + Math.sin(this.stateElapsed * 0.55) * 0.003
      }
    }
    if (this.state === 'talking') {
      return {
        x: Math.sin(this.stateElapsed * 2.4) * 0.012,
        y: Math.sin(this.stateElapsed * 1.8) * 0.007,
        z: Math.sin(this.stateElapsed * 1.4) * 0.003
      }
    }
    const sway = Math.sin(elapsed * 0.7) * 0.012
    return { x: 0, y: sway, z: sway * 0.35 }
  }

  #applyRotation(node, initial, offset, blend) {
    if (!node?.rotation || !initial) return
    node.rotation.x += (initial.x + offset.x - node.rotation.x) * blend
    node.rotation.y += (initial.y + offset.y - node.rotation.y) * blend
    node.rotation.z += (initial.z + offset.z - node.rotation.z) * blend
  }

  #restoreRotation(node, initial) {
    if (!node?.rotation || !initial) return
    node.rotation.x = initial.x
    node.rotation.y = initial.y
    node.rotation.z = initial.z
  }
}

export default AvatarBehaviorController
