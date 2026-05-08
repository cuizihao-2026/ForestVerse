<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { isLoggedIn as isLoggedInStore, currentUser as currentUserStore, logout, checkLoginStatus } from '../../stores/auth'
import { hasPermission } from '../../stores/permission'
import { siteTitle, isConfigLoaded, fullSiteIconUrl, loadSiteConfig as loadSiteConfigFromStore } from '../../stores/siteConfig'

const router = useRouter()
const route = useRoute()
const isMenuOpen = ref(false)
const isDropdownOpen = ref(false)

onMounted(() => {
  // 组件挂载时再次检查登录状态，确保最新
  checkLoginStatus()
  // 如果配置还没加载，重新加载一次
  if (!isConfigLoaded.value) {
    loadSiteConfigFromStore()
  }
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
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

const toggleDropdown = (e: Event) => {
  e.stopPropagation()
  isDropdownOpen.value = !isDropdownOpen.value
}

const closeDropdown = () => {
  isDropdownOpen.value = false
}

const handleClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.user-menu')) {
    closeDropdown()
  }
}
</script>

<template>
  <nav class="navbar">
    <div class="navbar-container">
      <div class="navbar-left">
        <div class="navbar-brand" role="button" tabindex="0" @click="goToHome" @keydown.enter="goToHome" aria-label="返回首页">
        <div class="brand-icon-wrapper" :class="{ 'loaded': isConfigLoaded }">
          <img v-if="fullSiteIconUrl" :src="fullSiteIconUrl" :alt="siteTitle" class="brand-icon-image" width="40" height="40" />
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
            <div class="user-menu">
              <button 
                class="user-button"
                @click="toggleDropdown"
                aria-label="用户菜单"
              >
                <div class="user-avatar-wrapper">
                  <img 
                    v-if="currentUser?.avatar" 
                    :src="currentUser.avatar" 
                    :alt="currentUser?.nickname || '用户'"
                    class="avatar large border"
                  />
                  <div v-else class="avatar large border fallback">
                    {{ avatarInitial }}
                  </div>
                </div>
                <span class="dropdown-arrow" :class="{ 'is-open': isDropdownOpen }"></span>
              </button>
              <div 
                class="user-dropdown"
                :class="{ 'is-open': isDropdownOpen }"
              >
                <button class="dropdown-item" @click="goToProfile">
                  <span class="item-text">个人中心</span>
                </button>
                <template v-if="hasPermission('admin.use')">
                  <button class="dropdown-item admin-btn" @click="goToAdmin">
                    <span class="item-text">管理面板</span>
                  </button>
                </template>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item logout-btn" @click="logout">
                  <span class="item-text">退出登录</span>
                </button>
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

  .navbar-brand {
    white-space: nowrap;
    padding: 8px 8px;
  }

  .brand-text {
    font-size: 18px;
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
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }

  .nav-link {
    padding: 9px 16px;
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
  }

  .user-button {
    justify-content: center;
    padding: 8px 12px;
  }

  .user-avatar {
    width: 42px;
    height: 42px;
    font-size: 18px;
  }

  .user-dropdown {
    position: static;
    background: #f8fafc;
    border-radius: 12px;
    margin-top: 12px;
    box-shadow: none;
    opacity: 0;
    max-height: 0;
    transform: none;
    pointer-events: none;
    border: 1px solid #f1f5f9;
    overflow: hidden;
    transition: all 0.3s ease;
  }

  .user-dropdown.is-open {
    opacity: 1;
    max-height: 300px;
    pointer-events: auto;
  }

  .dropdown-item {
    justify-content: center;
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
}

.user-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px 6px 6px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.user-button:hover {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.user-button:active {
  transform: scale(0.98);
}

.user-avatar-wrapper {
  display: flex;
  align-items: center;
}

.dropdown-arrow {
  display: block;
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 6px solid #64748b;
  transition: all 0.2s ease;
  margin-left: 4px;
}

.dropdown-arrow.is-open {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  transform: translateY(-8px);
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);
  min-width: 200px;
  margin-top: 10px;
  z-index: 1001;
  opacity: 0;
  pointer-events: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  padding: 6px;
}

.user-dropdown.is-open {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
  transition: all 0.15s ease;
  font-weight: 500;
  border-radius: 8px;
}

.dropdown-item:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.dropdown-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 4px 0;
}

.dropdown-item.logout-btn {
  color: #ef4444;
}

.dropdown-item.logout-btn:hover {
  background: #fef2f2;
  color: #dc2626;
}
</style>