<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { isLoggedIn } from '../../stores/auth'
import { feedbackApi, type Feedback } from '../../utils/feedback'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['select-feedback', 'new-feedback'])

const selectedFeedbackId = ref<number | null>(null)
const userFeedbacks = ref<Feedback[]>([])
const loading = ref(false)

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
  switch (type) {
    case 'BUG': return 'bug'
    case 'FEATURE': return 'feature'
    case 'CONTENT': return 'content'
    default: return 'other'
  }
}

const selectFeedback = (feedback: Feedback) => {
  selectedFeedbackId.value = feedback.id || null
  emit('select-feedback', feedback)
}

const handleNewFeedback = () => {
  selectedFeedbackId.value = null
  emit('new-feedback')
}

const loadUserFeedbacks = async () => {
  if (!isLoggedIn.value) {
    userFeedbacks.value = []
    return
  }
  
  loading.value = true
  try {
    userFeedbacks.value = await feedbackApi.getMyFeedbacks()
  } catch (error) {
    ElMessage.error('加载反馈列表失败')
  } finally {
    loading.value = false
  }
}

defineExpose({
  refresh: loadUserFeedbacks
})

onMounted(() => {
  loadUserFeedbacks()
})
</script>

<template>
  <div class="feedback-sidebar">
    <div class="sidebar-header">
      <div class="header-content">
        <h2>我的反馈</h2>
        <p>查看您的反馈记录</p>
      </div>
      <button class="new-feedback-btn" @click="handleNewFeedback">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            <span>新建</span>
          </button>
    </div>

    <div class="feedback-list-container">
      <div v-if="loading" class="empty-state">
        <span class="empty-text">加载中...</span>
      </div>
      <div v-else-if="userFeedbacks.length === 0" class="empty-state">
        <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 4h-4a3 3 0 0 0-3 3v2a3 3 0 0 0-3 3v2a3 3 0 0 0-3 3v2a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-2a3 3 0 0 0-3-3V9a3 3 0 0 0-3-3V7a3 3 0 0 0-3-3z"/>
        </svg>
        <span class="empty-text">暂无反馈记录</span>
        <button class="empty-action" @click="emit('new-feedback')">提交新反馈</button>
      </div>
      <div v-else class="feedback-list">
        <div 
          v-for="feedback in userFeedbacks" 
          :key="feedback.id" 
          class="feedback-item"
          :class="{ active: selectedFeedbackId === feedback.id }"
          @click="selectFeedback(feedback)"
        >
          <div class="feedback-header">
            <span class="feedback-type-icon">
              <svg v-if="getTypeText(feedback.type) === 'bug'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <svg v-else-if="getTypeText(feedback.type) === 'feature'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 23a10 10 0 0 0 10-10c0-5-5.5-7-10-7S2 7 2 12a10 10 0 0 0 10 11z"/>
              </svg>
              <svg v-else-if="getTypeText(feedback.type) === 'content'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <circle cx="12" cy="12" r="6"/>
                <circle cx="12" cy="16" r="2"/>
              </svg>
            </span>
            <div class="feedback-info">
              <span class="feedback-title">{{ feedback.title }}</span>
              <span class="feedback-time">{{ feedback.createdAt?.split('T')[0] }}</span>
            </div>
            <span class="feedback-status" :class="feedback.status === 'RESOLVED' || feedback.status === 'CLOSED' ? 'resolved' : 'pending'">
              {{ getStatusText(feedback.status) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feedback-sidebar {
  width: 360px;
  height: 100%;
  background: #fafafa;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.header-content p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.new-feedback-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.new-feedback-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.new-feedback-btn svg {
  width: 16px;
  height: 16px;
}

.feedback-list-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 12px;
}

.empty-icon {
  width: 48px;
  height: 48px;
  opacity: 0.5;
  color: #9ca3af;
}

.empty-text {
  font-size: 14px;
  color: #9ca3af;
}

.empty-action {
  margin-top: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.empty-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-item {
  padding: 14px 16px;
  background: white;
  border-radius: 10px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.feedback-item:hover {
  border-color: #dbeafe;
  background: #f9fafb;
}

.feedback-item.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feedback-type-icon {
  width: 20px;
  height: 20px;
  color: #64748b;
  flex-shrink: 0;
}

.feedback-item:hover .feedback-type-icon,
.feedback-item.active .feedback-type-icon {
  color: #3b82f6;
}

.feedback-info {
  flex: 1;
  min-width: 0;
}

.feedback-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.feedback-time {
  display: block;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.feedback-status {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: 500;
  flex-shrink: 0;
}

.feedback-status.resolved {
  background: #dcfce7;
  color: #166534;
}

.feedback-status.pending {
  background: #fef3c7;
  color: #92400e;
}

@media (max-width: 860px) {
  .feedback-sidebar {
    width: 100%;
  }
}
</style>
