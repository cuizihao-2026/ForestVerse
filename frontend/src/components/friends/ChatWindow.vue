<script setup lang="ts">
import { ref, watch, nextTick, toRefs, onMounted, onUnmounted, computed } from 'vue'
import { sendChatMessage, markAsRead, type ChatMessage, updateFriendGroup, updateFriendRemark, deleteFriend, getFriends, getConversation, uploadChatImage } from '../../utils/friends'
import { currentUser } from '../../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps<{
  chat?: any
}>()

const emit = defineEmits<{
  (e: 'send', message: ChatMessage): void
}>()

const { chat } = toRefs(props)

const newMessage = ref('')
const isUploading = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const showMenu = ref(false)

// 判断是否是图片链接
const isImageMessage = (content: string): boolean => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp']
  const lowerContent = content.toLowerCase()
  return imageExtensions.some(ext => lowerContent.endsWith(ext)) || lowerContent.includes('/uploads/chat-images/')
}

// 切换菜单显示
const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

// 选择图片
const selectImage = () => {
  showMenu.value = false
  fileInputRef.value?.click()
}

// 点击外部关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.image-btn') && !target.closest('.menu-container')) {
    showMenu.value = false
  }
}

// 组件挂载时添加点击事件监听
onMounted(() => {
  nextTick(() => {
    autoResize()
  })
  document.addEventListener('click', handleClickOutside)
  loadFriends()
})

// 组件卸载时移除监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})



// 处理文件选择
const handleFileSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  
  if (!file) return
  
  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg、png、gif、webp 格式的图片')
    return
  }
  
  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }
  
  await uploadAndSendImage(file)
  
  // 清空 input
  input.value = ''
}

// 上传并发送图片
const uploadAndSendImage = async (file: File) => {
  if (!chat.value) return
  
  isUploading.value = true
  
  try {
    const imageUrl = await uploadChatImage(file)
    // 发送图片消息
    const res = await sendChatMessage(chat.value.id, imageUrl)
    if (res.success) {
      emit('send', res.data)
      scrollToBottom()
    }
  } catch (err) {
    ElMessage.error('发送图片失败')
    console.error(err)
  } finally {
    isUploading.value = false
  }
}
const messagesEndRef = ref<HTMLDivElement>()
const textareaRef = ref<HTMLTextAreaElement>()
const chatMessagesRef = ref<HTMLDivElement>()

// 分页相关
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)
const loadingMore = ref(false)
const isLoadingHistory = ref(false) // 标记是否正在加载历史消息
const isLoadingInitial = ref(false) // 标记是否正在初始加载聊天

// 图片过期处理
const expiredImages = ref<Set<number>>(new Set())

const handleImageError = (messageId: number) => {
  expiredImages.value.add(messageId)
}

// 好友信息侧边栏
const showFriendInfo = ref(false)
const selectedFriend = ref<any>(null)
const editRemark = ref('')
const editGroupName = ref('我的好友')
const isEditingRemark = ref(false)
const isEditingGroup = ref(false)
const friendsList = ref<any[]>([])

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

const autoResize = () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    const maxHeight = 140 // 最大高度，与CSS中的max-height一致
    const newHeight = Math.min(textareaRef.value.scrollHeight, maxHeight)
    textareaRef.value.style.height = newHeight + 'px'
  }
}

const avatarInitial = (name: string) => {
  return name.charAt(0).toUpperCase();
};

// 判断是否需要显示时间分隔
const shouldShowTime = (msg: any, index: number | string) => {
  const numIndex = Number(index);
  if (numIndex === 0) return true;
  
  const prevMsg = chat.value?.messages[numIndex - 1];
  if (!prevMsg) return true;
  
  const currDate = new Date(msg.createdAt);
  const prevDate = new Date(prevMsg.createdAt);
  
  // 间隔超过30分钟或者不是同一天
  const timeDiff = currDate.getTime() - prevDate.getTime();
  const isSameDay = currDate.toDateString() === prevDate.toDateString();
  
  return timeDiff > 30 * 60 * 1000 || !isSameDay;
};

const formatMessageTime = (timeStr: any) => {
  try {
    let date: Date
    if (typeof timeStr === 'string') {
      // 尝试解析 yyyy-MM-dd HH:mm:ss 格式
      if (timeStr.includes(' ')) {
        const [datePart, timePart] = timeStr.split(' ')
        const [year, month, day] = datePart.split('-')
        const [hour, minute, second] = timePart ? timePart.split(':') : ['0', '0', '0']
        date = new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second || 0))
      } else {
        date = new Date(timeStr)
      }
    } else if (timeStr instanceof Date) {
      date = timeStr
    } else {
      return '刚刚'
    }
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      return '刚刚'
    }
    
    const now = new Date()
    
    // 今天只显示时间
    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
    }
    // 昨天
    const yesterday = new Date(now)
    yesterday.setDate(yesterday.getDate() - 1)
    if (date.toDateString() === yesterday.toDateString()) {
      return '昨天'
    }
    // 今年只显示月日
    if (date.getFullYear() === now.getFullYear()) {
      return (date.getMonth() + 1) + '-' + date.getDate()
    }
    // 其他显示完整日期
    return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate()
  } catch {
    return '刚刚'
  }
}

const scrollToBottom = () => {
  return new Promise<void>((resolve) => {
    nextTick(() => {
      messagesEndRef.value?.scrollIntoView({ behavior: 'smooth' })
      // 等待动画完成
      setTimeout(resolve, 100)
    })
  })
}

// 加载更多消息
const loadMoreMessages = async () => {
  if (!chat.value || !hasMore.value || loadingMore.value) return

  loadingMore.value = true
  isLoadingHistory.value = true // 标记正在加载历史消息
  
  // 保存当前的完整滚动信息
  const container = chatMessagesRef.value
  if (!container) return
  
  const oldScrollHeight = container.scrollHeight
  const oldScrollTop = container.scrollTop
  
  try {
    const res = await getConversation(chat.value.id, page.value + 1, pageSize)
    if (res?.data && res.data.length > 0) {
      page.value++
      // 将新消息添加到现有消息前面
      chat.value.messages = [...res.data, ...chat.value.messages]
      
      // 检查是否还有更多
      if (res.data.length < pageSize) {
        hasMore.value = false
      }
      
      // 使用 requestAnimationFrame 确保滚动更丝滑
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          if (chatMessagesRef.value) {
            const newScrollHeight = chatMessagesRef.value.scrollHeight
            // 精确计算滚动位置，让用户感觉不到跳动
            chatMessagesRef.value.scrollTop = newScrollHeight - oldScrollHeight + oldScrollTop
          }
        })
      })
    } else {
      hasMore.value = false
    }
  } catch (err) {
    console.error('加载更多消息失败:', err)
    ElMessage.error('加载消息失败，请重试')
  } finally {
    loadingMore.value = false
    // 延迟重置标志，确保watch不会在数据更新时触发scrollToBottom
    setTimeout(() => {
      isLoadingHistory.value = false
    }, 500)
  }
}

// 处理滚动
const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  // 滚动到顶部时加载更多
  if (target.scrollTop === 0 && hasMore.value && !loadingMore.value) {
    loadMoreMessages()
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey) {
    // 仅按 Enter：发送
    e.preventDefault()
    sendMessage()
    return
  }
  if (e.key === 'Enter' && e.ctrlKey) {
    // Ctrl+Enter：换行
    return
  }
}

const sendMessage = async () => {
  if (!chat.value || !newMessage.value.trim()) return
  if (newMessage.value.length > 1000) {
    ElMessage.warning('消息长度不能超过1000字')
    return
  }
  try {
    const res = await sendChatMessage(chat.value.id, newMessage.value.trim())
    if (res.success) {
      emit('send', res.data)
      newMessage.value = ''
      scrollToBottom()
      // 重置textarea高度
      if (textareaRef.value) {
        textareaRef.value.style.height = 'auto'
      }
    }
  } catch (err) {
    ElMessage.error('发送失败')
  }
}

// 监听消息变化，自动调整高度
watch(newMessage, () => {
  nextTick(() => {
    autoResize()
  })
})

// 监听chat变化，滚动到底部（但加载历史消息时除外）
watch(() => chat.value?.messages?.length, () => {
  if (!isLoadingHistory.value) {
    scrollToBottom()
  }
}, { immediate: true })

// 标记消息为已读
watch(() => chat.value?.id, async (newId) => {
  if (newId) {
    try {
      await markAsRead(newId)
    } catch (err) {
      console.error('标记已读失败', err)
    }
  }
}, { immediate: true })

// 组件挂载时初始化
onMounted(() => {
  nextTick(() => {
    autoResize()
  })
  // 加载好友列表用于分组选择
  loadFriends()
})

// 加载好友列表
const loadFriends = async () => {
  try {
    const res = await getFriends()
    if (res.success) {
      friendsList.value = res.data
    }
  } catch (err) {
    console.error('加载好友失败', err)
  }
}

// 打开好友信息
const openFriendInfo = () => {
  if (!chat.value) return
  // 查找好友详细信息
  const friendDetail = friendsList.value.find(f => f.id === chat.value?.id)
  selectedFriend.value = friendDetail || chat.value
  editRemark.value = selectedFriend.value.remark || ''
  editGroupName.value = selectedFriend.value.groupName || '我的好友'
  showFriendInfo.value = true
}

// 关闭好友信息
const closeFriendInfo = () => {
  showFriendInfo.value = false
  selectedFriend.value = null
}

// 修改备注
const saveRemark = async () => {
  if (!selectedFriend.value) return
  try {
    await updateFriendRemark(selectedFriend.value.id, editRemark.value)
    selectedFriend.value.remark = editRemark.value
    ElMessage.success('备注已更新')
    isEditingRemark.value = false
    // 更新聊天显示
    if (chat.value && chat.value.id === selectedFriend.value.id) {
      chat.value.nickname = editRemark.value || selectedFriend.value.nickname
    }
  } catch (err) {
    console.error('更新备注失败:', err)
    ElMessage.error('更新备注失败')
  }
}

// 修改分组
const saveGroup = async () => {
  if (!selectedFriend.value) return
  
  try {
    await updateFriendGroup(selectedFriend.value.id, editGroupName.value)
    selectedFriend.value.groupName = editGroupName.value
    ElMessage.success('分组已更新')
    isEditingGroup.value = false
    await loadFriends()
  } catch (err) {
    console.error('更新分组失败:', err)
    ElMessage.error('更新分组失败')
  }
}

// 删除好友
const handleDeleteFriend = async () => {
  if (!selectedFriend.value) return
  try {
    await ElMessageBox.confirm(
      `确定要删除好友 ${selectedFriend.value.nickname} 吗？`,
      '删除好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteFriend(selectedFriend.value.id)
    ElMessage.success('已删除好友')
    closeFriendInfo()
    await loadFriends()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除好友失败:', err)
      ElMessage.error('删除好友失败')
    }
  }
}

// 监听聊天变化，重置分页
watch(() => chat.value?.id, async (newId) => {
  if (newId) {
    page.value = 1
    hasMore.value = true
    isLoadingInitial.value = true
    
    const startTime = Date.now()
    const minDelay = 300
    
    // 检查是否需要加载聊天记录
    if (!chat.value.messages || chat.value.messages.length === 0) {
      try {
        const res = await getConversation(newId, 1, 20)
        if (res.success) {
          chat.value.messages = res.data
        }
      } catch (err) {
        console.error('加载聊天记录失败:', err)
      }
    }
    
    // 等待下一个 tick 让消息渲染完成
    await nextTick()
    
    // 滚动到底部并等待完成
    await scrollToBottom()
    
    // 确保至少显示300ms的加载动画，防止闪烁
    const elapsed = Date.now() - startTime
    if (elapsed < minDelay) {
      await new Promise(resolve => setTimeout(resolve, minDelay - elapsed))
    }
    
    isLoadingInitial.value = false
  }
  loadFriends()
})
</script>

<template>
  <div class="content-area">
    <div v-if="!chat" class="empty-content">
      <div class="empty-content-icon">💬</div>
      <p>选择一个聊天开始对话</p>
    </div>
    <div v-else class="chat-window">
      <div class="chat-window-header">
        <div class="chat-user-info">
          <div class="chat-user-avatar-wrapper">
            <img 
              v-if="chat.avatar" 
              :src="chat.avatar" 
              :alt="chat.nickname"
              class="chat-user-avatar"
            />
            <div v-else class="chat-user-avatar fallback">
              {{ avatarInitial(chat.nickname) }}
            </div>
          </div>
          <div class="chat-user-detail">
            <h4>{{ chat.nickname }}</h4>
          </div>
        </div>
        <div class="chat-window-actions">
          <button class="action-btn" @click="openFriendInfo">👤</button>
        </div>
      </div>

      <div class="chat-messages" ref="chatMessagesRef" @scroll="handleScroll">
          <!-- 初始加载遮罩 -->
          <div v-if="isLoadingInitial" class="initial-loading-overlay">
            <div class="initial-loading-spinner"></div>
            <span>加载中...</span>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="loadingMore" class="load-more-hint">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="!hasMore && chat.messages && chat.messages.length > 0" class="load-more-hint">没有更多消息了</div>
          
          <template v-for="(msg, index) in chat.messages" :key="msg.id">
            <div 
              v-if="shouldShowTime(msg, index)" 
              class="message-time-divider"
            >
              {{ formatMessageTime(msg.createdAt) }}
            </div>
            <div class="message-item" :class="msg.senderId === currentUser?.id ? 'me' : 'other'">
              <div class="message-avatar">
                <template v-if="msg.senderId !== currentUser?.id">
                  <img 
                    v-if="chat.avatar" 
                    :src="chat.avatar" 
                    :alt="chat.nickname"
                    class="message-avatar-inner"
                  />
                  <div v-else class="message-avatar-inner fallback">
                    {{ avatarInitial(chat.nickname) }}
                  </div>
                </template>
                <template v-else>
                  <img 
                    v-if="currentUser?.avatar" 
                    :src="currentUser.avatar" 
                    :alt="currentUser.nickname"
                    class="message-avatar-inner"
                  />
                  <div v-else class="message-avatar-inner fallback me">
                    {{ currentUser?.nickname ? avatarInitial(currentUser.nickname) : '我' }}
                  </div>
                </template>
              </div>
              <div class="message-content">
                <div class="message-bubble" v-if="!isImageMessage(msg.content)">
                  {{ msg.content }}
                </div>
                <div class="message-image-bubble" v-else>
                  <img 
                    v-if="!expiredImages.has(msg.id)" 
                    :src="msg.content" 
                    class="chat-image" 
                    alt="聊天图片" 
                    @error="handleImageError(msg.id)"
                  />
                  <div v-else class="expired-image-placeholder">
                    <span class="expired-icon">🗑️</span>
                    <span class="expired-text">图片已过期</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div ref="messagesEndRef"></div>
        </div>

      <div class="chat-input-area">
        <div class="chat-input-wrapper">
          <div class="input-row">
            <div class="menu-wrapper">
              <button class="image-btn" @click="toggleMenu" :disabled="isUploading">
                <span v-if="isUploading">⏳</span>
                <span v-else>➕</span>
              </button>
              <!-- 弹出菜单 -->
              <div v-if="showMenu" class="menu-container">
                <div class="menu-item" @click="selectImage">
                  <span class="menu-icon">📷</span>
                  <span class="menu-text">图片</span>
                </div>
              </div>
            </div>
            <input 
              type="file" 
              ref="fileInputRef" 
              style="display: none" 
              accept="image/jpeg,image/png,image/gif,image/webp"
              @change="handleFileSelect"
            />
            <div class="input-container">
              <textarea 
                ref="textareaRef"
                v-model="newMessage"
                class="chat-input"
                placeholder="输入消息... (Enter发送)"
                rows="1"
                maxlength="1000"
                @keydown="handleKeydown"
              ></textarea>
              <span class="char-count" :class="{ limit: newMessage.length > 950 }">
                {{ newMessage.length }}/1000
              </span>
            </div>
            <button class="send-btn" @click="sendMessage" :disabled="isUploading">发送</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 好友信息侧边栏 -->
  <div class="friend-info-overlay" v-if="showFriendInfo" @click.self="closeFriendInfo">
    <div class="friend-info-panel">
      <div class="friend-info-header">
        <button class="btn-close" @click="closeFriendInfo">←</button>
        <h3>好友信息</h3>
      </div>
      <div class="friend-info-body" v-if="selectedFriend">
        <div class="friend-avatar-section">
          <img 
            v-if="selectedFriend.avatar" 
            :src="selectedFriend.avatar" 
            :alt="selectedFriend.nickname"
            class="friend-avatar-large"
          />
          <div v-else class="friend-avatar-large fallback">
            {{ avatarInitial(selectedFriend.nickname) }}
          </div>
        </div>

        <div class="friend-info-section">
          <div class="info-item">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ selectedFriend.nickname }}</span>
          </div>

          <div class="info-item editable">
            <span class="info-label">备注</span>
            <div class="editable-value">
              <input 
                v-if="isEditingRemark" 
                v-model="editRemark"
                class="info-input"
                placeholder="设置备注"
                @blur="saveRemark"
                @keyup.enter="saveRemark"
              />
              <span v-else @click="isEditingRemark = true" class="info-value">
                {{ editRemark || '未设置' }}
                <span class="edit-icon">✎</span>
              </span>
            </div>
          </div>

          <div class="info-item editable">
            <span class="info-label">分组</span>
            <div class="editable-value">
              <select 
                v-if="isEditingGroup"
                v-model="editGroupName"
                class="info-input"
                @change="saveGroup"
                @blur="isEditingGroup = false"
              >
                <option v-for="group in groups" :key="group.id" :value="group.id">
                  {{ group.name }}
                </option>
              </select>
              <span v-else @click="isEditingGroup = true" class="info-value">
                {{ groups.find(g => g.id === editGroupName)?.name || '我的好友' }}
                <span class="edit-icon">✎</span>
              </span>
            </div>
          </div>

          <div class="info-item">
            <span class="info-label">简介</span>
            <span class="info-value">{{ selectedFriend.bio || '暂无简介' }}</span>
          </div>
        </div>

        <div class="friend-actions">
          <button class="btn-delete" @click="handleDeleteFriend">删除好友</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.empty-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #868686;
}

.empty-content-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  overflow: hidden;
}

.chat-window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e5e5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-user-avatar-wrapper {
  position: relative;
}

.chat-user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.chat-user-avatar.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}

.chat-user-detail h4 {
  margin: 0;
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.chat-user-detail p {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #868686;
}

.chat-window-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  font-size: 18px;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f0f0f0;
  transform: scale(1.05);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f5f5f5;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
  position: relative;
}

.chat-messages::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message-item.other {
  flex-direction: row;
}

.message-item.me {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  margin: 0 10px;
}

.message-avatar-inner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.message-avatar-inner.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
}

.message-avatar-inner.fallback.me {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 65%;
}

.message-item.other .message-content {
  align-items: flex-start;
}

.message-item.me .message-content {
  align-items: flex-end;
}

.message-bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.6;
  word-wrap: normal;
  word-break: normal;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  hyphens: auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: transform 0.1s ease;
  max-width: 100%;
  min-width: 80px;
  box-sizing: border-box;
  text-align: left;
}

.message-bubble:hover {
  transform: translateY(-1px);
}

.message-item.other .message-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-item.me .message-bubble {
  background: #95ec69;
  color: #333;
  border-bottom-right-radius: 4px;
}

.message-time-divider {
  text-align: center;
  font-size: 11px;
  color: #b5b5b5;
  margin: 10px 0;
  padding: 4px 0;
}

.chat-input-area {
  background: #fff;
  padding: 16px 24px;
  border-top: 1px solid #e5e5e5;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.chat-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.menu-wrapper {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-btn {
  padding: 0 16px;
  border: none;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  min-height: 44px;
  max-height: 44px;
  flex-shrink: 0;
  box-sizing: border-box;
  line-height: 44px;
}

.menu-container {
  position: absolute;
  bottom: 52px;
  left: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  min-width: 120px;
  z-index: 100;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:hover {
  background: #f5f5f5;
}

.menu-icon {
  font-size: 20px;
}

.menu-text {
  font-size: 14px;
  color: #333;
}

.image-btn:hover:not(:disabled) {
  background: #e0e0e0;
  transform: scale(1.05);
}

.image-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.message-image-bubble {
  display: inline-block;
  border-radius: 12px;
  overflow: hidden;
  max-width: 300px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.chat-image {
  display: block;
  width: 100%;
  height: auto;
  max-height: 300px;
  object-fit: contain;
  cursor: pointer;
}

.expired-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 150px;
  background: #f5f5f5;
  border-radius: 12px;
  gap: 8px;
}

.expired-icon {
  font-size: 32px;
}

.expired-text {
  font-size: 13px;
  color: #999;
}

.input-container {
  flex: 1;
  position: relative;
}

.chat-input {
  width: 100%;
  padding: 11px 60px 11px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  font-size: 15px;
  resize: none;
  outline: none;
  max-height: 140px;
  min-height: 44px;
  height: 44px;
  font-family: inherit;
  transition: all 0.2s;
  line-height: 22px;
  background: #fafafa;
  box-sizing: border-box;
}

.chat-input:focus {
  border-color: #12b7f5;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(18, 183, 245, 0.1);
}

.char-count {
  position: absolute;
  right: 16px;
  bottom: 12px;
  font-size: 12px;
  color: #b5b5b5;
}

.char-count.limit {
  color: #f44336;
}

.send-btn {
  padding: 0 24px;
  background: #12b7f5;
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  min-height: 44px;
  max-height: 44px;
  flex-shrink: 0;
  box-sizing: border-box;
  line-height: 44px;
}

.send-btn:hover {
  background: #108ee9;
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(18, 183, 245, 0.3);
}

.send-btn:active {
  transform: translateY(0);
}

.send-btn:disabled {
  background: #b5b5b5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #b5b5b5;
}

.load-more-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  font-size: 13px;
  color: #868686;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f0f0f0;
  border-top: 2px solid #07c160;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.initial-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 10;
  font-size: 14px;
  color: #868686;
}

.initial-loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e0e0e0;
  border-top: 3px solid #07c160;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.friend-info-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}

.friend-info-panel {
  width: 360px;
  background: #fff;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

.friend-info-header {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e5e5;
  gap: 12px;
}

.friend-info-header h3 {
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

.friend-info-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.friend-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.friend-avatar-large {
  width: 100px;
  height: 100px;
  border-radius: 16px;
  object-fit: cover;
  margin-bottom: 16px;
}

.friend-avatar-large.fallback {
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 600;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #868686;
}

.online-status.online {
  color: #52c41a;
}

.online-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d0d0d0;
}

.online-status.online .status-dot {
  background: #52c41a;
}

.friend-info-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 24px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  width: 60px;
  flex-shrink: 0;
  font-size: 14px;
  color: #868686;
}

.info-value {
  flex: 1;
  font-size: 14px;
  color: #333;
  word-wrap: break-word;
}

.editable-value {
  flex: 1;
  display: flex;
  align-items: center;
}

.editable-value .info-value {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.editable-value .info-value:hover {
  background: #f0f0f0;
}

.edit-icon {
  font-size: 12px;
  color: #868686;
  opacity: 0;
  transition: opacity 0.2s;
}

.editable-value .info-value:hover .edit-icon {
  opacity: 1;
}

.info-input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.info-input:focus {
  border-color: #12b7f5;
}

.friend-actions {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.btn-delete {
  width: 100%;
  padding: 12px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete:hover {
  background: #ff7875;
  transform: translateY(-1px);
}
</style>
