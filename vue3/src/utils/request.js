import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    const requestUrl = error.config?.url || ''
    const isLoginRequest = requestUrl.includes('/auth/login')

    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        if (!isLoginRequest) {
          ElMessage.error('登录已过期，请重新登录')
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('userInfo')
          router.push('/login')
        }
      } else if (status === 403) {
        if (!isLoginRequest) {
          ElMessage.error('权限不足')
        }
      } else if (status >= 500) {
        ElMessage.error('服务器错误')
      }
      // 400/404/409 等客户端错误不弹全局提示，由调用方自行处理
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
