import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { canShowResume, finalResultPresentation, findFailedStep, getResumeLabel, getRunRecoveryPresentation, sanitizeRunError } from '../src/utils/agentPresentation.js'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const api = source('../src/api/agent.js')
const store = source('../src/stores/agent.js')
const workspace = source('../src/views/AgentWorkspace.vue')
const runList = source('../src/components/agent/AgentRunList.vue')
const run = overrides => ({ id: 4, status: 'PENDING', canResume: true, recoveryState: 'PENDING', steps: [], ...overrides })

test('canResume false hides Resume', () => assert.equal(canShowResume(run({ canResume: false })), false))
test('PENDING plus canResume shows Start Agent', () => { const value = run(); assert.equal(canShowResume(value), true); assert.equal(getResumeLabel(value), 'Start Agent') })
test('STALE_RECOVERABLE shows Resume Agent', () => { const value = run({ status: 'RUNNING', recoveryState: 'STALE_RECOVERABLE' }); assert.equal(canShowResume(value), true); assert.equal(getResumeLabel(value), 'Resume Agent') })
test('ACTIVE hides Resume', () => assert.equal(canShowResume(run({ status: 'RUNNING', recoveryState: 'ACTIVE' })), false))
test('COMPLETED hides Resume despite inconsistent flag', () => assert.equal(canShowResume(run({ status: 'COMPLETED', recoveryState: 'TERMINAL' })), false))
test('FAILED hides Resume and never implies recovery', () => assert.equal(canShowResume(run({ status: 'FAILED', recoveryState: 'TERMINAL' })), false))

test('Resume API posts to the existing Run endpoint', () => assert.match(api, /resumeAgentRun = runId => request\(\{ url: `\/agent\/runs\/\$\{runId\}\/resume`, method: 'post' \}\)/))
test('Resume keeps the selected runId and does not navigate', () => { assert.match(store, /resumeAgentRun\(id\)/); assert.doesNotMatch(workspace.match(/const resumeSelectedRun[^\n]*/)?.[0] || '', /router|replace|push/) })
test('Resume does not create a requestId or new Run', () => { const action = store.match(/async resumeRun[\s\S]*?\n    },\n    async selectRun/)?.[0] || ''; assert.doesNotMatch(action, /createAgentRequestId|createAgentRun|requestId/) })
test('Resume success replaces selected Run and summary', () => assert.match(store, /this\.selectedRun = run[\s\S]*syncRunSummary\(this, run\)/))
test('Resume non-terminal response starts existing polling', () => assert.match(store, /if \(!isTerminalAgentStatus\(run\.status\)\) this\.startPolling\(id\)/))
test('Resume terminal response does not poll', () => assert.doesNotMatch(store, /if \(isTerminalAgentStatus\(run\.status\)\) this\.startPolling/))
test('duplicate Resume calls share one in-flight request', () => assert.match(store, /if \(duplicate\?\.runId === id\) return duplicate\.promise/))
test('stale Resume response cannot overwrite another selected Run', () => assert.match(store, /generation !== detailGenerationOf\(this\) \|\| this\.selectedRunId !== id \|\| run\.id !== id/))
test('Resume error preserves existing Run and Steps', () => { const action = store.match(/async resumeRun[\s\S]*?\n    },\n    async selectRun/)?.[0] || ''; assert.doesNotMatch(action, /selectedRun\s*=\s*null|steps\s*=/); assert.match(action, /this\.resumeError = messageOf\(error\)/) })
test('Resume retry uses the same selected Run', () => assert.match(workspace, /Retry Resume[\s\S]*resumeSelectedRun/))
test('refresh normalization keeps backend canResume changes', () => assert.match(store, /this\.selectedRun = run[\s\S]*syncRunSummary/))

test('stale recovery presentation explains checkpoint continuation', () => assert.match(getRunRecoveryPresentation(run({ status: 'RUNNING', recoveryState: 'STALE_RECOVERABLE' })).description, /persisted checkpoint/))
test('active recovery presentation says Agent is running', () => assert.equal(getRunRecoveryPresentation(run({ status: 'RUNNING', recoveryState: 'ACTIVE' })).label, 'Agent is running'))
test('terminal recovery presentation distinguishes completion and failure', () => { assert.equal(getRunRecoveryPresentation(run({ status: 'COMPLETED', recoveryState: 'TERMINAL' })).label, 'Completed'); assert.equal(getRunRecoveryPresentation(run({ status: 'FAILED', recoveryState: 'TERMINAL' })).label, 'Failed') })
test('completed final result uses the Run value', () => assert.equal(finalResultPresentation('Authoritative answer').formatted, 'Authoritative answer'))
test('completed null final result is honest and explicit', () => { const value = finalResultPresentation(null); assert.equal(value.empty, true); assert.equal(value.formatted, 'Completed without a final result.') })
test('long final result uses bounded expandable presentation', () => assert.equal(finalResultPresentation('x'.repeat(601)).isLong, true))
test('failed Run renders code, message and terminal times', () => { assert.match(workspace, /Agent Run Failed/); assert.match(workspace, /lastErrorCode/); assert.match(workspace, /completionTime/) })
test('failed Run error sanitization removes exception dump noise', () => assert.equal(sanitizeRunError('com.acme.AgentException: Useful failure\n\tat com.acme.Agent.run(Agent.java:4)\n... 2 more'), 'Useful failure'))
test('View failed step selects the latest failed Step', () => { assert.equal(findFailedStep([{ id: 1, status: 'FAILED' }, { id: 2, status: 'FAILED' }]).id, 2); assert.match(workspace, /viewFailedStep[\s\S]*selectStep\(failedStep\.value\.id\)/) })
test('terminal transition still stops polling through existing controller contract', () => assert.match(store, /onTerminal: run =>/))
test('Recent Runs presents a resumable badge without an action', () => { assert.match(runList, /Resume available/); assert.doesNotMatch(runList, /resumeRun|resumeAgentRun/) })
test('stale state is never calculated from client time', () => assert.doesNotMatch(`${store}\n${workspace}`, /Date\.now\(\)[\s\S]{0,120}(stale|recovery)|completionTime\s*[<>]|updateTime\s*[<>]/i))
test('Resume has accessible loading, alert and explicit labels', () => { assert.match(workspace, /:aria-label="resumeLabel"/); assert.match(workspace, /role="alert"/); assert.match(workspace, /:loading="agentStore\.resumeLoading"/) })
