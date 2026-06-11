import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'
import Login from '../views/Login.vue'
import { useAuthStore, ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_READER } from '../stores/auth'

// 超级管理员页面
const SuperAdminHome = () => import('../views/super-admin/Index.vue')
const AdminManagement = () => import('../views/super-admin/AdminManagement.vue')
const AuditLogs = () => import('../views/super-admin/AuditLogs.vue')
const LoginLogs = () => import('../views/super-admin/LoginLogs.vue')

// 管理员页面
const AdminHome = () => import('../views/admin/Index.vue')
const UserManagement = () => import('../views/admin/UserManagement.vue')
const AdminBookList = () => import('../views/admin/BookManagement.vue')
const BorrowManagement = () => import('../views/admin/BorrowManagement.vue')
const EBookManagement = () => import('../views/admin/EBookManagement.vue')
const CategoryManagement = () => import('../views/admin/CategoryManagement.vue')
const Ranking = () => import('../views/admin/Ranking.vue')

// 普通用户页面
const ReaderHome = () => import('../views/reader/Index.vue')
const ReaderBookList = () => import('../views/reader/BookList.vue')
const CategoryBrowse = () => import('../views/reader/CategoryBrowse.vue')
const MyFavorites = () => import('../views/reader/MyFavorites.vue')
const BorrowHistory = () => import('../views/reader/BorrowHistory.vue')
const EBookList = () => import('../views/reader/EBookList.vue')
const EBookReader = () => import('../views/reader/EBookReader.vue')

const routes = [
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false, title: '注册' }
  },
  {
    path: '/super-admin/login',
    name: 'SuperAdminLogin',
    component: () => import('../views/super-admin/SuperAdminLogin.vue'),
    meta: { requiresAuth: false, title: '超级管理员登录' }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false, title: '登录' }
  },
  // 超级管理员路由
  {
    path: '/super-admin',
    component: Layout,
    meta: { requiresAuth: true, roles: [ROLE_SUPER_ADMIN] },
    children: [
      {
        path: '',
        name: 'SuperAdminHome',
        component: SuperAdminHome,
        meta: { title: '超级管理员首页' }
      },
      {
        path: 'admin-management',
        name: 'AdminManagement',
        component: AdminManagement,
        meta: { title: '管理员管理' }
      },
      {
        path: 'audit-logs',
        name: 'AuditLogs',
        component: AuditLogs,
        meta: { title: '审计日志' }
      },
      {
        path: 'login-logs',
        name: 'LoginLogs',
        component: LoginLogs,
        meta: { title: '登录日志' }
      }
    ]
  },
  // 管理员路由
  {
    path: '/admin',
    component: Layout,
    meta: { requiresAuth: true, roles: [ROLE_ADMIN] },
    children: [
      {
        path: '',
        name: 'AdminHome',
        component: AdminHome,
        meta: { title: '管理员首页' }
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: UserManagement,
        meta: { title: '用户管理' }
      },
      {
        path: 'books',
        name: 'AdminBookList',
        component: AdminBookList,
        meta: { title: '图书管理' }
      },
      {
        path: 'borrows',
        name: 'BorrowManagement',
        component: BorrowManagement,
        meta: { title: '借阅管理' }
      },
      {
        path: 'ebooks',
        name: 'EBookManagement',
        component: EBookManagement,
        meta: { title: '电子书管理' }
      },
      {
        path: 'categories',
        name: 'CategoryManagement',
        component: CategoryManagement,
        meta: { title: '图书分类管理' }
      },
      {
        path: 'rankings',
        name: 'Ranking',
        component: Ranking,
        meta: { title: '阅读排行' }
      }
    ]
  },
  // 普通用户路由
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true, roles: [ROLE_READER, ROLE_ADMIN] },
    children: [
      {
        path: '',
        name: 'ReaderHome',
        component: ReaderHome,
        meta: { title: '首页' }
      },
      {
        path: 'books',
        name: 'ReaderBookList',
        component: ReaderBookList,
        meta: { title: '图书列表' }
      },
      {
        path: 'categories',
        name: 'CategoryBrowse',
        component: CategoryBrowse,
        meta: { title: '图书分类' }
      },
      {
        path: 'my-favorites',
        name: 'MyFavorites',
        component: MyFavorites,
        meta: { title: '我的收藏' }
      },
      {
        path: 'borrow-history',
        name: 'BorrowHistory',
        component: BorrowHistory,
        meta: { title: '我的借阅' }
      },
      {
        path: 'ebooks',
        name: 'EBookList',
        component: EBookList,
        meta: { title: '在线阅读' }
      }
    ]
  },
  // 阅读器页面（独立布局）
  {
    path: '/reader/ebooks/:id',
    name: 'EBookReader',
    component: EBookReader,
    meta: { requiresAuth: true, roles: [ROLE_READER, ROLE_ADMIN], title: '阅读' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 检查是否需要认证
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }
  
  // 已登录用户访问登录页，重定向到对应首页
  if ((to.path === '/login' || to.path === '/super-admin/login') && authStore.isAuthenticated) {
    // 如果已是超级管理员，访问超级管理员登录页时直接进入后台
    if (to.path === '/super-admin/login' && authStore.isSuperAdmin) {
      next('/super-admin')
      return
    }
    // 如果是非管理员访问登录页，重定向到首页
    if (to.path === '/login') {
      next(authStore.getHomeRoute())
      return
    }
  }
  
  // 检查角色权限
  if (to.meta.roles && to.meta.roles.length > 0) {
    const hasRole = authStore.hasRole(to.meta.roles)
    if (!hasRole) {
      // 没有权限，重定向到对应首页
      next(authStore.getHomeRoute())
      return
    }
  }
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 图书管理系统` : '图书管理系统'
  
  next()
})

export default router
