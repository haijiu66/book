<template>
  <div class="book-list">
    <el-card class="search-card">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索图书（书名、作者、ISBN、分类）"
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
          <el-button @click="handleImport" v-if="authStore.isAdmin">
            <el-icon><Upload /></el-icon>
            导入TXT
          </el-button>
          <!-- 下载功能已禁用: 导出TXT按钮 -->
          <!-- <el-button type="success" @click="handleExport" v-if="authStore.isAdmin">
            <el-icon><Download /></el-icon>
            导出TXT
          </el-button> -->
          <el-button type="primary" @click="handleAdd" v-if="authStore.isAdmin">
            <el-icon><Plus /></el-icon>
            添加图书
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" style="margin-top: 20px">
      <el-table :data="bookStore.books" v-loading="bookStore.loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="isbn" label="ISBN" width="150"></el-table-column>
        <el-table-column prop="title" label="书名" min-width="200"></el-table-column>
        <el-table-column prop="author" label="作者" width="120"></el-table-column>
        <el-table-column prop="publisher" label="出版社" width="150"></el-table-column>
        <el-table-column prop="publishDate" label="出版日期" width="120"></el-table-column>
        <el-table-column prop="category" label="分类" width="100"></el-table-column>
        <el-table-column prop="price" label="价格" width="100"></el-table-column>
        <el-table-column prop="stock" label="库存" width="80"></el-table-column>
        <el-table-column prop="available" label="可借" width="80"></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" v-if="authStore.isAdmin">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑图书' : '添加图书'"
      width="600px"
    >
      <el-form :model="bookForm" :rules="rules" ref="bookFormRef" label-width="100px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="bookForm.isbn" placeholder="请输入ISBN"></el-input>
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="bookForm.title" placeholder="请输入书名"></el-input>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="bookForm.author" placeholder="请输入作者"></el-input>
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="bookForm.publisher" placeholder="请输入出版社"></el-input>
        </el-form-item>
        <el-form-item label="出版日期" prop="publishDate">
          <el-date-picker
            v-model="bookForm.publishDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="bookForm.category" placeholder="请输入分类"></el-input>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="bookForm.price" :min="0" :precision="2" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="bookForm.stock" :min="0" style="width: 100%"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <input
      ref="fileInput"
      type="file"
      accept=".txt"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useBookStore } from '../stores/book'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Plus } from '@element-plus/icons-vue'
// import { Download } from '@element-plus/icons-vue'  // 下载功能已禁用

const bookStore = useBookStore()
const authStore = useAuthStore()
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const bookFormRef = ref(null)
const fileInput = ref(null)

const bookForm = ref({
  id: null,
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  category: '',
  price: 0,
  stock: 0
})

const rules = {
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

onMounted(() => {
  bookStore.fetchBooks()
})

async function handleSearch() {
  if (searchKeyword.value) {
    await bookStore.searchBooks(searchKeyword.value)
  } else {
    await bookStore.fetchBooks()
  }
}

function handleAdd() {
  isEdit.value = false
  bookForm.value = {
    id: null,
    isbn: '',
    title: '',
    author: '',
    publisher: '',
    publishDate: '',
    category: '',
    price: 0,
    stock: 0
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  bookForm.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!bookFormRef.value) return
  await bookFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await bookStore.updateBook(bookForm.value.id, bookForm.value)
          ElMessage.success('更新成功')
        } else {
          await bookStore.addBook(bookForm.value)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
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
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleImport() {
  fileInput.value.click()
}

async function handleFileChange(event) {
  const file = event.target.files[0]
  if (file) {
    try {
      const res = await bookStore.importBooks(file)
      ElMessage.success(res.message)
    } catch (error) {
      ElMessage.error('导入失败')
    }
    event.target.value = ''
  }
}

// ========== 下载功能已禁用 ==========
// async function handleExport() {
//   try {
//     await bookStore.exportBooks()
//     ElMessage.success('导出成功')
//   } catch (error) {
//     ElMessage.error('导出失败')
//   }
// }
</script>

<style scoped>
.search-card {
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}
</style>
