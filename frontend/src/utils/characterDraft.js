export const PROFILE_FIELDS = [
  'values',
  'likes',
  'dislikes',
  'interests',
  'fears',
  'secrets',
  'behaviorTendencies'
]

export function createCharacterForm(draft = {}) {
  const profile = draft.profile || {}
  return {
    characterType: draft.characterType === 'USER' ? 'USER' : 'AI',
    name: draft.name || '',
    age: draft.age ?? null,
    identity: draft.identity || '',
    corePersonality: draft.corePersonality || '',
    currentGoal: draft.currentGoal || '',
    biography: draft.biography || '',
    relationshipToUser: draft.relationshipToUser || '',
    speakingStyle: draft.speakingStyle || '',
    avatarColor: draft.avatarColor || '#7c5cff',
    profile: Object.fromEntries(PROFILE_FIELDS.map(field => [
      field,
      Array.isArray(profile[field]) ? profile[field].join('\n') : ''
    ]))
  }
}

export function applyCharacterDraft(form, draft) {
  const editable = createCharacterForm({ ...draft, avatarColor: form.avatarColor })
  Object.assign(form, editable)
  form.profile = { ...editable.profile }
  return form
}

function normalizeList(value) {
  return String(value || '')
    .split(/[\n,，]/)
    .map(item => item.trim())
    .filter(Boolean)
    .slice(0, 10)
}

function optionalText(value) {
  const normalized = String(value ?? '').trim()
  return normalized || null
}

export function buildCharacterCreatePayload(form, sourceDescription = '') {
  return {
    characterType: form.characterType,
    name: String(form.name || '').trim(),
    age: form.age === '' || form.age == null ? null : Number(form.age),
    identity: optionalText(form.identity),
    corePersonality: optionalText(form.corePersonality),
    currentGoal: optionalText(form.currentGoal),
    biography: optionalText(form.biography),
    relationshipToUser: optionalText(form.relationshipToUser),
    speakingStyle: optionalText(form.speakingStyle),
    profile: Object.fromEntries(PROFILE_FIELDS.map(field => [field, normalizeList(form.profile?.[field])])),
    sourceDescription: optionalText(sourceDescription),
    visualType: 'INITIAL',
    avatarColor: form.avatarColor
  }
}

export function buildCharacterUpdatePayload(form) {
  return {
    characterType: form.characterType,
    name: String(form.name || '').trim(),
    age: form.age === '' || form.age == null ? null : Number(form.age),
    identity: String(form.identity ?? '').trim(),
    corePersonality: String(form.corePersonality ?? '').trim(),
    currentGoal: String(form.currentGoal ?? '').trim(),
    biography: String(form.biography ?? '').trim(),
    relationshipToUser: String(form.relationshipToUser ?? '').trim(),
    speakingStyle: String(form.speakingStyle ?? '').trim(),
    profile: Object.fromEntries(PROFILE_FIELDS.map(field => [field, normalizeList(form.profile?.[field])])),
    avatarColor: form.avatarColor
  }
}
