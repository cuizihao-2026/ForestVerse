<template>
  <div class="audit-container">
    <div class="audit-card card">
      <div class="audit-body">
        <div class="audit-main">
          <h2 class="audit-article-title">{{ article?.title }}</h2>
          
          <div class="audit-content-wrapper">
            <div class="audit-content">
              <RichContentView :content="article?.content || ''" />
            </div>
          </div>
        </div>
        <div class="audit-sidebar">
          <div v-if="article?.cover" class="sidebar-section">
            <div class="section-title">封面图片</div>
            <img :src="API_BASE_URL + article.cover" class="cover-preview" alt="封面预览">
          </div>
          
          <div class="sidebar-section">
            <div class="section-title">审核操作</div>
            <div class="reject-reason-section">
              <div class="reject-reason-label">
                <label>驳回原因（选填）</label>
                <span class="char-count" :class="{ 'char-count-warning': rejectReason.length > 270 }">{{ rejectReason.length }}/300</span>
              </div>
              <textarea
                v-model="rejectReason"
                class="form-input sidebar-input"
                placeholder="如果驳回，请输入原因..."
                rows="4"
                maxlength="300"
              ></textarea>
            </div>
          </div>
          
          <div v-if="aiAuditEnabled" class="sidebar-section">
            <div class="section-title">AI审核</div>
            <button class="btn btn-info btn-full" @click="handleAIAudit" :disabled="processing || aiAuditing">
              {{ aiAuditing ? '🤖 AI审核中...' : '🤖 AI自动审核' }}
            </button>
            <div v-if="aiAuditResult" class="ai-audit-result" :class="aiAuditResult.approved ? 'success' : 'error'">
              <div class="ai-audit-result-title">
                {{ aiAuditResult.approved ? '✅ AI审核通过' : '❌ AI审核未通过' }}
              </div>
              <div v-if="aiAuditResult.reason" class="ai-audit-result-reason">
                {{ aiAuditResult.reason }}
              </div>
              <div v-if="aiAuditResult.action" class="ai-audit-result-action">
                {{ aiAuditResult.action }}
              </div>
            </div>
          </div>
          
          <div class="sidebar-actions">
            <button class="btn btn-secondary btn-full" @click="handleCancel">返回</button>
            <button class="btn btn-success btn-full" @click="handleApprove" :disabled="processing">
              {{ processing ? '处理中...' : '✅ 通过' }}
            </button>
            <button class="btn btn-warning btn-full" @click="handleReject" :disabled="processing">
              {{ processing ? '处理中...' : '❌ 驳回' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { put, post, get, API_BASE_URL } from '../../utils/api'
import { showModal, showConfirm } from '../../stores/modal'
import RichContentView from '../common/RichContentView.vue'

interface Article {
  id?: number
  userId?: number
  title: string
  cover?: string
  status: number
  rejectReason?: string
  category?: string
  tags?: string
  views?: number
  likes?: number
  comments?: number
  content: string
  createdAt?: string
  updatedAt?: string
  publishedAt?: string
}

interface Props {
  article?: Article
}

const props = withDefaults(defineProps<Props>(), {
  article: undefined
})

const emit = defineEmits<{
  cancel: []
  complete: []
}>()

const processing = ref(false)
const aiAuditing = ref(false)
const rejectReason = ref('')
const aiAuditResult = ref<{ approved: boolean; reason: string; action?: string } | null>(null)
const aiAuditEnabled = ref(false)

const loadSettings = async () => {
  try {
    const data = await get('/api/settings')
    aiAuditEnabled.value = data.aiArticleAuditEnabled || false
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

onMounted(() => {
  loadSettings()
})

const handleCancel = () => {
  emit('cancel')
}

const handleAIAudit = async () => {
  if (!props.article?.id) return

  aiAuditing.value = true
  aiAuditResult.value = null

  try {
    const result = await post(`/api/ai/audit/${props.article.id}`)
    const auditData = result?.data || result
    aiAuditResult.value = auditData
    if (auditData.action) {
      emit('complete')
    }
  } catch (error: any) {
    console.error('AI审核失败:', error)
    aiAuditResult.value = {
      approved: false,
      reason: error?.message || 'AI审核失败'
    }
  } finally {
    aiAuditing.value = false
  }
}

const handleApprove = async () => {
  if (!props.article?.id) return

  const confirmed = await showConfirm({
    title: '确认通过',
    message: '确定要通过这篇文章吗？',
    type: 'warning',
    confirmText: '确认通过',
    cancelText: '取消'
  })

  if (!confirmed) return

  processing.value = true
  try {
    await put(`/api/article/${props.article.id}/status`, {
      status: 3,
      rejectReason: null
    })
    showModal({
      title: '成功',
      message: '文章已通过审核',
      type: 'success'
    })
    emit('complete')
  } catch (error) {
    console.error('审核失败:', error)
    showModal({
      title: '失败',
      message: '审核失败',
      type: 'error'
    })
  } finally {
    processing.value = false
  }
}

const handleReject = async () => {
  if (!props.article?.id) return

  const confirmed = await showConfirm({
    title: '确认驳回',
    message: rejectReason.value.trim()
      ? '确定要驳回这篇文章吗？'
      : '确定要驳回这篇文章吗？建议填写驳回原因',
    type: 'warning',
    confirmText: '确认驳回',
    cancelText: '取消'
  })

  if (!confirmed) return

  processing.value = true
  try {
    await put(`/api/article/${props.article.id}/status`, {
      status: 2,
      rejectReason: rejectReason.value.trim() || null
    })
    showModal({
      title: '成功',
      message: '文章已驳回',
      type: 'success'
    })
    emit('complete')
  } catch (error) {
    console.error('驳回失败:', error)
    showModal({
      title: '失败',
      message: '驳回失败',
      type: 'error'
    })
  } finally {
    processing.value = false
  }
}
</script>

<style scoped>
.audit-container {
  animation: fadeIn 0.3s ease;
  height: 100%;
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

.audit-card {
  overflow: visible;
  padding: 0;
  box-shadow: none;
  border-radius: 0;
  height: 100%;
}

.audit-body {
  display: flex;
  gap: 0;
  padding: 0;
  height: 100%;
}

.audit-main {
  flex: 1;
  padding: 32px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  text-align: left;
  overflow: hidden;
  height: 100%;
}

.audit-article-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-h);
  margin: 0 0 24px 0;
  line-height: 1.4;
  text-align: center;
}

.audit-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.audit-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: white;
  text-align: left;
}

.audit-sidebar {
  width: 320px;
  padding: 24px;
  background: #f8fafc;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-input {
  font-size: 14px;
  padding: 10px 14px;
  border-radius: 8px;
  resize: none;
}

.reject-reason-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.char-count {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.char-count-warning {
  color: #f59e0b;
}

.cover-preview {
  width: 100%;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.reject-reason-section label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-h);
  font-size: 14px;
}

.form-input {
  width: 100%;
  padding: 14px 18px;
  border: 1px solid var(--border);
  border-radius: 12px;
  font-size: 15px;
  font-family: inherit;
  color: var(--text-h);
  background: white;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(30, 64, 175, 0.1);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.sidebar-actions {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-full {
  width: 100%;
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

@media (max-width: 768px) {
  .audit-body {
    flex-direction: column;
  }

  .audit-main {
    border-right: none;
    border-bottom: 1px solid var(--border);
    padding: 24px;
    height: auto;
    min-height: 50vh;
  }

  .audit-sidebar {
    width: 100%;
    height: auto;
    overflow-y: visible;
  }
}
</style>
