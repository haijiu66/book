<template>
  <div class="register-page">
    <div class="register-card">
      <div class="card-header">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          </svg>
        </div>
        <h1 class="gradient-text">创建账户</h1>
        <p>加入智慧书苑，畅游知识的海洋</p>
      </div>

      <form @submit.prevent="handleRegister">
        <div class="input-group">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M20 21a8 8 0 0 0-16 0"/></svg>
          <input v-model="registerForm.username" type="text" placeholder="用户名（至少3位）" autocomplete="username"/>
        </div>
        <div class="row">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" autocomplete="new-password"/>
          </div>
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" autocomplete="new-password"/>
          </div>
        </div>
        <div class="input-group">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <input v-model="registerForm.name" type="text" placeholder="姓名"/>
        </div>
        <div class="row">
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
            <input v-model="registerForm.phone" type="tel" placeholder="手机号（选填）"/>
          </div>
          <div class="input-group">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            <input v-model="registerForm.email" type="email" placeholder="邮箱（选填）"/>
          </div>
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>立即注册</span>
        </button>
      </form>

      <div class="footer-links">
        <router-link to="/login">已有账号？去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { ElMessage } from 'element-plus'
import { animate, utils } from 'animejs'

const router = useRouter()

onMounted(() => {
  animate('.register-card', {
    opacity: [0, 1], translateY: [30, 0],
    duration: 800, ease: 'outCubic'
  })
  animate('.input-group', {
    opacity: [0, 1], translateX: [-20, 0],
    delay: utils.stagger(80), duration: 500, ease: 'outCubic'
  })
})
const loading = ref(false)

const registerForm = ref({
  username: '', password: '', confirmPassword: '', name: '', phone: '', email: ''
})

const handleRegister = async () => {
  if (!registerForm.value.username || registerForm.value.username.length < 3) { ElMessage.warning('用户名至少3位'); return }
  if (!registerForm.value.password || registerForm.value.password.length < 6) { ElMessage.warning('密码至少6位'); return }
  if (registerForm.value.password !== registerForm.value.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (!registerForm.value.name) { ElMessage.warning('请输入姓名'); return }

  loading.value = true
  try {
    const res = await authApi.registerNormalUser({
      username: registerForm.value.username, password: registerForm.value.password,
      confirmPassword: registerForm.value.confirmPassword, name: registerForm.value.name,
      phone: registerForm.value.phone || undefined, email: registerForm.value.email || undefined
    })
    if (res.data.code === 200) { ElMessage.success('注册成功，请登录'); router.push('/login') }
    else { ElMessage.error(res.data.message || '注册失败') }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  position: relative; z-index: 1; padding: 24px;
}
.register-card {
  width: 480px; max-width: 100%;
  background: var(--bg-card); backdrop-filter: blur(24px);
  border: 1px solid var(--border-color); border-radius: var(--radius-xl);
  padding: 36px 32px;
  box-shadow: var(--shadow-lg), 0 0 60px rgba(99,102,241,0.08);
}
.card-header { text-align: center; margin-bottom: 24px; }
.logo-icon {
  width: 52px; height: 52px; margin: 0 auto 14px;
  background: var(--gradient-1); border-radius: 14px;
  display: flex; align-items: center; justify-content: center; color: white;
}
.logo-icon svg { width: 26px; height: 26px; }
.card-header h1 { font-size: 24px; font-weight: 700; margin-bottom: 4px; }
.card-header p { font-size: 14px; color: var(--text-secondary); }

.row { display: flex; gap: 10px; }
.row .input-group { flex: 1; min-width: 0; }
.row .input-group input { min-width: 0; width: 100%; }

.input-group {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; margin-bottom: 12px;
  background: var(--bg-glass); border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: border-color var(--transition);
}
.input-group:focus-within { border-color: var(--accent-1); }
.input-group svg { width: 18px; height: 18px; color: var(--text-muted); flex-shrink: 0; }
.input-group input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 14px; color: var(--text-primary);
}
.input-group input::placeholder { color: var(--text-muted); }

.submit-btn {
  width: 100%; padding: 14px; margin-top: 8px;
  background: var(--gradient-1); border: none; border-radius: var(--radius-md);
  color: white; font-size: 16px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(99,102,241,0.3); }
.submit-btn:disabled { opacity: 0.6; }
.spinner { width: 20px; height: 20px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; display: inline-block; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.footer-links { text-align: center; margin-top: 18px; }
.footer-links a { color: var(--text-muted); font-size: 14px; text-decoration: none; transition: color var(--transition); }
.footer-links a:hover { color: var(--accent-1); }

@media (max-width: 500px) { .row { flex-direction: column; gap: 0; } }
</style>
