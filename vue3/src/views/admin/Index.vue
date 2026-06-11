<template>
  <div class="admin-home">
    <!-- 库存预警 -->
    <el-alert
      v-if="inventoryWarning.hasWarning"
      :title="`库存预警：${inventoryWarning.lowStockCount} 本低库存，${inventoryWarning.outOfStockCount} 本缺货`"
      type="warning"
      :closable="false"
      show-icon
      class="inventory-alert"
    >
      <template #default>
        <div class="warning-content">
          <div v-if="inventoryWarning.outOfStockBooks?.length > 0" class="warning-section">
            <strong>缺货图书：</strong>
            <el-tag v-for="book in inventoryWarning.outOfStockBooks" :key="book.id" type="danger" size="small" class="book-tag">
              {{ book.title }}
            </el-tag>
          </div>
          <div v-if="inventoryWarning.lowStockBooks?.length > 0" class="warning-section">
            <strong>低库存图书：</strong>
            <el-tag v-for="book in inventoryWarning.lowStockBooks" :key="book.id" type="warning" size="small" class="book-tag">
              {{ book.title }} (剩余 {{ book.available }})
            </el-tag>
          </div>
        </div>
      </template>
    </el-alert>

    <!-- 统计概览 -->
    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-icon users-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="9" cy="7" r="4"></circle>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ userCount }}</span>
          <span class="stat-label">普通用户</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon books-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ bookCount }}</span>
          <span class="stat-label">图书数量</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon borrows-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
            <line x1="1" y1="10" x2="23" y2="10"></line>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ borrowCount }}</span>
          <span class="stat-label">借阅记录</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon system-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"></circle>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1 1.51z"></path>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value system-status">正常运行</span>
          <span class="stat-label">系统状态</span>
        </div>
      </div>
    </div>

    <!-- 价格统计 -->
    <el-card shadow="never" class="price-stats-card">
      <template #header><span>价格统计</span></template>
      <div class="price-stats">
        <div class="price-stat"><span class="label">最高价</span><span class="value">¥{{ priceStats.max }}</span></div>
        <div class="price-stat"><span class="label">最低价</span><span class="value">¥{{ priceStats.min }}</span></div>
        <div class="price-stat"><span class="label">平均价</span><span class="value">¥{{ priceStats.avg }}</span></div>
        <div class="price-stat"><span class="label">图书总数</span><span class="value">{{ priceStats.count }} 本</span></div>
      </div>
    </el-card>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 快速操作 -->
      <div class="quick-actions-card">
        <div class="card-header">
          <h3>快速操作</h3>
        </div>
        <div class="action-grid">
          <div class="action-item" @click="$router.push('/admin/user-management')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <div class="action-content">
              <h4>用户管理</h4>
              <p>管理普通用户账户</p>
            </div>
          </div>

          <div class="action-item" @click="$router.push('/admin/books')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
            </div>
            <div class="action-content">
              <h4>图书管理</h4>
              <p>维护图书信息</p>
            </div>
          </div>

          <div class="action-item" @click="$router.push('/admin/ebooks')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                <line x1="8" y1="21" x2="16" y2="21"></line>
                <line x1="12" y1="17" x2="12" y2="21"></line>
              </svg>
            </div>
            <div class="action-content">
              <h4>电子书管理</h4>
              <p>管理电子书资源</p>
            </div>
          </div>

          <div class="action-item" @click="$router.push('/admin/borrows')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 11 12 14 22 4"></polyline>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
              </svg>
            </div>
            <div class="action-content">
              <h4>借阅管理</h4>
              <p>处理借阅申请</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 管理员信息 -->
      <div class="admin-info-card">
        <div class="card-header">
          <h3>管理员信息</h3>
        </div>
        <div class="info-content">
          <div class="info-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <div class="info-details">
            <h4>{{ authStore.userInfo?.name }}</h4>
            <span class="role-badge admin">管理员</span>
            <p>您拥有管理普通用户和图书信息的权限，请谨慎操作。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { userApi } from '../../api/user'
import { bookApi } from '../../api/book'
import { useBookStore } from '../../stores/book'
import { useBorrowStore } from '../../stores/borrow'

const authStore = useAuthStore()
const bookStore = useBookStore()
const borrowStore = useBorrowStore()

const userCount = ref(0)
const bookCount = ref(0)
const borrowCount = ref(0)
const inventoryWarning = ref({
  hasWarning: false,
  lowStockCount: 0,
  outOfStockCount: 0,
  lowStockBooks: [],
  outOfStockBooks: []
})

// 价格统计
const priceStats = computed(() => {
  const prices = bookStore.books.map(b => Number(b.price)).filter(p => p > 0)
  return {
    max: prices.length ? Math.max(...prices).toFixed(2) : '0.00',
    min: prices.length ? Math.min(...prices).toFixed(2) : '0.00',
    avg: prices.length ? (prices.reduce((a, b) => a + b, 0) / prices.length).toFixed(2) : '0.00',
    count: prices.length
  }
})

onMounted(async () => {
  try {
    const res = await userApi.getAllUsers()
    if (res.data.code === 200) {
      userCount.value = res.data.data?.length || 0
    }
    await bookStore.fetchBooks()
    bookCount.value = bookStore.books.length
    await borrowStore.fetchAllBorrows()
    borrowCount.value = borrowStore.borrows.length

    // 获取库存预警
    try {
      const warningRes = await bookApi.getInventoryWarning()
      if (warningRes.data.code === 200) {
        inventoryWarning.value = warningRes.data.data
      }
    } catch (error) {
      console.error('获取库存预警失败', error)
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
})
</script>

<style scoped>
.inventory-alert {
  margin-bottom: 20px;
  border-radius: 12px;
}

.warning-content {
  margin-top: 12px;
}

.warning-section {
  margin-bottom: 8px;
}

.warning-section:last-child {
  margin-bottom: 0;
}

.book-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.admin-home {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 统计概览 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 28px;
  height: 28px;
}

.users-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.1) 100%);
  color: #60a5fa;
}

.books-icon {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(16, 185, 129, 0.1) 100%);
  color: #34d399;
}

.borrows-icon {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.1) 100%);
  color: #fbbf24;
}

.system-icon {
  background: linear-gradient(135deg, rgba(148, 163, 184, 0.15) 0%, var(--border-color) 100%);
  color: var(--text-secondary);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.system-status {
  font-size: 16px;
  color: #059669;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 主内容区 */
.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.price-stats-card { margin-bottom: 20px; }
.price-stats-card :deep(.el-card__body) { padding: 16px 20px; }
.price-stats { display: flex; gap: 24px; }
.price-stat { display: flex; flex-direction: column; gap: 4px; }
.price-stat .label { font-size: 13px; color: var(--text-secondary); }
.price-stat .value { font-size: 20px; font-weight: 700; color: var(--text-primary); }

/* 快速操作卡片 */
.quick-actions-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.card-header {
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-glass);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-item:hover {
  background: var(--bg-hover);
}

.action-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.action-icon svg {
  width: 24px;
  height: 24px;
}

.action-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.action-content p {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 管理员信息卡片 */
.admin-info-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.info-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 16px;
}

.info-avatar {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.info-avatar svg {
  width: 40px;
  height: 40px;
}

.info-details h4 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.role-badge.admin {
  background: rgba(245, 158, 11, 0.12);
  color: #fbbf24;
}

.info-details p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 12px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .main-content {
    grid-template-columns: 1fr;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }
}
</style>