<template>
  <div class="feedback-management">
    <div class="feedback-layout">
      <div class="feedback-list">
        <div class="list-header">
          <h3>反馈列表</h3>
          <div class="filter-buttons">
            <button 
              class="filter-btn" 
              :class="{ active: currentFilter === 'all' }"
              @click="currentFilter = 'all'"
            >
              全部
            </button>
            <button 
              class="filter-btn" 
              :class="{ active: currentFilter === 'pending' }"
              @click="currentFilter = 'pending'"
            >
              待处理
            </button>
            <button 
              class="filter-btn" 
              :class="{ active: currentFilter === 'processing' }"
              @click="currentFilter = 'processing'"
            >
              处理中
            </button>
            <button 
              class="filter-btn" 
              :class="{ active: currentFilter === 'resolved' }"
              @click="currentFilter = 'resolved'"
            >
              已解决
            </button>
          </div>
        </div>

        <div class="feedback-items">
          <div 
            v-for="feedback in filteredFeedbacks" 
            :key="feedback.id"
            class="feedback-item"
            :class="{ active: selectedFeedback?.id === feedback.id }"
            @click="selectFeedback(feedback)"
          >
            <div class="feedback-item-top">
              <span class="feedback-type" :class="feedback.type?.toLowerCase()">
                {{ getTypeLabel(feedback.type) }}
              </span>
              <span class="feedback-status" :class="feedback.status?.toLowerCase()">
                {{ getStatusLabel(feedback.status) }}
              </span>
            </div>
            <div class="feedback-title">{{ feedback.title }}</div>
            <div class="feedback-item-bottom">
              <div class="feedback-user">
                <img 
                  v-if="feedback.userAvatar" 
                  :src="feedback.userAvatar" 
                  class="user-avatar-img"
                />
                <span v-else class="user-avatar">{{ (feedback.userNickname || '匿').charAt(0) }}</span>
                <span>{{ feedback.userNickname || '匿名用户' }}</span>
              </div>
              <div class="feedback-time">
                {{ formatDate(feedback.createdAt) }}
              </div>
            </div>
            <div v-if="feedback.id && getUnreadCount(feedback.id) > 0" class="unread-badge">
              {{ getUnreadCount(feedback.id) }}
            </div>
            <div class="status-bar" :class="feedback.status?.toLowerCase()"></div>
          </div>
        </div>
      </div>

      <div class="feedback-detail">
        <div v-if="selectedFeedback" class="detail-content">
          <div class="detail-header">
            <div>
              <h3>{{ selectedFeedback.title }}</h3>
              <div class="detail-meta">
                <span class="feedback-type" :class="selectedFeedback.type?.toLowerCase()">
                  {{ getTypeLabel(selectedFeedback.type) }}
                </span>
                <span class="feedback-status" :class="selectedFeedback.status?.toLowerCase()">
                  {{ getStatusLabel(selectedFeedback.status) }}
                </span>
                <span class="feedback-priority">
                  {{ getPriorityLabel(selectedFeedback.priority) }}
                </span>
              </div>
            </div>
            <div v-if="selectedFeedback.status?.toLowerCase() !== 'resolved' && selectedFeedback.status?.toLowerCase() !== 'closed'" class="status-actions">
              <select v-model="newStatus" class="status-select">
                <option value="pending">待处理</option>
                <option value="processing">处理中</option>
                <option value="resolved">已解决</option>
                <option value="closed">已关闭</option>
              </select>
              <button class="action-btn" @click="updateFeedbackStatus">
                更新状态
              </button>
            </div>
          </div>

          <div class="detail-info">
            <div class="info-item">
              <label>提交用户</label>
              <span>{{ selectedFeedback.userNickname || '匿名用户' }}</span>
            </div>
            <div class="info-item">
              <label>联系方式</label>
              <span>{{ selectedFeedback.contact || '未提供' }}</span>
            </div>
            <div class="info-item">
              <label>提交时间</label>
              <span>{{ formatDate(selectedFeedback.createdAt) }}</span>
            </div>
            <div class="info-item">
              <label>更新时间</label>
              <span>{{ formatDate(selectedFeedback.updatedAt) }}</span>
            </div>
          </div>

          <div class="detail-body">
            <h4>反馈内容</h4>
            <div class="feedback-content">{{ selectedFeedback.content }}</div>
          </div>

          <div class="messages-section">
            <div class="messages-list">
              <div 
                v-for="(message, index) in messages" 
                :key="message.id"
              >
                <div v-if="shouldShowTimeDivider(message, index)" class="message-time-divider">
                  {{ formatTimeDivider(message.createdAt) }}
                </div>
                <div 
                  class="message-item"
                  :class="{ other: message.senderId !== selectedFeedback?.userId }"
                >
                  <div class="message-sender-name">{{ message.senderNickname || (message.senderId !== selectedFeedback?.userId ? '管理员' : '用户') }}</div>
                  <div class="message-body-wrapper">
                    <div class="message-avatar">
                      <img 
                        v-if="message.senderAvatar" 
                        :src="message.senderAvatar" 
                        class="message-avatar-img"
                      />
                      <div v-else class="message-avatar-inner" :class="{ 'is-other': message.senderId !== selectedFeedback?.userId }">
                        {{ (message.senderNickname?.charAt(0) || (message.senderId !== selectedFeedback?.userId ? '管' : '用')) }}
                      </div>
                    </div>
                    <div class="message-content">
                      <div class="message-bubble">{{ message.content }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div ref="messagesEndRef"></div>
            </div>
            <div v-if="selectedFeedback.status?.toLowerCase() !== 'resolved' && selectedFeedback.status?.toLowerCase() !== 'closed'" class="message-input-area">
              <textarea 
                v-model="newMessage"
                class="message-input"
                placeholder="输入回复内容..."
                rows="1"
                @keydown.enter.prevent="sendReplyMessage"
              ></textarea>
              <button class="send-btn" @click="sendReplyMessage">
                发送
              </button>
            </div>
          </div>
        </div>
        <div v-else class="no-selection">
          <div class="empty-state">
            <p>选择一个反馈来查看详情</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { feedbackApi } from '../../utils/feedback'
import type { Feedback, FeedbackMessage } from '../../utils/feedback'
import { ElMessage } from 'element-plus'

const feedbacks = ref<Feedback[]>([])
const selectedFeedback = ref<Feedback | null>(null)
const messages = ref<FeedbackMessage[]>([])
const newMessage = ref('')
const newStatus = ref('')
const currentFilter = ref('all')
const unreadCounts = ref<Record<number, number>>({})
const messagesEndRef = ref<HTMLDivElement>()

const filteredFeedbacks = computed(() => {
  if (currentFilter.value === 'all') {
    return feedbacks.value
  }
  return feedbacks.value.filter(f => f.status?.toLowerCase() === currentFilter.value)
})

const loadFeedbacks = async () => {
  try {
    const data = await feedbackApi.getAllFeedbacks()
    feedbacks.value = data
  } catch (error) {
    ElMessage.error('加载反馈列表失败')
  }
}

const selectFeedback = async (feedback: Feedback) => {
  selectedFeedback.value = feedback
  newStatus.value = (feedback.status || 'pending').toLowerCase()
  
  try {
    const msgs = await feedbackApi.getMessages(feedback.id!)
    messages.value = msgs
  } catch (error) {
    ElMessage.error('加载对话记录失败')
  }
}

const sendReplyMessage = async () => {
  if (!selectedFeedback.value || !newMessage.value.trim()) {
    return
  }

  try {
    await feedbackApi.sendMessage(selectedFeedback.value.id!, newMessage.value)
    newMessage.value = ''
    
    const msgs = await feedbackApi.getMessages(selectedFeedback.value.id!)
    messages.value = msgs
    
    ElMessage.success('回复发送成功')
  } catch (error) {
    ElMessage.error('回复发送失败')
  }
}

const updateFeedbackStatus = async () => {
  if (!selectedFeedback.value) return

  try {
    await feedbackApi.updateStatus(selectedFeedback.value.id!, newStatus.value)
    selectedFeedback.value.status = newStatus.value
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const getUnreadCount = (feedbackId: number): number => {
  return unreadCounts.value[feedbackId] || 0
}

const getTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    'bug': '问题反馈',
    'feature': '功能建议',
    'other': '其他'
  }
  return labels[type.toLowerCase()] || type
}

const getStatusLabel = (status?: string): string => {
  const labels: Record<string, string> = {
    'pending': '待处理',
    'processing': '处理中',
    'resolved': '已解决',
    'closed': '已关闭'
  }
  return labels[(status || '').toLowerCase()] || status || '未知'
}

const getPriorityLabel = (priority: string): string => {
  const labels: Record<string, string> = {
    'low': '低',
    'medium': '中',
    'normal': '中',
    'high': '高'
  }
  return labels[priority.toLowerCase()] || priority
}

const formatDate = (dateStr?: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatTimeDivider = (dateStr?: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return '今天'
  }
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const shouldShowTimeDivider = (message: FeedbackMessage, index: number): boolean => {
  if (index === 0) return true
  const prevMsg = messages.value[index - 1]
  if (!prevMsg) return true
  const currDate = new Date(message.createdAt || '')
  const prevDate = new Date(prevMsg.createdAt || '')
  const timeDiff = currDate.getTime() - prevDate.getTime()
  return timeDiff > 30 * 60 * 1000 || currDate.toDateString() !== prevDate.toDateString()
}

const scrollToBottom = () => {
  nextTick(() => {
    messagesEndRef.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(messages, () => {
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  loadFeedbacks()
})
</script>

<style scoped>
.feedback-management {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.feedback-layout {
  display: flex;
  flex: 1;
  gap: 24px;
  min-height: 0;
}

.feedback-list {
  width: 360px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
}

.list-header {
  padding: 20px 16px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: white;
  border-radius: 10px 10px 0 0;
}

.list-header h3 {
  font-size: 16px;
  margin: 0 0 14px 0;
  color: #111827;
  font-weight: 600;
}

.filter-buttons {
  display: flex;
  gap: 6px;
}

.filter-btn {
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #6b7280;
  transition: all 0.2s;
  font-weight: 500;
}

.filter-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #f8faff;
}

.filter-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.feedback-items {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px 16px 16px 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.feedback-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  transition: background 0.25s ease;
}

.feedback-item.status-bar-pending::before,
.status-bar.pending { background: #f59e0b; }

.feedback-item.status-bar-processing::before,
.status-bar.processing { background: #3b82f6; }

.feedback-item.status-bar-resolved::before,
.status-bar.resolved { background: #10b981; }

.feedback-item.status-bar-closed::before,
.status-bar.closed { background: #9ca3af; }

.feedback-item:hover {
  border-color: #d1d5db;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.feedback-item.active {
  border-color: #3b82f6;
  background: #f8faff;
  box-shadow: 0 0 0 1px #3b82f6;
}

.feedback-item.active::before {
  background: #3b82f6;
}

.status-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 10px 0 0 10px;
}

.status-bar.pending { background: #f59e0b; }
.status-bar.processing { background: #3b82f6; }
.status-bar.resolved { background: #10b981; }
.status-bar.closed { background: #9ca3af; }

.feedback-item-top {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.feedback-type {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.feedback-type.bug {
  background: #fef2f2;
  color: #dc2626;
}

.feedback-type.feature {
  background: #eff6ff;
  color: #2563eb;
}

.feedback-type.other {
  background: #f9fafb;
  color: #6b7280;
}

.feedback-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
}

.feedback-status.pending {
  background: #fffbeb;
  color: #d97706;
}

.feedback-status.processing {
  background: #eff6ff;
  color: #2563eb;
}

.feedback-status.resolved {
  background: #ecfdf5;
  color: #059669;
}

.feedback-status.closed {
  background: #f3f4f6;
  color: #6b7280;
}

.feedback-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 10px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feedback-item-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.feedback-user {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 11px;
  font-weight: 600;
}

.user-avatar-img {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
}

.feedback-time {
  font-size: 12px;
  color: #9ca3af;
}

.unread-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.3);
}

.feedback-detail {
  flex: 1;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.detail-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.detail-header {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.detail-header h3 {
  font-size: 22px;
  margin: 0 0 12px 0;
  color: #1f2937;
}

.detail-meta {
  display: flex;
  gap: 8px;
}

.feedback-priority {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.feedback-priority.high {
  background: #fee2e2;
  color: #dc2626;
}

.feedback-priority.medium,
.feedback-priority.normal {
  background: #fef3c7;
  color: #d97706;
}

.feedback-priority.low {
  background: #dbeafe;
  color: #2563eb;
}

.status-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.status-select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.action-btn {
  padding: 8px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.action-btn:hover {
  background: #2563eb;
}

.detail-info {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.info-item span {
  font-size: 14px;
  color: #1f2937;
}

.detail-body {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-body h4 {
  font-size: 16px;
  margin: 0 0 12px 0;
  color: #1f2937;
}

.feedback-content {
  font-size: 14px;
  line-height: 1.6;
  color: #4b5563;
  white-space: pre-wrap;
}

.messages-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f5f5;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.messages-list::-webkit-scrollbar {
  display: none;
}

.message-time-divider {
  text-align: center;
  font-size: 11px;
  color: #b5b5b5;
  margin: 4px 0;
  padding: 4px 0;
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.message-item.other {
  align-items: flex-end;
}

.message-sender-name {
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
  padding: 0 4px;
  text-align: left;
}

.message-item.other .message-sender-name {
  text-align: right;
}

.message-body-wrapper {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message-item.other .message-body-wrapper {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.message-avatar-inner {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #12b7f5 0%, #108ee9 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.message-avatar-inner.is-other {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 65%;
}

.message-item.other .message-content {
  align-items: flex-end;
}

.message-bubble {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-width: 100%;
}

.message-item .message-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-item.other .message-bubble {
  background: #95ec69;
  color: #333;
  border-bottom-right-radius: 4px;
}

.message-input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid #e5e5e5;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #fff;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.05);
}

.message-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
  max-height: 120px;
  outline: none;
  transition: border-color 0.2s;
}

.message-input:focus {
  border-color: #3b82f6;
}

.send-btn {
  padding: 10px 24px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover {
  background: #2563eb;
}

.no-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  text-align: center;
  color: #9ca3af;
}

.empty-state p {
  font-size: 16px;
  margin: 0;
}
</style>
