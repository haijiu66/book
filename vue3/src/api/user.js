import request from '../utils/request'

export const userApi = {
  // 获取所有用户
  getAllUsers() {
    return request.get('/admin/user-management')
  },
  
  // 获取用户详情
  getUserById(id) {
    return request.get(`/admin/user-management/${id}`)
  },
  
  // 创建用户
  createUser(data) {
    return request.post('/admin/user-management', data)
  },
  
  // 更新用户
  updateUser(id, data) {
    return request.put(`/admin/user-management/${id}`, data)
  },
  
  // 删除用户
  deleteUser(id) {
    return request.delete(`/admin/user-management/${id}`)
  }
}