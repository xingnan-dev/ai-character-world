import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo, setCurrentUserCharacter } from '../api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: {
      id: null,
      username: '',
      nickname: '',
      avatarUrl: ''
      ,currentUserCharacterId: null
      ,currentUserCharacter: null
    }
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    nickname: (state) => state.userInfo.nickname || state.userInfo.username,
    avatarUrl: (state) => state.userInfo.avatarUrl
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info) {
      this.userInfo = { ...this.userInfo, ...info }
    },
    async login(payload) {
      const res = await loginApi(payload)
      const data = res.data || res
      this.setToken(data.token || data.accessToken || '')
      if (data.user) {
        this.setUserInfo(data.user)
      }
      return data
    },
    async getUserInfoAction() {
      const res = await getUserInfo()
      const data = res.data || res
      this.setUserInfo(data)
      return data
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // ignore
      }
      this.resetState()
    },
    async selectCurrentUserCharacter(characterId) {
      const res = await setCurrentUserCharacter(characterId)
      const data = res.data || res
      this.setUserInfo(data)
      return data
    },
    resetState() {
      this.token = ''
      this.userInfo = { id: null, username: '', nickname: '', avatarUrl: '', currentUserCharacterId: null, currentUserCharacter: null }
      localStorage.removeItem('token')
    }
  },
  persist: {
    key: 'user-store',
    storage: localStorage,
    paths: ['token', 'userInfo']
  }
})
