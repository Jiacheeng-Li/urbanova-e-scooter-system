<template>
  <div class="container">
    <div class="logo-wrapper">
      <h1 class="logo">Urbanova</h1>
      
      <!-- 新增用户按钮 -->
      <div class="test-buttons">
        <el-button type="primary" @click="openAddDialog">新增用户</el-button>
      </div>

      <!-- 新增用户弹窗 -->
      <el-dialog
        v-model="dialogVisible"
        title="新增用户"
        width="400px"
        :close-on-click-modal="false"
        @close="resetForm"
      >
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="80px"
          label-position="left"
        >
          <el-form-item label="姓名" prop="name">
            <el-input 
              v-model="formData.name" 
              placeholder="请输入姓名"
              clearable
            />
          </el-form-item>
          
          <el-form-item label="年龄" prop="age">
            <el-input-number 
              v-model="formData.age" 
              :min="1" 
              :max="150"
              placeholder="请输入年龄"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button 
              type="primary" 
              @click="submitAdd" 
              :loading="loading"
            >
              新增
            </el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 显示新增结果 -->
      <el-card v-if="resultVisible" class="result-card" :shadow="hover">
        <template #header>
          <span>新增结果</span>
        </template>
        <div class="result-content">
          <el-result
            :icon="isSuccess ? 'success' : 'error'"
            :title="isSuccess ? '新增成功' : '新增失败'"
            :sub-title="isSuccess ? '用户已成功添加到系统' : '添加过程中出现错误'"
          >
            <template #extra>
              <div class="result-detail">
                <p v-if="addResult.id">用户ID：{{ addResult.id }}</p>
                <p v-if="addResult.name">姓名：{{ addResult.name }}</p>
                <p v-if="addResult.age">年龄：{{ addResult.age }}</p>
                <p v-if="!isSuccess && addResult.comment">
                  失败原因：{{ addResult.comment }}
                </p>
              </div>
            </template>
          </el-result>
        </div>
        <div class="result-actions">
          <el-button type="primary" @click="resultVisible = false">关闭</el-button>
          <el-button type="success" @click="openAddDialog">继续新增</el-button>
        </div>
      </el-card>

      <!-- 如果失败显示错误信息 -->
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        class="error-alert"
        :closable="false"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 响应式变量
const loading = ref(false)
const dialogVisible = ref(false)
const resultVisible = ref(false)
const addResult = ref({})
const errorMessage = ref('')
const formRef = ref(null)

// 计算属性：根据 comment 判断是否成功
const isSuccess = computed(() => {
  return addResult.value.comment === '添加成功'
})

// 表单数据
const formData = reactive({
  name: '',
  age: null
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  age: [
    { required: true, message: '请输入年龄', trigger: 'blur' },
    { type: 'number', min: 1, max: 150, message: '年龄必须在1-150之间', trigger: 'blur' }
  ]
}

// 打开新增弹窗
const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

// 提交新增
const submitAdd = async () => {
  // 表单验证
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      errorMessage.value = ''
      
      try {
        // 调用后端接口
        const response = await axios.post('/urbanova/test/add', {
          name: formData.name,
          age: formData.age
        })
        
        // 处理返回的数据
        addResult.value = response.data
        dialogVisible.value = false
        resultVisible.value = true
        
        // 根据 comment 显示不同提示
        if (response.data.comment === '添加成功') {
          ElMessage.success({
            message: `新增用户 ${formData.name} 成功！`,
            duration: 3000
          })
        } else {
          ElMessage.error({
            message: `新增失败：${response.data.comment}`,
            duration: 3000
          })
        }
        
      } catch (error) {
        console.error('新增失败:', error)
        errorMessage.value = error.response?.data?.message || error.message || '网络错误，请稍后重试'
        ElMessage.error('新增失败：网络错误')
      } finally {
        loading.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  formData.name = ''
  formData.age = null
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0;
  padding: 20px;
}

.logo-wrapper {
  text-align: center;
  max-width: 600px;
  width: 100%;
}

.logo {
  font-size: 6rem;
  font-weight: 800;
  color: white;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 4px;
  animation: fadeIn 1.5s ease-in-out;
  margin-bottom: 2rem;
  font-family: 'Arial', 'Helvetica', sans-serif;
}

.test-buttons {
  margin: 2rem 0;
}

.result-card {
  margin-top: 2rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  animation: slideIn 0.5s ease-out;
}

.result-content {
  padding: 1rem;
}

.result-detail {
  text-align: left;
  background: #f5f7fa;
  padding: 1rem;
  border-radius: 8px;
  margin-top: 1rem;
}

.result-detail p {
  margin: 0.5rem 0;
  color: #333;
}

.error-alert {
  margin-top: 2rem;
  animation: slideIn 0.5s ease-out;
}

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  margin-right: 0;
  border-bottom: 1px solid #eee;
  padding: 16px 20px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #eee;
  padding: 16px 20px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:global(body) {
  margin: 0;
  padding: 0;
}
</style>