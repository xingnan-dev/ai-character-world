import { defineStore } from 'pinia'
import { getAvatarList, createAvatar, deleteAvatar } from '../api/avatar'

export const useAvatarStore = defineStore('avatar', {
  state: () => ({
    avatarList: [],
    currentAvatarIndex: 0,
    loading: false,
    error: null
  }),
  getters: {
    currentAvatar: (state) => state.avatarList[state.currentAvatarIndex] || null,
    currentSlogan: (state) => state.avatarList[state.currentAvatarIndex]?.slogan || '',
    avatarCount: (state) => state.avatarList.length
  },
  actions: {
    async fetchAvatarList() {
      this.loading = true
      this.error = null
      try {
        const res = await getAvatarList()
        const list = res.data || res || []
        if (list.length === 0) {
          this.loadMockData()
        } else {
          this.avatarList = list
        }
        if (this.currentAvatarIndex >= this.avatarList.length) {
          this.currentAvatarIndex = 0
        }
      } catch (err) {
        this.loadMockData()
      } finally {
        this.loading = false
      }
    },
    loadMockData() {
      this.avatarList = [
        {
          id: 1,
          name: 'Luna',
          type: 1,
          gender: 2,
          baseModel: 'sky',
          modelUrl: '/models/avatars/sky.vrm',
          slogan: '所有没有你的日子，都存在缺陷',
          personalityId: 1,
          status: 1
        },
        {
          id: 2,
          name: 'Nova',
          type: 1,
          gender: 2,
          baseModel: 'nova',
          modelUrl: '/models/avatars/nova.vrm',
          slogan: '宇宙无垠，我们的旅程才刚刚开始',
          personalityId: 4,
          status: 1
        }
      ]
    },
    async createAvatarAction(data) {
      this.loading = true
      try {
        const res = await createAvatar(data)
        const newAvatar = res.data || res
        this.avatarList.push(newAvatar)
        return newAvatar
      } catch (err) {
        this.error = err.message || '创建形象失败'
        throw err
      } finally {
        this.loading = false
      }
    },
    async deleteAvatarAction(id) {
      this.loading = true
      try {
        await deleteAvatar(id)
        this.avatarList = this.avatarList.filter(a => a.id !== id)
        if (this.currentAvatarIndex >= this.avatarList.length) {
          this.currentAvatarIndex = Math.max(0, this.avatarList.length - 1)
        }
      } catch (err) {
        this.error = err.message || '删除形象失败'
        throw err
      } finally {
        this.loading = false
      }
    },
    setCurrentAvatar(index) {
      if (index >= 0 && index < this.avatarList.length) {
        this.currentAvatarIndex = index
      }
    },
    switchAvatar(avatar) {
      const index = this.avatarList.findIndex(a => a.id === avatar.id)
      if (index !== -1) {
        this.currentAvatarIndex = index
        return true
      }
      return false
    },
    resetState() {
      this.avatarList = []
      this.currentAvatarIndex = 0
      this.loading = false
      this.error = null
    }
  }
})
