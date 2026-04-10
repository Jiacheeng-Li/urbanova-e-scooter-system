<template>
  <div class="hire-option-manage-container">
    <div class="page-header">
      <h1 class="page-title">租赁选项管理</h1>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增租赁选项
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table 
        :data="hireOptions" 
        v-loading="loading"
        stripe
        border
      >
        <el-table-column prop="hireOptionId" label="选项ID" width="140" />
        <el-table-column prop="code" label="代码" width="100" />
        <el-table-column prop="durationMinutes" label="时长" width="120">
          <template #default="{ row }">
            {{ formatDuration(row.durationMinutes) }}
          </template>
        </el-table-column>
        <el-table-column prop="basePrice" label="基础价格" width="120">
          <template #default="{ row }">
            £{{ row.basePrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'danger'">
              {{ row.active ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              link 
              :disabled="!row.active"
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增租赁选项' : '编辑租赁选项'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="选项代码" prop="code" v-if="dialogMode === 'create'">
          <el-input 
            v-model="formData.code" 
            placeholder="例如: H2, D2"
            :disabled="submitting"
          />
          <div class="form-tip">代码将自动转换为大写</div>
        </el-form-item>

        <el-form-item label="时长(分钟)" prop="durationMinutes">
          <el-input-number 
            v-model="formData.durationMinutes" 
            :min="15" 
            :step="15"
            style="width: 100%"
            :disabled="submitting"
          />
          <div class="form-tip">预览: {{ formatDuration(formData.durationMinutes || 0) }}</div>
        </el-form-item>

        <el-form-item label="基础价格(£)" prop="basePrice">
          <el-input-number 
            v-model="formData.basePrice" 
            :min="0" 
            :precision="2" 
            :step="0.5"
            style="width: 100%"
            :disabled="submitting"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 禁用确认对话框 -->
    <el-dialog
      v-model="disableDialogVisible"
      title="确认禁用"
      width="400px"
    >
      <p>确定要禁用租赁选项 <strong>{{ selectedOption?.code }}</strong> 吗？</p>
      <p class="warning-text">禁用后该选项将无法被用户选择预订。</p>
      <template #footer>
        <el-button @click="disableDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="disabling" @click="confirmDisable">
          确认禁用
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminHireOptionsApi } from '../../api'

const loading = ref(false)
const hireOptions = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const disableDialogVisible = ref(false)
const disabling = ref(false)
const selectedOption = ref(null)
const formRef = ref(null)

const formData = ref({
  code: '',
  durationMinutes: 60,
  basePrice: 0
})

const formRules = {
  code: [
    { required: true, message: '请输入选项代码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]+$/, message: '代码只能包含字母和数字', trigger: 'blur' },
    { min: 1, max: 20, message: '代码长度1-20个字符', trigger: 'blur' }
  ],
  durationMinutes: [
    { required: true, message: '请输入时长', trigger: 'blur' },
    { type: 'number', min: 15, message: '时长至少15分钟', trigger: 'blur' }
  ],
  basePrice: [
    { required: true, message: '请输入基础价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能为负数', trigger: 'blur' }
  ]
}

const formatDuration = (minutes) => {
  if (!minutes) return '-'
  if (minutes < 60) {
    return `${minutes} 分钟`
  } else if (minutes === 60) {
    return '1 小时'
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return mins > 0 ? `${hours} 小时 ${mins} 分钟` : `${hours} 小时`
  } else {
    const days = Math.floor(minutes / 1440)
    const hours = Math.floor((minutes % 1440) / 60)
    return hours > 0 ? `${days} 天 ${hours} 小时` : `${days} 天`
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchHireOptions = async () => {
  loading.value = true
  try {
    const response = await adminHireOptionsApi.list()
    hireOptions.value = response.data.data
  } catch (error) {
    ElMessage.error('获取租赁选项列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    code: '',
    durationMinutes: 60,
    basePrice: 0
  }
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (option) => {
  dialogMode.value = 'edit'
  selectedOption.value = option
  formData.value = {
    code: option.code,
    durationMinutes: option.durationMinutes,
    basePrice: option.basePrice
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        const requestData = {
          code: formData.value.code.toUpperCase(),
          durationMinutes: formData.value.durationMinutes,
          basePrice: formData.value.basePrice
        }
        const response = await adminHireOptionsApi.create(requestData)
        ElMessage.success('创建成功')
        hireOptions.value.unshift(response.data.data)
      } else {
        const requestData = {
          durationMinutes: formData.value.durationMinutes,
          basePrice: formData.value.basePrice
        }
        const response = await adminHireOptionsApi.update(
          selectedOption.value.hireOptionId,
          requestData
        )
        ElMessage.success('更新成功')
        const index = hireOptions.value.findIndex(
          item => item.hireOptionId === selectedOption.value.hireOptionId
        )
        if (index !== -1) {
          hireOptions.value[index] = response.data.data
        }
      }
      dialogVisible.value = false
    } catch (error) {
      const errorMsg = error.response?.data?.error?.message || '操作失败'
      ElMessage.error(errorMsg)
    } finally {
      submitting.value = false
    }
  })
}

const handleDisable = (option) => {
  selectedOption.value = option
  disableDialogVisible.value = true
}

const confirmDisable = async () => {
  if (!selectedOption.value) return

  disabling.value = true
  try {
    const response = await adminHireOptionsApi.disable(selectedOption.value.hireOptionId)
    ElMessage.success('禁用成功')
    const index = hireOptions.value.findIndex(
      item => item.hireOptionId === selectedOption.value.hireOptionId
    )
    if (index !== -1) {
      hireOptions.value[index] = response.data.data
    }
    disableDialogVisible.value = false
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || '禁用失败'
    ElMessage.error(errorMsg)
  } finally {
    disabling.value = false
  }
}

onMounted(() => {
  fetchHireOptions()
})
</script>

<style scoped>
.hire-option-manage-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.table-card {
  border-radius: 12px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.warning-text {
  color: #e6a23c;
  font-size: 13px;
  margin-top: 8px;
}
</style>