<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'
import { feedbackApi, type Feedback, type FeedbackMessage } from '../../utils/feedback'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Props {
  selectedFeedback?: Feedback
}

const props = defineProps<Props>()
const emit = defineEmits(['submitted'])

const feedbackTypes = [
  { id: 'BUG', name: 'Bug反馈', icon: 'bug', desc: '报告程序问题' },
  { id: 'FEATURE', name: '功能建议', icon: 'lightbulb', desc: '提出新功能想法' },
  { id: 'CONTENT', name: '内容建议', icon: 'file-text', desc: '建议文章或话题' },
  { id: 'OTHER', name: '其他问题', icon: 'help-circle', desc: '其他反馈内容' }
]

const selectedType = ref<string>('')
const title = ref('')
const content = ref('')
const contact = ref('')
const priority = ref('NORMAL')
const isSubmitting = ref(false)
const viewMode = ref(false)

const messages = ref<FeedbackMessage[]>([])
const newMessage = ref('')
const isSendingMessage = ref(false)
const messagesEndRef = ref<HTMLElement | null>(null)

const getStatusText = (status?: string) => {
  switch (status) {
    case 'PENDING': return '待处理'
    case 'PROCESSING': return '处理中'
    case 'RESOLVED': return '已解决'
    case 'CLOSED': return '已关闭'
    default: return status || '待处理'
  }
}

const getTypeText = (type?: string) => {
  const t = feedbackTypes.find(ft => ft.id === type)
  return t?.name || type
}

const getPriorityText = (p?: string) => {
  switch (p?.toUpperCase()) {
    case 'HIGH': return '高优先级'
    case 'LOW': return '低优先级'
    default: return '普通'
  }
}

const resetForm = () => {
  selectedType.value = ''
  title.value = ''
  content.value = ''
  contact.value = ''
  priority.value = 'NORMAL'
  viewMode.value = false
  messages.value = []
  newMessage.value = ''
}

const selectType = (typeId: string) => {
  selectedType.value = typeId
  viewMode.value = false
}

const loadMessages = async () => {
  if (!props.selectedFeedback?.id) return
  try {
    messages.value = await feedbackApi.getMessages(props.selectedFeedback.id)
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim() || !props.selectedFeedback?.id) return
  isSendingMessage.value = true
  try {
    await feedbackApi.sendMessage(props.selectedFeedback.id, newMessage.value)
    newMessage.value = ''
    await loadMessages()
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    isSendingMessage.value = false
  }
}

const closeFeedback = async () => {
  if (!props.selectedFeedback?.id) return
  try {
    await ElMessageBox.confirm('确定要关闭这条反馈吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await feedbackApi.updateStatus(props.selectedFeedback.id, 'CLOSED')
    ElMessage.success('反馈已关闭')
    emit('submitted')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleSubmit = async () => {
  if (!selectedType.value) {
    ElMessage.warning('请选择反馈类型')
    return
  }
  if (!title.value.trim()) {
    ElMessage.warning('请输入反馈标题')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }

  isSubmitting.value = true
  try {
    await feedbackApi.createFeedback({
      type: selectedType.value,
      title: title.value,
      content: content.value,
      contact: contact.value,
      priority: priority.value
    })
    ElMessage.success('反馈提交成功！我们会尽快处理')
    resetForm()
    emit('submitted')
  } catch (error) {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

const scrollToBottom = () => {
  if (messagesEndRef.value) {
    messagesEndRef.value.scrollIntoView({ behavior: 'smooth' })
  }
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

watch(() => props.selectedFeedback, (newFeedback) => {
  if (newFeedback) {
    viewMode.value = true
    selectedType.value = newFeedback.type || ''
    title.value = newFeedback.title || ''
    content.value = newFeedback.content || ''
    priority.value = newFeedback.priority || 'NORMAL'
    contact.value = newFeedback.contact || ''
    loadMessages()
  } else {
    resetForm()
  }
}, { immediate: true })

onMounted(() => {
  if (props.selectedFeedback) {
    loadMessages()
  }
})
</script>

<template>
  <div class="feedback-form-container">
    <template v-if="viewMode && selectedFeedback">
      <div class="feedback-detail">
        <div class="detail-header">
          <div class="detail-header-left">
            <div class="type-row">
              <div class="type-badge">
                <svg v-if="selectedType === 'BUG'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                <svg v-else-if="selectedType === 'FEATURE'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 23a10 10 0 0 0 10-10c0-5-5.5-7-10-7S2 7 2 12a10 10 0 0 0 10 11z"/>
                </svg>
                <svg v-else-if="selectedType === 'CONTENT'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <circle cx="12" cy="12" r="6"/>
                  <circle cx="12" cy="16" r="2"/>
                </svg>
                <span>{{ getTypeText(selectedType) }}</span>
              </div>
              <span class="status-badge" :class="selectedFeedback.status === 'RESOLVED' || selectedFeedback.status === 'CLOSED' ? 'resolved' : 'pending'">
                {{ getStatusText(selectedFeedback.status) }}
              </span>
            </div>
            <h1>{{ title }}</h1>
          </div>
          <button 
            v-if="selectedFeedback.status !== 'RESOLVED' && selectedFeedback.status !== 'CLOSED'"
            class="btn btn-danger"
            @click="closeFeedback"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            关闭反馈
          </button>
        </div>

        <div class="detail-meta">
          <div class="meta-card">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            <div class="meta-info">
              <span class="meta-label">提交时间</span>
              <span class="meta-value">{{ selectedFeedback.createdAt?.split('T')[0] }}</span>
            </div>
          </div>
          <div class="meta-card priority" :class="priority.toLowerCase()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
            <div class="meta-info">
              <span class="meta-label">优先级</span>
              <span class="meta-value">{{ getPriorityText(priority) }}</span>
            </div>
          </div>
        </div>

        <div class="detail-content">
          <div class="content-section">
            <h3>反馈内容</h3>
            <div class="content-body">
              {{ content }}
            </div>
          </div>

          <div v-if="contact" class="content-section">
            <h3>联系方式</h3>
            <div class="content-body">
              {{ contact }}
            </div>
          </div>

          <div class="content-section">
            <h3>对话</h3>
            <div class="messages-container">
              <div v-if="messages.length === 0" class="no-messages">
                暂无对话，发送第一条消息开始沟通吧
              </div>
              <div v-else class="messages-list">
                <div 
                  v-for="(msg, index) in messages" 
                  :key="msg.id"
                >
                  <div v-if="shouldShowTimeDivider(msg, index)" class="message-time-divider">
                    {{ formatTimeDivider(msg.createdAt) }}
                  </div>
                  <div 
                    class="message-item"
                    :class="{ other: msg.senderId !== selectedFeedback?.userId }"
                  >
                    <div class="message-sender-name">{{ msg.senderNickname || (msg.senderId !== selectedFeedback?.userId ? '管理员' : '用户') }}</div>
                    <div class="message-body-wrapper">
                      <div class="message-avatar">
                        <img v-if="msg.senderAvatar" :src="msg.senderAvatar" :alt="msg.senderNickname" class="message-avatar-img" />
                        <div v-else class="message-avatar-inner" :class="{ 'is-other': msg.senderId !== selectedFeedback?.userId }">
                          {{ msg.senderNickname?.charAt(0) || (msg.senderId !== selectedFeedback?.userId ? '管' : '用') }}
                        </div>
                      </div>
                      <div class="message-content">
                        <div class="message-bubble">{{ msg.content }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div ref="messagesEndRef"></div>
            </div>

            <div v-if="selectedFeedback.status !== 'RESOLVED' && selectedFeedback.status !== 'CLOSED'" class="message-input-container">
              <textarea 
                v-model="newMessage" 
                class="message-input"
                placeholder="输入消息..."
                :disabled="isSendingMessage"
                @keydown.enter.prevent="sendMessage"
              ></textarea>
              <button 
                class="send-btn"
                @click="sendMessage"
                :disabled="isSendingMessage || !newMessage.trim()"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="feedback-form">
        <div class="type-selector">
          <h3>反馈类型</h3>
          <div class="type-grid">
            <button 
              v-for="type in feedbackTypes" 
              :key="type.id"
              class="type-card"
              :class="{ active: selectedType === type.id }"
              @click="selectType(type.id)"
            >
              <svg v-if="type.icon === 'bug'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
                <path d="M12 12v4"/>
                <path d="M8 12l-2 2"/>
                <path d="M16 12l2 2"/>
              </svg>
              <svg v-else-if="type.icon === 'lightbulb'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 23a10 10 0 0 0 10-10c0-5-5.5-7-10-7S2 7 2 12a10 10 0 0 0 10 11z"/>
                <path d="M9 12l2 2 4-4"/>
              </svg>
              <svg v-else-if="type.icon === 'file-text'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <circle cx="12" cy="12" r="6"/>
                <circle cx="12" cy="16" r="2"/>
              </svg>
              <span class="type-name">{{ type.name }}</span>
            </button>
          </div>
        </div>

        <div class="form-content">
          <div class="form-group">
            <label class="form-label">标题 *</label>
            <div class="input-wrapper">
              <input 
                v-model="title" 
                type="text" 
                class="form-input"
                placeholder="请简要描述您的问题或建议"
                maxlength="100"
                :disabled="isSubmitting"
              />
              <span class="char-count" :class="{ limit: title.length > 90 }">{{ title.length }}/100</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">详细内容 *</label>
            <div class="input-wrapper">
              <textarea 
                v-model="content" 
                class="form-textarea"
                placeholder="请详细描述您遇到的问题或建议..."
                rows="8"
                maxlength="1000"
                :disabled="isSubmitting"
              ></textarea>
              <span class="char-count" :class="{ limit: content.length > 900 }">{{ content.length }}/1000</span>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">优先级</label>
              <select v-model="priority" class="form-select" :disabled="isSubmitting">
                <option value="LOW">低</option>
                <option value="NORMAL">普通</option>
                <option value="HIGH">高</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">联系方式 (选填)</label>
              <input 
                v-model="contact" 
                type="text" 
                class="form-input"
                placeholder="邮箱或手机号，方便我们联系您"
                :disabled="isSubmitting"
              />
            </div>
          </div>

          <div class="form-actions">
            <button 
              class="btn btn-primary"
              @click="handleSubmit"
              :disabled="isSubmitting"
            >
              {{ isSubmitting ? '提交中...' : '提交反馈' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.feedback-form-container {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: white;
}

.feedback-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  padding: 32px 40px;
  border-bottom: 1px solid #e5e7eb;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-shrink: 0;
}

.detail-header-left {
  flex: 1;
}

.type-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

.type-badge svg {
  width: 16px;
  height: 16px;
}

.status-badge {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.resolved {
  background: #dcfce7;
  color: #166534;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.detail-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
}

.detail-meta {
  display: flex;
  gap: 16px;
  padding: 24px 40px;
  background: #f9fafb;
  flex-shrink: 0;
}

.meta-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  min-width: 180px;
}

.meta-card svg {
  width: 20px;
  height: 20px;
  color: #6b7280;
  flex-shrink: 0;
}

.meta-card.priority.high svg,
.meta-card.priority.high .meta-value {
  color: #dc2626;
}

.meta-card.priority.normal svg,
.meta-card.priority.normal .meta-value {
  color: #f59e0b;
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 12px;
  color: #9ca3af;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.detail-content {
  flex: 1;
  padding: 32px 40px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.content-section {
  display: flex;
  flex-direction: column;
}

.content-section:last-child {
  flex: 1;
  min-height: 0;
}

.content-section h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-align: left;
}

.content-body {
  padding: 16px 20px;
  background: #f9fafb;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.7;
  color: #374151;
  text-align: left;
}

.messages-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
  border-radius: 12px;
  overflow: hidden;
  min-height: 300px;
}

.no-messages {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
}

.messages-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.message-input-container {
  display: flex;
  gap: 12px;
  padding: 16px 0 0;
  border-top: 1px solid #e5e7eb;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  height: 48px;
  max-height: 120px;
  transition: all 0.2s;
}

.message-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.send-btn {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn svg {
  width: 20px;
  height: 20px;
}

.feedback-form {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.type-selector {
  padding: 24px 40px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;
  flex-shrink: 0;
}

.type-selector h3 {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: #64748b;
}

.type-card svg {
  width: 24px;
  height: 24px;
}

.type-card:hover {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #3b82f6;
}

.type-card.active {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #3b82f6;
}

.type-name {
  font-size: 13px;
  font-weight: 500;
}

.form-content {
  padding: 32px 40px;
  flex: 1;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  font-size: 15px;
  font-family: inherit;
  color: #1f2937;
  background: white;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea {
  resize: none;
  min-height: 120px;
  line-height: 1.6;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .form-textarea {
  padding-bottom: 32px;
}

.char-count {
  position: absolute;
  bottom: 10px;
  right: 12px;
  font-size: 12px;
  color: #9ca3af;
  pointer-events: none;
}

.char-count.limit {
  color: #ef4444;
}

.info-label {
  font-size: 14px;
  color: #6b7280;
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.btn-primary {
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-danger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-danger svg {
  width: 16px;
  height: 16px;
}

@media (max-width: 860px) {
  .form-content {
    padding: 24px 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .detail-header,
  .detail-meta,
  .detail-content {
    padding: 24px 20px;
  }
}
</style>
