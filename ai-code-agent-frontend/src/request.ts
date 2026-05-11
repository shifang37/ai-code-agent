import axios from 'axios'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api',
  timeout: 60_000,
  withCredentials: true,
})

request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data?.code === 40100) {
      const url = response.request?.responseURL as string | undefined
      if (
        url &&
        !url.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`
      }
    }
    return response
  },
  (error) => Promise.reject(error),
)

export default request
