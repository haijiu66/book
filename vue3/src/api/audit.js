import request from '../utils/request'

export const auditApi = {
  // 获取审计日志列表（带筛选，走 /search 端点）
  getAuditLogs(params = {}) {
    return request.get('/admin/audit-logs/search', { params })
  },

  // 获取审计日志详情
  getAuditLogById(id) {
    return request.get(`/admin/audit-logs/${id}`)
  },

  // 获取审计日志用户汇总
  getUserAuditSummary() {
    return request.get('/admin/audit-logs/user-summary')
  },

  // 获取指定用户的审计日志历史
  getUserAuditHistory(params) {
    return request.get('/admin/audit-logs/user-history', { params })
  },

  // 清理旧审计日志
  cleanOldLogs(beforeDate) {
    return request.delete('/admin/audit-logs/clean-old', { params: { beforeDate } })
  }
}