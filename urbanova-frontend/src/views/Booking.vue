<template>
  <div class="booking-container">
    <h1 class="page-title">Book a Scooter</h1>
    <p class="page-description">Select a scooter and hire option to complete your booking</p>

    <el-card class="booking-form-card">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        v-loading="loading"
      >
        <el-form-item label="Scooter ID" prop="scooterId">
          <el-select
            v-model="formData.scooterId"
            placeholder="Select a scooter"
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

        <el-form-item label="Hire Option" prop="hireOptionId">
          <el-select
            v-model="formData.hireOptionId"
            placeholder="Select a hire option"
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

        <el-form-item label="Planned Start Time" prop="plannedStartAt">
          <el-date-picker
            v-model="formData.plannedStartAt"
            type="datetime"
            placeholder="Select start time"
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
            Confirm Booking
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预订成功对话框 -->
    <el-dialog
      v-model="successDialogVisible"
      title="Booking Successful"
      width="450px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="success-content">
        <el-result
          icon="success"
          title="Booking Successful"
          sub-title="Your scooter booking has been created successfully"
        >
          <template #extra>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="Booking ID">
                {{ bookingResult.bookingId }}
              </el-descriptions-item>
              <el-descriptions-item label="Status">
                <el-tag :type="getStatusType(bookingResult.status)">
                  {{ bookingResult.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="Start Time">
                {{ formatDateTime(bookingResult.startAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="End Time">
                {{ formatDateTime(bookingResult.endAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="Base Price">
                £{{ bookingResult.priceBreakdown.base }}
              </el-descriptions-item>
              <el-descriptions-item label="Discount">
                £{{ bookingResult.priceBreakdown.discount }}
              </el-descriptions-item>
              <el-descriptions-item label="Final Price">
                <strong>£{{ bookingResult.priceBreakdown.finalPrice }}</strong>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-result>
      </div>

      <template #footer>
        <el-button type="primary" @click="goToMyBookings">
          View My Bookings
        </el-button>
        <el-button @click="resetFormAndContinue">
          Continue Booking
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { hireOptionsApi, scooterApi, bookingApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const hireOptions = ref([])
const availableScooters = ref([])
const successDialogVisible = ref(false)
const bookingResult = ref({})

const formData = reactive({
  scooterId: '',
  hireOptionId: '',
  plannedStartAt: ''
})

const formRules = {
  scooterId: [
    { required: true, message: 'Please select a scooter', trigger: 'change' }
  ],
  hireOptionId: [
    { required: true, message: 'Please select a hire option', trigger: 'change' }
  ],
  plannedStartAt: [
    { required: true, message: 'Please select planned start time', trigger: 'change' }
  ]
}

const formatDuration = (minutes) => {
  if (minutes < 60) {
    return `${minutes} minutes`
  } else if (minutes === 60) {
    return '1 hour'
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    return `${hours} hours`
  } else {
    const days = Math.floor(minutes / 1440)
    return `${days} days`
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
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
    ElMessage.error('Failed to fetch hire options')
  } finally {
    loading.value = false
  }
}

const fetchAvailableScooters = async () => {
  try {
    const response = await scooterApi.getScooterIdsByStatus('AVAILABLE')
    const scooterIds = response.data.data.scooterIds || []
    availableScooters.value = scooterIds.map(id => ({
      scooterId: id,
      status: 'AVAILABLE'
    }))
  } catch (error) {
    ElMessage.error('Failed to fetch available scooters')
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
        ElMessage.success('Booking successful!')
      } catch (error) {
        const errorMsg = error.response?.data?.error?.message || 'Booking failed, please try again'
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

const resetFormAndContinue = async () => {
  successDialogVisible.value = false
  await fetchAvailableScooters()
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
  fetchAvailableScooters()
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
