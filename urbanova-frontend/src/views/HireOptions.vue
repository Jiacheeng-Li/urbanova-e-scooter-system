<template>
  <div class="hire-options-container">
    <h1 class="page-title">租赁选项</h1>
    <p class="page-description">选择适合您的租赁时长和价格</p>

    <el-row :gutter="20" v-loading="loading">
      <el-col
        v-for="option in hireOptions"
        :key="option.hireOptionId"
        :xs="24"
        :sm="12"
        :md="6"
      >
        <el-card class="option-card" shadow="hover" @click="selectOption(option)">
          <div class="option-header">
            <el-tag size="large" type="primary">{{ option.code }}</el-tag>
            <span class="duration">{{ formatDuration(option.durationMinutes) }}</span>
          </div>

          <div class="option-price">
            <span class="currency">£</span>
            <span class="amount">{{ option.basePrice.toFixed(2) }}</span>
          </div>

          <div class="option-features">
            <el-button type="primary" size="small" class="select-button">
              查看详情
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 价格报价对话框 -->
    <el-dialog
      v-model="quoteDialogVisible"
      :title="`${selectedOption?.code} - 价格详情`"
      width="400px"
    >
      <div v-if="selectedOption" class="quote-details">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="租赁类型">
            {{ selectedOption.code }}
          </el-descriptions-item>
          <el-descriptions-item label="时长">
            {{ formatDuration(selectedOption.durationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="基础价格">
            £{{ selectedOption.basePrice.toFixed(2) }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="quoteResult" class="quote-result">
          <el-divider>价格报价</el-divider>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="基础价格">
              £{{ quoteResult.basePrice.toFixed(2) }}
            </el-descriptions-item>
            <el-descriptions-item label="折扣">
              -£{{ quoteResult.appliedDiscounts.length > 0 ?
                quoteResult.appliedDiscounts.reduce((sum, d) => sum + d.amount, 0).toFixed(2) : '0.00' }}
            </el-descriptions-item>
            <el-descriptions-item label="最终价格">
              <strong>£{{ quoteResult.finalPrice.toFixed(2) }}</strong>
            </el-descriptions-item>
            <el-descriptions-item label="货币">
              {{ quoteResult.currency }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <template #footer>
        <el-button @click="quoteDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="quoting" @click="getQuote">
          获取报价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { hireOptionsApi } from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const hireOptions = ref([])
const quoteDialogVisible = ref(false)
const selectedOption = ref(null)
const quoteResult = ref(null)
const quoting = ref(false)

const formatDuration = (minutes) => {
  if (minutes < 60) {
    return `${minutes} 分钟`
  } else if (minutes === 60) {
    return '1 小时'
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    return `${hours} 小时`
  } else {
    const days = Math.floor(minutes / 1440)
    return `${days} 天`
  }
}

const fetchHireOptions = async () => {
  loading.value = true
  try {
    const response = await hireOptionsApi.list()
    hireOptions.value = response.data.data
  } catch (error) {
    ElMessage.error('获取租赁选项失败')
  } finally {
    loading.value = false
  }
}

const selectOption = (option) => {
  selectedOption.value = option
  quoteResult.value = null
  quoteDialogVisible.value = true
}

const getQuote = async () => {
  if (!selectedOption.value) return

  quoting.value = true
  try {
    const response = await hireOptionsApi.quotePrice({
      hireOptionCode: selectedOption.value.code
    })
    quoteResult.value = response.data.data
    ElMessage.success('报价获取成功')
  } catch (error) {
    ElMessage.error('获取报价失败')
  } finally {
    quoting.value = false
  }
}

onMounted(() => {
  fetchHireOptions()
})
</script>

<style scoped>
.hire-options-container {
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

.option-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.option-card:hover {
  transform: translateY(-4px);
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.duration {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.option-price {
  text-align: center;
  margin: 20px 0;
}

.currency {
  font-size: 20px;
  color: #67c23a;
  vertical-align: top;
}

.amount {
  font-size: 36px;
  font-weight: bold;
  color: #67c23a;
}

.option-features {
  text-align: center;
}

.select-button {
  width: 100%;
}

.quote-details {
  padding: 0 10px;
}

.quote-result {
  margin-top: 20px;
}
</style>
