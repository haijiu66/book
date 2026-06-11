import request from '../utils/request'

export const rankingApi = {
  getBorrowRanking(type = 'weekly', limit = 10) {
    return request.get('/rankings/borrow', { params: { type, limit } })
  },

  getReadingRanking(type = 'weekly', limit = 10) {
    return request.get('/rankings/reading', { params: { type, limit } })
  }
}
