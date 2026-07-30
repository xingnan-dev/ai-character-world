import { get, post, put } from './request'

export const login = (data) => post('/auth/login', data)
export const register = (data) => post('/auth/register', data)
export const logout = () => post('/auth/logout')
export const getUserInfo = () => get('/user/info')
export const updateUserInfo = (data) => put('/user/update', data)
