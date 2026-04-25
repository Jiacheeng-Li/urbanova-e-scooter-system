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
    const currentPath = window.location.pathname
    if (error.response?.status === 401) {
      const isLoginPage = currentPath === '/login'
      if (isLoginPage) {
      } else {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('user')
        window.location.href = '/login?error=unknown'
      }
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

// ==================== Scooter API ====================

export const scooterApi = {
  // 根据状态获取滑板车ID列表
  getScooterIdsByStatus(status) {
    return api.get('/scooters/ids', { params: { status } })
  },

  // 获取所有滑板车（用户端）
  list() {
    return api.get('/scooters')
  },

  // 获取滑板车详情
  getDetail(scooterId) {
    return api.get(`/scooters/${scooterId}`)
  }
}

// ==================== Booking API (ID 5 & ID 12) ====================

export const bookingApi = {
  // 创建预订
  create(data) {
    return api.post('/bookings', data)
  },

  // 获取预订列表
  list(status) {
    const params = status ? { status } : {}
    return api.get('/bookings', { params })
  },

  // 获取预订详情
  getDetail(bookingId) {
    return api.get(`/bookings/${bookingId}`)
  },

  // 更新预订
  update(bookingId, data) {
    return api.patch(`/bookings/${bookingId}`, data)
  },

  // 取消预订
  cancel(bookingId, data = {}) {
    return api.post(`/bookings/${bookingId}/cancel`, data)
  }
}

// ==================== Admin Hire Options API (ID 16) ====================

export const adminHireOptionsApi = {
  // 获取所有租赁选项（管理端）
  list() {
    return api.get('/admin/hire-options')
  },

  // 创建租赁选项
  create(data) {
    return api.post('/admin/hire-options', data)
  },

  // 更新租赁选项
  update(hireOptionId, data) {
    return api.patch(`/admin/hire-options/${hireOptionId}`, data)
  },

  // 禁用租赁选项
  disable(hireOptionId) {
    return api.delete(`/admin/hire-options/${hireOptionId}`)
  },

  // 获取单个租赁选项（如果需要）
  get(hireOptionId) {
    return api.get(`/admin/hire-options/${hireOptionId}`)
  }
}

// ==================== Admin Scooters API ====================

export const adminScootersApi = {
  // 获取所有滑板车（管理端）
  list(params) {
    return api.get('/admin/scooters', { params })
  },

  // 添加滑板车
  create(data) {
    return api.post('/admin/scooters', data)
  },

  // 更新滑板车信息（电量、位置、区域）
  update(scooterId, data) {
    return api.patch(`/admin/scooters/${scooterId}`, data)
  },

  // 更新滑板车状态
  updateStatus(scooterId, data) {
    return api.patch(`/admin/scooters/${scooterId}/status`, data)
  },

  // 批量更新滑板车状态
  bulkUpdateStatus(data) {
    return api.post('/admin/scooters/bulk-status', data)
  }
}

// ==================== Admin Analytics API ====================

export const adminAnalyticsApi = {
  // 收入估算
  getRevenueEstimate(startDate, endDate) {
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return api.get('/admin/analytics/revenue/estimate', { params })
  },

  // 按租赁选项周收入
  getWeeklyByHireOption(startDate) {
    const params = {}
    if (startDate) params.startDate = startDate
    return api.get('/admin/analytics/revenue/weekly-by-hire-option', { params })
  },

  // 每日收入
  getDailyCombined(startDate) {
    const params = {}
    if (startDate) params.startDate = startDate
    return api.get('/admin/analytics/revenue/daily-combined', { params })
  },

  // 周收入图表数据
  getWeeklyChart(startDate) {
    const params = {}
    if (startDate) params.startDate = startDate
    return api.get('/admin/analytics/revenue/weekly-chart', { params })
  },

  // 高频用户列表
  getFrequentUsers() {
    return api.get('/admin/analytics/usage/frequent-users')
  }
}

// ==================== Admin Bookings API ====================

export const adminBookingsApi = {
  // 获取预订列表（管理端）
  list(status, paymentStatus, customerType) {
    const params = {}
    if (status) params.status = status
    if (paymentStatus) params.paymentStatus = paymentStatus
    if (customerType) params.customerType = customerType
    return api.get('/admin/bookings', { params })
  },

  // 获取预订详情（管理端）
  getDetail(bookingId) {
    return api.get(`/admin/bookings/${bookingId}`)
  },

  // 经理覆盖修改预订
  override(bookingId, data) {
    return api.patch(`/admin/bookings/${bookingId}/override`, data)
  }
}

// ==================== Admin Users API ====================

export const adminUsersApi = {
  // 获取用户列表
  list(role, accountStatus) {
    const params = {}
    if (role) params.role = role
    if (accountStatus) params.accountStatus = accountStatus
    return api.get('/admin/users', { params })
  },

  // 获取用户详情
  getUser(userId) {
    return api.get(`/admin/users/${userId}`)
  },

  // 更新用户状态
  updateStatus(userId, accountStatus) {
    return api.patch(`/admin/users/${userId}/status`, { accountStatus })
  },

  // 获取用户预订历史
  getUserBookings(userId) {
    return api.get(`/admin/users/${userId}/bookings`)
  },

  // 获取审计日志
  getAuditLogs(action, limit) {
    const params = {}
    if (action) params.action = action
    if (limit) params.limit = limit
    return api.get('/admin/audit-logs', { params })
  }
}

// ==================== Staff Bookings API (员工端 - 游客预订) ====================

export const staffBookingsApi = {
  // 创建游客预订
  createGuestBooking(data) {
    return api.post('/staff/bookings/guest', data)
  },

  // 获取游客预订详情
  getGuestBooking(bookingId) {
    return api.get(`/staff/bookings/guest/${bookingId}`)
  }
}

// ==================== Admin Issues API ====================

export const adminIssuesApi = {
  // 获取问题列表（管理端）
  listAdmin(status, priority) {
    const params = {}
    if (status) params.status = status
    if (priority) params.priority = priority
    return api.get('/admin/issues', { params })
  },

  // 创建问题（管理端）
  createIssue(data) {
    return api.post('/issues', data)
  },

  // 获取问题详情（管理端）
  getIssue(issueId) {
    return api.get(`/issues/${issueId}`)
  },

  // 更新优先级
  updatePriority(issueId, priority) {
    return api.patch(`/admin/issues/${issueId}/priority`, { priority })
  },

  // 更新状态
  updateStatus(issueId, status) {
    return api.patch(`/admin/issues/${issueId}/status`, { status })
  },

  // 解决问题
  resolveIssue(issueId, feedback) {
    const data = feedback ? { feedback } : {}
    return api.post(`/admin/issues/${issueId}/resolve`, data)
  },

  // 添加评论
  addComment(issueId, message) {
    return api.post(`/issues/${issueId}/comments`, { message })
  },

  // 获取高优先级问题
  listHighPriority() {
    return api.get('/admin/issues/high-priority')
  }
}

export default api
