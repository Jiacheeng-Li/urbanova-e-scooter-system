<template>
  <div class="my-bookings-container">
    <h1 class="page-title">我的预订</h1>
    <p class="page-description">查看和管理您的滑板车预订</p>

    <el-tabs v-model="activeTab" v-loading="loading">
      <el-tab-pane label="当前预订" name="active">
        <div v-if="activeBookings.length === 0" class="empty-state">
          <el-empty description="暂无当前预订">
            <el-button type="primary" @click="goToBooking">立即预订</el-button>
          </el-empty>
        </div>
        <el-row v-else :gutter="20">
          <el-col
            v-for="booking in activeBookings"
            :key="booking.bookingId"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <el-card class="booking-card" shadow="hover">
              <template #header>
                <div class="booking-header">
                  <span class="booking-id">{{ booking.bookingId }}</span>
                  <el-tag :type="getStatusType(booking.status)" size="small">
                    {{ booking.status }}
                  </el-tag>
                </div>
              </template>

              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="滑板车">
                  {{ booking.scooterId }}
                </el-descriptions-item>
                <el-descriptions-item label="开始时间">
                  {{ formatDateTime(booking.startAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="结束时间">
                  {{ formatDateTime(booking.endAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="价格">
                  <strong>£{{ booking.priceBreakdown.finalPrice }}</strong>
                </el-descriptions-item>
              </el-descriptions>

              <div class="booking-actions">
                <el-button
                  type="danger"
                  size="small"
                  @click="handleCancel(booking)"
                >
                  取消预订
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="已取消" name="cancelled">
        <div v-if="cancelledBookings.length === 0" class="empty-state">
          <el-empty description="暂无已取消的预订" />
        </div>
        <el-row v-else :gutter="20">
          <el-col
            v-for="booking in cancelledBookings"
            :key="booking.bookingId"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <el-card class="booking-card cancelled" shadow="hover">
              <template #header>
                <div class="booking-header">
                  <span class="booking-id">{{ booking.bookingId }}</span>
                  <el-tag type="danger" size="small">已取消</el-tag>
                </div>
              </template>

              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="滑板车">
                  {{ booking.scooterId }}
                </el-descriptions-item>
                <el-descriptions-item label="开始时间">
                  {{ formatDateTime(booking.startAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="结束时间">
                  {{ formatDateTime(booking.endAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="价格">
                  £{{ booking.priceBreakdown.finalPrice }}
                </el-descriptions-item>
                <el-descriptions-item v-if="booking.cancelledAt" label="取消时间">
                  {{ formatDateTime(booking.cancelledAt) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row-tab-pane>
    </el-tabs>
      </el>

    <!-- 取消确认对话框 -->
    <el-dialog
      v-model="cancelDialogVisible"
      title="取消预订"
      width="400px"
    >
      <p>确定要取消预订 <strong>{{ selectedBooking?.bookingId }}</strong> 吗？</p>
      <el-input
        v-model="cancelReason"
        type="textarea"
        placeholder="请输入取消原因（可选）"
        :rows="3"
      />
      <template #footer>
        <el-button @click="cancelDialogVisible = false">关闭</el-button>
        <el-button type="danger" :loading="cancelling" @click="confirmCancel">
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { bookingApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const activeTab = ref('active')
const bookings = ref([])
const cancelDialogVisible = ref(false)
const selectedBooking = ref(null)
const cancelReason = ref('')
const cancelling = ref(false)

const activeBookings = computed(() =>
  bookings.value.filter(b => b.status === 'CONFIRMED')
)

const cancelledBookings = computed(() =>
  bookings.value.filter(b => b.status === 'CANCELLED')
)

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const statusMap = {
    'CONFIRMED': 'success',
    'CANCELLED': 'danger',
    'COMPLETED': 'info'
  }
  return statusMap[status] || 'info'
}

const loadBookings = () => {
  const stored = localStorage.getItem('myBookings')
  if (stored) {
    bookings.value = JSON.parse(stored)
  }
}

const saveBookings = () => {
  localStorage.setItem('myBookings', JSON.stringify(bookings.value))
}

const handleCancel = (booking) => {
  selectedBooking.value = booking
  cancelReason.value = ''
  cancelDialogVisible.value = true
}

const confirmCancel = async () => {
  if (!selectedBooking.value) return

  cancelling.value = true
  try {
    const response = await bookingApi.cancel(
      selectedBooking.value.bookingId,
      { reason: cancelReason.value }
    )

    const result = response.data.data

    const index = bookings.value.findIndex(
      b => b.bookingId === selectedBooking.value.bookingId
    )
    if (index !== -1) {
      bookings.value[index] = {
        ...bookings.value[index],
        status: result.status,
        cancelledAt: result.cancelledAt
      }
      saveBookings()
    }

    cancelDialogVisible.value = false
    ElMessage.success('预订已取消')
  } catch (error) {
    ElMessage.error(error.response?.data?.error?.message || '取消失败')
  } finally {
    cancelling.value = false
  }
}

const goToBooking = () => {
  router.push('/booking')
}

onMounted(() => {
  loadBookings()
})
</script>

<style scoped>
.my-bookings-container {
  padding: 24px;
  max-width: 1200px;
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

.empty-state {
  padding: 40px 0;
}

.booking-card {
  margin-bottom: 20px;
}

.booking-card.cancelled {
  opacity: 0.7;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.booking-id {
  font-weight: 600;
  color: #303133;
}

.booking-actions {
  margin-top: 16px;
  text-align: center;
}
</style>
