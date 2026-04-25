<template>
  <div class="dashboard-content">
    <!-- 日期选择器 -->
    <el-card class="date-card" shadow="hover">
      <div class="date-selector">
        <div class="selector-title">
          <el-icon><Calendar /></el-icon>
          <span>Data Range</span>
        </div>
        <div class="selector-controls">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="to"
            start-placeholder="Start Date"
            end-placeholder="End Date"
            :shortcuts="dateShortcuts"
            @change="handleDateRangeChange"
          />
          <el-button type="primary" @click="refreshAllData" :loading="loading">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 收入估算卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card revenue-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon total-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">£{{ formatNumber(revenueEstimate.totalRevenue) }}</div>
              <div class="stat-label">Total Revenue</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card booking-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon booking-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ revenueEstimate.totalBookings || 0 }}</div>
              <div class="stat-label">Total Bookings</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card avg-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon avg-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">£{{ formatNumber(revenueEstimate.averageOrderValue) }}</div>
              <div class="stat-label">Avg Order Value</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card days-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon days-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ revenueEstimate.daysInRange || 0 }}</div>
              <div class="stat-label">Days in Range</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 周收入图表 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Weekly Revenue Trend</span>
              <el-tag type="info" size="small">Last 7 Days</el-tag>
            </div>
          </template>
          <div ref="weeklyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 每日收入图表 + 按租赁选项收入 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Daily Revenue Breakdown</span>
            </div>
          </template>
          <div ref="dailyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Revenue by Hire Option</span>
            </div>
          </template>
          <div ref="hireOptionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 高频用户列表 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="24">
        <el-card class="table-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Frequent Users</span>
              <el-tag type="warning" size="small">Top 10 by Ride Hours</el-tag>
            </div>
          </template>
          <el-table
            :data="frequentUsers"
            v-loading="loadingUsers"
            stripe
            border
            style="width: 100%"
          >
            <el-table-column prop="rank" label="Rank" width="80" align="center">
              <template #default="{ $index }">
                <el-tag :type="getRankType($index + 1)">{{ $index + 1 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fullName" label="User Name" min-width="150" />
            <el-table-column prop="email" label="Email" min-width="200" />
            <el-table-column prop="totalRideHours" label="Total Ride Hours" width="150" align="right">
              <template #default="{ row }">
                <strong>{{ row.totalRideHours?.toFixed(1) || '0.0' }} hrs</strong>
              </template>
            </el-table-column>
            <el-table-column prop="totalSpent" label="Total Spent" width="150" align="right">
              <template #default="{ row }">
                <span class="money">£{{ formatNumber(row.totalSpent) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="bookingCount" label="Bookings" width="120" align="center" />
            <el-table-column label="Avg per Booking" width="140" align="right">
              <template #default="{ row }">
                £{{ formatNumber(row.totalSpent / row.bookingCount) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { adminAnalyticsApi } from '../../api'
import { ElMessage } from 'element-plus'
import { Calendar, Refresh, Money, Document, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// Refs for charts
const weeklyChartRef = ref(null)
const dailyChartRef = ref(null)
const hireOptionChartRef = ref(null)

let weeklyChart = null
let dailyChart = null
let hireOptionChart = null

// Data
const loading = ref(false)
const loadingUsers = ref(false)
const dateRange = ref([])

const revenueEstimate = ref({
  totalRevenue: 0,
  totalBookings: 0,
  averageOrderValue: 0,
  daysInRange: 0
})

const weeklyChartData = ref({
  dates: [],
  revenue: []
})

const dailyCombinedData = ref([])
const weeklyByHireOption = ref([])
const frequentUsers = ref([])

// Date shortcuts
const dateShortcuts = [
  {
    text: 'Last 7 Days',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 7 * 24 * 3600 * 1000)
      return [start, end]
    }
  },
  {
    text: 'Last 30 Days',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 30 * 24 * 3600 * 1000)
      return [start, end]
    }
  },
  {
    text: 'This Month',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(1)
      return [start, end]
    }
  },
  {
    text: 'Last Month',
    value: () => {
      const end = new Date()
      end.setDate(1)
      end.setDate(0)
      const start = new Date()
      start.setMonth(start.getMonth() - 1)
      start.setDate(1)
      return [start, end]
    }
  }
]

// Helper functions
const formatNumber = (num) => {
  if (num === undefined || num === null) return '0.00'
  return num.toFixed(2)
}

const getRankType = (rank) => {
  if (rank === 1) return 'danger'
  if (rank === 2) return 'warning'
  if (rank === 3) return 'success'
  return 'info'
}

// Format date to YYYY-MM-DD
const formatDate = (date) => {
  if (!date) return null
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// API Calls
const fetchRevenueEstimate = async () => {
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value?.[1] ? formatDate(dateRange.value[1]) : null

    const response = await adminAnalyticsApi.getRevenueEstimate(startDate, endDate)
    revenueEstimate.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch revenue estimate:', error)
    ElMessage.error('Failed to fetch revenue estimate')
  }
}

const fetchWeeklyChart = async () => {
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const response = await adminAnalyticsApi.getWeeklyChart(startDate)
    const data = response.data.data

    weeklyChartData.value = {
      dates: data.dates || [],
      revenue: data.revenue || []
    }

    renderWeeklyChart()
  } catch (error) {
    console.error('Failed to fetch weekly chart:', error)
    ElMessage.error('Failed to fetch weekly revenue data')
  }
}

const fetchDailyCombined = async () => {
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const response = await adminAnalyticsApi.getDailyCombined(startDate)
    dailyCombinedData.value = response.data.data || []
    renderDailyChart()
  } catch (error) {
    console.error('Failed to fetch daily combined:', error)
    ElMessage.error('Failed to fetch daily revenue data')
  }
}

const fetchWeeklyByHireOption = async () => {
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const response = await adminAnalyticsApi.getWeeklyByHireOption(startDate)
    weeklyByHireOption.value = response.data.data || []
    renderHireOptionChart()
  } catch (error) {
    console.error('Failed to fetch hire option revenue:', error)
    ElMessage.error('Failed to fetch hire option revenue data')
  }
}

const fetchFrequentUsers = async () => {
  loadingUsers.value = true
  try {
    const response = await adminAnalyticsApi.getFrequentUsers()
    frequentUsers.value = response.data.data || []
  } catch (error) {
    console.error('Failed to fetch frequent users:', error)
    ElMessage.error('Failed to fetch frequent users')
  } finally {
    loadingUsers.value = false
  }
}

// Chart rendering
const renderWeeklyChart = () => {
  if (!weeklyChartRef.value) return

  if (weeklyChart) {
    weeklyChart.dispose()
  }

  weeklyChart = echarts.init(weeklyChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params[0]
        return `${item.axisValue}<br/>Revenue: £${item.value.toFixed(2)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: weeklyChartData.value.dates,
      axisLabel: {
        rotate: 30,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: 'Revenue (£)',
      axisLabel: {
        formatter: (value) => `£${value}`
      }
    },
    series: [
      {
        name: 'Revenue',
        type: 'bar',
        data: weeklyChartData.value.revenue,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#409eff' },
              { offset: 1, color: '#66b1ff' }
            ]
          }
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params) => `£${params.value.toFixed(2)}`,
          fontSize: 11
        }
      }
    ]
  }

  weeklyChart.setOption(option)
}

const renderDailyChart = () => {
  if (!dailyChartRef.value) return

  if (dailyChart) {
    dailyChart.dispose()
  }

  dailyChart = echarts.init(dailyChartRef.value)

  const dates = dailyCombinedData.value.map(item => item.date || item.day)
  const revenues = dailyCombinedData.value.map(item => item.revenue || item.totalRevenue || 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const item = params[0]
        return `${item.axisValue}<br/>Daily Revenue: £${item.value.toFixed(2)}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: 30,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: 'Revenue (£)',
      axisLabel: {
        formatter: (value) => `£${value}`
      }
    },
    series: [
      {
        name: 'Daily Revenue',
        type: 'line',
        data: revenues,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#67c23a'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
            ]
          }
        },
        itemStyle: {
          color: '#67c23a'
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params) => `£${params.value.toFixed(2)}`,
          fontSize: 10
        }
      }
    ]
  }

  dailyChart.setOption(option)
}

const renderHireOptionChart = () => {
  if (!hireOptionChartRef.value) return

  if (hireOptionChart) {
    hireOptionChart.dispose()
  }

  hireOptionChart = echarts.init(hireOptionChartRef.value)

  const hireOptions = weeklyByHireOption.value.map(item => item.hireOptionCode || item.code)
  const revenues = weeklyByHireOption.value.map(item => item.revenue || item.totalRevenue || 0)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>Revenue: £${params.value.toFixed(2)}<br/>Percentage: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      type: 'scroll',
      textStyle: {
        fontSize: 11
      }
    },
    series: [
      {
        name: 'Revenue by Hire Option',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: (params) => {
            return `${params.name}\n£${params.value.toFixed(2)}`
          },
          fontSize: 11
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: hireOptions.map((name, index) => ({
          name,
          value: revenues[index]
        }))
      }
    ]
  }

  hireOptionChart.setOption(option)
}

// Handle window resize
const handleResize = () => {
  if (weeklyChart) weeklyChart.resize()
  if (dailyChart) dailyChart.resize()
  if (hireOptionChart) hireOptionChart.resize()
}

// Refresh all data
const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchRevenueEstimate(),
      fetchWeeklyChart(),
      fetchDailyCombined(),
      fetchWeeklyByHireOption(),
      fetchFrequentUsers()
    ])
    ElMessage.success('Data refreshed successfully')
  } catch (error) {
    console.error('Failed to refresh data:', error)
  } finally {
    loading.value = false
  }
}

const handleDateRangeChange = () => {
  refreshAllData()
}

// Initialize default date range (last 7 days)
const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 7 * 24 * 3600 * 1000)
  dateRange.value = [start, end]
}

onMounted(() => {
  initDateRange()
  refreshAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (weeklyChart) weeklyChart.dispose()
  if (dailyChart) dailyChart.dispose()
  if (hireOptionChart) hireOptionChart.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-content {
  padding: 0;
}

/* Date selector card */
.date-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.date-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.selector-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.selector-controls {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* Stats row */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.total-icon {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.booking-icon {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.avg-icon {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
}

.days-icon {
  background: linear-gradient(135deg, #909399, #b4b6bc);
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

/* Chart cards */
.chart-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 350px;
}

/* Table card */
.table-card {
  border-radius: 12px;
}

.money {
  color: #67c23a;
  font-weight: 500;
}

/* Responsive */
@media (max-width: 768px) {
  .stat-value {
    font-size: 22px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
  }

  .stat-icon .el-icon {
    font-size: 22px;
  }

  .chart-container {
    height: 280px;
  }

  .date-selector {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
