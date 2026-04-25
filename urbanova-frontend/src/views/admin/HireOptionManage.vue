<template>
  <div class="hire-option-manage-container">
    <div class="page-header">
      <h1 class="page-title">Hire Options Management</h1>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        Add Hire Option
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table
        :data="hireOptions"
        v-loading="loading"
        stripe
        border
      >
        <el-table-column prop="hireOptionId" label="Option ID" width="140" />
        <el-table-column prop="code" label="Code" width="100" />
        <el-table-column prop="durationMinutes" label="Duration" width="120">
          <template #default="{ row }">
            {{ formatDuration(row.durationMinutes) }}
          </template>
        </el-table-column>
        <el-table-column prop="basePrice" label="Base Price" width="120">
          <template #default="{ row }">
            £{{ row.basePrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="active" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'danger'">
              {{ row.active ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated At" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="openEditDialog(row)"
            >
              Edit
            </el-button>
            <el-button
              type="danger"
              link
              :disabled="!row.active"
              @click="handleDisable(row)"
            >
              Disable
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? 'Add Hire Option' : 'Edit Hire Option'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="Option Code" prop="code" v-if="dialogMode === 'create'">
          <el-input
            v-model="formData.code"
            placeholder="e.g., H2, D2"
            :disabled="submitting"
          />
          <div class="form-tip">Code will be converted to uppercase</div>
        </el-form-item>

        <el-form-item label="Duration (minutes)" prop="durationMinutes">
          <el-input-number
            v-model="formData.durationMinutes"
            :min="15"
            :step="15"
            style="width: 100%"
            :disabled="submitting"
          />
          <div class="form-tip">Preview: {{ formatDuration(formData.durationMinutes || 0) }}</div>
        </el-form-item>

        <el-form-item label="Base Price (£)" prop="basePrice">
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
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogMode === 'create' ? 'Create' : 'Save' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 禁用确认对话框 -->
    <el-dialog
      v-model="disableDialogVisible"
      title="Confirm Disable"
      width="400px"
    >
      <p>Are you sure you want to disable hire option <strong>{{ selectedOption?.code }}</strong>?</p>
      <p class="warning-text">Disabled options will not be available for users to select.</p>
      <template #footer>
        <el-button @click="disableDialogVisible = false">Cancel</el-button>
        <el-button type="danger" :loading="disabling" @click="confirmDisable">
          Confirm Disable
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
    { required: true, message: 'Please enter option code', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]+$/, message: 'Code can only contain letters and numbers', trigger: 'blur' },
    { min: 1, max: 20, message: 'Code must be between 1 and 20 characters', trigger: 'blur' }
  ],
  durationMinutes: [
    { required: true, message: 'Please enter duration', trigger: 'blur' },
    { type: 'number', min: 15, message: 'Duration must be at least 15 minutes', trigger: 'blur' }
  ],
  basePrice: [
    { required: true, message: 'Please enter base price', trigger: 'blur' },
    { type: 'number', min: 0, message: 'Price cannot be negative', trigger: 'blur' }
  ]
}

const formatDuration = (minutes) => {
  if (!minutes) return '-'
  if (minutes < 60) {
    return `${minutes} minutes`
  } else if (minutes === 60) {
    return '1 hour'
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return mins > 0 ? `${hours} hours ${mins} minutes` : `${hours} hours`
  } else {
    const days = Math.floor(minutes / 1440)
    const hours = Math.floor((minutes % 1440) / 60)
    return hours > 0 ? `${days} days ${hours} hours` : `${days} days`
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const fetchHireOptions = async () => {
  loading.value = true
  try {
    const response = await adminHireOptionsApi.list()
    hireOptions.value = response.data.data
  } catch (error) {
    ElMessage.error('Failed to fetch hire options')
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
        ElMessage.success('Created successfully')
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
        ElMessage.success('Updated successfully')
        const index = hireOptions.value.findIndex(
          item => item.hireOptionId === selectedOption.value.hireOptionId
        )
        if (index !== -1) {
          hireOptions.value[index] = response.data.data
        }
      }
      dialogVisible.value = false
    } catch (error) {
      const errorMsg = error.response?.data?.error?.message || 'Operation failed'
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
    ElMessage.success('Disabled successfully')
    const index = hireOptions.value.findIndex(
      item => item.hireOptionId === selectedOption.value.hireOptionId
    )
    if (index !== -1) {
      hireOptions.value[index] = response.data.data
    }
    disableDialogVisible.value = false
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Disable failed'
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
