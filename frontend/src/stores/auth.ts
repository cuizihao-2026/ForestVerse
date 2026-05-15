import { ref } from 'vue'
import router from '../router'
import { get, post } from '../utils/api'
import { loadNotifications, resetNotifications } from './notification'
import {
  loadUserPermissions,
  loadUserRoleDescription,
  userPermissions,
  currentRoleDescription
} from './permission'

interface User {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  bio: string
  role: string
  status: string
}

export const isLoggedIn = ref(false)
export const currentUser = ref<User | null>(null)
export const authToken = ref<string | null>(null)

export const getToken = () => {
  if (!authToken.value) {
    authToken.value = localStorage.getItem('authToken')
  }
  return authToken.value
}

export const setToken = (token: string) => {
  authToken.value = token
  localStorage.setItem('authToken', token)
}

export const removeToken = () => {
  authToken.value = null
  localStorage.removeItem('authToken')
}

export const checkLoginStatus = async () => {
  const userStr = localStorage.getItem('currentUser')
  const token = getToken()

  if (userStr && token) {
    try {
      const user = JSON.parse(userStr)
      isLoggedIn.value = true
      currentUser.value = user

      const permissionsStr = localStorage.getItem('userPermissions')
      if (permissionsStr) {
        const permissions = JSON.parse(permissionsStr)
        userPermissions.value = new Set(permissions)
      }

      const roleDescriptionStr = localStorage.getItem('userRoleDescription')
      if (roleDescriptionStr) {
        currentRoleDescription.value = roleDescriptionStr
      }

      window.dispatchEvent(new CustomEvent('auth-login'))
      loadNotifications()
      await loadUserPermissions()
      await loadUserRoleDescription()
      return true
    } catch (error) {
      console.error('解析用户信息失败:', error)
      localStorage.removeItem('currentUser')
      localStorage.removeItem('userPermissions')
      localStorage.removeItem('userRoleDescription')
      removeToken()
      isLoggedIn.value = false
      currentUser.value = null
      userPermissions.value = new Set()
      return false
    }
  }

  isLoggedIn.value = false
  currentUser.value = null
  userPermissions.value = new Set()
  return false
}

export const login = async (username: string, password: string) => {
  try {
    const data = await post('/api/auth/login', { username, password }, { skipAuth: true })

    if (data.success && data.user) {
      localStorage.removeItem('currentUser')
      removeToken()

      window.dispatchEvent(new CustomEvent('auth-logout'))

      localStorage.setItem('currentUser', JSON.stringify(data.user))
      if (data.token) {
        setToken(data.token)
      }
      isLoggedIn.value = true
      currentUser.value = data.user

      await loadUserPermissions()
      await loadUserRoleDescription()

      window.dispatchEvent(new CustomEvent('auth-login'))

      await loadNotifications()

      return data.user
    } else {
      throw new Error('登录失败')
    }
  } catch (error) {
    console.error('登录错误:', error)
    throw error
  }
}

export const logout = async () => {
  try {
    const token = getToken()
    if (token) {
      await post('/api/auth/logout')
    }
  } catch (error) {
    console.error('登出请求失败:', error)
  } finally {
    localStorage.removeItem('currentUser')
    localStorage.removeItem('userPermissions')
    localStorage.removeItem('userRoleDescription')
    removeToken()
    isLoggedIn.value = false
    currentUser.value = null
    userPermissions.value = new Set()
    currentRoleDescription.value = '普通用户'
    resetNotifications()
    window.dispatchEvent(new CustomEvent('auth-logout'))
    router.push('/login')
  }
}

export const getCurrentUser = () => {
  return currentUser.value
}

export const updateCurrentUser = (user: User) => {
  currentUser.value = user
  localStorage.setItem('currentUser', JSON.stringify(user))
}

export const fetchCurrentUser = async () => {
  const token = getToken()
  if (!token) return

  try {
    const userData = await get('/api/user/profile')
    updateCurrentUser(userData)
    return userData
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}
