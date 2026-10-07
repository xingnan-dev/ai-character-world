import { defineStore } from 'pinia'
import { createAgentRun, getAgentRun, listAgentRuns, resumeAgentRun } from '../api/agent'
import { createAgentPollingController, isTerminalAgentStatus } from '../utils/agentPollingController'
import { canShowResume, createAgentRequestId, normalizeAgentRun, normalizeAgentRunPage, parseAgentRunId, validateAgentGoal } from '../utils/agentPresentation'

const pollingControllers = new WeakMap()
const createPromises = new WeakMap()
const resumePromises = new WeakMap()
const detailGenerations = new WeakMap()
const dataOf = response => response?.data ?? response
const messageOf = error => error?.response?.status === 404
  ? '找不到该 Agent Run，或你没有访问权限。'
  : error?.message || 'Agent 请求失败，请稍后重试。'

const detailGenerationKey = store => store?.$state || store
const detailGenerationOf = store => detailGenerations.get(detailGenerationKey(store))

const nextDetailGeneration = store => {
  const key = detailGenerationKey(store)
  const generation = (detailGenerations.get(key) || 0) + 1
  detailGenerations.set(key, generation)
  return generation
}

const syncRunSummary = (store, run) => {
  const index = store.runs.findIndex(item => item.id === run.id)
  if (index >= 0) store.runs[index] = { ...store.runs[index], ...run, steps: [] }
  else store.runs.unshift({ ...run, steps: [] })
}

const controllerFor = store => {
  if (!pollingControllers.has(store)) {
    pollingControllers.set(store, createAgentPollingController({
      fetchRun: async (runId, config) => normalizeAgentRun(dataOf(await getAgentRun(runId, config))),
      onRun: run => {
        if (store.selectedRunId !== run.id) return
        store.selectedRun = run
        store.pollingWarning = ''
        store.resumeError = ''
        syncRunSummary(store, run)
      },
      onState: state => {
        store.pollingRunId = state.pollingRunId
        store.pollingActive = state.pollingActive
        store.pollingFailureCount = state.pollingFailureCount
      },
      onWarning: warning => { store.pollingWarning = warning },
      onTerminal: run => {
        if (store.selectedRunId === run.id) store.selectedRun = run
        void store.loadRuns().catch(() => {})
      }
    }))
  }
  return pollingControllers.get(store)
}

export const useAgentStore = defineStore('agent', {
  state: () => ({
    runs: [],
    selectedRunId: null,
    selectedRun: null,
    listLoading: false,
    detailLoading: false,
    listError: '',
    detailError: '',
    createLoading: false,
    createError: '',
    resumeLoading: false,
    resumeError: '',
    pendingGoal: '',
    pendingRequestId: '',
    pollingRunId: null,
    pollingActive: false,
    pollingWarning: '',
    pollingFailureCount: 0
  }),
  actions: {
    async loadRuns() {
      this.listLoading = true
      this.listError = ''
      try {
        const page = normalizeAgentRunPage(await listAgentRuns({ page: 1, pageSize: 20 }))
        this.runs = page.items
        return page
      } catch (error) {
        this.listError = messageOf(error)
        throw error
      } finally {
        this.listLoading = false
      }
    },
    async createRun(goal, { retry = false } = {}) {
      const duplicate = createPromises.get(this)
      if (duplicate) return duplicate
      const candidate = retry ? this.pendingGoal : goal
      const validation = validateAgentGoal(candidate)
      if (!validation.valid) {
        this.createError = validation.error
        return null
      }
      if (retry && !this.pendingRequestId) {
        this.createError = '没有可重试的 Goal。'
        return null
      }
      const requestId = retry ? this.pendingRequestId : createAgentRequestId()
      this.pendingGoal = validation.goal
      this.pendingRequestId = requestId
      this.createLoading = true
      this.createError = ''
      const task = (async () => {
        try {
          const run = normalizeAgentRun(dataOf(await createAgentRun({ requestId, goal: validation.goal })))
          if (!run.id) throw new Error('Agent API 未返回有效 Run ID。')
          this.stopPolling()
          nextDetailGeneration(this)
          this.selectedRunId = run.id
          this.selectedRun = run
          this.detailError = ''
          syncRunSummary(this, run)
          this.pendingGoal = ''
          this.pendingRequestId = ''
          void this.loadRuns().catch(() => {})
          if (!isTerminalAgentStatus(run.status)) this.startPolling(run.id)
          return run
        } catch (error) {
          this.createError = messageOf(error)
          throw error
        } finally {
          this.createLoading = false
          createPromises.delete(this)
        }
      })()
      createPromises.set(this, task)
      return task
    },
    retryCreate() {
      return this.createRun(this.pendingGoal, { retry: true })
    },
    async resumeRun(runId = this.selectedRunId) {
      const id = parseAgentRunId(runId)
      const duplicate = resumePromises.get(this)
      if (duplicate?.runId === id) return duplicate.promise
      if (!id || this.selectedRunId !== id || this.selectedRun?.id !== id || !canShowResume(this.selectedRun)) return null
      const generation = nextDetailGeneration(this)
      this.stopPolling()
      this.resumeLoading = true
      this.resumeError = ''
      const promise = (async () => {
        try {
          const run = normalizeAgentRun(dataOf(await resumeAgentRun(id)))
          if (generation !== detailGenerationOf(this) || this.selectedRunId !== id || run.id !== id) return null
          this.selectedRun = run
          syncRunSummary(this, run)
          void this.loadRuns().catch(() => {})
          if (!isTerminalAgentStatus(run.status)) this.startPolling(id)
          return run
        } catch (error) {
          if (generation === detailGenerationOf(this) && this.selectedRunId === id) {
            this.resumeError = messageOf(error)
            if (!isTerminalAgentStatus(this.selectedRun?.status)) this.startPolling(id)
          }
          throw error
        } finally {
          if (generation === detailGenerationOf(this) && this.selectedRunId === id) this.resumeLoading = false
          if (resumePromises.get(this)?.promise === promise) resumePromises.delete(this)
        }
      })()
      resumePromises.set(this, { runId: id, promise })
      return promise
    },
    async selectRun(runId) {
      const id = parseAgentRunId(runId)
      this.stopPolling()
      const generation = nextDetailGeneration(this)
      if (!id) {
        this.selectedRunId = null
        this.detailError = '无效的 Agent Run ID。'
        return null
      }
      this.selectedRunId = id
      this.resumeError = ''
      this.resumeLoading = false
      return this.loadRun(id, generation)
    },
    async loadRun(runId = this.selectedRunId, generation = nextDetailGeneration(this)) {
      const id = parseAgentRunId(runId)
      if (!id) {
        this.detailError = '无效的 Agent Run ID。'
        return null
      }
      this.detailLoading = true
      this.detailError = ''
      try {
        const run = normalizeAgentRun(dataOf(await getAgentRun(id)))
        if (generation !== detailGenerationOf(this) || this.selectedRunId !== id) return null
        this.selectedRun = run
        syncRunSummary(this, run)
        if (!isTerminalAgentStatus(run.status)) this.startPolling(id)
        return run
      } catch (error) {
        if (generation === detailGenerationOf(this) && this.selectedRunId === id) this.detailError = messageOf(error)
        throw error
      } finally {
        if (generation === detailGenerationOf(this)) this.detailLoading = false
      }
    },
    startPolling(runId = this.selectedRunId) {
      const id = parseAgentRunId(runId)
      if (!id || this.selectedRunId !== id || isTerminalAgentStatus(this.selectedRun?.status)) return false
      return controllerFor(this).start(id)
    },
    stopPolling() {
      pollingControllers.get(this)?.stop()
      this.pollingRunId = null
      this.pollingActive = false
      this.pollingFailureCount = 0
      this.pollingWarning = ''
    },
    attachVisibility() {
      controllerFor(this).attachVisibility()
    },
    detachVisibility() {
      pollingControllers.get(this)?.detachVisibility()
    },
    disposePolling() {
      pollingControllers.get(this)?.dispose()
      pollingControllers.delete(this)
      this.pollingRunId = null
      this.pollingActive = false
      this.pollingFailureCount = 0
      this.pollingWarning = ''
    },
    clearSelection() {
      this.stopPolling()
      nextDetailGeneration(this)
      this.selectedRunId = null
      this.selectedRun = null
      this.detailError = ''
      this.detailLoading = false
      this.resumeError = ''
      this.resumeLoading = false
    }
  },
  persist: {
    key: 'agent-store',
    storage: localStorage,
    paths: ['selectedRunId']
  }
})
