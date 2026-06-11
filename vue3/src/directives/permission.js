import { useAuthStore } from '../stores/auth'

export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const requiredPermission = binding.value
    
    // 预留：细粒度权限控制
    // 目前使用角色控制即可
    // 如果需要更细粒度的权限，可以在这里实现
  },
  updated(el, binding) {
    const authStore = useAuthStore()
    const requiredPermission = binding.value
    
    // 预留：细粒度权限控制
  }
}
