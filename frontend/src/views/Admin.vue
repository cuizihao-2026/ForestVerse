<template>
  <div class="admin-container">
    <div class="admin-body">
      <!-- 侧边导航 -->
      <AdminSidebar :current-page="currentPage" @change-page="handlePageChange" />
      
      <!-- 内容区域 -->
      <div class="admin-content">
        <div v-if="currentPage === 'dashboard' && canAccessDashboard" class="content-section">
          <Dashboard />
        </div>
        
        <div v-else-if="currentPage === 'users' && canManageUsers" class="content-section">
          <UserManagement />
        </div>
        
        <div v-else-if="currentPage === 'roles' && canManageRoles" class="content-section">
          <RoleManagement />
        </div>
        
        <div v-else-if="currentPage === 'content' && canManageArticles" class="content-section">
          <ContentManagement />
        </div>
        
        <div v-else-if="currentPage === 'attachments' && canManageFiles" class="content-section">
          <AttachmentCenter />
        </div>
        
        <div v-else-if="currentPage === 'settings' && canManageSettings" class="content-section">
          <WebsiteSettings />
        </div>
        
        <div v-else-if="currentPage === 'audit' && canManageReviews" class="content-section">
          <AuditCenter />
        </div>
        
        <div v-else-if="currentPage === 'backup' && canManageBackups" class="content-section">
          <BackupCenter />
        </div>
        
        <div v-else class="content-section no-permission">
          <div class="no-permission-icon">🔒</div>
          <h3>您没有权限访问此页面</h3>
          <p>请联系管理员获取相应权限</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import AdminSidebar from '../components/admin/AdminSidebar.vue';
import UserManagement from '../components/admin/UserManagement.vue';
import ContentManagement from '../components/admin/ContentManagement.vue';
import WebsiteSettings from '../components/admin/WebsiteSettings.vue';
import Dashboard from '../components/admin/Dashboard.vue';
import AttachmentCenter from '../components/admin/AttachmentCenter.vue';
import AuditCenter from '../components/admin/AuditCenter.vue';
import RoleManagement from '../components/admin/RoleManagement.vue';
import BackupCenter from '../components/admin/BackupCenter.vue';
import { fetchCurrentUser } from '../stores/auth';
import { hasPermission } from '../stores/permission';

const router = useRouter();
const currentPage = ref('dashboard');

const canAccessDashboard = computed(() => hasPermission('console.use'));
const canManageUsers = computed(() => hasPermission('user:manage') || hasPermission('user.supermanage'));
const canManageRoles = computed(() => hasPermission('role:manage'));
const canManageArticles = computed(() => hasPermission('article:manage'));
const canManageFiles = computed(() => hasPermission('file:manage'));
const canManageReviews = computed(() => hasPermission('review:manage'));
const canManageSettings = computed(() => hasPermission('site:manage'));
const canManageBackups = computed(() => hasPermission('backup.manage'));

const handlePageChange = (page: string) => {
  if (page === 'dashboard' && !canAccessDashboard.value) {
    return;
  }
  if (page === 'users' && !canManageUsers.value) {
    return;
  }
  if (page === 'roles' && !canManageRoles.value) {
    return;
  }
  if (page === 'content' && !canManageArticles.value) {
    return;
  }
  if (page === 'attachments' && !canManageFiles.value) {
    return;
  }
  if (page === 'audit' && !canManageReviews.value) {
    return;
  }
  if (page === 'settings' && !canManageSettings.value) {
    return;
  }
  if (page === 'backup' && !canManageBackups.value) {
    return;
  }
  currentPage.value = page;
};

onMounted(async () => {
  // 先获取用户信息
  await fetchCurrentUser();
  
  // 检查是否有 admin.use 权限
  if (!hasPermission('admin.use')) {
    router.push('/home');
    return;
  }
  
  // 检查用户是否有权限访问任何页面，如果没有，跳转到首页
  let hasAccessToAnyPage = canAccessDashboard.value || 
                          canManageUsers.value || 
                          canManageRoles.value || 
                          canManageArticles.value || 
                          canManageFiles.value || 
                          canManageReviews.value || 
                          canManageSettings.value || 
                          canManageBackups.value;
  
  if (!hasAccessToAnyPage) {
    router.push('/home');
    return;
  }
  
  // 如果当前页面是受限页面但用户没有对应权限，自动切换到第一个有权限的页面
  if (currentPage.value === 'dashboard' && !canAccessDashboard.value) {
    if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageSettings.value) currentPage.value = 'settings';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'users' && !canManageUsers.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageSettings.value) currentPage.value = 'settings';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'roles' && !canManageRoles.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageSettings.value) currentPage.value = 'settings';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'content' && !canManageArticles.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageSettings.value) currentPage.value = 'settings';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'attachments' && !canManageFiles.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageSettings.value) currentPage.value = 'settings';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'audit' && !canManageReviews.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageSettings.value) currentPage.value = 'settings';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'settings' && !canManageSettings.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageBackups.value) currentPage.value = 'backup';
  }
  if (currentPage.value === 'backup' && !canManageBackups.value) {
    if (canAccessDashboard.value) currentPage.value = 'dashboard';
    else if (canManageUsers.value) currentPage.value = 'users';
    else if (canManageRoles.value) currentPage.value = 'roles';
    else if (canManageArticles.value) currentPage.value = 'content';
    else if (canManageFiles.value) currentPage.value = 'attachments';
    else if (canManageReviews.value) currentPage.value = 'audit';
    else if (canManageSettings.value) currentPage.value = 'settings';
  }
});
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0;
  font-family: 'Georgia', serif;
  margin: 0;
  overflow: hidden;
}

.admin-body {
  display: flex;
  height: 100vh;
  background: white;
  overflow: hidden;
  animation: fadeIn 1s ease-out 0.4s both;
}

.admin-content {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
  background: white;
  height: 100%;
}

.content-section {
  animation: fadeInUp 0.6s ease-out;
  height: 100%;
}

.no-permission {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 20px;
}

.no-permission-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.no-permission h3 {
  font-size: 24px;
  color: #374151;
  margin-bottom: 10px;
}

.no-permission p {
  font-size: 16px;
  color: #6b7280;
}

/* 动画 */
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

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-body {
    flex-direction: column;
    height: 100vh;
  }
  
  .admin-content {
    padding: 20px;
  }
}
</style>