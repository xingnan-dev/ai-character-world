export const AGENT_POLL_INTERVAL_MS = 1200
export const AGENT_HIDDEN_POLL_INTERVAL_MS = 5000
export const AGENT_MAX_POLL_BACKOFF_MS = 8000
export const AGENT_TERMINAL_STATUSES = new Set(['COMPLETED', 'FAILED'])

export const isTerminalAgentStatus = status => AGENT_TERMINAL_STATUSES.has(String(status || '').toUpperCase())
export const agentPollDelay = (failureCount, hidden = false) => hidden
  ? AGENT_HIDDEN_POLL_INTERVAL_MS
  : Math.min(AGENT_POLL_INTERVAL_MS * (2 ** Math.max(0, failureCount)), AGENT_MAX_POLL_BACKOFF_MS)

export function createAgentPollingController({
  fetchRun,
  onRun,
  onState = () => {},
  onWarning = () => {},
  onTerminal = () => {},
  setTimer = setTimeout,
  clearTimer = clearTimeout,
  visibility = typeof document === 'undefined' ? null : document
}) {
  let generation = 0
  let runId = null
  let timer = null
  let active = false
  let failureCount = 0
  let inFlightToken = null
  let refreshAfterFlight = false
  let abortController = null
  let visibilityAttached = false

  const state = () => ({ pollingRunId: runId, pollingActive: active, pollingFailureCount: failureCount })
  const emitState = () => onState(state())
  const hidden = () => visibility?.visibilityState === 'hidden'
  const clearScheduled = () => { if (timer != null) clearTimer(timer); timer = null }

  const schedule = (token, delay = agentPollDelay(failureCount, hidden())) => {
    if (!active || token !== generation) return
    clearScheduled()
    timer = setTimer(() => { timer = null; void poll(token) }, delay)
  }

  const poll = async token => {
    if (!active || token !== generation) return
    if (inFlightToken === token) { refreshAfterFlight = true; return }
    inFlightToken = token
    abortController = typeof AbortController === 'undefined' ? null : new AbortController()
    const requestedRunId = runId
    try {
      const run = await fetchRun(requestedRunId, abortController ? { signal: abortController.signal } : {})
      if (!active || token !== generation || requestedRunId !== runId) return
      failureCount = 0
      onWarning('')
      onRun(run)
      emitState()
      if (isTerminalAgentStatus(run?.status)) {
        active = false
        runId = null
        clearScheduled()
        emitState()
        onTerminal(run)
        return
      }
    } catch (error) {
      if (!active || token !== generation || requestedRunId !== runId || error?.name === 'CanceledError' || error?.name === 'AbortError') return
      failureCount += 1
      onWarning('连接暂时异常，正在重试。')
      emitState()
    } finally {
      if (token === generation && inFlightToken === token) {
        inFlightToken = null
        abortController = null
        const immediate = refreshAfterFlight
        refreshAfterFlight = false
        if (active) schedule(token, immediate ? 0 : agentPollDelay(failureCount, hidden()))
      }
    }
  }

  const stop = () => {
    generation += 1
    active = false
    runId = null
    failureCount = 0
    refreshAfterFlight = false
    clearScheduled()
    abortController?.abort()
    abortController = null
    inFlightToken = null
    emitState()
  }

  const start = id => {
    if (active && runId === id) return false
    stop()
    generation += 1
    runId = id
    active = true
    failureCount = 0
    emitState()
    schedule(generation, 0)
    return true
  }

  const refreshNow = () => {
    if (!active) return
    clearScheduled()
    if (inFlightToken === generation) refreshAfterFlight = true
    else schedule(generation, 0)
  }

  const handleVisibilityChange = () => {
    if (!active) return
    if (hidden()) schedule(generation, AGENT_HIDDEN_POLL_INTERVAL_MS)
    else refreshNow()
  }

  const attachVisibility = () => {
    if (!visibility || visibilityAttached) return
    visibility.addEventListener('visibilitychange', handleVisibilityChange)
    visibilityAttached = true
  }

  const detachVisibility = () => {
    if (!visibility || !visibilityAttached) return
    visibility.removeEventListener('visibilitychange', handleVisibilityChange)
    visibilityAttached = false
  }

  const dispose = () => { stop(); detachVisibility() }

  return { start, stop, refreshNow, handleVisibilityChange, attachVisibility, detachVisibility, dispose, snapshot: state }
}
