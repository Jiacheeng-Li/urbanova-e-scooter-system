<template>
  <div class="urbanova-container">
    <!-- 动态星空背景层 -->
    <div class="stars-bg"></div>
    <div class="twinkling-bg"></div>
    
    <!-- 主内容区域 -->
    <div class="content-wrapper">
      <!-- Hero 宣传区 -->
      <section class="hero-section">
        <div class="hero-content" data-aos="fade-up">
          <h1 class="hero-title">
            <span class="title-gradient">Urbanova</span>
            <span class="title-accent">Urban Mobility,</span>
            <span class="title-accent-last">Styled for You.</span>
          </h1>
          <p class="hero-description">
            Experience the future of city travel — lightweight, intelligent, and eco-friendly. 
            Glide through the city under a starry sky with Urbanova.
          </p>
          <div class="hero-buttons">
            <button class="btn-primary" @click="scrollToPricing">
              Explore Plans
              <span class="btn-icon">✨</span>
            </button>
            <button class="btn-secondary" @click="openDialog">
              Download App
              <span class="btn-icon">📱</span>
            </button>
          </div>
        </div>
        <div class="hero-stats" data-aos="fade-up" data-aos-delay="200">
          <div class="stat-item">
            <span class="stat-number">15k+</span>
            <span class="stat-label">Happy Riders</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">4.9</span>
            <span class="stat-label">App Rating</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">24/7</span>
            <span class="stat-label">Support</span>
          </div>
        </div>
      </section>

      <!-- 价格表区域 -->
      <section class="pricing-section" id="pricing-section">
        <div class="section-header" data-aos="fade-up">
          <h2>Choose Your Ride</h2>
          <p>Flexible plans for every rider — from tourists to daily commuters</p>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>Loading plans...</p>
        </div>

        <!-- 价格卡片网格 - 4个卡片同一行 -->
        <div v-else class="pricing-grid">
          <div 
            v-for="(plan, index) in hireOptions" 
            :key="plan.hireOptionId"
            class="pricing-card"
            :data-aos="'fade-up'"
            :data-aos-delay="index * 100"
          >
            <div class="card-header">
              <div class="plan-icon">{{ getPlanIcon(plan.code) }}</div>
              <h3>{{ plan.code }}</h3>
              <p class="plan-desc">{{ getPlanDescription(plan.durationMinutes) }}</p>
            </div>
            <div class="card-price">
              <span class="currency">£</span>
              <span class="price">{{ plan.basePrice.toFixed(2) }}</span>
              <span class="period">{{ formatDuration(plan.durationMinutes) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 功能亮点区 -->
      <section class="features-section">
        <div class="section-header" data-aos="fade-up">
          <h2>Why Urbanova?</h2>
          <p>Beyond ordinary sharing, defining a new urban lifestyle</p>
        </div>
        <div class="features-grid">
          <div class="feature-card" v-for="(feature, idx) in features" :key="idx" data-aos="zoom-in" :data-aos-delay="idx * 100">
            <div class="feature-icon-large">{{ feature.icon }}</div>
            <h4>{{ feature.title }}</h4>
            <p>{{ feature.description }}</p>
          </div>
        </div>
      </section>
    </div>

    <!-- 右下角弹窗 - 二维码下载 -->
    <div class="qrcode-dialog">
      <div class="qrcode-trigger" @click="toggleDialog">
        <span class="app-icon">📱</span>
        <span>Download App</span>
      </div>

      <transition name="fade">
        <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
          <div class="dialog-content" @click.stop>
            <div class="dialog-header">
              <h3>Scan to Download App</h3>
              <button class="close-btn" @click="closeDialog">×</button>
            </div>
            <div class="qrcode-wrapper">
              <img src="/download.png" alt="Download QR Code" class="qrcode-image">
              <p class="qrcode-tip">Scan QR code with your phone to download</p>
            </div>
            <div class="app-stores">
              <p>Or search "Urbanova" in App Store</p>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import AOS from 'aos'
import 'aos/dist/aos.css'
import { hireOptionsApi } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'Urbanova',
  data() {
    return {
      dialogVisible: false,
      loading: false,
      hireOptions: [],
      features: [
        {
          icon: '⚡',
          title: 'Fast & Efficient',
          description: 'Lightweight scooters with up to 40km range, covering any distance in the city.'
        },
        {
          icon: '🔒',
          title: 'Safe & Secure',
          description: 'GPS tracking, anti-theft system, and 24/7 customer support for peace of mind.'
        },
        {
          icon: '🌿',
          title: 'Eco-Friendly',
          description: 'Zero emissions, helping cities breathe easier. Ride green with Urbanova.'
        },
        {
          icon: '🎨',
          title: 'Stylish Design',
          description: 'Sleek, modern aesthetics that match your personal style and city vibe.'
        }
      ]
    }
  },
  mounted() {
    AOS.init({
      duration: 800,
      once: true,
      offset: 100
    })
    this.fetchHireOptions()
  },
  methods: {
    async fetchHireOptions() {
      this.loading = true
      try {
        const response = await hireOptionsApi.list()
        this.hireOptions = response.data.data || response.data || []
      } catch (error) {
        console.error('Failed to fetch hire options:', error)
        ElMessage.error('Failed to fetch rental plans')
        this.hireOptions = this.getFallbackOptions()
      } finally {
        this.loading = false
      }
    },
    getFallbackOptions() {
      return [
        { hireOptionId: '1', code: 'PAYG', basePrice: 1.50, durationMinutes: 30 },
        { hireOptionId: '2', code: 'DAY', basePrice: 12.00, durationMinutes: 1440 },
        { hireOptionId: '3', code: 'MONTHLY', basePrice: 45.00, durationMinutes: 43200 },
        { hireOptionId: '4', code: 'WEEKLY', basePrice: 25.00, durationMinutes: 10080 }
      ]
    },
    formatDuration(minutes) {
      if (minutes < 60) {
        return `/${minutes} min`
      } else if (minutes === 60) {
        return '/hour'
      } else if (minutes < 1440) {
        const hours = Math.floor(minutes / 60)
        return `/${hours} hours`
      } else if (minutes === 1440) {
        return '/day'
      } else if (minutes < 43200) {
        const days = Math.floor(minutes / 1440)
        return `/${days} days`
      } else {
        return '/month'
      }
    },
    getPlanIcon(code) {
      const icons = {
        'PAYG': '🛴',
        'DAY': '🌞',
        'MONTHLY': '🌙',
        'WEEKLY': '📅',
        'H1': '🛴',
        'H2': '⚡',
        'H3': '🌟',
        'H4': '💎'
      }
      return icons[code] || '🛴'
    },
    getPlanDescription(minutes) {
      if (minutes <= 30) return 'Perfect for short trips'
      if (minutes <= 60) return 'Ideal for hourly use'
      if (minutes <= 1440) return 'Full day of access'
      return 'Best value for long-term'
    },
    toggleDialog() {
      this.dialogVisible = !this.dialogVisible
    },
    closeDialog() {
      this.dialogVisible = false
    },
    openDialog() {
      this.dialogVisible = true
    },
    scrollToPricing() {
      const pricingSection = document.getElementById('pricing-section')
      if (pricingSection) {
        pricingSection.scrollIntoView({ behavior: 'smooth' })
      }
    }
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.urbanova-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a2a 0%, #1a1a3a 50%, #2a1a4a 100%);
  overflow-x: hidden;
}

/* 动态星空背景 */
.stars-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPjxkZWZzPjxwYXR0ZXJuIGlkPSJhIiB3aWR0aD0iNTAiIGhlaWdodD0iNTAiIHBhdHRlcm5Vbml0cz0idXNlclNwYWNlT25Vc2UiIHBhdHRlcm5UcmFuc2Zvcm09InJvdGF0ZSg0NSkiPjxjaXJjbGUgY3g9IjI1IiBjeT0iMjUiIHI9IjEiIGZpbGw9IndoaXRlIiBmaWxsLW9wYWNpdHk9IjAuMyIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNhKSIvPjwvc3ZnPg==') repeat;
  pointer-events: none;
  z-index: 0;
  animation: starMove 60s linear infinite;
}

.twinkling-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPjxkZWZzPjxwYXR0ZXJuIGlkPSJiIiB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PGNpcmNsZSBjeD0iNTAiIGN5PSI1MCIgcj0iMS41IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjUiLz48Y2lyY2xlIGN4PSIyMCIgY3k9IjgwIiByPSIwLjgiIGZpbGw9IndoaXRlIiBmaWxsLW9wYWNpdHk9IjAuNCIvPjxjaXJjbGUgY3g9IjcwIiBjeT0iMTUiIHI9IjEiIGZpbGw9IndoaXRlIiBmaWxsLW9wYWNpdHk9IjAuNiIvPjwvcGF0dGVybj48L2RlZnM+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNiKSIvPjwvc3ZnPg==') repeat;
  pointer-events: none;
  z-index: 0;
  animation: twinkle 4s ease-in-out infinite;
}

@keyframes starMove {
  0% { transform: translateX(0) translateY(0); }
  100% { transform: translateX(-50px) translateY(-50px); }
}

@keyframes twinkle {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.content-wrapper {
  position: relative;
  z-index: 2;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 5%;
}

.hero-section {
  min-height: 90vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 100px 0 60px;
}

.hero-content {
  max-width: 800px;
}

.hero-title {
  font-size: clamp(2.5rem, 6vw, 5rem);
  font-weight: 700;
  margin-bottom: 24px;
  line-height: 1.2;
}

.title-gradient {
  background: linear-gradient(135deg, #c77dff, #a855f7, #7c3aed, #c77dff);
  background-size: 300% 300%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: gradientShift 5s ease infinite;
  display: block;
}

.title-accent {
  display: block;
  color: rgba(255, 255, 255, 0.9);
}

.title-accent-last {
  display: block;
  background: linear-gradient(135deg, #e9d5ff, #d8b4fe);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.hero-description {
  font-size: clamp(1rem, 2vw, 1.25rem);
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
  margin-bottom: 36px;
}

.hero-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary, .btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 32px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 50px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: white;
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.4);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(124, 58, 237, 0.6);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(168, 85, 247, 0.5);
}

.btn-secondary:hover {
  background: rgba(168, 85, 247, 0.3);
  transform: translateY(-3px);
}

.btn-icon {
  font-size: 1.2rem;
}

.hero-stats {
  display: flex;
  gap: 48px;
  margin-top: 80px;
  flex-wrap: wrap;
  justify-content: center;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #c77dff, #a855f7);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  color: rgba(255, 255, 255, 0.7);
  gap: 20px;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(168, 85, 247, 0.3);
  border-top-color: #a855f7;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.pricing-section {
  padding: 80px 0;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: clamp(1.8rem, 4vw, 2.5rem);
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #d8b4fe);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 16px;
}

.section-header p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 1.1rem;
}

/* ========== 关键修改：4个卡片同一行 ========== */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 平板：显示2个一行 */
@media (max-width: 1024px) {
  .pricing-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }
}

/* 手机：显示1个一行 */
@media (max-width: 640px) {
  .pricing-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
/* ========== 修改结束 ========== */

.pricing-card {
  background: linear-gradient(135deg, rgba(100, 50, 150, 0.8), rgba(60, 30, 100, 0.8));
  backdrop-filter: blur(15px);
  border-radius: 28px;
  padding: 32px 24px;
  transition: all 0.4s ease;
  border: 1px solid #a855f7;
  position: relative;
  overflow: hidden;
}

.pricing-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #a855f7, #c77dff, transparent);
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.pricing-card:hover::before {
  transform: translateX(100%);
}

.pricing-card:hover {
  transform: translateY(-10px);
  border-color: #c77dff;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.plan-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}

.pricing-card h3 {
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
}

.plan-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.875rem;
}

.card-price {
  margin: 24px 0 0;
  text-align: center;
  border-top: 1px solid rgba(168, 85, 247, 0.3);
  border-bottom: none;
  padding: 20px 0 8px;
}

.currency {
  font-size: 1.5rem;
  font-weight: 600;
  color: #c77dff;
  vertical-align: top;
}

.price {
  font-size: 3.5rem;
  font-weight: 800;
  color: white;
}

.period {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
}

.features-section {
  padding: 80px 0 120px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: rgba(30, 20, 60, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 32px 24px;
  text-align: center;
  transition: all 0.3s ease;
  border: 1px solid rgba(168, 85, 247, 0.15);
}

.feature-card:hover {
  transform: translateY(-5px);
  background: rgba(30, 20, 60, 0.6);
  border-color: rgba(168, 85, 247, 0.3);
}

.feature-icon-large {
  font-size: 3rem;
  margin-bottom: 20px;
}

.feature-card h4 {
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
  margin-bottom: 12px;
}

.feature-card p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.875rem;
  line-height: 1.5;
}

.qrcode-dialog {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 100;
}

.qrcode-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: white;
  padding: 14px 24px;
  border-radius: 50px;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.4);
  transition: all 0.3s ease;
  font-weight: 500;
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.qrcode-trigger:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(124, 58, 237, 0.5);
}

.app-icon {
  font-size: 22px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(30, 20, 60, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(245, 240, 255, 0.98));
  border-radius: 24px;
  width: 360px;
  max-width: 90%;
  padding: 28px 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
  border: 1px solid #a855f7;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.dialog-header h3 {
  font-size: 20px;
  color: #4B0082;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 32px;
  cursor: pointer;
  color: #a855f7;
  transition: all 0.2s;
}

.close-btn:hover {
  color: #7c3aed;
  transform: scale(1.1);
}

.qrcode-wrapper {
  text-align: center;
}

.qrcode-image {
  width: 200px;
  height: 200px;
  margin: 10px 0;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(168, 85, 247, 0.2);
  border: 2px solid #a855f7;
}

.qrcode-tip {
  color: #6A0DAD;
  font-size: 14px;
  margin-top: 16px;
  font-weight: 500;
}

.app-stores {
  text-align: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(168, 85, 247, 0.3);
  color: #a855f7;
  font-size: 13px;
  font-weight: 500;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .content-wrapper {
    padding: 0 20px;
  }
  
  .hero-section {
    padding: 60px 0 40px;
  }
  
  .hero-buttons {
    gap: 12px;
  }
  
  .btn-primary, .btn-secondary {
    padding: 10px 20px;
    font-size: 0.875rem;
  }
  
  .hero-stats {
    gap: 30px;
    margin-top: 50px;
  }
  
  .pricing-section,
  .features-section {
    padding: 50px 0;
  }
  
  .qrcode-dialog {
    bottom: 20px;
    right: 20px;
  }
  
  .qrcode-trigger {
    padding: 10px 18px;
    font-size: 14px;
  }
  
  .app-icon {
    font-size: 18px;
  }
  
  .dialog-content {
    width: 300px;
    padding: 20px;
  }
}

@media (max-width: 480px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
}
</style>