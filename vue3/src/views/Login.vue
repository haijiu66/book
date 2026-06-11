<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          </svg>
        </div>
        <h1 class="gradient-text">智慧书苑</h1>
        <p>开启知识的殿堂，让阅读成为习惯</p>
      </div>

      <div class="role-selector">
        <button class="role-btn" :class="{ active: loginForm.role === 'READER' }" @click="loginForm.role = 'READER'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
          </svg>
          <span>普通用户</span>
        </button>
        <button class="role-btn" :class="{ active: loginForm.role === 'ADMIN' }" @click="loginForm.role = 'ADMIN'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/>
            <line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/>
          </svg>
          <span>管理员</span>
        </button>
      </div>
      <p class="role-hint">同名账号拥有多重身份时，选择对应角色即可按该身份登录</p>

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
          <span v-else>登 录</span>
        </button>
      </form>

      <div class="footer-links">
        <router-link to="/register">还没有账号？立即注册</router-link>
      </div>

      <div class="demo-info">
        <span>admin / 123456</span>
        <span>reader / 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { animate, utils } from 'animejs'

onMounted(() => {
  animate('.login-card', {
    opacity: [0, 1], translateY: [30, 0],
    duration: 800, ease: 'outCubic'
  })
  animate('.input-group', {
    opacity: [0, 1], translateX: [-20, 0],
    delay: utils.stagger(100), duration: 600, ease: 'outCubic'
  })
})

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const loginForm = ref({ username: '', password: '', role: 'READER' })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    const success = await authStore.login(loginForm.value)
    if (success) { ElMessage.success('登录成功'); router.push(authStore.getHomeRoute()) }
    else { ElMessage.error(authStore.loginError || '登录失败') }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  position: relative; z-index: 1; padding: 24px;
}
.login-card {
  width: 420px; max-width: 100%;
  background: var(--bg-card); backdrop-filter: blur(24px);
  border: 1px solid var(--border-color); border-radius: var(--radius-xl);
  padding: 40px 36px;
  box-shadow: var(--shadow-lg), 0 0 60px rgba(99,102,241,0.08);
}
.card-header { text-align: center; margin-bottom: 28px; }
.logo-icon {
  width: 56px; height: 56px; margin: 0 auto 16px;
  background: var(--gradient-1); border-radius: 16px;
  display: flex; align-items: center; justify-content: center; color: white;
}
.logo-icon svg { width: 28px; height: 28px; }
.card-header h1 { font-size: 26px; font-weight: 700; margin-bottom: 6px; }
.card-header p { font-size: 14px; color: var(--text-secondary); }

.role-selector { display: flex; gap: 8px; margin-bottom: 10px; }
.role-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 8px; border-radius: var(--radius-md);
  border: 1px solid var(--border-color); background: var(--bg-glass);
  color: var(--text-secondary); font-size: 13px; cursor: pointer;
  transition: all var(--transition);
}
.role-btn svg { width: 16px; height: 16px; }
.role-btn:hover { border-color: var(--border-glow); color: var(--text-primary); }
.role-btn.active { background: rgba(99,102,241,0.12); border-color: var(--accent-1); color: var(--accent-1); }

.role-hint { text-align: center; font-size: 12px; color: var(--text-muted); margin-bottom: 20px; }

.input-group {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; margin-bottom: 14px;
  background: var(--bg-glass); border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: border-color var(--transition);
}
.input-group:focus-within { border-color: var(--accent-1); }
.input-group svg { width: 20px; height: 20px; color: var(--text-muted); flex-shrink: 0; }
.input-group input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 15px; color: var(--text-primary);
}
.input-group input::placeholder { color: var(--text-muted); }

.submit-btn {
  width: 100%; padding: 14px; margin-top: 6px;
  background: var(--gradient-1); border: none; border-radius: var(--radius-md);
  color: white; font-size: 16px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(99,102,241,0.3); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.spinner { width: 20px; height: 20px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; display: inline-block; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.footer-links { text-align: center; margin-top: 20px; }
.footer-links a { color: var(--text-muted); font-size: 14px; text-decoration: none; transition: color var(--transition); }
.footer-links a:hover { color: var(--accent-1); }

.demo-info { display: flex; justify-content: center; gap: 16px; margin-top: 12px; font-size: 12px; color: var(--text-muted); }
</style>
