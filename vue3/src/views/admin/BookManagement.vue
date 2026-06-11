<template>
  <div class="book-management-page">
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
            placeholder="搜索图书（书名、作者、ISBN、分类）"
            @keyup.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <button class="search-btn" @click="handleSearch">搜索</button>
        <button class="add-btn" @click="handleAdd">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          添加图书
        </button>
      </div>
    </div>

    <!-- 库存预警 -->
    <div v-if="inventoryWarning.hasWarning" class="warning-banner">
      <div class="warning-banner-content">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="warning-icon">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
          <line x1="12" y1="9" x2="12" y2="13"></line>
          <line x1="12" y1="17" x2="12.01" y2="17"></line>
        </svg>
        <div class="warning-text">
          <template v-if="inventoryWarning.outOfStockCount > 0">
            <span class="warning-count danger">{{ inventoryWarning.outOfStockCount }}</span> 本已售罄
          </template>
          <template v-if="inventoryWarning.outOfStockCount > 0 && inventoryWarning.lowStockCount > 0">，</template>
          <template v-if="inventoryWarning.lowStockCount > 0">
            <span class="warning-count warn">{{ inventoryWarning.lowStockCount }}</span> 本库存不足
          </template>
        </div>
      </div>
      <button class="filter-toggle" :class="{ active: showLowStockOnly }" @click="showLowStockOnly = !showLowStockOnly">
        {{ showLowStockOnly ? '显示全部' : '只看低库存' }}
      </button>
    </div>

    <!-- 图书表格 -->
    <div class="table-section">
      <div class="table-header">
        <h3>图书列表</h3>
        <span class="record-count">共 {{ bookList.length }} 条记录<template v-if="showLowStockOnly">（已筛选）</template></span>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>正在加载...</span>
      </div>

      <div v-else class="table-wrapper">
        <el-table :data="displayBookList" stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="60"></el-table-column>
          <el-table-column prop="isbn" label="ISBN" width="130"></el-table-column>
          <el-table-column prop="title" label="书名" min-width="180"></el-table-column>
          <el-table-column prop="author" label="作者" width="100"></el-table-column>
          <el-table-column prop="publisher" label="出版社" width="120"></el-table-column>
          <el-table-column prop="publishDate" label="出版日期" width="100"></el-table-column>
          <el-table-column label="分类" width="140">
            <template #default="{ row }">
              <template v-if="(row.categories || []).length > 0">
                <el-tag v-for="cat in row.categories" :key="cat.id" size="small" style="margin-right: 4px">{{ cat.name }}</el-tag>
              </template>
              <el-tag v-else type="warning" size="small">未分类</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="80"></el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{ row }">
              <div class="stock-cell">
                <span class="stock-value" :class="{ low: row.available <= 2 && row.available > 0, out: row.available === 0 }">
                  {{ row.available }}/{{ row.stock }}
                </span>
                <el-tag v-if="row.available === 0" type="danger" size="small">售罄</el-tag>
                <el-tag v-else-if="row.available <= 2" type="warning" size="small">低库存</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <button
                  v-if="row.available > 0"
                  class="action-btn borrow-btn"
                  @click="handleBorrow(row)"
                >
                  借书
                </button>
                <button v-if="isSuperAdmin && (!row.categories || row.categories.length === 0)" class="action-btn cate-btn" @click="handleRecategorize(row)">分类</button>
                <button class="action-btn edit-btn" @click="handleEdit(row)">编辑</button>
                <button class="action-btn delete-btn" @click="handleDelete(row)">删除</button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 借书对话框 -->
    <el-dialog
      v-model="borrowDialogVisible"
      title="借书登记"
      width="480px"
      class="custom-dialog"
    >
      <div class="dialog-form">
        <div class="form-item">
          <label>图书ID</label>
          <input type="text" :value="borrowForm.bookId" disabled />
        </div>
        <div class="form-item">
          <label>书名</label>
          <input type="text" :value="borrowForm.bookTitle" disabled />
        </div>
        <div class="form-item required">
          <label>用户名</label>
          <input
            v-model="borrowForm.username"
            type="text"
            placeholder="请输入借阅用户名"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <button class="cancel-btn" @click="borrowDialogVisible = false">取消</button>
          <button class="confirm-btn" @click="handleBorrowSubmit">确认借阅</button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑/添加对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑图书' : '添加图书'"
      width="560px"
      class="custom-dialog"
    >
      <div class="dialog-form">
        <div class="form-row">
          <div class="form-item required">
            <label>ISBN</label>
            <input v-model="bookForm.isbn" type="text" placeholder="请输入ISBN" />
          </div>
          <div class="form-item required">
            <label>书名</label>
            <input v-model="bookForm.title" type="text" placeholder="请输入书名" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-item required">
            <label>作者</label>
            <input v-model="bookForm.author" type="text" placeholder="请输入作者" />
          </div>
          <div class="form-item">
            <label>出版社</label>
            <input v-model="bookForm.publisher" type="text" placeholder="请输入出版社" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-item">
            <label>出版日期</label>
            <el-date-picker
              v-model="bookForm.publishDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </div>
          <div class="form-item required">
            <label>分类</label>
            <el-select v-model="bookForm.categoryIds" placeholder="请选择分类" multiple filterable style="width: 100%">
              <el-option
                v-if="categories.length === 0"
                label="未分类"
                :value="0"
              />
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item">
            <label>价格</label>
            <el-input-number v-model="bookForm.price" :min="0" :precision="2" style="width: 100%" />
          </div>
          <div class="form-item required">
            <label>库存</label>
            <el-input-number v-model="bookForm.stock" :min="0" style="width: 100%" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-item" style="width:100%">
            <label>封面图片</label>
            <div style="display:flex;align-items:center;gap:12px">
              <img v-if="bookForm.coverPath" :src="bookForm.coverPath" class="cover-preview" />
              <el-upload :auto-upload="false" :limit="1" :on-change="handleCoverChange" accept="image/*">
                <el-button size="small" type="primary">选择图片</el-button>
              </el-upload>
              <el-button v-if="bookForm.coverPath" size="small" type="danger" @click="bookForm.coverPath=''; coverFile=null">移除</el-button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useBookStore } from '../../stores/book'
import { useBorrowStore } from '../../stores/borrow'
import { useAuthStore } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '../../api/category'
import { bookApi } from '../../api/book'

const bookStore = useBookStore()
const borrowStore = useBorrowStore()
const authStore = useAuthStore()
const isSuperAdmin = authStore.isSuperAdmin
const searchKeyword = ref('')
const dialogVisible = ref(false)
const recategorizeVisible = ref(false)
const recategorizeIds = ref([])
const recategorizeBookId = ref(null)
const borrowDialogVisible = ref(false)
const isEdit = ref(false)
const bookList = ref([])
const loading = ref(false)
const coverFile = ref(null)
const showLowStockOnly = ref(false)
const inventoryWarning = ref({ hasWarning: false, lowStockCount: 0, outOfStockCount: 0, lowStockBooks: [], outOfStockBooks: [] })

const displayBookList = computed(() => {
  if (!showLowStockOnly.value) return bookList.value
  return bookList.value.filter(b => b.available <= 2)
})

const bookForm = ref({
  id: null,
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  coverPath: '',
  categoryIds: [],
  price: 0,
  stock: 0
})

const borrowForm = ref({
  bookId: null,
  bookTitle: '',
  username: ''
})

const categories = ref([])

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (e) {
    // 下拉框为空，不影响主要功能
  }
}

const loadWarning = async () => {
  try {
    const res = await bookApi.getInventoryWarning()
    if (res.data.code === 200) {
      inventoryWarning.value = res.data.data
    }
  } catch (e) { /* non-critical */ }
}

onMounted(async () => {
  loading.value = true
  loadCategories()
  loadWarning()
  try {
    await bookStore.fetchBooks()
    bookList.value = bookStore.books
  } finally {
    loading.value = false
  }
})

async function handleSearch() {
  loading.value = true
  try {
    if (searchKeyword.value) {
      await bookStore.searchBooks(searchKeyword.value)
    } else {
      await bookStore.fetchBooks()
    }
    bookList.value = bookStore.books
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchKeyword.value = ''
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  coverFile.value = null
  bookForm.value = {
    id: null, isbn: '', title: '', author: '', publisher: '',
    publishDate: '', coverPath: '', categoryIds: [], price: 0, stock: 0
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  coverFile.value = null
  bookForm.value = {
    ...row,
    coverPath: row.coverPath || '',
    categoryIds: (row.categories || []).map(c => c.id)
  }
  dialogVisible.value = true
}

const handleCoverChange = (file) => {
  coverFile.value = file.raw
  bookForm.value.coverPath = URL.createObjectURL(file.raw)
}

async function handleSubmit() {
  if (!bookForm.value.isbn || !bookForm.value.title || !bookForm.value.author) {
    ElMessage.warning('请填写必填项')
    return
  }
  // 价格正则校验：最多两位小数的正数
  const priceRegex = /^\d+(\.\d{1,2})?$/
  if (bookForm.value.price !== undefined && bookForm.value.price !== null && !priceRegex.test(String(bookForm.value.price))) {
    ElMessage.warning('价格格式不正确，最多两位小数')
    return
  }
  if (categories.value.length > 0 && (!bookForm.value.categoryIds || bookForm.value.categoryIds.length === 0)) {
    ElMessage.warning('请至少选择一个分类')
    return
  }
  try {
    const payload = {
      ...bookForm.value,
      categories: bookForm.value.categoryIds.map(id => ({ id }))
    }
    delete payload.categoryIds
    delete payload.coverPath
    let savedBook
    if (isEdit.value) {
      savedBook = await bookStore.updateBook(payload.id, payload)
    } else {
      savedBook = await bookStore.addBook(payload)
    }
    // 上传封面
    if (coverFile.value && savedBook.data) {
      const bookId = savedBook.data.id || payload.id
      const fd = new FormData()
      fd.append('file', coverFile.value)
      await bookApi.uploadCover(bookId, fd)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    await bookStore.fetchBooks()
    bookList.value = bookStore.books
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

function handleBorrow(row) {
  borrowForm.value = {
    bookId: row.id,
    bookTitle: row.title,
    username: ''
  }
  borrowDialogVisible.value = true
}

async function handleBorrowSubmit() {
  if (!borrowForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  try {
    await borrowStore.borrowBook(borrowForm.value)
    ElMessage.success('借书登记成功')
    borrowDialogVisible.value = false
    await bookStore.fetchBooks()
    bookList.value = bookStore.books
  } catch (error) {
    ElMessage.error(error.message || '借书登记失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除这本图书吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await bookStore.deleteBook(row.id)
    ElMessage.success('删除成功')
    bookList.value = bookStore.books
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleRecategorize(row) {
  recategorizeBookId.value = row.id
  recategorizeIds.value = []
  recategorizeVisible.value = true
}

async function handleRecategorizeSubmit() {
  if (!recategorizeIds.value.length) {
    ElMessage.warning('请选择分类')
    return
  }
  try {
    await bookApi.updateCategories(recategorizeBookId.value, recategorizeIds.value)
    ElMessage.success('分类更新成功')
    recategorizeVisible.value = false
    await bookStore.fetchBooks()
    bookList.value = bookStore.books
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  }
}
</script>

<style scoped>
.book-management-page {
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

.clear-btn:hover {
  color: var(--text-secondary);
}

.clear-btn svg {
  width: 16px;
  height: 16px;
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

.add-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #059669 0%, #10b981 100%);
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
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

/* 库存预警横幅 */
.warning-banner {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(245, 158, 11, 0.08) 100%);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 14px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.warning-banner-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.warning-icon {
  width: 24px;
  height: 24px;
  color: #f59e0b;
  flex-shrink: 0;
}

.warning-text {
  font-size: 14px;
  color: var(--text-secondary);
}

.warning-count {
  font-weight: 700;
  font-size: 18px;
}

.warning-count.danger { color: #f87171; }
.warning-count.warn { color: #fbbf24; }

.filter-toggle {
  padding: 8px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.filter-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.filter-toggle.active {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
  color: #f87171;
}

/* 表格区域 */
.table-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.table-header h3 {
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
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 表格 */
.table-wrapper {
  border-radius: 12px;
  overflow: hidden;
}

.stock-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-value {
  font-weight: 500;
  color: #059669;
}

.stock-value.low {
  color: #f59e0b;
}

.stock-value.out {
  color: #dc2626;
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

.borrow-btn {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
}

.borrow-btn:hover {
  background: rgba(16, 185, 129, 0.2);
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

.cate-btn {
  background: rgba(245, 158, 11, 0.12);
  color: #fbbf24;
}

.cate-btn:hover {
  background: rgba(245, 158, 11, 0.2);
}

/* 对话框样式 */
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 0;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.form-item input:disabled {
  background: var(--bg-hover);
  color: var(--text-secondary);
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
@media (max-width: 768px) {
  .search-box {
    flex-wrap: wrap;
  }

  .search-input-wrapper {
    width: 100%;
  }

  .search-btn, .add-btn {
    flex: 1;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}

.cover-preview {
  width: 80px; height: 106px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}
</style>