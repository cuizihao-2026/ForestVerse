<template>
  <div class="personal-center">
    <div class="personal-container">
      <!-- 左侧导航 -->
      <PersonalCenterNav
        :active-tab="currentTab"
        @change-tab="handleTabChange"
      />

      <!-- 右侧内容 -->
      <div class="content">
        <div v-if="currentTab === 'profile'" class="content-section">
          <ProfileEditor />
        </div>
        <div v-else-if="currentTab === 'articles'" class="content-section">
          <MyArticles />
        </div>
        <div v-else-if="currentTab === 'likes'" class="content-section">
          <MyLikes />
        </div>
        <div v-else-if="currentTab === 'notifications'" class="content-section">
          <Notifications />
        </div>
        <div v-else-if="currentTab === 'settings'" class="content-section">
          <AccountSettings />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PersonalCenterNav from '../components/personal/PersonalCenterNav.vue'
import ProfileEditor from '../components/personal/ProfileEditor.vue'
import MyArticles from '../components/personal/MyArticles.vue'
import MyLikes from '../components/personal/MyLikes.vue'
import Notifications from '../components/personal/Notifications.vue'
import AccountSettings from '../components/personal/AccountSettings.vue'
import { fetchCurrentUser } from '../stores/auth'
import { hasPermission, loadUserPermissions } from '../stores/permission'

const activeTab = ref('profile')

// 如果当前激活的是 articles 但没有权限，自动切换到 profile
const currentTab = computed(() => {
  if (activeTab.value === 'articles' && !hasPermission('article:create')) {
    return 'profile'
  }
  return activeTab.value
})

onMounted(async () => {
  // 获取用户信息
  await fetchCurrentUser()
  // 加载权限
  await loadUserPermissions()
})

const handleTabChange = (tab: string) => {
  // 如果尝试切换到 articles 但没有权限，不允许
  if (tab === 'articles' && !hasPermission('article:create')) {
    return
  }
  activeTab.value = tab
}
</script>

<style scoped>
.personal-center {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0;
  font-family: var(--sans);
  margin: 0;
  overflow: hidden;
}

.personal-container {
  display: flex;
  height: 100vh;
  background: white;
  overflow: hidden;
  animation: fadeIn 1s ease-out 0.4s both;
}

.content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  scrollbar-width: none;
  background: white;
}

.content::-webkit-scrollbar {
  width: 0;
}

.content-section {
  animation: fadeInUp 0.6s ease-out;
  height: 100%;
  display: flex;
  flex-direction: column;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .personal-container {
    flex-direction: column;
    height: 100vh;
  }

  .content {
    padding: 20px;
  }
}
</style>