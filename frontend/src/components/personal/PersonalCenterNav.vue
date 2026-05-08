<template>
  <div class="sidebar">
    <div class="sidebar-user-profile">
      <img v-if="user?.avatar" :src="user.avatar" alt="用户头像" class="avatar huge border">
      <div v-else class="avatar huge border fallback">
        {{ avatarInitial }}
      </div>
      <div class="sidebar-user-info">
        <h3 class="sidebar-user-name">{{ user?.nickname || user?.username }}</h3>
        <p class="sidebar-user-role">{{ currentRoleDescription }}</p>
      </div>
    </div>

    <nav class="sidebar-nav-menu">
      <div 
        v-for="item in navItems" 
        :key="item.id"
        class="sidebar-nav-item"
        :class="{ active: activeTab === item.id }"
        @click="$emit('change-tab', item.id)"
      >
        <span class="nav-text">{{ item.label }}</span>
        <span v-if="item.hasUnread" class="unread-dot"></span>
      </div>
    </nav>

    <div class="sidebar-nav-item sidebar-nav-footer-item" @click="handleGoBack">
      <span class="nav-text">返回上页</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { currentUser as currentUserStore } from '../../stores/auth'
import { hasPermission, currentRoleDescription } from '../../stores/permission'
import { loadNotifications, unreadCount, notificationsLoaded } from '../../stores/notification'

interface NavItem {
  id: string
  label: string
  icon: string
  hasUnread?: boolean
}

const props = defineProps<{
  activeTab: string
}>()

const router = useRouter()
const emit = defineEmits<{
  'change-tab': [tab: string]
}>()

const user = computed(() => currentUserStore.value)

const avatarInitial = computed(() => {
  if (!user.value) return '?'
  const name = user.value.nickname || user.value.username || '?'
  return name.charAt(0).toUpperCase()
})

const handleGoBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

const navItems = computed<NavItem[]>(() => {
  const items: NavItem[] = [
    { id: 'profile', label: '个人资料', icon: '' }
  ]
  
  // 只有有 article:create 权限的用户才显示我的文章
  if (hasPermission('article:create')) {
    items.push({ id: 'articles', label: '我的文章', icon: '' })
  }
  
  items.push(
    { id: 'likes', label: '我的喜欢', icon: '' },
    { id: 'notifications', label: '信息通知', icon: '', hasUnread: unreadCount.value > 0 },
    { id: 'settings', label: '账户设置', icon: '' }
  )
  
  return items
})



onMounted(async () => {
  if (!notificationsLoaded.value) {
    loadNotifications()
  }
})

// 监听用户变化，刷新通知
watch(user, (newUser, oldUser) => {
  if (newUser && (!oldUser || newUser.id !== oldUser.id)) {
    loadNotifications()
  }
})
</script>

<style scoped>
/* 特定组件的样式，通用样式已在common.css中 */

.sidebar-nav-item {
  position: relative;
}

.unread-dot {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>