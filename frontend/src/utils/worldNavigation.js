export function worldInteractionLocation(worldId) {
  const id = Number(worldId)
  if (!Number.isSafeInteger(id) || id <= 0) return null
  return { name: 'WorldInteraction', params: { worldId: String(id) } }
}
