<template>
  <div class="login-monitor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户登录监控</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="filterUsername"
          placeholder="搜索用户名"
          clearable
          style="width: 200px"
          @input="applyFilters"
        />
        <el-select
          v-model="filterUserType"
          placeholder="用户类型"
          clearable
          style="width: 160px"
          @change="applyFilters"
        >
          <el-option label="超级管理员" value="SUPER_ADMIN" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="READER" />
        </el-select>
        <el-select
          v-model="filterLoginStatus"
          placeholder="登录状态"
          clearable
          style="width: 160px"
          @change="applyFilters"
        >
          <el-option label="登录过" value="LOGGED_IN" />
          <el-option label="从未登录" value="NEVER" />
        </el-select>
      </div>

      <div class="cleanup-bar">
        <span class="cleanup-label">定时清理：</span>
        <el-date-picker
          v-model="cleanupDate"
          type="datetime"
          placeholder="选择清理时间点"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 240px"
        />
        <el-button type="danger" @click="handleCleanup" :loading="cleanupLoading" :disabled="!cleanupDate">
          清理此日期之前的日志
        </el-button>
        <span class="cleanup-hint">每天凌晨3:00自动清理90天前的日志</span>
      </div>

      <el-table
        v-loading="loading"
        :data="displayList"
        stripe
        style="width: 100%; margin-top: 16px"
        @expand-change="handleExpand"
        :row-key="(row) => row.userType + '_' + row.userId"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <h4>{{ row.username }} 的登录历史</h4>
              <el-table
                v-loading="row._historyLoading"
                :data="row._history || []"
                stripe
                size="small"
              >
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="createTime" label="登录时间" width="180" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row: log }">
                    <el-tag :type="log.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                      {{ log.status === 'SUCCESS' ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="ipAddress" label="IP 地址" width="150" />
                <el-table-column prop="userAgent" label="浏览器信息" show-overflow-tooltip />
                <el-table-column label="失败原因" show-overflow-tooltip>
                  <template #default="{ row: log }">
                    {{ log.errorMessage || '-' }}
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="row._historyTotal > (row._historySize || 5)" class="history-pagination">
                <el-pagination
                  background
                  layout="prev, pager, next"
                  :total="row._historyTotal"
                  :page-size="row._historySize || 5"
                  :current-page="row._historyPage || 1"
                  @current-change="(p) => handleHistoryPage(row, p)"
                  small
                />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column label="用户类型" width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.userType === 'SUPER_ADMIN' ? 'danger' : row.userType === 'ADMIN' ? 'warning' : 'success'"
              size="small"
            >
              {{ row.userType === 'SUPER_ADMIN' ? '超级管理员' : row.userType === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="最近登录时间" width="180">
          <template #default="{ row }">
            {{ row.lastLoginTime || '从未登录' }}
          </template>
        </el-table-column>
        <el-table-column label="登录状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="!row.lastLoginTime" type="info" size="small">从未登录</el-tag>
            <el-tag v-else-if="row.lastLoginStatus === 'SUCCESS'" type="success" size="small">成功</el-tag>
            <el-tag v-else type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginIp" label="最近登录IP" width="150" />
        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ACTIVE'" type="success" size="small">正常</el-tag>
            <el-tag v-else type="danger" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { loginLogApi } from '../../api/loginLog'

const loading = ref(false)
const userList = ref([])
const displayList = ref([])

const filterUsername = ref('')
const filterUserType = ref('')
const filterLoginStatus = ref('')
const cleanupDate = ref('')
const cleanupLoading = ref(false)

const fetchUserSummary = async () => {
  loading.value = true
  try {
    const res = await loginLogApi.getUserLoginSummary()
    if (res.data.code === 200) {
      userList.value = (res.data.data || []).map(u => ({
        ...u,
        _history: [],
        _historyLoading: false,
        _historyPage: 1,
        _historySize: 5,
        _historyTotal: 0
      }))
      applyFilters()
    }
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  displayList.value = userList.value.filter(u => {
    if (filterUsername.value && !u.username.toLowerCase().includes(filterUsername.value.toLowerCase())) {
      return false
    }
    if (filterUserType.value && u.userType !== filterUserType.value) {
      return false
    }
    if (filterLoginStatus.value === 'LOGGED_IN' && !u.lastLoginTime) {
      return false
    }
    if (filterLoginStatus.value === 'NEVER' && u.lastLoginTime) {
      return false
    }
    return true
  })
}

const handleExpand = async (row, expandedRows) => {
  if (expandedRows.includes(row)) {
    row._historyLoading = true
    row._historyPage = 1
    try {
      const res = await loginLogApi.getUserLoginHistory({
        userId: row.userId,
        userType: row.userType,
        page: 0,
        size: row._historySize
      })
      if (res.data.code === 200) {
        row._history = res.data.data.content || []
        row._historyTotal = res.data.data.totalElements || 0
      }
    } finally {
      row._historyLoading = false
    }
  }
}

const handleHistoryPage = async (row, page) => {
  row._historyPage = page
  row._historyLoading = true
  try {
    const res = await loginLogApi.getUserLoginHistory({
      userId: row.userId,
      userType: row.userType,
      page: page - 1,
      size: row._historySize
    })
    if (res.data.code === 200) {
      row._history = res.data.data.content || []
      row._historyTotal = res.data.data.totalElements || 0
    }
  } finally {
    row._historyLoading = false
  }
}

const handleCleanup = async () => {
  if (!cleanupDate.value) return
  cleanupLoading.value = true
  try {
    const res = await loginLogApi.cleanOldLogs(cleanupDate.value)
    if (res.data.code === 200) {
      ElMessage.success('清理成功')
      cleanupDate.value = ''
      await fetchUserSummary()
    } else {
      ElMessage.error(res.data.message || '清理失败')
    }
  } catch (e) {
    ElMessage.error('清理失败')
  } finally {
    cleanupLoading.value = false
  }
}

onMounted(() => {
  fetchUserSummary()
})
</script>

<style scoped>
.login-monitor {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: var(--bg-glass);
  border-radius: 12px;
  margin-bottom: 8px;
}

.expand-content {
  padding: 20px 40px;
}

.expand-content h4 {
  margin: 0 0 12px 0;
  color: var(--text-primary, #1e293b);
  font-size: 15px;
}

.history-pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.cleanup-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.08);
  border-radius: 10px;
  margin-bottom: 8px;
}

.cleanup-label {
  font-size: 14px;
  font-weight: 500;
  color: #f87171;
  white-space: nowrap;
}

.cleanup-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 8px;
}
</style>
