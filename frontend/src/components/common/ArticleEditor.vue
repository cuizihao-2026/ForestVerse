<template>
  <div class="editor-container">
    <div class="editor-card card">
      <div class="editor-body">
        <main class="editor-main">
          <div class="form-group">
            <label class="section-title">
              文章标题
              <span class="char-count" :class="{ 'char-count-warning': formData.title.length > 90 }">{{ formData.title.length }}/100</span>
            </label>
            <input type="text" v-model="formData.title" class="form-input" placeholder="请输入文章标题..." maxlength="100" autocomplete="off" />
          </div>
          
          <div class="form-group content-group">
            <RichTextEditor 
              v-model="formData.content" 
              placeholder="请输入文章内容..."
            />
          </div>
        </main>
        
        <aside class="editor-sidebar">
          <section class="sidebar-section">
            <h3 class="section-title">封面图片</h3>
            <div class="cover-upload-area" v-if="formData.cover">
              <img :src="API_BASE_URL + formData.cover" class="cover-preview" alt="封面预览">
              <button class="btn btn-sm btn-secondary" @click="formData.cover = ''">更换封面</button>
            </div>
            <div v-else class="cover-upload-placeholder">
              <input type="file" ref="coverFileInput" accept="image/*" @change="handleCoverUpload" hidden>
              <button class="btn btn-secondary" @click="coverFileInput?.click()" :disabled="uploadingCover">
                {{ uploadingCover ? '上传中...' : '上传封面' }}
              </button>
            </div>
          </section>
          
          <section class="sidebar-section">
            <h3 class="section-title">分类</h3>
            <input type="text" v-model="formData.category" class="form-input sidebar-input" placeholder="添加分类" disabled>
          </section>
          
          <section class="sidebar-section">
            <h3 class="section-title">标签</h3>
            <input type="text" v-model="formData.tags" class="form-input sidebar-input" placeholder="多个标签用逗号分隔" disabled>
          </section>
          
          <section class="sidebar-section">
            <h3 class="section-title">发布状态</h3>
            <div class="tab-toggle-wrapper">
              <div class="tab-toggle-track">
                <div class="tab-toggle-thumb" :class="{ 'is-right': (articleAuditEnabled ? formData.status === 1 : formData.status === 3) }"></div>
              </div>
              <button 
                type="button"
                class="tab-toggle-label"
                :class="{ active: formData.status === 0 }"
                @click="formData.status = 0"
              >
                保存草稿
              </button>
              <button 
                type="button"
                class="tab-toggle-label"
                :class="{ active: articleAuditEnabled ? formData.status === 1 : formData.status === 3 }"
                @click="formData.status = articleAuditEnabled ? 1 : 3"
              >
                {{ articleAuditEnabled ? '提交审核' : '直接发布' }}
              </button>
            </div>
          </section>
          
          <div class="sidebar-actions">
            <button class="btn btn-secondary btn-full" @click="handleCancel">返回</button>
            <button class="btn btn-primary btn-full" @click="handleSave" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { post, put, uploadFile, API_BASE_URL, get } from '../../utils/api'
import { showModal, showConfirm } from '../../stores/modal'
import RichTextEditor from '../common/RichTextEditor.vue'

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
  save: []
}>()

const isEditExisting = ref(!!props.article)
const saving = ref(false)
const uploadingCover = ref(false)
const coverFileInput = ref<HTMLInputElement | null>(null)
const articleAuditEnabled = ref(false)

const originalData = ref<{
  title: string
  cover: string
  status: number
  category: string
  tags: string
  content: string
}>({
  title: props.article?.title || '',
  cover: props.article?.cover || '',
  status: props.article?.status ?? 0,
  category: props.article?.category || '',
  tags: props.article?.tags || '',
  content: props.article?.content || ''
})

const formData = ref<Article>({
  title: props.article?.title || '',
  cover: props.article?.cover || '',
  status: props.article?.status ?? 0,
  category: props.article?.category || '',
  tags: props.article?.tags || '',
  content: props.article?.content || ''
})

// 获取文章审核设置
const loadSettings = async () => {
  try {
    const settings = await get('/api/settings/public/security-config')
    articleAuditEnabled.value = settings.articleAuditEnabled || false
    
    if (!props.article?.status) {
      if (articleAuditEnabled.value) {
        formData.value.status = 0
      } else {
        formData.value.status = 3
      }
    } else if (articleAuditEnabled.value && props.article.status === 3) {
      formData.value.status = 1
      originalData.value.status = 1
    }
  } catch (error) {
    console.error('获取设置失败:', error)
  }
}

onMounted(() => {
  loadSettings()
})

const hasChanges = () => {
  return (
    formData.value.title !== originalData.value.title ||
    formData.value.cover !== originalData.value.cover ||
    formData.value.status !== originalData.value.status ||
    formData.value.category !== originalData.value.category ||
    formData.value.tags !== originalData.value.tags ||
    formData.value.content !== originalData.value.content
  )
}

const handleCancel = async () => {
  if (hasChanges()) {
    const confirmed = await showConfirm({
      title: '未保存的更改',
      message: '您有未保存的更改，确定要离开吗？',
      type: 'warning',
      confirmText: '确定离开',
      cancelText: '继续编辑'
    })
    if (!confirmed) {
      return
    }
  }
  emit('cancel')
}

const handleCoverUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploadingCover.value = true
  try {
    const fileUrl = await uploadFile(file, 'article-covers')
    formData.value.cover = fileUrl
  } catch (error) {
    console.error('封面上传失败:', error)
    showModal({
      title: '失败',
      message: '封面上传失败',
      type: 'error'
    })
  } finally {
    uploadingCover.value = false
  }
}

const handleSave = async () => {
  if (!formData.value.title.trim()) {
    showModal({
      title: '提示',
      message: '请输入文章标题',
      type: 'warning'
    })
    return
  }
  if (formData.value.title.length > 100) {
    showModal({
      title: '提示',
      message: '标题长度不能超过100字',
      type: 'warning'
    })
    return
  }

  console.log('准备保存文章，数据:', formData.value)

  saving.value = true
  try {
    if (isEditExisting.value && props.article?.id) {
      await put(`/api/article/${props.article.id}`, formData.value)
    } else {
      await post('/api/article/create', formData.value)
    }
    
    // 保存成功后更新原始数据
    originalData.value = {
      title: formData.value.title,
      cover: formData.value.cover || '',
      status: formData.value.status,
      category: formData.value.category || '',
      tags: formData.value.tags || '',
      content: formData.value.content
    }
    
    showModal({
      title: '成功',
      message: '文章保存成功',
      type: 'success'
    })
    emit('save')
  } catch (error: any) {
    console.error('保存失败:', error)
    showModal({
      title: '失败',
      message: error.message || '保存失败',
      type: 'error'
    })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.editor-container {
  animation: fadeIn 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
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

.editor-card {
  overflow: hidden;
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.editor-body {
  display: flex;
  gap: 0;
  padding: 0;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.editor-main {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  text-align: left;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
}

.editor-main::-webkit-scrollbar {
  width: 0;
}

.editor-sidebar {
  width: 320px;
  padding: 24px;
  background: #f8fafc;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
}

.editor-sidebar::-webkit-scrollbar {
  width: 0;
}

.content-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.char-count-warning {
  color: #f59e0b;
}

.sidebar-input {
  font-size: 14px;
  padding: 10px 14px;
  border-radius: 8px;
}

.status-toggle-group {
  display: flex;
  gap: 8px;
}

.status-toggle-btn {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: white;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  text-align: center;
  white-space: nowrap;
}

.status-toggle-btn:hover {
  border-color: #93c5fd;
  color: #3b82f6;
  background: #eff6ff;
}

.status-toggle-btn.active {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #1e40af;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.btn-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.cover-upload-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cover-preview {
  width: 100%;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.cover-upload-placeholder {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-group {
  margin-bottom: 24px;
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

@media (max-width: 768px) {
  .editor-body {
    flex-direction: column;
  }

  .editor-main {
    border-right: none;
    border-bottom: 1px solid var(--border);
    padding: 16px;
  }

  .editor-sidebar {
    width: 100%;
  }
}
</style>
