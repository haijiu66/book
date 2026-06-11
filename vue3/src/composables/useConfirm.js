import { ref, reactive } from 'vue'

export function useConfirm() {
  const visible = ref(false)
  const confirmState = reactive({
    title: '确认',
    message: '',
    description: '',
    type: 'warning',
    confirmText: '确定',
    cancelText: '取消',
    loading: false
  })

  let resolveCallback = null
  let rejectCallback = null

  const showConfirm = (options) => {
    return new Promise((resolve, reject) => {
      confirmState.title = options.title || '确认'
      confirmState.message = options.message || ''
      confirmState.description = options.description || ''
      confirmState.type = options.type || 'warning'
      confirmState.confirmText = options.confirmText || '确定'
      confirmState.cancelText = options.cancelText || '取消'
      confirmState.loading = false
      
      resolveCallback = resolve
      rejectCallback = reject
      visible.value = true
    })
  }

  const handleConfirm = () => {
    if (resolveCallback) {
      resolveCallback(true)
      resolveCallback = null
      rejectCallback = null
    }
    visible.value = false
  }

  const handleCancel = () => {
    if (rejectCallback) {
      rejectCallback(false)
      resolveCallback = null
      rejectCallback = null
    }
    visible.value = false
  }

  const setLoading = (loading) => {
    confirmState.loading = loading
  }

  const close = () => {
    visible.value = false
    if (rejectCallback) {
      rejectCallback(false)
      resolveCallback = null
      rejectCallback = null
    }
  }

  return {
    visible,
    confirmState,
    showConfirm,
    handleConfirm,
    handleCancel,
    setLoading,
    close
  }
}

// 便捷方法
export function confirm(options) {
  return new Promise((resolve, reject) => {
    // 这里只是创建了 Promise，实际使用时需要配合 ConfirmDialog 组件
    // 建议在 app 级别创建全局的 confirm 方法
    reject('请使用 useConfirm composable')
  })
}
