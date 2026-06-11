import { defineStore } from 'pinia'
import { ref } from 'vue'
import { borrowApi } from '../api/borrow'

// 借阅状态常量
export const BORROW_STATUS_BORROWED = 'BORROWED'
export const BORROW_STATUS_RETURNED = 'RETURNED'
export const BORROW_STATUS_OVERDUE = 'OVERDUE'
export const BORROW_STATUS_CANCELLED = 'CANCELLED'

export const useBorrowStore = defineStore('borrow', () => {
  const borrows = ref([])
  const myBorrows = ref([])
  const loading = ref(false)
  const total = ref(0)

  // 获取所有借阅记录
  const fetchBorrows = async (params = {}) => {
    loading.value = true
    try {
      const res = await borrowApi.getAll(params)
      if (res.data.code === 200) {
        borrows.value = res.data.data.list || res.data.data || []
        total.value = res.data.data.total || borrows.value.length
      }
    } finally {
      loading.value = false
    }
  }

  // 获取当前用户的借阅记录
  const fetchMyBorrows = async (params = {}) => {
    loading.value = true
    try {
      const res = await borrowApi.getMyBorrows(params)
      if (res.data.code === 200) {
        myBorrows.value = res.data.data || []
        total.value = myBorrows.value.length
      } else {
        myBorrows.value = []
        total.value = 0
      }
    } catch (e) {
      console.error('获取借阅记录失败', e)
      myBorrows.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  // 借书
  const borrowBook = async (data) => {
    const bookId = data?.bookId
    const borrowDays = data?.borrowDays
    const res = await borrowApi.borrow(bookId, borrowDays)
    if (res.data.code === 200) {
      return res.data.data
    }
    throw new Error(res.data.message || '借书失败')
  }

  // 还书
  const returnBook = async (id) => {
    const res = await borrowApi.returnBook(id)
    if (res.data.code === 200) {
      return res.data.data
    }
    throw new Error(res.data.message || '还书失败')
  }

  // 续借
  const renewBook = async (id, newDueDate) => {
    const res = await borrowApi.renew(id, newDueDate)
    if (res.data.code === 200) {
      return res.data.data
    }
    throw new Error(res.data.message || '续借失败')
  }

  // 取消借阅
  const cancelBorrow = async (id) => {
    const res = await borrowApi.cancel(id)
    if (res.data.code === 200) {
      return res.data.data
    }
    throw new Error(res.data.message || '取消失败')
  }

  // 获取状态显示文本
  const getStatusText = (status) => {
    switch (status) {
      case BORROW_STATUS_BORROWED:
        return '借阅中'
      case BORROW_STATUS_RETURNED:
        return '已归还'
      case BORROW_STATUS_OVERDUE:
        return '已逾期'
      case BORROW_STATUS_CANCELLED:
        return '已取消'
      default:
        return '未知'
    }
  }

  // 获取状态标签类型
  const getStatusType = (status) => {
    switch (status) {
      case BORROW_STATUS_BORROWED:
        return 'primary'
      case BORROW_STATUS_RETURNED:
        return 'success'
      case BORROW_STATUS_OVERDUE:
        return 'danger'
      case BORROW_STATUS_CANCELLED:
        return 'info'
      default:
        return 'info'
    }
  }

  // 检查是否逾期
  const isOverdue = (borrow) => {
    if (!borrow.dueDate) return false
    const dueDate = new Date(borrow.dueDate)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return borrow.status === BORROW_STATUS_BORROWED && dueDate < today
  }

  return {
    borrows,
    myBorrows,
    loading,
    total,
    fetchBorrows,
    fetchMyBorrows,
    borrowBook,
    returnBook,
    renewBook,
    cancelBorrow,
    getStatusText,
    getStatusType,
    isOverdue
  }
})
