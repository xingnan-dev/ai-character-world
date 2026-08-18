import AvatarCapabilityDetector from './AvatarCapabilityDetector.js'

const EMOTION_ALIASES = Object.freeze({
  happy: ['happy', 'joy'],
  sad: ['sad', 'sorrow'],
  surprised: ['surprised']
})

const MOUTH_NAMES = ['aa', 'ih', 'ou', 'ee', 'oh']

function clamp(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return 0
  return Math.min(1, Math.max(0, number))
}

export class AvatarExpressionAdapter {
  constructor(vrm, detector = new AvatarCapabilityDetector()) {
    this.vrm = vrm ?? null
    this.manager = this.vrm?.expressionManager ?? null
    this.capabilities = detector.detect(this.vrm)
    this.disposed = false
  }

  setBlink(value) {
    if (!this.capabilities.blink) return false
    return this.#setFirstAvailable(['blink', 'blinkLeft', 'blinkRight'], value)
  }

  setMouthOpen(value) {
    if (!this.capabilities.mouth) return false
    return this.#setFirstAvailable(MOUTH_NAMES, value)
  }

  setEmotion(name, value) {
    const names = EMOTION_ALIASES[name]
    if (!names || !this.capabilities[name]) return false
    return this.#setFirstAvailable(names, value)
  }

  resetExpressions() {
    if (!this.#canOperate()) return false
    const names = new Set([
      'blink', 'blinkLeft', 'blinkRight',
      ...MOUTH_NAMES,
      ...Object.values(EMOTION_ALIASES).flat()
    ])
    let reset = false
    for (const name of names) {
      if (this.#hasExpression(name)) {
        reset = this.#set(name, 0) || reset
      }
    }
    return reset
  }

  dispose() {
    if (this.disposed) return
    this.resetExpressions()
    this.disposed = true
    this.vrm = null
    this.manager = null
  }

  #canOperate() {
    return !this.disposed && Boolean(this.manager)
  }

  #hasExpression(name) {
    if (!this.#canOperate()) return false
    try {
      if (typeof this.manager.getExpression === 'function') {
        return Boolean(this.manager.getExpression(name))
      }
      return Boolean(this.manager.expressionMap?.[name] || this.manager.presetExpressionMap?.[name])
    } catch {
      return false
    }
  }

  #setFirstAvailable(names, value) {
    if (!this.#canOperate()) return false
    let changed = false
    for (const name of names) {
      if (this.#hasExpression(name)) {
        changed = this.#set(name, value) || changed
        if (name !== 'blinkLeft' && name !== 'blinkRight') break
      }
    }
    return changed
  }

  #set(name, value) {
    try {
      this.manager.setValue(name, clamp(value))
      return true
    } catch {
      return false
    }
  }
}

export default AvatarExpressionAdapter
