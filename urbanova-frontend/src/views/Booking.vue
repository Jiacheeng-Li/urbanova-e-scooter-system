<template>
  <div class="booking-container">
    <h1 class="page-title">预订滑板车</h1>
    <p class="page-description">选择滑板车和租赁选项来完成预订</p>

    <el-card class="booking-form-card">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        v-loading="loading"
      >
        <el-form-item label="滑板车ID" prop="scooterId">
          <el-select
            v-model="formData.scooterId"
            placeholder="请选择滑板车"
            style="width: 100%"
          >
            <el-option
              v-for="scooter in availableScooters"
              :key="scooter.scooterId"
              :label="`${scooter.scooterId} - ${scooter.status}`"
              :value="scooter.scooterId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="租赁选项" prop="hireOptionId">
          <el-select
            v-model="formData.hireOptionId"
            placeholder="请选择租赁选项"
            style="width: 100%"
          >
            <el-option
              v-for="option in hireOptions"
              :key="option.hireOptionId"
              :label="`${option.code} - ${formatDuration(option.durationMinutes)} - £${option.basePrice}`"
              :value="option.hireOptionId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="计划开始时间" prop="plannedStartAt">
          <el-date-picker
            v-model="formData.plannedStartAt"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            class="submit-button"
            @click="handleSubmit"
          >
            确认预订
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预订成功对话框 -->
    <el-dialog
      v-model="successDialogVisible"
      title="预订成功"
      width="450px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="success-content">
        <el-result
          icon="success"
          title="预订成功"
          sub-title="您的滑板车预订已成功创建"
        >
          <template #extra>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="预订ID">
                {{ bookingResult.bookingId }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(bookingResult.status)">
                  {{ bookingResult.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ formatDateTime(bookingResult.startAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="结束时间">
                {{ formatDateTime(bookingResult.endAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="基础价格">
                £{{ bookingResult.priceBreakdown.base }}
              </el-descriptions-item>
              <el-descriptions-item label="折扣">
                £{{ bookingResult.priceBreakdown.discount }}
              </el-descriptions-item>
              <el-descriptions-item label="最终价格">
                <strong>£{{ bookingResult.priceBreakdown.finalPrice }}</strong>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-result>
      </div>

      <template #footer>
        <el-button type="primary" @click="goToMyBookings">
          查看我的预订
        </el-button>
        <el-button @click="resetFormAndContinue">
          继续预订
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { hireOptionsApi, bookingApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const hireOptions = ref([])
const availableScooters = ref([
  { scooterId: 'SCO-0001', status: 'AVAILABLE' },
  { scooterId: 'SCO-0002', status: 'AVAILABLE' },
  { scooterId: 'SCO-0003', status: 'AVAILABLE' },
  { scooterId: 'SCO-0004', status: 'AVAILABLE' },
  { scooterId: 'SCO-0005', status: 'AVAILABLE' }
])
const successDialogVisible = ref(false)
const bookingResult = ref({})

const formData = reactive({
  scooterId: '',
  hireOptionId: '',
  plannedStartAt: ''
})

const formRules = {
  scooterId: [
    { required: true, message: '请选择滑板车', trigger: 'change' }
  ],
  hireOptionId: [
    { required: true, message: '请选择租赁选项', trigger: 'change' }
  ],
  plannedStartAt: [
    { required: true, message: '请选择计划开始时间', trigger: 'change' }
  ]
}

const formatDuration = (minutes) => {
  if (minutes < 60) {
    return `${minutes} 分钟`
  } else if (minutes === 60) {
    return '1 小时'
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    return `${hours} 小时`
  } else {
    const days = Math.floor(minutes / 1440)
    return `${days} 天`
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const getStatusType = (status) => {
  const statusMap = {
    'CONFIRMED': 'success',
    'CANCELLED': 'danger',
    'COMPLETED': 'info'
  }
  return statusMap[status] || 'info'
}

const fetchHireOptions = async () => {
  loading.value = true
  try {
    const response = await hireOptionsApi.list()
    hireOptions.value = response.data.data
  } catch (error) {
    ElMessage.error('获取租赁选项失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const requestData = {
          scooterId: formData.scooterId,
          hireOptionId: formData.hireOptionId,
          plannedStartAt: new Date(formData.plannedStartAt).toISOString()
        }

        const response = await bookingApi.create(requestData)
        bookingResult.value = response.data.data

        // 保存到本地存储（模拟后端返回历史记录）
        const storedBookings = JSON.parse(localStorage.getItem('myBookings') || '[]')
        storedBookings.unshift(bookingResult.value)
        localStorage.setItem('myBookings', JSON.stringify(storedBookings))

        successDialogVisible.value = true
        ElMessage.success('预订成功！')
      } catch (error) {
        const errorMsg = error.response?.data?.error?.message || '预订失败，请稍后重试'
        ElMessage.error(errorMsg)
      } finally {
        submitting.value = false
      }
    }
  })
}

const goToMyBookings = () => {
  successDialogVisible.value = false
  router.push('/my-bookings')
}

const resetFormAndContinue = () => {
  successDialogVisible.value = false
  resetForm()
}

const resetForm = () => {
  formData.scooterId = ''
  formData.hireOptionId = ''
  formData.plannedStartAt = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  fetchHireOptions()
})
</script>

<style scoped>
.booking-container {
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  text-align: center;
  color: #303133;
  margin-bottom: 8px;
}

.page-description {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}

.booking-form-card {
  border-radius: 12px;
}

.submit-button {
  width: 100%;
  margin-top: 10px;
}

.success-content {
  padding: 0 10px;
}
</style>
