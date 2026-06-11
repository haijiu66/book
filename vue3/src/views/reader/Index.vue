<template>
  <div class="reader-home">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-banner">
        <div class="welcome-content">
          <h1>欢迎，{{ authStore.userInfo?.name }}！</h1>
          <p>今天是{{ todayStr }}，祝你阅读愉快</p>
        </div>
        <div class="welcome-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
          </svg>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <div class="action-card" @click="$router.push('/books')">
        <div class="action-icon books-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <div class="action-content">
          <h3>浏览图书</h3>
          <p>在线书库 + 借阅书库，按分类筛选</p>
        </div>
        <div class="action-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="action-card" @click="$router.push('/ebooks')">
        <div class="action-icon ebooks-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
            <line x1="8" y1="21" x2="16" y2="21"></line>
            <line x1="12" y1="17" x2="12" y2="21"></line>
          </svg>
        </div>
        <div class="action-content">
          <h3>在线阅读</h3>
          <p>随时随地畅读电子书</p>
        </div>
        <div class="action-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>

      <div class="action-card" @click="$router.push('/borrow-history')">
        <div class="action-icon history-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="action-content">
          <h3>我的借阅</h3>
          <p>查看借阅历史记录</p>
        </div>
        <div class="action-arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <div class="stats-card">
        <div class="stats-header">
          <h3>阅读统计</h3>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalBorrows }}</div>
            <div class="stat-label">总借阅数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.activeBorrows }}</div>
            <div class="stat-label">当前借阅</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.returned }}</div>
            <div class="stat-label">已归还</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ stats.overdue }}</div>
            <div class="stat-label">逾期未还</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useBorrowStore } from '../../stores/borrow'

const authStore = useAuthStore()
const borrowStore = useBorrowStore()

const todayStr = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

const stats = ref({
  totalBorrows: 0,
  activeBorrows: 0,
  returned: 0,
  overdue: 0
})

onMounted(async () => {
  try {
    await borrowStore.fetchMyBorrows()
    const borrows = borrowStore.myBorrows || []
    stats.value.totalBorrows = borrows.length
    stats.value.activeBorrows = borrows.filter(b => b.status === 'BORROWED' || b.status === 'ACTIVE' || b.status === '借阅中').length
    stats.value.returned = borrows.filter(b => b.status === 'RETURNED' || b.status === '已归还').length
    stats.value.overdue = borrows.filter(b => b.status === 'OVERDUE' || b.status === '逾期').length
  } catch (e) {
    console.log('获取借阅统计失败')
  }
})
</script>

<style scoped>
.reader-home {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: 8px;
}

.welcome-banner {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 20px;
  padding: 32px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  transform: translate(50%, -50%);
}

.welcome-content h1 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.welcome-content p {
  font-size: 16px;
  opacity: 0.9;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-icon svg {
  width: 40px;
  height: 40px;
}

/* 快捷入口 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.action-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: transparent;
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon svg {
  width: 28px;
  height: 28px;
}

.books-icon {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #059669;
}

.ebooks-icon {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(139, 92, 246, 0.15) 100%);
  color: var(--accent-2);
}

.history-icon {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.1) 100%);
  color: #fbbf24;
}

.action-content {
  flex: 1;
}

.action-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.action-content p {
  font-size: 14px;
  color: var(--text-secondary);
}

.action-arrow {
  width: 24px;
  height: 24px;
  color: var(--text-muted);
}

.action-card:hover .action-arrow {
  color: #6366f1;
}

/* 统计信息 */
.stats-section {
  margin-top: 8px;
}

.stats-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.stats-header {
  margin-bottom: 20px;
}

.stats-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: var(--bg-glass);
  border-radius: 12px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #6366f1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 响应式 */
@media (max-width: 1024px) {
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .quick-actions {
    grid-template-columns: 1fr;
  }

  .welcome-banner {
    flex-direction: column;
    text-align: center;
    padding: 24px;
  }

  .welcome-icon {
    margin-top: 16px;
  }
}
</style>