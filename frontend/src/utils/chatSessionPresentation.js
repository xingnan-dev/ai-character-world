const clean = (value) => typeof value === 'string' && value.trim() ? value.trim() : ''

export function isCharacterSession(session) {
  return session?.characterId != null
}

export function resolveAssistantPresentation(session) {
  if (!session) return { name: 'AI', imageUrl: '', avatarColor: '', type: 'AI', entityKey: '' }
  const character = isCharacterSession(session)
  return {
    name: clean(character ? session.characterName : session.avatarName) || clean(session.title) || 'AI',
    imageUrl: character ? clean(session.imageUrl) : '',
    avatarColor: clean(session.avatarColor),
    type: 'AI',
    entityKey: character ? `character-session-${session.id}` : `avatar-session-${session.id}`
  }
}

export function resolveUserPresentation(session, user = {}) {
  const snapshot = session?.userCharacter?.characterType === 'USER' ? session.userCharacter : null
  return {
    name: clean(snapshot?.name) || clean(user.nickname) || '用户',
    imageUrl: clean(snapshot?.imageUrl),
    avatarColor: clean(snapshot?.avatarColor),
    type: 'USER',
    entityKey: snapshot?.sourceCharacterId || `user-${user.id || user.nickname || 'current'}`
  }
}

export function resolveMessagePresentation(session, role, user) {
  return role === 'assistant'
    ? resolveAssistantPresentation(session)
    : resolveUserPresentation(session, user)
}

export function resolveCharacterDetails(session) {
  if (!session) return []
  return [
    ['身份', clean(session.identity)],
    ['性格', clean(session.corePersonality)],
    ['当前目标', clean(session.currentGoal)],
    ['说话风格', clean(session.speakingStyle)],
    ['与你的关系', clean(session.relationshipToUser)]
  ].filter(([, value]) => value)
}

export function imageAfterFailure(imageUrl, failedUrl = '') {
  const safe = clean(imageUrl)
  return safe && safe !== failedUrl ? safe : ''
}
