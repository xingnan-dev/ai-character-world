import { defineStore } from 'pinia'
import { getWorldById } from '../api/world'
import {
  createWorldRound,
  executeWorldRound,
  getActiveWorldRound,
  getWorldRound,
  getWorldRoundEvents,
  getWorldTimeline
} from '../api/worldInteraction'
import {
  clearPendingInteraction,
  createWorldRequestId,
  currentParticipantId,
  dataOf,
  errorCodeOf,
  interactionStorageKey,
  isActiveRound,
  isTerminalRound,
  mergeTimeline,
  normalizeWorldInput,
  pollDelay,
  readPendingInteraction,
  worldInteractionErrorMessage,
  writePendingInteraction
} from '../utils/worldInteraction'

const runtimes = new WeakMap()

function runtime(store) {
  if (!runtimes.has(store)) {
    runtimes.set(store, {
      timer: null,
      controllers: new Set(),
      dispatchingRoundIds: new Set(),
      polling: false,
      hidden: false,
      startedAt: 0
    })
  }
  return runtimes.get(store)
}

function sessionStore() {
  try {
    return globalThis.sessionStorage
  } catch {
    return null
  }
}

export const useWorldInteractionStore = defineStore('worldInteraction', {
  state: () => ({
    worldId: null,
    userId: null,
    world: null,
    timelineItems: [],
    historyCursor: null,
    hasMore: false,
    historyError: '',
    activeRound: null,
    activeEvents: [],
    pendingSubmission: null,
    draftInput: '',
    phase: 'IDLE',
    loadingInitial: false,
    loadingMore: false,
    sending: false,
    polling: false,
    queueBusy: false,
    pollFailureCount: 0,
    pollCount: 0,
    notice: '',
    error: '',
    generation: 0
  }),
  getters: {
    busy: state => state.sending || state.polling || isActiveRound(state.activeRound?.status),
    orderedParticipants: state => [...(state.world?.participants || [])]
      .sort((a, b) => Number(a.displayOrder || 0) - Number(b.displayOrder || 0)),
    speakingParticipantId() {
      return currentParticipantId(this.orderedParticipants, this.activeEvents, this.activeRound?.status)
    },
    canRecover: state => Boolean(state.pendingSubmission) && (
      state.phase === 'CREATE_UNCERTAIN' || state.phase === 'QUEUE_BUSY'
      || state.phase === 'DISPATCH_UNCERTAIN' || state.activeRound?.executionRecoverable
    )
  },
  actions: {
    storageKey() {
      return interactionStorageKey(this.userId, this.worldId)
    },
    persistPending(patch = {}) {
      if (!this.pendingSubmission) return
      this.pendingSubmission = { ...this.pendingSubmission, ...patch }
      writePendingInteraction(sessionStore(), this.storageKey(), this.pendingSubmission)
    },
    clearPending() {
      clearPendingInteraction(sessionStore(), this.storageKey())
      this.pendingSubmission = null
    },
    async tracked(factory, generation = this.generation) {
      const controller = new AbortController()
      const run = runtime(this)
      run.controllers.add(controller)
      try {
        const result = await factory({ signal: controller.signal, skipGlobalError: true })
        if (generation !== this.generation) throw new DOMException('Stale interaction request', 'AbortError')
        return result
      } finally {
        run.controllers.delete(controller)
      }
    },
    stopRuntime() {
      const run = runtime(this)
      if (run.timer) clearTimeout(run.timer)
      run.timer = null
      for (const controller of run.controllers) controller.abort()
      run.controllers.clear()
      run.dispatchingRoundIds.clear()
      run.polling = false
      run.startedAt = 0
      this.polling = false
      this.generation += 1
    },
    dispose() {
      this.stopRuntime()
      this.worldId = null
      this.world = null
      this.timelineItems = []
      this.activeRound = null
      this.activeEvents = []
      this.historyError = ''
      this.phase = 'IDLE'
      this.notice = ''
      this.error = ''
    },
    async initialize(worldId, userId) {
      this.stopRuntime()
      this.worldId = Number(worldId)
      this.userId = userId
      this.world = null
      this.timelineItems = []
      this.historyCursor = null
      this.hasMore = false
      this.historyError = ''
      this.activeRound = null
      this.activeEvents = []
      this.pendingSubmission = readPendingInteraction(sessionStore(), this.storageKey())
      this.phase = 'BOOTSTRAPPING'
      this.loadingInitial = true
      this.error = ''
      this.notice = ''
      this.queueBusy = false
      this.pollFailureCount = 0
      this.pollCount = 0
      const generation = this.generation
      try {
        const [worldResponse, timelineResponse, activeResponse] = await Promise.all([
          this.tracked(config => getWorldById(this.worldId, config), generation),
          this.tracked(config => getWorldTimeline(this.worldId, { limit: 20 }, config), generation),
          this.tracked(config => getActiveWorldRound(this.worldId, config), generation)
        ])
        this.world = dataOf(worldResponse)
        this.applyTimelinePage(dataOf(timelineResponse), false)
        const active = dataOf(activeResponse)
        if (active) await this.adoptActiveRound(active, generation)
        else await this.resumeSavedSubmission(generation)
        if (!this.activeRound && !this.pendingSubmission) this.phase = 'READY'
      } catch (error) {
        if (error?.name !== 'AbortError') {
          this.error = worldInteractionErrorMessage(error, '互动页面加载失败，请稍后重试')
          this.phase = 'FATAL'
        }
      } finally {
        if (generation === this.generation) this.loadingInitial = false
      }
    },
    applyTimelinePage(page, historyPage) {
      this.timelineItems = mergeTimeline(this.timelineItems, page?.items || [])
      if (historyPage || this.historyCursor == null) {
        this.historyCursor = page?.nextBeforeRoundId ?? null
        this.hasMore = Boolean(page?.hasMore)
      }
    },
    mergeActive(round, events = this.activeEvents) {
      this.activeRound = round
      this.activeEvents = events
      this.timelineItems = mergeTimeline(this.timelineItems, [{ round, events }])
    },
    async loadOlder() {
      if (this.loadingMore || !this.hasMore || !this.historyCursor) return
      this.loadingMore = true
      this.historyError = ''
      try {
        const response = await this.tracked(config => getWorldTimeline(
          this.worldId, { beforeRoundId: this.historyCursor, limit: 20 }, config
        ))
        this.applyTimelinePage(dataOf(response), true)
      } catch (error) {
        if (error?.name !== 'AbortError') this.historyError = worldInteractionErrorMessage(error, '更早的互动记录加载失败')
      } finally {
        this.loadingMore = false
      }
    },
    async refreshTimelineHead(generation = this.generation) {
      const response = await this.tracked(
        config => getWorldTimeline(this.worldId, { limit: 20 }, config), generation
      )
      this.timelineItems = mergeTimeline(this.timelineItems, dataOf(response)?.items || [])
    },
    async adoptActiveRound(round, generation = this.generation) {
      this.activeRound = round
      this.phase = round.status
      this.queueBusy = false
      if (!this.pendingSubmission || String(this.pendingSubmission.roundId) !== String(round.id)) {
        this.pendingSubmission = {
          requestId: round.requestId,
          userInput: round.userInput,
          roundId: round.id,
          phase: round.status,
          createdAt: Date.now()
        }
        this.persistPending()
      }
      const synced = await this.syncKnownRound(round.id, generation)
      if (isTerminalRound(synced.status)) return
      if (synced.executionRecoverable) await this.dispatchKnownRound(generation)
      else this.startPolling()
    },
    async resumeSavedSubmission(generation = this.generation) {
      if (!this.pendingSubmission) return
      if (this.pendingSubmission.roundId) {
        try {
          const response = await this.tracked(config => getWorldRound(
            this.worldId, this.pendingSubmission.roundId, config
          ), generation)
          const round = dataOf(response)
          if (isTerminalRound(round.status)) {
            await this.finishRound(round, generation)
          } else {
            await this.adoptActiveRound(round, generation)
          }
        } catch (error) {
          if (error?.name !== 'AbortError') {
            this.notice = worldInteractionErrorMessage(error, '未能恢复上次互动，请重新加载页面')
          }
        }
        return
      }
      await this.createPendingRound(generation)
    },
    async send(userInput) {
      const normalized = normalizeWorldInput(userInput)
      if (!normalized || normalized.length > 4000 || this.busy || this.pendingSubmission) return false
      this.sending = true
      this.error = ''
      this.notice = ''
      this.queueBusy = false
      this.pendingSubmission = {
        requestId: createWorldRequestId(),
        userInput: normalized,
        roundId: null,
        phase: 'CREATING',
        createdAt: Date.now()
      }
      this.persistPending()
      try {
        await this.createPendingRound(this.generation)
        return true
      } finally {
        this.sending = false
      }
    },
    async createPendingRound(generation = this.generation) {
      if (!this.pendingSubmission) return
      this.phase = 'CREATING'
      try {
        const response = await this.tracked(config => createWorldRound(this.worldId, {
          requestId: this.pendingSubmission.requestId,
          userInput: this.pendingSubmission.userInput
        }, config), generation)
        const round = dataOf(response)
        this.persistPending({ roundId: round.id, phase: 'CREATED' })
        this.draftInput = ''
        this.mergeActive(round, [])
        await this.dispatchKnownRound(generation)
      } catch (error) {
        if (error?.name === 'AbortError') return
        if (errorCodeOf(error) === 'WORLD_ROUND_ACTIVE') {
          this.notice = worldInteractionErrorMessage('WORLD_ROUND_ACTIVE')
          const activeResponse = await this.tracked(
            config => getActiveWorldRound(this.worldId, config), generation
          )
          const active = dataOf(activeResponse)
          if (active) await this.adoptActiveRound(active, generation)
          return
        }
        this.phase = 'CREATE_UNCERTAIN'
        this.notice = worldInteractionErrorMessage(error, '本轮创建结果暂不确定，将使用同一请求标识恢复')
      }
    },
    async dispatchKnownRound(generation = this.generation) {
      const round = this.activeRound
      if (!round || isTerminalRound(round.status)) return
      const run = runtime(this)
      if (run.dispatchingRoundIds.has(round.id)) return
      run.dispatchingRoundIds.add(round.id)
      this.phase = 'DISPATCHING'
      this.persistPending({ roundId: round.id, phase: 'DISPATCHING' })
      try {
        const response = await this.tracked(
          config => executeWorldRound(this.worldId, round.id, config), generation
        )
        const dispatch = dataOf(response)
        this.queueBusy = false
        this.notice = dispatch?.dispatchStatus === 'ALREADY_ACCEPTED' ? '本轮已在执行，正在恢复进度' : ''
        this.phase = dispatch?.dispatchStatus === 'TERMINAL' ? 'SYNCING' : 'POLLING'
      } catch (error) {
        if (error?.name === 'AbortError') return
        if (errorCodeOf(error) === 'WORLD_EXECUTION_BUSY') {
          this.queueBusy = true
          this.phase = 'QUEUE_BUSY'
          this.notice = worldInteractionErrorMessage('WORLD_EXECUTION_BUSY')
        } else {
          this.phase = 'DISPATCH_UNCERTAIN'
          this.notice = worldInteractionErrorMessage(error, '执行请求结果暂不确定，正在查询本轮状态')
        }
      } finally {
        run.dispatchingRoundIds.delete(round.id)
        if (generation === this.generation) this.startPolling()
      }
    },
    async recoverExecution() {
      if (!this.activeRound && this.pendingSubmission && !this.pendingSubmission.roundId) {
        await this.createPendingRound(this.generation)
        return
      }
      if (!this.activeRound || isTerminalRound(this.activeRound.status)) return
      if (this.activeRound.status === 'RUNNING' && !this.activeRound.executionRecoverable) {
        this.notice = '本轮仍在执行，请稍候'
        this.startPolling(true)
        return
      }
      this.queueBusy = false
      await this.dispatchKnownRound(this.generation)
    },
    async syncKnownRound(roundId, generation = this.generation) {
      const [roundResponse, eventsResponse] = await Promise.all([
        this.tracked(config => getWorldRound(this.worldId, roundId, config), generation),
        this.tracked(config => getWorldRoundEvents(this.worldId, roundId, config), generation)
      ])
      const round = dataOf(roundResponse)
      const events = dataOf(eventsResponse) || []
      this.mergeActive(round, events)
      if (isTerminalRound(round.status)) await this.finishRound(round, generation)
      return round
    },
    async finishRound(round, generation = this.generation) {
      this.mergeActive(round, this.activeEvents)
      try {
        await this.refreshTimelineHead(generation)
      } catch (error) {
        if (error?.name !== 'AbortError') this.notice = '本轮已结束，历史记录将在下次刷新时同步'
      }
      this.stopPollingOnly()
      this.phase = round.status
      this.clearPending()
      this.queueBusy = false
      this.notice = round.status === 'COMPLETED'
        ? ''
        : worldInteractionErrorMessage(round.errorCode, '本轮互动未能全部完成')
    },
    stopPollingOnly() {
      const run = runtime(this)
      if (run.timer) clearTimeout(run.timer)
      run.timer = null
      run.polling = false
      run.startedAt = 0
      this.polling = false
    },
    startPolling(immediate = false) {
      if (!this.activeRound || isTerminalRound(this.activeRound.status)) return
      const run = runtime(this)
      if (!run.startedAt) run.startedAt = Date.now()
      this.polling = true
      this.schedulePoll(immediate ? 0 : undefined)
    },
    schedulePoll(delay) {
      const run = runtime(this)
      if (run.timer) clearTimeout(run.timer)
      const wait = delay ?? pollDelay(Date.now() - run.startedAt, this.pollFailureCount, run.hidden)
      run.timer = setTimeout(() => this.pollOnce(), wait)
    },
    async pollOnce() {
      const run = runtime(this)
      if (run.polling || !this.activeRound || isTerminalRound(this.activeRound.status)) return
      run.polling = true
      const generation = this.generation
      try {
        const activeResponse = await this.tracked(
          config => getActiveWorldRound(this.worldId, config), generation
        )
        const active = dataOf(activeResponse)
        const knownId = active?.id || this.activeRound?.id || this.pendingSubmission?.roundId
        if (active) this.activeRound = active
        if (knownId) await this.syncKnownRound(knownId, generation)
        this.pollFailureCount = 0
        this.pollCount += 1
        if (this.pollCount % 4 === 0 && !isTerminalRound(this.activeRound?.status)) {
          await this.refreshTimelineHead(generation)
        }
        if (!active && this.activeRound && !isTerminalRound(this.activeRound.status)) {
          this.notice = this.activeRound.executionRecoverable
            ? '本轮执行已暂停，可以恢复执行'
            : this.notice
        }
      } catch (error) {
        if (error?.name !== 'AbortError') {
          this.pollFailureCount += 1
          if (this.pollFailureCount >= 2) this.notice = '网络不稳定，正在自动恢复互动进度'
        }
      } finally {
        run.polling = false
        if (generation === this.generation && this.activeRound && !isTerminalRound(this.activeRound.status)) {
          this.schedulePoll()
        }
      }
    },
    setPageHidden(hidden) {
      const run = runtime(this)
      run.hidden = Boolean(hidden)
      if (!this.activeRound || isTerminalRound(this.activeRound.status)) return
      if (hidden) this.schedulePoll(15000)
      else this.schedulePoll(0)
    }
  }
})
