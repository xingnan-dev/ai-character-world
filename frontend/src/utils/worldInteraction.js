export const ROUND_TERMINAL_STATUSES = Object.freeze(['COMPLETED', 'PARTIAL_FAILED', 'FAILED'])
export const ROUND_ACTIVE_STATUSES = Object.freeze(['PENDING', 'RUNNING'])

const ERROR_MESSAGES = Object.freeze({
  WORLD_EXECUTION_BUSY: '当前互动任务较多，本轮已保留，请稍后恢复执行',
  WORLD_ROUND_ACTIVE: '当前世界已有一轮互动正在进行，已为你恢复',
  WORLD_NO_AI_PARTICIPANT: '当前世界没有可执行的AI角色',
  WORLD_ALL_PARTICIPANTS_FAILED: '本轮所有角色均未能回应，请稍后再试',
  WORLD_PARTICIPANT_FAILURE: '部分角色未能回应，其他回答已为你保留',
  WORLD_SNAPSHOT_INVALID: '角色资料暂时不可用',
  WORLD_AI_INTERNAL_ERROR: '该角色暂时无法回应',
  LLM_CONFIGURATION: 'AI服务暂未正确配置',
  LLM_INVALID_REQUEST: '该角色暂时无法处理本轮内容',
  LLM_AUTHENTICATION: 'AI服务认证失败',
  LLM_RATE_LIMIT: 'AI服务繁忙，该角色暂未回应',
  LLM_TIMEOUT: '该角色回应超时',
  LLM_NETWORK: 'AI服务网络暂时不可用',
  LLM_UPSTREAM_ERROR: 'AI服务暂时不可用',
  LLM_INVALID_RESPONSE: '该角色返回了无效内容',
  LLM_CANCELLED: '该角色的回应已取消'
})

export const isTerminalRound = status => ROUND_TERMINAL_STATUSES.includes(status)
export const isActiveRound = status => ROUND_ACTIVE_STATUSES.includes(status)

export function dataOf(response) {
  if (response !== null && typeof response === 'object'
    && Object.prototype.hasOwnProperty.call(response, 'data')) {
    return response.data
  }
  return response
}

export function normalizeWorldInput(value) {
  return typeof value === 'string' ? value.trim() : ''
}

export function createWorldRequestId(cryptoProvider = globalThis.crypto) {
  if (!cryptoProvider || typeof cryptoProvider.randomUUID !== 'function') {
    throw new Error('当前浏览器无法生成安全请求标识')
  }
  return cryptoProvider.randomUUID()
}

export function interactionStorageKey(userId, worldId) {
  const owner = Number.isSafeInteger(Number(userId)) && Number(userId) > 0 ? Number(userId) : 'session'
  return `world-interaction:${owner}:${worldId}:pending`
}

export function readPendingInteraction(storage, key) {
  try {
    const value = JSON.parse(storage?.getItem(key) || 'null')
    if (!value || typeof value.requestId !== 'string' || typeof value.userInput !== 'string') return null
    if (!value.requestId || value.requestId.length > 64 || !normalizeWorldInput(value.userInput)) return null
    return {
      requestId: value.requestId,
      userInput: value.userInput,
      roundId: Number.isSafeInteger(Number(value.roundId)) && Number(value.roundId) > 0 ? Number(value.roundId) : null,
      phase: typeof value.phase === 'string' ? value.phase : 'CREATING',
      createdAt: Number.isFinite(Number(value.createdAt)) ? Number(value.createdAt) : Date.now()
    }
  } catch {
    return null
  }
}

export function writePendingInteraction(storage, key, descriptor) {
  storage?.setItem(key, JSON.stringify(descriptor))
  return descriptor
}

export function clearPendingInteraction(storage, key) {
  storage?.removeItem(key)
}

export function sortEvents(events) {
  return [...(Array.isArray(events) ? events : [])].sort((a, b) =>
    Number(a.sequenceNo || 0) - Number(b.sequenceNo || 0) || Number(a.id || 0) - Number(b.id || 0)
  )
}

export function mergeEvents(current = [], incoming = []) {
  const byId = new Map()
  for (const event of [...current, ...incoming]) {
    if (event?.id == null) continue
    byId.set(String(event.id), { ...(byId.get(String(event.id)) || {}), ...event })
  }
  return sortEvents([...byId.values()])
}

export function mergeTimeline(current = [], incoming = []) {
  const byRoundId = new Map()
  for (const item of [...current, ...incoming]) {
    if (item?.round?.id == null) continue
    const key = String(item.round.id)
    const previous = byRoundId.get(key)
    byRoundId.set(key, {
      round: { ...(previous?.round || {}), ...item.round },
      events: mergeEvents(previous?.events, item.events)
    })
  }
  return [...byRoundId.values()].sort((a, b) => Number(a.round.id) - Number(b.round.id))
}

export function participantForEvent(participants, event) {
  if (event?.participantId == null) return null
  return (Array.isArray(participants) ? participants : []).find(
    participant => String(participant?.id) === String(event.participantId)
  ) || null
}

export function errorCodeOf(error) {
  const candidates = [
    error?.response?.data?.msg,
    error?.response?.data?.message,
    error?.code,
    error?.message
  ]
  return candidates.find(value => typeof value === 'string' && value.trim())?.trim() || ''
}

export function worldInteractionErrorMessage(errorOrCode, fallback = 'World互动暂时不可用，请稍后重试') {
  const code = typeof errorOrCode === 'string' ? errorOrCode : errorCodeOf(errorOrCode)
  return ERROR_MESSAGES[code] || fallback
}

export function pollDelay(elapsedMs, failureCount = 0, hidden = false) {
  if (hidden) return 15000
  if (failureCount > 0) return Math.min(15000, 1000 * (2 ** Math.min(failureCount, 4)))
  if (elapsedMs < 15000) return 1000
  if (elapsedMs < 60000) return 2000
  return 5000
}

export function currentParticipantId(participants, events, roundStatus) {
  if (!isActiveRound(roundStatus)) return null
  const completedIds = new Set((Array.isArray(events) ? events : [])
    .filter(event => event?.participantId != null)
    .map(event => String(event.participantId)))
  return (Array.isArray(participants) ? participants : [])
    .slice().sort((a, b) => Number(a.displayOrder || 0) - Number(b.displayOrder || 0))
    .find(participant => !completedIds.has(String(participant.id)))?.id || null
}
