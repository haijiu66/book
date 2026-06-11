<template>
  <div class="admin-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>管理员管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加管理员
          </el-button>
        </div>
      </template>

      <el-table :data="adminList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tableName" label="数据表" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑管理员' : '添加管理员'"
      width="500px"
    >
      <el-form :model="adminForm" :rules="formRules" ref="adminFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username" v-if="!isEdit">
          <el-input v-model="adminForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="adminForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="adminForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="adminForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="adminForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="新密码" v-if="isEdit">
          <el-input v-model="adminForm.password" type="password" placeholder="留空则不修改密码" show-password />
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-radio-group v-model="adminForm.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类权限" v-if="isEdit">
          <el-checkbox-group v-model="adminFormPermissions">
            <el-checkbox label="CATEGORY_ADD">添加图书分类</el-checkbox>
            <el-checkbox label="CATEGORY_DELETE">删除图书分类</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '../../api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const adminFormRef = ref(null)
const adminList = ref([])

const adminForm = ref({
  id: null,
  username: '',
  password: '',
  name: '',
  phone: '',
  email: '',
  status: 'ACTIVE',
  permissions: ''
})

const adminFormPermissions = ref([])

const formRules = computed(() => {
  if (isEdit.value) {
    return {
      name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
    }
  }
  return {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
  }
})

// 获取管理员列表
const fetchAdmins = async () => {
  loading.value = true
  try {
    const res = await adminApi.getAllAdmins()
    if (res.data.code === 200) {
      adminList.value = res.data.data || []
    } else {
      ElMessage.error(res.data.message || '获取管理员列表失败')
    }
  } catch (error) {
    console.error('获取管理员列表失败', error)
    ElMessage.error('获取管理员列表失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  adminForm.value = {
    id: null,
    username: '',
    password: '',
    name: '',
    phone: '',
    email: '',
    status: 'ACTIVE',
    permissions: ''
  }
  adminFormPermissions.value = []
}

// 添加管理员
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑管理员
const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  adminForm.value.id = row.id
  adminForm.value.name = row.name || ''
  adminForm.value.phone = row.phone || ''
  adminForm.value.email = row.email || ''
  adminForm.value.status = row.status || 'ACTIVE'
  adminForm.value.permissions = row.permissions || ''
  adminFormPermissions.value = (row.permissions || '').split(',').filter(Boolean)
  dialogVisible.value = true
}

// 删除管理员
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该管理员吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await adminApi.deleteAdmin(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchAdmins()
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

// 提交表单
const handleSubmit = async () => {
  if (!adminFormRef.value) return
  await adminFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        let res
        if (isEdit.value) {
          adminForm.value.permissions = adminFormPermissions.value.join(',')
          res = await adminApi.updateAdmin(adminForm.value.id, adminForm.value)
        } else {
          res = await adminApi.createAdmin(adminForm.value)
        }
        if (res.data.code === 200) {
          ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
          dialogVisible.value = false
          fetchAdmins()
        } else {
          ElMessage.error(res.data.message || '操作失败')
        }
      } catch (error) {
        console.error('操作失败', error)
        ElMessage.error('操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchAdmins()
})
</script>

<style scoped>
.admin-management {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>