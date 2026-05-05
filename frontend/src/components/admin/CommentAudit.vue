<template>
  <div class="comment-audit-container">
    <div v-if="comments.length === 0" class="empty-state">
      <div class="empty-icon">✨</div>
      <h2>暂无待审核的评论</h2>
      <p>所有评论都已审核完毕</p>
    </div>

    <div v-else>
      <div v-if="aiAuditEnabled" class="batch-actions">
        <button class="btn btn-info" @click="handleBatchAIAudit" :disabled="batchAuditing">
          {{ batchAuditing ? '🤖 AI批量审核中...' : '🤖 AI批量审核所有' }}
        </button>
        <span v-if="batchProgress !== null" class="batch-progress">
          已审核 {{ batchProgress }}/{{ comments.length }}
        </span>
      </div>

      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-card card">
          <div class="comment-header">
            <el-avatar :size="40" :src="comment.userAvatar || ''">
              {{ (comment.userName || '用')[0] }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ comment.userName || '匿名用户' }}</div>
              <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
            </div>
            <div v-if="comment.replyToUserName" class="reply-to">
              回复 {{ comment.replyToUserName }}
            </div>
          </div>

          <div class="comment-content">{{ comment.content }}</div>

          <div v-if="aiAuditEnabled" class="ai-audit-section">
            <button
              class="btn btn-info btn-small"
              @click="handleAIAudit(comment)"
              :disabled="processing[comment.id] || aiAuditing[comment.id] || batchAuditing"
            >
              {{ aiAuditing[comment.id] ? '🤖 AI审核中...' : '🤖 AI自动审核' }}
            </button>
            <div v-if="aiAuditResults[comment.id]" class="ai-audit-result" :class="aiAuditResults[comment.id]!.approved ? 'success' : 'error'">
              <div class="ai-audit-result-title">
                {{ aiAuditResults[comment.id]!.approved ? '✅ AI审核通过' : '❌ AI审核未通过' }}
              </div>
              <div v-if="aiAuditResults[comment.id]!.reason" class="ai-audit-result-reason">
                {{ aiAuditResults[comment.id]!.reason }}
              </div>
              <div v-if="aiAuditResults[comment.id]!.action" class="ai-audit-result-action">
                {{ aiAuditResults[comment.id]!.action }}
              </div>
            </div>
          </div>

          <div class="comment-actions">
            <div class="reject-reason-wrap">
              <textarea
                v-if="showReject[comment.id]"
                :value="rejectReasons[comment.id] || ''"
                @input="rejectReasons[comment.id] = ($event.target as HTMLTextAreaElement).value"
                placeholder="驳回原因（选填）"
                rows="2"
                class="form-input"
              ></textarea>
            </div>
            <div class="btn-group">
              <button
                v-if="!showReject[comment.id]"
                class="btn btn-warning"
                @click="showReject[comment.id] = true"
                :disabled="processing[comment.id] || aiAuditing[comment.id] || batchAuditing"
              >
                驳回
              </button>
              <template v-else>
                <button class="btn btn-secondary" @click="showReject[comment.id] = false" :disabled="processing[comment.id]">
                  取消
                </button>
                <button class="btn btn-warning" @click="handleReject(comment)" :disabled="processing[comment.id]">
                  {{ processing[comment.id] ? '处理中...' : '确认驳回' }}
                </button>
              </template>
              <button class="btn btn-success" @click="handleApprove(comment)" :disabled="processing[comment.id] || aiAuditing[comment.id] || batchAuditing">
                {{ processing[comment.id] ? '处理中...' : '通过' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { get, put, post } from '../../utils/api'
import { showModal, showConfirm } from '../../stores/modal'

const comments = ref<any[]>([])
const loading = ref(false)
const processing = ref<Record<number, boolean>>({})
const aiAuditing = ref<Record<number, boolean>>({})
const showReject = ref<Record<number, boolean>>({})
const rejectReasons = ref<Record<number, string>>({})
const aiAuditResults = ref<Record<number, { approved: boolean; reason: string; action?: string } | null>>({})
const aiAuditEnabled = ref(false)
const batchAuditing = ref(false)
const batchProgress = ref<number | null>(null)

const loadSettings = async () => {
  try {
    const data = await get('/api/settings')
    aiAuditEnabled.value = data.aiCommentAuditEnabled || false
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

const loadComments = async () => {
  loading.value = true
  try {
    const data = await get('/api/comment/admin/pending')
    comments.value = data || []
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAIAudit = async (comment: any) => {
  aiAuditing.value[comment.id] = true
  aiAuditResults.value[comment.id] = null

  try {
    const result = await post(`/api/ai/audit/comment/${comment.id}`)
    const auditData = result?.data || result
    aiAuditResults.value[comment.id] = auditData
    await loadComments()
  } catch (error: any) {
    console.error('AI审核失败:', error)
    aiAuditResults.value[comment.id] = {
      approved: false,
      reason: error?.message || 'AI审核失败'
    }
  } finally {
    aiAuditing.value[comment.id] = false
  }
}

const handleBatchAIAudit = async () => {
  if (comments.value.length === 0) return

  const confirmed = await showConfirm({
    title: '确认批量AI审核',
    message: `确定要使用AI自动审核这 ${comments.value.length} 条评论吗？`,
    type: 'warning',
    confirmText: '确认审核',
    cancelText: '取消'
  })

  if (!confirmed) return

  batchAuditing.value = true
  batchProgress.value = 0

  try {
    for (let i = 0; i < comments.value.length; i++) {
      const comment = comments.value[i]
      if (!comment.id) continue

      try {
        await post(`/api/ai/audit/comment/${comment.id}`)
      } catch (e) {
        console.error(`审核评论 ${comment.id} 失败:`, e)
      }

      batchProgress.value = i + 1
      await new Promise(resolve => setTimeout(resolve, 500))
    }

    showModal({
      title: '完成',
      message: '批量AI审核完成',
      type: 'success'
    })

    await loadComments()
  } catch (error) {
    console.error('批量审核失败:', error)
    showModal({
      title: '失败',
      message: '批量审核失败',
      type: 'error'
    })
  } finally {
    batchAuditing.value = false
    batchProgress.value = null
  }
}

const formatTime = (dateStr: string) => {
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

const handleApprove = async (comment: any) => {
  processing.value[comment.id] = true
  try {
    await put(`/api/comment/${comment.id}/approve`, {})
    showModal({
      title: '成功',
      message: '评论已通过审核',
      type: 'success'
    })
    await loadComments()
  } catch (error) {
    console.error('审核失败:', error)
    showModal({
      title: '失败',
      message: '审核失败，请重试',
      type: 'error'
    })
  } finally {
    processing.value[comment.id] = false
  }
}

const handleReject = async (comment: any) => {
  processing.value[comment.id] = true
  try {
    await put(`/api/comment/${comment.id}/reject`, {
      reason: rejectReasons.value[comment.id] || ''
    })
    showModal({
      title: '成功',
      message: '评论已驳回',
      type: 'success'
    })
    showReject.value[comment.id] = false
    await loadComments()
  } catch (error) {
    console.error('驳回失败:', error)
    showModal({
      title: '失败',
      message: '驳回失败，请重试',
      type: 'error'
    })
  } finally {
    processing.value[comment.id] = false
  }
}

const handleCommentAudited = (event: any) => {
  console.log('收到评论审核通过事件（管理后台），刷新待审核列表', event?.detail);
  loadComments()
}

onMounted(() => {
  loadSettings()
  loadComments()
  window.addEventListener('comment-approved', handleCommentAudited)
})

onUnmounted(() => {
  window.removeEventListener('comment-approved', handleCommentAudited)
})
</script>

<style scoped>
.comment-audit-container {
  animation: fadeIn 0.3s ease;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.batch-progress {
  font-size: 14px;
  color: #64748b;
}

.btn-info {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  color: white;
  border: none;
}

.btn-info:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #475569;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-card {
  padding: 20px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

.reply-to {
  margin-left: auto;
  padding: 4px 10px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.comment-content {
  font-size: 15px;
  color: #374151;
  line-height: 1.6;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 16px;
}

.comment-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-audit-section {
  margin-bottom: 12px;
}

.btn-small {
  padding: 8px 16px;
  font-size: 13px;
}

.btn-info {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  color: white;
  border: none;
}

.btn-info:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
}

.ai-audit-result {
  margin-top: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.ai-audit-result.success {
  background: #d1fae5;
  border-color: #10b981;
}

.ai-audit-result.error {
  background: #fee2e2;
  border-color: #ef4444;
}

.ai-audit-result-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 14px;
}

.ai-audit-result.success .ai-audit-result-title {
  color: #065f46;
}

.ai-audit-result.error .ai-audit-result-title {
  color: #991b1b;
}

.ai-audit-result-reason {
  font-size: 13px;
  color: #475569;
  margin-bottom: 6px;
  line-height: 1.5;
}

.ai-audit-result-action {
  font-size: 13px;
  font-weight: 600;
  color: #059669;
}

.reject-reason-wrap {
  min-height: 0;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  color: #374151;
  background: white;
  transition: all 0.2s;
  box-sizing: border-box;
  resize: none;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.btn-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>
