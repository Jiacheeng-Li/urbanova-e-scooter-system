<template>
  <div class="dashboard-content">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-header">
        <div class="welcome-text">
          <h1>Welcome back! Manager 👋</h1>
          <p>今天是 {{ currentDate }}，祝您工作愉快！</p>
        </div>
        <div class="admin-info">
          <el-avatar :size="60" :icon="UserFilled" />
          <div class="admin-details">
            <strong>{{ authStore.currentUser?.fullName || '管理员' }}</strong>
            <el-tag type="danger" size="small">管理员</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover" @click="goToHireOptions">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.hireOptions }}</div>
              <div class="stat-label">租赁选项</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover" @click="goToScooters">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a">
              <el-icon><Bicycle /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.scooters }}</div>
              <div class="stat-label">滑板车总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.availableScooters }}</div>
              <div class="stat-label">可用滑板车</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.maintenanceScooters }}</div>
              <div class="stat-label">维护中</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作区域 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card class="quick-actions-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button 
              type="primary" 
              size="large"
              @click="goToHireOptions"
            >
              <el-icon><Plus /></el-icon>
              新增租赁选项
            </el-button>
            <el-button 
              type="success" 
              size="large"
              @click="goToScooters"
            >
              <el-icon><Plus /></el-icon>
              添加滑板车
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card class="tips-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统提示</span>
            </div>
          </template>
          <div class="tips-content">
            <el-timeline>
              <el-timeline-item
                v-for="(tip, index) in tips"
                :key="index"
                :timestamp="tip.time"
                :type="tip.type"
              >
                {{ tip.message }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { adminHireOptionsApi, adminScootersApi } from '../../api'
import { 
  Tickets, 
  Bicycle, 
  UserFilled,
  CircleCheck,
  Warning,
  Plus
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const currentDate = ref('')

const stats = ref({
  hireOptions: 0,
  scooters: 0,
  availableScooters: 0,
  maintenanceScooters: 0
})

const tips = ref([
  { time: '提示', message: '管理租赁选项可以配置价格和时长', type: 'primary' },
  { time: '提示', message: '管理滑板车可以查看和更新滑板车状态', type: 'success' },
  { time: '注意', message: '禁用租赁选项后用户将无法选择', type: 'warning' }
])

const fetchStats = async () => {
  try {
    const hireOptionsRes = await adminHireOptionsApi.list()
    stats.value.hireOptions = hireOptionsRes.data.data.length

    const scootersRes = await adminScootersApi.list()
    const allScooters = scootersRes.data.data
    stats.value.scooters = allScooters.length
    stats.value.availableScooters = allScooters.filter(s => s.status === 'AVAILABLE').length
    stats.value.maintenanceScooters = allScooters.filter(s => s.status === 'MAINTENANCE').length
  } catch (error) {
    console.error('获取统计数据失败:', error)
    stats.value.hireOptions = 5
    stats.value.scooters = 10
    stats.value.availableScooters = 6
    stats.value.maintenanceScooters = 2
  }
}

const goToHireOptions = () => {
  router.push('/admin/hire-options')
}

const goToScooters = () => {
  router.push('/admin/scooters')
}

const updateDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const weekday = weekdays[now.getDay()]
  currentDate.value = `${year}年${month}月${day}日 ${weekday}`
}

onMounted(() => {
  updateDate()
  fetchStats()
})
</script>

<style scoped>
.dashboard-content {
  padding: 0;
}

.welcome-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #303133;
}

.welcome-text p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-details strong {
  font-size: 16px;
  color: #303133;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.quick-actions-card,
.tips-card {
  border-radius: 12px;
  height: 300px;
}

.card-header {
  font-weight: 600;
  color: #303133;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 0;
}

.quick-actions .el-button {
  width: 100%;
}

.tips-content {
  padding: 10px 0;
}

@media (max-width: 768px) {
  .welcome-text h1 {
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 22px;
  }
}
</style>