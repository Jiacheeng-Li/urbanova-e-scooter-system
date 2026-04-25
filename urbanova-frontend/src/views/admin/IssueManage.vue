<template>
  <div class="issue-manage-container">
    <div class="page-header">
      <h1 class="page-title">Issue Management</h1>
      <div class="header-actions">
        <el-button type="success" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          Create Issue
        </el-button>
        <el-button type="warning" @click="fetchHighPriorityIssues" :loading="loadingHighPriority">
          <el-icon><Flag /></el-icon>
          High Priority Issues
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
        <el-form-item label="Status">
          <el-select 
            v-model="filterForm.status" 
            placeholder="All Status" 
            clearable
            @change="handleFilterChange"
            style="width: 130px"
          >
            <el-option label="All" value="" />
            <el-option label="Open" value="OPEN" />
            <el-option label="In Review" value="IN_REVIEW" />
            <el-option label="Resolved" value="RESOLVED" />
            <el-option label="Closed" value="CLOSED" />
          </el-select>
        </el-form-item>

        <el-form-item label="Priority">
          <el-select 
            v-model="filterForm.priority" 
            placeholder="All Priority" 
            clearable
            @change="handleFilterChange"
            style="width: 130px"
          >
            <el-option label="All" value="" />
            <el-option label="Low" value="LOW" />
            <el-option label="High" value="HIGH" />
            <el-option label="Critical" value="CRITICAL" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="fetchIssues">
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

    <!-- 问题列表 -->
    <el-card class="table-card">
      <el-table 
        :data="paginatedIssues" 
        v-loading="loading"
        stripe
        border
        @row-click="handleRowClick"
        style="cursor: pointer"
      >
        <el-table-column prop="issueId" label="Issue ID" width="140" />
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="priority" label="Priority" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bookingId" label="Booking ID" width="140">
          <template #default="{ row }">
            {{ row.bookingId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="scooterId" label="Scooter ID" width="120">
          <template #default="{ row }">
            {{ row.scooterId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Reporter" width="150">
          <template #default="{ row }">
            {{ getReporterName(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="240" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click.stop="openDetailDialog(row)"
            >
              Detail
            </el-button>
            
            <!-- 修改优先级下拉菜单 -->
            <el-dropdown 
              @command="(cmd) => updatePriority(row.issueId, cmd)" 
              trigger="click"
              @click.stop
            >
              <el-button type="warning" link @click.stop>
                Priority <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="LOW">
                    <el-tag type="info" size="small">Low</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item command="HIGH">
                    <el-tag type="warning" size="small">High</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item command="CRITICAL">
                    <el-tag type="danger" size="small">Critical</el-tag>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <!-- 修改状态下拉菜单 -->
            <el-dropdown 
              @command="(cmd) => updateStatus(row.issueId, cmd)" 
              trigger="click"
              @click.stop
            >
              <el-button type="info" link @click.stop>
                Status <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="OPEN">
                    <el-tag type="danger" size="small">Open</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item command="IN_REVIEW">
                    <el-tag type="warning" size="small">In Review</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item command="RESOLVED">
                    <el-tag type="success" size="small">Resolved</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item command="CLOSED">
                    <el-tag type="info" size="small">Closed</el-tag>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-button 
              type="success" 
              link 
              :disabled="row.status === 'RESOLVED' || row.status === 'CLOSED'"
              @click.stop="openResolveDialog(row)"
            >
              Resolve
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
          :total="filteredIssues.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 高优先级问题对话框 -->
    <el-dialog
      v-model="highPriorityDialogVisible"
      title="High Priority Issues"
      width="800px"
    >
      <el-table 
        :data="highPriorityIssues" 
        v-loading="loadingHighPriority"
        stripe
        border
        @row-click="handleHighPriorityRowClick"
        style="cursor: pointer"
      >
        <el-table-column prop="issueId" label="Issue ID" width="140" />
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="priority" label="Priority" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Reporter" width="150">
          <template #default="{ row }">
            {{ getReporterName(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="highPriorityDialogVisible = false">Close</el-button>
      </template>
    </el-dialog>

    <!-- 创建 Issue 对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="Create Issue"
      width="550px"
      @close="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createFormRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="Title" prop="title" required>
          <el-input 
            v-model="createForm.title" 
            placeholder="Enter issue title"
            :disabled="creating"
          />
        </el-form-item>

        <el-form-item label="Description" prop="description" required>
          <el-input 
            v-model="createForm.description" 
            type="textarea"
            placeholder="Describe the issue in detail"
            :rows="4"
            :disabled="creating"
          />
        </el-form-item>

        <el-form-item label="Priority" prop="priority">
          <el-select 
            v-model="createForm.priority" 
            placeholder="Select priority"
            style="width: 100%"
            :disabled="creating"
          >
            <el-option label="Low" value="LOW" />
            <el-option label="High" value="HIGH" />
            <el-option label="Critical" value="CRITICAL" />
          </el-select>
          <div class="form-tip">Default: Low</div>
        </el-form-item>

        <el-form-item label="Booking ID" prop="bookingId">
          <el-input 
            v-model="createForm.bookingId" 
            placeholder="Optional: Link to an existing booking"
            :disabled="creating"
          />
          <div class="form-tip">Optional: Link to an existing booking</div>
        </el-form-item>

        <el-form-item label="Scooter ID" prop="scooterId">
          <el-input 
            v-model="createForm.scooterId" 
            placeholder="Optional: Link to a specific scooter"
            :disabled="creating"
          />
          <div class="form-tip">Optional: Link to a specific scooter</div>
        </el-form-item>

        <el-form-item label="Customer" prop="userId">
          <el-select
            v-model="createForm.userId"
            placeholder="Select customer (optional)"
            style="width: 100%"
            filterable
            clearable
            :disabled="creating"
            :loading="loadingUsers"
          >
            <el-option
              v-for="user in userList"
              :key="user.userId"
              :label="`${user.fullName} (${user.email})`"
              :value="user.userId"
            />
          </el-select>
          <div class="form-tip">Optional: If not selected, issue will be created under current admin</div>
        </el-form-item>
      </el-form>

      <el-alert
        title="Note"
        description="Issues created by admin will be visible to customers and can be tracked in the issue list."
        type="info"
        show-icon
        :closable="false"
        style="margin: 16px 0"
      />

      <template #footer>
        <el-button @click="createDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateIssue">
          Create Issue
        </el-button>
      </template>
    </el-dialog>

    <!-- 问题详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="`Issue Details - ${selectedIssue?.issueId || ''}`"
      width="700px"
      @close="closeDetailDialog"
    >
      <div v-loading="loadingDetail">
        <el-descriptions :column="2" border v-if="selectedIssue">
          <el-descriptions-item label="Issue ID" :span="2">
            <strong>{{ selectedIssue.issueId }}</strong>
          </el-descriptions-item>
          
          <el-descriptions-item label="Title" :span="2">
            {{ selectedIssue.title }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Description" :span="2">
            <div class="description-text">{{ selectedIssue.description }}</div>
          </el-descriptions-item>
          
          <el-descriptions-item label="Priority">
            <el-tag :type="getPriorityType(selectedIssue.priority)" size="small">
              {{ getPriorityLabel(selectedIssue.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="getStatusType(selectedIssue.status)" size="small">
              {{ getStatusLabel(selectedIssue.status) }}
            </el-tag>
          </el-descriptions-item>
          
          <el-descriptions-item label="Reported By">
            {{ getReporterName(selectedIssue) }}
          </el-descriptions-item>
          <el-descriptions-item label="Reported At">
            {{ formatDateTime(selectedIssue.createdAt) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Booking ID" v-if="selectedIssue.bookingId">
            {{ selectedIssue.bookingId }}
          </el-descriptions-item>
          <el-descriptions-item label="Scooter ID" v-if="selectedIssue.scooterId">
            {{ selectedIssue.scooterId }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Last Updated">
            {{ formatDateTime(selectedIssue.updatedAt) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="Resolution Feedback" v-if="selectedIssue.managerFeedback" :span="2">
            <div class="feedback-text">{{ selectedIssue.managerFeedback }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 评论区域 -->
        <div class="section-title">
          <el-divider content-position="left">Comments</el-divider>
          <div class="comments-list" v-if="issueComments.length > 0">
            <div 
              v-for="comment in issueComments" 
              :key="comment.commentId"
              class="comment-item"
            >
              <div class="comment-header">
                <span class="comment-author">
                  <el-tag :type="comment.authorRole === 'MANAGER' ? 'danger' : 'info'" size="small">
                    {{ comment.authorRole === 'MANAGER' ? 'Admin' : 'Customer' }}
                  </el-tag>
                  <span style="margin-left: 8px;">{{ comment.authorUserId?.substring(0, 8) || 'User' }}</span>
                </span>
                <span class="comment-time">{{ formatDateTime(comment.createdAt) }}</span>
              </div>
              <div class="comment-content">{{ comment.message }}</div>
            </div>
          </div>
          <div v-else class="empty-text">No comments yet</div>
        </div>

        <!-- 添加评论 -->
        <div class="add-comment">
          <el-input
            v-model="newComment"
            type="textarea"
            placeholder="Add a comment..."
            :rows="3"
            :disabled="addingComment"
          />
          <el-button 
            type="primary" 
            :loading="addingComment"
            style="margin-top: 12px"
            @click="addComment"
          >
            Add Comment
          </el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">Close</el-button>
        <div class="detail-actions">
          <!-- 详情弹窗中的优先级修改 -->
          <el-dropdown @command="(cmd) => updatePriority(selectedIssue?.issueId, cmd)">
            <el-button type="warning">
              Change Priority <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="LOW">Low</el-dropdown-item>
                <el-dropdown-item command="HIGH">High</el-dropdown-item>
                <el-dropdown-item command="CRITICAL">Critical</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <!-- 详情弹窗中的状态修改 -->
          <el-dropdown @command="(cmd) => updateStatus(selectedIssue?.issueId, cmd)">
            <el-button type="info">
              Change Status <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="OPEN">Open</el-dropdown-item>
                <el-dropdown-item command="IN_REVIEW">In Review</el-dropdown-item>
                <el-dropdown-item command="RESOLVED">Resolved</el-dropdown-item>
                <el-dropdown-item command="CLOSED">Closed</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <el-button 
            type="success" 
            :disabled="selectedIssue?.status === 'RESOLVED' || selectedIssue?.status === 'CLOSED'"
            @click="openResolveDialog(selectedIssue)"
          >
            Resolve Issue
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 解决问题对话框 -->
    <el-dialog
      v-model="resolveDialogVisible"
      title="Resolve Issue"
      width="500px"
    >
      <el-form :model="resolveForm" label-width="100px">
        <el-form-item label="Issue ID">
          <span>{{ selectedIssue?.issueId }}</span>
        </el-form-item>
        
        <el-form-item label="Title">
          <span>{{ selectedIssue?.title }}</span>
        </el-form-item>
        
        <el-form-item label="Feedback">
          <el-input
            v-model="resolveForm.feedback"
            type="textarea"
            placeholder="Enter resolution feedback (optional)"
            :rows="4"
          />
          <div class="form-tip">Optional: Add notes about how the issue was resolved</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resolveDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="resolving" @click="confirmResolve">
          Confirm Resolve
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Plus, Flag, ArrowDown } from '@element-plus/icons-vue'
import { adminIssuesApi, adminUsersApi } from '../../api'

const loading = ref(false)
const loadingDetail = ref(false)
const loadingHighPriority = ref(false)
const loadingUsers = ref(false)
const addingComment = ref(false)
const resolving = ref(false)
const updating = ref(false)
const creating = ref(false)
const issues = ref([])
const selectedIssue = ref(null)
const issueComments = ref([])
const highPriorityIssues = ref([])
const userList = ref([])

const detailDialogVisible = ref(false)
const highPriorityDialogVisible = ref(false)
const resolveDialogVisible = ref(false)
const createDialogVisible = ref(false)
const resolveForm = ref({ feedback: '' })
const newComment = ref('')

// 用户信息缓存
const userCache = new Map()

// 创建表单
const createFormRef = ref(null)
const createForm = ref({
  title: '',
  description: '',
  priority: 'LOW',
  bookingId: '',
  scooterId: '',
  userId: ''
})

const createFormRules = {
  title: [
    { required: true, message: 'Please enter issue title', trigger: 'blur' },
    { min: 3, max: 200, message: 'Title must be between 3 and 200 characters', trigger: 'blur' }
  ],
  description: [
    { required: true, message: 'Please enter issue description', trigger: 'blur' },
    { min: 10, max: 2000, message: 'Description must be between 10 and 2000 characters', trigger: 'blur' }
  ]
}

// 分页
const currentPage = ref(1)
const pageSize = ref(20)

// 筛选条件
const filterForm = ref({
  status: '',
  priority: ''
})

// 过滤后的问题列表
const filteredIssues = computed(() => {
  let result = [...issues.value]
  
  if (filterForm.value.status) {
    result = result.filter(i => i.status === filterForm.value.status)
  }
  if (filterForm.value.priority) {
    result = result.filter(i => i.priority === filterForm.value.priority)
  }
  
  return result
})

// 分页显示
const paginatedIssues = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredIssues.value.slice(start, end)
})

// 辅助函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const getPriorityType = (priority) => {
  const typeMap = {
    'LOW': 'info',
    'HIGH': 'warning',
    'CRITICAL': 'danger'
  }
  return typeMap[priority] || 'info'
}

const getPriorityLabel = (priority) => {
  const labelMap = {
    'LOW': 'Low',
    'HIGH': 'High',
    'CRITICAL': 'Critical'
  }
  return labelMap[priority] || priority
}

const getStatusType = (status) => {
  const typeMap = {
    'OPEN': 'danger',
    'IN_REVIEW': 'warning',
    'RESOLVED': 'success',
    'CLOSED': 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const labelMap = {
    'OPEN': 'Open',
    'IN_REVIEW': 'In Review',
    'RESOLVED': 'Resolved',
    'CLOSED': 'Closed'
  }
  return labelMap[status] || status
}

// 获取报告人姓名
const getReporterName = (issue) => {
  if (issue._userInfo?.fullName) {
    return issue._userInfo.fullName
  }
  if (userCache.has(issue.reporterUserId)) {
    const user = userCache.get(issue.reporterUserId)
    return user?.fullName || issue.reporterUserId?.substring(0, 8)
  }
  return issue.reporterUserId?.substring(0, 8) || '-'
}

// 获取用户信息
const fetchUserInfo = async (userId) => {
  if (userCache.has(userId)) {
    return userCache.get(userId)
  }
  try {
    const response = await adminUsersApi.getUser(userId)
    const userData = response.data.data
    userCache.set(userId, userData)
    return userData
  } catch (error) {
    console.error(`Failed to fetch user ${userId}:`, error)
    return null
  }
}

// 加载用户列表（用于创建时的下拉选择）
const fetchUserList = async () => {
  loadingUsers.value = true
  try {
    const response = await adminUsersApi.list('CUSTOMER', 'ACTIVE')
    userList.value = response.data.data || []
  } catch (error) {
    console.error('Failed to fetch user list:', error)
  } finally {
    loadingUsers.value = false
  }
}

// 为问题列表添加用户信息
const enrichIssuesWithUsers = async (issueList) => {
  const userIds = [...new Set(issueList.map(i => i.reporterUserId).filter(Boolean))]
  
  const promises = userIds.map(async (userId) => {
    if (!userCache.has(userId)) {
      const userInfo = await fetchUserInfo(userId)
      if (userInfo) {
        userCache.set(userId, userInfo)
      }
    }
  })
  
  await Promise.all(promises)
  
  return issueList.map(issue => ({
    ...issue,
    _userInfo: userCache.get(issue.reporterUserId)
  }))
}

// API 调用
const fetchIssues = async () => {
  loading.value = true
  try {
    const response = await adminIssuesApi.listAdmin(
      filterForm.value.status || null,
      filterForm.value.priority || null
    )
    const rawIssues = response.data.data || []
    issues.value = await enrichIssuesWithUsers(rawIssues)
    currentPage.value = 1
  } catch (error) {
    ElMessage.error('Failed to fetch issues')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchIssueDetail = async (issueId) => {
  loadingDetail.value = true
  try {
    const response = await adminIssuesApi.getIssue(issueId)
    const detailData = response.data.data
    
    // 获取报告人信息
    if (detailData.reporterUserId && !userCache.has(detailData.reporterUserId)) {
      await fetchUserInfo(detailData.reporterUserId)
    }
    detailData._userInfo = userCache.get(detailData.reporterUserId)
    
    selectedIssue.value = detailData
    issueComments.value = detailData.comments || []
  } catch (error) {
    ElMessage.error('Failed to fetch issue details')
    console.error(error)
  } finally {
    loadingDetail.value = false
  }
}

const fetchHighPriorityIssues = async () => {
  highPriorityDialogVisible.value = true
  loadingHighPriority.value = true
  try {
    const response = await adminIssuesApi.listHighPriority()
    const rawIssues = response.data.data || []
    highPriorityIssues.value = await enrichIssuesWithUsers(rawIssues)
  } catch (error) {
    ElMessage.error('Failed to fetch high priority issues')
    console.error(error)
  } finally {
    loadingHighPriority.value = false
  }
}

const addComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('Please enter a comment')
    return
  }
  
  addingComment.value = true
  try {
    await adminIssuesApi.addComment(selectedIssue.value.issueId, newComment.value)
    ElMessage.success('Comment added successfully')
    newComment.value = ''
    await fetchIssueDetail(selectedIssue.value.issueId)
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Failed to add comment'
    ElMessage.error(errorMsg)
  } finally {
    addingComment.value = false
  }
}

// 更新优先级
const updatePriority = async (issueId, priority) => {
  if (!issueId) return
  
  updating.value = true
  try {
    await adminIssuesApi.updatePriority(issueId, priority)
    ElMessage.success(`Priority updated to ${getPriorityLabel(priority)}`)
    
    await fetchIssues()
    if (detailDialogVisible.value && selectedIssue.value?.issueId === issueId) {
      await fetchIssueDetail(issueId)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Failed to update priority'
    ElMessage.error(errorMsg)
  } finally {
    updating.value = false
  }
}

// 更新状态
const updateStatus = async (issueId, status) => {
  if (!issueId) return
  
  updating.value = true
  try {
    await adminIssuesApi.updateStatus(issueId, status)
    ElMessage.success(`Status updated to ${getStatusLabel(status)}`)
    
    await fetchIssues()
    if (detailDialogVisible.value && selectedIssue.value?.issueId === issueId) {
      await fetchIssueDetail(issueId)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Failed to update status'
    ElMessage.error(errorMsg)
  } finally {
    updating.value = false
  }
}

const createIssue = async (data) => {
  creating.value = true
  try {
    const requestData = {
      title: data.title,
      description: data.description,
      priority: data.priority || 'LOW'
    }
    
    if (data.bookingId) requestData.bookingId = data.bookingId
    if (data.scooterId) requestData.scooterId = data.scooterId
    if (data.userId) requestData.userId = data.userId
    
    const response = await adminIssuesApi.createIssue(requestData)
    ElMessage.success('Issue created successfully')
    createDialogVisible.value = false
    
    await fetchIssues()
    
    const newIssue = response.data.data
    if (newIssue?.issueId) {
      ElMessage.info(`Issue ID: ${newIssue.issueId}`)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Failed to create issue'
    ElMessage.error(errorMsg)
    console.error(error)
  } finally {
    creating.value = false
  }
}

const resolveIssue = async (issueId, feedback) => {
  resolving.value = true
  try {
    await adminIssuesApi.resolveIssue(issueId, feedback)
    ElMessage.success('Issue resolved successfully')
    resolveDialogVisible.value = false
    
    await fetchIssues()
    if (detailDialogVisible.value && selectedIssue.value) {
      await fetchIssueDetail(issueId)
    }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Failed to resolve issue'
    ElMessage.error(errorMsg)
  } finally {
    resolving.value = false
  }
}

// 事件处理
const resetFilter = () => {
  filterForm.value = {
    status: '',
    priority: ''
  }
  fetchIssues()
}

const refreshData = () => {
  fetchIssues()
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchIssues()
}

const handleRowClick = (row) => {
  openDetailDialog(row)
}

const handleHighPriorityRowClick = (row) => {
  highPriorityDialogVisible.value = false
  openDetailDialog(row)
}

const openDetailDialog = async (row) => {
  selectedIssue.value = { ...row }
  detailDialogVisible.value = true
  await fetchIssueDetail(row.issueId)
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
  selectedIssue.value = null
  issueComments.value = []
  newComment.value = ''
}

const openResolveDialog = (row) => {
  selectedIssue.value = row
  resolveForm.value = { feedback: '' }
  resolveDialogVisible.value = true
}

const confirmResolve = async () => {
  if (!selectedIssue.value) return
  await resolveIssue(selectedIssue.value.issueId, resolveForm.value.feedback || null)
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = {
    title: '',
    description: '',
    priority: 'LOW',
    bookingId: '',
    scooterId: '',
    userId: ''
  }
  if (createFormRef.value) {
    createFormRef.value.clearValidate()
  }
}

const handleCreateIssue = async () => {
  if (!createFormRef.value) return
  
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    await createIssue(createForm.value)
  })
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handlePageChange = (val) => {
  currentPage.value = val
}

onMounted(() => {
  fetchIssues()
  fetchUserList()
})
</script>

<style scoped>
.issue-manage-container {
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

.description-text {
  white-space: pre-wrap;
  line-height: 1.5;
  max-height: 150px;
  overflow-y: auto;
}

.feedback-text {
  color: #67c23a;
  font-style: italic;
  white-space: pre-wrap;
}

.comments-list {
  max-height: 300px;
  overflow-y: auto;
}

.comment-item {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 600;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-content {
  color: #606266;
  line-height: 1.4;
}

.add-comment {
  margin-top: 16px;
}

.empty-text {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>