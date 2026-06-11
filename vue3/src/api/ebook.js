import request from '../utils/request'

export function getEBookList() {
  return request.get('/ebooks')
}

export function getEBookDetail(id) {
  return request.get(`/ebooks/${id}`)
}

export function getChapters(ebookId) {
  return request.get(`/ebooks/${ebookId}/chapters`)
}

export function getChapterByIndex(ebookId, index) {
  return request.get(`/ebooks/${ebookId}/chapters/${index}`)
}

export function getStartReading(ebookId) {
  return request.get(`/ebooks/${ebookId}/start`)
}

export function getProgress(ebookId) {
  return request.get(`/ebooks/progress/${ebookId}`)
}

export function getUserProgressList() {
  return request.get('/ebooks/progress')
}

export function saveProgress(data) {
  return request.post('/ebooks/progress', data)
}

export function uploadEBook(formData) {
  return request.post('/ebooks/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteEBook(id) {
  return request.delete(`/ebooks/${id}`)
}

export function updateEBookCategories(id, categoryIds) {
  return request.put(`/ebooks/${id}/categories`, categoryIds)
}

export function updateEBook(id, data) {
  return request.put(`/ebooks/${id}`, data)
}

export function uploadCover(id, formData) {
  return request.post(`/ebooks/${id}/cover`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ========== 下载功能已禁用 ==========
// export function cacheEBook(id) {
//   return request.post(`/ebooks/${id}/cache`, null, { timeout: 300000 })
// }

// export function getCacheProgress(id) {
//   return request.get(`/ebooks/${id}/cache-progress`)
// }

// export function cacheAndDownload(id) {
//   return request.post(`/ebooks/${id}/cache-and-download`, null, {
//     responseType: 'blob',
//     timeout: 300000
//   })
// }

export function getEBooksByCategoryId(categoryId) {
  return request.get(`/ebooks/category/${categoryId}`)
}

export const ebookApi = {
  getEBookList,
  getEBookDetail,
  getChapters,
  getChapterByIndex,
  getStartReading,
  getProgress,
  getUserProgressList,
  saveProgress,
  uploadEBook,
  deleteEBook,
  updateEBook,
  updateEBookCategories,
  uploadCover,
  // cacheEBook,     // 下载功能已禁用
  // getCacheProgress, // 下载功能已禁用
  // cacheAndDownload, // 下载功能已禁用
  getEBooksByCategoryId
}
