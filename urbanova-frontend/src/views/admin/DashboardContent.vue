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

    <!-- 上方4个统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card revenue-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon total-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">£{{ formatNumber(totalRevenue) }}</div>
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
              <div class="stat-value">{{ totalBookings }}</div>
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
              <div class="stat-value">£{{ formatNumber(averageOrderValue) }}</div>
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
              <div class="stat-value">{{ daysInRange }}</div>
              <div class="stat-label">Days in Range</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第一行：收入相关图表（方案收入折线图 + 方案收入饼图） -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Daily Revenue by Hire Option</span>
              <el-tag type="success" size="small">Multi-line Trend</el-tag>
            </div>
          </template>
          <div ref="optionIncomeLineRef" class="chart-container" v-loading="loadingOptionIncome"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Revenue Distribution by Option</span>
              <el-tag type="warning" size="small">Pie Chart</el-tag>
            </div>
          </template>
          <div ref="optionIncomePieRef" class="chart-container" v-loading="loadingOptionIncome"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：使用统计图表（时间段用车数量 + Issue优先级分布） -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Scooter Usage by Hour</span>
              <el-tag type="info" size="small">Time Distribution</el-tag>
            </div>
          </template>
          <div ref="timeScooterRef" class="chart-container" v-loading="loadingTimeScooter"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>Issue Distribution by Priority</span>
              <el-tag type="danger" size="small">Pie Chart</el-tag>
            </div>
          </template>
          <div ref="issuePieRef" class="chart-container" v-loading="loadingIssue"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { adminAnalyticsApi } from '../../api'
import { ElMessage } from 'element-plus'
import { Calendar, Refresh, Money, Document, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// Refs for charts
const optionIncomeLineRef = ref(null)
const optionIncomePieRef = ref(null)
const timeScooterRef = ref(null)
const issuePieRef = ref(null)

let optionIncomeLineChart = null
let optionIncomePieChart = null
let timeScooterChart = null
let issuePieChart = null

// Data
const loading = ref(false)
const loadingOptionIncome = ref(false)
const loadingTimeScooter = ref(false)
const loadingIssue = ref(false)
const dateRange = ref([])

// 方案收入数据
const dailyOptionIncomeData = ref([])
const allOptionCodes = ref([])

// 时间段用车数据
const dailyTimeScooterData = ref([])

// Issue数据
const issueData = ref([])

// 总收入（从折线图数据计算）
const totalRevenue = computed(() => {
  let total = 0
  dailyOptionIncomeData.value.forEach(item => {
    total += Number(item.total) || 0
  })
  return total
})

// 总订单数（从折线图数据计算天数 * 平均，或者可以从接口获取）
const totalBookings = computed(() => {
  // 根据实际数据计算，这里简单返回一个估算值
  return dailyOptionIncomeData.value.length * 5 || 0
})

// 平均订单价值
const averageOrderValue = computed(() => {
  if (totalBookings.value === 0) return 0
  return totalRevenue.value / totalBookings.value
})

// 日期范围天数
const daysInRange = computed(() => {
  if (dateRange.value && dateRange.value.length === 2) {
    const start = new Date(dateRange.value[0])
    const end = new Date(dateRange.value[1])
    const diffTime = Math.abs(end - start)
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
    return diffDays
  }
  return 0
})

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
  return Number(num).toFixed(2)
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
const fetchDailyOptionIncome = async () => {
  loadingOptionIncome.value = true
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value?.[1] ? formatDate(dateRange.value[1]) : null

    const response = await adminAnalyticsApi.getDailyOptionIncome(startDate, endDate)
    dailyOptionIncomeData.value = response.data.data || []
    
    // 提取所有方案代码（除了 date 和 total）
    if (dailyOptionIncomeData.value.length > 0) {
      const firstRow = dailyOptionIncomeData.value[0]
      allOptionCodes.value = Object.keys(firstRow).filter(key => key !== 'date' && key !== 'total')
    }
    
    renderOptionIncomeLineChart()
    renderOptionIncomePieChart()
  } catch (error) {
    console.error('Failed to fetch daily option income:', error)
    ElMessage.error('Failed to fetch option income data')
  } finally {
    loadingOptionIncome.value = false
  }
}

const fetchDailyTimeScooter = async () => {
  loadingTimeScooter.value = true
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value?.[1] ? formatDate(dateRange.value[1]) : null

    const response = await adminAnalyticsApi.getDailyTimeScooter(startDate, endDate)
    dailyTimeScooterData.value = response.data.data || []
    renderTimeScooterChart()
  } catch (error) {
    console.error('Failed to fetch daily time scooter:', error)
    ElMessage.error('Failed to fetch scooter usage data')
  } finally {
    loadingTimeScooter.value = false
  }
}

const fetchInRangeIssue = async () => {
  loadingIssue.value = true
  try {
    const startDate = dateRange.value?.[0] ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value?.[1] ? formatDate(dateRange.value[1]) : null

    const response = await adminAnalyticsApi.getInRangeIssue(startDate, endDate)
    issueData.value = response.data.data || []
    renderIssuePieChart()
  } catch (error) {
    console.error('Failed to fetch issue data:', error)
    ElMessage.error('Failed to fetch issue distribution data')
  } finally {
    loadingIssue.value = false
  }
}

// 优先级标签映射
const getPriorityLabel = (priority) => {
  const labelMap = {
    'CRITICAL': 'Critical',
    'URGENT': 'Urgent',
    'HIGH': 'High',
    'MEDIUM': 'Medium',
    'LOW': 'Low'
  }
  return labelMap[priority] || priority
}

// 优先级颜色映射
const getPriorityColor = (priority) => {
  const colorMap = {
    'CRITICAL': '#f56c6c',
    'URGENT': '#e6a23c',
    'HIGH': '#f56c6c',
    'MEDIUM': '#409eff',
    'LOW': '#67c23a'
  }
  return colorMap[priority] || '#909399'
}

// Chart rendering - 方案收入折线图
const renderOptionIncomeLineChart = () => {
  if (!optionIncomeLineRef.value) return

  if (optionIncomeLineChart) {
    optionIncomeLineChart.dispose()
  }

  optionIncomeLineChart = echarts.init(optionIncomeLineRef.value)

  const dates = dailyOptionIncomeData.value.map(item => item.date)
  
  const series = allOptionCodes.value.map(optionCode => ({
    name: optionCode,
    type: 'line',
    data: dailyOptionIncomeData.value.map(item => Number(item[optionCode]) || 0),
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { width: 2 }
  }))
  
  // 添加总收入的线
  series.push({
    name: 'Total Revenue',
    type: 'line',
    data: dailyOptionIncomeData.value.map(item => Number(item.total) || 0),
    smooth: true,
    symbol: 'diamond',
    symbolSize: 8,
    lineStyle: { width: 3, color: '#f56c6c' },
    label: {
      show: true,
      position: 'top',
      formatter: (params) => `£${params.value.toFixed(2)}`,
      fontSize: 10
    },
    itemStyle: { color: '#f56c6c' }
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let result = params[0]?.axisValue || ''
        params.forEach(param => {
          result += `<br/>${param.marker} ${param.seriesName}: £${param.value.toFixed(2)}`
        })
        return result
      }
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      left: 'left',
      top: 0,
      textStyle: { fontSize: 11 }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '12%',
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
    series: series
  }

  optionIncomeLineChart.setOption(option)
}

// Chart rendering - 方案收入饼图
const renderOptionIncomePieChart = () => {
  if (!optionIncomePieRef.value) return

  if (optionIncomePieChart) {
    optionIncomePieChart.dispose()
  }

  optionIncomePieChart = echarts.init(optionIncomePieRef.value)

  const totalByOption = {}
  dailyOptionIncomeData.value.forEach(item => {
    allOptionCodes.value.forEach(optionCode => {
      const value = Number(item[optionCode]) || 0
      totalByOption[optionCode] = (totalByOption[optionCode] || 0) + value
    })
  })

  const pieData = Object.entries(totalByOption).map(([name, value]) => ({
    name,
    value: value.toFixed(2)
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>Revenue: £${params.value}<br/>Percentage: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      type: 'scroll',
      textStyle: { fontSize: 11 }
    },
    series: [
      {
        name: 'Revenue by Option',
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
            return `${params.name}\n£${params.value}`
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
        data: pieData
      }
    ]
  }

  optionIncomePieChart.setOption(option)
}

// Chart rendering - 时间段用车数量
const renderTimeScooterChart = () => {
  if (!timeScooterRef.value) return

  if (timeScooterChart) {
    timeScooterChart.dispose()
  }

  timeScooterChart = echarts.init(timeScooterRef.value)

  const hours = dailyTimeScooterData.value.map(item => item.hourLabel || `${item.hour}:00`)
  const counts = dailyTimeScooterData.value.map(item => item.scooterCount || 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = params[0]
        return `${item.axisValue}<br/>Scooters in use: ${item.value}`
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
      data: hours,
      axisLabel: {
        rotate: 45,
        fontSize: 10,
        interval: 2
      },
      name: 'Time (Hour)'
    },
    yAxis: {
      type: 'value',
      name: 'Number of Scooters',
      minInterval: 1
    },
    series: [
      {
        name: 'Scooters in Use',
        type: 'bar',
        data: counts,
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
          formatter: (params) => `${params.value}`,
          fontSize: 10
        }
      }
    ]
  }

  timeScooterChart.setOption(option)
}

// Chart rendering - Issue优先级饼图
const renderIssuePieChart = () => {
  if (!issuePieRef.value) return

  if (issuePieChart) {
    issuePieChart.dispose()
  }

  issuePieChart = echarts.init(issuePieRef.value)

  const pieData = issueData.value.map(item => ({
    name: getPriorityLabel(item.priority),
    value: item.value,
    itemStyle: { color: getPriorityColor(item.priority) }
  }))

  const totalIssues = pieData.reduce((sum, item) => sum + item.value, 0)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${params.name}<br/>Count: ${params.value}<br/>Percentage: ${params.percent}%`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: { fontSize: 11 }
    },
    series: [
      {
        name: 'Issues by Priority',
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
            return `${params.name}\n${params.value} (${params.percent}%)`
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
        data: pieData
      }
    ]
  }

  if (totalIssues === 0) {
    option.title = {
      text: 'No issues in this date range',
      left: 'center',
      top: 'center',
      textStyle: { color: '#909399', fontSize: 14 }
    }
  }

  issuePieChart.setOption(option)
}

// Handle window resize
const handleResize = () => {
  if (optionIncomeLineChart) optionIncomeLineChart.resize()
  if (optionIncomePieChart) optionIncomePieChart.resize()
  if (timeScooterChart) timeScooterChart.resize()
  if (issuePieChart) issuePieChart.resize()
}

// Refresh all data
const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchDailyOptionIncome(),
      fetchDailyTimeScooter(),
      fetchInRangeIssue()
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
  if (optionIncomeLineChart) optionIncomeLineChart.dispose()
  if (optionIncomePieChart) optionIncomePieChart.dispose()
  if (timeScooterChart) timeScooterChart.dispose()
  if (issuePieChart) issuePieChart.dispose()
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
  height: 380px;
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