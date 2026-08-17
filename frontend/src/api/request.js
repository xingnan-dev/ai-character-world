import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined && res.code !== 200) {
      const message = res.msg || res.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.msg || error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

const get = (url, params, config = {}) => service.get(url, { params, ...config })
const post = (url, data, config = {}) => service.post(url, data, config)
const put = (url, data, config = {}) => service.put(url, data, config)
const del = (url, params, config = {}) => service.delete(url, { params, ...config })

export { get, post, put, del }
export default service
