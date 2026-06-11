import { useAuthStore } from '../stores/auth'

/**
 * 检查用户是否拥有指定角色
 * @param {string|Array} role - 角色或角色数组
 * @returns {boolean}
 */
export function hasRole(role) {
  const authStore = useAuthStore()
  return authStore.hasRole(role)
}

/**
 * 检查是否是超级管理员
 * @returns {boolean}
 */
export function isSuperAdmin() {
  const authStore = useAuthStore()
  return authStore.isSuperAdmin
}

/**
 * 检查是否是管理员（包含超级管理员）
 * @returns {boolean}
 */
export function isAdmin() {
  const authStore = useAuthStore()
  return authStore.hasAnyAdminRole
}

/**
 * 检查是否是普通用户
 * @returns {boolean}
 */
export function isReader() {
  const authStore = useAuthStore()
  return authStore.isReader
}

/**
 * 检查用户是否已登录
 * @returns {boolean}
 */
export function isAuthenticated() {
  const authStore = useAuthStore()
  return authStore.isAuthenticated
}
