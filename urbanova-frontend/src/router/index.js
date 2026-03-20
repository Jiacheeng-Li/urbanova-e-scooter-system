import { createRouter, createWebHistory } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const routes = [
  {
    path: '/',
    redirect: '/hire-options'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/hire-options',
    name: 'HireOptions',
    component: () => import('../views/HireOptions.vue'),
    meta: { title: '租赁选项', requiresAuth: false }
  },
  {
    path: '/booking',
    name: 'Booking',
    component: () => import('../views/Booking.vue'),
    meta: { title: '预订滑板车', requiresAuth: true }
  },
  {
    path: '/my-bookings',
    name: 'MyBookings',
    component: () => import('../views/MyBookings.vue'),
    meta: { title: '我的预订', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 将 router 实例暴露给 window，用于 axios 拦截器
window.__router__ = router

// 路由守卫 - 检查登录状态
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('accessToken')

  // 已登录用户访问登录页，跳转到首页
  if (to.path === '/login' && token) {
    next({ path: '/hire-options' })
    return
  }

  // 需要登录的页面但未登录
  if (to.meta.requiresAuth && !token) {
    try {
      await ElMessageBox.confirm(
        '该内容需要登录才能使用',
        '提示',
        {
          confirmButtonText: '前往登录',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } catch {
      // 用户点击取消，跳转到首页
      next({ path: '/hire-options' })
    }
  } else {
    next()
  }
})

export default router
