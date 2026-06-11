<template>
  <div class="super-admin-home">
    <!-- 统计概览 -->
    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-icon admins-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <line x1="20" y1="8" x2="20" y2="14"></line>
            <line x1="23" y1="11" x2="17" y2="11"></line>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ adminCount }}</span>
          <span class="stat-label">管理员数量</span>
        </div>
        <div class="stat-trend positive">
          <span>+2</span>
          <span>本周新增</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon audit-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ auditCount }}</span>
          <span class="stat-label">审计日志</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon login-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ loginCount }}</span>
          <span class="stat-label">今日登录</span>
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
          <span class="stat-value system-status">
            <span class="status-dot"></span>
            运行正常
          </span>
          <span class="stat-label">系统状态</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 快速操作 -->
      <div class="operations-card">
        <div class="card-header">
          <h3>系统操作</h3>
          <span class="header-badge super">超级权限</span>
        </div>
        <div class="action-grid">
          <div class="action-item" @click="$router.push('/super-admin/admin-management')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="8.5" cy="7" r="4"></circle>
                <line x1="20" y1="8" x2="20" y2="14"></line>
                <line x1="23" y1="11" x2="17" y2="11"></line>
              </svg>
            </div>
            <div class="action-content">
              <h4>管理员管理</h4>
              <p>管理所有管理员账户</p>
            </div>
            <div class="action-arrow">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>

          <div class="action-item" @click="$router.push('/super-admin/audit-logs')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <div class="action-content">
              <h4>审计日志</h4>
              <p>查看系统操作记录</p>
            </div>
            <div class="action-arrow">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>

          <div class="action-item" @click="$router.push('/super-admin/login-logs')">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                <polyline points="10 17 15 12 10 7"></polyline>
                <line x1="15" y1="12" x2="3" y2="12"></line>
              </svg>
            </div>
            <div class="action-content">
              <h4>登录监控</h4>
              <p>监控用户登录行为</p>
            </div>
            <div class="action-arrow">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 系统信息 -->
      <div class="system-info-card">
        <div class="card-header">
          <h3>系统监控</h3>
        </div>
        <div class="system-stats">
          <div class="stat-row">
            <span class="stat-name">服务器时间</span>
            <span class="stat-data">{{ currentTime }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-name">运行状态</span>
            <span class="stat-data status-tag success">正常运行</span>
          </div>
          <div class="stat-row">
            <span class="stat-name">系统版本</span>
            <span class="stat-data">v2.0.0</span>
          </div>
          <div class="stat-row">
            <span class="stat-name">在线用户</span>
            <span class="stat-data status-tag primary">{{ onlineUsers }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-name">数据库连接</span>
            <span class="stat-data status-tag success">正常</span>
          </div>
          <div class="stat-row">
            <span class="stat-name">API响应时间</span>
            <span class="stat-data">~120ms</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 超级管理员信息 -->
    <div class="admin-banner">
      <div class="banner-content">
        <div class="banner-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
        </div>
        <div class="banner-text">
          <h4>超级管理员：{{ authStore.userInfo?.name }}</h4>
          <p>您拥有系统最高权限，可管理所有管理员账户并查看系统日志，请谨慎操作。</p>
        </div>
      </div>
      <div class="banner-badge">
        <span>最高权限</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { adminApi } from '../../api/admin'

const authStore = useAuthStore()
const adminCount = ref(0)
const auditCount = ref(0)
const loginCount = ref(0)
const onlineUsers = ref(0)
const currentTime = ref('')

let timeInterval = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(async () => {
  try {
    const statsRes = await adminApi.getDashboardStats()
    if (statsRes.data.code === 200) {
      const data = statsRes.data.data || {}
      adminCount.value = data.adminCount || 0
      auditCount.value = data.auditCount || 0
      loginCount.value = data.todayLogins || 0
      onlineUsers.value = data.onlineUsers || 0
    }
  } catch (error) {
    console.error('获取仪表盘数据失败', error)
  }

  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.super-admin-home {
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
  position: relative;
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

.admins-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.1) 100%);
  color: #60a5fa;
}

.audit-icon {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(16, 185, 129, 0.1) 100%);
  color: #34d399;
}

.login-icon {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.1) 100%);
  color: #fbbf24;
}

.system-icon {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2) 0%, rgba(239, 68, 68, 0.1) 100%);
  color: #f87171;
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
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #059669;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.stat-trend {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.stat-trend.positive {
  color: #059669;
}

.stat-trend span:first-child {
  font-size: 14px;
  font-weight: 600;
}

.stat-trend span:last-child {
  font-size: 12px;
  opacity: 0.8;
}

/* 主内容区 */
.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

/* 操作卡片 */
.operations-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.card-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-badge {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.header-badge.super {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
}

.action-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-glass);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-item:hover {
  background: var(--bg-hover);
}

.action-item:hover .action-arrow {
  color: #dc2626;
}

.action-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
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

.action-content {
  flex: 1;
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

.action-arrow {
  width: 24px;
  height: 24px;
  color: var(--text-muted);
}

/* 系统信息卡片 */
.system-info-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.system-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-glass);
  border-radius: 8px;
}

.stat-name {
  font-size: 14px;
  color: var(--text-secondary);
}

.stat-data {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.success {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
}

.status-tag.primary {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
}

/* 超级管理员横幅 */
.admin-banner {
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
  border-radius: 16px;
  padding: 24px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.banner-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-icon svg {
  width: 28px;
  height: 28px;
}

.banner-text h4 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.banner-text p {
  font-size: 14px;
  opacity: 0.9;
}

.banner-badge {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 1024px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .main-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }

  .admin-banner {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .banner-content {
    flex-direction: column;
  }
}
</style>