import test from 'node:test'
import assert from 'node:assert/strict'
import { createPinia, defineStore, setActivePinia } from 'pinia'
import { createWorldInteractionStoreDefinition } from '../src/stores/worldInteractionStoreFactory.js'

let storeSequence = 0

function deferred() {
  let resolve
  let reject
  const promise = new Promise((onResolve, onReject) => { resolve = onResolve; reject = onReject })
  return { promise, resolve, reject }
}

function fakeTimers() {
  const originalSetTimeout = globalThis.setTimeout
  const originalClearTimeout = globalThis.clearTimeout
  let sequence = 0
  const timers = new Map()
  globalThis.setTimeout = (callback, delay = 0) => {
    const id = ++sequence
    timers.set(id, { callback, delay })
    return id
  }
  globalThis.clearTimeout = id => timers.delete(id)
  return {
    count: () => timers.size,
    nextDelay: () => timers.values().next().value?.delay,
    fireNext() {
      const entry = timers.entries().next().value
      if (!entry) return Promise.resolve(false)
      const [id, timer] = entry
      timers.delete(id)
      return Promise.resolve(timer.callback()).then(() => true)
    },
    restore() {
      timers.clear()
      globalThis.setTimeout = originalSetTimeout
      globalThis.clearTimeout = originalClearTimeout
    }
  }
}

function defaultApi(overrides = {}) {
  return {
    createWorldRound: async () => ({ data: null }),
    executeWorldRound: async () => ({ data: null }),
    getActiveWorldRound: async () => ({ data: null }),
    getWorldById: async id => ({ data: { id } }),
    getWorldRound: async (worldId, roundId) => ({ data: { id: roundId, status: 'PENDING' } }),
    getWorldRoundEvents: async () => ({ data: [] }),
    getWorldTimeline: async () => ({ data: { items: [], hasMore: false, nextBeforeRoundId: null } }),
    ...overrides
  }
}

function freshStore(api = defaultApi()) {
  setActivePinia(createPinia())
  const useStore = defineStore(`world-interaction-polling-${++storeSequence}`, createWorldInteractionStoreDefinition(api))
  return useStore()
}

test('real startPolling keeps exactly one timer across repeated starts', () => {
  const timers = fakeTimers()
  const store = freshStore()
  try {
    store.activeRound = { id: 1, status: 'PENDING' }
    store.startPolling()
    store.startPolling()
    store.startPolling(true)
    assert.equal(timers.count(), 1)
    assert.equal(timers.nextDelay(), 0)
  } finally {
    store.dispose()
    timers.restore()
  }
})

test('foreground resume during a real in-flight poll does not overlap and leaves one timer', async () => {
  const timers = fakeTimers()
  const active = deferred()
  let activeCalls = 0
  const store = freshStore(defaultApi({
    getActiveWorldRound: async () => { activeCalls += 1; return active.promise }
  }))
  try {
    store.worldId = 1
    store.activeRound = { id: 11, status: 'PENDING' }
    store.startPolling(true)
    const firstPoll = timers.fireNext()
    await Promise.resolve()
    store.setPageHidden(false)
    await timers.fireNext()
    assert.equal(activeCalls, 1)
    active.resolve({ data: { id: 11, status: 'PENDING' } })
    await firstPoll
    assert.equal(activeCalls, 1)
    assert.equal(timers.count(), 1)
  } finally {
    store.dispose()
    active.resolve({ data: null })
    timers.restore()
  }
})

test('dispose clears timer, aborts real tracked requests and advances generation', async () => {
  const timers = fakeTimers()
  let signal
  const request = deferred()
  const store = freshStore()
  try {
    const before = store.generation
    const pending = store.tracked(({ signal: trackedSignal }) => {
      signal = trackedSignal
      trackedSignal.addEventListener('abort', () => request.reject(new DOMException('aborted', 'AbortError')), { once: true })
      return request.promise
    }).catch(error => error)
    store.activeRound = { id: 1, status: 'RUNNING' }
    store.startPolling()
    store.dispose()
    assert.equal(signal.aborted, true)
    assert.equal(timers.count(), 0)
    assert.equal(store.generation, before + 1)
    assert.equal((await pending).name, 'AbortError')
  } finally {
    request.resolve(null)
    store.dispose()
    timers.restore()
  }
})

test('late World A initialization cannot write over World B', async () => {
  const worldA = deferred()
  const timelineA = deferred()
  const activeA = deferred()
  const api = defaultApi({
    getWorldById: id => id === 1 ? worldA.promise : Promise.resolve({ data: { id: 2, name: 'B' } }),
    getWorldTimeline: worldId => worldId === 1 ? timelineA.promise : Promise.resolve({ data: { items: [], hasMore: false } }),
    getActiveWorldRound: worldId => worldId === 1 ? activeA.promise : Promise.resolve({ data: null })
  })
  const store = freshStore(api)
  const initializingA = store.initialize(1, 7)
  const initializingB = store.initialize(2, 7)
  await initializingB
  worldA.resolve({ data: { id: 1, name: 'A' } })
  timelineA.resolve({ data: { items: [], hasMore: false } })
  activeA.resolve({ data: null })
  await initializingA
  assert.equal(store.worldId, 2)
  assert.equal(store.world?.name, 'B')
  assert.equal(store.phase, 'READY')
  store.dispose()
})

test('old finishRound crossing await cannot clear the new World pending state or timer', async () => {
  const timers = fakeTimers()
  const oldTimeline = deferred()
  const store = freshStore(defaultApi({ getWorldTimeline: () => oldTimeline.promise }))
  try {
    store.worldId = 1
    store.activeRound = { id: 101, status: 'RUNNING' }
    store.pendingSubmission = { requestId: 'old', userInput: 'old', roundId: 101 }
    const finishing = store.finishRound({ id: 101, status: 'COMPLETED' }, store.generation)
    await Promise.resolve()
    store.stopRuntime()
    store.worldId = 2
    store.activeRound = { id: 202, status: 'PENDING' }
    store.pendingSubmission = { requestId: 'new', userInput: 'new', roundId: 202 }
    store.startPolling()
    assert.equal(timers.count(), 1)
    oldTimeline.resolve({ data: { items: [] } })
    await finishing
    assert.equal(store.pendingSubmission?.requestId, 'new')
    assert.equal(store.activeRound?.id, 202)
    assert.equal(timers.count(), 1)
  } finally {
    oldTimeline.resolve({ data: { items: [] } })
    store.dispose()
    timers.restore()
  }
})

test('old finishRound in the same World cannot clear a newer Round pending state or timer', async () => {
  const timers = fakeTimers()
  const oldTimeline = deferred()
  const store = freshStore(defaultApi({ getWorldTimeline: () => oldTimeline.promise }))
  try {
    store.worldId = 1
    store.activeRound = { id: 101, status: 'RUNNING' }
    store.pendingSubmission = { requestId: 'old', userInput: 'old', roundId: 101 }
    const finishing = store.finishRound({ id: 101, status: 'COMPLETED' }, store.generation)
    await Promise.resolve()
    store.activeRound = { id: 102, status: 'PENDING' }
    store.pendingSubmission = { requestId: 'new', userInput: 'new', roundId: 102 }
    store.startPolling()
    oldTimeline.resolve({ data: { items: [] } })
    await finishing
    assert.equal(store.pendingSubmission?.requestId, 'new')
    assert.equal(store.activeRound?.id, 102)
    assert.equal(timers.count(), 1)
  } finally {
    oldTimeline.resolve({ data: { items: [] } })
    store.dispose()
    timers.restore()
  }
})

test('normal terminal completion clears timer and pending state and allows the next send', async () => {
  const timers = fakeTimers()
  let createCalls = 0
  const store = freshStore(defaultApi({
    createWorldRound: async () => {
      createCalls += 1
      return { data: { id: 202, status: 'PENDING' } }
    },
    executeWorldRound: async () => ({ data: { dispatchStatus: 'ACCEPTED' } })
  }))
  try {
    store.worldId = 1
    store.userId = 7
    store.activeRound = { id: 101, status: 'RUNNING' }
    store.pendingSubmission = { requestId: 'old', userInput: 'old', roundId: 101 }
    store.startPolling()
    await store.finishRound({ id: 101, status: 'COMPLETED' }, store.generation)
    assert.equal(timers.count(), 0)
    assert.equal(store.pendingSubmission, null)
    assert.equal(store.phase, 'COMPLETED')
    assert.equal(store.busy, false)
    assert.equal(await store.send('next round'), true)
    assert.equal(createCalls, 1)
    assert.equal(store.pendingSubmission?.roundId, 202)
    assert.equal(timers.count(), 1)
  } finally {
    store.dispose()
    timers.restore()
  }
})

test('active statuses continue, terminal statuses stop, and successful poll resets failure count', async () => {
  for (const status of ['PENDING', 'RUNNING']) {
    const timers = fakeTimers()
    const store = freshStore(defaultApi({ getActiveWorldRound: async () => ({ data: { id: 1, status } }), getWorldRound: async () => ({ data: { id: 1, status } }) }))
    try {
      store.worldId = 1
      store.activeRound = { id: 1, status }
      store.pollFailureCount = 3
      store.startPolling(true)
      await timers.fireNext()
      assert.equal(store.pollFailureCount, 0)
      assert.equal(timers.count(), 1)
    } finally { store.dispose(); timers.restore() }
  }
  for (const status of ['COMPLETED', 'PARTIAL_FAILED', 'FAILED']) {
    const timers = fakeTimers()
    const store = freshStore()
    try {
      store.activeRound = { id: 1, status }
      store.startPolling()
      assert.equal(timers.count(), 0)
    } finally { store.dispose(); timers.restore() }
  }
})

test('poll failures back off to 15 seconds and do not overwrite historyError', async () => {
  const timers = fakeTimers()
  let failures = 0
  const store = freshStore(defaultApi({
    getActiveWorldRound: async () => {
      if (failures++ < 5) throw new Error('offline')
      return { data: { id: 1, status: 'PENDING' } }
    }
  }))
  try {
    store.worldId = 1
    store.activeRound = { id: 1, status: 'PENDING' }
    store.historyError = 'history-only'
    store.startPolling(true)
    for (const expected of [2000, 4000, 8000, 15000, 15000]) {
      await timers.fireNext()
      assert.equal(timers.nextDelay(), expected)
    }
    await timers.fireNext()
    assert.equal(store.pollFailureCount, 0)
    assert.equal(store.historyError, 'history-only')
  } finally {
    store.dispose()
    timers.restore()
  }
})
