import request from '../utils/request'

export const loginLogApi = {
  getLoginLogs(params = {}) {
    // 使用 /search 端点以支持 username/userType/loginStatus 筛选
    return request.get('/admin/login-logs/search', { params })
  },

  /**
   * 获取登录日志详情
   * @param {number} id - 日志ID
   */
  getLoginLogById(id) {
    return request.get(`/admin/login-logs/${id}`)
  },

  /**
   * 删除登录日志
   * @param {number} id - 日志ID
   */
  deleteLoginLog(id) {
    return request.delete(`/admin/login-logs/${id}`)
  },

  /**
   * 批量删除登录日志
   * @param {Array<number>} ids - 日志ID数组
   */
  batchDeleteLoginLogs(ids) {
    return request.delete('/admin/login-logs/batch', { data: { ids } })
  },

  /**
   * 获取用户登录汇总列表
   */
  getUserLoginSummary() {
    return request.get('/admin/login-logs/user-summary')
  },

  /**
   * 获取指定用户的登录历史
   * @param {Object} params - { userId, userType, page, size }
   */
  getUserLoginHistory(params) {
    return request.get('/admin/login-logs/user-history', { params })
  },

  // 清理旧登录日志
  cleanOldLogs(beforeDate) {
    return request.delete('/admin/login-logs/clean-old', { params: { beforeDate } })
  }
}
