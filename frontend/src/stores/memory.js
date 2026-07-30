import { defineStore } from 'pinia'
import { getMemoryList, deleteMemory, clearMemory } from '../api/memory'

export const useMemoryStore = defineStore('memory', {
  state: () => ({
    memories: [],
    loading: false,
    error: null
  }),
  getters: {
    learningMemories: (state) => state.memories.filter(m => m.category === 1),
    preferenceMemories: (state) => state.memories.filter(m => m.category === 2),
    achievementMemories: (state) => state.memories.filter(m => m.category === 3),
    recentMemories: (state) => [...state.memories].sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  },
  actions: {
    async fetchMemories() {
      this.loading = true
      this.error = null
      try {
        const res = await getMemoryList()
        const list = res.data || res || []
        if (list.length === 0) {
          this.loadMockData()
        } else {
          this.memories = list
        }
      } catch (err) {
        this.error = err.message || '获取记忆失败'
        this.loadMockData()
      } finally {
        this.loading = false
      }
    },
    loadMockData() {
      this.memories = [
        {
          id: 1,
          category: 1,
          categoryName: '学习',
          value: '你正在学习 Spring Boot 后端开发',
          importance: 0.8,
          createTime: new Date().toISOString()
        },
        {
          id: 2,
          category: 2,
          categoryName: '偏好',
          value: '你喜欢未来科技风格的设计',
          importance: 0.9,
          createTime: new Date().toISOString()
        },
        {
          id: 3,
          category: 3,
          categoryName: '成就',
          value: '完成了第一次AI角色创建',
          importance: 0.7,
          createTime: new Date().toISOString()
        }
      ]
    },
    async deleteMemoryAction(id) {
      try {
        await deleteMemory(id)
        this.memories = this.memories.filter(m => m.id !== id)
      } catch (err) {
        this.error = err.message || '删除记忆失败'
        throw err
      }
    },
    async clearAllMemories() {
      try {
        await clearMemory()
        this.memories = []
      } catch (err) {
        this.error = err.message || '清空记忆失败'
        throw err
      }
    },
    addMemory(memory) {
      this.memories.unshift({
        id: Date.now(),
        createTime: new Date().toISOString(),
        importance: 0.5,
        ...memory
      })
    },
    resetState() {
      this.memories = []
      this.loading = false
      this.error = null
    }
  }
})
