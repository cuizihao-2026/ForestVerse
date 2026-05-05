<template>
  <div class="my-articles">
    <div class="header" v-if="!isEditing">
      <button class="btn btn-primary" @click="startCreate">
        + 写文章
      </button>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 编辑界面 -->
    <div v-else-if="isEditing">
      <ArticleEditor 
        :article="currentArticle" 
        @cancel="cancelEdit"
        @save="onEditorSave"
      />
    </div>

    <!-- 列表界面 -->
    <div v-else>
      <div v-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <h3>暂无文章</h3>
        <p>还没有文章，快去写一篇吧！</p>
      </div>

      <div v-else class="articles-grid">
        <div v-for="article in articles" :key="article.id" class="article-card card card-hover">
          <div class="article-cover" v-if="article.cover">
            <img :src="API_BASE_URL + article.cover" :alt="article.title" class="cover-image">
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
                  <span class="meta-item">👁️ {{ article.views || 0 }}</span>
                  <span class="meta-item">❤️ {{ article.likes || 0 }}</span>
                  <span class="meta-item">💬 {{ article.comments || 0 }}</span>
                </div>
                <span class="meta-time">{{ formatDate(article.createdAt) }}</span>
              </div>
            </div>
            <div class="article-actions">
              <button class="action-btn btn btn-sm btn-secondary" @click="viewArticle(article)" title="查看">
                👁️ 查看
              </button>
              <button class="action-btn btn btn-sm btn-secondary" @click="startEdit(article)" title="编辑">
                ✏️ 编辑
              </button>
              <button class="action-btn btn btn-sm btn-secondary" @click="deleteArticle(article)" title="删除">
                🗑️ 删除
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get, del, API_BASE_URL } from '../../utils/api'
import { showModal } from '../../stores/modal'
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

const formatDate = (dateStr: any): string => {
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
    
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (e) {
    return ''
  }
}

const loadArticles = async () => {
  loading.value = true
  try {
    const data = await get('/api/article/my')
    if (Array.isArray(data)) {
      articles.value = data
    } else {
      articles.value = []
    }
  } catch (error) {
    articles.value = []
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
  font-family: var(--sans);
}

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: var(--spacing-2xl);
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
  font-size: 48px;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
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
  gap: 3px;
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
    flex-direction: column;
    gap: var(--spacing-md);
  }
}
</style>
