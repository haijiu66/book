<template>
  <div class="category-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图书分类管理</span>
          <el-button v-if="authStore.hasPermission('CATEGORY_ADD')" type="primary" @click="handleAdd(null)">
            <el-icon><Plus /></el-icon>
            添加根分类
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar" style="margin-bottom:16px;display:flex;align-items:center;gap:12px;background:var(--bg-glass);border:2px solid var(--border-color);border-radius:12px;padding:10px 16px">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;color:var(--text-muted)"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <input v-model="searchKeyword" type="text" placeholder="搜索分类名称" @input="filterTree" style="flex:1;border:none;outline:none;font-size:15px;background:transparent;color:var(--text-primary)" />
        <button v-if="searchKeyword" @click="searchKeyword='';filterTree()" style="background:none;border:none;cursor:pointer;color:var(--text-muted);padding:0;font-size:20px">&times;</button>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>正在加载...</span>
      </div>

      <div v-else-if="treeData.length === 0" class="empty-state">
        <span>暂无分类</span>
      </div>

      <div v-else class="tree-wrapper">
        <el-tree
          ref="treeRef"
          :data="treeData"
          node-key="id"
          :props="treeProps"
          default-expand-all
          highlight-current
          :filter-node-method="filterNode"
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <div class="node-content">
                <span class="node-name">{{ data.name }}</span>
                <span v-if="data.description" class="node-desc">{{ data.description }}</span>
                <span class="node-time">{{ formatTime(data.createTime) }}</span>
              </div>
              <div class="node-actions">
                <el-button
                  v-if="authStore.hasPermission('CATEGORY_ADD')"
                  size="small"
                  link
                  type="primary"
                  @click.stop="handleAdd(data)"
                >添加子分类</el-button>
                <el-button
                  v-if="authStore.hasPermission('CATEGORY_ADD')"
                  size="small"
                  link
                  type="primary"
                  @click.stop="handleEdit(data)"
                >编辑</el-button>
                <el-button
                  v-if="authStore.hasPermission('CATEGORY_DELETE')"
                  size="small"
                  link
                  type="danger"
                  @click.stop="handleDelete(data)"
                >删除</el-button>
              </div>
            </div>
          </template>
        </el-tree>
      </div>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述（可选）"
          />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" placeholder="不选则为根分类" clearable style="width: 100%">
            <el-option
              v-for="cat in flatCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
              :disabled="isEdit && cat.id === editingId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { categoryApi } from '../../api/category'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const treeRef = ref(null)
const treeData = ref([])
const flatCategories = ref([])
const searchKeyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const parentCategory = ref(null)
const formRef = ref(null)

const treeProps = { label: 'name', children: 'children' }

const form = ref({ name: '', description: '', parentId: null })

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑分类'
  if (parentCategory.value) return '添加子分类 — ' + parentCategory.value.name
  return '添加根分类'
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

const filterNode = (value, data) => {
  if (!value) return true
  return (data.name || '').toLowerCase().includes(value.toLowerCase())
}

const filterTree = () => {
  treeRef.value?.filter(searchKeyword.value)
}

const fetchCategories = async () => {
  loading.value = true
  try {
    const [treeRes, flatRes] = await Promise.all([
      categoryApi.getTree(),
      categoryApi.getAll()
    ])
    if (treeRes.data.code === 200) {
      treeData.value = treeRes.data.data || []
    }
    if (flatRes.data.code === 200) {
      flatCategories.value = flatRes.data.data || []
    }
  } catch (e) {
    console.error('获取分类列表失败', e)
  } finally {
    loading.value = false
  }
}

function handleAdd(parent) {
  isEdit.value = false
  editingId.value = null
  parentCategory.value = parent
  form.value = { name: '', description: '', parentId: parent ? parent.id : null }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  parentCategory.value = null
  form.value = {
    name: row.name,
    description: row.description || '',
    parentId: row.parentId || null
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await categoryApi.update(editingId.value, form.value)
        ElMessage.success('更新成功')
      } else {
        await categoryApi.create(form.value)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      await fetchCategories()
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      (row.children && row.children.length > 0)
        ? `「${row.name}」下还有 ${row.children.length} 个子分类，请先删除子分类`
        : `确定要删除分类「${row.name}」吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await categoryApi.delete(row.id)
    ElMessage.success('删除成功')
    await fetchCategories()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.category-management {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px;
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

@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  display: flex;
  justify-content: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.tree-wrapper {
  border-radius: 12px;
  overflow: hidden;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding: 4px 8px;
  gap: 12px;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.node-desc {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.node-time {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  margin-left: auto;
}

.node-actions {
  display: flex;
  gap: 4px;
  opacity: 0.4;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
</style>
