<template>
  <div class="layout-container">
    <!-- 移动端遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="toggleSidebar"></div>

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="book-icon">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <h1 class="logo-text">智慧书苑</h1>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="menu in sidebarMenus"
          :key="menu.path"
          :to="menu.path"
          class="nav-item"
          :class="{ active: activeMenu === menu.path }"
          @click="closeSidebar"
        >
          <span class="nav-icon">
            <component :is="getIconComponent(menu.icon)" />
          </span>
          <span class="nav-text">{{ menu.title }}</span>
        </router-link>
      </nav>

      <!-- 超级管理员入口（非超级管理员可见） -->
      <div v-if="authStore.isAdmin" class="sa-sidebar-entry" @click="goToSuperAdminLogin">
        <div class="sa-sidebar-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
        </div>
        <div class="sa-sidebar-text">
          <span class="sa-sidebar-title">超级管理员</span>
          <span class="sa-sidebar-sub">系统管理入口</span>
        </div>
        <svg class="sa-sidebar-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
      </div>

      <div class="sidebar-footer">
        <div class="system-info">
          <span>图书馆管理系统</span>
          <span>v2.0</span>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部导航栏 -->
      <header class="header">
        <div class="header-left">
          <button class="hamburger-btn" @click="toggleSidebar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
          </button>
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <div class="user-info">
            <div class="user-avatar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="8" r="4"></circle>
                <path d="M20 21a8 8 0 0 0-16 0"></path>
              </svg>
            </div>
            <div class="user-details">
              <span class="user-name">{{ authStore.userInfo?.name }}</span>
              <span class="user-role" :class="getRoleClass()">{{ authStore.getRoleName() }}</span>
            </div>
          </div>
          <button class="logout-btn" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
            <span class="logout-text">退出</span>
          </button>
        </div>
      </header>

      <!-- 内容区 -->
      <div class="content-area">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, h, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled,
  User,
  UserFilled,
  Reading,
  Document,
  Tickets,
  Monitor,
  Clock,
  Search,
  Collection,
  TrendCharts,
  StarFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const sidebarOpen = ref(false)
const isMobile = ref(false)

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
  if (window.innerWidth >= 768) {
    sidebarOpen.value = false
  }
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize)
})

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const closeSidebar = () => {
  if (isMobile.value) {
    sidebarOpen.value = false
  }
}

const activeMenu = computed(() => route.path)

const sidebarMenus = computed(() => authStore.getSidebarMenus())

const pageTitle = computed(() => {
  return route.meta?.title || '智慧书苑'
})

const getIconComponent = (iconName) => {
  const icons = {
    HomeFilled: HomeFilled,
    User: User,
    UserFilled: UserFilled,
    Reading: Reading,
    Document: Document,
    Tickets: Tickets,
    Monitor: Monitor,
    Clock: Clock,
    Search: Search,
    Collection: Collection,
    TrendCharts: TrendCharts,
    StarFilled: StarFilled
  }
  return icons[iconName] || HomeFilled
}

const getRoleClass = () => {
  switch (authStore.userRole) {
    case 'SUPER_ADMIN':
      return 'role-super'
    case 'ADMIN':
      return 'role-admin'
    default:
      return 'role-reader'
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'custom-message-box'
    })
    await authStore.logout()
    ElMessage.success('退出成功')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出失败', error)
    }
  }
}

const goToSuperAdminLogin = async () => {
  try {
    await ElMessageBox.confirm(
      '即将跳转至超级管理员登录页面，当前账号将被登出。',
      '超级管理员登录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
        customClass: 'custom-message-box'
      }
    )
    await authStore.logout()
    ElMessage.success('已登出，正在跳转...')
    router.push('/super-admin/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('跳转失败', error)
    }
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
  background: transparent;
}

/* 移动端遮罩 */
.sidebar-overlay {
  display: none;
}

/* 汉堡菜单按钮 */
.hamburger-btn {
  display: none;
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-primary, #1e293b);
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  transition: background 0.2s ease;
}

.hamburger-btn:hover {
  background: var(--bg-hover);
}

.hamburger-btn svg {
  width: 22px;
  height: 22px;
}

/* 侧边栏 — 玻璃态 */
.sidebar {
  width: clamp(200px, 18vw, 260px);
  background: rgba(15, 15, 30, 0.85);
  backdrop-filter: blur(24px);
  border-right: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0; top: 0; bottom: 0;
  z-index: 100;
  transition: transform 0.35s cubic-bezier(0.4,0,0.2,1);
}

.sidebar-header {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
}

.logo-icon {
  width: 40px; height: 40px;
  background: var(--gradient-1);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
}

.book-icon { width: 22px; height: 22px; }

.logo-text {
  font-size: 18px; font-weight: 700;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-nav { flex: 1; padding: 12px; overflow-y: auto; }

.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; margin-bottom: 2px;
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition);
  text-decoration: none;
  position: relative;
}
.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}
.nav-item.active {
  background: rgba(99,102,241,0.12);
  color: var(--accent-2);
  box-shadow: inset 3px 0 0 var(--accent-1);
}

.nav-icon { width: 20px; height: 20px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.nav-text { font-size: 14px; font-weight: 500; }

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}
.system-info {
  display: flex; justify-content: space-between;
  color: var(--text-muted); font-size: 12px;
}

/* 主内容区 */
.main-content {
  flex: 1; margin-left: clamp(200px, 18vw, 260px);
  display: flex; flex-direction: column; min-height: 100vh;
  transition: margin-left 0.35s ease;
}

/* 顶部导航栏 — 玻璃态 */
.header {
  height: 64px;
  background: rgba(18,18,26,0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-color);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  position: sticky; top: 0; z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px; font-weight: 600;
  color: var(--text-primary);
}

.header-right { display: flex; align-items: center; gap: 16px; }

.user-info { display: flex; align-items: center; gap: 10px; }

.user-avatar {
  width: 36px; height: 36px;
  background: var(--gradient-1);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
}
.user-avatar svg { width: 18px; height: 18px; }

.user-details { display: flex; flex-direction: column; }
.user-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.user-role { font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }

.role-super { background: rgba(239,68,68,0.15); color: var(--accent-5); }
.role-admin { background: rgba(245,158,11,0.15); color: var(--accent-4); }
.role-reader { background: rgba(16,185,129,0.15); color: var(--accent-6); }

.logout-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary); font-size: 13px;
  cursor: pointer;
  transition: all var(--transition);
}
.logout-btn:hover { background: rgba(239,68,68,0.1); border-color: rgba(239,68,68,0.3); color: var(--accent-5); }
.logout-btn svg { width: 16px; height: 16px; }

/* 内容区 */
.content-area { flex: 1; padding: 24px; overflow-y: auto; position: relative; z-index: 1; }

/* 超级管理员侧边栏入口 */
.sa-sidebar-entry {
  margin: 8px 12px;
  padding: 12px 16px;
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.sa-sidebar-entry:hover {
  background: rgba(220, 38, 38, 0.18);
  border-color: rgba(220, 38, 38, 0.35);
  transform: translateX(2px);
}

.sa-sidebar-icon {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.sa-sidebar-icon svg {
  width: 18px;
  height: 18px;
}

.sa-sidebar-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
}

.sa-sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #fca5a5;
}

.sa-sidebar-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.sa-sidebar-arrow {
  width: 16px;
  height: 16px;
  color: rgba(255, 255, 255, 0.3);
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.sa-sidebar-entry:hover .sa-sidebar-arrow {
  transform: translateX(3px);
  color: #fca5a5;
}

/* ========== 响应式布局 ========== */

/* 1280px 以下 */
@media (max-width: 1280px) {
  .user-details {
    display: none;
  }
  .logout-text {
    display: none;
  }
}

/* 1024px 以下 */
@media (max-width: 1024px) {
  .header-right {
    gap: 16px;
  }
  .logout-btn {
    padding: 10px;
  }
}

/* 768px 以下（平板 / 手机横屏）：侧边栏变滑出式 */
@media (max-width: 768px) {
  .hamburger-btn {
    display: flex;
  }

  .sidebar {
    transform: translateX(-100%);
    width: 260px;
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 99;
  }

  .main-content {
    margin-left: 0;
  }

  .content-area {
    padding: 16px;
  }

  .header {
    height: 60px;
    padding: 0 16px;
  }

  .page-title {
    font-size: 17px;
  }

  .header-right {
    gap: 12px;
  }

  .user-info {
    gap: 8px;
  }

  .user-details {
    display: none;
  }

  .logout-text {
    display: none;
  }

  .logout-btn {
    padding: 8px;
  }
}

/* 480px 以下（手机竖屏）：最大紧凑 */
@media (max-width: 480px) {
  .header {
    height: 56px;
    padding: 0 12px;
  }

  .page-title {
    font-size: 15px;
  }

  .content-area {
    padding: 12px;
  }

  .header-right {
    gap: 8px;
  }

  .user-avatar {
    width: 34px;
    height: 34px;
  }

  .user-avatar svg {
    width: 16px;
    height: 16px;
  }

  .logout-btn {
    padding: 6px;
  }
}
</style>