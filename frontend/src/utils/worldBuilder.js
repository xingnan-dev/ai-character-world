export const WORLD_FIELD_LIMITS = Object.freeze({
  name: 100,
  background: 2000,
  rules: 2000,
  atmosphere: 500,
  scene: 1000,
  sourceDescription: 2000
})

const text = (value) => typeof value === 'string' ? value.trim() : ''

export function createWorldForm(source = {}) {
  return {
    name: text(source.name),
    background: text(source.background),
    rules: text(source.rules),
    atmosphere: text(source.atmosphere),
    scene: text(source.scene),
    sourceDescription: text(source.sourceDescription)
  }
}

export function applyWorldDraft(form, draft, originalDescription) {
  const next = createWorldForm(draft)
  Object.assign(form, next, { sourceDescription: text(originalDescription) })
  return form
}

export function buildWorldSemanticPayload(form) {
  const payload = createWorldForm(form)
  return Object.fromEntries(Object.entries(payload).map(([key, value]) => [key, value || null]))
}

export function eligibleAiCharacters(characters) {
  return (Array.isArray(characters) ? characters : []).filter(character =>
    character?.characterType === 'AI' && character?.deleted !== true && character?.deleted !== 1
      && character?.status !== 0
  )
}

export function normalizeSelectedCharacters(characters) {
  const seen = new Set()
  return (Array.isArray(characters) ? characters : []).filter(character => {
    if (!character?.id || seen.has(character.id)) return false
    seen.add(character.id)
    return true
  })
}

export function moveRosterItem(items, index, direction) {
  const target = index + direction
  if (!Array.isArray(items) || index < 0 || target < 0 || index >= items.length || target >= items.length) {
    return items
  }
  const next = [...items]
  ;[next[index], next[target]] = [next[target], next[index]]
  return next
}

export function buildParticipantPayload(characters) {
  return normalizeSelectedCharacters(characters).map((character, displayOrder) => ({
    characterId: character.id,
    participantType: 'AI',
    displayOrder
  }))
}

export function snapshotSummary(character) {
  if (!character) return '暂无角色摘要'
  return text(character.identity) || text(character.corePersonality) || text(character.currentGoal) || '暂无角色摘要'
}

export function accentForWorld(id) {
  const accents = ['accent-sky', 'accent-mint', 'accent-peach']
  const numericId = Number(id)
  return accents[Number.isFinite(numericId) ? Math.abs(numericId) % accents.length : 0]
}

export function visibleWorldParticipants(world) {
  return (Array.isArray(world?.participants) ? world.participants : []).slice(0, 4)
}
