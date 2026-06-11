import request from '../utils/request'

export const adminApi = {
  // 获取所有管理员
  getAllAdmins() {
    return request.get('/admin/admin-management')
  },
  
  // 获取管理员详情
  getAdminById(id) {
    return request.get(`/admin/admin-management/${id}`)
  },
  
  // 创建管理员
  createAdmin(data) {
    return request.post('/admin/admin-management', data)
  },
  
  // 更新管理员
  updateAdmin(id, data) {
    return request.put(`/admin/admin-management/${id}`, data)
  },
  
  // 删除管理员
  deleteAdmin(id) {
    return request.delete(`/admin/admin-management/${id}`)
  },

  // 获取仪表盘统计数据
  getDashboardStats() {
    return request.get('/admin/admin-management/dashboard-stats')
  }
}