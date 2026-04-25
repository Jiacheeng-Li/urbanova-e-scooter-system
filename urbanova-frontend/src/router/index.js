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
    meta: { title: 'Login' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: 'Register' }
  },
  {
    path: '/hire-options',
    name: 'HireOptions',
    component: () => import('../views/HireOptions.vue'),
    meta: { title: 'Hire Options', requiresAuth: false }
  },
  {
    path: '/booking',
    name: 'Booking',
    component: () => import('../views/Booking.vue'),
    meta: { title: 'Book Scooter', requiresAuth: true }
  },
  {
    path: '/my-bookings',
    name: 'MyBookings',
    component: () => import('../views/MyBookings.vue'),
    meta: { title: 'My Bookings', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/Dashboard.vue'),
    meta: {
      title: 'Admin Dashboard',
      requiresAuth: true,
      requiresManager: true
    },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/admin/DashboardContent.vue'),
        meta: { title: 'Dashboard' }
      },
      {
        path: 'hire-options',
        name: 'AdminHireOptions',
        component: () => import('../views/admin/HireOptionManage.vue'),
        meta: { title: 'Manage Hire Options' }
      },
      {
        path: 'scooters',
        name: 'AdminScooters',
        component: () => import('../views/admin/ScooterManage.vue'),
        meta: { title: 'Manage Scooters' }
      },
      {
        path: 'bookings',
        name: 'AdminBookings',
        component: () => import('../views/admin/BookingManage.vue'),
        meta: { title: 'Manage Bookings' }
      },
      {
        path: 'issues',
        name: 'AdminIssues',
        component: () => import('../views/admin/IssueManage.vue'),
        meta: { title: 'Issue Management' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { title: 'User Management' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

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
        'This content requires login to access',
        'Notice',
        {
          confirmButtonText: 'Go to Login',
          cancelButtonText: 'Cancel',
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
