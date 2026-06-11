import { defineStore } from 'pinia'
import { ref } from 'vue'
import { bookApi } from '../api/book'

export const useBookStore = defineStore('book', () => {
  const books = ref([])
  const loading = ref(false)

  async function fetchBooks() {
    loading.value = true
    try {
      const res = await bookApi.getAll()
      if (res.data.code === 200) {
        books.value = res.data.data
      }
    } catch (error) {
      console.error('获取图书列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  async function searchBooks(keyword) {
    loading.value = true
    try {
      const res = await bookApi.search(keyword)
      if (res.data.code === 200) {
        books.value = res.data.data
      }
    } catch (error) {
      console.error('搜索失败:', error)
    } finally {
      loading.value = false
    }
  }

  async function addBook(book) {
    try {
      const res = await bookApi.add(book)
      if (res.data.code === 200) {
        await fetchBooks()
        return res.data
      }
    } catch (error) {
      console.error('添加图书失败:', error)
      throw error
    }
  }

  async function updateBook(id, book) {
    try {
      const res = await bookApi.update(id, book)
      if (res.data.code === 200) {
        await fetchBooks()
        return res.data
      }
    } catch (error) {
      console.error('更新图书失败:', error)
      throw error
    }
  }

  async function deleteBook(id) {
    try {
      const res = await bookApi.delete(id)
      if (res.data.code === 200) {
        await fetchBooks()
        return res.data
      }
    } catch (error) {
      console.error('删除图书失败:', error)
      throw error
    }
  }

  async function importBooks(file) {
    try {
      const res = await bookApi.import(file)
      if (res.data.code === 200) {
        await fetchBooks()
        return res.data
      }
    } catch (error) {
      console.error('导入失败:', error)
      throw error
    }
  }

  // ========== 下载功能已禁用 ==========
  // async function exportBooks() {
  //   try {
  //     const res = await bookApi.export()
  //     const url = window.URL.createObjectURL(new Blob([res.data]))
  //     const link = document.createElement('a')
  //     link.href = url
  //     link.setAttribute('download', 'books.txt')
  //     document.body.appendChild(link)
  //     link.click()
  //     link.remove()
  //   } catch (error) {
  //     console.error('导出失败:', error)
  //     throw error
  //   }
  // }

  return {
    books,
    loading,
    fetchBooks,
    searchBooks,
    addBook,
    updateBook,
    deleteBook,
    importBooks,
    // exportBooks  // 下载功能已禁用
  }
})
