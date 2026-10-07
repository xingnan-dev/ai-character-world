const RUN_STATUSES = {
  PENDING: { label: '等待执行', tone: 'pending' },
  RUNNING: { label: '执行中', tone: 'running' },
  COMPLETED: { label: '已完成', tone: 'completed' },
  FAILED: { label: '执行失败', tone: 'failed' }
}

const RECOVERY_STATES = {
  PENDING: '可开始执行',
  ACTIVE: '正在执行',
  STALE_RECOVERABLE: '可恢复执行',
  TERMINAL: '执行已结束'
}

const TOOL_PRESENTATIONS = {
  get_character_context: { label: 'Character Context', description: 'Reads the selected Character’s current context.' },
  search_character_memory: { label: 'Character Memory Search', description: 'Retrieves relevant Character-scoped memories.' },
  get_world_context: { label: 'World Context', description: 'Reads the selected World context.' }
}

const text = value => value == null ? '' : String(value)
const integer = (value, fallback = 0) => Number.isInteger(Number(value)) ? Number(value) : fallback
const nullableInteger = value => value == null || value === '' ? null : (Number.isInteger(Number(value)) ? Number(value) : null)

export const sanitizeAgentError = value => {
  const source = text(value).trim()
  if (!source) return ''
  return source.split(/\r?\n/)
    .filter(line => !/^\s*(?:at\s+[\w.$]+\(|\.\.\.\s+\d+\s+more|Suppressed:)/.test(line))
    .map(line => line.replace(/^\s*(?:Caused by:\s*)?(?:[\w$]+\.)+(?:[\w$]*(?:Exception|Error)):\s*/, ''))
    .filter(Boolean).join('\n').slice(0, 2000)
}

export const sanitizeRunError = sanitizeAgentError

export const parseAgentRunId = value => {
  const source = Array.isArray(value) ? value[0] : value
  if (typeof source !== 'string' && typeof source !== 'number') return null
  const normalized = String(source).trim()
  if (!/^[1-9]\d*$/.test(normalized)) return null
  const id = Number(normalized)
  return Number.isSafeInteger(id) ? id : null
}

export const normalizeAgentStep = step => ({
  id: nullableInteger(step?.id),
  stepNumber: integer(step?.stepNumber),
  decisionType: text(step?.decisionType),
  decisionSummary: text(step?.decisionSummary),
  toolCallId: text(step?.toolCallId),
  toolName: text(step?.toolName),
  toolArguments: step?.toolArguments ?? '',
  toolResult: step?.toolResult ?? '',
  status: text(step?.status).toUpperCase(),
  retryCount: integer(step?.retryCount),
  toolAttemptCount: integer(step?.toolAttemptCount),
  errorCode: text(step?.errorCode),
  errorMessage: sanitizeAgentError(step?.errorMessage),
  createTime: step?.createTime ?? null,
  updateTime: step?.updateTime ?? null,
  completionTime: step?.completionTime ?? null
})

export const normalizeAgentRun = run => ({
  id: nullableInteger(run?.id),
  requestId: text(run?.requestId),
  goal: text(run?.goal),
  status: text(run?.status).toUpperCase(),
  currentStep: integer(run?.currentStep),
  maxSteps: integer(run?.maxSteps),
  finalResult: text(run?.finalResult),
  lastErrorCode: text(run?.lastErrorCode),
  lastErrorMessage: sanitizeAgentError(run?.lastErrorMessage),
  version: nullableInteger(run?.version),
  executionVersion: nullableInteger(run?.executionVersion),
  canResume: run?.canResume === true,
  recoveryState: text(run?.recoveryState).toUpperCase(),
  createTime: run?.createTime ?? null,
  updateTime: run?.updateTime ?? null,
  completionTime: run?.completionTime ?? null,
  steps: Array.isArray(run?.steps) ? run.steps.map(normalizeAgentStep) : []
})

export const normalizeAgentRunPage = payload => {
  const page = payload?.data ?? payload ?? {}
  return {
    items: Array.isArray(page.items) ? page.items.map(normalizeAgentRun) : [],
    page: Math.max(1, integer(page.page, 1)),
    pageSize: Math.max(1, integer(page.pageSize, 20)),
    total: Math.max(0, integer(page.total))
  }
}

export const agentStatusPresentation = status => RUN_STATUSES[text(status).toUpperCase()] || { label: '未知状态', tone: 'unknown' }
export const agentRecoveryLabel = state => RECOVERY_STATES[text(state).toUpperCase()] || '状态未知'

export const getRunRecoveryPresentation = run => {
  const recoveryState = text(run?.recoveryState).toUpperCase()
  const status = text(run?.status).toUpperCase()
  if (recoveryState === 'TERMINAL' && status === 'COMPLETED') return { label: 'Completed', description: 'The Agent completed this run.', tone: 'completed' }
  if (recoveryState === 'TERMINAL' && status === 'FAILED') return { label: 'Failed', description: 'The Agent could not complete this run.', tone: 'failed' }
  if (recoveryState === 'STALE_RECOVERABLE') return { label: 'Execution paused', description: 'The previous execution is no longer active. Resume continues this existing run from its persisted checkpoint.', tone: 'recoverable' }
  if (recoveryState === 'ACTIVE') return { label: 'Agent is running', description: 'Execution is active and progress will update automatically.', tone: 'running' }
  if (recoveryState === 'PENDING') return { label: 'Ready to start', description: 'This run is awaiting execution.', tone: 'pending' }
  const fallback = agentStatusPresentation(status)
  return { label: fallback.label, description: 'Run status is available in the execution metadata.', tone: fallback.tone }
}

export const canShowResume = run => run?.canResume === true
  && !['COMPLETED', 'FAILED'].includes(text(run?.status).toUpperCase())
  && ['PENDING', 'STALE_RECOVERABLE'].includes(text(run?.recoveryState).toUpperCase())

export const getResumeLabel = run => text(run?.recoveryState).toUpperCase() === 'PENDING' ? 'Start Agent' : 'Resume Agent'

export const findFailedStep = steps => Array.isArray(steps)
  ? [...steps].reverse().find(step => text(step?.status).toUpperCase() === 'FAILED') || null
  : null

export const formatAgentTime = value => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

export const safeJsonPresentation = value => {
  if (value == null || value === '') return '—'
  if (typeof value === 'object') {
    try { return JSON.stringify(value, null, 2) } catch { return String(value) }
  }
  const source = String(value)
  try { return JSON.stringify(JSON.parse(source), null, 2) } catch { return source }
}

export const parseStructuredPayload = value => {
  if (value == null || value === '') return { kind: 'empty', formatted: '—', value: null, isLong: false }
  if (typeof value === 'object') {
    try {
      const formatted = JSON.stringify(value, null, 2)
      return { kind: 'json', formatted, value, isLong: formatted.length > 600 }
    } catch {
      const formatted = String(value)
      return { kind: 'text', formatted, value: formatted, isLong: formatted.length > 600 }
    }
  }
  const source = String(value)
  try {
    const parsed = JSON.parse(source)
    const formatted = JSON.stringify(parsed, null, 2)
    return { kind: 'json', formatted, value: parsed, isLong: formatted.length > 600 }
  } catch {
    return { kind: 'text', formatted: source, value: source, isLong: source.length > 600 }
  }
}

export const finalResultPresentation = value => {
  if (value == null || String(value).trim() === '') return { ...parseStructuredPayload('Completed without a final result.'), empty: true }
  return { ...parseStructuredPayload(value), empty: false }
}

export const getToolPresentation = toolName => TOOL_PRESENTATIONS[text(toolName)] || {
  label: text(toolName) || 'No tool selected',
  description: text(toolName) ? 'Read-only Agent tool.' : 'This step does not use a tool.'
}

export const getStepPresentation = step => {
  const status = text(step?.status).toUpperCase()
  const decisionType = text(step?.decisionType).toUpperCase()
  if (status === 'FAILED') return { label: 'Step failed', phase: 'Result', tone: 'failed' }
  if (status === 'PENDING') return { label: 'Waiting for decision', phase: 'Decision', tone: 'pending' }
  if (status === 'TOOL_RUNNING') return { label: 'Retrieving context', phase: 'Action', tone: 'running' }
  if (status === 'DECIDED' && decisionType === 'TOOL_CALL') return { label: 'Action selected', phase: 'Action', tone: 'decided' }
  if (status === 'COMPLETED' && decisionType === 'DUPLICATE_TOOL_CALL') return { label: 'Reused previous result', phase: 'Observation', tone: 'reused' }
  if (status === 'COMPLETED' && decisionType === 'FINAL') return { label: 'Final result', phase: 'Result', tone: 'completed' }
  if (status === 'COMPLETED' && decisionType === 'TOOL_CALL') return { label: 'Observation received', phase: 'Observation', tone: 'completed' }
  if (status === 'DECIDED' && decisionType === 'FINAL') return { label: 'Final response selected', phase: 'Result', tone: 'decided' }
  return { label: status ? status.replaceAll('_', ' ').toLowerCase() : 'Preparing', phase: 'Decision', tone: 'unknown' }
}

export const formatAttemptSummary = (toolAttemptCount, retryCount, decisionType = '') => {
  const attempts = Math.max(0, integer(toolAttemptCount))
  const retries = Math.max(0, integer(retryCount))
  if (text(decisionType).toUpperCase() === 'DUPLICATE_TOOL_CALL' && attempts === 0) return 'Reused previous completed result'
  if (attempts === 0) return 'No tool attempt yet'
  if (attempts === 1 && retries === 0) return 'First attempt'
  if (retries === 1) return 'Retried once'
  if (retries > 1) return `Retried ${retries} times`
  return `${attempts} tool attempts`
}

export const chooseDefaultStep = steps => {
  if (!Array.isArray(steps) || steps.length === 0) return null
  const ordered = [...steps].sort((a, b) => integer(a.stepNumber) - integer(b.stepNumber))
  return [...ordered].reverse().find(step => text(step.status).toUpperCase() === 'FAILED')
    || [...ordered].reverse().find(step => ['PENDING', 'DECIDED', 'TOOL_RUNNING'].includes(text(step.status).toUpperCase()))
    || ordered.at(-1)
}

export const shouldFollowLatestStep = (selectedStepId, steps) => {
  const preferred = chooseDefaultStep(steps)
  return preferred != null && preferred.id === selectedStepId
}

export const buildAgentStepDetail = (step, runFinalResult = '') => {
  if (!step) return null
  const presentation = getStepPresentation(step)
  const tool = getToolPresentation(step.toolName)
  const decisionType = text(step.decisionType).toUpperCase()
  const status = text(step.status).toUpperCase()
  const kind = status === 'FAILED' ? 'failed'
    : decisionType === 'DUPLICATE_TOOL_CALL' ? 'duplicate'
      : decisionType === 'FINAL' ? 'final'
        : step.toolName ? 'tool' : 'active'
  return {
    kind,
    presentation,
    decisionSummary: text(step.decisionSummary) || 'No decision summary available yet.',
    tool,
    toolName: text(step.toolName),
    arguments: parseStructuredPayload(step.toolArguments),
    result: parseStructuredPayload(kind === 'final' ? (runFinalResult || step.toolResult) : step.toolResult),
    resultLabel: kind === 'final' ? 'Final Result' : kind === 'duplicate' ? 'Reused Tool Result' : 'Observation / Result',
    attemptSummary: formatAttemptSummary(step.toolAttemptCount, step.retryCount, decisionType),
    toolAttemptCount: integer(step.toolAttemptCount),
    retryCount: integer(step.retryCount),
    toolCallId: text(step.toolCallId),
    errorCode: text(step.errorCode),
    errorMessage: sanitizeAgentError(step.errorMessage),
    createTime: step.createTime ?? null,
    updateTime: step.updateTime ?? null,
    completionTime: step.completionTime ?? null,
    waitingForResult: status === 'TOOL_RUNNING' && !step.toolResult
  }
}

export const summarizeAgentGoal = (goal, length = 72) => {
  const normalized = text(goal).replace(/\s+/g, ' ').trim()
  return normalized.length > length ? `${normalized.slice(0, length).trimEnd()}…` : normalized || '未命名目标'
}

export const AGENT_EMPTY_STATE = Object.freeze({
  title: 'Agent Workspace',
  description: '给 Agent 一个目标，并查看它如何使用上下文和工具完成任务。',
  actionLabel: '新建目标（下一阶段开放）'
})

export const validateAgentGoal = value => {
  const goal = text(value).trim()
  if (!goal) return { valid: false, goal, error: '请输入 Agent Goal。' }
  if (goal.length > 4000) return { valid: false, goal, error: 'Goal 不能超过 4000 个字符。' }
  return { valid: true, goal, error: '' }
}

export const createAgentRequestId = cryptoSource => {
  const source = cryptoSource || globalThis.crypto
  const uuid = typeof source?.randomUUID === 'function'
    ? source.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`
  return `agent-${uuid}`.slice(0, 64)
}
