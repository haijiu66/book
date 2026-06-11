import request from '../utils/request'

export const favoriteApi = {
  toggle(targetType, targetId) {
    return request.post('/favorites/toggle', { targetType, targetId })
  },

  getMyIds(targetType) {
    return request.get('/favorites/my/ids', { params: { targetType } })
  },

  getMyList(targetType) {
    return request.get('/favorites/my/list', { params: { targetType } })
  },

  getStatus(targetType, ids) {
    return request.get('/favorites/status', { params: { targetType, ids: ids.join(',') } })
  },

  getRanking(targetType, limit = 10) {
    return request.get(`/favorites/ranking/${targetType}`, { params: { limit } })
  }
}
