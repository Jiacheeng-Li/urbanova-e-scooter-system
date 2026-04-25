<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <h2>Register for Urbanova</h2>
          <p class="subtitle">Join the Electric Scooter Rental Service</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="Full Name" prop="fullName">
          <el-input
            v-model="formData.fullName"
            placeholder="Enter full name"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="Email" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="Enter email"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <el-form-item label="Phone (Optional)" prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="Enter phone number"
            prefix-icon="Phone"
            size="large"
          />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="Enter password (at least 8 characters)"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="Confirm password"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="authStore.loading"
            class="register-button"
            @click="handleRegister"
          >
            Register
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-links">
        <span>Already have an account?</span>
        <el-link type="primary" @click="goToLogin">Login Now</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)

const formData = reactive({
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== formData.password) {
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const formRules = {
  fullName: [
    { required: true, message: 'Please enter full name', trigger: 'blur' },
    { max: 100, message: 'Full name cannot exceed 100 characters', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please enter email', trigger: 'blur' },
    { type: 'email', message: 'Please enter a valid email address', trigger: 'blur' }
  ],
  phone: [
    { max: 30, message: 'Phone number cannot exceed 30 characters', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Please enter password', trigger: 'blur' },
    { min: 8, max: 72, message: 'Password must be between 8 and 72 characters', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      const { confirmPassword, ...registerData } = formData
      const result = await authStore.register(registerData)

      if (result.success) {
        ElMessage.success(`Registration successful! Welcome ${result.user.fullName}!`)
        router.push('/hire-options')
      } else {
        ElMessage.error(result.message || 'Registration failed')
      }
    }
  })
}

onMounted(() => {
  // 检查是否已登录
  const token = localStorage.getItem('accessToken')
  if (token) {
    ElMessage.warning('You are already logged in')
    router.push('/hire-options')
  }
})

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 420px;
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

.register-button {
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
