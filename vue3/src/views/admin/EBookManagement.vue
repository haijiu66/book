<template>
  <div class="ebook-management">
    <el-card class="header-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索电子书（书名、作者）"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button type="primary" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon>
            上传电子书
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" style="margin-top: 20px" v-loading="loading">
      <el-empty v-if="ebookList.length === 0 && !loading" description="暂无电子书">
        <el-button type="primary" @click="showUploadDialog = true">上传电子书</el-button>
      </el-empty>
      <el-table v-else :data="ebookList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="title" label="书名" min-width="200"></el-table-column>
        <el-table-column prop="author" label="作者" width="120"></el-table-column>
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="fileType" label="格式" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节数" width="80"></el-table-column>
        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            <template v-if="(row.categories || []).length > 0">
              <el-tag v-for="cat in row.categories" :key="cat.id" size="small" style="margin-right:4px">{{ cat.name }}</el-tag>
            </template>
            <el-tag v-else type="warning" size="small">未分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="160"></el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isSuperAdmin && (!row.categories || row.categories.length === 0)" type="warning" size="small" @click="handleRecategorize(row)">分类</el-button>
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="handleCoverDialog(row)">封面</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传电子书"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            accept=".txt,.docx"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 TXT 和 DOCX 格式文件
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="uploadForm.title" placeholder="可留空，系统将自动识别" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="uploadForm.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="uploadForm.categoryIds" placeholder="请选择分类（必选）" multiple filterable style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="封面">
          <div style="display:flex;align-items:center;gap:12px">
            <img v-if="uploadForm.coverPreview" :src="uploadForm.coverPreview" class="cover-preview" />
            <el-upload :auto-upload="false" :limit="1" :on-change="handleEbookCoverChange" accept="image/*">
              <el-button size="small" type="primary">选择图片</el-button>
            </el-upload>
            <el-button v-if="uploadForm.coverPreview" size="small" type="danger" @click="uploadForm.coverPreview=''; ebookCoverFile=null">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" title="编辑电子书" width="500px">
      <el-form label-width="80px">
        <el-form-item label="书名">
          <el-input v-model="editForm.title" placeholder="书名" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="editForm.author" placeholder="作者" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryIds" placeholder="请选择分类" multiple filterable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="简介" />
        </el-form-item>
        <el-form-item label="封面">
          <div style="display:flex;align-items:center;gap:12px">
            <img v-if="editForm.coverPreview" :src="editForm.coverPreview" class="cover-preview" />
            <el-upload :auto-upload="false" :limit="1" :on-change="handleEditCoverChange" accept="image/*">
              <el-button size="small">选择图片</el-button>
            </el-upload>
            <el-button v-if="editForm.coverPreview" size="small" type="danger" @click="editForm.coverPreview=''; editCoverFile=null">移除</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 补充分类对话框 -->
    <el-dialog v-model="recategorizeVisible" title="补充分类" width="420px">
      <el-select v-model="recategorizeIds" placeholder="请选择分类" multiple filterable style="width: 100%">
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <template #footer>
        <el-button @click="recategorizeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecategorizeSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 封面对话框 -->
    <el-dialog v-model="coverDialogVisible" title="上传封面" width="400px">
      <div style="display:flex;flex-direction:column;align-items:center;gap:12px">
        <img v-if="coverPreview" :src="coverPreview" class="cover-preview-lg" />
        <el-upload :auto-upload="false" :limit="1" :on-change="handleCoverFileChange" accept="image/*">
          <el-button type="primary">选择图片</el-button>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="coverDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCoverSubmit">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useEBookStore } from '../../stores/ebook'
import { useAuthStore } from '../../stores/auth'
import { uploadEBook, deleteEBook, ebookApi } from '../../api/ebook'
import { categoryApi } from '../../api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue'

const router = useRouter()
const ebookStore = useEBookStore()
const authStore = useAuthStore()
const isSuperAdmin = authStore.isSuperAdmin

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const recategorizeVisible = ref(false)
const recategorizeIds = ref([])
const recategorizeEbookId = ref(null)
const searchKeyword = ref('')
const uploadRef = ref(null)
const ebookList = ref([])
const ebookCoverFile = ref(null)
const coverDialogVisible = ref(false)
const coverPreview = ref('')
const coverFile = ref(null)
const coverEbookId = ref(null)
const editVisible = ref(false)
const editCoverFile = ref(null)
const editForm = ref({ id: null, title: '', author: '', categoryIds: [], description: '', coverPreview: '' })

const categories = ref([])

const uploadForm = ref({
  file: null,
  title: '',
  author: '',
  categoryIds: [],
  coverPreview: '',
  description: ''
})

const loadEBookList = async () => {
  loading.value = true
  try {
    await ebookStore.loadEBookList()
    ebookList.value = ebookStore.ebooks
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  loading.value = true
  try {
    await ebookStore.loadEBookList()
    if (searchKeyword.value) {
      ebookList.value = ebookStore.ebooks.filter(ebook =>
        ebook.title?.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        ebook.author?.toLowerCase().includes(searchKeyword.value.toLowerCase())
      )
    } else {
      ebookList.value = ebookStore.ebooks
    }
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  uploadForm.value.file = file.raw
  if (!uploadForm.value.title) {
    const name = file.name
    const lastDot = name.lastIndexOf('.')
    uploadForm.value.title = lastDot > 0 ? name.substring(0, lastDot) : name
  }
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

const handleUpload = async () => {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择文件')
    return
  }
  if (!uploadForm.value.categoryIds || uploadForm.value.categoryIds.length === 0) {
    ElMessage.warning('请至少选择一个分类')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    if (uploadForm.value.title) {
      formData.append('title', uploadForm.value.title)
    }
    if (uploadForm.value.author) {
      formData.append('author', uploadForm.value.author)
    }
    if (uploadForm.value.categoryIds && uploadForm.value.categoryIds.length > 0) {
      uploadForm.value.categoryIds.forEach(id => formData.append('categoryIds', id))
    }
    if (uploadForm.value.description) {
      formData.append('description', uploadForm.value.description)
    }

    const response = await uploadEBook(formData)
    if (response.data.code === 200) {
      // 上传封面
      const ebookId = response.data.data?.id
      if (ebookId && ebookCoverFile.value) {
        const fd = new FormData()
        fd.append('file', ebookCoverFile.value)
        await ebookApi.uploadCover(ebookId, fd)
      }
      ElMessage.success('上传成功')
      showUploadDialog.value = false
      resetUploadForm()
      await loadEBookList()
    } else {
      ElMessage.error(response.data.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const resetUploadForm = () => {
  uploadForm.value = {
    file: null, title: '', author: '',
    categoryIds: [], coverPreview: '', description: ''
  }
  ebookCoverFile.value = null
  if (uploadRef.value) uploadRef.value.clearFiles()
}

const handleEbookCoverChange = (file) => {
  ebookCoverFile.value = file.raw
  uploadForm.value.coverPreview = URL.createObjectURL(file.raw)
}

const handleEdit = (row) => {
  editForm.value = {
    id: row.id,
    title: row.title || '',
    author: row.author || '',
    categoryIds: (row.categories || []).map(c => c.id),
    description: row.description || '',
    coverPreview: row.coverPath || ''
  }
  editCoverFile.value = null
  editVisible.value = true
}

const handleEditCoverChange = (file) => {
  editCoverFile.value = file.raw
  editForm.value.coverPreview = URL.createObjectURL(file.raw)
}

const handleEditSubmit = async () => {
  try {
    await ebookApi.updateEBook(editForm.value.id, {
      title: editForm.value.title,
      author: editForm.value.author,
      categoryIds: editForm.value.categoryIds,
      description: editForm.value.description
    })
    if (editCoverFile.value) {
      const fd = new FormData()
      fd.append('file', editCoverFile.value)
      await ebookApi.uploadCover(editForm.value.id, fd)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadEBookList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

const handleCoverDialog = (row) => {
  coverEbookId.value = row.id
  coverPreview.value = row.coverPath || ''
  coverFile.value = null
  coverDialogVisible.value = true
}

const handleCoverFileChange = (file) => {
  coverFile.value = file.raw
  coverPreview.value = URL.createObjectURL(file.raw)
}

const handleCoverSubmit = async () => {
  if (!coverFile.value) { ElMessage.warning('请选择图片'); return }
  try {
    const fd = new FormData()
    fd.append('file', coverFile.value)
    await ebookApi.uploadCover(coverEbookId.value, fd)
    ElMessage.success('封面上传成功')
    coverDialogVisible.value = false
    await loadEBookList()
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

const handleView = (row) => {
  router.push(`/reader/ebooks/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除电子书 "${row.title}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await deleteEBook(row.id)
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      await loadEBookList()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (e) {}
}

function handleRecategorize(row) {
  recategorizeEbookId.value = row.id
  recategorizeIds.value = []
  recategorizeVisible.value = true
}

async function handleRecategorizeSubmit() {
  if (!recategorizeIds.value.length) {
    ElMessage.warning('请选择分类')
    return
  }
  try {
    await ebookApi.updateCategories(recategorizeEbookId.value, recategorizeIds.value)
    ElMessage.success('分类更新成功')
    recategorizeVisible.value = false
    await loadEBookList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  }
}

onMounted(() => {
  loadEBookList()
  loadCategories()
})
</script>

<style scoped>
.ebook-management {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-card {
  border-radius: 16px;
  border: 1px solid var(--border-color, var(--border-color));
}

.table-card {
  border-radius: 16px;
  border: 1px solid var(--border-color, var(--border-color));
}

.cover-preview {
  width: 60px; height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.cover-preview-lg {
  width: 160px; height: 210px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--border-color);
}
</style>