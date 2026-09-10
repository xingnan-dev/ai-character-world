const SAFE_USER_COLOR = '#f4b293'
const SAFE_AI_COLOR = '#83cdf3'
const SAFE_UNKNOWN_COLOR = '#8b979d'

const isObject = value => value !== null && typeof value === 'object' && !Array.isArray(value)
const text = value => typeof value === 'string' ? value.trim() : ''

function safeColor(value, fallback) {
  const candidate = text(value)
  return /^(?:#[0-9a-f]{3}|#[0-9a-f]{4}|#[0-9a-f]{6}|#[0-9a-f]{8})$/i.test(candidate)
    ? candidate
    : fallback
}

function initial(name, fallback) {
  return Array.from(text(name))[0]?.toUpperCase() || fallback
}

function imageUrlOf(snapshot) {
  return text(snapshot?.imageUrl) || null
}

function userActor(round) {
  const snapshot = isObject(round?.userCharacter)
    && round.userCharacter.characterType === 'USER'
    ? round.userCharacter
    : null
  const name = text(snapshot?.name) || '用户角色'
  return {
    kind: 'USER',
    name,
    initial: initial(snapshot?.name, '我'),
    avatarColor: safeColor(snapshot?.avatarColor, SAFE_USER_COLOR),
    imageUrl: imageUrlOf(snapshot),
    snapshotSource: snapshot ? 'ROUND' : 'FALLBACK',
    sourceCharacterId: snapshot?.sourceCharacterId ?? null,
    participantId: null
  }
}

function participantById(participants, participantId) {
  if (participantId == null) return null
  return participants.find(participant => participant?.id != null
    && String(participant.id) === String(participantId)) || null
}

function aiActor(event, participants) {
  const participant = participantById(participants, event?.participantId)
  const snapshot = isObject(participant?.character) ? participant.character : null
  const name = text(snapshot?.name) || '未知角色'
  return {
    kind: 'AI',
    name,
    initial: initial(snapshot?.name, '角'),
    avatarColor: safeColor(snapshot?.avatarColor, SAFE_AI_COLOR),
    imageUrl: imageUrlOf(snapshot),
    snapshotSource: snapshot ? 'PARTICIPANT' : 'FALLBACK',
    sourceCharacterId: participant?.sourceCharacterId ?? null,
    participantId: event?.participantId ?? null
  }
}

function unknownActor() {
  return {
    kind: 'UNKNOWN',
    name: '未知事件',
    initial: '?',
    avatarColor: SAFE_UNKNOWN_COLOR,
    imageUrl: null,
    snapshotSource: 'FALLBACK',
    sourceCharacterId: null,
    participantId: null
  }
}

function eventViewModel(event, round, participants) {
  const kind = event?.eventType === 'USER_MESSAGE'
    ? 'USER'
    : event?.eventType === 'AI_MESSAGE' ? 'AI' : 'UNKNOWN'
  const actor = kind === 'USER'
    ? userActor(round)
    : kind === 'AI' ? aiActor(event, participants) : unknownActor()
  return { ...event, kind, actor }
}

export function buildWorldTimelineViewModel(items, participants) {
  const sourceItems = Array.isArray(items) ? items : []
  const sourceParticipants = Array.isArray(participants) ? participants : []
  return sourceItems.map(item => {
    const round = isObject(item?.round) ? item.round : {}
    const events = Array.isArray(item?.events) ? item.events : []
    return {
      ...item,
      round: { ...round },
      events: events.map(event => eventViewModel(event, round, sourceParticipants))
    }
  })
}
