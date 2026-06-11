import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getEBookList, getEBookDetail, getChapters, getStartReading, getChapterByIndex, saveProgress } from '../api/ebook'

export const useEBookStore = defineStore('ebook', () => {
  // 电子书列表
  const ebooks = ref([])
  // 当前阅读的电子书
  const currentEBook = ref(null)
  // 当前电子书的章节列表
  const chapters = ref([])
  // 当前章节
  const currentChapter = ref(null)
  // 当前章节索引
  const currentChapterIndex = ref(0)
  // 阅读进度
  const readingProgress = ref(null)
  // 阅读设置
  const readingSettings = ref({
    fontSize: 18,
    fontWeight: 400,
    theme: 'day' // day, night
  })

  // 从 localStorage 加载阅读设置
  const loadSettings = () => {
    const saved = localStorage.getItem('readingSettings')
    if (saved) {
      const parsed = JSON.parse(saved)
      readingSettings.value = {
        ...readingSettings.value,
        ...parsed
      }
      // 兼容旧版本 small/medium/large 与 normal/bold 配置
      if (typeof readingSettings.value.fontSize === 'string') {
        const sizeMap = { small: 14, medium: 18, large: 22 }
        readingSettings.value.fontSize = sizeMap[readingSettings.value.fontSize] || 18
      }
      if (typeof readingSettings.value.fontWeight === 'string') {
        readingSettings.value.fontWeight = readingSettings.value.fontWeight === 'bold' ? 700 : 400
      }
    }
  }

  // 保存阅读设置到 localStorage
  const saveSettings = () => {
    localStorage.setItem('readingSettings', JSON.stringify(readingSettings.value))
  }

  // 获取字体大小
  const fontSize = computed(() => {
    return `${readingSettings.value.fontSize || 18}px`
  })

  // 获取字体粗细
  const fontWeight = computed(() => {
    return `${readingSettings.value.fontWeight || 400}`
  })

  // 获取主题样式
  const themeStyles = computed(() => {
    if (readingSettings.value.theme === 'night') {
      return {
        background: '#1a1a1a',
        color: '#e0e0e0'
      }
    }
    return {
      background: '#ffffff',
      color: '#333333'
    }
  })

  // 设置字体大小
  const setFontSize = (size) => {
    readingSettings.value.fontSize = size
    saveSettings()
  }

  // 设置字体粗细
  const setFontWeight = (weight) => {
    readingSettings.value.fontWeight = weight
    saveSettings()
  }

  // 设置主题
  const setTheme = (theme) => {
    readingSettings.value.theme = theme
    saveSettings()
  }

  // 加载电子书列表
  const loadEBookList = async () => {
    try {
      const response = await getEBookList()
      if (response.data.code === 200) {
        ebooks.value = response.data.data || []
      }
    } catch (error) {
      console.error('加载电子书列表失败:', error)
    }
  }

  // 加载章节列表
  const loadChapters = async (ebookId) => {
    try {
      const response = await getChapters(ebookId)
      if (response.data.code === 200) {
        chapters.value = response.data.data || []
      }
    } catch (error) {
      console.error('加载章节失败:', error)
    }
  }

  // 开始阅读
  const startReading = async (ebookId) => {
    try {
      // 先从本地列表查找，如果没有则从 API 获取（支持直接 URL 进入）
      let ebook = ebooks.value.find(e => e.id === ebookId)
      if (!ebook) {
        const detailRes = await getEBookDetail(ebookId)
        if (detailRes.data.code === 200 && detailRes.data.data) {
          ebook = detailRes.data.data
        }
      }
      currentEBook.value = ebook || null

      await loadChapters(ebookId)
      const response = await getStartReading(ebookId)
      if (response.data.code === 200) {
        currentChapter.value = response.data.data?.chapter || null
        readingProgress.value = response.data.data?.progress || null
        if (currentChapter.value) {
          currentChapterIndex.value = currentChapter.value.chapterIndex
        }
      }
    } catch (error) {
      console.error('开始阅读失败:', error)
    }
  }

  // 跳转到指定章节
  const goToChapter = async (index) => {
    if (index < 0 || index >= chapters.value.length) {
      return
    }
    currentChapterIndex.value = index
    let chapter = chapters.value[index]
    // 如果章节内容为空，从 API 加载
    if (!chapter.content) {
      try {
        const res = await getChapterByIndex(currentEBook.value.id, index)
        if (res.data.code === 200 && res.data.data) {
          chapter = res.data.data
          chapters.value[index] = chapter
        }
      } catch (error) {
        console.error('加载章节内容失败:', error)
      }
    }
    currentChapter.value = chapter
    await saveCurrentProgress(0)
  }

  // 上一章
  const prevChapter = async () => {
    if (currentChapterIndex.value > 0) {
      await goToChapter(currentChapterIndex.value - 1)
    }
  }

  // 下一章
  const nextChapter = async () => {
    if (currentChapterIndex.value < chapters.value.length - 1) {
      await goToChapter(currentChapterIndex.value + 1)
    }
  }

  // 保存当前阅读进度
  const saveCurrentProgress = async (scrollPosition) => {
    if (!currentEBook.value || !currentChapter.value) {
      return
    }
    try {
      const response = await saveProgress({
        ebookId: currentEBook.value.id,
        chapterId: currentChapter.value.id,
        chapterIndex: currentChapterIndex.value,
        scrollPosition: scrollPosition
      })
      if (response.data.code === 200) {
        readingProgress.value = response.data.data
      }
    } catch (error) {
      console.error('保存进度失败:', error)
    }
  }

  // 初始化
  loadSettings()

  return {
    ebooks,
    currentEBook,
    chapters,
    currentChapter,
    currentChapterIndex,
    readingProgress,
    readingSettings,
    fontSize,
    fontWeight,
    themeStyles,
    setFontSize,
    setFontWeight,
    setTheme,
    loadEBookList,
    loadChapters,
    startReading,
    goToChapter,
    prevChapter,
    nextChapter,
    saveCurrentProgress
  }
})
