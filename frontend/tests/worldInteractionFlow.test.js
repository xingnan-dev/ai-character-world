import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { worldInteractionLocation } from '../src/utils/worldNavigation.js'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const api = source('../src/api/worldInteraction.js')
const store = source('../src/stores/worldInteraction.js')
const storeFactory = source('../src/stores/worldInteractionStoreFactory.js')
const router = source('../src/router/index.js')
const detail = source('../src/views/WorldDetail.vue')
const page = source('../src/views/WorldInteraction.vue')
const timeline = source('../src/components/world/WorldTimeline.vue')

test('authenticated interaction route and detail entry are connected', () => {
  assert.match(router, /path:\s*'\/worlds\/:worldId\/interaction'[\s\S]*requiresAuth:\s*true/)
  assert.match(detail, /worldInteractionLocation\(route\.params\.worldId\)/)
  assert.deepEqual(worldInteractionLocation('42'), { name: 'WorldInteraction', params: { worldId: '42' } })
  assert.equal(worldInteractionLocation('42').params.worldId, '42')
  assert.notEqual(worldInteractionLocation('42').name, 'Chat')
  assert.doesNotMatch(detail, /router\.push\(['"`]\/chat/)
})

test('interaction API covers create, execute, active, round, events and timeline', () => {
  for (const fragment of ['/rounds', '/execute', '/active', '/events', '/timeline']) assert.match(api, new RegExp(fragment))
  assert.match(api, /method:\s*'post'/)
  assert.match(api, /method:\s*'get'/)
})

test('store persists before create and reuses the same pending request', () => {
  assert.match(storeFactory, /this\.pendingSubmission\s*=\s*\{[\s\S]*requestId:\s*createWorldRequestId\(\)/)
  assert.match(storeFactory, /this\.persistPending\(\)[\s\S]*await this\.createPendingRound/)
  assert.match(storeFactory, /requestId:\s*this\.pendingSubmission\.requestId/)
  assert.doesNotMatch(storeFactory, /ChatSession|ChatMessage|useChatStore|EventSource|WebSocket/)
})

test('empty active initialization becomes ready without a pending submission or undefined requests', () => {
  assert.match(storeFactory, /const active = dataOf\(activeResponse\)[\s\S]*if \(active\) await this\.adoptActiveRound\(active, generation\)[\s\S]*else await this\.resumeSavedSubmission\(generation\)/)
  assert.match(storeFactory, /if \(!this\.activeRound && !this\.pendingSubmission\) this\.phase = 'READY'/)
  assert.doesNotMatch(storeFactory, /rounds\/undefined|syncKnownRound\(undefined|adoptActiveRound\(undefined/)
})

test('first send after empty active creates one pending request and calls create Round', () => {
  assert.match(storeFactory, /async send\(userInput\)[\s\S]*this\.pendingSubmission = \{[\s\S]*requestId: createWorldRequestId\(\)[\s\S]*this\.persistPending\(\)[\s\S]*await this\.createPendingRound\(this\.generation\)/)
  assert.match(storeFactory, /async createPendingRound[\s\S]*createWorldRound\(this\.worldId, \{[\s\S]*requestId: this\.pendingSubmission\.requestId/)
})

test('active null still syncs the known round and terminal refreshes timeline', () => {
  assert.match(storeFactory, /const knownId = active\?\.id \|\| this\.activeRound\?\.id \|\| this\.pendingSubmission\?\.roundId/)
  assert.match(storeFactory, /if \(knownId\) await this\.syncKnownRound\(knownId, generation\)/)
  assert.match(storeFactory, /finishRound[\s\S]*refreshTimelineHead/)
})

test('queue busy and uncertain execute retain the original round for recovery', () => {
  assert.match(storeFactory, /WORLD_EXECUTION_BUSY[\s\S]*this\.queueBusy = true/)
  assert.match(storeFactory, /DISPATCH_UNCERTAIN/)
  assert.match(storeFactory, /recoverExecution/)
  assert.match(storeFactory, /dispatchingRoundIds/)
})

test('interaction page cleans up visibility listener, timers and requests', () => {
  assert.match(page, /visibilitychange/)
  assert.match(page, /store\.setPageHidden\(document\.hidden\)/)
  assert.match(page, /onMounted\(\(\) => \{[\s\S]*handleVisibility\(\)[\s\S]*initialize\(\)/)
  assert.match(storeFactory, /stopRuntime\(\) \{(?:(?!run\.hidden\s*=)[\s\S])*?this\.generation \+= 1/)
  assert.match(page, /onUnmounted[\s\S]*store\.dispose\(\)/)
  assert.match(storeFactory, /controller\.abort\(\)/)
  assert.match(storeFactory, /clearTimeout\(run\.timer\)/)
})

test('store wrapper preserves the public Pinia export and injects all seven APIs', () => {
  assert.match(store, /export const useWorldInteractionStore = defineStore\('worldInteraction', createWorldInteractionStoreDefinition\(\{/)
  for (const name of ['createWorldRound', 'executeWorldRound', 'getActiveWorldRound', 'getWorldById', 'getWorldRound', 'getWorldRoundEvents', 'getWorldTimeline']) {
    assert.match(store, new RegExp(`\\b${name}\\b`))
  }
})

test('timeline uses the snapshot view model and no remote images or raw HTML', () => {
  assert.match(timeline, /buildWorldTimelineViewModel\(props\.items, props\.participants\)/)
  assert.match(timeline, /event\.kind === 'USER'/)
  assert.match(timeline, /event\.kind === 'AI'/)
  assert.doesNotMatch(page + timeline, /v-html|<img|AvatarRenderer|\.vrm/)
})

test('layout is desktop two-column and responsive single-column', () => {
  assert.match(page, /grid-template-columns:310px minmax\(0,1fr\)/)
  assert.match(page, /@media\(max-width:850px\)[\s\S]*grid-template-columns:1fr/)
  assert.match(page, /var\(--world-bg\)/)
})
