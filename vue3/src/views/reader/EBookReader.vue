<template>
  <div class="reader-container" :style="ebookStore.themeStyles">
    <!-- 顶部导航 -->
    <div class="reader-header">
      <el-button @click="goBack" text>
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <span class="ebook-title">{{ ebookStore.currentEBook?.title }}</span>
      <div class="header-actions">
        <el-button @click="showParagraphDrawer = true" text>
          <el-icon><List /></el-icon>
          段落目录
        </el-button>
        <el-button @click="showSettings = true" text>
          <el-icon><Setting /></el-icon>
          设置
        </el-button>
      </div>
    </div>

    <!-- 侧边目录 -->
    <div class="reader-content">
      <div class="chapter-list" v-if="showChapters">
        <h3>目录</h3>
        <ul>
          <li
            v-for="(chapter, index) in ebookStore.chapters"
            :key="chapter.id"
            :class="{ active: index === ebookStore.currentChapterIndex }"
            @click="ebookStore.goToChapter(index)"
          >
            {{ chapter.title }}
          </li>
        </ul>
      </div>

      <!-- 阅读区域 -->
      <div class="reading-area" ref="readingArea" @scroll="handleScroll">
        <div v-if="ebookStore.currentChapter" class="chapter-content">
          <h2 class="chapter-title">{{ ebookStore.currentChapter.title }}</h2>
          <div
            class="content-text"
            :style="{
              fontSize: ebookStore.fontSize,
              fontWeight: ebookStore.fontWeight
            }"
          >
            <p
              v-for="(paragraph, index) in paragraphs"
              :key="index"
              :id="`paragraph-${index}`"
              :class="{ 'current-paragraph': index === currentParagraphIndex }"
            >
              {{ paragraph }}
            </p>
          </div>
        </div>
        <div v-else class="loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <p>加载中...</p>
        </div>
      </div>
    </div>

    <!-- 底部翻页 -->
    <div class="reader-footer">
      <el-button
        :disabled="ebookStore.currentChapterIndex <= 0"
        @click="ebookStore.prevChapter"
      >
        <el-icon><DArrowLeft /></el-icon>
        上一章
      </el-button>
      <span class="page-info">
        {{ ebookStore.currentChapterIndex + 1 }} / {{ ebookStore.chapters.length }}
      </span>
      <el-button
        :disabled="ebookStore.currentChapterIndex >= ebookStore.chapters.length - 1"
        @click="ebookStore.nextChapter"
      >
        下一章
        <el-icon><DArrowRight /></el-icon>
      </el-button>
    </div>

    <!-- 设置面板 -->
    <el-drawer v-model="showSettings" title="阅读设置" size="300px">
      <div class="settings-panel">
        <div class="setting-item">
          <label>字体大小</label>
          <el-slider
            v-model="fontSizeValue"
            :min="14"
            :max="30"
            :step="1"
            show-input
            @change="handleFontSizeChange"
          />
        </div>

        <div class="setting-item">
          <label>字体粗细</label>
          <el-slider
            v-model="fontWeightValue"
            :min="300"
            :max="900"
            :step="100"
            show-input
            @change="handleFontWeightChange"
          />
        </div>

        <div class="setting-item">
          <label>阅读模式</label>
          <el-radio-group v-model="ebookStore.readingSettings.theme" @change="ebookStore.saveSettings">
            <el-radio-button value="day">白天</el-radio-button>
            <el-radio-button value="night">夜晚</el-radio-button>
          </el-radio-group>
        </div>

        <div class="setting-item">
          <el-button @click="showChapters = !showChapters" type="primary">
            {{ showChapters ? '隐藏目录' : '显示目录' }}
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="showParagraphDrawer" title="段落目录" size="360px">
      <div class="paragraph-drawer-content">
        <div class="paragraph-summary">
          当前段落：{{ currentParagraphIndex + 1 }} / {{ paragraphs.length }}
        </div>
        <div class="paragraph-list">
          <div
            v-for="(paragraph, index) in paragraphs"
            :key="`toc-${index}`"
            class="paragraph-item"
            :class="{ active: index === currentParagraphIndex }"
            @click="jumpToParagraph(index)"
          >
            <span class="paragraph-index">{{ index + 1 }}.</span>
            <span class="paragraph-title">{{ getParagraphTitle(paragraph) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useEBookStore } from '../../stores/ebook'
import { useAuthStore } from '../../stores/auth'
import {
  ArrowLeft,
  Setting,
  DArrowLeft,
  DArrowRight,
  Loading,
  List
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const ebookStore = useEBookStore()
const authStore = useAuthStore()

const readingArea = ref(null)
const showSettings = ref(false)
const showChapters = ref(true)
const showParagraphDrawer = ref(false)
const currentParagraphIndex = ref(0)
let saveProgressTimer = null
const fontSizeValue = ref(18)
const fontWeightValue = ref(400)

const paragraphs = computed(() => {
  if (!ebookStore.currentChapter) return []
  return ebookStore.currentChapter.content.split('\n').filter(p => p.trim())
})

const goBack = () => {
  if (authStore.isAdmin || authStore.isSuperAdmin) {
    router.push('/admin/ebooks')
  } else {
    router.push('/ebooks')
  }
}

const handleScroll = () => {
  updateCurrentParagraphByScroll()
  if (saveProgressTimer) {
    clearTimeout(saveProgressTimer)
  }
  saveProgressTimer = setTimeout(() => {
    if (readingArea.value) {
      ebookStore.saveCurrentProgress(readingArea.value.scrollTop)
    }
  }, 1000)
}

const updateCurrentParagraphByScroll = () => {
  if (!readingArea.value || paragraphs.value.length === 0) return
  const paragraphElements = readingArea.value.querySelectorAll('.content-text p')
  const readerRect = readingArea.value.getBoundingClientRect()
  let active = 0

  paragraphElements.forEach((el, index) => {
    const rect = el.getBoundingClientRect()
    if (rect.top - readerRect.top <= 80) {
      active = index
    }
  })

  currentParagraphIndex.value = active
}

const jumpToParagraph = (index) => {
  const target = document.getElementById(`paragraph-${index}`)
  if (!target || !readingArea.value) return
  readingArea.value.scrollTo({
    top: target.offsetTop - 20,
    behavior: 'smooth'
  })
  currentParagraphIndex.value = index
}

const getParagraphTitle = (paragraph) => {
  const cleaned = paragraph.trim().replace(/\s+/g, ' ')
  return cleaned.length > 24 ? `${cleaned.slice(0, 24)}...` : cleaned
}

const handleFontSizeChange = (value) => {
  ebookStore.setFontSize(value)
}

const handleFontWeightChange = (value) => {
  ebookStore.setFontWeight(value)
}

const restoreScrollPosition = async () => {
  if (readingArea.value && ebookStore.readingProgress) {
    await nextTick()
    requestAnimationFrame(() => {
      if (readingArea.value) {
        readingArea.value.scrollTop = ebookStore.readingProgress.scrollPosition || 0
        updateCurrentParagraphByScroll()
      }
    })
  }
}

watch(() => ebookStore.currentChapterIndex, () => {
  if (readingArea.value) {
    readingArea.value.scrollTop = 0
  }
  currentParagraphIndex.value = 0
})

watch(
  () => ebookStore.readingSettings,
  (settings) => {
    fontSizeValue.value = settings.fontSize
    fontWeightValue.value = settings.fontWeight
  },
  { deep: true, immediate: true }
)

watch(paragraphs, async () => {
  await nextTick()
  updateCurrentParagraphByScroll()
})

onMounted(async () => {
  const ebookId = Number(route.params.id)
  await ebookStore.startReading(ebookId)
  await restoreScrollPosition()
})

onUnmounted(() => {
  if (saveProgressTimer) {
    clearTimeout(saveProgressTimer)
  }
  ebookStore.saveCurrentProgress(readingArea.value?.scrollTop || 0)
})
</script>

<style scoped>
.reader-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
}

.reader-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.ebook-title {
  font-size: 18px;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.reader-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chapter-list {
  width: 250px;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
  padding: 20px;
  overflow-y: auto;
}

.chapter-list h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.chapter-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.chapter-list li {
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.chapter-list li:hover {
  background: rgba(0, 0, 0, 0.05);
}

.chapter-list li.active {
  background: var(--el-color-primary);
  color: white;
}

.reading-area {
  flex: 1;
  overflow-y: auto;
  padding: 40px 60px;
}

.chapter-content {
  max-width: 800px;
  margin: 0 auto;
}

.chapter-title {
  text-align: center;
  margin-bottom: 40px;
  font-size: 24px;
}

.content-text {
  line-height: 2;
  text-align: justify;
}

.content-text p {
  margin: 0 0 20px 0;
  text-indent: 2em;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.content-text p.current-paragraph {
  background: rgba(64, 158, 255, 0.12);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.loading .el-icon {
  font-size: 32px;
  margin-bottom: 16px;
}

.reader-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.page-info {
  color: #666;
}

.settings-panel {
  padding: 10px;
}

.setting-item {
  margin-bottom: 24px;
}

.setting-item label {
  display: block;
  margin-bottom: 12px;
  font-weight: bold;
}

.paragraph-drawer-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.paragraph-summary {
  color: #666;
  font-size: 13px;
}

.paragraph-list {
  overflow-y: auto;
  flex: 1;
}

.paragraph-item {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.paragraph-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

.paragraph-item.active {
  background: rgba(64, 158, 255, 0.16);
  color: #409eff;
}

.paragraph-index {
  width: 28px;
  color: #999;
  text-align: right;
  flex-shrink: 0;
}

.paragraph-title {
  flex: 1;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
</style>
