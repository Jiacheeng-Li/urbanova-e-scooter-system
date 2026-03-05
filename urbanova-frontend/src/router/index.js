import { createRouter, createWebHistory } from 'vue-router'

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

// 路由守卫 - 检查登录状态
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
