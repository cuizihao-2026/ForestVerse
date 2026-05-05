<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { currentUser } from '../../stores/auth'
import { onlineUserIds } from '../../stores/websocket'
import { searchUsers, sendFriendRequest, acceptFriendRequest, rejectFriendRequest, getReceivedRequests, getSentRequests, getFriendsWithChatInfo } from '../../utils/friends'
import type { User, FriendRequest, ChatMessage, FriendWithChatInfo } from '../../utils/friends'
import { ElMessage } from 'element-plus'

const router = useRouter()

const props = defineProps<{
  modelValue?: any
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: any): void
}>()

const searchQuery = ref('')
const activeTab = ref('chats')
const expandedGroups = ref<string[]>(['myFriends'])
const searching = ref(false)
const searchResults = ref<User[]>([])
const searchFriendQuery = ref('')
const searchFriendResults = ref<User[]>([])
const friendRequests = ref<FriendRequest[]>([])
const sentRequests = ref<FriendRequest[]>([])
const requestTab = ref<'received' | 'sent'>('received')
const friendsList = ref<User[]>([])
const friendsWithChatInfo = ref<FriendWithChatInfo[]>([])
const showRequestModal = ref(false)
const showSearchUserModal = ref(false)
const selectedUser = ref<User | null>(null)
const requestMessage = ref('')
const searchUserQuery = ref('')
let searchTimer: any = null
const showFullMessageModal = ref(false)
const fullMessage = ref('')

// 常用分组
const commonGroups = [
  { id: '我的好友', name: '我的好友' },
  { id: '家人', name: '家人' },
  { id: '同学', name: '同学' },
  { id: '同事', name: '同事' },
  { id: '陌生人', name: '陌生人' }
]

// 分组数据 - 常用分组 + 用户实际使用的分组
const groups = computed(() => {
  // 收集用户实际使用的分组
  const usedGroupSet = new Set<string>()
  friendsList.value.forEach((friend: any) => {
    if (friend.groupName) {
      usedGroupSet.add(friend.groupName)
    }
  })
  
  // 先添加常用分组
  const result = [...commonGroups]
  
  // 再添加用户实际使用但不在常用列表中的分组
  usedGroupSet.forEach(groupName => {
    if (!commonGroups.find(g => g.id === groupName)) {
      result.push({ id: groupName, name: groupName })
    }
  })
  
  return result
})

// 聊天记录
const chatsData = ref<any[]>([])

// 加载好友请求
const loadFriendRequests = async () => {
  try {
    const [receivedRes, sentRes] = await Promise.all([
      getReceivedRequests(),
      getSentRequests()
    ])
    if (receivedRes.success) {
      friendRequests.value = receivedRes.data
    }
    if (sentRes.success) {
      sentRequests.value = sentRes.data
    }
  } catch (err) {
    console.error('加载好友请求失败:', err)
  }
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const res = await getFriendsWithChatInfo()
    if (res?.data) {
      friendsWithChatInfo.value = res.data
      friendsList.value = res.data.map((info: FriendWithChatInfo) => info.friend)
      // 更新在线状态
      updateOnlineStatus()
      // 使用批量数据更新聊天列表
      updateChatsListFromBatch()
    }
  } catch (err) {
    console.error('加载好友列表失败:', err)
  }
}

// 更新在线状态
const updateOnlineStatus = () => {
  // 更新好友列表的在线状态
  for (const friend of friendsList.value) {
    friend.status = onlineUserIds.value.has(friend.id) ? 'online' : 'offline'
  }
  // 更新聊天列表的在线状态
  for (const chat of chatsData.value) {
    chat.status = onlineUserIds.value.has(chat.id) ? 'online' : 'offline'
  }
}

// 判断是否是图片链接
const isImageMessage = (content: string): boolean => {
  if (!content) return false
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp']
  const lowerContent = content.toLowerCase()
  return imageExtensions.some(ext => lowerContent.endsWith(ext)) || lowerContent.includes('/uploads/chat-images/')
}

// 更新聊天列表（使用批量数据）
const updateChatsListFromBatch = () => {
  chatsData.value = friendsWithChatInfo.value.map((info: FriendWithChatInfo) => ({
    id: info.friend.id,
    nickname: info.friend.remark || info.friend.nickname,
    avatar: info.friend.avatar,
    lastMessage: info.lastMessage && isImageMessage(info.lastMessage) ? '[图片]' : info.lastMessage,
    lastTime: info.lastMessageTime,
    lastTimestamp: info.lastTimestamp || 0,
    unread: info.unreadCount,
    status: onlineUserIds.value.has(info.friend.id) ? 'online' : 'offline',
    messages: []
  })).sort((a, b) => {
    const aTime = a.lastTimestamp || 0
    const bTime = b.lastTimestamp || 0
    return bTime - aTime
  })
}

// 发送好友请求
const handleSendRequest = async () => {
  if (!selectedUser.value) return
  try {
    const res = await sendFriendRequest(selectedUser.value.id, requestMessage.value)
    if (res.success) {
      const request = res.data
      if (request.status === 'ACCEPTED') {
        // 如果直接成为好友了
        ElMessage.success('已成为好友')
        await loadFriends()
        await loadFriendRequests()
      } else {
        ElMessage.success('好友请求已发送')
      }
      showRequestModal.value = false
      requestMessage.value = ''
      selectedUser.value = null
    }
  } catch (err: any) {
    console.error('发送失败:', err)
    ElMessage.error(err.message || '发送失败')
  }
}

// 接受好友请求
const acceptRequest = async (id: number) => {
  try {
    const res = await acceptFriendRequest(id)
    if (res.success) {
      ElMessage.success('已添加好友')
      await loadFriendRequests()
      await loadFriends()
    }
  } catch (err: any) {
    console.error('接受失败:', err)
    ElMessage.error(err.message || '操作失败')
  }
}

// 拒绝好友请求
const rejectRequest = async (id: number) => {
  try {
    const res = await rejectFriendRequest(id)
    if (res.success) {
      ElMessage.success('已拒绝')
      await loadFriendRequests()
    }
  } catch (err: any) {
    console.error('拒绝失败:', err)
    ElMessage.error(err.message || '操作失败')
  }
}

// 过滤聊天记录搜索结果
const filteredChats = computed(() => {
  let chats = chatsData.value
  
  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    chats = chats.filter(c => 
      c.nickname.toLowerCase().includes(query) || 
      c.lastMessage.toLowerCase().includes(query)
    )
  }
  
  return chats
})

// 按分组整理好友
const getFriendsByGroup = (groupId: string) => {
  return friendsList.value
    .filter(f => {
      const friendGroup = f.groupName || 'myFriends'
      // 兼容中英文分组名
      if (groupId === 'myFriends') {
        return friendGroup === 'myFriends' || friendGroup === '我的好友'
      }
      if (groupId === 'classmates') {
        return friendGroup === 'classmates' || friendGroup === '同学'
      }
      if (groupId === 'colleagues') {
        return friendGroup === 'colleagues' || friendGroup === '同事'
      }
      if (groupId === 'strangers') {
        return friendGroup === 'strangers' || friendGroup === '陌生人'
      }
      return friendGroup === groupId
    })
    .map(f => {
      // 从批量数据中获取未读数量
      const friendInfo = friendsWithChatInfo.value.find(info => info.friend.id === f.id)
      return {
        ...f,
        signature: f.bio || '',
        status: onlineUserIds.value.has(f.id) ? 'online' : 'offline',
        group: groupId,
        unread: friendInfo?.unreadCount || 0
      }
    })
}

// 切换分组展开/收起
const toggleGroup = (groupId: string) => {
  const index = expandedGroups.value.indexOf(groupId)
  if (index > -1) {
    expandedGroups.value.splice(index, 1)
  } else {
    expandedGroups.value.push(groupId)
  }
}

const avatarInitial = (name: string) => {
  return name.charAt(0).toUpperCase()
}

const startChat = async (item: any) => {
  // 先根据ID查找已有的聊天
  let chat = chatsData.value.find(c => c.id === item.id || String(c.id) === String(item.id))
  
  // 如果没有找到，创建一个新的聊天对象
  if (!chat) {
    chat = {
      id: item.id,
      nickname: item.nickname,
      avatar: item.avatar,
      lastMessage: '',
      lastTime: '刚刚',
      lastTimestamp: Date.now(),
      unread: 0,
      status: item.status || 'online',
      messages: []
    }
    // 添加到聊天列表
    chatsData.value.push(chat)
  }
  
  // 清除未读
  chat.unread = 0
  
  // 更新选中的聊天，让 ChatWindow 组件负责加载消息
  emit('update:modelValue', chat)
}

// 格式化时间
const formatTime = (timeStr: string) => {
  try {
    const date = new Date(timeStr)
    const now = new Date()
    
    // 今天
    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
    }
    // 昨天
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    if (date.toDateString() === yesterday.toDateString()) {
      return '昨天'
    }
    // 今年
    if (date.getFullYear() === now.getFullYear()) {
      return (date.getMonth() + 1) + '-' + date.getDate()
    }
    // 其他
    return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate()
  } catch {
    return timeStr
  }
}

// 显示完整验证消息
const openFullMessage = (message: string) => {
  fullMessage.value = message
  showFullMessageModal.value = true
}

// 获取状态显示文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return '等待中'
    case 'ACCEPTED': return '已接受'
    case 'REJECTED': return '已拒绝'
    default: return status
  }
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  switch (status) {
    case 'PENDING': return '#f59e0b'
    case 'ACCEPTED': return '#10b981'
    case 'REJECTED': return '#ef4444'
    default: return '#6b7280'
  }
}

// 侧边栏搜索：只搜索自己的好友
const handleSearchFriend = async () => {
  if (!searchFriendQuery.value.trim()) {
    searchFriendResults.value = []
    return
  }

  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      // 只在自己的好友列表中搜索
      const query = searchFriendQuery.value.trim().toLowerCase()
      searchFriendResults.value = friendsList.value.filter((friend: User) => 
        friend.nickname.toLowerCase().includes(query) || 
        (friend.remark && friend.remark.toLowerCase().includes(query))
      )
    } catch (err) {
      console.error('搜索失败:', err)
      ElMessage.error('搜索失败')
    } finally {
      searching.value = false
    }
  }, 300)
}

// 添加好友弹窗搜索：搜索所有用户
const handleSearchAllUsers = async () => {
  if (!searchUserQuery.value.trim()) {
    searchResults.value = []
    return
  }

  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      const res = await searchUsers(searchUserQuery.value.trim())
      if (res.success) {
        searchResults.value = res.data
      }
    } catch (err) {
      console.error('搜索失败:', err)
      ElMessage.error('搜索失败')
    } finally {
      searching.value = false
    }
  }, 300)
}

// 打开添加好友对话框
const openSearchUserModal = () => {
  showSearchUserModal.value = true
  searchUserQuery.value = ''
  searchResults.value = []
}

// 打开好友请求对话框
const openRequestModal = (user: User) => {
  selectedUser.value = user
  requestMessage.value = `你好，我是${currentUser.value?.nickname || ''}`
  showRequestModal.value = true
}

const onNewFriendRequest = async () => {
  await loadFriendRequests()
  ElMessage.info('收到新的好友请求')
}

const onFriendAccepted = async () => {
  await loadFriends()
  await loadFriendRequests()
  ElMessage.success('好友请求已被接受')
}

const onNewChatMessage = async (event: any) => {
  const message: ChatMessage = event.detail
  console.log('收到新消息:', message)
  
  // 找到对应的聊天
  let chat = chatsData.value.find(c => c.id === message.senderId || c.id === message.receiverId)
  
  if (chat) {
    // 添加消息
    chat.messages.push(message)
    // 更新最后消息和时间戳
    chat.lastMessage = isImageMessage(message.content) ? '[图片]' : message.content
    chat.lastTime = formatTime(message.createdAt)
    chat.lastTimestamp = new Date(message.createdAt).getTime()
    // 如果不是当前聊天，增加未读
    if (!props.modelValue || props.modelValue.id !== chat.id) {
      chat.unread++
      // 同时更新 friendsWithChatInfo 中的未读数
      const friendInfo = friendsWithChatInfo.value.find(info => info.friend.id === chat.id)
      if (friendInfo) {
        friendInfo.unreadCount++
      }
    }
  } else {
    // 如果没有聊天，说明是新好友，刷新好友列表
    await loadFriends()
  }
}

const onOnlineUsersChanged = () => {
  updateOnlineStatus()
}

onMounted(() => {
  loadFriendRequests()
  loadFriends()
  window.addEventListener('new-friend-request', onNewFriendRequest)
  window.addEventListener('friend-accepted', onFriendAccepted)
  window.addEventListener('new-chat-message', onNewChatMessage as EventListener)
  window.addEventListener('online-users-changed', onOnlineUsersChanged)
})

onUnmounted(() => {
  window.removeEventListener('new-friend-request', onNewFriendRequest)
  window.removeEventListener('friend-accepted', onFriendAccepted)
  window.removeEventListener('new-chat-message', onNewChatMessage as EventListener)
  window.removeEventListener('online-users-changed', onOnlineUsersChanged)
})
</script>

<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <div class="user-info" @click="router.push('/personal')">
        <div class="user-avatar-wrapper">
          <img 
            v-if="currentUser?.avatar" 
            :src="currentUser.avatar" 
            :alt="currentUser.nickname"
            class="user-avatar"
          />
          <div v-else class="user-avatar fallback">
            {{ currentUser ? avatarInitial(currentUser.nickname) : '我' }}
          </div>
        </div>
        <div class="user-detail">
          <h3>{{ currentUser?.nickname || '我的昵称' }}</h3>
        </div>
      </div>
      <button class="btn-add-icon" @click="openSearchUserModal" title="添加好友">
        ➕
      </button>
    </div>

    <div class="search-box">
      <input 
        type="text" 
        v-model="searchFriendQuery"
        placeholder="搜索好友..." 
        class="search-input"
        @input="handleSearchFriend"
      />
      <span class="search-icon">🔍</span>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results" v-if="searchFriendQuery.trim() !== ''">
      <div v-if="searching" class="search-loading">
        <p>搜索中...</p>
      </div>
      <div v-else-if="searchFriendResults.length > 0">
        <div 
          class="search-result-item" 
          v-for="user in searchFriendResults" 
          :key="user.id"
          @click="startChat(user)"
        >
          <div class="search-avatar-wrapper">
            <img v-if="user.avatar" :src="user.avatar" :alt="user.nickname" class="search-avatar" />
            <div v-else class="search-avatar fallback">{{ avatarInitial(user.nickname) }}</div>
          </div>
          <div class="search-detail">
            <h4>{{ user.remark || user.nickname }}</h4>
            <p>{{ user.bio || '' }}</p>
          </div>
        </div>
      </div>
      <div v-else class="search-empty">
        <p>未找到相关好友</p>
      </div>
    </div>

    <div class="nav-tabs">
      <button 
        class="nav-tab" 
        :class="{ active: activeTab === 'chats' }"
        @click="activeTab = 'chats'"
      >
        💬 消息
        <span v-if="chatsData.reduce((sum, c) => sum + c.unread, 0) > 0" class="tab-badge">
          {{ chatsData.reduce((sum, c) => sum + c.unread, 0) }}
        </span>
      </button>
      <button 
        class="nav-tab" 
        :class="{ active: activeTab === 'friends' }"
        @click="activeTab = 'friends'"
      >
        👥 好友
      </button>
      <button 
        class="nav-tab" 
        :class="{ active: activeTab === 'requests' }"
        @click="activeTab = 'requests'"
      >
        <span class="requests-badge" v-if="friendRequests.filter(r => r.status === 'PENDING').length > 0">{{ friendRequests.filter(r => r.status === 'PENDING').length }}</span>
        📩 验证消息
      </button>
    </div>

    <!-- 聊天记录列表 -->
    <div class="chat-list" v-if="activeTab === 'chats'">
      <div v-if="filteredChats.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <p>暂无聊天记录</p>
      </div>
      <div v-else>
        <div 
          v-for="chat in filteredChats" 
          :key="chat.id"
          class="chat-item"
          :class="{ active: modelValue?.id === chat.id }"
          @click="startChat(chat)"
        >
          <div class="chat-avatar-wrapper">
            <img 
              v-if="chat.avatar" 
              :src="chat.avatar" 
              :alt="chat.nickname"
              class="chat-avatar"
            />
            <div v-else class="chat-avatar fallback">
              {{ avatarInitial(chat.nickname) }}
            </div>
            <span v-if="chat.status === 'online'" class="status-dot small"></span>
          </div>
          <div class="chat-detail">
            <div class="chat-top">
              <h4>{{ chat.nickname }}</h4>
              <span class="chat-time">{{ chat.lastTime }}</span>
            </div>
            <div class="chat-bottom">
              <p>{{ chat.lastMessage }}</p>
              <span v-if="chat.unread > 0" class="unread-badge">{{ chat.unread }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 好友分组列表 -->
    <div class="group-list" v-if="activeTab === 'friends'">
      <div 
        v-for="group in groups" 
        :key="group.id"
        class="group-item"
      >
        <div class="group-header" @click="toggleGroup(group.id)">
          <span class="group-arrow" :class="{ expanded: expandedGroups.includes(group.id) }">▶</span>
          <span class="group-name">{{ group.name }}</span>
          <span class="group-count">{{ getFriendsByGroup(group.id).filter(f => f.status === 'online').length }}/{{ getFriendsByGroup(group.id).length }}</span>
        </div>
        <div class="group-friends" v-show="expandedGroups.includes(group.id)">
          <div 
            v-for="friend in getFriendsByGroup(group.id)" 
            :key="friend.id"
            class="friend-item"
            @click="startChat(friend)"
            @contextmenu.prevent="console.log('右键菜单')"
          >
            <div class="friend-avatar-wrapper">
              <img 
                v-if="friend.avatar" 
                :src="friend.avatar" 
                :alt="friend.nickname"
                class="friend-avatar"
              />
              <div v-else class="friend-avatar fallback">
                {{ avatarInitial(friend.nickname) }}
              </div>
              <span class="status-dot" :class="friend.status"></span>
            </div>
            <div class="friend-detail">
              <h4 :class="{ offline: friend.status === 'offline' }">
                {{ friend.remark || friend.nickname }}
              </h4>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 好友请求列表 -->
    <div class="requests-list" v-if="activeTab === 'requests'">
      <!-- 请求标签页 -->
      <div class="request-tabs">
        <div 
          class="request-tab" 
          :class="{ active: requestTab === 'received' }"
          @click="requestTab = 'received'"
        >
          收到的请求
          <span v-if="friendRequests.filter(r => r.status === 'PENDING').length > 0" class="request-badge">
            {{ friendRequests.filter(r => r.status === 'PENDING').length }}
          </span>
        </div>
        <div 
          class="request-tab" 
          :class="{ active: requestTab === 'sent' }"
          @click="requestTab = 'sent'"
        >
          发送的请求
        </div>
      </div>

      <div class="requests-content">
        <!-- 收到的请求 -->
        <div v-if="requestTab === 'received'">
          <div v-if="friendRequests.length === 0" class="empty-requests">
            <div class="empty-icon">✨</div>
            <p>暂无验证消息</p>
          </div>
          <div v-else>
            <div v-for="request in friendRequests" :key="request.id" class="request-item">
              <div class="request-avatar-wrapper">
                <img 
                  v-if="request.sender?.avatar" 
                  :src="request.sender.avatar" 
                  :alt="request.sender?.nickname"
                  class="request-avatar"
                />
                <div v-else class="request-avatar fallback">
                  {{ avatarInitial(request.sender?.nickname || '用户') }}
                </div>
              </div>
              <div class="request-detail">
                <div class="request-header">
                  <h4>{{ request.sender?.nickname || '用户' }}</h4>
                  <span class="request-status" :style="{ color: getStatusColor(request.status) }">
                    {{ getStatusText(request.status) }}
                  </span>
                </div>
                <p 
                  class="request-message" 
                  @click="openFullMessage(request.message || '想添加你为好友')"
                >
                  {{ request.message || '想添加你为好友' }}
                </p>
              </div>
              <div class="request-actions" v-if="request.status === 'PENDING'">
                <button class="btn-accept" @click="acceptRequest(request.id)">同意</button>
                <button class="btn-reject" @click="rejectRequest(request.id)">拒绝</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 发送的请求 -->
        <div v-if="requestTab === 'sent'">
          <div v-if="sentRequests.length === 0" class="empty-requests">
            <div class="empty-icon">📤</div>
            <p>暂无发送的请求</p>
          </div>
          <div v-else>
            <div v-for="request in sentRequests" :key="request.id" class="request-item">
              <div class="request-avatar-wrapper">
                <img 
                  v-if="request.receiver?.avatar" 
                  :src="request.receiver.avatar" 
                  :alt="request.receiver?.nickname"
                  class="request-avatar"
                />
                <div v-else class="request-avatar fallback">
                  {{ avatarInitial(request.receiver?.nickname || '用户') }}
                </div>
              </div>
              <div class="request-detail">
                <div class="request-header">
                  <h4>{{ request.receiver?.nickname || '用户' }}</h4>
                  <span class="request-status" :style="{ color: getStatusColor(request.status) }">
                    {{ getStatusText(request.status) }}
                  </span>
                </div>
                <p 
                  class="request-message" 
                  @click="openFullMessage(request.message || '想添加你为好友')"
                >
                  {{ request.message || '想添加你为好友' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 添加好友对话框 -->
  <div class="modal-overlay" v-if="showRequestModal" @click.self="showRequestModal = false">
    <div class="modal-content">
      <div class="modal-header">
        <h3>添加好友</h3>
        <button class="btn-close" @click="showRequestModal = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="modal-user-info">
          <div class="modal-avatar-wrapper">
            <img v-if="selectedUser?.avatar" :src="selectedUser.avatar" :alt="selectedUser.nickname" class="modal-avatar" />
            <div v-else class="modal-avatar fallback">{{ selectedUser ? avatarInitial(selectedUser.nickname) : '用' }}</div>
          </div>
          <div class="modal-user-detail">
            <h4>{{ selectedUser?.nickname }}</h4>
            <p>{{ selectedUser?.bio }}</p>
          </div>
        </div>
        <div class="modal-input-wrapper">
          <label>验证信息</label>
          <textarea v-model="requestMessage" class="modal-textarea" placeholder="请输入验证信息..." maxlength="60"></textarea>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showRequestModal = false">取消</button>
        <button class="btn-send" @click="handleSendRequest">发送</button>
      </div>
    </div>
  </div>

  <!-- 搜索用户对话框 -->
  <div class="modal-overlay" v-if="showSearchUserModal" @click.self="showSearchUserModal = false">
    <div class="modal-content search-modal">
      <div class="modal-header">
        <h3>添加好友</h3>
        <button class="btn-close" @click="showSearchUserModal = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="search-box-modal">
          <input 
            type="text" 
            v-model="searchUserQuery"
            placeholder="搜索用户名或昵称..." 
            class="search-input-modal"
            @input="handleSearchAllUsers"
          />
          <span class="search-icon-modal">🔍</span>
        </div>
        <div class="search-results-modal">
          <div v-if="searching" class="search-loading-modal">
            <p>搜索中...</p>
          </div>
          <div v-else-if="searchResults.length > 0">
            <div v-for="user in searchResults" :key="user.id" class="search-result-modal">
              <div class="search-avatar-wrapper-modal">
                <img v-if="user.avatar" :src="user.avatar" :alt="user.nickname" class="search-avatar-modal" />
                <div v-else class="search-avatar-modal fallback">{{ avatarInitial(user.nickname) }}</div>
              </div>
              <div class="search-detail-modal">
                <h4>{{ user.nickname }}</h4>
                <p>{{ user.bio || '暂无简介' }}</p>
              </div>
              <button class="btn-add-modal" @click="openRequestModal(user); showSearchUserModal = false">添加</button>
            </div>
          </div>
          <div v-else-if="searchUserQuery">
            <div class="search-empty-modal">
              <p>未找到匹配的用户</p>
            </div>
          </div>
          <div v-else class="search-hint-modal">
            <p>输入用户名或昵称进行搜索</p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 完整消息弹窗 -->
  <div class="modal-overlay" v-if="showFullMessageModal" @click.self="showFullMessageModal = false">
    <div class="modal-content full-message-modal">
      <div class="modal-header">
        <h3>验证消息</h3>
        <button class="btn-close" @click="showFullMessageModal = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="full-message-content">{{ fullMessage }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 320px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e5e5;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.btn-add-icon {
  padding: 8px 12px;
  background: #f0f0f0;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-icon:hover {
  background: #12b7f5;
  transform: scale(1.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f0f0f0;
}

.user-avatar-wrapper {
  position: relative;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  object-fit: cover;
}

.user-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.user-detail h3 {
  margin: 0;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.user-detail p {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #868686;
}

.search-box {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  position: relative;
}

.search-input {
  width: 100%;
  padding: 8px 32px 8px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  background: #f4f4f4;
  transition: all 0.2s;
}

.search-input:focus {
  background: #fff;
  border-color: #12b7f5;
}

.search-icon {
  position: absolute;
  right: 28px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  opacity: 0.5;
  cursor: pointer;
}

/* 搜索结果 */
.search-results {
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  max-height: 200px;
  overflow-y: auto;
}

.search-loading,
.search-empty {
  padding: 20px 16px;
  text-align: center;
  color: #868686;
  font-size: 13px;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.search-result-item:hover {
  background: #f0f0f0;
}

.search-avatar-wrapper {
  margin-right: 12px;
  flex-shrink: 0;
}

.search-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}

.search-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
}

.search-detail {
  flex: 1;
  min-width: 0;
}

.search-detail h4 {
  margin: 0;
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.search-detail p {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #868686;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-add-friend {
  padding: 6px 12px;
  background: #12b7f5;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-add-friend:hover {
  background: #108ee9;
}

.nav-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
}

.nav-tab {
  flex: 1;
  padding: 12px 8px;
  border: none;
  background: transparent;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.nav-tab:hover {
  background: #f0f0f0;
}

.nav-tab.active {
  background: #e5e5e5;
  font-weight: 500;
}

.tab-badge {
  background: #f44336;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.requests-badge {
  position: absolute;
  top: 6px;
  right: 12px;
  background: #f44336;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.chat-list::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #868686;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f0f0f0;
}

.chat-item:hover {
  background: #e5e5e5;
}

.chat-item.active {
  background: #e5e5e5;
}

.chat-avatar-wrapper {
  position: relative;
  margin-right: 12px;
  flex-shrink: 0;
}

.chat-avatar {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
}

.chat-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fafafa;
  background: #868686;
}

.status-dot.small {
  width: 10px;
  height: 10px;
  border-width: 2px;
  background: #10b981;
}

.status-dot.online {
  background: #10b981;
}

.chat-detail {
  flex: 1;
  min-width: 0;
}

.chat-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.chat-top h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.chat-time {
  font-size: 12px;
  color: #b5b5b5;
  flex-shrink: 0;
}

.chat-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-bottom p {
  margin: 0;
  font-size: 12px;
  color: #868686;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread-badge {
  background: #f44336;
  color: #fff;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  flex-shrink: 0;
  margin-left: 8px;
}

.group-list {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.group-list::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.group-item {
  border-bottom: 1px solid #f0f0f0;
}

.group-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.group-header:hover {
  background: #f0f0f0;
}

.group-arrow {
  font-size: 10px;
  color: #868686;
  margin-right: 8px;
  transition: transform 0.2s;
}

.group-arrow.expanded {
  transform: rotate(90deg);
}

.group-name {
  flex: 1;
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.group-count {
  font-size: 12px;
  color: #868686;
}

.group-friends {
  background: #fafafa;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 8px 16px 8px 38px;
  cursor: pointer;
  transition: background 0.15s;
}

.friend-item:hover {
  background: #e5e5e5;
}

.friend-avatar-wrapper {
  position: relative;
  margin-right: 10px;
  flex-shrink: 0;
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}

.friend-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
}

.friend-detail {
  flex: 1;
  min-width: 0;
}

.friend-detail h4 {
  margin: 0;
  font-size: 13px;
  color: #333;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-detail h4.offline {
  color: #868686;
}

.friend-detail p {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #868686;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-detail p.offline {
  color: #b5b5b5;
}

.requests-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.requests-list::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.request-tabs {
  display: flex;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e5e5;
  background: #fff;
  gap: 8px;
  flex-shrink: 0;
}

.request-tab {
  flex: 1;
  text-align: center;
  padding: 8px 12px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.request-tab:hover {
  background: #f5f5f5;
}

.request-tab.active {
  background: #12b7f5;
  color: #fff;
}

.request-badge {
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.requests-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty-requests {
  text-align: center;
  padding: 60px 20px;
  color: #868686;
}

.request-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 8px;
  border: 1px solid #e5e5e5;
}

.request-avatar-wrapper {
  margin-right: 12px;
  flex-shrink: 0;
}

.request-avatar {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
}

.request-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.request-detail {
  flex: 1;
  min-width: 0;
}

.request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.request-status {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.request-detail h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.request-detail .request-message {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #868686;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  word-break: break-word;
  cursor: pointer;
  transition: color 0.2s;
}

.request-detail .request-message:hover {
  color: #12b7f5;
}

.mutual-friends {
  color: #12b7f5 !important;
  font-size: 11px !important;
}

.request-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.btn-accept,
.btn-reject {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-accept {
  background: #12b7f5;
  color: #fff;
}

.btn-accept:hover {
  background: #108ee9;
}

.btn-reject {
  background: #f0f0f0;
  color: #333;
}

.btn-reject:hover {
  background: #e0e0e0;
}

.chat-list::-webkit-scrollbar,
.group-list::-webkit-scrollbar,
.requests-list::-webkit-scrollbar,
.search-results::-webkit-scrollbar {
  width: 6px;
}

.chat-list::-webkit-scrollbar-track,
.group-list::-webkit-scrollbar-track,
.requests-list::-webkit-scrollbar-track,
.search-results::-webkit-scrollbar-track {
  background: transparent;
}

.chat-list::-webkit-scrollbar-thumb,
.group-list::-webkit-scrollbar-thumb,
.requests-list::-webkit-scrollbar-thumb,
.search-results::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

.chat-list::-webkit-scrollbar-thumb:hover,
.group-list::-webkit-scrollbar-thumb:hover,
.requests-list::-webkit-scrollbar-thumb:hover,
.search-results::-webkit-scrollbar-thumb:hover {
  background: #b5b5b5;
}

/* 添加好友对话框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 12px;
  width: 400px;
  max-width: 90vw;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e5e5;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 18px;
  color: #868686;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 20px;
}

.modal-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.modal-avatar-wrapper {
  flex-shrink: 0;
}

.modal-avatar {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
}

.modal-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
}

.modal-user-detail h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.modal-user-detail p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #868686;
}

.modal-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.modal-input-wrapper label {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.modal-textarea {
  width: 100%;
  min-height: 80px;
  padding: 10px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.modal-textarea:focus {
  border-color: #12b7f5;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e5e5e5;
}

.btn-cancel,
.btn-send {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: #f0f0f0;
  color: #333;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.btn-send {
  background: #12b7f5;
  color: #fff;
}

.btn-send:hover {
  background: #108ee9;
}

/* 搜索用户对话框样式 */
.search-modal {
  width: 420px;
}

.search-box-modal {
  position: relative;
  margin-bottom: 16px;
}

.search-input-modal {
  width: 100%;
  padding: 10px 40px 10px 14px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.search-input-modal:focus {
  border-color: #12b7f5;
  box-shadow: 0 0 0 3px rgba(18, 183, 245, 0.1);
}

.search-icon-modal {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  opacity: 0.6;
}

.search-results-modal {
  max-height: 320px;
  overflow-y: auto;
}

.search-loading-modal,
.search-empty-modal,
.search-hint-modal {
  padding: 40px 20px;
  text-align: center;
  color: #868686;
  font-size: 14px;
}

.search-hint-modal {
  color: #b5b5b5;
}

.search-result-modal {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  transition: background 0.2s;
  margin-bottom: 8px;
}

.search-result-modal:hover {
  background: #f5f5f5;
}

.search-avatar-wrapper-modal {
  margin-right: 12px;
  flex-shrink: 0;
}

.search-avatar-modal {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
}

.search-avatar-modal.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.search-detail-modal {
  flex: 1;
  min-width: 0;
}

.search-detail-modal h4 {
  margin: 0;
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.search-detail-modal p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #868686;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-add-modal {
  padding: 8px 16px;
  background: #12b7f5;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-modal:hover {
  background: #108ee9;
  transform: translateY(-1px);
}

.full-message-modal {
  max-width: 400px;
}

.full-message-content {
  padding: 20px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  word-break: break-word;
  white-space: pre-wrap;
}

</style>
