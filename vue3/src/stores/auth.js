import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

// 角色常量
export const ROLE_SUPER_ADMIN = 'SUPER_ADMIN'
export const ROLE_ADMIN = 'ADMIN'
export const ROLE_READER = 'READER'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || 'null'))
  const userRole = ref(sessionStorage.getItem('userRole') || '')
  const loginError = ref('')
  const userIdStr = sessionStorage.getItem('userId')
  const userId = ref(userIdStr ? Number(userIdStr) : null)
  const permissions = ref(sessionStorage.getItem('permissions') || '')

  const login = async (loginData) => {
    loginError.value = ''
    try {
      const res = await authApi.login(loginData)
      if (res.data.code === 200) {
        const data = res.data.data
        token.value = data.token
        userInfo.value = {
          username: data.username,
          name: data.name
        }
        userRole.value = data.role || data.userType
        userId.value = data.userId
        
        permissions.value = data.permissions || ''
        sessionStorage.setItem('token', data.token)
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        sessionStorage.setItem('userRole', userRole.value)
        sessionStorage.setItem('userId', data.userId ? String(data.userId) : '')
        sessionStorage.setItem('permissions', permissions.value)
        return true
      }
      loginError.value = res.data?.message || '登录失败'
      return false
    } catch (error) {
      console.error('登录失败', error)
      loginError.value =
        error.response?.data?.message ||
        (error.response?.status === 401 ? '用户名或密码错误' : '') ||
        (error.response?.status === 403 ? '当前账号无权限登录该入口' : '') ||
        '登录失败，请稍后重试'
      return false
    }
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('登出API调用失败', error)
    } finally {
      clearAuth()
    }
  }

  // 清除认证信息
  const clearAuth = () => {
    token.value = ''
    userInfo.value = null
    userRole.value = ''
    userId.value = null
    permissions.value = ''

    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    sessionStorage.removeItem('userRole')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('permissions')
  }

  // 角色判断
  const isSuperAdmin = computed(() => userRole.value === ROLE_SUPER_ADMIN)
  const isAdmin = computed(() => userRole.value === ROLE_ADMIN)
  const isReader = computed(() => userRole.value === ROLE_READER)
  
  // 检查是否已认证
  const isAuthenticated = computed(() => !!token.value)

  // 检查是否有权限访问指定角色（支持数组）
  const hasRole = (role) => {
    if (Array.isArray(role)) {
      return role.some(r => userRole.value === r)
    }
    return userRole.value === role
  }

  // 检查细粒度权限（SuperAdmin 拥有所有权限）
  const hasPermission = (permissionCode) => {
    if (isSuperAdmin.value) return true
    if (!permissions.value) return false
    return permissions.value.split(',').includes(permissionCode)
  }

  // 检查是否有任意一种管理权限
  const hasAnyAdminRole = computed(() => isSuperAdmin.value || isAdmin.value)

  // 根据角色获取首页路由
  const getHomeRoute = () => {
    if (isSuperAdmin.value) return '/super-admin'
    if (isAdmin.value) return '/admin'
    return '/'
  }

  // 获取角色显示名称
  const getRoleName = () => {
    switch (userRole.value) {
      case ROLE_SUPER_ADMIN:
        return '超级管理员'
      case ROLE_ADMIN:
        return '管理员'
      case ROLE_READER:
        return '普通用户'
      default:
        return '未知'
    }
  }

  // 获取角色标签类型
  const getRoleTagType = () => {
    switch (userRole.value) {
      case ROLE_SUPER_ADMIN:
        return 'danger'
      case ROLE_ADMIN:
        return 'warning'
      case ROLE_READER:
        return 'success'
      default:
        return 'info'
    }
  }

  // 获取侧边栏菜单
  const getSidebarMenus = () => {
    switch (userRole.value) {
      case ROLE_SUPER_ADMIN:
        return [
          { path: '/super-admin', title: '首页', icon: 'HomeFilled' },
          { path: '/super-admin/admin-management', title: '管理员管理', icon: 'UserFilled' },
          { path: '/super-admin/audit-logs', title: '审计日志', icon: 'Document' },
          { path: '/super-admin/login-logs', title: '登录监控', icon: 'Clock' }
        ]
      case ROLE_ADMIN:
        return [
          { path: '/admin', title: '首页', icon: 'HomeFilled' },
          { path: '/admin/user-management', title: '用户管理', icon: 'User' },
          { path: '/admin/books', title: '图书管理', icon: 'Reading' },
          { path: '/admin/categories', title: '分类管理', icon: 'Collection' },
          { path: '/admin/ebooks', title: '电子书管理', icon: 'Document' },
          { path: '/admin/borrows', title: '借阅管理', icon: 'Tickets' },
          { path: '/admin/rankings', title: '阅读排行', icon: 'TrendCharts' }
        ]
      case ROLE_READER:
      default:
        return [
          { path: '/', title: '首页', icon: 'HomeFilled' },
          { path: '/books', title: '图书列表', icon: 'Reading' },
          { path: '/categories', title: '图书分类', icon: 'Collection' },
          { path: '/my-favorites', title: '我的收藏', icon: 'StarFilled' },
          { path: '/borrow-history', title: '我的借阅', icon: 'Tickets' },
          { path: '/ebooks', title: '在线阅读', icon: 'Monitor' }
        ]
    }
  }

  return {
    token,
    userInfo,
    userRole,
    userId,
    permissions,
    login,
    logout,
    clearAuth,
    loginError,
    isSuperAdmin,
    isAdmin,
    isReader,
    isAuthenticated,
    hasRole,
    hasPermission,
    hasAnyAdminRole,
    getHomeRoute,
    getRoleName,
    getRoleTagType,
    getSidebarMenus
  }
})
