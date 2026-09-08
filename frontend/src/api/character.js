import request from './request'
import { AI_PARSE_TIMEOUT_MS } from '../utils/aiDraftResponse'

export const parseCharacter = (data) => request({
  url: '/characters/parse',
  method: 'post',
  data,
  timeout: AI_PARSE_TIMEOUT_MS
})

export const createCharacter = (data) => request({
  url: '/characters',
  method: 'post',
  data
})

export const getCharacterList = (type) => request({
  url: '/characters',
  method: 'get',
  params: type ? { type } : undefined
})

export const getCharacterById = (id) => request({
  url: `/characters/${id}`,
  method: 'get'
})

export const updateCharacter = (id, data) => request({
  url: `/characters/${id}`,
  method: 'put',
  data
})

export const deleteCharacter = (id) => request({
  url: `/characters/${id}`,
  method: 'delete'
})

export const generateCharacterImage = (data) => request({ method: 'post', url: '/character-images/generations', data, skipGlobalError: true })
export const confirmCharacterImage = (data) => request({ method: 'post', url: '/character-images/confirm', data })
