<template>
  <div class="ebook-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2>在线阅读</h2>
        <p>随时随地畅读电子书，开启知识之旅</p>
      </div>
      <div class="header-decoration">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
          <line x1="8" y1="21" x2="16" y2="21"></line>
          <line x1="12" y1="17" x2="12" y2="21"></line>
        </svg>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar-section" v-if="!loading && ebookStore.ebooks.length > 0">
      <div style="display:flex;align-items:center;gap:12px;background:var(--bg-glass);border:2px solid var(--border-color);border-radius:12px;padding:12px 16px">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;color:var(--text-muted)"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <input v-model="searchKeyword" type="text" placeholder="搜索电子书（书名、作者）" @input="applySearch" style="flex:1;border:none;outline:none;font-size:15px;background:transparent;color:var(--text-primary)" />
        <button v-if="searchKeyword" @click="searchKeyword='';applySearch()" style="background:none;border:none;cursor:pointer;color:var(--text-muted);padding:0;font-size:20px">&times;</button>
      </div>
    </div>

    <!-- 电子书列表 -->
    <div class="books-section">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>正在加载...</span>
      </div>

      <div v-else-if="ebookStore.ebooks.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">
          <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
          <line x1="8" y1="21" x2="16" y2="21"></line>
          <line x1="12" y1="17" x2="12" y2="21"></line>
        </svg>
        <p>暂无电子书</p>
        <span>敬请期待更多精彩内容</span>
      </div>

      <div v-else>
        <div v-if="filteredList.length === 0" class="empty-state">
          <p>{{ searchKeyword ? '未找到匹配的电子书' : '暂无电子书' }}</p>
        </div>

        <div v-else class="books-grid">
          <div
            v-for="ebook in filteredList"
            :key="ebook.id"
            class="ebook-card"
            @click="openReader(ebook.id)"
          >
          <div class="ebook-cover">
            <div class="cover-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <span class="file-type">{{ ebook.fileType?.toUpperCase() || 'PDF' }}</span>
          </div>
          <div class="ebook-content">
            <h4 class="ebook-title">{{ ebook.title }}</h4>
            <p class="ebook-author" v-if="ebook.author">{{ ebook.author }}</p>
            <p class="ebook-desc" v-if="ebook.description">{{ truncate(ebook.description, 60) }}</p>
            <div class="ebook-footer">
              <span class="read-badge">点击阅读</span>
              <!-- 下载功能已禁用: 缓存下载按钮 -->
              <!-- <button class="cache-btn" @click.stop="handleCacheDownload(ebook)">缓存下载</button> -->
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow-icon">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>

    <!-- 下载功能已禁用: 缓存下载进度弹窗 -->
    <!-- <el-dialog :model-value="cacheProgress > 0 && cacheProgress < 100" title="缓存下载中" width="400px" :close-on-click-modal="false" :show-close="false">
      <div style="text-align:center">
        <p style="margin-bottom:16px">正在缓存「{{ cacheEbookTitle }}」所有章节...</p>
        <el-progress :percentage="cacheProgress" :stroke-width="20" :text-inside="true" />
      </div>
    </el-dialog> -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useEBookStore } from '../../stores/ebook'
// import { cacheEBook, getCacheProgress } from '../../api/ebook'  // 下载功能已禁用
import { ElMessage } from 'element-plus'

const router = useRouter()
const ebookStore = useEBookStore()
const loading = ref(false)
const searchKeyword = ref('')
const filteredList = ref([])

const applySearch = () => {
  const kw = searchKeyword.value.toLowerCase()
  const list = ebookStore.ebooks || []
  if (!kw) {
    filteredList.value = list
    return
  }
  filteredList.value = list.filter(item =>
    (item.title || '').toLowerCase().includes(kw) ||
    (item.author || '').toLowerCase().includes(kw)
  )
}

const loadEBookList = async () => {
  loading.value = true
  await ebookStore.loadEBookList()
  applySearch()
  loading.value = false
}

const openReader = (ebookId) => {
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
//     const res = await cacheEBook(ebook.id)
//     cacheDownloadUrl.value = res.data.data
//     const poll = setInterval(async () => {
//       try {
//         const pRes = await getCacheProgress(ebook.id)
//         cacheProgress.value = pRes.data.data || 0
//         if (cacheProgress.value >= 100) {
//           clearInterval(poll)
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

const truncate = (text, length) => {
  if (!text) return ''
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

onMounted(() => {
  loadEBookList()
})
</script>

<style scoped>
.ebook-list-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border-radius: 20px;
  padding: 32px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.header-content h2 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.header-content p {
  font-size: 16px;
  opacity: 0.9;
}

.header-decoration {
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-decoration svg {
  width: 32px;
  height: 32px;
}

/* 图书列表区域 */
.books-section {
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
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: var(--text-muted);
}

.empty-state p {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.empty-state span {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 电子书网格 */
.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.ebook-card {
  display: flex;
  gap: 20px;
  padding: 24px;
  background: var(--bg-glass);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.ebook-card:hover {
  background: var(--bg-card);
  border-color: var(--accent-1);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
  transform: translateY(-4px);
}

.ebook-cover {
  width: 100px;
  height: 120px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(139, 92, 246, 0.15) 100%);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.cover-icon {
  width: 48px;
  height: 48px;
  color: var(--accent-2);
}

.cover-icon svg {
  width: 48px;
  height: 48px;
}

.file-type {
  padding: 4px 10px;
  background: var(--bg-card);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-2);
}

.ebook-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ebook-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ebook-author {
  font-size: 14px;
  color: var(--text-secondary);
}

.ebook-desc {
  font-size: 13px;
  color: var(--text-muted);
  flex: 1;
  line-height: 1.5;
}

.ebook-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.read-badge {
  padding: 6px 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border-radius: 6px;
  font-size: 13px;
  color: white;
  font-weight: 500;
}
.cache-btn {
  padding: 6px 12px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: white;
  cursor: pointer;
  margin-left: 8px;
}

.arrow-icon {
  width: 20px;
  height: 20px;
  color: var(--text-muted);
}

.ebook-card:hover .arrow-icon {
  color: var(--accent-2);
}

/* 响应式 */
@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .books-grid {
    grid-template-columns: 1fr;
  }

  .ebook-card {
    flex-direction: column;
    text-align: center;
  }

  .ebook-cover {
    width: 100%;
    height: 100px;
  }
}
</style>