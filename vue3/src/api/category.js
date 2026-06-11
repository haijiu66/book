import request from '../utils/request'

export const categoryApi = {
  getAll() {
    return request.get('/categories')
  },

  getTree() {
    return request.get('/categories', { params: { tree: true } })
  },

  getById(id) {
    return request.get(`/categories/${id}`)
  },

  create(data) {
    return request.post('/categories', data)
  },

  update(id, data) {
    return request.put(`/categories/${id}`, data)
  },

  delete(id) {
    return request.delete(`/categories/${id}`)
  }
}
