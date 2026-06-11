<template>
  <div class="user-management-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h3>普通用户管理</h3>
        <span class="user-count">共 {{ userList.length }} 位用户</span>
      </div>
      <button class="add-btn" @click="handleAdd">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        添加用户
      </button>
    </div>

    <!-- 用户列表 -->
    <div class="table-section">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>正在加载...</span>
      </div>

      <div v-else class="table-wrapper">
        <el-table :data="userList" stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="60"></el-table-column>
          <el-table-column prop="username" label="用户名" width="140"></el-table-column>
          <el-table-column prop="name" label="姓名" width="120"></el-table-column>
          <el-table-column prop="phone" label="手机号" width="120"></el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="160"></el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="row.status === 'ACTIVE' ? 'active' : 'inactive'">
                {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <button class="action-btn edit-btn" @click="handleEdit(row)">编辑</button>
                <button class="action-btn delete-btn" @click="handleDelete(row)">删除</button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="480px"
      class="custom-dialog"
    >
      <div class="dialog-form">
        <div v-if="!isEdit" class="form-row">
          <div class="form-item required">
            <label>用户名</label>
            <input v-model="userForm.username" type="text" placeholder="请输入用户名" />
          </div>
        </div>
        <div v-if="!isEdit" class="form-row">
          <div class="form-item required">
            <label>密码</label>
            <input v-model="userForm.password" type="password" placeholder="请输入密码" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-item required">
            <label>姓名</label>
            <input v-model="userForm.name" type="text" placeholder="请输入姓名" />
          </div>
        </div>
        <div class="form-row two-cols">
          <div class="form-item">
            <label>手机号</label>
            <input v-model="userForm.phone" type="text" placeholder="请输入手机号" />
          </div>
          <div class="form-item">
            <label>邮箱</label>
            <input v-model="userForm.email" type="email" placeholder="请输入邮箱" />
          </div>
        </div>
        <div v-if="isEdit" class="form-row">
          <div class="form-item">
            <label>新密码</label>
            <input v-model="userForm.password" type="password" placeholder="留空则不修改密码" />
          </div>
        </div>
        <div v-if="isEdit" class="form-row">
          <div class="form-item">
            <label>状态</label>
            <div class="status-selector">
              <button
                class="status-btn"
                :class="{ active: userForm.status === 'ACTIVE' }"
                @click="userForm.status = 'ACTIVE'"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                启用
              </button>
              <button
                class="status-btn"
                :class="{ active: userForm.status === 'INACTIVE' }"
                @click="userForm.status = 'INACTIVE'"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="15" y1="9" x2="9" y2="15"></line>
                  <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
                禁用
              </button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="cancel-btn" @click="dialogVisible = false">取消</button>
          <button class="confirm-btn" @click="handleSubmit">确认</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '../../api/user'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const userList = ref([])

const userForm = ref({
  id: null,
  username: '',
  password: '',
  name: '',
  phone: '',
  email: '',
  status: 'ACTIVE'
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getAllUsers()
    if (res.data.code === 200) {
      userList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.message || '获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  userForm.value = {
    id: null,
    username: '',
    password: '',
    name: '',
    phone: '',
    email: '',
    status: 'ACTIVE'
  }
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  userForm.value.id = row.id
  userForm.value.name = row.name || ''
  userForm.value.phone = row.phone || ''
  userForm.value.email = row.email || ''
  userForm.value.status = row.status || 'ACTIVE'
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await userApi.deleteUser(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchUsers()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!userForm.value.name) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!isEdit.value && !userForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!isEdit.value && !userForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await userApi.updateUser(userForm.value.id, userForm.value)
    } else {
      res = await userApi.createUser(userForm.value)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      fetchUsers()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (error) {
    console.error('操作失败', error)
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 页面头部 */
.page-header {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-count {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.add-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-btn svg {
  width: 18px;
  height: 18px;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

/* 表格区域 */
.table-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
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
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 状态标签 */
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
}

.status-badge.inactive {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.edit-btn {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
}

.edit-btn:hover {
  background: rgba(59, 130, 246, 0.2);
}

.delete-btn {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
}

/* 对话框样式 */
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 0;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row.two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.form-item.required label::after {
  content: ' *';
  color: #dc2626;
}

.form-item input {
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-glass);
  transition: all 0.2s ease;
}

.form-item input:focus {
  border-color: #6366f1;
  background: var(--bg-card);
  outline: none;
}

.status-selector {
  display: flex;
  gap: 12px;
}

.status-btn {
  flex: 1;
  padding: 10px 16px;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-card);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-secondary);
}

.status-btn svg {
  width: 16px;
  height: 16px;
}

.status-btn:hover {
  border-color: #c7d2fe;
}

.status-btn.active {
  border-color: var(--accent-1);
  background: rgba(99, 102, 241, 0.12);
  color: var(--accent-2);
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.cancel-btn {
  padding: 10px 20px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
}

.cancel-btn:hover {
  background: var(--bg-hover);
}

.confirm-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(99, 102, 241, 0.3);
}

/* 响应式 */
@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
  }

  .form-row.two-cols {
    grid-template-columns: 1fr;
  }
}
</style>