<template>
  <div class="fav-page">
    <div class="tab-bar">
      <div class="tab-item" :class="{ active: activeTab === 'online' }" @click="switchTab('online')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect><line x1="8" y1="21" x2="16" y2="21"></line><line x1="12" y1="17" x2="12" y2="21"></line></svg>
        <span>在线阅读</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'borrow' }" @click="switchTab('borrow')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
        <span>借阅图书</span>
      </div>
    </div>

    <div class="search-bar" style="background:var(--bg-card);border-radius:14px;padding:14px 22px;border:1px solid var(--border-color);margin-bottom:14px">
      <div style="display:flex;align-items:center;gap:12px;background:var(--bg-glass);border:2px solid var(--border-color);border-radius:12px;padding:12px 16px">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;color:var(--text-muted)"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <input v-model="searchKeyword" type="text" placeholder="搜索收藏" @input="applySearch" style="flex:1;border:none;outline:none;font-size:15px;background:transparent;color:var(--text-primary)" />
        <button v-if="searchKeyword" @click="searchKeyword='';applySearch()" style="background:none;border:none;cursor:pointer;color:var(--text-muted);padding:0;font-size:20px">&times;</button>
      </div>
    </div>

    <div class="content-section">
      <div v-if="loading" class="loading-state"><div class="loading-spinner"></div><span>正在加载...</span></div>
      <div v-else-if="filteredList.length === 0" class="empty-state"><p>{{ searchKeyword ? '未找到匹配的收藏' : '暂无收藏' }}</p></div>

      <!-- 在线书卡片 -->
      <div v-else-if="activeTab === 'online'" class="books-grid">
        <div v-for="ebook in filteredList" :key="'e' + ebook.id" class="book-card">
          <div class="book-cover">
            <img v-if="ebook.coverPath" :src="ebook.coverPath" class="cover-img" />
            <div v-else class="cover-placeholder ebook-cover-bg">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect><line x1="8" y1="21" x2="16" y2="21"></line><line x1="12" y1="17" x2="12" y2="21"></line></svg>
            </div>
          </div>
          <div class="book-content">
            <h4 class="book-title">{{ ebook.title }}</h4>
            <p class="book-author">{{ ebook.author || '佚名' }}</p>
            <div class="book-meta">
              <el-tag v-for="cat in (ebook.categories || [])" :key="cat.id" size="small" style="margin-right:4px">{{ cat.name }}</el-tag>
            </div>
            <div class="book-actions">
              <button class="action-btn read-btn" @click="goRead(ebook.id)">开始阅读</button>
              <button class="fav-btn active" @click="toggleFav('EBOOK', ebook.id)">
                <svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 借阅书卡片 -->
      <div v-else class="books-grid">
        <div v-for="book in filteredList" :key="'b' + book.id" class="book-card">
          <div class="book-cover">
            <img v-if="book.coverPath" :src="book.coverPath" class="cover-img" />
            <div v-else class="cover-placeholder borrow-cover-bg">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
            </div>
          </div>
          <div class="book-content">
            <h4 class="book-title">{{ book.title }}</h4>
            <p class="book-author">{{ book.author }}</p>
            <div class="book-meta">
              <el-tag v-for="cat in (book.categories || [])" :key="cat.id" size="small" style="margin-right:4px">{{ cat.name }}</el-tag>
            </div>
            <div class="book-stock">
              <span class="stock-value" :class="{ out: book.available === 0 }">库存 {{ book.available }}/{{ book.stock }}</span>
            </div>
            <div class="book-actions">
              <button v-if="book.available > 0" class="action-btn borrow-btn" @click="handleBorrow(book)">借阅</button>
              <span v-else class="out-stock">暂无库存</span>
              <button class="fav-btn active" @click="toggleFav('BOOK', book.id)">
                <svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi } from '../../api/favorite'
import { useBorrowStore } from '../../stores/borrow'
import { ElMessage } from 'element-plus'

const router = useRouter()
const borrowStore = useBorrowStore()

const activeTab = ref('online')
const displayList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const filteredList = ref([])

const applySearch = () => {
  const kw = searchKeyword.value.toLowerCase()
  const list = displayList.value || []
  if (!kw) {
    filteredList.value = list
    return
  }
  filteredList.value = list.filter(item =>
    (item.title || '').toLowerCase().includes(kw) ||
    (item.author || '').toLowerCase().includes(kw)
  )
}

const fetchData = async () => {
  loading.value = true
  try {
    const targetType = activeTab.value === 'online' ? 'EBOOK' : 'BOOK'
    const res = await favoriteApi.getMyList(targetType)
    if (res.data.code === 200) {
      let list = res.data.data || []
      list.sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
      displayList.value = list
    }
  } catch (e) { displayList.value = [] }
  finally { loading.value = false; applySearch() }
}

const switchTab = (tab) => { activeTab.value = tab; searchKeyword.value = ''; fetchData() }

const toggleFav = async (targetType, targetId) => {
  await favoriteApi.toggle(targetType, targetId)
  fetchData()
}

const goRead = (ebookId) => { router.push(`/reader/ebooks/${ebookId}`) }

const handleBorrow = async (book) => {
  try {
    await borrowStore.borrowBook({ bookId: book.id })
    ElMessage.success('借阅成功')
    fetchData()
  } catch (e) { ElMessage.error(e.message || '借阅失败') }
}

onMounted(() => fetchData())
</script>

<style scoped>
.fav-page { display: flex; flex-direction: column; gap: 14px; }

.tab-bar { display: flex; background: var(--bg-card); border-radius: 14px; border: 1px solid var(--border-color); overflow: hidden; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 16px 24px; cursor: pointer; font-size: 15px; font-weight: 600; color: var(--text-secondary); transition: all 0.2s ease; border-bottom: 3px solid transparent; }
.tab-item:hover { color: var(--accent-2); background: var(--bg-hover); }
.tab-item.active { color: var(--accent-2); border-bottom-color: var(--accent-1); background: var(--bg-hover); }
.tab-item svg { width: 20px; height: 20px; }

.content-section { background: var(--bg-card); border-radius: 14px; padding: 24px; border: 1px solid var(--border-color); min-height: 300px; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px 20px; gap: 16px; color: var(--text-secondary); }
.loading-spinner { width: 40px; height: 40px; border: 3px solid var(--border-color); border-top-color: var(--accent-1); border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.books-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.book-card { display: flex; gap: 16px; padding: 20px; background: var(--bg-glass); border-radius: 12px; border: 1px solid transparent; transition: all 0.3s ease; }
.book-card:hover { background: var(--bg-hover); border-color: var(--border-glow); box-shadow: 0 8px 16px rgba(0,0,0,0.3); }
.book-cover { width: 80px; height: 106px; border-radius: 8px; flex-shrink: 0; overflow: hidden; }
.cover-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.cover-placeholder svg { width: 32px; height: 32px; }
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.ebook-cover-bg { background: linear-gradient(135deg, rgba(99,102,241,0.2) 0%, rgba(139,92,246,0.15) 100%); color: var(--accent-2); }
.borrow-cover-bg { background: linear-gradient(135deg, rgba(16,185,129,0.2) 0%, rgba(5,150,105,0.15) 100%); color: var(--accent-6); }
.book-content { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.book-title { font-size: 16px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.book-author { font-size: 13px; color: var(--text-secondary); }
.book-meta { display: flex; gap: 6px; flex-wrap: wrap; }
.book-stock { display: flex; align-items: center; }
.stock-value { font-size: 13px; font-weight: 500; color: #059669; }
.stock-value.out { color: #dc2626; }
.book-actions { margin-top: 2px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.action-btn { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; color: white; }
.read-btn { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); }
.borrow-btn { background: linear-gradient(135deg, #059669 0%, #10b981 100%); }
.out-stock { font-size: 13px; color: #dc2626; font-weight: 500; }
.fav-btn { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--bg-card); color: #ef4444; cursor: pointer; }
.fav-btn svg { width: 16px; height: 16px; }
.fav-btn:hover { border-color: rgba(239, 68, 68, 0.5); }
.fav-btn.active { border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.1); }
@media (max-width: 768px) { .books-grid { grid-template-columns: 1fr; } }
</style>
