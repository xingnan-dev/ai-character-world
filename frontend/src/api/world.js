import request from './request'
import { AI_PARSE_TIMEOUT_MS } from '../utils/aiDraftResponse'

export const parseWorld = (data) => request({ url: '/worlds/parse', method: 'post', data, timeout: AI_PARSE_TIMEOUT_MS })
export const getWorldList = () => request({ url: '/worlds', method: 'get' })
export const getWorldById = (id, config = {}) => request({ url: `/worlds/${id}`, method: 'get', ...config })
export const createWorld = (data) => request({ url: '/worlds', method: 'post', data })
export const updateWorld = (id, data) => request({ url: `/worlds/${id}`, method: 'put', data })
export const deleteWorld = (id) => request({ url: `/worlds/${id}`, method: 'delete' })
export const replaceWorldParticipants = (id, data) => request({
  url: `/worlds/${id}/participants`, method: 'put', data
})
export const setWorldUserCharacter = (id, characterId) => request({
  url: `/worlds/${id}/user-character/${characterId}`, method: 'put'
})
