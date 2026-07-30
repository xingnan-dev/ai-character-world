import request from './request'

export const getMemoryList = () => {
  return request({
    url: '/memory/list',
    method: 'get'
  })
}

export const updateMemory = (data) => {
  return request({
    url: '/memory/update',
    method: 'put',
    data
  })
}

export const deleteMemory = (id) => {
  return request({
    url: `/memory/${id}`,
    method: 'delete'
  })
}

export const clearMemory = () => {
  return request({
    url: '/memory/clear',
    method: 'delete'
  })
}
