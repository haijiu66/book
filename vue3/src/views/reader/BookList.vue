<template>
  <div class="latest-page">
    <!-- Tab 切换 -->
    <div class="tab-bar">
      <div class="tab-item" :class="{ active: activeTab === 'online' }" @click="switchTab('online')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
          <line x1="8" y1="21" x2="16" y2="21"></line>
          <line x1="12" y1="17" x2="12" y2="21"></line>
        </svg>
        <span>在线阅读</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'borrow' }" @click="switchTab('borrow')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
        </svg>
        <span>借阅图书</span>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-input-wrapper">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
          <circle cx="11" cy="11" r="8"></circle>
          <path d="m21 21-4.35-4.35"></path>
        </svg>
        <input v-model="searchKeyword" type="text" :placeholder="activeTab === 'online' ? '搜索电子书（书名、作者）' : '搜索借阅图书（书名、作者）'" @input="applySearch" />
        <button v-if="searchKeyword" class="clear-btn" @click="searchKeyword='';applySearch()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    </div>

    <div class="content-section">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>正在加载...</span>
      </div>

      <div v-else-if="displayList.length === 0" class="empty-state">
        <p>暂无图书</p>
      </div>

      <div v-else-if="filteredList.length === 0" class="empty-state">
        <p>{{ searchKeyword ? '未找到匹配的图书' : '暂无图书' }}</p>
      </div>

      <!-- 在线阅读卡片 -->
      <div v-else-if="activeTab === 'online'" class="books-grid">
        <div v-for="ebook in filteredList" :key="'e' + ebook.id" class="book-card">
          <div class="book-cover">
            <img v-if="ebook.coverPath" :src="ebook.coverPath" class="cover-img" />
            <div v-else class="cover-placeholder ebook-cover-bg">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                <line x1="8" y1="21" x2="16" y2="21"></line>
                <line x1="12" y1="17" x2="12" y2="21"></line>
              </svg>
            </div>
          </div>
          <div class="book-content">
            <h4 class="book-title">{{ ebook.title }}</h4>
            <p class="book-author">{{ ebook.author || '佚名' }}</p>
            <div class="book-meta">
              <template v-if="(ebook.categories || []).length > 0">
                <el-tag v-for="cat in ebook.categories" :key="cat.id" size="small" style="margin-right:4px">{{ cat.name }}</el-tag>
              </template>
              <el-tag v-else type="warning" size="small">未分类</el-tag>
              <span class="book-chapters">{{ ebook.chapterCount || 0 }}章</span>
              <span class="book-format">{{ ebook.fileType?.toUpperCase() }}</span>
            </div>
            <p class="book-time">上传于 {{ formatTime(ebook.createTime) }}</p>
            <div class="book-actions">
              <button class="action-btn read-btn" @click="goRead(ebook.id)">开始阅读</button>
              <!-- 下载功能已禁用: 缓存下载按钮 -->
              <!-- <button class="action-btn cache-btn" @click="handleCacheDownload(ebook)">缓存下载</button> -->
              <button class="fav-btn" :class="{ active: favMap['EBOOK_' + ebook.id] }" @click="toggleFav('EBOOK', ebook.id)">
                <svg viewBox="0 0 24 24" :fill="favMap['EBOOK_' + ebook.id] ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 借阅图书卡片 -->
      <div v-else class="books-grid">
        <div v-for="book in filteredList" :key="'b' + book.id" class="book-card">
          <div class="book-cover">
            <img v-if="book.coverPath" :src="book.coverPath" class="cover-img" />
            <div v-else class="cover-placeholder borrow-cover-bg">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
            </div>
          </div>
          <div class="book-content">
            <h4 class="book-title">{{ book.title }}</h4>
            <p class="book-author">{{ book.author }}</p>
            <div class="book-meta">
              <template v-if="(book.categories || []).length > 0">
                <el-tag v-for="cat in book.categories" :key="cat.id" size="small" style="margin-right:4px">{{ cat.name }}</el-tag>
              </template>
              <el-tag v-else type="warning" size="small">未分类</el-tag>
              <span class="book-publisher">{{ book.publisher }}</span>
            </div>
            <div class="book-stock">
              <span class="stock-value" :class="{ low: book.available <= 2, out: book.available === 0 }">
                库存 {{ book.available }}/{{ book.stock }}
              </span>
            </div>
            <p class="book-time">上架于 {{ formatTime(book.createTime) }}</p>
            <div class="book-actions">
              <button v-if="book.available > 0" class="action-btn borrow-btn" @click="handleBorrow(book)">借阅</button>
              <span v-else class="out-stock">暂无库存</span>
              <button class="fav-btn" :class="{ active: favMap['BOOK_' + book.id] }" @click="toggleFav('BOOK', book.id)">
                <svg viewBox="0 0 24 24" :fill="favMap['BOOK_' + book.id] ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下载功能已禁用: 缓存下载进度条 -->
    <!-- <el-dialog :model-value="cacheProgress > 0 && cacheProgress < 100" title="缓存下载中" width="400px" :close-on-click-modal="false" :show-close="false">
      <div style="text-align:center">
        <p style="margin-bottom:16px">正在缓存「{{ cacheEbookTitle }}」所有章节...</p>
        <el-progress :percentage="cacheProgress" :stroke-width="20" :text-inside="true" />
        <p style="margin-top:8px;color:#94a3b8;font-size:13px">首次缓存后阅读无需再查数据库</p>
      </div>
    </el-dialog> -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { bookApi } from '../../api/book'
import { ebookApi } from '../../api/ebook'
// import { cacheEBook, getCacheProgress } from '../../api/ebook'  // 下载功能已禁用
import { useBorrowStore } from '../../stores/borrow'
import { favoriteApi } from '../../api/favorite'
import { ElMessage } from 'element-plus'

const router = useRouter()
const borrowStore = useBorrowStore()

const activeTab = ref('online')
const displayList = ref([])
const loading = ref(false)
const favMap = ref({})
const searchKeyword = ref('')
const filteredList = ref([])

const applySearch = () => {
  if (!searchKeyword.value) {
    filteredList.value = displayList.value
    return
  }
  const kw = searchKeyword.value.toLowerCase()
  filteredList.value = displayList.value.filter(item =>
    (item.title || '').toLowerCase().includes(kw) ||
    (item.author || '').toLowerCase().includes(kw)
  )
}

const toggleFav = async (targetType, targetId) => {
  try {
    const res = await favoriteApi.toggle(targetType, targetId)
    if (res.data.code === 200) {
      favMap.value[targetType + '_' + targetId] = res.data.data.favorited
    }
  } catch (e) {}
}

const switchTab = (tab) => {
  activeTab.value = tab
  searchKeyword.value = ''
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'online') {
      const res = await ebookApi.getEBookList()
      let list = res.data.code === 200 ? (res.data.data || []) : []
      list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
      displayList.value = list
    } else {
      const res = await bookApi.getAll()
      let list = res.data.code === 200 ? (res.data.data || []) : []
      list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
      displayList.value = list
    }
  } catch (e) {
    displayList.value = []
  } finally {
    loading.value = false
    applySearch()
  }
}

const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

const goRead = (ebookId) => {
  router.push(`/reader/ebooks/${ebookId}`)
}

// ========== 下载功能已禁用 ==========
// const cacheProgress = ref(0)
// const cacheEbookTitle = ref('')
// const cacheDownloadUrl = ref('')
//
// const handleCacheDownload = async (ebook) => {
//   cacheEbookTitle.value = ebook.title
//   cacheProgress.value = 0
//   try {
//     // 启动缓存
//     const res = await cacheEBook(ebook.id)
//     cacheDownloadUrl.value = res.data.data
//     // 轮询进度
//     const poll = setInterval(async () => {
//       try {
//         const pRes = await getCacheProgress(ebook.id)
//         cacheProgress.value = pRes.data.data || 0
//         if (cacheProgress.value >= 100) {
//           clearInterval(poll)
//           // 自动触发下载
//           const a = document.createElement('a')
//           a.href = '/api' + cacheDownloadUrl.value
//           a.download = cacheEbookTitle.value + '.txt'
//           document.body.appendChild(a)
//           a.click()
//           document.body.removeChild(a)
//           cacheProgress.value = 0
//           cacheDownloadUrl.value = ''
//           ElMessage.success('缓存完成，TXT 已下载')
//         }
//       } catch (e) { clearInterval(poll) }
//     }, 500)
//   } catch (e) {
//     cacheProgress.value = 0
//     ElMessage.error('缓存失败')
//   }
// }

const handleBorrow = async (book) => {
  try {
    const result = await borrowStore.borrowBook({ bookId: book.id })
    ElMessage.success('借阅成功')
    if (result?.stockWarning) {
      ElMessage.warning(result.stockWarning)
    }
    fetchData()
  } catch (e) {
    ElMessage.error(e.message || '借阅失败')
  }
}

const loadFavStatus = async () => {
  try {
    const [ebookRes, bookRes] = await Promise.all([
      favoriteApi.getMyIds('EBOOK'),
      favoriteApi.getMyIds('BOOK')
    ])
    if (ebookRes.data.code === 200) {
      (ebookRes.data.data || []).forEach(id => favMap.value['EBOOK_' + id] = true)
    }
    if (bookRes.data.code === 200) {
      (bookRes.data.data || []).forEach(id => favMap.value['BOOK_' + id] = true)
    }
  } catch (e) {}
}

onMounted(async () => {
  await loadFavStatus()
  fetchData()
})
</script>

<style scoped>
.latest-page { display: flex; flex-direction: column; gap: 14px; }

.search-bar {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 16px 24px;
  border: 1px solid var(--border-color);
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-glass);
  border: 2px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 16px;
  transition: border-color 0.2s;
}

.search-input-wrapper:focus-within {
  border-color: var(--accent-1);
  background: var(--bg-hover);
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

.search-icon {
  width: 20px;
  height: 20px;
  min-width: 20px;
  max-width: 20px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.clear-btn {
  width: 20px;
  height: 20px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn:hover { color: var(--text-secondary); }

.clear-btn svg {
  width: 16px;
  height: 16px;
}

.tab-bar {
  display: flex;
  background: var(--bg-card);
  border-radius: 14px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 24px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  border-bottom: 3px solid transparent;
}

.tab-item:hover { color: var(--accent-2); background: var(--bg-hover); }
.tab-item.active { color: var(--accent-2); border-bottom-color: var(--accent-1); background: var(--bg-hover); }
.tab-item svg { width: 20px; height: 20px; }

.content-section {
  background: var(--bg-card);
  border-radius: 14px;
  padding: 24px;
  border: 1px solid var(--border-color);
  min-height: 300px;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  gap: 16px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 40px; height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.book-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: var(--bg-glass);
  border-radius: 12px;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
}
.book-card:hover { background: var(--bg-hover); border-color: var(--border-glow); box-shadow: 0 8px 16px rgba(0,0,0,0.3); }

.book-cover { width: 80px; height: 106px; border-radius: 8px; flex-shrink: 0; overflow: hidden; }
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
}
.cover-placeholder svg { width: 32px; height: 32px; }
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.ebook-cover-bg { background: linear-gradient(135deg, rgba(99,102,241,0.2) 0%, rgba(139,92,246,0.15) 100%); color: var(--accent-2); }
.borrow-cover-bg { background: linear-gradient(135deg, rgba(16,185,129,0.2) 0%, rgba(5,150,105,0.15) 100%); color: var(--accent-6); }

.book-content { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.book-title { font-size: 16px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.book-author { font-size: 13px; color: var(--text-secondary); }
.book-meta { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.book-chapters, .book-format { font-size: 12px; color: var(--text-secondary); }
.book-publisher { font-size: 12px; color: var(--text-secondary); }
.book-time { font-size: 12px; color: var(--text-muted); }
.book-stock { display: flex; align-items: center; }
.stock-value { font-size: 13px; font-weight: 500; color: #059669; }
.stock-value.low { color: #f59e0b; }
.stock-value.out { color: #dc2626; }

.book-actions { margin-top: 2px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.action-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border: none; border-radius: 6px;
  font-size: 13px; font-weight: 500; cursor: pointer; color: white;
  transition: all 0.15s ease;
}
.read-btn { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); }
.read-btn:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(99,102,241,0.3); }
.cache-btn { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); }
.cache-btn:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(245,158,11,0.3); }
.borrow-btn { background: linear-gradient(135deg, #059669 0%, #10b981 100%); }
.borrow-btn:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(5,150,105,0.3); }
.out-stock { font-size: 13px; color: #dc2626; font-weight: 500; }

.fav-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 34px; height: 34px;
  border: 1px solid var(--border-color); border-radius: 8px;
  background: var(--bg-glass); color: var(--text-secondary);
  cursor: pointer; transition: all 0.15s ease;
}
.fav-btn svg { width: 16px; height: 16px; }
.fav-btn:hover { border-color: rgba(239, 68, 68, 0.5); color: var(--accent-5); }
.fav-btn.active { color: var(--accent-5); border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.1); }

@media (max-width: 768px) { .books-grid { grid-template-columns: 1fr; } }
</style>
