<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>Login to Urbanova</h2>
          <p class="subtitle">Electric Scooter Rental System</p>
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
            placeholder="Enter email"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="Enter password"
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
            Login
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <span>Don't have an account?</span>
        <el-link type="primary" @click="goToRegister">Register Now</el-link>
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

        const redirect = route.query.redirect || '/hire-options'
        router.push(redirect)
      } else {
        // 登录失败时清空密码
        formData.password = ''
        ElMessage.error(result.message || 'Login failed')
      }
    }
  })
}

onMounted(() => {
  // 检查是否有未知错误参数
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}

.footer-links {
  text-align: center;
  margin-top: 20px;
  color: #606266;
}

.footer-links .el-link {
  margin-left: 8px;
}
</style>
