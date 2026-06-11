<template>
  <div class="ranking-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>阅读排行</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- Tab 1: 借阅排行 -->
        <el-tab-pane label="借阅排行" name="borrow">
          <div class="tab-toolbar">
            <span class="toolbar-label">时间范围：</span>
            <el-radio-group v-model="borrowPeriod" @change="fetchRanking" size="small">
              <el-radio-button value="weekly">本周</el-radio-button>
              <el-radio-button value="monthly">本月</el-radio-button>
              <el-radio-button value="yearly">本年</el-radio-button>
            </el-radio-group>
          </div>

          <div class="search-bar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <input v-model="searchKeyword" type="text" placeholder="搜索书名、作者" @input="applySearch" />
            <button v-if="searchKeyword" @click="searchKeyword='';applySearch()">&times;</button>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>正在加载...</span>
          </div>
          <div v-else-if="filteredList.length === 0" class="empty-state">
            <span>{{ searchKeyword ? '未找到匹配数据' : '暂无借阅数据' }}</span>
          </div>
          <el-table v-else :data="filteredList" stripe style="width: 100%" :default-sort="{ prop: 'count', order: 'descending' }">
            <el-table-column label="排名" width="80">
              <template #default="{ $index }">
                <span v-if="$index === 0" class="medal">&#129351;</span>
                <span v-else-if="$index === 1" class="medal">&#129352;</span>
                <span v-else-if="$index === 2" class="medal">&#129353;</span>
                <span v-else class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="140" />
            <el-table-column prop="count" label="借阅次数" width="130" sortable :sort-orders="['descending']">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.count }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 2: 阅读排行 -->
        <el-tab-pane label="阅读排行" name="reading">
          <div class="search-bar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <input v-model="searchKeyword" type="text" placeholder="搜索书名、作者" @input="applySearch" />
            <button v-if="searchKeyword" @click="searchKeyword='';applySearch()">&times;</button>
          </div>
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>正在加载...</span>
          </div>
          <div v-else-if="filteredList.length === 0" class="empty-state">
            <span>{{ searchKeyword ? '未找到匹配数据' : '暂无阅读数据' }}</span>
          </div>
          <el-table v-else :data="filteredList" stripe style="width: 100%" :default-sort="{ prop: 'count', order: 'descending' }">
            <el-table-column label="排名" width="80">
              <template #default="{ $index }">
                <span v-if="$index === 0" class="medal">&#129351;</span>
                <span v-else-if="$index === 1" class="medal">&#129352;</span>
                <span v-else-if="$index === 2" class="medal">&#129353;</span>
                <span v-else class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="140" />
            <el-table-column prop="count" label="阅读次数" width="130" sortable :sort-orders="['descending']">
              <template #default="{ row }">
                <el-tag type="success" size="small">{{ row.count }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 图书收藏排行 -->
        <el-tab-pane label="图书收藏排行" name="favBook">
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>正在加载...</span>
          </div>
          <div v-else-if="filteredList.length === 0" class="empty-state">
            <span>暂无收藏数据</span>
          </div>
          <el-table v-else :data="filteredList" stripe style="width: 100%" :default-sort="{ prop: 'count', order: 'descending' }">
            <el-table-column label="排名" width="80">
              <template #default="{ $index }">
                <span v-if="$index === 0" class="medal">&#129351;</span>
                <span v-else-if="$index === 1" class="medal">&#129352;</span>
                <span v-else-if="$index === 2" class="medal">&#129353;</span>
                <span v-else class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="140" />
            <el-table-column prop="count" label="收藏人数" width="130" sortable :sort-orders="['descending']">
              <template #default="{ row }">
                <el-tag type="warning" size="small">{{ row.count }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 4: 电子书收藏排行 -->
        <el-tab-pane label="电子书收藏排行" name="favEbook">
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>正在加载...</span>
          </div>
          <div v-else-if="filteredList.length === 0" class="empty-state">
            <span>暂无收藏数据</span>
          </div>
          <el-table v-else :data="filteredList" stripe style="width: 100%" :default-sort="{ prop: 'count', order: 'descending' }">
            <el-table-column label="排名" width="80">
              <template #default="{ $index }">
                <span v-if="$index === 0" class="medal">&#129351;</span>
                <span v-else-if="$index === 1" class="medal">&#129352;</span>
                <span v-else-if="$index === 2" class="medal">&#129353;</span>
                <span v-else class="rank-num">{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="180" show-overflow-tooltip />
            <el-table-column prop="author" label="作者" width="140" />
            <el-table-column prop="count" label="收藏人数" width="130" sortable :sort-orders="['descending']">
              <template #default="{ row }">
                <el-tag type="warning" size="small">{{ row.count }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { rankingApi } from '../../api/ranking'
import { favoriteApi } from '../../api/favorite'

const activeTab = ref('borrow')
const borrowPeriod = ref('weekly')
const rankingData = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const filteredList = ref([])

const applySearch = () => {
  const kw = searchKeyword.value.toLowerCase()
  if (!kw) {
    filteredList.value = rankingData.value
    return
  }
  filteredList.value = rankingData.value.filter(item =>
    (item.title || '').toLowerCase().includes(kw) ||
    (item.author || '').toLowerCase().includes(kw)
  )
}

const onTabChange = () => {
  searchKeyword.value = ''
  fetchRanking()
}

const fetchRanking = async () => {
  loading.value = true
  try {
    let res
    const tab = activeTab.value
    if (tab === 'borrow') {
      res = await rankingApi.getBorrowRanking(borrowPeriod.value, 50)
    } else if (tab === 'reading') {
      res = await rankingApi.getReadingRanking('all', 50)
    } else if (tab === 'favBook') {
      res = await favoriteApi.getRanking('BOOK', 50)
    } else if (tab === 'favEbook') {
      res = await favoriteApi.getRanking('EBOOK', 50)
    }
    if (res && res.data.code === 200) {
      rankingData.value = res.data.data || []
      applySearch()
    }
  } catch (e) {
    console.error('获取排行失败', e)
    rankingData.value = []
    filteredList.value = []
  } finally {
    loading.value = false
  }
}

watch(borrowPeriod, () => { if (activeTab.value === 'borrow') fetchRanking() })

onMounted(() => fetchRanking())
</script>

<style scoped>
.ranking-page { display: flex; flex-direction: column; gap: 24px; }
.card-header { font-size: 16px; font-weight: 600; }

.tab-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}
.toolbar-label { font-size: 14px; color: var(--text-secondary); }

.search-bar {
  display: flex; align-items: center; gap: 12px;
  background: var(--bg-glass);
  border: 2px solid var(--border-color);
  border-radius: 12px; padding: 10px 16px; margin-bottom: 16px;
}
.search-bar:focus-within { border-color: var(--accent-1); }
.search-bar svg { width: 20px; height: 20px; color: var(--text-muted); flex-shrink: 0; }
.search-bar input {
  flex: 1; border: none; outline: none; font-size: 15px;
  background: transparent; color: var(--text-primary);
}
.search-bar input::placeholder { color: var(--text-muted); }
.search-bar button {
  background: none; border: none; cursor: pointer;
  color: var(--text-muted); padding: 0; font-size: 20px;
}

.loading-state {
  display: flex; flex-direction: column; align-items: center;
  padding: 60px; gap: 16px; color: var(--text-secondary);
}
.loading-spinner {
  width: 40px; height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  display: flex; justify-content: center; padding: 40px;
  color: var(--text-muted); font-size: 14px;
}
.medal { font-size: 22px; }
.rank-num { font-weight: 600; color: var(--text-secondary); }
</style>
