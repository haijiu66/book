<template>
  <div class="browse-page">
    <div class="top-bar">
      <span class="bar-label">分类</span>
      <el-select v-model="selectedCategoryId" placeholder="全部分类" clearable @change="refresh" style="width: 200px">
        <el-option label="全部分类" :value="null" />
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
    </div>

    <div class="top-bar">
      <span class="bar-label">排行</span>
      <el-radio-group v-model="rankPeriod" @change="refresh" size="default">
        <el-radio-button value="weekly">周排行</el-radio-button>
        <el-radio-button value="monthly">月排行</el-radio-button>
        <el-radio-button value="yearly">年排行</el-radio-button>
        <el-radio-button value="favorite">收藏排行</el-radio-button>
      </el-radio-group>
    </div>

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

    <div class="top-bar">
      <span class="bar-label">搜索</span>
      <div style="display:flex;align-items:center;gap:12px;background:var(--bg-glass);border:2px solid var(--border-color);border-radius:12px;padding:10px 16px;flex:1">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;color:var(--text-muted)"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <input v-model="searchKeyword" type="text" placeholder="搜索书名、作者" @input="applySearch" style="flex:1;border:none;outline:none;font-size:15px;background:transparent;color:var(--text-primary)" />
        <button v-if="searchKeyword" @click="searchKeyword='';applySearch()" style="background:none;border:none;cursor:pointer;color:var(--text-muted);padding:0;font-size:20px">&times;</button>
      </div>
    </div>

    <div class="content-section">
      <div v-if="loading" class="loading-state"><div class="loading-spinner"></div><span>正在加载...</span></div>
      <div v-else-if="filteredList.length === 0" class="empty-state"><p>{{ searchKeyword ? '未找到匹配数据' : '暂无数据' }}</p></div>

      <el-table v-else :data="filteredList" stripe style="width: 100%">
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <span v-if="$index === 0" style="font-size:22px">🥇</span>
            <span v-else-if="$index === 1" style="font-size:22px">🥈</span>
            <span v-else-if="$index === 2" style="font-size:22px">🥉</span>
            <span v-else class="rank-num">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            <template v-if="(row._categories || []).length > 0">
              <el-tag v-for="cat in row._categories" :key="cat.id" size="small" style="margin-right:4px">{{ cat.name }}</el-tag>
            </template>
            <el-tag v-else type="warning" size="small">未分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="rankPeriod === 'favorite' ? '收藏数' : (activeTab === 'online' ? '阅读人数' : '借阅次数')" width="110">
          <template #default="{ row }">
            <el-tag :type="activeTab === 'online' ? 'success' : 'primary'" size="small">{{ row.count }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <button v-if="activeTab === 'online'" class="action-btn read-btn" @click="goRead(row.itemId)">阅读</button>
            <button v-else-if="row._available > 0" class="action-btn borrow-btn" @click="handleBorrow(row)">借阅</button>
            <span v-else class="out-stock">无库存</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { bookApi } from '../../api/book'
import { ebookApi } from '../../api/ebook'
import { categoryApi } from '../../api/category'
import { rankingApi } from '../../api/ranking'
import { favoriteApi } from '../../api/favorite'
import { useBorrowStore } from '../../stores/borrow'
import { ElMessage } from 'element-plus'

const router = useRouter()
const borrowStore = useBorrowStore()

const categories = ref([])
const selectedCategoryId = ref(null)
const rankPeriod = ref('weekly')
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

let allBooksCache = []
let allEbooksCache = []

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch (e) {}
}

const loadAllData = async () => {
  try {
    const [bookRes, ebookRes] = await Promise.all([bookApi.getAll(), ebookApi.getEBookList()])
    allBooksCache = bookRes.data.code === 200 ? (bookRes.data.data || []) : []
    allEbooksCache = ebookRes.data.code === 200 ? (ebookRes.data.data || []) : []
  } catch (e) {}
}

const switchTab = (tab) => { activeTab.value = tab; searchKeyword.value = ''; refresh() }

const refresh = () => {
  if (rankPeriod.value === 'favorite') fetchFavoriteRanking()
  else if (activeTab.value === 'online') fetchOnlineRanking()
  else fetchBorrowRanking()
}

const fetchOnlineRanking = async () => {
  loading.value = true
  try {
    const res = await rankingApi.getReadingRanking(rankPeriod.value, 50)
    if (res.data.code === 200) {
      let list = (res.data.data || []).map(item => {
        const ebook = allEbooksCache.find(e => e.id === item.itemId)
        return { ...item, _categories: ebook?.categories || [] }
      })
      if (selectedCategoryId.value) list = list.filter(item => item._categories.some(c => c.id === selectedCategoryId.value))
      displayList.value = list
    }
  } catch (e) { displayList.value = [] }
  finally { loading.value = false; applySearch() }
}

const fetchBorrowRanking = async () => {
  loading.value = true
  try {
    const res = await rankingApi.getBorrowRanking(rankPeriod.value, 50)
    if (res.data.code === 200) {
      let list = (res.data.data || []).map(item => {
        const book = allBooksCache.find(b => b.id === item.itemId)
        return { ...item, _categories: book?.categories || [], _available: book?.available ?? 0 }
      })
      if (selectedCategoryId.value) list = list.filter(item => item._categories.some(c => c.id === selectedCategoryId.value))
      displayList.value = list
    }
  } catch (e) { displayList.value = [] }
  finally { loading.value = false; applySearch() }
}

const fetchFavoriteRanking = async () => {
  loading.value = true
  try {
    const targetType = activeTab.value === 'online' ? 'EBOOK' : 'BOOK'
    const res = await favoriteApi.getRanking(targetType, 50)
    if (res.data.code === 200) {
      let list = (res.data.data || []).map(item => {
        if (targetType === 'EBOOK') {
          const ebook = allEbooksCache.find(e => e.id === item.itemId)
          return { ...item, _categories: ebook?.categories || [] }
        } else {
          const book = allBooksCache.find(b => b.id === item.itemId)
          return { ...item, _categories: book?.categories || [], _available: book?.available ?? 0 }
        }
      })
      if (selectedCategoryId.value) list = list.filter(item => item._categories.some(c => c.id === selectedCategoryId.value))
      displayList.value = list
    }
  } catch (e) { displayList.value = [] }
  finally { loading.value = false; applySearch() }
}

const goRead = (ebookId) => { if (ebookId) router.push(`/reader/ebooks/${ebookId}`) }

const handleBorrow = async (row) => {
  try {
    await borrowStore.borrowBook({ bookId: row.itemId })
    ElMessage.success('借阅成功')
    refresh()
  } catch (e) { ElMessage.error(e.message || '借阅失败') }
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadAllData()])
  refresh()
})
</script>

<style scoped>
.browse-page { display: flex; flex-direction: column; gap: 14px; }
.top-bar { background: var(--bg-card); border-radius: 14px; padding: 14px 22px; border: 1px solid var(--border-color); display: flex; align-items: center; gap: 16px; }
.bar-label { font-size: 15px; font-weight: 600; color: var(--text-primary); white-space: nowrap; min-width: 48px; }
.tab-bar { display: flex; background: var(--bg-card); border-radius: 14px; border: 1px solid var(--border-color); overflow: hidden; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 16px 24px; cursor: pointer; font-size: 15px; font-weight: 600; color: var(--text-secondary); transition: all 0.2s ease; border-bottom: 3px solid transparent; }
.tab-item:hover { color: var(--accent-2); background: var(--bg-hover); }
.tab-item.active { color: var(--accent-2); border-bottom-color: var(--accent-1); background: var(--bg-hover); }
.tab-item svg { width: 20px; height: 20px; }
.content-section { background: var(--bg-card); border-radius: 14px; padding: 20px 24px; border: 1px solid var(--border-color); min-height: 300px; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px 20px; gap: 16px; color: var(--text-secondary); }
.loading-spinner { width: 40px; height: 40px; border: 3px solid var(--border-color); border-top-color: var(--accent-1); border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.rank-num { font-weight: 600; color: var(--text-secondary); }
.action-btn { display: inline-flex; align-items: center; gap: 4px; padding: 6px 14px; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.15s ease; color: white; }
.read-btn { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); }
.read-btn:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(99,102,241,0.3); }
.borrow-btn { background: linear-gradient(135deg, #059669 0%, #10b981 100%); }
.borrow-btn:hover { transform: translateY(-1px); box-shadow: 0 3px 8px rgba(5,150,105,0.3); }
.out-stock { font-size: 13px; color: #dc2626; font-weight: 500; }
</style>
