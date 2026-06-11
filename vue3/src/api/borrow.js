import request from '../utils/request'

export const borrowApi = {
  // 获取所有借阅记录（管理员）
  getAll(params) {
    return request.get('/borrows', { params })
  },

  // 获取借阅记录详情
  getById(id) {
    return request.get(`/borrows/${id}`)
  },

  // 获取当前用户的借阅记录
  getMyBorrows(params) {
    return request.get('/borrows/my', { params })
  },

  // 借书
  borrow(bookId, borrowDays) {
    const params = { bookId }
    if (borrowDays) params.borrowDays = borrowDays
    return request.post('/borrows/borrow', null, { params })
  },

  // 还书
  returnBook(id) {
    return request.post(`/borrows/return/${id}`)
  },

  // 获取逾期记录
  getOverdue(params) {
    return request.get('/borrows/overdue', { params })
  },

  // 更新逾期状态
  updateOverdue() {
    return request.post('/borrows/update-overdue')
  },

  // 获取指定状态的借阅记录（管理员）
  getByStatus(status) {
    return request.get(`/borrows/status/${status}`)
  },

  // 获取指定用户的借阅记录（管理员）
  getByUserId(userId) {
    return request.get(`/borrows/user/${userId}`)
  },

  // 获取指定图书的借阅记录（管理员）
  getByBookId(bookId) {
    return request.get(`/borrows/book/${bookId}`)
  },

  // 续借
  renew(id, newDueDate) {
    const params = {}
    if (newDueDate) params.newDueDate = newDueDate
    return request.post(`/borrows/renew/${id}`, null, { params })
  },

  // 取消借阅
  cancel(id) {
    return request.post(`/borrows/cancel/${id}`)
  },

  // 删除借阅记录（管理员）
  delete(id) {
    return request.delete(`/borrows/${id}`)
  }
}
