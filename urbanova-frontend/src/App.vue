<template>
  <div id="app">
    <!-- 只在非管理端页面显示用户端导航栏 -->
    <template v-if="!isAdminRoute">
      <el-container class="app-container">
        <el-header class="app-header">
          <div class="header-content">
            <router-link to="/" class="logo">
              <h1>Urbanova</h1>
            </router-link>

            <el-menu
              mode="horizontal"
              :router="true"
              :default-active="currentRoute"
              class="header-menu"
            >
              <el-menu-item index="/hire-options">
                <el-icon><Tickets /></el-icon>
                租赁选项
              </el-menu-item>

              <el-menu-item index="/booking" v-if="authStore.isLoggedIn">
                <el-icon><Calendar /></el-icon>
                预订滑板车
              </el-menu-item>

              <el-menu-item index="/my-bookings" v-if="authStore.isLoggedIn">
                <el-icon><List /></el-icon>
                我的预订
              </el-menu-item>
            </el-menu>

            <div class="header-actions">
              <template v-if="authStore.isLoggedIn">
                <el-dropdown @command="handleUserCommand">
                  <span class="user-info">
                    <el-icon><User /></el-icon>
                    {{ authStore.currentUser?.fullName || '用户' }}
                    <el-icon><ArrowDown /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="profile">
                        个人信息
                      </el-dropdown-item>
                      <el-dropdown-item 
                        command="admin" 
                        v-if="authStore.currentUser?.role === 'MANAGER'">
                        管理界面
                      </el-dropdown-item>
                      <el-dropdown-item command="logout" divided>
                        退出登录
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

              <template v-else>
                <el-button @click="goToLogin">登录</el-button>
                <el-button type="primary" @click="goToRegister">注册</el-button>
              </template>
            </div>
          </div>
        </el-header>

        <el-main class="app-main">
          <router-view />
        </el-main>

        <el-footer class="app-footer">
          <p>&copy; 2026 Urbanova 电动滑板车租赁系统</p>
        </el-footer>
      </el-container>
    </template>

    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { Tickets, Calendar, List, User, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin')
})

const currentRoute = computed(() => route.path)

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const handleUserCommand = (command) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
    localStorage.removeItem('myBookings')
  } else if (command === 'profile') {
    // TODO: 个人中心页面
  } else if (command === 'admin') {
    router.push('/admin/dashboard')
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

#app {
  min-height: 100vh;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo {
  text-decoration: none;
  margin-right: 40px;
}

.logo h1 {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
  margin: 0;
}

.header-menu {
  flex: 1;
  border-bottom: none !important;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.app-main {
  background-color: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.app-footer {
  background-color: #fff;
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
  border-top: 1px solid #e4e7ed;
}
</style>