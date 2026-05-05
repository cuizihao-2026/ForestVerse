import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import { isLoggedIn } from '../stores/auth'
import { isAdmin } from '../stores/permission'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/personal',
    name: 'Personal',
    component: () => import('../views/Personal.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/article/:id',
    name: 'Article',
    component: () => import('../views/Article.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/friends',
    name: 'Friends',
    component: () => import('../views/Friends.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 检查是否是登录页面且已经登录
  if ((to.path === '/login' || to.path === '/register') && isLoggedIn.value) {
    // 已登录访问登录或注册页面，跳转到首页
    next('/home')
  } else if (to.meta.requiresAuth && !isLoggedIn.value) {
    // 需要登录但未登录，跳转到登录页
    next('/login')
  } else if (to.meta.requiresAdmin && !isAdmin()) {
    // 需要管理员权限但不是管理员，跳转到首页
    next('/home')
  } else {
    // 其他情况正常跳转
    next()
  }
})

export default router
