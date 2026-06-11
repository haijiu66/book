import { useAuthStore } from '../stores/auth'

export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const requiredRole = binding.value
    
    if (!authStore.hasRole(requiredRole)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el, binding) {
    const authStore = useAuthStore()
    const requiredRole = binding.value
    
    if (!authStore.hasRole(requiredRole)) {
      el.parentNode?.removeChild(el)
    }
  }
}
