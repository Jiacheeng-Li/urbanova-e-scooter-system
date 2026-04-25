<template>
  <div class="scooter-manage-container">
    <div class="page-header">
      <h1 class="page-title">Scooter Management</h1>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        Add Scooter
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="Filter by Status">
          <el-select
            v-model="filterForm.status"
            placeholder="All Status"
            clearable
            @change="fetchScooters"
            style="width: 150px"
          >
            <el-option label="All" value="" />
            <el-option label="Available" value="AVAILABLE" />
            <el-option label="Reserved" value="RESERVED" />
            <el-option label="In Use" value="IN_USE" />
            <el-option label="Maintenance" value="MAINTENANCE" />
            <el-option label="Unavailable" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchScooters">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            Reset
          </el-button>
          <el-button type="warning" @click="openBulkStatusDialog">
            <el-icon><Operation /></el-icon>
            Bulk Update Status
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 滑板车列表 -->
    <el-card class="table-card">
      <el-table
        :data="scooters"
        v-loading="loading"
        stripe
        border
      >
        <el-table-column prop="scooterId" label="Scooter ID" width="140" />
        <el-table-column prop="typeCode" label="Model" width="120" />
        <el-table-column prop="color" label="Color" width="100">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <div
                :style="{
                  width: '20px',
                  height: '20px',
                  backgroundColor: row.color || '#ddd',
                  borderRadius: '4px',
                  border: '1px solid #e4e7ed'
                }"
              />
              <span>{{ row.color || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batteryPercent" label="Battery" width="100">
          <template #default="{ row }">
            <el-progress
              :percentage="row.batteryPercent"
              :color="getBatteryColor(row.batteryPercent)"
              :stroke-width="8"
              :show-text="false"
              style="width: 80px; display: inline-block"
            />
            <span style="margin-left: 8px">{{ row.batteryPercent }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="zone" label="Zone" width="120" />
        <el-table-column prop="lat" label="Latitude" width="120" />
        <el-table-column prop="lng" label="Longitude" width="120" />
        <el-table-column prop="version" label="Version" width="80" />
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
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="openEditDialog(row)"
            >
              Edit
            </el-button>
            <el-button
              type="warning"
              link
              @click="openStatusDialog(row)"
            >
              Change Status
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? 'Add Scooter' : 'Edit Scooter'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="Scooter ID" prop="scooterId" v-if="dialogMode === 'create'">
          <el-input
            v-model="formData.scooterId"
            placeholder="e.g., SCO-0100"
            :disabled="submitting"
          />
          <div class="form-tip">ID will be converted to uppercase</div>
        </el-form-item>

        <el-form-item label="Model" prop="typeCode">
          <el-input
            v-model="formData.typeCode"
            placeholder="e.g., X9-PRO, E-SCOOTER"
            :disabled="submitting"
          />
          <div class="form-tip">Scooter model code</div>
        </el-form-item>

        <el-form-item label="Color" prop="color">
          <div style="display: flex; gap: 12px; align-items: center;">
            <el-input
              v-model="formData.color"
              placeholder="e.g., Red, #FF0000, Black"
              style="flex: 1"
              :disabled="submitting"
            />
            <el-color-picker
              v-model="formData.color"
              show-alpha
              :predefine="predefineColors"
              :disabled="submitting"
            />
          </div>
          <div class="form-tip">Supports color name, hex, or RGB value</div>
        </el-form-item>

        <el-form-item label="Status" prop="status">
          <el-select
            v-model="formData.status"
            placeholder="Select status"
            style="width: 100%"
            :disabled="submitting"
          >
            <el-option label="Available" value="AVAILABLE" />
            <el-option label="Reserved" value="RESERVED" />
            <el-option label="In Use" value="IN_USE" />
            <el-option label="Maintenance" value="MAINTENANCE" />
            <el-option label="Unavailable" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>

        <el-form-item label="Battery (%)" prop="batteryPercent">
          <el-slider
            v-model="formData.batteryPercent"
            :min="0"
            :max="100"
            :disabled="submitting"
          />
        </el-form-item>

        <el-form-item label="Zone" prop="zone">
          <el-input
            v-model="formData.zone"
            placeholder="e.g., ZONE-A"
            :disabled="submitting"
          />
        </el-form-item>

        <el-form-item label="Latitude" prop="lat">
          <el-input-number
            v-model="formData.lat"
            :step="0.001"
            :precision="6"
            style="width: 100%"
            placeholder="e.g., 51.5074"
            :disabled="submitting"
          />
        </el-form-item>

        <el-form-item label="Longitude" prop="lng">
          <el-input-number
            v-model="formData.lng"
            :step="0.001"
            :precision="6"
            style="width: 100%"
            placeholder="e.g., -0.1278"
            :disabled="submitting"
          />
        </el-form-item>

        <!-- 编辑模式下显示版本号（只读） -->
        <el-form-item label="Version" v-if="dialogMode === 'edit'">
          <el-input
            :value="formData.version"
            disabled
            placeholder="Auto-incremented"
          />
          <div class="form-tip">Version increments automatically on each update</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogMode === 'create' ? 'Add' : 'Save' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改状态对话框 -->
    <el-dialog
      v-model="statusDialogVisible"
      title="Change Scooter Status"
      width="450px"
    >
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="Scooter ID">
          <span>{{ selectedScooter?.scooterId }}</span>
        </el-form-item>
        <el-form-item label="Model">
          <span>{{ selectedScooter?.typeCode }}</span>
        </el-form-item>
        <el-form-item label="Color">
          <div style="display: flex; align-items: center; gap: 8px;">
            <div
              :style="{
                width: '20px',
                height: '20px',
                backgroundColor: selectedScooter?.color || '#ddd',
                borderRadius: '4px',
                border: '1px solid #e4e7ed'
              }"
            />
            <span>{{ selectedScooter?.color || '-' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="Current Status">
          <el-tag :type="getStatusType(selectedScooter?.status)">
            {{ getStatusLabel(selectedScooter?.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="Current Version">
          <span>{{ selectedScooter?.version }}</span>
        </el-form-item>
        <el-form-item label="New Status">
          <el-select
            v-model="statusForm.status"
            placeholder="Select new status"
            style="width: 100%"
          >
            <el-option label="Available" value="AVAILABLE" />
            <el-option label="Reserved" value="RESERVED" />
            <el-option label="In Use" value="IN_USE" />
            <el-option label="Maintenance" value="MAINTENANCE" />
            <el-option label="Unavailable" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="statusDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="updatingStatus" @click="confirmStatusUpdate">
          Confirm Change
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量更新状态对话框 -->
    <el-dialog
      v-model="bulkStatusDialogVisible"
      title="Bulk Update Status"
      width="500px"
    >
      <el-form :model="bulkStatusForm" label-width="100px">
        <el-form-item label="Select Scooters">
          <el-select
            v-model="bulkStatusForm.scooterIds"
            multiple
            placeholder="Select scooters"
            style="width: 100%"
          >
            <el-option
              v-for="scooter in scooters"
              :key="scooter.scooterId"
              :label="`${scooter.scooterId} (${scooter.typeCode}) - ${getStatusLabel(scooter.status)}`"
              :value="scooter.scooterId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Target Status">
          <el-select
            v-model="bulkStatusForm.status"
            placeholder="Select status"
            style="width: 100%"
          >
            <el-option label="Available" value="AVAILABLE" />
            <el-option label="Reserved" value="RESERVED" />
            <el-option label="In Use" value="IN_USE" />
            <el-option label="Maintenance" value="MAINTENANCE" />
            <el-option label="Unavailable" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="bulkStatusDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="updatingBulkStatus" @click="confirmBulkStatusUpdate">
          Bulk Update
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh, Operation } from '@element-plus/icons-vue'
import { adminScootersApi } from '../../api'

const loading = ref(false)
const scooters = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const statusDialogVisible = ref(false)
const updatingStatus = ref(false)
const bulkStatusDialogVisible = ref(false)
const updatingBulkStatus = ref(false)
const selectedScooter = ref(null)
const formRef = ref(null)

// 预定义颜色
const predefineColors = [
  '#ff0000', '#00ff00', '#0000ff', '#ffff00',
  '#ff00ff', '#00ffff', '#000000', '#ffffff',
  '#ff8800', '#8800ff', '#0088ff', '#88ff00'
]

const filterForm = ref({
  status: ''
})

const formData = ref({
  scooterId: '',
  typeCode: '',
  color: '',
  status: 'AVAILABLE',
  batteryPercent: 100,
  zone: '',
  lat: null,
  lng: null,
  version: 0
})

const statusForm = ref({
  status: ''
})

const bulkStatusForm = ref({
  scooterIds: [],
  status: ''
})

const formRules = {
  scooterId: [
    { required: true, message: 'Please enter scooter ID', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9\-]+$/, message: 'Scooter ID can only contain letters, numbers and hyphens', trigger: 'blur' },
    { min: 1, max: 50, message: 'Length must be between 1 and 50 characters', trigger: 'blur' }
  ],
  typeCode: [
    { required: true, message: 'Please enter model', trigger: 'blur' },
    { min: 1, max: 50, message: 'Length must be between 1 and 50 characters', trigger: 'blur' }
  ],
  color: [
    { max: 50, message: 'Color description cannot exceed 50 characters', trigger: 'blur' }
  ],
  status: [
    { required: true, message: 'Please select status', trigger: 'change' }
  ],
  batteryPercent: [
    { required: true, message: 'Please enter battery level', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: 'Battery level must be between 0 and 100', trigger: 'blur' }
  ]
}

const getStatusType = (status) => {
  const statusMap = {
    'AVAILABLE': 'success',
    'RESERVED': 'warning',
    'IN_USE': 'primary',
    'MAINTENANCE': 'danger',
    'UNAVAILABLE': 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const statusMap = {
    'AVAILABLE': 'Available',
    'RESERVED': 'Reserved',
    'IN_USE': 'In Use',
    'MAINTENANCE': 'Maintenance',
    'UNAVAILABLE': 'Unavailable'
  }
  return statusMap[status] || status
}

const getBatteryColor = (percent) => {
  if (percent >= 70) return '#67c23a'
  if (percent >= 30) return '#e6a23c'
  return '#f56c6c'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const fetchScooters = async () => {
  loading.value = true
  try {
    const params = filterForm.value.status ? { status: filterForm.value.status } : {}
    const response = await adminScootersApi.list(params)
    scooters.value = response.data.data
  } catch (error) {
    ElMessage.error('Failed to fetch scooters')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.value.status = ''
  fetchScooters()
}

const resetForm = () => {
  formData.value = {
    scooterId: '',
    typeCode: '',
    color: '',
    status: 'AVAILABLE',
    batteryPercent: 100,
    zone: '',
    lat: null,
    lng: null,
    version: 0
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

const openEditDialog = (scooter) => {
  dialogMode.value = 'edit'
  selectedScooter.value = scooter
  formData.value = {
    scooterId: scooter.scooterId,
    typeCode: scooter.typeCode,
    color: scooter.color || '',
    status: scooter.status,
    batteryPercent: scooter.batteryPercent,
    zone: scooter.zone || '',
    lat: scooter.lat,
    lng: scooter.lng,
    version: scooter.version
  }
  dialogVisible.value = true
}

const openStatusDialog = (scooter) => {
  selectedScooter.value = scooter
  statusForm.value.status = scooter.status
  statusDialogVisible.value = true
}

const openBulkStatusDialog = () => {
  bulkStatusForm.value = { scooterIds: [], status: '' }
  bulkStatusDialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        const requestData = {
          scooterId: formData.value.scooterId.toUpperCase(),
          typeCode: formData.value.typeCode,
          color: formData.value.color || null,
          status: formData.value.status,
          batteryPercent: formData.value.batteryPercent,
          zone: formData.value.zone || null,
          lat: formData.value.lat,
          lng: formData.value.lng
        }
        const response = await adminScootersApi.create(requestData)
        ElMessage.success('Added successfully')
        scooters.value.unshift(response.data.data)
      } else {
        const requestData = {
          typeCode: formData.value.typeCode,
          color: formData.value.color || null,
          batteryPercent: formData.value.batteryPercent,
          zone: formData.value.zone || null,
          lat: formData.value.lat,
          lng: formData.value.lng
        }
        const response = await adminScootersApi.update(
          selectedScooter.value.scooterId,
          requestData
        )
        ElMessage.success('Updated successfully')
        const index = scooters.value.findIndex(
          item => item.scooterId === selectedScooter.value.scooterId
        )
        if (index !== -1) {
          scooters.value[index] = response.data.data
        }
      }
      dialogVisible.value = false
      await fetchScooters() // 刷新列表
    } catch (error) {
      const errorMsg = error.response?.data?.error?.message || 'Operation failed'
      ElMessage.error(errorMsg)
    } finally {
      submitting.value = false
    }
  })
}

const confirmStatusUpdate = async () => {
  if (!selectedScooter.value) return

  updatingStatus.value = true
  try {
    const response = await adminScootersApi.updateStatus(
      selectedScooter.value.scooterId,
      { status: statusForm.value.status }
    )
    ElMessage.success('Status updated successfully')

    const index = scooters.value.findIndex(
      item => item.scooterId === selectedScooter.value.scooterId
    )
    if (index !== -1) {
      scooters.value[index] = response.data.data
    }
    statusDialogVisible.value = false
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Status update failed'
    ElMessage.error(errorMsg)
  } finally {
    updatingStatus.value = false
  }
}

const confirmBulkStatusUpdate = async () => {
  if (bulkStatusForm.value.scooterIds.length === 0) {
    ElMessage.warning('Please select at least one scooter')
    return
  }
  if (!bulkStatusForm.value.status) {
    ElMessage.warning('Please select target status')
    return
  }

  updatingBulkStatus.value = true
  try {
    const response = await adminScootersApi.bulkUpdateStatus({
      scooterIds: bulkStatusForm.value.scooterIds,
      status: bulkStatusForm.value.status
    })
    ElMessage.success(`Successfully updated status for ${response.data.data.updatedCount} scooters`)

    await fetchScooters()
    bulkStatusDialogVisible.value = false
    bulkStatusForm.value = { scooterIds: [], status: '' }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || 'Bulk update failed'
    ElMessage.error(errorMsg)
  } finally {
    updatingBulkStatus.value = false
  }
}

onMounted(() => {
  fetchScooters()
})
</script>

<style scoped>
.scooter-manage-container {
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
