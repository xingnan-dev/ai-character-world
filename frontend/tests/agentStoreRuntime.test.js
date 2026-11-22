import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createPinia, defineStore, setActivePinia } from 'pinia'

const storeUrl = new URL('../src/stores/agent.js', import.meta.url)
const api = {}
globalThis.__p6AgentTestApi = api
globalThis.__p6AgentDefineStore = defineStore
globalThis.localStorage = { getItem: () => null, setItem() {}, removeItem() {} }
const names = ['createAgentRun', 'getAgentRun', 'listAgentRuns', 'resumeAgentRun']
const moduleUrl = code => 'data:text/javascript,' + encodeURIComponent(code)
const apiUrl = moduleUrl(names.map(name => `export const ${name} = (...args) => globalThis.__p6AgentTestApi.${name}(...args)`).join('\n'))
const piniaUrl = moduleUrl('export const defineStore = globalThis.__p6AgentDefineStore')
// Replace import dependencies only. Assertions below execute the real store actions
// through Pinia; they do not infer runtime behavior from source text patterns.
const source = readFileSync(storeUrl, 'utf8')
  .replace("from 'pinia'", `from '${piniaUrl}'`)
  .replace("from '../api/agent'", `from '${apiUrl}'`)
  .replace("from '../utils/agentPollingController'", `from '${new URL('../src/utils/agentPollingController.js', import.meta.url).href}'`)
  .replace("from '../utils/agentPresentation'", `from '${new URL('../src/utils/agentPresentation.js', import.meta.url).href}'`)
const { useAgentStore } = await import(moduleUrl(source))
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }
const fresh = () => { setActivePinia(createPinia()); api.listAgentRuns = async () => ({ data: { items: [] } }); return useAgentStore() }
// Pinia/devtools can supply different action receiver proxies for one logical store.
const receiver = store => new Proxy(store, {})

test('concurrent create actions share a single request across receiver proxies', async () => {
  const store = fresh(), pending = deferred(), calls = []
  api.createAgentRun = data => { calls.push(data); return pending.promise }
  const a = Reflect.apply(store.createRun, receiver(store), ['same goal'])
  const b = Reflect.apply(store.createRun, receiver(store), ['same goal'])
  const count = calls.length
  pending.resolve({ data: { id: 20, status: 'COMPLETED', steps: [] } })
  await Promise.all([a, b])
  assert.equal(count, 1)
})

test('concurrent resume actions share a single request across receiver proxies', async () => {
  const store = fresh(), pending = deferred(); let calls = 0
  store.selectedRunId = 20; store.selectedRun = { id: 20, status: 'RUNNING', recoveryState: 'STALE_RECOVERABLE', canResume: true }
  api.resumeAgentRun = () => { calls += 1; return pending.promise }
  const a = Reflect.apply(store.resumeRun, receiver(store), [20])
  const b = Reflect.apply(store.resumeRun, receiver(store), [20])
  pending.resolve({ data: { id: 20, status: 'COMPLETED', steps: [] } })
  await Promise.all([a, b]); assert.equal(calls, 1)
})

test('polling started through one receiver is stopped through another', () => {
  const originalSet = globalThis.setTimeout, originalClear = globalThis.clearTimeout
  const timers = new Map(); let sequence = 0
  globalThis.setTimeout = fn => { timers.set(++sequence, fn); return sequence }
  globalThis.clearTimeout = id => timers.delete(id)
  const store = fresh()
  try {
    store.selectedRunId = 20; store.selectedRun = { id: 20, status: 'RUNNING' }
    Reflect.apply(store.startPolling, receiver(store), [20]); assert.equal(timers.size, 1)
    Reflect.apply(store.stopPolling, receiver(store), []); assert.equal(timers.size, 0)
  } finally { store.disposePolling(); globalThis.setTimeout = originalSet; globalThis.clearTimeout = originalClear }
})

test('late detail response for Run A cannot replace selected Run B', async () => {
  const store = fresh(), pending = deferred()
  api.getAgentRun = id => id === 20 ? pending.promise : Promise.resolve({ data: { id, status: 'COMPLETED', steps: [] } })
  const a = Reflect.apply(store.selectRun, receiver(store), [20])
  await Reflect.apply(store.selectRun, receiver(store), [21])
  pending.resolve({ data: { id: 20, status: 'COMPLETED', steps: [] } }); await a
  assert.equal(store.selectedRunId, 21); assert.equal(store.selectedRun.id, 21)
})


test('failed resume preserves existing Run and completed Step data', async () => {
  const store = fresh()
  store.selectedRunId = 20
  store.selectedRun = { id: 20, status: 'RUNNING', recoveryState: 'STALE_RECOVERABLE', canResume: true, steps: [{ id: 7, status: 'COMPLETED' }] }
  api.resumeAgentRun = async () => { throw new Error('controlled resume transport failure') }
  try {
    await assert.rejects(store.resumeRun(20), /controlled resume transport failure/)
    assert.equal(store.selectedRun.id, 20)
    assert.deepEqual(store.selectedRun.steps, [{ id: 7, status: 'COMPLETED' }])
    assert.match(store.resumeError, /controlled resume transport failure/)
  } finally { store.disposePolling() }
})
