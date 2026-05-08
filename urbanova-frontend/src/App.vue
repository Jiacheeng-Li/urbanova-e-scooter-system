<template>
  <div id="app">
    <!-- 只在非管理端页面显示用户端导航栏 -->
    <template v-if="!isAdminRoute">
      <el-container class="app-container">
        <el-header class="app-header">
          <div class="header-content">
            <!-- 左侧Logo - 可点击跳转到Urbanova -->
            <div class="logo" @click="goToUrbanova">
              <img src="/logo.png" alt="Urbanova Logo" class="logo-image">
            </div>

            <!-- 右侧用户操作区域 -->
            <div class="header-actions">
              <template v-if="authStore.isLoggedIn">
                <el-dropdown @command="handleUserCommand">
                  <span class="user-info">
                    <el-icon><User /></el-icon>
                    {{ authStore.currentUser?.fullName || 'User' }}
                    <el-icon><ArrowDown /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        command="admin"
                        v-if="authStore.currentUser?.role === 'MANAGER'">
                        Admin Panel
                      </el-dropdown-item>
                      <el-dropdown-item command="logout" divided>
                        Logout
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

              <template v-else>
                <el-button class="login-btn" @click="goToLogin">Login</el-button>
                <el-button class="register-btn" type="primary" @click="goToRegister">Register</el-button>
              </template>
            </div>
          </div>
        </el-header>

        <el-main class="app-main">
          <router-view />
        </el-main>

        <el-footer class="app-footer">
          <p>&copy; 2026 Urbanova Electric Scooter Rental System</p>
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
import { User, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin')
})

const goToUrbanova = () => {
  router.push('/Urbanova')
}

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
  background: linear-gradient(135deg, #f5f0ff 0%, #e8e0ff 100%);
}

#app {
  min-height: 100vh;
}

.app-container {
  min-height: 100vh;
}

/* 浅紫星空主题 - 导航栏样式 */
.app-header {
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.95) 0%, rgba(75, 0, 130, 0.95) 100%);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding: 0;
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.2);
  position: relative;
  overflow: hidden;
}

/* 添加星空效果 - 仅背景装饰 */
.app-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(2px 2px at 20px 30px, #fff, rgba(0,0,0,0)),
    radial-gradient(1px 1px at 60px 80px, #fff, rgba(0,0,0,0)),
    radial-gradient(3px 3px at 100px 150px, #fff, rgba(0,0,0,0)),
    radial-gradient(1px 1px at 200px 50px, #fff, rgba(0,0,0,0)),
    radial-gradient(2px 2px at 300px 120px, #fff, rgba(0,0,0,0));
  background-repeat: no-repeat;
  background-size: 200px 200px;
  opacity: 0.3;
  pointer-events: none;
  animation: twinkle 4s infinite;
}

@keyframes twinkle {
  0%, 100% { opacity: 0.2; }
  50% { opacity: 0.5; }
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

/* Logo 样式 */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.05);
}

.logo-image {
  width: 400px;
  height: 180px;
  object-fit: contain;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  transform: translateX(-140px);
}

.logo h1 {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #fff 0%, #e0d4ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 右侧操作区域 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 登录注册按钮样式 */
.login-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: white;
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-2px);
}

.register-btn {
  background: linear-gradient(135deg, #fff 0%, #e0d4ff 100%);
  border: none;
  color: #8A2BE2;
  font-weight: 600;
  transition: all 0.3s ease;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(138, 43, 226, 0.3);
}

/* 用户信息样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

/* 主内容区域 - 浅紫色背景 */
.app-main {
  background: linear-gradient(135deg, #f5f0ff 0%, #e8e0ff 100%);
  min-height: calc(100vh - 120px);
  position: relative;
  padding: 0;  /* 确保没有内边距 */
  margin: 0;  /* 确保没有外边距 */
}

/* 添加星空装饰点到主内容区（可选，不影响功能） */
.app-main::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(1px 1px at 10% 20%, rgba(138, 43, 226, 0.2), rgba(0,0,0,0)),
    radial-gradient(2px 2px at 30% 50%, rgba(138, 43, 226, 0.15), rgba(0,0,0,0)),
    radial-gradient(1px 1px at 70% 80%, rgba(138, 43, 226, 0.2), rgba(0,0,0,0)),
    radial-gradient(1px 1px at 90% 30%, rgba(138, 43, 226, 0.1), rgba(0,0,0,0));
  background-repeat: no-repeat;
  pointer-events: none;
  z-index: 0;
}

/* 确保内容在星空之上 */
.app-main > * {
  position: relative;
  z-index: 1;
}

/* 页脚样式 - 浅紫主题 */
.app-footer {
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.9) 0%, rgba(75, 0, 130, 0.9) 100%);
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .logo h1 {
    font-size: 18px;
  }
  
  .logo-image {
    width: 30px;
    height: 30px;
  }
  
  .header-content {
    padding: 0 15px;
  }
  
  .user-info {
    padding: 6px 12px;
    font-size: 14px;
  }
  
  .login-btn, .register-btn {
    padding: 8px 16px;
    font-size: 12px;
  }
}
</style>