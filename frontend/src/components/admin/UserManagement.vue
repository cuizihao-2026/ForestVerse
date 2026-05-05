<template>
  <div class="user-management">
    <!-- 搜索和筛选 -->
    <div class="search-filter">
      <input 
        type="text" 
        v-model="searchQuery" 
        placeholder="搜索用户名或昵称" 
        class="search-input form-input"
      >
      <select v-model="roleFilter" class="filter-select form-input form-select">
        <option value="">所有角色</option>
        <option value="ADMIN">管理员</option>
        <option value="AUTHOR">作者</option>
        <option value="USER">普通用户</option>
      </select>
      <select v-model="statusFilter" class="filter-select form-input form-select">
        <option value="">所有状态</option>
        <option value="ACTIVE">活跃</option>
        <option value="BANNED">禁用</option>
      </select>
      <span class="user-count">共 {{ filteredUsers.length }} 人</span>
    </div>
    
    <!-- 用户列表 -->
    <div class="user-list">
      <table class="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.nickname }}</td>
            <td>{{ user.email }}</td>
            <td>
              <template v-if="user.role === 'SUPER_ADMIN'">
                <span class="super-admin-badge">{{ getRoleText(user.role) }}</span>
              </template>
              <template v-else>
                <select
                  v-model="user.role"
                  @change="updateUserRole(user.id, user.role)"
                  class="role-select form-input form-select"
                >
                  <option 
                    v-for="roleOption in getAvailableRoles()" 
                    :key="roleOption.value" 
                    :value="roleOption.value"
                  >
                    {{ roleOption.text }}
                  </option>
                </select>
              </template>
            </td>
            <td>
              <select
                v-model="user.status"
                @change="updateUserStatus(user.id, user.status)"
                class="status-select form-input form-select"
              >
                <option value="ACTIVE">活跃</option>
                <option value="BANNED">禁用</option>
              </select>
            </td>
            <td>
              <button
                class="btn btn-danger"
                @click="deleteUser(user.id)"
                :disabled="user.id === currentUserId || user.role === 'SUPER_ADMIN'"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 分页控件 -->
    <div v-if="totalPages > 1" class="pagination">
      <button 
        class="page-btn" 
        :disabled="currentPage === 1" 
        @click="goToPage(currentPage - 1)"
      >
        上一页
      </button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button 
        class="page-btn" 
        :disabled="currentPage === totalPages" 
        @click="goToPage(currentPage + 1)"
      >
        下一页
      </button>
    </div>
    
    <!-- 消息提示 -->
    <div 
      v-if="showMessage" 
      class="message" 
      :class="messageType"
    >
      <span class="message-icon">{{ messageType === 'success' ? '✓' : '✕' }}</span>
      <span class="message-text">{{ messageText }}</span>
      <button class="message-close" @click="showMessage = false">×</button>
    </div>

    <!-- 删除确认框 -->
    <div v-if="showDeleteDialog" class="delete-dialog-overlay" @click.self="cancelDelete">
      <div class="delete-dialog">
        <div class="delete-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 9V13M12 17H12.01M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="#ef5350" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h3 class="delete-title">确认删除</h3>
        <p class="delete-message">您确定要删除用户 <span class="user-name">{{ deletingUserName }}</span> 吗？</p>
        <p class="delete-warning">此操作不可撤销！</p>
        <div class="delete-buttons">
          <button class="btn btn-secondary" @click="cancelDelete">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { currentUser as currentUserStore } from '../../stores/auth';
import { get, put, del } from '../../utils/api';

const user = computed(() => currentUserStore.value);
const currentUserId = computed(() => user.value?.id || 0);
const users = ref<any[]>([]);
const isLoading = ref(false);
const showMessage = ref(false);
const messageText = ref('');
const messageType = ref<'success' | 'error'>('success');
const searchQuery = ref('');
const roleFilter = ref('');
const statusFilter = ref('');
const showDeleteDialog = ref(false);
const deletingUserId = ref<number | null>(null);
const deletingUserName = ref('');
const currentPage = ref(1);
const pageSize = 15;

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  const end = start + pageSize;
  return filteredUsers.value.slice(start, end);
});

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / pageSize));

// 当筛选条件变化时，重置到第一页
watch([searchQuery, roleFilter, statusFilter], () => {
  currentPage.value = 1;
});

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

// 获取当前用户可用的角色选项
const getAvailableRoles = () => {
  if (user.value?.role === 'SUPER_ADMIN') {
    return [
      { value: 'USER', text: '普通用户' },
      { value: 'AUTHOR', text: '作者' },
      { value: 'ADMIN', text: '管理员' }
    ];
  }
  // 管理员只能选择普通用户和作者
  return [
    { value: 'USER', text: '普通用户' },
    { value: 'AUTHOR', text: '作者' }
  ];
};

// 获取角色显示文本
const getRoleText = (role?: string) => {
  const roleMap: Record<string, string> = {
    'SUPER_ADMIN': '超级管理员',
    'ADMIN': '管理员',
    'AUTHOR': '作者',
    'USER': '普通用户'
  };
  return roleMap[role || 'USER'] || '普通用户';
};

// 过滤用户列表
const filteredUsers = computed(() => {
  let filtered = users.value;
  
  // 排除自己
  filtered = filtered.filter(u => u.id !== currentUserId.value);
  
  // 根据当前用户角色过滤
  if (user.value?.role === 'ADMIN') {
    // 管理员只能看到作者和普通用户
    filtered = filtered.filter(u => u.role === 'AUTHOR' || u.role === 'USER');
  } else if (user.value?.role === 'SUPER_ADMIN') {
    // 超级管理员可以看到所有用户（除了自己）
  }
  
  // 搜索和筛选
  return filtered.filter(user => {
    const matchesSearch = user.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
                        user.nickname.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchesRole = !roleFilter.value || user.role === roleFilter.value;
    const matchesStatus = !statusFilter.value || user.status === statusFilter.value;
    return matchesSearch && matchesRole && matchesStatus;
  });
});

// 获取用户列表
const fetchUsers = async () => {
  isLoading.value = true;
  try {
    users.value = await get('/api/user/all');
  } catch (error: any) {
    console.error('获取用户列表失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '获取用户列表失败';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } finally {
    isLoading.value = false;
  }
};

// 更新用户角色
const updateUserRole = async (userId: number, role: string) => {
  try {
    await put(`/api/user/role/${userId}`, { role });
    showMessage.value = true;
    messageText.value = '角色更新成功';
    messageType.value = 'success';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } catch (error: any) {
    console.error('更新角色失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '更新角色失败';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    // 恢复原来的角色
    const user = users.value.find(u => u.id === userId);
    if (user) {
      // 这里可以从后端重新获取用户信息，或者保存原来的角色
    }
  }
};

// 更新用户状态
const updateUserStatus = async (userId: number, status: string) => {
  try {
    await put(`/api/user/status/${userId}`, { status });
    showMessage.value = true;
    messageText.value = '状态更新成功';
    messageType.value = 'success';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } catch (error: any) {
    console.error('更新状态失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '更新状态失败';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    // 恢复原来的状态
    const user = users.value.find(u => u.id === userId);
    if (user) {
      // 这里可以从后端重新获取用户信息，或者保存原来的状态
    }
  }
};

// 删除用户 - 显示确认框
const deleteUser = (userId: number) => {
  const targetUser = users.value.find(u => u.id === userId);
  deletingUserId.value = userId;
  deletingUserName.value = targetUser?.nickname || targetUser?.username || '该用户';
  showDeleteDialog.value = true;
};

// 确认删除
const confirmDelete = async () => {
  if (deletingUserId.value === null) return;
  
  try {
    await del(`/api/user/${deletingUserId.value}`);
    // 从列表中移除删除的用户
    users.value = users.value.filter(user => user.id !== deletingUserId.value);
    showMessage.value = true;
    messageText.value = '用户删除成功';
    messageType.value = 'success';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } catch (error: any) {
    console.error('删除用户失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '删除用户失败';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } finally {
    showDeleteDialog.value = false;
    deletingUserId.value = null;
    deletingUserName.value = '';
  }
};

// 取消删除
const cancelDelete = () => {
  showDeleteDialog.value = false;
  deletingUserId.value = null;
  deletingUserName.value = '';
};

// 组件挂载时获取用户列表
onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.user-management {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-3xl);
  box-shadow: var(--shadow);
  width: 100%;
  font-family: var(--sans);
}



.search-filter {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
  align-items: center;
}

.search-input {
  flex: 1 1 200px;
  min-width: 0;
}

.filter-select {
  flex: 0 0 140px;
  width: 140px;
}

.user-count {
  font-size: 13px;
  color: var(--text-m);
  white-space: nowrap;
}

.user-list {
  overflow-x: auto;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: var(--spacing);
  text-align: left;
  border-bottom: 1px solid var(--border);
}

.user-table th {
  background: var(--bg-alt);
  font-weight: 600;
  color: var(--text-h);
  font-size: 16px;
}

.user-table td {
  font-size: 15px;
  color: var(--text);
}

.role-select,
.status-select {
  min-width: 100px;
}

.super-admin-badge {
  display: inline-block;
  padding: var(--spacing-xs) var(--spacing-sm);
  background: linear-gradient(135deg, #ffd700 0%, #ffb347 100%);
  color: #8B4513;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
}

/* 消息提示样式 */
.message {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing) var(--spacing-lg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  position: fixed;
  top: var(--spacing-lg);
  right: var(--spacing-lg);
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
}

.message.success {
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
  color: white;
}

.message.error {
  background: var(--danger-gradient);
  color: white;
}

.message-content {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
}

.message-close {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 删除确认框样式 */
.delete-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease-out;
}

.delete-dialog {
  background: var(--bg);
  border-radius: var(--radius-xl);
  padding: var(--spacing-3xl);
  max-width: 420px;
  width: 90%;
  text-align: center;
  box-shadow: var(--shadow-xl);
  animation: scaleIn 0.3s ease-out;
}

.delete-icon {
  margin-bottom: var(--spacing-xl);
  animation: shake 0.5s ease-in-out;
}

.delete-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-h);
  margin-bottom: var(--spacing);
}

.delete-message {
  font-size: 16px;
  color: var(--text);
  margin-bottom: var(--spacing-xs);
  line-height: 1.6;
}

.user-name {
  font-weight: 600;
  color: var(--danger);
}

.delete-warning {
  font-size: 14px;
  color: var(--warning);
  margin-bottom: var(--spacing-2xl);
}

.delete-buttons {
  display: flex;
  gap: var(--spacing);
  justify-content: center;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-management {
    padding: 20px;
  }
  
  .user-management h2 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .search-filter {
    flex-direction: column;
    flex-wrap: wrap;
  }
  
  .search-input {
    min-width: auto;
  }
  
  .user-table th,
  .user-table td {
    padding: 12px;
    font-size: 14px;
  }
  
  .role-select,
  .status-select {
    padding: 6px 10px;
    font-size: 13px;
  }
  
  .btn {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .user-count {
    width: 100%;
    text-align: right;
    margin-top: 8px;
  }
}
</style>