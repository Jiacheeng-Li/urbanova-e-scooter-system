<template>
  <div class="admin-dashboard-container">
    <!-- 管理员专属导航栏 -->
    <el-menu
      mode="horizontal"
      :router="true"
      :default-active="currentRoute"
      class="admin-nav"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
    >
      <div class="nav-logo">
        <h2>Urbanova Admin</h2>
      </div>

      <el-menu-item index="/admin/dashboard">
        <el-icon><Odometer /></el-icon>
        Dashboard
      </el-menu-item>

      <el-menu-item index="/admin/hire-options">
        <el-icon><Tickets /></el-icon>
        Hire Options
      </el-menu-item>

      <el-menu-item index="/admin/scooters">
        <el-icon><Bicycle /></el-icon>
        Scooters
      </el-menu-item>

      <el-menu-item index="/admin/bookings">
        <el-icon><List /></el-icon>
        Bookings
      </el-menu-item>

      <el-menu-item index="/admin/issues">
        <el-icon><ChatLineRound /></el-icon>
        Issues
      </el-menu-item>

      <el-menu-item index="/admin/users">
        <el-icon><User /></el-icon>
        Users
      </el-menu-item>

      <div class="nav-actions">
        <el-button
          type="primary"
          link
          @click="goBackToHome"
          class="back-home-btn"
        >
          <el-icon><House /></el-icon>
          Back to Home
        </el-button>
        <el-button
          type="danger"
          link
          @click="handleLogout"
          class="logout-btn"
        >
          <el-icon><SwitchButton /></el-icon>
          Logout
        </el-button>
      </div>
    </el-menu>

    <!-- 子页面渲染区域 -->
    <div class="dashboard-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import {
  Odometer,
  Tickets,
  Bicycle,
  List,
  ChatLineRound,
  User,
  House,
  SwitchButton
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentRoute = computed(() => route.path)

const goBackToHome = () => {
  router.push('/hire-options')
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
  localStorage.removeItem('myBookings')
  ElMessage.success('Logged out successfully')
}
</script>

<style scoped>
.admin-dashboard-container {
  min-height: 100vh;
  background-color: #f0f2f6;
}

/* 管理员导航栏样式 */
.admin-nav {
  display: flex;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-logo h2 {
  margin: 0;
  color: #fff;
  font-size: 20px;
  margin-right: 40px;
}

.nav-actions {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.back-home-btn {
  color: #fff !important;
  font-size: 14px;
  padding: 10px 16px;
}

.back-home-btn:hover {
  background-color: #263445 !important;
}

.logout-btn {
  color: #f56c6c !important;
  font-size: 14px;
  padding: 10px 16px;
}

.logout-btn:hover {
  background-color: #263445 !important;
  color: #ff7878 !important;
}

/* 主要内容区域 */
.dashboard-content {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}
</style>
