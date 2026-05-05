import { ref } from 'vue'
import { get } from '../utils/api'

interface Notification {
  id: number
  userId: number
  type: string
  title: string
  message: string
  read: boolean
  createdAt: string
}

export const notifications = ref<Notification[]>([])
export const unreadCount = ref(0)
export const notificationsLoaded = ref(false)

export async function loadNotifications() {
  try {
    const data = await get('/api/notification/list')
    if (Array.isArray(data)) {
      notifications.value = data as Notification[]
      unreadCount.value = data.filter((n: any) => !n.read).length
      notificationsLoaded.value = true
    }
  } catch (error) {
    console.error('加载通知失败:', error)
  }
}

export async function refreshNotifications() {
  await loadNotifications()
}

export function resetNotifications() {
  notifications.value = []
  unreadCount.value = 0
  notificationsLoaded.value = false
}
