<template>
  <div class="online-users">
    <div class="view-header">
      <button class="back-btn" @click="handleBack">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1>在线用户</h1>
      <div class="header-spacer"></div>
    </div>
    <div v-if="onlineUsers.length === 0" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无在线用户</p>
    </div>
    <div v-else class="user-list">
      <div v-for="user in onlineUsers" :key="user.id" class="user-item">
        <img v-if="user.avatar" :src="user.avatar" class="user-avatar" />
        <div v-else class="user-avatar-placeholder">
          {{ (user.nickname || user.username || 'U').charAt(0).toUpperCase() }}
        </div>
        <div class="user-details">
          <div class="user-nickname">{{ user.nickname || user.username }}</div>
          <div class="user-username">@{{ user.username }}</div>
        </div>
        <div class="status-badge">
          <span class="status-dot"></span>
          在线
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  onlineUsers: Array<{
    id: number;
    username: string;
    nickname?: string;
    avatar?: string;
  }>;
}>();

const emit = defineEmits<{
  (e: 'back'): void;
}>();

const handleBack = () => {
  emit('back');
};
</script>

<style scoped>
.online-users {
  width: 100%;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.back-icon {
  font-size: 16px;
  font-weight: bold;
}

.online-users h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: #111827;
}

.header-spacer {
  width: 70px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.2s ease;
}

.user-item:hover {
  background: #f9fafb;
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
  background: #3b82f6;
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
  color: #1f2937;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-username {
  font-size: 12px;
  color: #9ca3af;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
  flex-shrink: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 768px) {
  .online-users h1 {
    font-size: 20px;
  }
}
</style>
