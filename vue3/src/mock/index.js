import Mock from 'mockjs'
import request from '../utils/request'
import MockAdapter from 'axios-mock-adapter'

const mock = new MockAdapter(request, { delayResponse: 400 })

// ── Mock Data ──────────────────────────────────────────────
const now = new Date().toISOString().replace('T', ' ').substring(0, 19)

const categories = [
  { id: 1, name: '玄幻', description: '东方玄幻、异界大陆类小说', parentId: null, createTime: '2024-01-10 10:00:00', updateTime: '2024-01-10 10:00:00' },
  { id: 2, name: '都市', description: '现代都市生活、职场类小说', parentId: null, createTime: '2024-01-10 10:00:00', updateTime: '2024-01-10 10:00:00' },
  { id: 3, name: '科幻', description: '未来科技、星际探索类小说', parentId: null, createTime: '2024-01-10 10:00:00', updateTime: '2024-01-10 10:00:00' },
  { id: 4, name: '东方玄幻', description: '东方奇幻修真', parentId: 1, createTime: '2024-01-15 10:00:00', updateTime: '2024-01-15 10:00:00' },
  { id: 5, name: '异界大陆', description: '穿越异界题材', parentId: 1, createTime: '2024-01-15 10:00:00', updateTime: '2024-01-15 10:00:00' },
  { id: 6, name: '硬科幻', description: '基于科学理论的科幻', parentId: 3, createTime: '2024-01-15 10:00:00', updateTime: '2024-01-15 10:00:00' },
]

function buildCategoryTree(flatList) {
  const map = {}
  const roots = []
  flatList.forEach(c => { map[c.id] = { ...c, children: [] } })
  flatList.forEach(c => {
    if (c.parentId && map[c.parentId]) {
      map[c.parentId].children.push(map[c.id])
    } else if (!c.parentId) {
      roots.push(map[c.id])
    }
  })
  return roots
}

const books = [
  { id: 1, isbn: '978-7-5322-6000-1', title: '斗破苍穹', author: '天蚕土豆', publisher: '浙江文艺出版社', publishDate: '2011-05-01', price: 68.00, stock: 10, available: 8, categories: [{ id: 1, name: '玄幻' }], createTime: '2024-01-10 10:00:00', updateTime: now },
  { id: 2, isbn: '978-7-5366-9000-2', title: '斗罗大陆', author: '唐家三少', publisher: '浙江少年儿童出版社', publishDate: '2009-01-01', price: 58.00, stock: 15, available: 2, categories: [{ id: 1, name: '玄幻' }], createTime: '2024-01-10 10:00:00', updateTime: now },
  { id: 3, isbn: '978-7-5000-1000-3', title: '三体', author: '刘慈欣', publisher: '重庆出版社', publishDate: '2008-01-01', price: 93.00, stock: 20, available: 0, categories: [{ id: 3, name: '科幻' }], createTime: '2024-01-10 10:00:00', updateTime: now },
  { id: 4, isbn: '978-7-5000-2000-4', title: '都市之最强赘婿', author: '匿名', publisher: '起点中文网', publishDate: '2020-06-15', price: 45.00, stock: 8, available: 1, categories: [{ id: 2, name: '都市' }], createTime: '2024-01-10 10:00:00', updateTime: now },
  { id: 5, isbn: '978-7-5000-3000-5', title: '凡人修仙传', author: '忘语', publisher: '起点中文网', publishDate: '2009-07-01', price: 75.00, stock: 12, available: 5, categories: [{ id: 1, name: '玄幻' }], createTime: '2024-01-10 10:00:00', updateTime: now },
]
let bookIdSeq = 6

const ebooks = [
  { id: 1, title: '三体全集', author: '刘慈欣', description: '包含三体、黑暗森林、死神永生', fileType: 'txt', fileSize: 2048000, chapterCount: 120, coverPath: '', categories: [{ id: 3, name: '科幻' }], createTime: '2024-02-01 10:00:00', updateTime: now },
  { id: 2, title: '斗破苍穹', author: '天蚕土豆', description: '三十年河东三十年河西，莫欺少年穷', fileType: 'txt', fileSize: 5120000, chapterCount: 320, coverPath: '', categories: [{ id: 1, name: '玄幻' }], createTime: '2024-02-01 10:00:00', updateTime: now },
  { id: 3, title: '都市极品医圣', author: '风中的阳光', description: '妙手仁心，都市行医', fileType: 'txt', fileSize: 1560000, chapterCount: 98, coverPath: '', categories: [{ id: 2, name: '都市' }], createTime: '2024-02-01 10:00:00', updateTime: now },
]

const users = [
  { id: 1, username: 'reader1', name: '张读者', email: 'reader1@example.com', phone: '13800001001', status: 1, createTime: '2024-01-01 10:00:00' },
  { id: 2, username: 'reader2', name: '李书虫', email: 'reader2@example.com', phone: '13800001002', status: 1, createTime: '2024-01-02 10:00:00' },
  { id: 3, username: 'reader3', name: '王爱书', email: 'reader3@example.com', phone: '13800001003', status: 0, createTime: '2024-01-03 10:00:00' },
]

const admins = [
  { id: 1, username: 'admin1', name: '管理员甲', email: 'admin1@library.com', status: 1, role: 'ADMIN', permissions: 'BOOK_ADD,BOOK_EDIT,BOOK_DELETE,CATEGORY_ADD,CATEGORY_DELETE,USER_VIEW,USER_EDIT,BORROW_VIEW' },
]

const borrows = [
  { id: 1, bookId: 1, bookTitle: '斗破苍穹', userId: 1, username: 'reader1', borrowTime: '2024-05-01 10:00:00', dueTime: '2024-06-01 10:00:00', returnTime: null, status: 'BORROWED' },
  { id: 2, bookId: 2, bookTitle: '斗罗大陆', userId: 2, username: 'reader2', borrowTime: '2024-04-15 10:00:00', dueTime: '2024-05-15 10:00:00', returnTime: '2024-05-10 10:00:00', status: 'RETURNED' },
  { id: 3, bookId: 3, bookTitle: '三体', userId: 1, username: 'reader1', borrowTime: '2024-04-01 10:00:00', dueTime: '2024-05-01 10:00:00', returnTime: null, status: 'OVERDUE' },
]

let borrowIdSeq = 4

// ── Auth ───────────────────────────────────────────────────
mock.onPost('/api/auth/login').reply((config) => {
  const { username, password, role } = JSON.parse(config.data)
  if (username === 'admin' && password === '123456') {
    return [200, { code: 200, message: '登录成功', data: { token: 'mock-token-admin-' + Date.now(), username: 'admin', role: role || 'ADMIN', name: '管理员', userId: 1, userType: 'ADMIN', permissions: 'BOOK_ADD,BOOK_EDIT,BOOK_DELETE,CATEGORY_ADD,CATEGORY_DELETE,USER_VIEW,USER_EDIT,BORROW_VIEW' } }]
  }
  if (username === 'super' && password === '123456') {
    return [200, { code: 200, message: '登录成功', data: { token: 'mock-token-super-' + Date.now(), username: 'super', role: 'SUPER_ADMIN', name: '超级管理员', userId: 999, userType: 'SUPER_ADMIN', permissions: '' } }]
  }
  if (username === 'reader1' && password === '123456') {
    return [200, { code: 200, message: '登录成功', data: { token: 'mock-token-reader-' + Date.now(), username: 'reader1', role: 'READER', name: '张读者', userId: 1, userType: 'USER', permissions: '' } }]
  }
  return [401, { code: 401, message: '用户名或密码错误', data: null }]
})

mock.onPost('/api/auth/logout').reply(200, { code: 200, message: '已退出', data: null })
mock.onPost('/api/auth/register').reply(200, { code: 200, message: '注册成功', data: null })

// ── Books ──────────────────────────────────────────────────
mock.onGet('/api/books').reply((config) => {
  const url = config.url || ''
  if (url.includes('search')) {
    const keyword = new URLSearchParams(url.split('?')[1] || '').get('keyword') || ''
    const result = books.filter(b => b.title.includes(keyword) || b.author.includes(keyword) || b.isbn.includes(keyword))
    return [200, { code: 200, message: '获取成功', data: result }]
  }
  if (url.includes('warning')) {
    const low = books.filter(b => b.available > 0 && b.available <= 2)
    const out = books.filter(b => b.available === 0)
    return [200, { code: 200, message: '获取成功', data: { hasWarning: low.length > 0 || out.length > 0, lowStockCount: low.length, outOfStockCount: out.length, lowStockBooks: low.map(b => ({ id: b.id, title: b.title, available: b.available })), outOfStockBooks: out.map(b => ({ id: b.id, title: b.title })) } }]
  }
  return [200, { code: 200, message: '获取成功', data: books }]
})

mock.onGet(/\/api\/books\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/api\/books\/(\d+)$/)[1])
  const book = books.find(b => b.id === id)
  return book ? [200, { code: 200, message: '获取成功', data: book }] : [404, { code: 404, message: '图书不存在', data: null }]
})

mock.onPost('/api/books').reply((config) => {
  const data = JSON.parse(config.data)
  const newBook = { id: bookIdSeq++, isbn: data.isbn || '', title: data.title, author: data.author, publisher: data.publisher || '', publishDate: data.publishDate || '', price: data.price || 0, stock: data.stock || 0, available: data.stock || 0, categories: [], createTime: new Date().toISOString().replace('T', ' ').substring(0, 19), updateTime: now }
  books.unshift(newBook)
  return [200, { code: 200, message: '添加成功', data: newBook }]
})

mock.onPut(/\/api\/books\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/api\/books\/(\d+)$/)[1])
  const data = JSON.parse(config.data)
  const idx = books.findIndex(b => b.id === id)
  if (idx >= 0) { Object.assign(books[idx], data); return [200, { code: 200, message: '更新成功', data: books[idx] }] }
  return [404, { code: 404, message: '图书不存在', data: null }]
})

mock.onDelete(/\/api\/books\/\d+$/).reply(200, { code: 200, message: '删除成功', data: null })
mock.onPost('/api/books/import').reply(200, { code: 200, message: '导入成功', data: [] })
mock.onGet('/api/books/export').reply(200, '模拟导出TXT内容')

// ── Categories ─────────────────────────────────────────────
mock.onGet('/api/categories').reply((config) => {
  const url = config.url || ''
  if (url.includes('tree=true')) {
    return [200, { code: 200, message: '获取成功', data: buildCategoryTree(categories) }]
  }
  return [200, { code: 200, message: '获取成功', data: categories }]
})

mock.onGet(/\/api\/categories\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/api\/categories\/(\d+)$/)[1])
  const cat = categories.find(c => c.id === id)
  return cat ? [200, { code: 200, message: '获取成功', data: cat }] : [404, { code: 404, message: '分类不存在', data: null }]
})

mock.onPost('/api/categories').reply((config) => {
  const data = JSON.parse(config.data)
  const newCat = { id: categories.length + 1, name: data.name, description: data.description || '', parentId: data.parentId || null, createTime: new Date().toISOString().replace('T', ' ').substring(0, 19), updateTime: now }
  categories.push(newCat)
  return [200, { code: 200, message: '添加成功', data: newCat }]
})

mock.onPut(/\/api\/categories\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/api\/categories\/(\d+)$/)[1])
  const data = JSON.parse(config.data)
  const idx = categories.findIndex(c => c.id === id)
  if (idx >= 0) { Object.assign(categories[idx], data); return [200, { code: 200, message: '更新成功', data: categories[idx] }] }
  return [404, { code: 404, message: '分类不存在', data: null }]
})

mock.onDelete(/\/api\/categories\/\d+$/).reply(200, { code: 200, message: '删除成功', data: null })

// ── Users & Admins ─────────────────────────────────────────
mock.onGet('/api/admin/user-management').reply(200, { code: 200, message: '获取成功', data: users })
mock.onPost('/api/admin/user-management').reply(200, { code: 200, message: '添加成功', data: { id: 99, username: 'newuser', name: '新用户', status: 1 } })
mock.onPut(/\/api\/admin\/user-management\/\d+$/).reply(200, { code: 200, message: '更新成功', data: null })
mock.onDelete(/\/api\/admin\/user-management\/\d+$/).reply(200, { code: 200, message: '删除成功', data: null })

mock.onGet('/api/admin/admin-management').reply(200, { code: 200, message: '获取成功', data: admins })
mock.onGet('/api/admin/admin-management/dashboard-stats').reply(200, { code: 200, message: '获取成功', data: { userCount: users.length, adminCount: admins.length, bookCount: books.length, borrowCount: borrows.length, auditCount: 45, loginLogCount: 120 } })

// ── Borrows ────────────────────────────────────────────────
mock.onGet('/api/borrows').reply(200, { code: 200, message: '获取成功', data: borrows })
mock.onGet(/\/api\/borrows\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/api\/borrows\/(\d+)$/)[1])
  const b = borrows.find(x => x.id === id)
  return b ? [200, { code: 200, message: '获取成功', data: b }] : [404, { code: 404, message: '不存在', data: null }]
})
mock.onGet('/api/borrows/my').reply(200, { code: 200, message: '获取成功', data: borrows.slice(0, 2) })
mock.onPost('/api/borrows/borrow').reply((config) => {
  const data = JSON.parse(config.data)
  const book = books.find(b => b.id === data.bookId)
  if (!book || book.available <= 0) return [400, { code: 400, message: '库存不足', data: null }]
  book.available--
  const newBorrow = { id: borrowIdSeq++, bookId: book.id, bookTitle: book.title, userId: data.userId || 1, username: 'reader1', borrowTime: new Date().toISOString().replace('T', ' ').substring(0, 19), dueTime: new Date(Date.now() + 30 * 86400000).toISOString().replace('T', ' ').substring(0, 19), returnTime: null, status: 'BORROWED' }
  borrows.unshift(newBorrow)
  return [200, { code: 200, message: '借阅成功', data: newBorrow }]
})
mock.onPost(/\/api\/borrows\/return\/\d+$/).reply(200, { code: 200, message: '归还成功', data: null })
mock.onPost(/\/api\/borrows\/cancel\/\d+$/).reply(200, { code: 200, message: '取消成功', data: null })
mock.onGet('/api/borrows/overdue').reply(200, { code: 200, message: '获取成功', data: borrows.filter(b => b.status === 'OVERDUE') })

// ── EBooks ─────────────────────────────────────────────────
mock.onGet('/api/ebooks').reply(200, { code: 200, message: '获取成功', data: ebooks })
mock.onGet(/\/api\/ebooks\/\d+$/).reply((config) => {
  const id = parseInt(config.url.match(/\/api\/ebooks\/(\d+)$/)[1])
  const eb = ebooks.find(e => e.id === id)
  return eb ? [200, { code: 200, message: '获取成功', data: eb }] : [404, { code: 404, message: '电子书不存在', data: null }]
})
mock.onGet(/\/api\/ebooks\/\d+\/chapters$/).reply(200, { code: 200, message: '获取成功', data: [{ index: 0, title: '第一章 楔子', wordCount: 3500 }, { index: 1, title: '第二章 开始', wordCount: 4200 }, { index: 2, title: '第三章 发展', wordCount: 3800 }] })
mock.onGet(/\/api\/ebooks\/\d+\/chapters\/\d+$/).reply(200, { code: 200, message: '获取成功', data: { index: 0, title: '第一章 楔子', content: '这是模拟的章节内容。' + Mock.Random.paragraph(20), wordCount: 3500 } })
mock.onPost('/api/ebooks/upload').reply(200, { code: 200, message: '上传成功', data: { id: 99, title: '新电子书' } })
mock.onPut(/\/api\/ebooks\/\d+$/).reply(200, { code: 200, message: '更新成功', data: null })
mock.onDelete(/\/api\/ebooks\/\d+$/).reply(200, { code: 200, message: '删除成功', data: null })

// ── Rankings ───────────────────────────────────────────────
mock.onGet('/api/rankings/borrow').reply(200, { code: 200, message: '获取成功', data: books.slice(0, 3).map(b => ({ id: b.id, title: b.title, author: b.author, count: Mock.Random.integer(20, 200) })) })
mock.onGet('/api/rankings/reading').reply(200, { code: 200, message: '获取成功', data: ebooks.slice(0, 3).map(e => ({ id: e.id, title: e.title, author: e.author, count: Mock.Random.integer(10, 150) })) })

// ── Favorites ──────────────────────────────────────────────
mock.onPost('/api/favorites/toggle').reply(200, { code: 200, message: '操作成功', data: { favorited: true } })
mock.onGet('/api/favorites/my/ids').reply(200, { code: 200, message: '获取成功', data: [1, 2] })
mock.onGet('/api/favorites/my/list').reply(200, { code: 200, message: '获取成功', data: [books[0], books[1]] })
mock.onGet('/api/favorites/ranking').reply(200, { code: 200, message: '获取成功', data: books.slice(0, 4).map(b => ({ id: b.id, title: b.title, author: b.author, count: Mock.Random.integer(5, 80) })) })

// ── Audit Logs (Super Admin) ───────────────────────────────
mock.onGet('/api/admin/audit-logs').reply(200, { code: 200, message: '获取成功', data: { records: [], total: 0 } })
mock.onGet('/api/admin/login-logs').reply(200, { code: 200, message: '获取成功', data: { records: [], total: 0 } })

// ── Fallback ───────────────────────────────────────────────
mock.onAny().reply((config) => {
  console.warn('[Mock] 未匹配的请求:', config.method, config.url)
  return [200, { code: 200, message: 'mock ok', data: null }]
})

export default mock
