import request from '../utils/request'

export const bookApi = {
  getAll() {
    return request.get('/books')
  },
  
  getById(id) {
    return request.get(`/books/${id}`)
  },
  
  add(book) {
    return request.post('/books', book)
  },
  
  update(id, book) {
    return request.put(`/books/${id}`, book)
  },
  
  delete(id) {
    return request.delete(`/books/${id}`)
  },

  uploadCover(id, formData) {
    return request.post(`/books/${id}/cover`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  updateCategories(id, categoryIds) {
    return request.put(`/books/${id}/categories`, categoryIds)
  },
  
  getByCategoryId(categoryId) {
    return request.get(`/books/category/${categoryId}`)
  },

  search(keyword) {
    return request.get('/books/search', { params: { keyword } })
  },
  
  import(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/books/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  export() {
    return request.get('/books/export', { responseType: 'blob' })
  },

  getInventoryWarning() {
    return request.get('/books/warning')
  }
}
