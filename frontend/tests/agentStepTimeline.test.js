import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { buildAgentStepDetail, chooseDefaultStep, formatAttemptSummary, getStepPresentation, getToolPresentation, parseStructuredPayload, sanitizeAgentError, shouldFollowLatestStep } from '../src/utils/agentPresentation.js'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const timeline = source('../src/components/agent/AgentStepTimeline.vue')
const detail = source('../src/components/agent/AgentStepDetail.vue')
const payload = source('../src/components/agent/AgentPayloadBlock.vue')
const workspace = source('../src/views/AgentWorkspace.vue')
const step = overrides => ({ id: 1, stepNumber: 1, status: 'COMPLETED', decisionType: 'TOOL_CALL', decisionSummary: 'Use context', toolName: 'get_character_context', toolArguments: '{"characterId":3}', toolResult: '{"name":"Aster"}', toolAttemptCount: 1, retryCount: 0, ...overrides })

test('TOOL_CALL timeline maps a completed action to an observation', () => assert.deepEqual(getStepPresentation(step()), { label: 'Observation received', phase: 'Observation', tone: 'completed' }))
test('FINAL timeline maps to the authoritative result phase', () => assert.equal(getStepPresentation(step({ decisionType: 'FINAL', toolName: '', toolResult: '' })).label, 'Final result'))
test('FAILED timeline is explicit and not color-only', () => assert.deepEqual(getStepPresentation(step({ status: 'FAILED' })), { label: 'Step failed', phase: 'Result', tone: 'failed' }))
test('DUPLICATE_TOOL_CALL timeline describes reuse', () => assert.equal(getStepPresentation(step({ decisionType: 'DUPLICATE_TOOL_CALL', toolAttemptCount: 0 })).label, 'Reused previous result'))
test('PENDING timeline describes waiting for a decision', () => assert.equal(getStepPresentation(step({ status: 'PENDING', decisionType: null })).label, 'Waiting for decision'))
test('TOOL_RUNNING timeline describes context retrieval', () => assert.equal(getStepPresentation(step({ status: 'TOOL_RUNNING', toolResult: '' })).label, 'Retrieving context'))

test('all three real read-only tools have friendly labels and descriptions', () => {
  assert.equal(getToolPresentation('get_character_context').label, 'Character Context')
  assert.equal(getToolPresentation('search_character_memory').label, 'Character Memory Search')
  assert.equal(getToolPresentation('get_world_context').label, 'World Context')
  assert.match(getToolPresentation('get_world_context').description, /Reads/)
})
test('unknown tool safely preserves its real name', () => assert.deepEqual(getToolPresentation('future_read_tool'), { label: 'future_read_tool', description: 'Read-only Agent tool.' }))
test('valid JSON arguments receive structured pretty presentation', () => { const view = parseStructuredPayload('{"characterId":3}'); assert.equal(view.kind, 'json'); assert.match(view.formatted, /"characterId": 3/) })
test('invalid JSON arguments safely remain text', () => assert.deepEqual(parseStructuredPayload('{broken'), { kind: 'text', formatted: '{broken', value: '{broken', isLong: false }))
test('valid JSON results receive structured pretty presentation', () => assert.equal(parseStructuredPayload('[{"id":1}]').kind, 'json'))
test('long text is marked for bounded presentation', () => assert.equal(parseStructuredPayload('x'.repeat(601)).isLong, true))
test('first tool attempt is distinct from retry count zero', () => assert.equal(formatAttemptSummary(1, 0), 'First attempt'))
test('second tool attempt with one retry is described naturally', () => assert.equal(formatAttemptSummary(2, 1), 'Retried once'))
test('duplicate completed tool with zero attempts describes reuse', () => assert.equal(formatAttemptSummary(0, 0, 'DUPLICATE_TOOL_CALL'), 'Reused previous completed result'))

test('failed detail model exposes safe error and attempt metadata', () => { const model = buildAgentStepDetail(step({ status: 'FAILED', errorCode: 'TOOL_FAILED', errorMessage: 'Readable\n at com.example.Agent.run(Agent.java:1)' })); assert.equal(model.kind, 'failed'); assert.equal(model.errorMessage, 'Readable'); assert.equal(model.toolAttemptCount, 1) })
test('final detail model prioritizes Run finalResult', () => { const model = buildAgentStepDetail(step({ decisionType: 'FINAL', toolName: '', toolResult: 'step result' }), 'run result'); assert.equal(model.kind, 'final'); assert.equal(model.result.formatted, 'run result') })
test('tool detail model contains arguments and observation', () => { const model = buildAgentStepDetail(step()); assert.equal(model.kind, 'tool'); assert.equal(model.arguments.value.characterId, 3); assert.equal(model.result.value.name, 'Aster') })
test('duplicate detail model has zero execution attempts and reused result', () => { const model = buildAgentStepDetail(step({ decisionType: 'DUPLICATE_TOOL_CALL', toolAttemptCount: 0, retryCount: 0 })); assert.equal(model.kind, 'duplicate'); assert.equal(model.resultLabel, 'Reused Tool Result'); assert.equal(model.toolAttemptCount, 0) })
test('active tool detail waits instead of inventing a result', () => assert.equal(buildAgentStepDetail(step({ status: 'TOOL_RUNNING', toolResult: '' })).waitingForResult, true))

test('default selection prioritizes the latest failed step', () => assert.equal(chooseDefaultStep([step({ id: 1 }), step({ id: 2, stepNumber: 2, status: 'FAILED' })]).id, 2))
test('default selection prioritizes latest active step when none failed', () => assert.equal(chooseDefaultStep([step({ id: 1 }), step({ id: 2, stepNumber: 2, status: 'TOOL_RUNNING' })]).id, 2))
test('default selection falls back to the last ordered step', () => assert.equal(chooseDefaultStep([step({ id: 8, stepNumber: 2 }), step({ id: 7, stepNumber: 1 })]).id, 8))
test('manual old-step selection survives polling', () => assert.equal(shouldFollowLatestStep(1, [step({ id: 1 }), step({ id: 2, stepNumber: 2 })]), false))
test('latest selection follows only while it is preferred', () => assert.equal(shouldFollowLatestStep(2, [step({ id: 1 }), step({ id: 2, stepNumber: 2 })]), true))
test('new polling steps are rendered from reactive props rather than a snapshot', () => { assert.match(timeline, /computed\(\(\)=>\[\.\.\.props\.steps\]/); assert.match(workspace, /watch\(\(\)=>visibleRun\.value\?\.steps/) })
test('empty timeline communicates preparation', () => assert.match(timeline, /Agent is preparing the first decision\./))
test('mobile detail uses Element Plus drawer and retains local selectedStepId', () => { assert.match(workspace, /el-drawer v-model="detailDrawerOpen"/); assert.match(workspace, /selectedStepId=ref\(null\)/) })
test('timeline supports keyboard selection and exposes selected state', () => { assert.match(timeline, /ArrowDown/); assert.match(timeline, /aria-selected/); assert.match(timeline, /aria-current/) })
test('long payloads are bounded and offer labelled expand collapse', () => { assert.match(payload, /max-height:420px/); assert.match(payload, /aria-label/); assert.match(payload, /is-collapsed/) })
test('presentation contains no chain-of-thought or hidden reasoning labels', () => assert.doesNotMatch(`${timeline}\n${detail}\n${workspace}`, /chain.of.thought|thought process|hidden reasoning|思维链|思考过程/i))
test('raw Java stack frames are removed before presentation', () => assert.equal(sanitizeAgentError('Useful message\n\tat com.acme.Tool.run(Tool.java:10)\n\tat java.base.Thread.run(Thread.java:1)'), 'Useful message'))
