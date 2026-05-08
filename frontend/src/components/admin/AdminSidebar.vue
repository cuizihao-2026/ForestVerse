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
        v-if="canAccessDashboard"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'dashboard' }"
        @click="$emit('change-page', 'dashboard')"
      >
        <span class="nav-text">控制台</span>
      </div>
      
      <div 
        v-if="canManageUsers"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'users' }"
        @click="$emit('change-page', 'users')"
      >
        <span class="nav-text">用户管理</span>
      </div>
      
      <div 
        v-if="canManageRoles"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'roles' }"
        @click="$emit('change-page', 'roles')"
      >
        <span class="nav-text">角色管理</span>
      </div>
      
      <div 
        v-if="canManageArticles"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'content' }"
        @click="$emit('change-page', 'content')"
      >
        <span class="nav-text">内容管理</span>
      </div>
      
      <div 
        v-if="canManageReviews"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'audit' }"
        @click="$emit('change-page', 'audit')"
      >
        <span class="nav-text">审核中心</span>
      </div>
      
      <div 
        v-if="canManageFiles"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'attachments' }"
        @click="$emit('change-page', 'attachments')"
      >
        <span class="nav-text">附件中心</span>
      </div>
      
      <div 
        v-if="canManageBackups"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'backup' }"
        @click="$emit('change-page', 'backup')"
      >
        <span class="nav-text">备份中心</span>
      </div>
      
      <div 
        v-if="canManageSettings"
        class="sidebar-nav-item" 
        :class="{ active: currentPage === 'settings' }"
        @click="$emit('change-page', 'settings')"
      >
        <span class="nav-text">设置中心</span>
      </div>
    </nav>

    <div class="sidebar-nav-item sidebar-nav-footer-item" @click="handleGoBack">
      <span class="nav-text">返回上页</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { currentUser as currentUserStore } from '../../stores/auth';
import { hasPermission, currentRoleDescription } from '../../stores/permission';

defineProps<{
  currentPage: string
}>();

const router = useRouter();
const emit = defineEmits<{
  'change-page': [page: string]
}>();

const user = computed(() => currentUserStore.value);

const canAccessDashboard = computed(() => hasPermission('console.use'));
const canManageUsers = computed(() => hasPermission('user:manage') || hasPermission('user.supermanage'));
const canManageRoles = computed(() => hasPermission('role:manage'));
const canManageArticles = computed(() => hasPermission('article:manage'));
const canManageFiles = computed(() => hasPermission('file:manage'));
const canManageReviews = computed(() => hasPermission('review:manage'));
const canManageSettings = computed(() => hasPermission('site:manage'));
const canManageBackups = computed(() => hasPermission('backup.manage'));

const avatarInitial = computed(() => {
  if (!user.value) return '?'
  const name = user.value.nickname || user.value.username || '?'
  return name.charAt(0).toUpperCase()
});

const handleGoBack = () => {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/home');
  }
};

;
</script>

<style scoped>
/* 特定组件的样式，通用样式已在common.css中 */
</style>