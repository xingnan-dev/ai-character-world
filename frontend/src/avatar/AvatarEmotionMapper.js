const RULES = Object.freeze([
  {
    emotion: 'sad',
    keywords: ['很抱歉', '抱歉', '对不起', '遗憾', '难过', '伤心', '可惜', '失望'],
    intensity: 0.5,
    duration: 1200
  },
  {
    emotion: 'surprised',
    action: 'nod',
    keywords: ['真的吗', '不会吧', '没想到', '天啊', '居然', '竟然', '原来如此', '真的', '哇'],
    intensity: 0.55,
    duration: 1050
  },
  {
    emotion: 'happy',
    keywords: ['太好了', '好开心', '太棒了', '真棒', '恭喜', '成功', '好耶', '哈哈', '嘿嘿', '开心', '喜欢'],
    action: 'nod',
    intensity: 0.65,
    duration: 1200
  },
  {
    action: 'shakeHead',
    keywords: ['不可以', '不行', '不能', '不是', '不对', '错误', '无法'],
    intensity: 0.5,
    duration: 900
  },
  {
    action: 'nod',
    keywords: ['没问题', '明白', '当然', '可以', '是的', '对的', '好的', '好'],
    intensity: 0.5,
    duration: 900
  }
])

export function mapReplyToReaction(text) {
  if (typeof text !== 'string') return null
  const normalized = text.trim().toLowerCase()
  if (!normalized) return null

  const rule = RULES.find(({ keywords }) => keywords.some((keyword) => normalized.includes(keyword)))
  if (!rule) return null

  return {
    emotion: rule.emotion ?? null,
    action: rule.action ?? null,
    intensity: rule.intensity,
    duration: rule.duration
  }
}

export default mapReplyToReaction
