import { get, post, put, del } from './request'

export const login = (data) => post('/auth/login', data)
export const register = (data) => post('/auth/register', data)
export const logout = () => post('/auth/logout')
export const getUserInfo = () => get('/user/info')
export const updateUserInfo = (data) => put('/user/update', data)
export const setCurrentUserCharacter = (characterId) => put(`/user/current-character/${characterId}`)
export const clearCurrentUserCharacter = () => del('/user/current-character')
