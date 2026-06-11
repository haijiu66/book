<template>
  <div class="audit-logs">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计日志</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="filterOperatorName"
          placeholder="搜索操作者名称"
          clearable
          style="width: 200px"
          @input="applyFilters"
        />
        <el-select
          v-model="filterOperatorType"
          placeholder="操作者类型"
          clearable
          style="width: 160px"
          @change="applyFilters"
        >
          <el-option label="超级管理员" value="SUPER_ADMIN" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="READER" />
        </el-select>
        <el-select
          v-model="filterOperationType"
          placeholder="操作类型"
          clearable
          style="width: 160px"
          @change="applyFilters"
        >
          <el-option
            v-for="opt in operationTypeOptions"
            :key="opt"
            :label="opt"
            :value="opt"
          />
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
        :row-key="(row) => row.operatorType + '_' + row.operatorId"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <h4>{{ row.operatorName }} 的操作记录</h4>
              <el-table
                v-loading="row._historyLoading"
                :data="row._history || []"
                stripe
                size="small"
              >
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="createTime" label="操作时间" width="180" />
                <el-table-column prop="operationType" label="操作类型" width="160" />
                <el-table-column prop="operationDetail" label="操作详情" show-overflow-tooltip />
                <el-table-column prop="targetType" label="目标类型" width="100" />
                <el-table-column prop="targetId" label="目标ID" width="80" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row: log }">
                    <el-tag :type="log.operationStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
                      {{ log.operationStatus === 'SUCCESS' ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="ipAddress" label="IP地址" width="140" />
                <el-table-column label="错误信息" show-overflow-tooltip>
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
        <el-table-column prop="operatorName" label="操作者" width="140" />
        <el-table-column label="操作者类型" width="130">
          <template #default="{ row }">
            <el-tag
              :type="row.operatorType === 'SUPER_ADMIN' ? 'danger' : row.operatorType === 'ADMIN' ? 'warning' : 'success'"
              size="small"
            >
              {{ row.operatorType === 'SUPER_ADMIN' ? '超级管理员' : row.operatorType === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditCount" label="操作次数" width="100" />
        <el-table-column label="最近操作时间" width="180">
          <template #default="{ row }">
            {{ row.lastAuditTime || '暂无记录' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '../../api/audit'

const loading = ref(false)
const userList = ref([])
const displayList = ref([])

const filterOperatorName = ref('')
const filterOperatorType = ref('')
const filterOperationType = ref('')
const cleanupDate = ref('')
const cleanupLoading = ref(false)

const operationTypeOptions = computed(() => {
  const types = new Set()
  userList.value.forEach(u => {
    if (u._history) {
      u._history.forEach(h => types.add(h.operationType))
    }
  })
  return [...types].sort()
})

const fetchUserSummary = async () => {
  loading.value = true
  try {
    const res = await auditApi.getUserAuditSummary()
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
    } else {
      ElMessage.error(res.data.message || '获取审计日志汇总失败')
    }
  } catch (e) {
    console.error('获取审计日志汇总失败', e)
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  displayList.value = userList.value.filter(u => {
    if (filterOperatorName.value && !u.operatorName.toLowerCase().includes(filterOperatorName.value.toLowerCase())) {
      return false
    }
    if (filterOperatorType.value && u.operatorType !== filterOperatorType.value) {
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
      const res = await auditApi.getUserAuditHistory({
        operatorType: row.operatorType,
        operatorId: row.operatorId,
        page: 0,
        size: row._historySize
      })
      if (res.data.code === 200) {
        row._history = res.data.data.content || []
        row._historyTotal = res.data.data.totalElements || 0
      } else {
        ElMessage.error(res.data.message || '获取操作记录失败')
      }
    } catch (e) {
      console.error('获取操作记录失败', e)
    } finally {
      row._historyLoading = false
    }
  }
}

const handleHistoryPage = async (row, page) => {
  row._historyPage = page
  row._historyLoading = true
  try {
    const res = await auditApi.getUserAuditHistory({
      operatorType: row.operatorType,
      operatorId: row.operatorId,
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
    const res = await auditApi.cleanOldLogs(cleanupDate.value)
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
.audit-logs {
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
