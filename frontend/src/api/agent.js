import request from './request'

export const createAgentRun = payload => request({ url: '/agent/runs', method: 'post', data: payload })
export const getAgentRun = (runId, config = {}) => request({ url: `/agent/runs/${runId}`, method: 'get', skipGlobalError: true, ...config })
export const listAgentRuns = params => request({ url: '/agent/runs', method: 'get', params, skipGlobalError: true })
export const resumeAgentRun = runId => request({ url: `/agent/runs/${runId}/resume`, method: 'post' })
