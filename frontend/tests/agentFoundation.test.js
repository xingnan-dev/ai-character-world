import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  AGENT_EMPTY_STATE,
  agentRecoveryLabel,
  agentStatusPresentation,
  normalizeAgentRun,
  normalizeAgentRunPage,
  parseAgentRunId,
  safeJsonPresentation,
  summarizeAgentGoal
} from '../src/utils/agentPresentation.js'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const router = source('../src/router/index.js')
const topNav = source('../src/components/layout/AppTopNav.vue')
const sideMenu = source('../src/components/home/SideMenu.vue')
const api = source('../src/api/agent.js')
const store = source('../src/stores/agent.js')
const workspace = source('../src/views/AgentWorkspace.vue')
const runList = source('../src/components/agent/AgentRunList.vue')

test('/agent route exists with AppShell and Agent section metadata', () => {
  assert.match(router, /path: '\/agent'[\s\S]*name: 'AgentWorkspace'[\s\S]*appShell: true[\s\S]*navSection: 'agent'/)
})

test('/agent route requires authentication', () => {
  assert.match(router, /path: '\/agent'[\s\S]*requiresAuth: true/)
})

test('Agent is a first-level destination in both navigation surfaces', () => {
  assert.match(topNav, /label:'Agent',to:'\/agent',section:'agent'/)
  assert.match(sideMenu, /key: 'agent', label: 'Agent'[^\n]*path: '\/agent'/)
})

test('Agent navigation participates in active route state', () => {
  assert.match(topNav, /route\.path\.startsWith\('\/agent'\)\?'agent'/)
  assert.match(topNav, /activeSection === item\.section/)
  assert.match(sideMenu, /currentPath\.startsWith\(i\.path\)/)
})

test('Agent API client matches all real backend endpoints without repeating /api', () => {
  assert.match(api, /url: '\/agent\/runs', method: 'post'/)
  assert.match(api, /`\/agent\/runs\/\$\{runId\}`[^\n]*method: 'get'/)
  assert.match(api, /url: '\/agent\/runs', method: 'get', params/)
  assert.match(api, /`\/agent\/runs\/\$\{runId\}\/resume`[^\n]*method: 'post'/)
  assert.doesNotMatch(api, /\/api\/agent/)
})

test('list DTO normalization preserves backend newest-first response order', () => {
  const page = normalizeAgentRunPage({ data: { items: [
    { id: 3, goal: 'newest', status: 'RUNNING', currentStep: 1, maxSteps: 6 },
    { id: 2, goal: 'older', status: 'COMPLETED', currentStep: 2, maxSteps: 6 }
  ], page: 1, pageSize: 20, total: 2 } })
  assert.deepEqual(page.items.map(run => run.id), [3, 2])
  assert.equal(page.total, 2)
  assert.equal(page.items[0].steps.length, 0)
})

test('detail DTO normalization uses only real Run and Step fields', () => {
  const run = normalizeAgentRun({ id: '9', requestId: 'r', goal: 'goal', status: 'completed', currentStep: 1, maxSteps: 6,
    finalResult: 'done', canResume: false, recoveryState: 'terminal', steps: [{ id: 4, stepNumber: 1, decisionType: 'FINAL', decisionSummary: 'done', status: 'COMPLETED', retryCount: 0, toolAttemptCount: 0 }] })
  assert.equal(run.id, 9)
  assert.equal(run.status, 'COMPLETED')
  assert.equal(run.recoveryState, 'TERMINAL')
  assert.deepEqual(run.steps.map(step => [step.stepNumber, step.decisionType, step.toolAttemptCount]), [[1, 'FINAL', 0]])
})

test('status presentation supplies readable text in addition to tone', () => {
  assert.deepEqual(agentStatusPresentation('RUNNING'), { label: '执行中', tone: 'running' })
  assert.deepEqual(agentStatusPresentation('FAILED'), { label: '执行失败', tone: 'failed' })
  assert.equal(agentStatusPresentation('unexpected').label, '未知状态')
})

test('recovery presentation distinguishes active and stale runs', () => {
  assert.equal(agentRecoveryLabel('ACTIVE'), '正在执行')
  assert.equal(agentRecoveryLabel('STALE_RECOVERABLE'), '可恢复执行')
  assert.notEqual(agentRecoveryLabel('ACTIVE'), agentRecoveryLabel('STALE_RECOVERABLE'))
})

test('malformed JSON presentation safely falls back to original text', () => {
  assert.equal(safeJsonPresentation('{broken'), '{broken')
  assert.match(safeJsonPresentation('{"worldId":1}'), /"worldId": 1/)
})

test('goal summary is bounded and suitable for recent Run rendering', () => {
  assert.equal(summarizeAgentGoal('  short   goal '), 'short goal')
  assert.match(summarizeAgentGoal('x'.repeat(90), 20), /^x{20}…$/)
  assert.match(runList, /summarizeAgentGoal\(run\.goal\)/)
  assert.doesNotMatch(runList, /finalResult/)
})

test('selectedRunId store behavior validates IDs and loads detail', () => {
  assert.match(store, /const id = parseAgentRunId\(runId\)/)
  assert.match(store, /this\.selectedRunId = id[\s\S]*return this\.loadRun\(id, generation\)/)
  assert.match(store, /clearSelection\(\)[\s\S]*this\.selectedRunId = null/)
})

test('detail generation fencing uses stable Pinia state identity across nested actions', () => {
  assert.match(store, /const detailGenerationKey = store => store\?\.\$state \|\| store/)
  assert.match(store, /generation !== detailGenerationOf\(this\)/)
})

test('query runId parsing accepts only positive safe integer identities', () => {
  assert.equal(parseAgentRunId('123'), 123)
  assert.equal(parseAgentRunId(['7']), 7)
  for (const invalid of [undefined, '', '0', '-1', '12x', '9007199254740992']) assert.equal(parseAgentRunId(invalid), null)
  assert.match(workspace, /watch\(\(\)=>route\.query\.runId/)
})

test('initial URL restoration loads the Run after the recent list is ready', () => {
  assert.match(workspace, /onMounted\(async\(\)=>\{agentStore\.attachVisibility\(\);await loadRuns\(\);await syncRouteRun\(route\.query\.runId\)\}\)/)
  assert.doesNotMatch(workspace, /watch\(\(\)=>route\.query\.runId[\s\S]{0,500}immediate:true/)
})

test('empty state is product-facing and opens the Goal composer', () => {
  assert.equal(AGENT_EMPTY_STATE.title, 'Agent Workspace')
  assert.match(AGENT_EMPTY_STATE.description, /目标/)
  assert.match(workspace, /@click="openComposer">＋ 新建目标/)
  assert.match(workspace, /title="新建 Agent Goal"/)
})

test('invalid runId produces an explicit detail error instead of an API call', () => {
  assert.match(store, /if \(!id\) \{[\s\S]*this\.detailError = '无效的 Agent Run ID。'[\s\S]*return null/)
  assert.equal(parseAgentRunId('not-an-id'), null)
})

test('Agent store persists only selectedRunId and not context payloads', () => {
  const persistedPaths = store.match(/paths: \[([^\]]*)\]/)?.[1]
  assert.equal(persistedPaths, "'selectedRunId'")
  for (const forbidden of ["'selectedRun'", "'runs'", "'steps'", "'toolResult'"]) assert.ok(!persistedPaths.includes(forbidden))
})

test('workspace keeps responsive foundations without push transports', () => {
  assert.match(workspace, /grid-template-columns:280px minmax\(0,1fr\) 340px/)
  assert.match(workspace, /v-loading="true"/)
  assert.match(workspace, /role="alert"/)
  assert.match(workspace, /el-drawer[\s\S]*runsDrawerOpen/)
  assert.match(workspace, /AgentStepTimeline/)
  assert.match(workspace, /AgentStepDetail/)
  assert.doesNotMatch(workspace, /resumeAgentRun|EventSource|WebSocket/)
})
