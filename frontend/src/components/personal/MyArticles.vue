<template>
  <div class="my-articles">
    <!-- 汇总数据栏和按钮在同一行 -->
    <div class="header" v-if="!isEditing">
      <!-- 汇总数据栏 -->
      <div v-if="!loading" class="stats-row">
        <div class="stat-item">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">全部</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.published }}</span>
          <span class="stat-label">已发布</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.pending }}</span>
          <span class="stat-label">待审核</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.draft }}</span>
          <span class="stat-label">草稿</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.totalViews }}</span>
          <span class="stat-label">浏览</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.totalLikes }}</span>
          <span class="stat-label">点赞</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.totalComments }}</span>
          <span class="stat-label">评论</span>
        </div>
        <div class="stat-divider" v-if="stats.rejected > 0"></div>
        <div class="stat-item" v-if="stats.rejected > 0">
          <span class="stat-value">{{ stats.rejected }}</span>
          <span class="stat-label">驳回</span>
        </div>
      </div>
      
      <button class="btn btn-primary" @click="startCreate">
        + 写文章
      </button>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 编辑界面 -->
    <div v-else-if="isEditing" class="editor-wrapper">
      <ArticleEditor 
        :article="currentArticle" 
        @cancel="cancelEdit"
        @save="onEditorSave"
      />
    </div>

    <!-- 列表界面 -->
    <div v-else class="list-wrapper">
      <div v-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
            <polyline points="10 9 9 9 8 9"></polyline>
          </svg>
        </div>
        <h3>暂无文章</h3>
        <p>还没有文章，快去写一篇吧！</p>
      </div>

      <div v-else class="articles-grid">
        <div v-for="article in articles" :key="article.id" class="article-card card card-hover">
          <div class="article-cover">
            <img v-if="article.cover" :src="API_BASE_URL + article.cover" :alt="article.title" class="cover-image">
            <div v-else class="cover-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                <circle cx="8.5" cy="8.5" r="1.5"></circle>
                <polyline points="21 15 16 10 5 21"></polyline>
              </svg>
              <span>无封面</span>
            </div>
          </div>
          <div class="article-content">
            <div class="article-body">
              <div class="article-top">
                <h3 class="article-title">{{ article.title }}</h3>
                <div class="article-badges">
                  <span 
                    class="article-status" 
                    :class="getStatusClass(article.status)"
                    v-if="article.status === 2 && article.rejectReason"
                    @click="openFullRejectReason(article.rejectReason)"
                    style="cursor: pointer;"
                  >
                    查看驳回原因
                  </span>
                  <span class="article-status" :class="getStatusClass(article.status)" v-else>
                    {{ getStatusText(article.status) }}
                  </span>
                  <span v-if="article.category" class="article-category">
                    {{ article.category }}
                  </span>
                </div>
              </div>
              
              <div class="article-tags" v-if="article.tags">
                <span class="tag" v-for="tag in article.tags.split(',').slice(0, 4)" :key="tag">{{ tag.trim() }}</span>
              </div>
              
              <div class="article-meta">
                <div class="meta-left">
                  <span class="meta-item">
                    <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                    {{ article.views || 0 }}
                  </span>
                  <span class="meta-item">
                    <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                    {{ article.likes || 0 }}
                  </span>
                  <span class="meta-item">
                    <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                    </svg>
                    {{ article.comments || 0 }}
                  </span>
                </div>
                <span class="meta-time">{{ formatRelativeTime(article.createdAt) }}</span>
              </div>
            </div>
            <div class="article-actions">
              <button class="action-btn btn btn-sm btn-secondary" @click="viewArticle(article)" title="查看">
                <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                查看
              </button>
              <button class="action-btn btn btn-sm btn-secondary" @click="startEdit(article)" title="编辑">
                <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                编辑
              </button>
              <button class="action-btn btn btn-sm btn-danger" @click="deleteArticle(article)" title="删除">
                <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <!-- 完整驳回信息弹窗 -->
  <div class="modal-overlay" v-if="showFullRejectModal" @click.self="showFullRejectModal = false">
    <div class="modal-content full-reject-modal">
      <div class="modal-header">
        <h3>驳回原因</h3>
        <button class="btn-close" @click="showFullRejectModal = false">✕</button>
      </div>
      <div class="modal-body">
        <div class="full-reject-content">{{ fullRejectReason }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get, del, API_BASE_URL } from '../../utils/api'
import { showModal } from '../../stores/modal'
import { hasPermission } from '../../stores/permission'
import ArticleEditor from '../common/ArticleEditor.vue'

const router = useRouter()
const showFullRejectModal = ref(false)
const fullRejectReason = ref('')

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

const articles = ref<Article[]>([])
const loading = ref(false)
const isEditing = ref(false)
const currentArticle = ref<Article | undefined>(undefined)

const stats = computed(() => {
  const total = articles.value.length
  const draft = articles.value.filter(a => a.status === 0).length
  const pending = articles.value.filter(a => a.status === 1).length
  const rejected = articles.value.filter(a => a.status === 2).length
  const published = articles.value.filter(a => a.status === 3).length
  const totalViews = articles.value.reduce((sum, a) => sum + (a.views || 0), 0)
  const totalLikes = articles.value.reduce((sum, a) => sum + (a.likes || 0), 0)
  const totalComments = articles.value.reduce((sum, a) => sum + (a.comments || 0), 0)
  
  return {
    total,
    draft,
    pending,
    rejected,
    published,
    totalViews,
    totalLikes,
    totalComments
  }
})

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '待审核',
  2: '已驳回',
  3: '已发布'
}

const getStatusText = (status: number): string => {
  return statusMap[status] || '未知'
}

const getStatusClass = (status: number): string => {
  const classMap: Record<number, string> = {
    0: 'status-draft',
    1: 'status-pending',
    2: 'status-rejected',
    3: 'status-published'
  }
  return classMap[status] || 'status-unknown'
}

const formatRelativeTime = (dateStr: any): string => {
  if (!dateStr) return ''
  try {
    let date: Date
    if (typeof dateStr === 'string') {
      date = new Date(dateStr)
    } else if (dateStr.date && dateStr.time) {
      const { year, month, day } = dateStr.date
      const { hour, minute, second } = dateStr.time
      date = new Date(year, month - 1, day, hour, minute, second)
    } else {
      return ''
    }
    
    if (isNaN(date.getTime())) return ''
    
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    const months = Math.floor(days / 30)
    const years = Math.floor(days / 365)
    
    if (seconds < 60) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 30) return `${days}天前`
    if (months < 12) return `${months}个月前`
    return `${years}年前`
  } catch (e) {
    return ''
  }
}

const loadArticles = async () => {
  // 检查权限
  if (!hasPermission('article:create')) {
    router.push('/home')
    return
  }
  
  loading.value = true
  try {
    const data = await get('/api/article/my')
    if (Array.isArray(data)) {
      articles.value = data
    } else {
      articles.value = []
    }
  } catch (error: any) {
    articles.value = []
    if (error?.status === 403) {
      showModal({
        title: '权限不足',
        message: '您没有访问此页面的权限',
        type: 'error',
        onConfirm: () => {
          router.push('/home')
        }
      })
    }
  } finally {
    loading.value = false
  }
}

const startCreate = () => {
  currentArticle.value = undefined
  isEditing.value = true
}

const startEdit = async (article: Article) => {
  try {
    loading.value = true
    const articleDetail = await get(`/api/article/${article.id}`)
    currentArticle.value = articleDetail
    isEditing.value = true
  } catch (error) {
    console.error('获取文章详情失败:', error)
    showModal({
      title: '失败',
      message: '获取文章详情失败',
      type: 'error'
    })
  } finally {
    loading.value = false
  }
}

const cancelEdit = () => {
  isEditing.value = false
  currentArticle.value = undefined
}

const onEditorSave = async () => {
  await loadArticles()
  cancelEdit()
}

const viewArticle = (article: Article) => {
  router.push(`/article/${article.id}`)
}

const openFullRejectReason = (reason: string) => {
  fullRejectReason.value = reason
  showFullRejectModal.value = true
}

const deleteArticle = async (article: Article) => {
  showModal({
    title: '确认删除',
    message: `确定要删除文章《${article.title}》吗？`,
    type: 'warning',
    onConfirm: async () => {
      try {
        await del(`/api/article/${article.id}`)
        await loadArticles()
        showModal({
          title: '成功',
          message: '删除成功',
          type: 'success'
        })
      } catch (error) {
        console.error('删除失败:', error)
        showModal({
          title: '失败',
          message: '删除失败',
          type: 'error'
        })
      }
    }
  })
}

onMounted(() => {
  loadArticles()
})
</script>

<style scoped>
.my-articles {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: var(--sans);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2xl);
  padding: 20px 24px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

/* 汇总数据栏 */
.stats-row {
  display: flex;
  align-items: center;
  gap: 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 20px;
  min-width: 70px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #e5e7eb;
}

.editor-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.list-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3xl) var(--spacing-xl);
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
  padding: var(--spacing-3xl) var(--spacing-xl);
  color: var(--text-l);
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto var(--spacing-md);
  color: #d1d5db;
}

.empty-state h3 {
  margin: 0 0 var(--spacing-sm);
  color: var(--text-h);
  font-size: 20px;
}

.empty-state p {
  margin: 0;
  color: var(--text-m);
  font-size: 15px;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: var(--spacing-lg);
}

.article-card {
  display: flex;
  flex-direction: row;
  overflow: hidden;
}

.article-cover {
  width: 220px;
  height: 180px;
  flex-shrink: 0;
  background: var(--bg-alt);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  color: #d1d5db;
  gap: 8px;
}

.cover-placeholder svg {
  width: 48px;
  height: 48px;
}

.cover-placeholder span {
  font-size: 12px;
}

.article-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.article-body {
  padding: 14px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.article-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.article-badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.article-status {
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 500;
}

.article-status.status-draft {
  background: #f1f5f9;
  color: #64748b;
}

.article-status.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.article-status.status-rejected {
  background: #fee2e2;
  color: #dc2626;
}

.article-status.status-published {
  background: #dcfce7;
  color: #16a34a;
}

.article-category {
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 500;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-h);
  margin: 0;
  line-height: 1.35;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  text-align: left;
}

/* 弹窗样式 */
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

.full-reject-content {
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #333;
  line-height: 1.6;
  text-align: left;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-l);
  margin-top: auto;
  padding-top: 8px;
}

.meta-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  width: 14px;
  height: 14px;
  opacity: 0.6;
}

.meta-time {
  flex-shrink: 0;
}

.article-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.tag {
  padding: 3px 8px;
  border-radius: 3px;
  font-size: 11px;
  background: #f1f5f9;
  color: #475569;
}

.article-actions {
  padding: 10px 16px;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 8px;
  background: var(--bg-alt);
}

.action-btn {
  flex: 1;
  font-size: 13px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-icon {
  width: 14px;
  height: 14px;
}

.btn-danger {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.btn-danger:hover {
  background: #fee2e2;
}

@media (max-width: 1024px) {
  .header {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  
  .stats-row {
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }
  
  .stat-item {
    padding: 8px 16px;
    min-width: auto;
  }
  
  .stat-divider {
    display: none;
  }
  
  .stat-value {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .articles-grid {
    grid-template-columns: 1fr;
  }

  .article-card {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 200px;
  }
  
  .header {
    padding: 12px;
  }
  
  .stat-item {
    padding: 6px 12px;
  }
  
  .stat-value {
    font-size: 18px;
  }
  
  .stat-label {
    font-size: 12px;
  }
}
</style>
