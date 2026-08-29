import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const api = source('../src/api/worldInteraction.js')
const store = source('../src/stores/worldInteraction.js')
const router = source('../src/router/index.js')
const detail = source('../src/views/WorldDetail.vue')
const page = source('../src/views/WorldInteraction.vue')
const timeline = source('../src/components/world/WorldTimeline.vue')

test('authenticated interaction route and detail entry are connected', () => {
  assert.match(router, /path:\s*'\/worlds\/:worldId\/interaction'[\s\S]*requiresAuth:\s*true/)
  assert.match(detail, /router\.push\(`\/worlds\/\$\{id\}\/interaction`\)/)
})

test('interaction API covers create, execute, active, round, events and timeline', () => {
  for (const fragment of ['/rounds', '/execute', '/active', '/events', '/timeline']) assert.match(api, new RegExp(fragment))
  assert.match(api, /method:\s*'post'/)
  assert.match(api, /method:\s*'get'/)
})

test('store persists before create and reuses the same pending request', () => {
  assert.match(store, /this\.pendingSubmission\s*=\s*\{[\s\S]*requestId:\s*createWorldRequestId\(\)/)
  assert.match(store, /this\.persistPending\(\)[\s\S]*await this\.createPendingRound/)
  assert.match(store, /requestId:\s*this\.pendingSubmission\.requestId/)
  assert.doesNotMatch(store, /ChatSession|ChatMessage|useChatStore|EventSource|WebSocket/)
})

test('empty active initialization becomes ready without a pending submission or undefined requests', () => {
  assert.match(store, /const active = dataOf\(activeResponse\)[\s\S]*if \(active\) await this\.adoptActiveRound\(active, generation\)[\s\S]*else await this\.resumeSavedSubmission\(generation\)/)
  assert.match(store, /if \(!this\.activeRound && !this\.pendingSubmission\) this\.phase = 'READY'/)
  assert.doesNotMatch(store, /rounds\/undefined|syncKnownRound\(undefined|adoptActiveRound\(undefined/)
})

test('first send after empty active creates one pending request and calls create Round', () => {
  assert.match(store, /async send\(userInput\)[\s\S]*this\.pendingSubmission = \{[\s\S]*requestId: createWorldRequestId\(\)[\s\S]*this\.persistPending\(\)[\s\S]*await this\.createPendingRound\(this\.generation\)/)
  assert.match(store, /async createPendingRound[\s\S]*createWorldRound\(this\.worldId, \{[\s\S]*requestId: this\.pendingSubmission\.requestId/)
})

test('active null still syncs the known round and terminal refreshes timeline', () => {
  assert.match(store, /const knownId = active\?\.id \|\| this\.activeRound\?\.id \|\| this\.pendingSubmission\?\.roundId/)
  assert.match(store, /if \(knownId\) await this\.syncKnownRound\(knownId, generation\)/)
  assert.match(store, /finishRound[\s\S]*refreshTimelineHead/)
})

test('queue busy and uncertain execute retain the original round for recovery', () => {
  assert.match(store, /WORLD_EXECUTION_BUSY[\s\S]*this\.queueBusy = true/)
  assert.match(store, /DISPATCH_UNCERTAIN/)
  assert.match(store, /recoverExecution/)
  assert.match(store, /dispatchingRoundIds/)
})

test('interaction page cleans up visibility listener, timers and requests', () => {
  assert.match(page, /visibilitychange/)
  assert.match(page, /store\.setPageHidden\(document\.hidden\)/)
  assert.match(page, /onUnmounted[\s\S]*store\.dispose\(\)/)
  assert.match(store, /controller\.abort\(\)/)
  assert.match(store, /clearTimeout\(run\.timer\)/)
})

test('timeline uses participant mapping and no remote images or raw HTML', () => {
  assert.match(timeline, /participantForEvent\(props\.participants, event\)/)
  assert.doesNotMatch(page + timeline, /v-html|<img|AvatarRenderer|\.vrm/)
})

test('layout is desktop two-column and responsive single-column', () => {
  assert.match(page, /grid-template-columns:310px minmax\(0,1fr\)/)
  assert.match(page, /@media\(max-width:850px\)[\s\S]*grid-template-columns:1fr/)
  assert.match(page, /var\(--world-bg\)/)
})
