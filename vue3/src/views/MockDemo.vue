<template>
  <div class="mock-demo">
    <div class="demo-header">
      <h2>Mock 接口演示</h2>
      <p class="description">
        本页面使用 Mock 数据模拟后端 API，无需启动真实后端即可演示系统功能。
        <br>
        演示账号：admin / 123456
      </p>
      <div class="mock-status">
        <el-tag :type="mockEnabled ? 'success' : 'info'" size="large">
          {{ mockEnabled ? 'Mock 已启用' : 'Mock 已禁用' }}
        </el-tag>
      </div>
    </div>

    <div class="demo-content">
      <!-- 登录演示 -->
      <el-card class="demo-card">
        <template #header>
          <div class="card-header">
            <span>Mock 登录演示</span>
          </div>
        </template>
        <el-form :model="loginForm" class="login-form">
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" placeholder="123456" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleLogin" :loading="loading">
              登录
            </el-button>
          </el-form-item>
        </el-form>
        <div v-if="loginResult" class="result-box">
          <el-tag :type="loginResult.code === 200 ? 'success' : 'danger'">
            {{ loginResult.code === 200 ? '登录成功' : '登录失败' }}
          </el-tag>
          <pre v-if="loginResult.code === 200">{{ JSON.stringify(loginResult.data, null, 2) }}</pre>
        </div>
      </el-card>

      <!-- 图书列表演示 -->
      <el-card class="demo-card">
        <template #header>
          <div class="card-header">
            <span>Mock 图书列表</span>
            <el-button type="primary" size="small" @click="fetchBooks" :loading="booksLoading">
              刷新
            </el-button>
          </div>
        </template>
        <el-table :data="books" v-loading="booksLoading" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="书名" />
          <el-table-column prop="author" label="作者" />
          <el-table-column prop="available" label="库存" width="100">
            <template #default="{ row }">
              <el-tag :type="row.available <= 3 ? 'warning' : 'success'" size="small">
                {{ row.available }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="120">
            <template #default="{ row }">
              <el-tag v-for="cat in row.categories" :key="cat.id" size="small">
                {{ cat.name }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 库存预警演示 -->
      <el-card class="demo-card">
        <template #header>
          <div class="card-header">
            <span>Mock 库存预警</span>
            <el-button type="primary" size="small" @click="fetchWarning">
              检查预警
            </el-button>
          </div>
        </template>
        <div v-if="warning.hasWarning" class="warning-box">
          <el-alert
            :title="`库存预警：${warning.lowStockCount} 本低库存，${warning.outOfStockCount} 本缺货`"
            type="warning"
            :closable="false"
            show-icon
          >
            <div class="warning-details">
              <div v-if="warning.lowStockBooks?.length">
                <strong>低库存图书：</strong>
                <el-tag v-for="book in warning.lowStockBooks" :key="book.id" type="warning" size="small">
                  {{ book.title }} (剩余 {{ book.available }})
                </el-tag>
              </div>
              <div v-if="warning.outOfStockBooks?.length">
                <strong>缺货图书：</strong>
                <el-tag v-for="book in warning.outOfStockBooks" :key="book.id" type="danger" size="small">
                  {{ book.title }}
                </el-tag>
              </div>
            </div>
          </el-alert>
        </div>
        <el-empty v-else description="暂无库存预警" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const mockEnabled = ref(false)
const loading = ref(false)
const booksLoading = ref(false)
const loginForm = ref({
  username: 'admin',
  password: '123456'
})
const loginResult = ref(null)
const books = ref([])
const warning = ref({ hasWarning: false })

onMounted(() => {
  fetchBooks()
})

const handleLogin = async () => {
  loading.value = true
  loginResult.value = null
  try {
    const res = await axios.post('/api/auth/login', loginForm.value)
    loginResult.value = res.data
    if (res.data.code === 200) {
      ElMessage.success('Mock 登录成功！')
    }
  } catch (error) {
    loginResult.value = { code: 500, message: error.message }
  } finally {
    loading.value = false
  }
}

const fetchBooks = async () => {
  booksLoading.value = true
  try {
    const res = await axios.get('/api/books')
    if (res.data.code === 200) {
      books.value = res.data.data
      mockEnabled.value = true
    }
  } catch (error) {
    ElMessage.error('获取图书失败：' + error.message)
  } finally {
    booksLoading.value = false
  }
}

const fetchWarning = async () => {
  try {
    const res = await axios.get('/api/books/warning')
    if (res.data.code === 200) {
      warning.value = res.data.data
      if (warning.value.hasWarning) {
        ElMessage.warning('检测到库存预警！')
      } else {
        ElMessage.success('暂无库存预警')
      }
    }
  } catch (error) {
    ElMessage.error('获取预警失败：' + error.message)
  }
}
</script>

<style scoped>
.mock-demo {
  padding: 20px;
}

.demo-header {
  margin-bottom: 24px;
}

.demo-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.description {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
}

.mock-status {
  margin-top: 12px;
}

.demo-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.demo-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.login-form {
  max-width: 400px;
}

.result-box {
  margin-top: 16px;
  padding: 12px;
  background: var(--bg-glass);
  border-radius: 8px;
}

.result-box pre {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-all;
}

.warning-box {
  margin-top: 12px;
}

.warning-details {
  margin-top: 12px;
}

.warning-details > div {
  margin-bottom: 8px;
}

.warning-details .el-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}
</style>
