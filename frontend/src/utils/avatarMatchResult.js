export const UNMATCHED_ATTRIBUTE_LABELS = Object.freeze({
  gender: '性别',
  hairColor: '发色',
  hairStyle: '发型',
  eyeColor: '瞳色',
  bodyType: '体型',
  outfitStyle: '服装风格',
  outfitColor: '服装颜色',
  earType: '耳朵',
  wingType: '翅膀',
  accessories: '配饰'
})

export function getMatchMessage(assetMatchType) {
  if (assetMatchType === 'MATCHED') return '模型完全匹配'
  if (assetMatchType === 'NEAREST') {
    return '当前资产库暂无完全匹配模型，已选择最接近模型'
  }
  return '模型匹配状态未知'
}

export function getParseSourceMessage(parseSource) {
  return parseSource === 'RULE_FALLBACK'
    ? 'AI解析失败，本次使用规则解析'
    : ''
}

export function mapUnmatchedAttributes(attributes) {
  if (!Array.isArray(attributes)) return []
  return [...new Set(attributes)]
    .filter((attribute) => typeof attribute === 'string' && attribute.length > 0)
    .map((attribute) => UNMATCHED_ATTRIBUTE_LABELS[attribute] || attribute)
}
