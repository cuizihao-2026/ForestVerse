<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { isLoggedIn as isLoggedInStore, currentUser as currentUserStore, logout, checkLoginStatus } from '../../stores/auth'
import { hasPermission } from '../../stores/permission'
import { siteTitle, isConfigLoaded, fullSiteIconUrl, loadSiteConfig as loadSiteConfigFromStore } from '../../stores/siteConfig'

const router = useRouter()
const route = useRoute()
const isMenuOpen = ref(false)
const isDropdownOpen = ref(false)
let dropdownTimer: ReturnType<typeof setTimeout> | null = null

onMounted(() => {
  // 组件挂载时再次检查登录状态，确保最新
  checkLoginStatus()
  // 如果配置还没加载，重新加载一次
  if (!isConfigLoaded.value) {
    loadSiteConfigFromStore()
  }
})

const isLoggedIn = computed(() => isLoggedInStore.value)
const currentUser = computed(() => currentUserStore.value)
const isHomePage = computed(() => route.path === '/home')
const isFriendsPage = computed(() => route.path === '/friends')

const avatarInitial = computed(() => {
  if (!currentUser.value) return '?'
  const name = currentUser.value.nickname || currentUser.value.username || '?'
  return name.charAt(0).toUpperCase()
})

const goToHome = () => {
  router.push('/home')
}

const goToFriends = () => {
  router.push('/friends')
}

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const goToProfile = () => {
  router.push('/personal')
  closeDropdown()
}

const goToAdmin = () => {
  router.push('/admin')
  closeDropdown()
}

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const openDropdown = () => {
  if (dropdownTimer) {
    clearTimeout(dropdownTimer)
    dropdownTimer = null
  }
  isDropdownOpen.value = true
}

const closeDropdown = () => {
  if (dropdownTimer) {
    clearTimeout(dropdownTimer)
  }
  dropdownTimer = setTimeout(() => {
    isDropdownOpen.value = false
  }, 300) // 300毫秒延迟
}
</script>

<template>
  <nav class="navbar">
    <div class="navbar-container">
      <div class="navbar-left">
        <div class="navbar-brand" @click="goToHome">
          <div class="brand-icon-wrapper" :class="{ 'loaded': isConfigLoaded }">
            <img v-if="fullSiteIconUrl" :src="fullSiteIconUrl" :alt="siteTitle" class="brand-icon-image" />
          </div>
          <span v-if="siteTitle" class="brand-text">{{ siteTitle }}</span>
        </div>
        <ul class="nav-list">
          <li class="nav-item">
            <button class="nav-link" :class="{ active: isHomePage }" @click="goToHome">首页</button>
          </li>
          <li class="nav-item">
            <button class="nav-link" :class="{ active: isFriendsPage }" @click="goToFriends">好友</button>
          </li>
        </ul>
      </div>

      <button class="menu-toggle" @click="toggleMenu">
        <span class="hamburger"></span>
      </button>

      <div class="navbar-menu" :class="{ 'is-open': isMenuOpen }">
        <div class="navbar-auth">
          <template v-if="isLoggedIn">
            <div 
              class="user-menu"
              @mouseenter="openDropdown"
              @mouseleave="closeDropdown"
            >
              <div class="user-avatar-wrapper">
                <img 
                  v-if="currentUser?.avatar" 
                  :src="currentUser.avatar" 
                  :alt="currentUser?.nickname || '用户'"
                  class="user-avatar"
                />
                <div v-else class="user-avatar fallback">
                  {{ avatarInitial }}
                </div>
                <div 
                  class="user-dropdown"
                  :class="{ 'is-open': isDropdownOpen }"
                >
                  <button class="dropdown-item" @click="goToProfile">个人中心</button>
                  <template v-if="hasPermission('admin.use')">
                    <button class="dropdown-item admin-btn" @click="goToAdmin">管理面板</button>
                  </template>
                  <button class="dropdown-item logout-btn" @click="logout">退出登录</button>
                </div>
              </div>
            </div>
          </template>
          <template v-else>
            <button class="auth-btn login-btn" @click="goToLogin">登录</button>
            <button class="auth-btn register-btn" @click="goToRegister">注册</button>
          </template>
        </div>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 68px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 3px solid #e2e8f0;
  z-index: 1000;
  font-family: var(--sans);
}

.navbar-container {
  width: 100%;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-sizing: border-box;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 8px 12px;
  border-radius: 10px;
}

.navbar-brand:hover {
  background: rgba(0, 0, 0, 0.04);
}

.brand-icon-wrapper {
  opacity: 0;
  transition: opacity 0.3s ease;
}

.brand-icon-wrapper.loaded {
  opacity: 1;
}

.brand-icon-image {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.brand-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.3px;
}

.menu-toggle {
  display: none;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  cursor: pointer;
  padding: 10px;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.menu-toggle:hover {
  background: rgba(0, 0, 0, 0.08);
}

.hamburger {
  display: block;
  width: 22px;
  height: 2px;
  background: #374151;
  position: relative;
  transition: all 0.2s ease;
  border-radius: 2px;
}

.hamburger::before,
.hamburger::after {
  content: '';
  position: absolute;
  width: 22px;
  height: 2px;
  background: #374151;
  transition: all 0.2s ease;
  border-radius: 2px;
}

.hamburger::before {
  top: -7px;
}

.hamburger::after {
  top: 7px;
}

.navbar-menu {
  display: flex;
  align-items: center;
  gap: 28px;
}

.nav-list {
  display: flex;
  flex-direction: row;
  list-style: none;
  gap: 8px;
  margin: 0;
  padding: 0;
}

.nav-item {
  position: relative;
}

.nav-link,
.auth-btn {
  color: #475569;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  padding: 9px 20px;
  border-radius: 10px;
  transition: all 0.2s ease;
  background: #f8fafc;
  border: 1px solid #d1d5db;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
  transform: translateY(0);
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.02);
}

.nav-link:hover,
.auth-btn:hover {
  color: #1e293b;
  border-color: #9ca3af;
  background: #f9fafb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.nav-link.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.navbar-auth {
  display: flex;
  gap: 10px;
  align-items: center;
}



@media (max-width: 768px) {
  .menu-toggle {
    display: block;
  }

  .navbar-menu {
    position: fixed;
    top: 68px;
    left: 0;
    right: 0;
    background: #fff;
    flex-direction: column;
    padding: 20px;
    gap: 20px;
    transform: translateY(-100%);
    opacity: 0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
    border-bottom: 1px solid #f1f5f9;
  }

  .navbar-menu.is-open {
    transform: translateY(0);
    opacity: 1;
    pointer-events: auto;
  }

  .nav-list {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
    width: 100%;
  }

  .nav-link {
    width: 100%;
    text-align: center;
    justify-content: center;
  }

  .navbar-auth {
    flex-direction: column;
    width: 100%;
    gap: 8px;
  }

  .auth-btn {
    width: 100%;
  }

  .user-menu {
    position: relative;
    width: 100%;
    display: flex;
    justify-content: center;
  }

  .user-avatar-wrapper {
    margin: 8px 0;
  }

  .user-avatar {
    width: 52px;
    height: 52px;
    font-size: 22px;
  }

  .user-dropdown {
    position: static;
    background: #f8fafc;
    border-radius: 10px;
    margin-top: 12px;
    box-shadow: none;
    opacity: 1;
    transform: none;
    pointer-events: auto;
    border: 1px solid #f1f5f9;
  }

  .dropdown-item {
    width: 100%;
    text-align: center;
    color: #475569;
  }

  .dropdown-item:hover {
    background: #f1f5f9;
  }
}

/* 用户菜单样式 */
.user-menu {
  position: relative;
  cursor: pointer;
}

.user-avatar-wrapper {
  position: relative;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.user-avatar-wrapper:hover {
  background: rgba(0, 0, 0, 0.04);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
  transition: all 0.2s ease;
}

.user-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.user-avatar:hover {
  border-color: #cbd5e1;
}

.user-dropdown {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(-8px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  min-width: 170px;
  margin-top: 8px;
  z-index: 1001;
  opacity: 0;
  pointer-events: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  border: 1px solid #f1f5f9;
}

.user-dropdown.is-open {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
  pointer-events: auto;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 11px 18px;
  text-align: left;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
  transition: all 0.15s ease;
  font-weight: 500;
}

.dropdown-item:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.dropdown-item.logout-btn {
  color: #ef4444;
  border-top: 1px solid #f1f5f9;
}

.dropdown-item.admin-btn {
  color: #667eea;
}

.dropdown-item.admin-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.dropdown-item.logout-btn:hover {
  background: #fef2f2;
  color: #dc2626;
}
</style>