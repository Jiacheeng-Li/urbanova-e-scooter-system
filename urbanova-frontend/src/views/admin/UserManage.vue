<template>
  <div class="user-manage-container">
    <div class="page-header">
      <h1 class="page-title">User Management</h1>
      <el-button type="primary" @click="refreshData" :loading="loading">
        <el-icon><Refresh /></el-icon>
        Refresh
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="Role">
          <el-select 
            v-model="filterForm.role" 
            placeholder="All Roles" 
            clearable
            @change="handleFilterChange"
            style="width: 140px"
          >
            <el-option label="All" value="" />
            <el-option label="Customer" value="CUSTOMER" />
            <el-option label="Staff" value="STAFF" />
            <el-option label="Manager" value="MANAGER" />
          </el-select>
        </el-form-item>

        <el-form-item label="Account Status">
          <el-select 
            v-model="filterForm.accountStatus" 
            placeholder="All Status" 
            clearable
            @change="handleFilterChange"
            style="width: 140px"
          >
            <el-option label="All" value="" />
            <el-option label="Active" value="ACTIVE" />
            <el-option label="Suspended" value="SUSPENDED" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="fetchUsers">
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

    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table 
        :data="paginatedUsers" 
        v-loading="loading"
        stripe
        border
        @row-click="handleRowClick"
        style="cursor: pointer"
      >
        <el-table-column prop="fullName" label="User Name" min-width="150" />
        <el-table-column prop="email" label="Email" min-width="200" />
        <el-table-column prop="phone" label="Phone" width="140">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="Role" width="110">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)" size="small">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accountStatus" label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="row.accountStatus === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.accountStatus === 'ACTIVE' ? 'Active' : 'Suspended' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Discount" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">
              {{ getDiscountLabel(row.discountCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Registered" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="160" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click.stop="openDetailDialog(row)"
            >
              Detail
            </el-button>
            <el-button 
              v-if="row.accountStatus === 'ACTIVE'"
              type="danger" 
              link 
              @click.stop="openStatusDialog(row, 'SUSPENDED')"
            >
              Suspend
            </el-button>
            <el-button 
              v-else-if="row.accountStatus === 'SUSPENDED'"
              type="success" 
              link 
              @click.stop="openStatusDialog(row, 'ACTIVE')"
            >
              Activate
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
          :total="filteredUsers.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="`User Details - ${selectedUser?.fullName || ''}`"
      width="700px"
      @close="closeDetailDialog"
    >
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="selectedUser">
          <el-descriptions-item label="User ID" :span="2">
            <strong>{{ selectedUser.userId }}</strong>
          </el-descriptions-item>
          
          <el-descriptions-item label="Full Name">
            {{ selectedUser.fullName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Email">
            {{ selectedUser.email || '-' }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Phone">
            {{ selectedUser.phone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Role">
            <el-tag :type="getRoleType(selectedUser.role)" size="small">
              {{ getRoleLabel(selectedUser.role) }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="Account Status">
            <el-tag :type="selectedUser.accountStatus === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ selectedUser.accountStatus === 'ACTIVE' ? 'Active' : 'Suspended' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Discount Category">
            {{ getDiscountLabel(selectedUser.discountCategory) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Registered At">
            {{ formatDateTime(selectedUser.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="Last Updated">
            {{ formatDateTime(selectedUser.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 用户预订记录 -->
        <div class="section-title">
          <el-divider content-position="left">Booking History</el-divider>
          <el-table 
            :data="userBookings" 
            v-loading="loadingBookings"
            size="small"
            border
            max-height="300"
          >
            <el-table-column prop="bookingId" label="Booking ID" width="140" />
            <el-table-column prop="scooterId" label="Scooter" width="100" />
            <el-table-column prop="status" label="Status" width="110">
              <template #default="{ row }">
                <el-tag :type="getBookingStatusType(row.status)" size="small">
                  {{ getBookingStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Amount" width="100" align="right">
              <template #default="{ row }">
                £{{ formatNumber(row.priceFinal) }}
              </template>
            </el-table-column>
            <el-table-column label="Start Time" width="170">
              <template #default="{ row }">
                {{ formatDateTime(row.startAt) }}
              </template>
            </el-table-column>
            <el-table-column label="End Time" width="170">
              <template #default="{ row }">
                {{ formatDateTime(row.endAt) }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="userBookings.length === 0 && !loadingBookings" class="empty-text">
            No bookings found for this user
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">Close</el-button>
      </template>
    </el-dialog>

    <!-- 状态确认对话框 -->
    <el-dialog
      v-model="statusDialogVisible"
      :title="statusDialogTitle"
      width="400px"
    >
      <p>
        Are you sure you want to 
        <strong>{{ statusDialogAction === 'ACTIVE' ? 'activate' : 'suspend' }}</strong> 
        user <strong>{{ selectedUser?.fullName }}</strong>?
      </p>
      <p v-if="statusDialogAction === 'SUSPENDED'" class="warning-text">
        Suspended users cannot log in or make new bookings.
      </p>
      <p v-else class="success-text">
        Activated users can log in and make bookings.
      </p>

      <template #footer>
        <el-button @click="statusDialogVisible = false">Cancel</el-button>
        <el-button 
          :type="statusDialogAction === 'ACTIVE' ? 'success' : 'danger'" 
          :loading="updatingStatus"
          @click="confirmStatusUpdate"
        >
          {{ statusDialogAction === 'ACTIVE' ? 'Activate User' : 'Suspend User' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { adminUsersApi } from '../../api'

const loading = ref(false)
const loadingDetail = ref(false)
const loadingBookings = ref(false)
const updatingStatus = ref(false)
const users = ref([])
const selectedUser = ref(null)
const userBookings = ref([])
const detailDialogVisible = ref(false)
const statusDialogVisible = ref(false)
const statusDialogAction = ref('')
const statusDialogTitle = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)

// 筛选条件
const filterForm = ref({
  role: '',
  accountStatus: ''
})

// 过滤后的用户列表
const filteredUsers = computed(() => {
  let result = [...users.value]
  
  if (filterForm.value.role) {
    result = result.filter(u => u.role === filterForm.value.role)
  }
  if (filterForm.value.accountStatus) {
    result = result.filter(u => u.accountStatus === filterForm.value.accountStatus)
  }
  
  return result
})

// 分页显示
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredUsers.value.slice(start, end)
})

// 辅助函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const formatNumber = (num) => {
  if (num === undefined || num === null) return '0.00'
  return Number(num).toFixed(2)
}

const getRoleType = (role) => {
  const typeMap = {
    'CUSTOMER': 'success',
    'STAFF': 'warning',
    'MANAGER': 'danger'
  }
  return typeMap[role] || 'info'
}

const getRoleLabel = (role) => {
  const labelMap = {
    'CUSTOMER': 'Customer',
    'STAFF': 'Staff',
    'MANAGER': 'Manager'
  }
  return labelMap[role] || role
}

const getDiscountLabel = (category) => {
  const labelMap = {
    'NONE': 'None',
    'FREQUENT_USER': 'Frequent',
    'STUDENT': 'Student',
    'SENIOR': 'Senior'
  }
  return labelMap[category] || category || 'None'
}

const getBookingStatusType = (status) => {
  const typeMap = {
    'PENDING_PAYMENT': 'warning',
    'CONFIRMED': 'success',
    'ACTIVE': 'primary',
    'COMPLETED': 'info',
    'CANCELLED': 'danger'
  }
  return typeMap[status] || 'info'
}

const getBookingStatusLabel = (status) => {
  const labelMap = {
    'PENDING_PAYMENT': 'Pending',
    'CONFIRMED': 'Confirmed',
    'ACTIVE': 'Active',
    'COMPLETED': 'Completed',
    'CANCELLED': 'Cancelled'
  }
  return labelMap[status] || status
}

// API 调用
const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await adminUsersApi.list(
      filterForm.value.role || null,
      filterForm.value.accountStatus || null
    )
    users.value = response.data.data || []
    currentPage.value = 1
  } catch (error) {
    ElMessage.error('Failed to fetch users')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchUserDetail = async (userId) => {
  loadingDetail.value = true
  try {
    const response = await adminUsersApi.getUser(userId)
    selectedUser.value = response.data.data
  } catch (error) {
    ElMessage.error('Failed to fetch user details')
    console.error(error)
  } finally {
    loadingDetail.value = false
  }
}

const fetchUserBookings = async (userId) => {
  loadingBookings.value = true
  try {
    const response = await adminUsersApi.getUserBookings(userId)
    userBookings.value = response.data.data || []
  } catch (error) {
    console.error('Failed to fetch user bookings:', error)
    userBookings.value = []
  } finally {
    loadingBookings.value = false
  }
}

const updateUserStatus = async (userId, accountStatus) => {
  updatingStatus.value = true
  try {
    await adminUsersApi.updateStatus(userId, accountStatus)
    ElMessage.success(`User ${accountStatus === 'ACTIVE' ? 'activated' : 'suspended'} successfully`)
    
    // 刷新列表
    await fetchUsers()
    
    // 如果详情对话框打开，也刷新详情
    if (detailDialogVisible.value && selectedUser.value) {
      await fetchUserDetail(userId)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Failed to update user status'
    ElMessage.error(errorMsg)
  } finally {
    updatingStatus.value = false
  }
}

// 事件处理
const resetFilter = () => {
  filterForm.value = {
    role: '',
    accountStatus: ''
  }
  fetchUsers()
}

const refreshData = () => {
  fetchUsers()
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchUsers()
}

const handleRowClick = (row) => {
  openDetailDialog(row)
}

const openDetailDialog = async (row) => {
  selectedUser.value = { ...row }
  detailDialogVisible.value = true
  
  // 并行获取用户详情和预订记录
  await Promise.all([
    fetchUserDetail(row.userId),
    fetchUserBookings(row.userId)
  ])
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
  selectedUser.value = null
  userBookings.value = []
}

const openStatusDialog = (row, action) => {
  selectedUser.value = row
  statusDialogAction.value = action  // action 是 'ACTIVE' 或 'SUSPENDED'
  statusDialogTitle.value = action === 'ACTIVE' ? 'Activate User' : 'Suspend User'
  statusDialogVisible.value = true
}

const confirmStatusUpdate = async () => {
  if (!selectedUser.value) return
  
  await updateUserStatus(selectedUser.value.userId, statusDialogAction.value)
  statusDialogVisible.value = false
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => {
  currentPage.value = val
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-manage-container {
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

.warning-text {
  color: #e6a23c;
  font-size: 13px;
  margin-top: 8px;
}

.success-text {
  color: #67c23a;
  font-size: 13px;
  margin-top: 8px;
}

.empty-text {
  text-align: center;
  padding: 20px;
  color: #909399;
}
</style>