import axios from 'axios'

const API_BASE_URL = '/urbanova/api/v1'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一处理错误
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ==================== Auth API (ID 1) ====================

export const authApi = {
  // 注册
  register(data) {
    return api.post('/auth/register', data)
  },

  // 登录
  login(data) {
    return api.post('/auth/login', data)
  },

  // 获取当前用户信息
  getCurrentUser() {
    return api.get('/users/me')
  }
}

// ==================== Hire Options API (ID 4) ====================

export const hireOptionsApi = {
  // 获取所有租赁选项
  list() {
    return api.get('/hire-options')
  },

  // 获取价格报价
  quotePrice(data) {
    return api.post('/pricing/quotes', data)
  }
}

// ==================== Booking API (ID 5 & ID 12) ====================

export const bookingApi = {
  // 创建预订
  create(data) {
    return api.post('/bookings', data)
  },

  // 取消预订
  cancel(bookingId, data = {}) {
    return api.post(`/bookings/${bookingId}/cancel`, data)
  }
}

export default api
