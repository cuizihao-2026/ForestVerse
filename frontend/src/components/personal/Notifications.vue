<template>
  <div class="notifications">
    <div class="notifications-header">
      <h2>信息通知</h2>
      <div class="header-buttons">
        <button v-if="notifications.some(n => n.read)" class="btn btn-secondary" @click="deleteReadNotifications">
          删除已读
        </button>
        <button v-if="notifications.some(n => !n.read)" class="btn btn-primary" @click="markAllAsRead">
          全部标记已读
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="notifications.length === 0" class="empty-state">
      <div class="empty-icon">🔔</div>
      <h3>暂无通知</h3>
      <p>目前没有任何通知</p>
    </div>

    <div v-else class="notifications-list">
      <div 
        v-for="notification in notifications" 
        :key="notification.id" 
        class="notification-item card"
        :class="{ unread: !notification.read }"
      >
        <div class="notification-content-wrapper" @click="handleNotificationClick(notification)">
          <div class="notification-icon" :class="notification.type">
            {{ getIcon(notification.type) }}
          </div>
          <div class="notification-content">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-message">{{ notification.message }}</div>
            <div class="notification-time">{{ formatDate(notification.createdAt) }}</div>
          </div>
        </div>
        <button class="delete-btn" @click.stop="deleteNotification(notification.id)">
          <el-icon><Delete /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { put, del } from '../../utils/api'
import { notifications as notificationsStore, unreadCount, loadNotifications as loadNotificationsFromStore } from '../../stores/notification'

interface Notification {
  id: number
  userId: number
  type: string
  title: string
  message: string
  read: boolean
  createdAt: string
}

const notifications = notificationsStore
const loading = ref(false)

const getIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    'success': '✅',
    'warning': '⚠️',
    'error': '❌',
    'info': 'ℹ️'
  }
  return iconMap[type] || '📌'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return ''
  }
}

const loadNotifications = async () => {
  loading.value = true
  try {
    await loadNotificationsFromStore()
  } catch (error) {
    console.error('加载通知失败:', error)
  } finally {
    loading.value = false
  }
}

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.read) {
    try {
      await put(`/api/notification/${notification.id}/read`)
      notification.read = true
      // 更新未读计数
      unreadCount.value = notifications.value.filter(n => !n.read).length
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

const markAllAsRead = async () => {
  try {
    await put('/api/notification/read-all')
    notifications.value.forEach(n => n.read = true)
    unreadCount.value = 0
  } catch (error) {
    console.error('全部标记失败:', error)
  }
}

const deleteReadNotifications = async () => {
  try {
    await del('/api/notification/delete-read')
    // 只保留未读通知
    notifications.value = notifications.value.filter(n => !n.read)
  } catch (error) {
    console.error('删除已读失败:', error)
  }
}

const deleteNotification = async (id: number) => {
  try {
    await del(`/api/notification/${id}`)
    // 移除删除的通知
    const index = notifications.value.findIndex(n => n.id === id)
    if (index !== -1) {
      const wasUnread = !notifications.value[index].read
      notifications.value.splice(index, 1)
      // 如果删除的是未读通知，更新未读计数
      if (wasUnread) {
        unreadCount.value = notifications.value.filter(n => !n.read).length
      }
    }
  } catch (error) {
    console.error('删除通知失败:', error)
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notifications {
  width: 100%;
}

.notifications-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.notifications-header h2 {
  font-size: 24px;
  color: var(--text-h);
  margin: 0;
  font-weight: 600;
}

.header-buttons {
  display: flex;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-content-wrapper {
  display: flex;
  gap: 16px;
  flex: 1;
  cursor: pointer;
}

.notification-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.delete-btn {
  background: none;
  border: none;
  color: var(--text-l);
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-btn:hover {
  color: var(--danger);
  background: rgba(239, 68, 68, 0.1);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border);
  border-top: 3px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-l);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 8px;
  color: var(--text-h);
  font-size: 18px;
}

.empty-state p {
  margin: 0;
  color: var(--text-m);
  font-size: 14px;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.notification-item.unread {
  border-left: 4px solid var(--primary);
  background: linear-gradient(to right, rgba(79, 209, 197, 0.05), transparent);
}

.notification-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.notification-icon.success {
  background: #dcfce7;
}

.notification-icon.warning {
  background: #fef3c7;
}

.notification-icon.error {
  background: #fee2e2;
}

.notification-icon.info {
  background: #dbeafe;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-h);
  margin-bottom: 6px;
}

.notification-message {
  font-size: 14px;
  color: var(--text-m);
  margin-bottom: 8px;
  line-height: 1.5;
}

.notification-time {
  font-size: 12px;
  color: var(--text-l);
}
</style>
