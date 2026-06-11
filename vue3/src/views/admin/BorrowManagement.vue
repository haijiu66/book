<template>
  <div class="borrow-management">
    <el-card class="search-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索借阅记录（用户名、书名、ISBN）"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="statusFilter" placeholder="借阅状态" clearable style="width: 100%">
            <el-option label="借阅中" value="BORROWED" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已逾期" value="OVERDUE" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
        <el-col :span="10" style="text-align: right">
          <el-button type="primary" @click="handleBorrow">
            <el-icon><Plus /></el-icon>
            借书登记
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" style="margin-top: 20px">
      <el-table :data="borrowList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="username" label="借阅人" width="120"></el-table-column>
        <el-table-column prop="bookTitle" label="书名" min-width="180"></el-table-column>
        <el-table-column prop="isbn" label="ISBN" width="150"></el-table-column>
        <el-table-column label="借阅状态" width="100">
          <template #default="{ row }">
            <el-tag :type="borrowStore.getStatusType(row.status)">
              {{ borrowStore.getStatusText(row.status) }}
            </el-tag>
            <el-tag v-if="borrowStore.isOverdue(row)" type="danger" size="small" style="margin-left: 5px">
              逾期
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="borrowDate" label="借书日期" width="120"></el-table-column>
        <el-table-column prop="dueDate" label="应还日期" width="120">
          <template #default="{ row }">
            <span :class="{ 'overdue': borrowStore.isOverdue(row) }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="returnDate" label="归还日期" width="120"></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'BORROWED'"
              type="success"
              size="small"
              @click="handleReturn(row)"
            >
              还书
            </el-button>
            <el-button
              v-if="row.status === 'BORROWED' && !borrowStore.isOverdue(row)"
              type="primary"
              size="small"
              @click="handleRenew(row)"
            >
              续借
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 借书登记对话框 -->
    <el-dialog v-model="borrowDialogVisible" title="借书登记" width="500px">
      <el-form :model="borrowForm" :rules="borrowRules" ref="borrowFormRef" label-width="100px">
        <el-form-item label="图书ID" prop="bookId">
          <el-input-number v-model="borrowForm.bookId" :min="1" style="width: 100%" placeholder="请输入图书ID" />
        </el-form-item>
        <el-form-item label="借阅天数" prop="borrowDays">
          <el-input-number v-model="borrowForm.borrowDays" :min="1" :max="365" style="width: 100%" placeholder="默认30天" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBorrowSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useBorrowStore } from '../../stores/borrow'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const borrowStore = useBorrowStore()
const searchKeyword = ref('')
const statusFilter = ref('')
const borrowDialogVisible = ref(false)
const borrowFormRef = ref(null)

const borrowForm = ref({
  bookId: null,
  borrowDays: null
})

const borrowRules = {
  bookId: [{ required: true, message: '请输入图书ID', trigger: 'blur' }]
}

const borrowList = ref([])
const loading = ref(false)

onMounted(async () => {
  await fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value) params.status = statusFilter.value
    await borrowStore.fetchBorrows(params)
    borrowList.value = borrowStore.borrows
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  await fetchData()
}

function handleBorrow() {
  borrowForm.value = { bookId: null, borrowDays: null }
  borrowDialogVisible.value = true
}

async function handleBorrowSubmit() {
  if (!borrowFormRef.value) return
  await borrowFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await borrowStore.borrowBook(borrowForm.value)
        ElMessage.success('借书登记成功')
        borrowDialogVisible.value = false
        await fetchData()
      } catch (error) {
        ElMessage.error(error.message || '借书登记失败')
      }
    }
  })
}

async function handleReturn(row) {
  try {
    await ElMessageBox.confirm('确定要归还这本书吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await borrowStore.returnBook(row.id)
    ElMessage.success('还书成功')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '还书失败')
    }
  }
}

async function handleRenew(row) {
  try {
    await ElMessageBox.confirm('确定要续借这本书吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await borrowStore.renewBook(row.id)
    ElMessage.success('续借成功')
    await fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '续借失败')
    }
  }
}
</script>

<style scoped>
.borrow-management {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.search-card {
  border-radius: 16px;
  border: 1px solid var(--border-color, var(--border-color));
}

.table-card {
  border-radius: 16px;
  border: 1px solid var(--border-color, var(--border-color));
}

.overdue {
  color: #dc2626;
  font-weight: 600;
}
</style>
