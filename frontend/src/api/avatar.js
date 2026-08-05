import request from './request'

export const generateAvatar = (data) => {
  return request({
    url: '/avatar/generate',
    method: 'post',
    data
  })
}

export const createAvatar = (data) => {
  return request({
    url: '/avatar/create',
    method: 'post',
    data
  })
}

export const getAvatarList = () => {
  return request({
    url: '/avatar/list',
    method: 'get'
  })
}

export const getAvatarById = (id) => {
  return request({
    url: `/avatar/${id}`,
    method: 'get'
  })
}

export const updateAvatar = (data) => {
  return request({
    url: '/avatar/update',
    method: 'put',
    data
  })
}

export const deleteAvatar = (id) => {
  return request({
    url: `/avatar/${id}`,
    method: 'delete'
  })
}
