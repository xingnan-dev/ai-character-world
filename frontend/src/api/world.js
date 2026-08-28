import request from './request'

export const parseWorld = (data) => request({ url: '/worlds/parse', method: 'post', data })
export const getWorldList = () => request({ url: '/worlds', method: 'get' })
export const getWorldById = (id) => request({ url: `/worlds/${id}`, method: 'get' })
export const createWorld = (data) => request({ url: '/worlds', method: 'post', data })
export const updateWorld = (id, data) => request({ url: `/worlds/${id}`, method: 'put', data })
export const deleteWorld = (id) => request({ url: `/worlds/${id}`, method: 'delete' })
export const replaceWorldParticipants = (id, data) => request({
  url: `/worlds/${id}/participants`, method: 'put', data
})
