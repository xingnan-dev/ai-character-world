import { defineStore } from 'pinia'
import {
  createWorld as createWorldApi,
  deleteWorld,
  getWorldById,
  getWorldList,
  parseWorld as parseWorldApi,
  replaceWorldParticipants,
  updateWorld as updateWorldApi
} from '../api/world'

const dataOf = (response) => response?.data ?? response
const messageOf = (error) => error?.message || 'World请求失败'

export const useWorldStore = defineStore('world', {
  state: () => ({
    list: [],
    currentWorld: null,
    draft: null,
    loading: false,
    error: ''
  }),
  actions: {
    clearTransient({ keepDraft = false } = {}) {
      this.error = ''
      this.currentWorld = null
      if (!keepDraft) this.draft = null
    },
    async run(action) {
      this.loading = true
      this.error = ''
      try {
        return await action()
      } catch (error) {
        this.error = messageOf(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async parse(description) {
      this.draft = null
      return this.run(async () => {
        const draft = dataOf(await parseWorldApi({ description }))
        this.draft = draft
        return draft
      })
    },
    async loadList() {
      this.currentWorld = null
      return this.run(async () => {
        this.list = dataOf(await getWorldList()) || []
        return this.list
      })
    },
    async loadDetail(id) {
      this.currentWorld = null
      return this.run(async () => {
        this.currentWorld = dataOf(await getWorldById(id))
        return this.currentWorld
      })
    },
    async create(payload) {
      return this.run(async () => {
        const world = dataOf(await createWorldApi(payload))
        this.currentWorld = world
        this.draft = null
        return world
      })
    },
    async update(id, payload) {
      return this.run(async () => {
        this.currentWorld = dataOf(await updateWorldApi(id, payload))
        return this.currentWorld
      })
    },
    async remove(id) {
      return this.run(async () => {
        await deleteWorld(id)
        this.list = this.list.filter(world => world.id !== id)
        if (this.currentWorld?.id === id) this.currentWorld = null
      })
    },
    async replaceParticipants(id, participants) {
      return this.run(async () => {
        this.currentWorld = dataOf(await replaceWorldParticipants(id, { participants }))
        return this.currentWorld
      })
    }
  }
})
