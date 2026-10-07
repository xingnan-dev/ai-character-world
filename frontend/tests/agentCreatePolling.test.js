import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  AGENT_HIDDEN_POLL_INTERVAL_MS,
  AGENT_MAX_POLL_BACKOFF_MS,
  AGENT_POLL_INTERVAL_MS,
  agentPollDelay,
  createAgentPollingController,
  isTerminalAgentStatus
} from '../src/utils/agentPollingController.js'
import { createAgentRequestId, validateAgentGoal } from '../src/utils/agentPresentation.js'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const storeSource = source('../src/stores/agent.js')
const pageSource = source('../src/views/AgentWorkspace.vue')
const pollingSource = source('../src/utils/agentPollingController.js')

const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

const fakeClock = () => {
  let sequence = 0
  const tasks = new Map()
  return {
    set(fn, delay) { const id = ++sequence; tasks.set(id, { fn, delay }); return id },
    clear(id) { tasks.delete(id) },
    delays() { return [...tasks.values()].map(task => task.delay) },
    size() { return tasks.size },
    async runNext() {
      const entry = tasks.entries().next().value
      assert.ok(entry, 'expected a scheduled poll')
      tasks.delete(entry[0]); entry[1].fn()
      await new Promise(resolve => setImmediate(resolve))
    }
  }
}

const visibilityFake = () => {
  const listeners = new Set()
  return {
    visibilityState: 'visible',
    addEventListener(type, fn) { if (type === 'visibilitychange') listeners.add(fn) },
    removeEventListener(type, fn) { if (type === 'visibilitychange') listeners.delete(fn) },
    emit() { for (const listener of listeners) listener() },
    count() { return listeners.size }
  }
}

const controller = ({ fetchRun, onRun = () => {}, onTerminal = () => {}, onWarning = () => {}, onState = () => {}, clock = fakeClock(), visibility = visibilityFake() }) => ({
  clock,
  visibility,
  instance: createAgentPollingController({ fetchRun, onRun, onTerminal, onWarning, onState, setTimer: clock.set, clearTimer: clock.clear, visibility })
})

test('Goal validation trims valid input and rejects empty or over 4000 characters', () => {
  assert.deepEqual(validateAgentGoal('  summarize character  '), { valid: true, goal: 'summarize character', error: '' })
  assert.equal(validateAgentGoal('   ').valid, false)
  assert.equal(validateAgentGoal('x'.repeat(4001)).valid, false)
})

test('new Goal request IDs use UUID capability and stay within backend length', () => {
  const id = createAgentRequestId({ randomUUID: () => '123e4567-e89b-12d3-a456-426614174000' })
  assert.equal(id, 'agent-123e4567-e89b-12d3-a456-426614174000')
  assert.ok(id.length <= 64)
})

test('store create sends requestId and Goal without developer controls', () => {
  assert.match(storeSource, /createAgentRun\(\{ requestId, goal: validation\.goal \}\)/)
  assert.doesNotMatch(pageSource, /temperature|model picker|Tool picker|system prompt/i)
})

test('successful create selects Run, refreshes list and conditionally starts polling', () => {
  assert.match(storeSource, /this\.selectedRunId = run\.id[\s\S]*this\.selectedRun = run/)
  assert.match(storeSource, /void this\.loadRuns\(\)\.catch/)
  assert.match(storeSource, /if \(!isTerminalAgentStatus\(run\.status\)\) this\.startPolling\(run\.id\)/)
  assert.match(pageSource, /runId:String\(run\.id\)/)
})

test('create retry retains Goal and reuses the pending requestId', () => {
  assert.match(storeSource, /const requestId = retry \? this\.pendingRequestId : createAgentRequestId\(\)/)
  assert.match(storeSource, /retryCreate\(\)[\s\S]*retry: true/)
  assert.match(pageSource, /重试会复用同一个 requestId/)
})

test('duplicate create submissions share one in-flight operation', () => {
  assert.match(storeSource, /const duplicate = createPromises\.get\(this\)[\s\S]*if \(duplicate\) return duplicate/)
  assert.match(pageSource, /:loading="agentStore\.createLoading"[\s\S]*:disabled="agentStore\.createLoading"/)
})

test('polling uses recursive timeout and never interval', () => {
  assert.match(pollingSource, /setTimer = setTimeout/)
  assert.match(pollingSource, /timer = setTimer\(\(\) => \{ timer = null; void poll\(token\) \}, delay\)/)
  assert.doesNotMatch(pollingSource, /setInterval/)
})

test('running poll applies progressive Run and schedules exactly one next poll', async () => {
  const seen = []
  const harness = controller({ fetchRun: async id => ({ id, status: 'RUNNING', currentStep: 2 }), onRun: run => seen.push(run) })
  assert.equal(harness.instance.start(3), true)
  assert.equal(harness.clock.size(), 1)
  await harness.clock.runNext()
  assert.equal(seen[0].currentStep, 2)
  assert.equal(harness.clock.size(), 1)
  assert.deepEqual(harness.clock.delays(), [AGENT_POLL_INTERVAL_MS])
})

test('starting the same Run twice keeps one polling loop', () => {
  const harness = controller({ fetchRun: async id => ({ id, status: 'RUNNING' }) })
  assert.equal(harness.instance.start(4), true)
  assert.equal(harness.instance.start(4), false)
  assert.equal(harness.clock.size(), 1)
})

test('COMPLETED and FAILED are terminal and stop polling immediately', async () => {
  for (const status of ['COMPLETED', 'FAILED']) {
    let terminal = ''
    const harness = controller({ fetchRun: async id => ({ id, status }), onTerminal: run => { terminal = run.status } })
    harness.instance.start(5)
    await harness.clock.runNext()
    assert.equal(terminal, status)
    assert.equal(harness.instance.snapshot().pollingActive, false)
    assert.equal(harness.clock.size(), 0)
    assert.equal(isTerminalAgentStatus(status), true)
  }
})

test('switching Run invalidates an old in-flight response', async () => {
  const old = deferred()
  const seen = []
  const harness = controller({ fetchRun: id => id === 1 ? old.promise : Promise.resolve({ id, status: 'RUNNING' }), onRun: run => seen.push(run.id) })
  harness.instance.start(1)
  await harness.clock.runNext()
  harness.instance.start(2)
  old.resolve({ id: 1, status: 'COMPLETED' })
  await new Promise(resolve => setImmediate(resolve))
  assert.deepEqual(seen, [])
  assert.equal(harness.instance.snapshot().pollingRunId, 2)
  await harness.clock.runNext()
  assert.deepEqual(seen, [2])
})

test('network failure preserves last Run and applies bounded exponential backoff', async () => {
  let calls = 0
  const seen = []
  const warnings = []
  const harness = controller({ fetchRun: async id => { calls += 1; if (calls < 4) throw new Error('offline'); return { id, status: 'RUNNING' } }, onRun: run => seen.push(run), onWarning: warning => warnings.push(warning) })
  harness.instance.start(8)
  await harness.clock.runNext()
  assert.deepEqual(seen, [])
  assert.match(warnings.at(-1), /连接暂时异常/)
  assert.deepEqual(harness.clock.delays(), [2400])
  await harness.clock.runNext()
  assert.deepEqual(harness.clock.delays(), [4800])
  await harness.clock.runNext()
  assert.deepEqual(harness.clock.delays(), [8000])
  assert.equal(agentPollDelay(99), AGENT_MAX_POLL_BACKOFF_MS)
})

test('successful poll resets failure count and normal interval', async () => {
  let calls = 0
  const harness = controller({ fetchRun: async id => { calls += 1; if (calls === 1) throw new Error('temporary'); return { id, status: 'RUNNING' } } })
  harness.instance.start(9)
  await harness.clock.runNext()
  assert.equal(harness.instance.snapshot().pollingFailureCount, 1)
  await harness.clock.runNext()
  assert.equal(harness.instance.snapshot().pollingFailureCount, 0)
  assert.deepEqual(harness.clock.delays(), [AGENT_POLL_INTERVAL_MS])
})

test('hidden tab slows polling and visible tab refreshes immediately', () => {
  const harness = controller({ fetchRun: async id => ({ id, status: 'RUNNING' }) })
  harness.instance.attachVisibility()
  harness.instance.attachVisibility()
  assert.equal(harness.visibility.count(), 1)
  harness.instance.start(10)
  harness.visibility.visibilityState = 'hidden'
  harness.visibility.emit()
  assert.deepEqual(harness.clock.delays(), [AGENT_HIDDEN_POLL_INTERVAL_MS])
  harness.visibility.visibilityState = 'visible'
  harness.visibility.emit()
  assert.deepEqual(harness.clock.delays(), [0])
})

test('dispose clears timers and visibility listener for unmount and route leave cleanup', () => {
  const harness = controller({ fetchRun: async id => ({ id, status: 'RUNNING' }) })
  harness.instance.attachVisibility()
  harness.instance.start(11)
  harness.instance.dispose()
  assert.equal(harness.clock.size(), 0)
  assert.equal(harness.visibility.count(), 0)
  assert.equal(harness.instance.snapshot().pollingActive, false)
  assert.match(pageSource, /onUnmounted\(\(\)=>agentStore\.disposePolling\(\)\)/)
  assert.match(pageSource, /onBeforeRouteLeave\(\(\)=>agentStore\.disposePolling\(\)\)/)
})

test('detail restore starts only non-terminal Run polling and terminal transition refreshes list', () => {
  assert.match(storeSource, /if \(!isTerminalAgentStatus\(run\.status\)\) this\.startPolling\(id\)/)
  assert.match(storeSource, /onTerminal: run => \{[\s\S]*void store\.loadRuns\(\)\.catch/)
  assert.match(pageSource, /watch\(\(\)=>route\.query\.runId/)
})

test('store persistence remains selectedRunId-only and scope excludes push transports', () => {
  assert.match(storeSource, /paths: \['selectedRunId'\]/)
  assert.doesNotMatch(storeSource + pageSource, /EventSource|WebSocket/)
  assert.doesNotMatch(pageSource, /Reasoning|Thought Process|思维链|AI 思考过程/)
})
