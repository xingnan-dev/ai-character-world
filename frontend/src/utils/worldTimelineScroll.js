export function isNearBottom({ scrollTop = 0, clientHeight = 0, scrollHeight = 0, threshold = 160 } = {}) {
  const top = Number(scrollTop)
  const height = Number(clientHeight)
  const total = Number(scrollHeight)
  const limit = Number(threshold)
  if (![top, height, total, limit].every(Number.isFinite)) return false
  return total - (top + height) <= Math.max(0, limit)
}

export function createTimelineSignature(items = []) {
  if (!Array.isArray(items)) return ''
  return items.map(item => {
    const round = item?.round
    const roundKey = round?.id == null ? '?' : String(round.id)
    const events = Array.isArray(item?.events) ? item.events : []
    const eventSignature = events.map(event => [event?.id, event?.status, event?.content, event?.errorCode].map(value => String(value ?? '')).join(':')).join('|')
    return `${roundKey}[${eventSignature}]`
  }).join('||')
}

export function classifyTimelineUpdate(previousItems = [], nextItems = [], { historyLoad = false } = {}) {
  if (!Array.isArray(previousItems) || previousItems.length === 0) return 'INITIAL'
  if (!Array.isArray(nextItems) || nextItems.length === 0) return 'UNCHANGED'
  if (historyLoad) return 'HISTORY_PREPEND'
  const previous = createTimelineSignature(previousItems)
  const next = createTimelineSignature(nextItems)
  if (previous === next) return 'UNCHANGED'
  const previousLast = previousItems.at(-1)?.round?.id
  const nextLast = nextItems.at(-1)?.round?.id
  return String(previousLast) === String(nextLast) ? 'TAIL_UPDATE' : 'TAIL_APPEND'
}

export function calculateAnchorAdjustment(previousTop, nextTop) {
  const before = Number(previousTop)
  const after = Number(nextTop)
  if (![before, after].every(Number.isFinite)) return 0
  return before - after
}
