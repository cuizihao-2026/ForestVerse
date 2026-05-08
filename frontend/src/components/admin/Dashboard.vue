<template>
  <div class="dashboard">
    <div v-if="viewMode === 'stats'" class="stats-view">
      <DashboardStats 
        :stats="stats" 
        :serverStats="serverStats"
        @viewOnline="showOnlineUsersList"
      />
    </div>
    <div v-else class="user-list-view">
      <div class="view-header">
        <button class="back-btn" @click="hideUserList" aria-label="返回仪表盘">
          <span class="back-icon" aria-hidden="true">←</span>
          返回
        </button>
        <h1>在线用户</h1>
        <div class="header-spacer"></div>
      </div>
      
      <div class="search-container">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索在线用户..." 
          class="search-input"
        />
        <span class="search-count">共 {{ filteredUsers.length }} 人</span>
      </div>
      
      <div v-if="displayUsers.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>{{ searchQuery ? '未找到匹配的用户' : '暂无在线用户' }}</p>
      </div>
      <div v-else class="user-list">
        <div v-for="user in displayUsers" :key="user.id" class="user-item">
          <img v-if="user.avatar" :src="user.avatar" class="user-avatar" />
          <div v-else class="user-avatar-placeholder">
            {{ (user.nickname || user.username || 'U').charAt(0).toUpperCase() }}
          </div>
          <div class="user-details">
            <div class="user-nickname">{{ user.nickname || user.username }}</div>
            <div class="user-username">@{{ user.username }}</div>
          </div>
          <div class="status-badge">
            <span class="status-dot"></span>在线
          </div>
        </div>
      </div>
      
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import DashboardStats from './DashboardStats.vue';
import { get } from '../../utils/api';

const stats = ref({
  publishedArticles: 0,
  pendingCount: 0,
  totalUsers: 0,
  onlineUsers: 0
});
const serverStats = ref({
  cpuPercent: 15,
  memoryPercent: 50,
  usedMemory: 4096,
  totalMemory: 8192,
  heapMemoryPercent: 30,
  heapMemoryUsed: 150,
  heapMemoryMax: 512,
  availableProcessors: 4,
  systemLoad: 1.5
});
const viewMode = ref<'stats' | 'online'>('stats');
const onlineUsers = ref<any[]>([]);
const searchQuery = ref('');
const currentPage = ref(1);
const pageSize = 15;

const filteredUsers = computed(() => {
  const users = onlineUsers.value;
  if (!searchQuery.value.trim()) {
    return users;
  }
  const query = searchQuery.value.toLowerCase();
  return users.filter(user => 
    (user.username && user.username.toLowerCase().includes(query)) ||
    (user.nickname && user.nickname.toLowerCase().includes(query))
  );
});

const displayUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  const end = start + pageSize;
  return filteredUsers.value.slice(start, end);
});

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / pageSize));

let refreshInterval: number | null = null;

const fetchStats = async () => {
  try {
    const response = await get('/api/monitor/stats');
    const data = response.data || response;
    stats.value = {
      publishedArticles: data.publishedArticles || 0,
      pendingCount: data.pendingCount || 0,
      totalUsers: data.totalUsers || 0,
      onlineUsers: data.onlineUsers || 0
    };
  } catch (error) {
    console.error('获取统计数据失败:', error);
  }
};

const fetchServerStats = async () => {
  try {
    const response = await get('/api/monitor/server');
    const data = response.data || response;
    serverStats.value = {
      cpuPercent: data.cpuPercent ?? 0,
      memoryPercent: data.memoryPercent ?? 0,
      usedMemory: data.usedMemory ?? 0,
      totalMemory: data.totalMemory ?? 0,
      heapMemoryPercent: data.heapMemoryPercent ?? 0,
      heapMemoryUsed: data.heapMemoryUsed ?? 0,
      heapMemoryMax: data.heapMemoryMax ?? 0,
      availableProcessors: data.availableProcessors ?? 0,
      systemLoad: data.systemLoad ?? -1
    };
  } catch (error) {
    console.error('获取服务器资源失败:', error);
  }
};

const startRefreshTimer = () => {
  stopRefreshTimer();
  refreshInterval = window.setInterval(() => {
    fetchStats();
    fetchServerStats();
  }, 10000);
};

const stopRefreshTimer = () => {
  if (refreshInterval !== null) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
};

const showOnlineUsersList = async () => {
  try {
    const response = await get('/api/monitor/online');
    onlineUsers.value = response.data || response;
  } catch (error) {
    console.error('获取在线用户失败:', error);
  }
  currentPage.value = 1;
  searchQuery.value = '';
  viewMode.value = 'online';
};

// 当搜索内容变化时，重置到第一页
watch(searchQuery, () => {
  currentPage.value = 1;
});

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

const hideUserList = () => {
  viewMode.value = 'stats';
};

const setupWebSocketListener = () => {
  const handleOnlineCountChange = (event: CustomEvent) => {
    stats.value.onlineUsers = event.detail;
  };
  window.addEventListener('online-count-changed', handleOnlineCountChange as EventListener);
  return () => {
    window.removeEventListener('online-count-changed', handleOnlineCountChange as EventListener);
  };
};

onMounted(() => {
  fetchStats();
  fetchServerStats();
  startRefreshTimer();
  const cleanupWs = setupWebSocketListener();
  onUnmounted(() => {
    stopRefreshTimer();
    cleanupWs();
  });
});
</script>

<style scoped>
.dashboard {
  width: 100%;
  height: 100%;
  color: var(--text);
  font-family: var(--sans);
}

.user-list-view {
  width: 100%;
  animation: fadeIn var(--transition);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  cursor: pointer;
  transition: all var(--transition);
}

.back-btn:hover {
  background: var(--bg-alt);
  border-color: var(--border-light);
}

.back-icon {
  font-size: 16px;
  font-weight: bold;
}

.user-list-view h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--text-h);
}

.header-spacer {
  width: 70px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-count {
  font-size: 14px;
  color: #9ca3af;
  white-space: nowrap;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.user-item {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md) var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  transition: all var(--transition);
}

.user-item:hover {
  background: var(--bg-alt);
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.user-avatar-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-nickname {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-username {
  font-size: 12px;
  color: var(--text-m);
}

.status-badge {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 12px;
  color: var(--success);
  font-weight: 500;
  flex-shrink: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: var(--success);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-m);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: var(--bg-alt);
  border-color: var(--border-light);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-m);
}

@media (max-width: 768px) {
  .user-list-view h1 {
    font-size: 20px;
  }
}
</style>
