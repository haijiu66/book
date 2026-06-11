<template>
  <div class="sa-login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="badge">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span>超级管理员</span>
        </div>
        <h1 class="gradient-text">智慧书苑</h1>
        <p>系统最高权限 · 安全验证</p>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="input-group">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <input v-model="loginForm.username" type="text" placeholder="用户名" autocomplete="username"/>
        </div>
        <div class="input-group">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <input v-model="loginForm.password" type="password" placeholder="密码" autocomplete="current-password"/>
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>超级管理员登录</span>
        </button>
      </form>

      <div class="back-link" @click="router.push(authStore.isAdmin ? '/admin' : '/login')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        {{ authStore.isAdmin ? '返回管理员界面' : '返回普通登录' }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'
import { animate, utils } from 'animejs'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  if (authStore.isAuthenticated && authStore.isSuperAdmin) router.push('/super-admin')
  animate('.login-card', {
    opacity: [0, 1], translateY: [30, 0],
    duration: 800, ease: 'outCubic'
  })
  animate('.input-group', {
    opacity: [0, 1], translateX: [-20, 0],
    delay: utils.stagger(100), duration: 600, ease: 'outCubic'
  })
})
const loading = ref(false)
const loginForm = ref({ username: '', password: '', role: 'SUPER_ADMIN' })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    const success = await authStore.login(loginForm.value)
    if (success) { ElMessage.success('登录成功'); router.push('/super-admin') }
    else { ElMessage.error(authStore.loginError || '登录失败') }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  } finally { loading.value = false }
}
</script>

<style scoped>
.sa-login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  position: relative; z-index: 1; padding: 24px;
}
.login-card {
  width: 420px; max-width: 100%;
  background: var(--bg-card); backdrop-filter: blur(24px);
  border: 1px solid var(--border-color); border-radius: var(--radius-xl);
  padding: 40px 36px;
  box-shadow: var(--shadow-lg), 0 0 60px rgba(239,68,68,0.08);
}
.card-header { text-align: center; margin-bottom: 28px; }
.badge {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 6px 16px; margin-bottom: 16px;
  background: rgba(239,68,68,0.12); border: 1px solid rgba(239,68,68,0.2);
  border-radius: 20px; color: var(--accent-5); font-size: 13px; font-weight: 600;
}
.badge svg { width: 16px; height: 16px; }
.card-header h1 { font-size: 26px; font-weight: 700; margin-bottom: 6px; }
.card-header p { font-size: 14px; color: var(--text-secondary); }

.input-group {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; margin-bottom: 14px;
  background: var(--bg-glass); border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: border-color var(--transition);
}
.input-group:focus-within { border-color: var(--accent-5); }
.input-group svg { width: 20px; height: 20px; color: var(--text-muted); flex-shrink: 0; }
.input-group input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 15px; color: var(--text-primary);
}
.input-group input::placeholder { color: var(--text-muted); }

.submit-btn {
  width: 100%; padding: 14px; margin-top: 6px;
  background: var(--gradient-2); border: none; border-radius: var(--radius-md);
  color: white; font-size: 16px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(239,68,68,0.3); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.spinner { width: 20px; height: 20px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; display: inline-block; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.back-link {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 20px; color: var(--text-muted); font-size: 14px; cursor: pointer;
  transition: color var(--transition);
}
.back-link:hover { color: var(--text-primary); }
.back-link svg { width: 18px; height: 18px; }
</style>
