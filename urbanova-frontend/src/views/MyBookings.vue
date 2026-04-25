<template>
  <div class="my-bookings-container">
    <h1 class="page-title">My Bookings</h1>
    <p class="page-description">View and manage your scooter bookings</p>

    <el-tabs v-model="activeTab" v-loading="loading">
      <el-tab-pane label="Active Bookings" name="active">
        <div v-if="activeBookings.length === 0" class="empty-state">
          <el-empty description="No active bookings">
            <el-button type="primary" @click="goToBooking">Book Now</el-button>
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
                <el-descriptions-item label="Scooter">
                  {{ booking.scooterId }}
                </el-descriptions-item>
                <el-descriptions-item label="Start Time">
                  {{ formatDateTime(booking.startAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="End Time">
                  {{ formatDateTime(booking.endAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="Price">
                  <strong>£{{ booking.priceBreakdown.finalPrice }}</strong>
                </el-descriptions-item>
              </el-descriptions>

              <div class="booking-actions">
                <el-button
                  type="danger"
                  size="small"
                  @click="handleCancel(booking)"
                >
                  Cancel Booking
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="Cancelled" name="cancelled">
        <div v-if="cancelledBookings.length === 0" class="empty-state">
          <el-empty description="No cancelled bookings" />
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
                  <el-tag type="danger" size="small">Cancelled</el-tag>
                </div>
              </template>

              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="Scooter">
                  {{ booking.scooterId }}
                </el-descriptions-item>
                <el-descriptions-item label="Start Time">
                  {{ formatDateTime(booking.startAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="End Time">
                  {{ formatDateTime(booking.endAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="Price">
                  £{{ booking.priceBreakdown.finalPrice }}
                </el-descriptions-item>
                <el-descriptions-item v-if="booking.cancelledAt" label="Cancelled At">
                  {{ formatDateTime(booking.cancelledAt) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 取消确认对话框 -->
    <el-dialog
      v-model="cancelDialogVisible"
      title="Cancel Booking"
      width="400px"
    >
      <p>Are you sure you want to cancel booking <strong>{{ selectedBooking?.bookingId }}</strong>?</p>
      <el-input
        v-model="cancelReason"
        type="textarea"
        placeholder="Please enter cancellation reason (optional)"
        :rows="3"
      />
      <template #footer>
        <el-button @click="cancelDialogVisible = false">Close</el-button>
        <el-button type="danger" :loading="cancelling" @click="confirmCancel">
          Confirm Cancel
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
  return new Date(dateStr).toLocaleString()
}

const getStatusType = (status) => {
  const statusMap = {
    'CONFIRMED': 'success',
    'CANCELLED': 'danger',
    'COMPLETED': 'info'
  }
  return statusMap[status] || 'info'
}

const loadBookings = async () => {
  loading.value = true
  try {
    const response = await bookingApi.list()
    bookings.value = response.data.data || []
  } catch (error) {
    // 使用本地存储的预订作为后备
    const localBookings = localStorage.getItem('myBookings')
    if (localBookings) {
      bookings.value = JSON.parse(localBookings)
    } else {
      bookings.value = []
    }
    console.error('Failed to fetch bookings:', error)
  } finally {
    loading.value = false
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

    cancelDialogVisible.value = false
    ElMessage.success('Booking cancelled')

    // 重新加载预订列表
    await loadBookings()
  } catch (error) {
    ElMessage.error(error.response?.data?.error?.message || 'Cancellation failed')
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
