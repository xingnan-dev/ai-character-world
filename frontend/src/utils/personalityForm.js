const EDITABLE_FIELDS = [
  'corePersonality',
  'identity',
  'languageStyle',
  'hobbies',
  'relationship'
]

export function createPersonalityEditForm(personality) {
  return Object.fromEntries(
    EDITABLE_FIELDS.map(field => [field, personality?.[field] || ''])
  )
}

export function buildPersonalityUpdatePayload(avatarId, personality, form) {
  const parsedAvatarId = Number(avatarId)
  if (!Number.isSafeInteger(parsedAvatarId) || parsedAvatarId <= 0) {
    throw new Error('无效的形象ID')
  }
  if (!personality?.name) {
    throw new Error('缺少人格名称')
  }

  const payload = {
    avatarId: parsedAvatarId,
    name: personality.name,
    templateType: personality.templateType ?? 1
  }
  for (const field of EDITABLE_FIELDS) {
    payload[field] = typeof form?.[field] === 'string' ? form[field].trim() : ''
  }
  return payload
}
