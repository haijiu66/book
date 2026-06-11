<template>
  <div class="borrow-history-page">
    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-box">
        <div class="search-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.35-4.35"></path>
          </svg>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索借阅记录（书名、ISBN）"
            @keyup.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="filter-select">
          <select v-model="statusFilter" @change="handleSearch">
            <option value="">全部状态</option>
            <option value="BORROWED">借阅中</option>
            <option value="RETURNED">已归还</option>
            <option value="OVERDUE">已逾期</option>
            <option value="CANCELLED">已取消</option>
          </select>
        </div>
        <button class="search-btn" @click="handleSearch">筛选</button>
      </div>
    </div>

    <!-- 逾期提醒 -->
    <div v-if="hasOverdue" class="alert-card overdue">
      <div class="alert-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
      </div>
      <div class="alert-content">
        <h4>逾期提醒</h4>
        <p>您有逾期未还的图书，请尽快归还！</p>
      </div>
    </div>

    <!-- 借阅记录列表 -->
    <div class="records-section">
      <div class="section-header">
        <h3>借阅记录</h3>
        <span class="record-count">共 {{ borrowList.length }} 条记录</span>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>正在加载...</span>
      </div>

      <div v-else-if="borrowList.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
          <line x1="1" y1="10" x2="23" y2="10"></line>
        </svg>
        <p>暂无借阅记录</p>
        <span>快去图书列表借阅你感兴趣的图书吧</span>
      </div>

      <div v-else class="records-list">
        <div
          v-for="record in borrowList"
          :key="record.id"
          class="record-card"
          :class="{ overdue: borrowStore.isOverdue(record) }"
        >
          <div class="record-header">
            <div class="book-info">
              <h4 class="book-title">{{ record.bookTitle }}</h4>
              <span class="book-isbn">{{ record.isbn }}</span>
            </div>
            <div class="status-section">
              <span class="status-badge" :class="getStatusClass(record.status)">
                {{ borrowStore.getStatusText(record.status) }}
              </span>
              <span v-if="borrowStore.isOverdue(record)" class="overdue-badge">逾期</span>
            </div>
          </div>
          <div class="record-body">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">借书日期</span>
                <span class="info-value">{{ record.borrowDate }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">应还日期</span>
                <span class="info-value" :class="{ 'overdue-text': borrowStore.isOverdue(record) }">
                  {{ record.dueDate }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">归还日期</span>
                <span class="info-value">{{ record.returnDate || '未归还' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">借阅ID</span>
                <span class="info-value">#{{ record.id }}</span>
              </div>
            </div>
          </div>
          <div v-if="record.status === 'BORROWED' && !borrowStore.isOverdue(record)" class="record-footer">
            <button class="renew-btn" @click="handleRenew(record)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10"></polyline>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
              申请续借
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 续借对话框 -->
    <el-dialog v-model="renewDialogVisible" title="申请续借" width="460px">
      <div class="renew-form">
        <div class="renew-info">
          <div class="info-row"><span class="info-label">书名：</span><span>{{ renewRecord?.bookTitle }}</span></div>
          <div class="info-row"><span class="info-label">当前应还日期：</span><span>{{ renewRecord?.dueDate }}</span></div>
          <div class="info-row"><span class="info-label">最大可续至：</span><span>{{ maxRenewDate }}</span></div>
        </div>
        <div class="renew-date-picker">
          <label>选择归还日期：</label>
          <el-date-picker
            v-model="selectedRenewDate"
            type="date"
            placeholder="请选择归还日期"
            :disabled-date="disabledRenewDate"
            :default-value="renewDefaultDate"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenewSubmit" :loading="renewLoading">确认续借</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useBorrowStore } from '../../stores/borrow'

const borrowStore = useBorrowStore()
const searchKeyword = ref('')
const statusFilter = ref('')
const borrowList = ref([])
const loading = ref(false)
const renewDialogVisible = ref(false)
const renewRecord = ref(null)
const selectedRenewDate = ref('')
const renewLoading = ref(false)

const maxRenewDate = computed(() => {
  if (!renewRecord.value?.dueDate) return ''
  const d = new Date(renewRecord.value.dueDate)
  d.setDate(d.getDate() + 120)
  return d.toISOString().substring(0, 10)
})

const renewDefaultDate = computed(() => {
  if (!renewRecord.value?.dueDate) return new Date()
  const d = new Date(renewRecord.value.dueDate)
  d.setDate(d.getDate() + 30)
  return d
})

const disabledRenewDate = (date) => {
  if (!renewRecord.value?.dueDate) return true
  const dueDate = new Date(renewRecord.value.dueDate)
  dueDate.setHours(0, 0, 0, 0)
  const minDate = new Date(dueDate)
  minDate.setDate(minDate.getDate() + 1)
  const maxDate = new Date(dueDate)
  maxDate.setDate(maxDate.getDate() + 120)
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  if (d < minDate) return true
  if (d > maxDate) return true
  return false
}

const hasOverdue = computed(() => {
  return borrowList.value.some(item => borrowStore.isOverdue(item))
})

const getStatusClass = (status) => {
  switch (status) {
    case 'BORROWED':
      return 'borrowed'
    case 'RETURNED':
      return 'returned'
    case 'OVERDUE':
      return 'overdue'
    case 'CANCELLED':
      return 'cancelled'
    default:
      return 'default'
  }
}

onMounted(async () => {
  await fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value) params.status = statusFilter.value
    await borrowStore.fetchMyBorrows(params)
    borrowList.value = borrowStore.myBorrows
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchKeyword.value = ''
  statusFilter.value = ''
  fetchData()
}

async function handleSearch() {
  await fetchData()
}

function handleRenew(row) {
  renewRecord.value = row
  selectedRenewDate.value = ''
  renewDialogVisible.value = true
}

async function handleRenewSubmit() {
  if (!selectedRenewDate.value) {
    ElMessage.warning('请选择归还日期')
    return
  }
  renewLoading.value = true
  try {
    await borrowStore.renewBook(renewRecord.value.id, selectedRenewDate.value)
    ElMessage.success('续借成功')
    renewDialogVisible.value = false
    await fetchData()
  } catch (error) {
    ElMessage.error(error.message || '续借失败')
  } finally {
    renewLoading.value = false
  }
}
</script>

<style scoped>
.borrow-history-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 搜索区域 */
.search-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid var(--border-color);
}

.search-box {
  display: flex;
  gap: 12px;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-glass);
  border: 2px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 16px;
  transition: all 0.2s ease;
}

.search-input-wrapper:focus-within {
  border-color: #6366f1;
  background: var(--bg-card);
}

.search-icon {
  width: 20px;
  height: 20px;
  color: var(--text-muted);
}

.search-input-wrapper input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: var(--text-primary);
  background: transparent;
}

.search-input-wrapper input::placeholder {
  color: var(--text-muted);
}

.clear-btn {
  width: 20px;
  height: 20px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn svg {
  width: 16px;
  height: 16px;
}

.filter-select select {
  padding: 12px 16px;
  border: 2px solid var(--border-color);
  border-radius: 12px;
  font-size: 15px;
  color: var(--text-primary);
  background: var(--bg-glass);
  cursor: pointer;
  min-width: 120px;
}

.filter-select select:focus {
  border-color: #6366f1;
  background: var(--bg-card);
  outline: none;
}

.search-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

/* 提醒卡片 */
.alert-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  gap: 16px;
  border: 1px solid transparent;
}

.alert-card.overdue {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.12) 0%, rgba(239, 68, 68, 0.08) 100%);
  border-color: rgba(239, 68, 68, 0.25);
}

.alert-icon {
  width: 48px;
  height: 48px;
  background: var(--bg-card);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-card.overdue .alert-icon {
  color: #dc2626;
}

.alert-icon svg {
  width: 24px;
  height: 24px;
}

.alert-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.alert-card.overdue .alert-content h4 {
  color: #dc2626;
}

.alert-content p {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 记录区域 */
.records-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.record-count {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: var(--text-muted);
}

.empty-state p {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.empty-state span {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 记录列表 */
.records-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-card {
  background: var(--bg-glass);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.record-card:hover {
  background: var(--bg-card);
  border-color: var(--border-color);
  box-shadow: var(--shadow-sm);
}

.record-card.overdue {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, var(--bg-glass) 100%);
  border-color: rgba(239, 68, 68, 0.25);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.book-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.book-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.book-isbn {
  font-size: 13px;
  color: var(--text-muted);
}

.status-section {
  display: flex;
  gap: 8px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.borrowed {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
}

.status-badge.returned {
  background: rgba(5, 150, 105, 0.12);
  color: #34d399;
}

.status-badge.overdue {
  background: rgba(220, 38, 38, 0.12);
  color: #f87171;
}

.status-badge.cancelled {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.overdue-badge {
  padding: 4px 8px;
  background: #dc2626;
  color: white;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.record-body {
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
}

.overdue-text {
  color: #dc2626;
  font-weight: 500;
}

.record-footer {
  display: flex;
  justify-content: flex-end;
}

.renew-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.renew-btn svg {
  width: 16px;
  height: 16px;
}

.renew-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(99, 102, 241, 0.3);
}

/* 响应式 */
@media (max-width: 768px) {
  .search-box {
    flex-wrap: wrap;
  }

  .search-input-wrapper {
    width: 100%;
  }

  .filter-select, .search-btn {
    flex: 1;
  }

  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>