<template>
  <div class="scooter-manage-container">
    <div class="page-header">
      <h1 class="page-title">滑板车管理</h1>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        添加滑板车
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="状态筛选">
          <el-select 
            v-model="filterForm.status" 
            placeholder="全部状态" 
            clearable
            @change="fetchScooters"
            style="width: 150px"
          >
            <el-option label="全部" value="" />
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="已预订" value="RESERVED" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="不可用" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchScooters">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button type="warning" @click="openBulkStatusDialog">
            <el-icon><Operation /></el-icon>
            批量更新状态
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
        <el-table-column prop="scooterId" label="滑板车ID" width="140" />
        <el-table-column prop="typeCode" label="型号" width="120" />
        <el-table-column prop="color" label="颜色" width="100">
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
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batteryPercent" label="电量" width="100">
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
        <el-table-column prop="zone" label="区域" width="120" />
        <el-table-column prop="lat" label="纬度" width="120" />
        <el-table-column prop="lng" label="经度" width="120" />
        <el-table-column prop="version" label="版本" width="80" />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button 
              type="warning" 
              link 
              @click="openStatusDialog(row)"
            >
              修改状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '添加滑板车' : '编辑滑板车信息'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="滑板车ID" prop="scooterId" v-if="dialogMode === 'create'">
          <el-input 
            v-model="formData.scooterId" 
            placeholder="例如: SCO-0100"
            :disabled="submitting"
          />
          <div class="form-tip">ID将自动转换为大写</div>
        </el-form-item>

        <el-form-item label="型号" prop="typeCode">
          <el-input 
            v-model="formData.typeCode" 
            placeholder="例如: X9-PRO, E-SCOOTER"
            :disabled="submitting"
          />
          <div class="form-tip">滑板车型号代码</div>
        </el-form-item>

        <el-form-item label="颜色" prop="color">
          <div style="display: flex; gap: 12px; align-items: center;">
            <el-input 
              v-model="formData.color" 
              placeholder="例如: 红色, #FF0000, 黑色"
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
          <div class="form-tip">支持颜色名称、十六进制或RGB值</div>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select 
            v-model="formData.status" 
            placeholder="请选择状态"
            style="width: 100%"
            :disabled="submitting"
          >
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="已预订" value="RESERVED" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="不可用" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>

        <el-form-item label="电量(%)" prop="batteryPercent">
          <el-slider 
            v-model="formData.batteryPercent" 
            :min="0" 
            :max="100"
            :disabled="submitting"
          />
        </el-form-item>

        <el-form-item label="所在区域" prop="zone">
          <el-input 
            v-model="formData.zone" 
            placeholder="例如: ZONE-A"
            :disabled="submitting"
          />
        </el-form-item>

        <el-form-item label="纬度" prop="lat">
          <el-input-number 
            v-model="formData.lat" 
            :step="0.001"
            :precision="6"
            style="width: 100%"
            placeholder="例如: 51.5074"
            :disabled="submitting"
          />
        </el-form-item>

        <el-form-item label="经度" prop="lng">
          <el-input-number 
            v-model="formData.lng" 
            :step="0.001"
            :precision="6"
            style="width: 100%"
            placeholder="例如: -0.1278"
            :disabled="submitting"
          />
        </el-form-item>

        <!-- 编辑模式下显示版本号（只读） -->
        <el-form-item label="版本号" v-if="dialogMode === 'edit'">
          <el-input 
            :value="formData.version" 
            disabled
            placeholder="版本号自动递增"
          />
          <div class="form-tip">版本号在每次更新时由系统自动递增</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogMode === 'create' ? '添加' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改状态对话框 -->
    <el-dialog
      v-model="statusDialogVisible"
      title="修改滑板车状态"
      width="450px"
    >
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="滑板车ID">
          <span>{{ selectedScooter?.scooterId }}</span>
        </el-form-item>
        <el-form-item label="型号">
          <span>{{ selectedScooter?.typeCode }}</span>
        </el-form-item>
        <el-form-item label="颜色">
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
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(selectedScooter?.status)">
            {{ getStatusLabel(selectedScooter?.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="当前版本">
          <span>{{ selectedScooter?.version }}</span>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select 
            v-model="statusForm.status" 
            placeholder="请选择新状态"
            style="width: 100%"
          >
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="已预订" value="RESERVED" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="不可用" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="updatingStatus" @click="confirmStatusUpdate">
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量更新状态对话框 -->
    <el-dialog
      v-model="bulkStatusDialogVisible"
      title="批量更新状态"
      width="500px"
    >
      <el-form :model="bulkStatusForm" label-width="100px">
        <el-form-item label="选择滑板车">
          <el-select 
            v-model="bulkStatusForm.scooterIds" 
            multiple
            placeholder="请选择滑板车"
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
        <el-form-item label="目标状态">
          <el-select 
            v-model="bulkStatusForm.status" 
            placeholder="请选择状态"
            style="width: 100%"
          >
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="已预订" value="RESERVED" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="不可用" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="bulkStatusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="updatingBulkStatus" @click="confirmBulkStatusUpdate">
          批量更新
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
    { required: true, message: '请输入滑板车ID', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9\-]+$/, message: '滑板车ID只能包含字母、数字和连字符', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在1-50个字符之间', trigger: 'blur' }
  ],
  typeCode: [
    { required: true, message: '请输入型号', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在1-50个字符之间', trigger: 'blur' }
  ],
  color: [
    { max: 50, message: '颜色描述不能超过50个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  batteryPercent: [
    { required: true, message: '请输入电量', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '电量应在0-100之间', trigger: 'blur' }
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
    'AVAILABLE': '可用',
    'RESERVED': '已预订',
    'IN_USE': '使用中',
    'MAINTENANCE': '维护中',
    'UNAVAILABLE': '不可用'
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
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchScooters = async () => {
  loading.value = true
  try {
    const params = filterForm.value.status ? { status: filterForm.value.status } : {}
    const response = await adminScootersApi.list(params)
    scooters.value = response.data.data
  } catch (error) {
    ElMessage.error('获取滑板车列表失败')
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
        ElMessage.success('添加成功')
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
        ElMessage.success('更新成功')
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
      const errorMsg = error.response?.data?.error?.message || '操作失败'
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
    ElMessage.success('状态更新成功')
    
    const index = scooters.value.findIndex(
      item => item.scooterId === selectedScooter.value.scooterId
    )
    if (index !== -1) {
      scooters.value[index] = response.data.data
    }
    statusDialogVisible.value = false
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || '状态更新失败'
    ElMessage.error(errorMsg)
  } finally {
    updatingStatus.value = false
  }
}

const confirmBulkStatusUpdate = async () => {
  if (bulkStatusForm.value.scooterIds.length === 0) {
    ElMessage.warning('请至少选择一个滑板车')
    return
  }
  if (!bulkStatusForm.value.status) {
    ElMessage.warning('请选择目标状态')
    return
  }

  updatingBulkStatus.value = true
  try {
    const response = await adminScootersApi.bulkUpdateStatus({
      scooterIds: bulkStatusForm.value.scooterIds,
      status: bulkStatusForm.value.status
    })
    ElMessage.success(`成功更新 ${response.data.data.updatedCount} 辆滑板车的状态`)
    
    await fetchScooters()
    bulkStatusDialogVisible.value = false
    bulkStatusForm.value = { scooterIds: [], status: '' }
  } catch (error) {
    const errorMsg = error.response?.data?.error?.message || '批量更新失败'
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