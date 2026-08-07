import request from './request'

export const getPersonalityByAvatarId = (avatarId) => {
  return request({
    url: `/personality/avatar/${avatarId}`,
    method: 'get'
  })
}

export const updatePersonality = (data) => {
  return request({
    url: '/personality/update',
    method: 'put',
    data
  })
}
