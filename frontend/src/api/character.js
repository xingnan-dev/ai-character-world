import request from './request'

export const parseCharacter = (data) => request({
  url: '/characters/parse',
  method: 'post',
  data
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
