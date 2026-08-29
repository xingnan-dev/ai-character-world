import request from './request'

export const createWorldRound = (worldId, data, config = {}) => request({
  url: `/worlds/${worldId}/rounds`, method: 'post', data, ...config
})

export const getActiveWorldRound = (worldId, config = {}) => request({
  url: `/worlds/${worldId}/rounds/active`, method: 'get', ...config
})

export const getWorldRound = (worldId, roundId, config = {}) => request({
  url: `/worlds/${worldId}/rounds/${roundId}`, method: 'get', ...config
})

export const executeWorldRound = (worldId, roundId, config = {}) => request({
  url: `/worlds/${worldId}/rounds/${roundId}/execute`, method: 'post', ...config
})

export const getWorldRoundEvents = (worldId, roundId, config = {}) => request({
  url: `/worlds/${worldId}/rounds/${roundId}/events`, method: 'get', ...config
})

export const getWorldTimeline = (worldId, params = {}, config = {}) => request({
  url: `/worlds/${worldId}/timeline`, method: 'get', params, ...config
})
