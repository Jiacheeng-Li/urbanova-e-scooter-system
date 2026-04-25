<template>
  <div class="booking-manage-container">
    <div class="page-header">
      <h1 class="page-title">Booking Management</h1>
      <div class="header-actions">
        <el-button type="success" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          Create Guest Booking
        </el-button>
        <el-button type="primary" @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="Booking Status">
          <el-select 
            v-model="filterForm.status" 
            placeholder="All Status" 
            clearable
            @change="handleFilterChange"
            style="width: 150px"
          >
            <el-option label="All" value="" />
            <el-option label="Pending Payment" value="PENDING_PAYMENT" />
            <el-option label="Confirmed" value="CONFIRMED" />
            <el-option label="Active" value="ACTIVE" />
            <el-option label="Completed" value="COMPLETED" />
            <el-option label="Cancelled" value="CANCELLED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Payment Status">
          <el-select 
            v-model="filterForm.paymentStatus" 
            placeholder="All Payment" 
            clearable
            @change="handleFilterChange"
            style="width: 150px"
          >
            <el-option label="All" value="" />
            <el-option label="Unpaid" value="UNPAID" />
            <el-option label="Paid" value="PAID" />
            <el-option label="Refunded" value="REFUNDED" />
            <el-option label="Partially Refunded" value="PARTIALLY_REFUNDED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Customer Type">
          <el-select 
            v-model="filterForm.customerType" 
            placeholder="All Types" 
            clearable
            @change="handleFilterChange"
            style="width: 150px"
          >
            <el-option label="All" value="" />
            <el-option label="Registered" value="REGISTERED" />
            <el-option label="Guest" value="GUEST" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="fetchBookings">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            Reset
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预订列表 -->
    <el-card class="table-card">
      <el-table 
        :data="paginatedBookings" 
        v-loading="loading"
        stripe
        border
        @row-click="handleRowClick"
        style="cursor: pointer"
      >
        <el-table-column prop="bookingId" label="Booking ID" width="140" />
        <el-table-column label="Customer" min-width="180">
          <template #default="{ row }">
            <div v-if="row._userLoading" class="loading-cell">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>Loading...</span>
            </div>
            <div v-else>
              <div class="customer-name">
                <strong>{{ getDisplayName(row) }}</strong>
              </div>
              <div class="customer-contact" v-if="getDisplayEmail(row)">
                {{ getDisplayEmail(row) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="customerType" label="Type" width="100">
          <template #default="{ row }">
            <el-tag :type="row.customerType === 'REGISTERED' ? 'success' : 'warning'" size="small">
              {{ row.customerType === 'REGISTERED' ? 'Registered' : 'Guest' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scooterId" label="Scooter ID" width="120" />
        <el-table-column label="Hire Option" width="100">
          <template #default="{ row }">
            {{ row.hireOptionId?.replace('HIRE-', '') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Payment" width="120">
          <template #default="{ row }">
            <el-tag :type="getPaymentStatusType(row.paymentStatus)">
              {{ getPaymentStatusLabel(row.paymentStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Amount" width="110" align="right">
          <template #default="{ row }">
            <strong>£{{ formatNumber(row.priceFinal) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="Start Time" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.startAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="140" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click.stop="openDetailDialog(row)"
            >
              Detail
            </el-button>
            <el-button 
              type="warning" 
              link 
              @click.stop="openOverrideDialog(row)"
              :disabled="row.status === 'COMPLETED' || row.status === 'CANCELLED'"
            >
              Override
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredBookings.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 预订详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="Booking Details"
      width="850px"
      @close="closeDetailDialog"
    >
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="selectedBooking">
          <el-descriptions-item label="Booking ID" :span="2">
            <strong>{{ selectedBooking.bookingId }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="Booking Ref" v-if="selectedBooking.bookingRef" :span="2">
            {{ selectedBooking.bookingRef }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Customer Type" label-span="1">
            <el-tag :type="selectedBooking.customerType === 'REGISTERED' ? 'success' : 'warning'" size="small">
              {{ selectedBooking.customerType === 'REGISTERED' ? 'Registered User' : 'Guest' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Customer Name" label-span="1">
            <strong>{{ getDisplayName(selectedBooking) }}</strong>
          </el-descriptions-item>
          
          <el-descriptions-item label="Email" v-if="getDisplayEmail(selectedBooking)" :span="1">
            {{ getDisplayEmail(selectedBooking) }}
          </el-descriptions-item>
          <el-descriptions-item label="Phone" v-if="getDisplayPhone(selectedBooking)" :span="1">
            {{ getDisplayPhone(selectedBooking) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Scooter ID">
            {{ selectedBooking.scooterId }}
          </el-descriptions-item>
          <el-descriptions-item label="Hire Option">
            {{ selectedBooking.hireOptionId?.replace('HIRE-', '') || '-' }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Booking Status">
            <el-tag :type="getStatusType(selectedBooking.status)">
              {{ getStatusLabel(selectedBooking.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Payment Status">
            <el-tag :type="getPaymentStatusType(selectedBooking.paymentStatus)">
              {{ getPaymentStatusLabel(selectedBooking.paymentStatus) }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="Planned Start">
            {{ formatDateTime(selectedBooking.startAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="Planned End">
            {{ formatDateTime(selectedBooking.endAt) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Actual Start" v-if="selectedBooking.actualStartAt">
            {{ formatDateTime(selectedBooking.actualStartAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="Actual End" v-if="selectedBooking.actualEndAt">
            {{ formatDateTime(selectedBooking.actualEndAt) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Created At">
            {{ formatDateTime(selectedBooking.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="Created By">
            {{ selectedBooking.createdByRole || '-' }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Base Price">
            £{{ formatNumber(selectedBooking.priceBase) }}
          </el-descriptions-item>
          <el-descriptions-item label="Discount">
            -£{{ formatNumber(selectedBooking.priceDiscount) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Final Price" :span="2">
            <strong style="color: #67c23a; font-size: 18px;">£{{ formatNumber(selectedBooking.priceFinal) }}</strong>
          </el-descriptions-item>
          
          <el-descriptions-item label="Cancel Reason" v-if="selectedBooking.cancelReason" :span="2">
            <span style="color: #f56c6c;">{{ selectedBooking.cancelReason }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedBooking?.payments?.length" class="section-title">
          <el-divider content-position="left">Payment History</el-divider>
          <el-table :data="selectedBooking.payments" size="small" border>
            <el-table-column prop="paymentId" label="Payment ID" width="140" />
            <el-table-column prop="amount" label="Amount" width="100" align="right">
              <template #default="{ row }">£{{ formatNumber(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="Status" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="method" label="Method" width="120" />
            <el-table-column prop="createdAt" label="Created At" width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="selectedBooking?.timeline?.length" class="section-title">
          <el-divider content-position="left">Event Timeline</el-divider>
          <el-timeline>
            <el-timeline-item
              v-for="event in selectedBooking.timeline"
              :key="event.eventId || event.id"
              :timestamp="formatDateTime(event.createdAt)"
              :type="getEventType(event.eventType)"
            >
              <strong>{{ event.eventType }}</strong> - {{ event.description || event.message }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">Close</el-button>
      </template>
    </el-dialog>

    <!-- 经理覆盖对话框 -->
    <el-dialog
      v-model="overrideDialogVisible"
      title="Override Booking"
      width="500px"
    >
      <el-form :model="overrideForm" label-width="120px">
        <el-form-item label="Booking ID">
          <span>{{ selectedBooking?.bookingId }}</span>
        </el-form-item>
        
        <el-form-item label="Current Status">
          <el-tag :type="getStatusType(selectedBooking?.status)">
            {{ getStatusLabel(selectedBooking?.status) }}
          </el-tag>
        </el-form-item>

        <el-form-item label="Current Payment">
          <el-tag :type="getPaymentStatusType(selectedBooking?.paymentStatus)">
            {{ getPaymentStatusLabel(selectedBooking?.paymentStatus) }}
          </el-tag>
        </el-form-item>

        <el-divider />

        <el-form-item label="Override Status">
          <el-select v-model="overrideForm.status" placeholder="Select new status" clearable style="width: 100%">
            <el-option label="Pending Payment" value="PENDING_PAYMENT" />
            <el-option label="Confirmed" value="CONFIRMED" />
            <el-option label="Active" value="ACTIVE" />
            <el-option label="Completed" value="COMPLETED" />
            <el-option label="Cancelled" value="CANCELLED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Override Payment">
          <el-select v-model="overrideForm.paymentStatus" placeholder="Select new payment status" clearable style="width: 100%">
            <el-option label="Unpaid" value="UNPAID" />
            <el-option label="Paid" value="PAID" />
            <el-option label="Refunded" value="REFUNDED" />
            <el-option label="Partially Refunded" value="PARTIALLY_REFUNDED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Scooter ID">
          <el-input v-model="overrideForm.scooterId" placeholder="Change scooter ID" />
        </el-form-item>

        <el-form-item label="Cancel Reason" v-if="overrideForm.status === 'CANCELLED'">
          <el-input
            v-model="overrideForm.cancelReason"
            type="textarea"
            placeholder="Enter cancellation reason"
            :rows="3"
          />
        </el-form-item>
      </el-form>

      <el-alert
        title="Warning"
        description="Override operations bypass normal business rules. Please use with caution."
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <template #footer>
        <el-button @click="overrideDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmOverride">
          Confirm Override
        </el-button>
      </template>
    </el-dialog>

    <!-- 创建游客预订对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="Create Guest Booking"
      width="550px"
      @close="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createFormRules"
        label-width="120px"
        label-position="right"
      >
        <el-divider content-position="left">Guest Information</el-divider>
        
        <el-form-item label="Guest Name" prop="guestName" required>
          <el-input 
            v-model="createForm.guestName" 
            placeholder="Enter guest full name"
            :disabled="creating"
          />
        </el-form-item>

        <el-form-item label="Email" prop="guestEmail">
          <el-input 
            v-model="createForm.guestEmail" 
            placeholder="guest@example.com"
            :disabled="creating"
          />
          <div class="form-tip">Optional, for sending confirmation</div>
        </el-form-item>

        <el-form-item label="Phone" prop="guestPhone">
          <el-input 
            v-model="createForm.guestPhone" 
            placeholder="Enter phone number"
            :disabled="creating"
          />
          <div class="form-tip">Optional, for contact</div>
        </el-form-item>

        <el-divider content-position="left">Booking Information</el-divider>

        <el-form-item label="Scooter" prop="scooterId" required>
          <el-select
            v-model="createForm.scooterId"
            placeholder="Select available scooter"
            style="width: 100%"
            filterable
            :disabled="creating"
            @change="handleScooterChange"
          >
            <el-option
              v-for="scooter in availableScooters"
              :key="scooter.scooterId"
              :label="`${scooter.scooterId} - ${scooter.typeCode || 'Standard'} (${scooter.batteryPercent || 0}%)`"
              :value="scooter.scooterId"
            />
          </el-select>
          <div class="form-tip" v-if="selectedScooter">
            Battery: {{ selectedScooter.batteryPercent }}% | 
            Zone: {{ selectedScooter.zone || 'N/A' }}
          </div>
        </el-form-item>

        <el-form-item label="Hire Option" prop="hireOptionId" required>
          <el-select
            v-model="createForm.hireOptionId"
            placeholder="Select hire option"
            style="width: 100%"
            :disabled="creating"
            @change="handleHireOptionChange"
          >
            <el-option
              v-for="option in hireOptions"
              :key="option.hireOptionId"
              :label="`${option.code} - ${formatDuration(option.durationMinutes)} - £${option.basePrice.toFixed(2)}`"
              :value="option.hireOptionId"
            />
          </el-select>
          <div class="form-tip" v-if="selectedHireOption">
            Duration: {{ formatDuration(selectedHireOption.durationMinutes) }} | 
            Base Price: £{{ selectedHireOption.basePrice.toFixed(2) }}
          </div>
        </el-form-item>

        <el-form-item label="Start Time" prop="plannedStartAt" required>
          <el-date-picker
            v-model="createForm.plannedStartAt"
            type="datetime"
            placeholder="Select start time"
            style="width: 100%"
            :disabled-date="disabledDate"
            :disabled="creating"
          />
          <div class="form-tip">Select a future start time (at least 15 minutes from now)</div>
        </el-form-item>

        <div v-if="pricePreview" class="price-preview">
          <el-divider content-position="left">Price Preview</el-divider>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="Base Price">
              £{{ formatNumber(pricePreview.basePrice || 0) }}
            </el-descriptions-item>
            <el-descriptions-item label="Discount">
              -£{{ formatNumber(pricePreview.discount || 0) }}
            </el-descriptions-item>
            <el-descriptions-item label="Final Price" label-span="2">
              <strong style="color: #67c23a; font-size: 16px;">£{{ formatNumber(pricePreview.finalPrice || 0) }}</strong>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-form>

      <el-alert
        title="Note"
        description="Guest bookings will be created with PENDING_PAYMENT status. The guest will need to complete payment to confirm the booking."
        type="info"
        show-icon
        :closable="false"
        style="margin: 16px 0"
      />

      <template #footer>
        <el-button @click="createDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateGuestBooking">
          Create Booking
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Loading, Plus } from '@element-plus/icons-vue'
import { adminBookingsApi, adminUsersApi, staffBookingsApi, hireOptionsApi, scooterApi } from '../../api'

const loading = ref(false)
const loadingDetail = ref(false)
const submitting = ref(false)
const creating = ref(false)
const bookings = ref([])
const enrichedBookings = ref([])
const selectedBooking = ref(null)
const detailDialogVisible = ref(false)
const overrideDialogVisible = ref(false)
const createDialogVisible = ref(false)

const createFormRef = ref(null)
const hireOptions = ref([])
const availableScooters = ref([])
const selectedScooter = ref(null)
const selectedHireOption = ref(null)
const pricePreview = ref(null)

const createForm = ref({
  guestName: '',
  guestEmail: '',
  guestPhone: '',
  scooterId: '',
  hireOptionId: '',
  plannedStartAt: ''
})

const createFormRules = {
  guestName: [
    { required: true, message: 'Please enter guest name', trigger: 'blur' },
    { min: 2, max: 100, message: 'Name must be between 2 and 100 characters', trigger: 'blur' }
  ],
  guestEmail: [
    { type: 'email', message: 'Please enter a valid email address', trigger: 'blur' }
  ],
  scooterId: [
    { required: true, message: 'Please select a scooter', trigger: 'change' }
  ],
  hireOptionId: [
    { required: true, message: 'Please select a hire option', trigger: 'change' }
  ],
  plannedStartAt: [
    { required: true, message: 'Please select start time', trigger: 'change' }
  ]
}

const userCache = new Map()
const loadingUsers = new Set()

const currentPage = ref(1)
const pageSize = ref(20)

const filterForm = ref({
  status: '',
  paymentStatus: '',
  customerType: ''
})

const overrideForm = ref({
  status: '',
  paymentStatus: '',
  scooterId: '',
  cancelReason: ''
})

// ==================== 工具函数 ====================

// 格式化时间为后端期望的格式 (YYYY-MM-DDTHH:MM:SS)
const formatDateTimeForBackend = (date) => {
  if (!date) return null
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
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

const formatNumber = (num) => {
  if (num === undefined || num === null) return '0.00'
  return Number(num).toFixed(2)
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const disabledDate = (time) => {
  const minTime = Date.now() - 15 * 60 * 1000
  return time.getTime() < minTime
}

// ==================== 创建订单相关 ====================

const fetchHireOptions = async () => {
  try {
    const response = await hireOptionsApi.list()
    hireOptions.value = response.data.data || []
  } catch (error) {
    console.error('Failed to fetch hire options:', error)
    ElMessage.error('Failed to fetch hire options')
  }
}

const fetchAvailableScooters = async () => {
  try {
    const response = await scooterApi.getScooterIdsByStatus('AVAILABLE')
    const scooterIds = response.data.data.scooterIds || []
    
    const scooterDetails = await Promise.all(
      scooterIds.map(async (id) => {
        try {
          const detailRes = await scooterApi.getDetail(id)
          return detailRes.data.data
        } catch {
          return { scooterId: id }
        }
      })
    )
    availableScooters.value = scooterDetails
  } catch (error) {
    console.error('Failed to fetch available scooters:', error)
    ElMessage.error('Failed to fetch available scooters')
  }
}

const handleScooterChange = (scooterId) => {
  selectedScooter.value = availableScooters.value.find(s => s.scooterId === scooterId)
  fetchPricePreview()
}

const handleHireOptionChange = (hireOptionId) => {
  selectedHireOption.value = hireOptions.value.find(h => h.hireOptionId === hireOptionId)
  fetchPricePreview()
}

const fetchPricePreview = async () => {
  if (!createForm.value.scooterId || !createForm.value.hireOptionId) {
    pricePreview.value = null
    return
  }
  
  try {
    const selectedOption = hireOptions.value.find(h => h.hireOptionId === createForm.value.hireOptionId)
    if (selectedOption) {
      pricePreview.value = {
        basePrice: selectedOption.basePrice,
        discount: 0,
        finalPrice: selectedOption.basePrice
      }
    }
  } catch (error) {
    console.error('Failed to get price preview:', error)
  }
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = {
    guestName: '',
    guestEmail: '',
    guestPhone: '',
    scooterId: '',
    hireOptionId: '',
    plannedStartAt: ''
  }
  selectedScooter.value = null
  selectedHireOption.value = null
  pricePreview.value = null
  if (createFormRef.value) {
    createFormRef.value.clearValidate()
  }
}

const handleCreateGuestBooking = async () => {
  if (!createFormRef.value) return
  
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    creating.value = true
    try {
      // 关键修复：将时间格式化为后端期望的格式
      const formattedStartAt = formatDateTimeForBackend(createForm.value.plannedStartAt)
      
      const requestData = {
        guestName: createForm.value.guestName,
        guestEmail: createForm.value.guestEmail || null,
        guestPhone: createForm.value.guestPhone || null,
        scooterId: createForm.value.scooterId,
        hireOptionId: createForm.value.hireOptionId,
        plannedStartAt: formattedStartAt
      }
      
      console.log('Creating guest booking with data:', requestData)
      
      const response = await staffBookingsApi.createGuestBooking(requestData)
      
      ElMessage.success('Guest booking created successfully!')
      createDialogVisible.value = false
      
      await fetchBookings()
      
      if (response.data.data?.bookingId) {
        ElMessage.info(`Booking ID: ${response.data.data.bookingId}`)
      }
    } catch (error) {
      console.error('Create booking error:', error)
      const errorMsg = error.response?.data?.error?.message || 'Failed to create guest booking'
      ElMessage.error(errorMsg)
    } finally {
      creating.value = false
    }
  })
}

// ==================== 用户信息获取函数 ====================

const fetchUserInfo = async (userId) => {
  if (userCache.has(userId)) {
    return userCache.get(userId)
  }
  
  if (loadingUsers.has(userId)) {
    let retries = 0
    while (loadingUsers.has(userId) && retries < 50) {
      await new Promise(resolve => setTimeout(resolve, 100))
      retries++
    }
    return userCache.get(userId) || null
  }
  
  loadingUsers.add(userId)
  try {
    const response = await adminUsersApi.getUser(userId)
    const userData = response.data.data
    userCache.set(userId, userData)
    return userData
  } catch (error) {
    console.error(`Failed to fetch user ${userId}:`, error)
    const errorInfo = {
      _fetchSuccess: false,
      _fetchError: error.response?.data?.error?.message || error.message || 'Failed to fetch user',
      _statusCode: error.response?.status
    }
    userCache.set(userId, errorInfo)
    return errorInfo
  } finally {
    loadingUsers.delete(userId)
  }
}

const getDisplayName = (row) => {
  if (row.customerType === 'GUEST') {
    return row.guestName || 'Guest User'
  }
  
  const userInfo = row._userInfo
  
  if (userInfo && userInfo._fetchSuccess === false) {
    if (userInfo._statusCode === 404) {
      return `[Deleted User] ${row.userId?.substring(0, 8)}...`
    }
    return `[Error] ${row.userId?.substring(0, 8)}...`
  }
  
  if (userInfo?.fullName) {
    return userInfo.fullName
  }
  
  if (row._userLoading) {
    return 'Loading...'
  }
  
  if (row.userId) {
    return `User: ${row.userId.substring(0, 8)}...`
  }
  
  return 'Unknown User'
}

const getDisplayEmail = (row) => {
  if (row.customerType === 'GUEST') {
    return row.guestEmail || null
  }
  return row._userInfo?.email || row.userEmail || null
}

const getDisplayPhone = (row) => {
  if (row.customerType === 'GUEST') {
    return row.guestPhone || null
  }
  return row._userInfo?.phone || row.userPhone || null
}

// ==================== 筛选和分页 ====================

const filteredBookings = computed(() => {
  let result = [...enrichedBookings.value]
  
  if (filterForm.value.status) {
    result = result.filter(b => b.status === filterForm.value.status)
  }
  if (filterForm.value.paymentStatus) {
    result = result.filter(b => b.paymentStatus === filterForm.value.paymentStatus)
  }
  if (filterForm.value.customerType) {
    result = result.filter(b => b.customerType === filterForm.value.customerType)
  }
  
  return result
})

const paginatedBookings = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredBookings.value.slice(start, end)
})

// ==================== 状态标签辅助函数 ====================

const getStatusType = (status) => {
  const statusMap = {
    'PENDING_PAYMENT': 'warning',
    'CONFIRMED': 'success',
    'ACTIVE': 'primary',
    'COMPLETED': 'info',
    'CANCELLED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const statusMap = {
    'PENDING_PAYMENT': 'Pending Payment',
    'CONFIRMED': 'Confirmed',
    'ACTIVE': 'Active',
    'COMPLETED': 'Completed',
    'CANCELLED': 'Cancelled'
  }
  return statusMap[status] || status
}

const getPaymentStatusType = (status) => {
  const statusMap = {
    'UNPAID': 'danger',
    'PAID': 'success',
    'REFUNDED': 'warning',
    'PARTIALLY_REFUNDED': 'info'
  }
  return statusMap[status] || 'info'
}

const getPaymentStatusLabel = (status) => {
  const statusMap = {
    'UNPAID': 'Unpaid',
    'PAID': 'Paid',
    'REFUNDED': 'Refunded',
    'PARTIALLY_REFUNDED': 'Partially Refunded'
  }
  return statusMap[status] || status
}

const getEventType = (eventType) => {
  const typeMap = {
    'BOOKING_CREATED': 'primary',
    'PAYMENT_RECEIVED': 'success',
    'BOOKING_STARTED': 'primary',
    'BOOKING_ENDED': 'info',
    'BOOKING_CANCELLED': 'danger',
    'BOOKING_EXTENDED': 'warning',
    'OVERRIDE_APPLIED': 'warning'
  }
  return typeMap[eventType] || 'info'
}

// ==================== API 调用 ====================

const fetchBookings = async () => {
  loading.value = true
  try {
    const response = await adminBookingsApi.list(
      filterForm.value.status || null,
      filterForm.value.paymentStatus || null,
      filterForm.value.customerType || null
    )
    const rawBookings = response.data.data || []
    
    const bookingsWithLoading = rawBookings.map(booking => ({
      ...booking,
      _userLoading: booking.customerType === 'REGISTERED' && booking.userId && !userCache.has(booking.userId),
      _userInfo: userCache.get(booking.userId) || null
    }))
    
    enrichedBookings.value = bookingsWithLoading
    currentPage.value = 1
    
    loadUserInfoAsync(rawBookings)
  } catch (error) {
    ElMessage.error('Failed to fetch bookings')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadUserInfoAsync = async (bookingList) => {
  const userIdsToFetch = [...new Set(
    bookingList
      .filter(b => b.customerType === 'REGISTERED' && b.userId && !userCache.has(b.userId))
      .map(b => b.userId)
  )]
  
  if (userIdsToFetch.length === 0) return
  
  const promises = userIdsToFetch.map(async (userId) => {
    try {
      const userInfo = await fetchUserInfo(userId)
      return { userId, userInfo }
    } catch (error) {
      return { userId, userInfo: null }
    }
  })
  
  const results = await Promise.all(promises)
  
  enrichedBookings.value = enrichedBookings.value.map(booking => {
    if (booking.customerType === 'REGISTERED' && booking.userId) {
      const result = results.find(r => r.userId === booking.userId)
      if (result) {
        return {
          ...booking,
          _userLoading: false,
          _userInfo: result.userInfo
        }
      }
    }
    return {
      ...booking,
      _userLoading: false
    }
  })
}

const fetchBookingDetail = async (bookingId) => {
  loadingDetail.value = true
  try {
    const response = await adminBookingsApi.getDetail(bookingId)
    const detailData = response.data.data
    
    if (detailData.customerType === 'REGISTERED' && detailData.userId) {
      const userInfo = await fetchUserInfo(detailData.userId)
      detailData._userInfo = userInfo
    }
    
    selectedBooking.value = detailData
  } catch (error) {
    ElMessage.error('Failed to fetch booking details')
    console.error(error)
  } finally {
    loadingDetail.value = false
  }
}

const resetFilter = () => {
  filterForm.value = {
    status: '',
    paymentStatus: '',
    customerType: ''
  }
  fetchBookings()
}

const refreshData = () => {
  fetchBookings()
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchBookings()
}

const handleRowClick = (row) => {
  openDetailDialog(row)
}

const openDetailDialog = async (row) => {
  selectedBooking.value = { ...row }
  detailDialogVisible.value = true
  await fetchBookingDetail(row.bookingId)
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
  selectedBooking.value = null
}

const openOverrideDialog = (row) => {
  selectedBooking.value = row
  overrideForm.value = {
    status: '',
    paymentStatus: '',
    scooterId: '',
    cancelReason: ''
  }
  overrideDialogVisible.value = true
}

const confirmOverride = async () => {
  const requestData = {}
  if (overrideForm.value.status) requestData.status = overrideForm.value.status
  if (overrideForm.value.paymentStatus) requestData.paymentStatus = overrideForm.value.paymentStatus
  if (overrideForm.value.scooterId) requestData.scooterId = overrideForm.value.scooterId
  if (overrideForm.value.cancelReason) requestData.cancelReason = overrideForm.value.cancelReason

  if (Object.keys(requestData).length === 0) {
    ElMessage.warning('Please select at least one field to override')
    return
  }

  try {
    await ElMessageBox.confirm(
      'Are you sure you want to override this booking? This action may affect booking business logic.',
      'Confirm Override',
      {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
    )

    submitting.value = true
    await adminBookingsApi.override(selectedBooking.value.bookingId, requestData)
    
    ElMessage.success('Booking overridden successfully')
    overrideDialogVisible.value = false
    await fetchBookings()
    
    if (detailDialogVisible.value && selectedBooking.value) {
      await fetchBookingDetail(selectedBooking.value.bookingId)
    }
  } catch (error) {
    if (error !== 'cancel') {
      const errorMsg = error.response?.data?.error?.message || 'Override failed'
      ElMessage.error(errorMsg)
    }
  } finally {
    submitting.value = false
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => {
  currentPage.value = val
}

onMounted(() => {
  fetchBookings()
  fetchHireOptions()
  fetchAvailableScooters()
})

onUnmounted(() => {
  // 可选：清除缓存
})
</script>

<style scoped>
.booking-manage-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.table-card {
  border-radius: 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.section-title {
  margin-top: 20px;
}

.el-table__row:hover {
  background-color: #f5f7fa;
  cursor: pointer;
}

.customer-name {
  font-weight: 500;
  color: #303133;
}

.customer-contact {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.loading-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.price-preview {
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .el-table__body-wrapper {
    overflow-x: auto;
  }
}
</style>