const EXPRESSION_NAMES = Object.freeze({
  blink: ['blink', 'blinkLeft', 'blinkRight'],
  mouth: ['aa', 'ih', 'ou', 'ee', 'oh'],
  happy: ['happy', 'joy'],
  sad: ['sad', 'sorrow'],
  surprised: ['surprised']
})

function hasExpression(manager, names) {
  if (!manager) return false

  return names.some((name) => {
    try {
      if (typeof manager.getExpression === 'function') {
        return Boolean(manager.getExpression(name))
      }
      return Boolean(manager.expressionMap?.[name] || manager.presetExpressionMap?.[name])
    } catch {
      return false
    }
  })
}

function hasBone(humanoid, name) {
  if (!humanoid) return false
  try {
    if (typeof humanoid.getNormalizedBoneNode === 'function') {
      return Boolean(humanoid.getNormalizedBoneNode(name))
    }
    if (typeof humanoid.getRawBoneNode === 'function') {
      return Boolean(humanoid.getRawBoneNode(name))
    }
  } catch {
    return false
  }
  return false
}

export class AvatarCapabilityDetector {
  detect(vrm) {
    const expressionManager = vrm?.expressionManager ?? null
    const humanoid = vrm?.humanoid ?? null

    return Object.freeze({
      expressionManager: Boolean(expressionManager),
      blink: hasExpression(expressionManager, EXPRESSION_NAMES.blink),
      mouth: hasExpression(expressionManager, EXPRESSION_NAMES.mouth),
      happy: hasExpression(expressionManager, EXPRESSION_NAMES.happy),
      sad: hasExpression(expressionManager, EXPRESSION_NAMES.sad),
      surprised: hasExpression(expressionManager, EXPRESSION_NAMES.surprised),
      head: hasBone(humanoid, 'head'),
      neck: hasBone(humanoid, 'neck')
    })
  }
}

export default AvatarCapabilityDetector
