<template>
  <div class="login-container">
    <!-- 动态星空背景 -->
    <div class="stars-bg"></div>
    <div class="twinkling-bg"></div>

    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="card-header">
          <div class="logo-icon">✨</div>
          <h2>Welcome Back</h2>
          <p class="subtitle">Sign in to continue your journey with Urbanova</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="Email" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="Enter your email"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="Enter your password"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="authStore.loading"
            class="login-button"
            @click="handleLogin"
          >
            <span v-if="!authStore.loading">Login</span>
            <span v-else>Logging in...</span>
            <span class="btn-star" v-if="!authStore.loading">✨</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <span>Don't have an account?</span>
        <el-link type="primary" @click="goToRegister" class="register-link">
          Create Account
          <span class="link-star">→</span>
        </el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)

const formData = reactive({
  email: '',
  password: ''
})

const formRules = {
  email: [
    { required: true, message: 'Please enter email', trigger: 'blur' },
    { type: 'email', message: 'Please enter a valid email address', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Please enter password', trigger: 'blur' },
    { min: 8, message: 'Password must be at least 8 characters', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      const result = await authStore.login(formData)

      if (result.success) {
        ElMessage.success(`Welcome back, ${result.user.fullName}!`)

        const redirect = route.query.redirect || '/Urbanova'
        router.push(redirect)
      } else {
        formData.password = ''
        ElMessage.error(result.message || 'Login failed')
      }
    }
  })
}

onMounted(() => {
  if (route.query.error === 'unknown') {
    ElMessage.error('An unknown error occurred, please contact administrator')
  }
})

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0a0a2a 0%, #1a1a3a 50%, #2a1a4a 100%);
}

/* 动态星空背景 */
.stars-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPjxkZWZzPjxwYXR0ZXJuIGlkPSJhIiB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIHBhdHRlcm5Vbml0cz0idXNlclNwYWNlT25Vc2UiIHBhdHRlcm5UcmFuc2Zvcm09InJvdGF0ZSg0NSkiPjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjEiIGZpbGw9IndoaXRlIiBmaWxsLW9wYWNpdHk9IjAuMyIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNhKSIvPjwvc3ZnPg==') repeat;
  pointer-events: none;
  z-index: 0;
  animation: starMove 60s linear infinite;
}

.twinkling-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPjxkZWZzPjxwYXR0ZXJuIGlkPSJiIiB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iMS41IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjUiLz48Y2lyY2xlIGN4PSIyMCIgY3k9IjgwIiByPSIwLjgiIGZpbGw9IndoaXRlIiBmaWxsLW9wYWNpdHk9IjAuNCIvPjxjaXJjbGUgY3g9IjcwIiBjeT0iMTUiIHI9IjEiIGZpbGw9IndoaXRlIiBmaWxsLW9wYWNpdHk9IjAuNiIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNiKSIvPjwvc3ZnPg==') repeat;
  pointer-events: none;
  z-index: 0;
  animation: twinkle 4s ease-in-out infinite;
}

@keyframes starMove {
  0% { transform: translateX(0) translateY(0); }
  100% { transform: translateX(-50px) translateY(-50px); }
}

@keyframes twinkle {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 28px;
  background: rgba(30, 20, 60, 0.75);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(168, 85, 247, 0.3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.login-card:hover {
  transform: translateY(-5px);
  border-color: rgba(168, 85, 247, 0.5);
  box-shadow: 0 25px 50px rgba(168, 85, 247, 0.2);
}

:deep(.el-card__header) {
  border-bottom: 1px solid rgba(168, 85, 247, 0.2);
  padding: 24px 24px 16px;
}

.card-header {
  text-align: center;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.card-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #d8b4fe);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

:deep(.el-card__body) {
  padding: 24px;
}

:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
  padding-bottom: 6px;
}

:deep(.el-input__wrapper) {
  background: rgba(20, 15, 40, 0.6);
  border: 1px solid rgba(168, 85, 247, 0.3);
  border-radius: 16px;
  box-shadow: none;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(168, 85, 247, 0.6);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #a855f7;
  box-shadow: 0 0 0 2px rgba(168, 85, 247, 0.2);
}

:deep(.el-input__inner) {
  color: white;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.el-input__prefix) {
  color: #a855f7;
}

.login-button {
  width: 100%;
  margin-top: 10px;
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  border: none;
  border-radius: 50px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(168, 85, 247, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

.btn-star {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.login-button:hover .btn-star {
  transform: rotate(15deg) scale(1.1);
}

.footer-links {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(168, 85, 247, 0.2);
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.register-link {
  margin-left: 8px;
  color: #c77dff !important;
  font-weight: 500;
}

.register-link:hover {
  color: #d8b4fe !important;
}

.link-star {
  display: inline-block;
  margin-left: 4px;
  transition: transform 0.3s ease;
}

.register-link:hover .link-star {
  transform: translateX(4px);
}
</style>