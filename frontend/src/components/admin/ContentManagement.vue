<template>
  <div class="content-management">
    <!-- 标签页 -->
    <AdminTabs :tabs="tabs" v-model="activeTab" />

    <!-- 文章管理 -->
    <div v-if="activeTab === 'articles'" class="tab-content">
      <div class="search-filter">
        <input 
          type="text" 
          v-model="articleSearchQuery" 
          placeholder="搜索文章标题" 
          class="search-input form-input"
        >
        <select v-model="articleStatusFilter" class="filter-select form-input form-select">
          <option value="">所有状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <span class="item-count">共 {{ articleTotalElements }} 篇</span>
      </div>

      <!-- 文章列表 -->
      <div class="cards-grid">
        <div v-for="article in paginatedArticles" :key="article.id" class="article-card card card-hover">
          <div class="article-cover" v-if="article.cover">
            <img :src="imageUrl(article.cover)" :alt="article.title" class="cover-image">
          </div>
          <div class="article-content">
            <div class="article-body">
              <div class="article-top">
                <h3 class="article-title" @click="viewArticle(article.id)">{{ article.title }}</h3>
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
              <button class="action-btn btn btn-sm btn-secondary" @click="viewArticle(article.id)" title="查看">
                👁️ 查看
              </button>
              <button class="action-btn btn btn-sm btn-secondary" @click="deleteArticle(article.id)" title="删除">
                🗑️ 删除
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页控件 -->
      <div v-if="totalArticlePages > 1" class="pagination">
        <button 
          class="page-btn" 
          :disabled="articleCurrentPage === 1" 
          @click="goToArticlePage(articleCurrentPage - 1)"
        >
          上一页
        </button>
        <span class="page-info">第 {{ articleCurrentPage }} / {{ totalArticlePages }} 页</span>
        <button 
          class="page-btn" 
          :disabled="articleCurrentPage === totalArticlePages" 
          @click="goToArticlePage(articleCurrentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 评论管理 -->
    <div v-if="activeTab === 'comments'" class="tab-content">
      <div class="search-filter">
        <input 
          type="text" 
          v-model="commentSearchQuery" 
          placeholder="搜索文章标题" 
          class="search-input form-input"
        >
        <select v-model="commentStatusFilter" class="filter-select form-input form-select">
          <option value="">所有状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <span class="item-count">共 {{ commentTotalElements }} 条</span>
      </div>

      <!-- 评论列表 -->
      <div class="cards-grid">
        <div v-for="comment in paginatedComments" :key="comment.id" class="comment-card card card-hover">
          <div class="comment-header">
            <div class="comment-id">#{{ comment.id }}</div>
            <div class="comment-status-badges">
                <span 
                  class="status-badge" 
                  :class="getCommentStatusClass(comment.status)"
                  v-if="comment.status === 3 && comment.rejectReason"
                  @click="openFullRejectReason(comment.rejectReason)"
                  style="cursor: pointer;"
                >
                  查看驳回原因
                </span>
                <span class="status-badge" :class="getCommentStatusClass(comment.status)" v-else>
                  {{ getCommentStatusText(comment.status) }}
                </span>
              </div>
            </div>
            
            <div class="comment-body">
              <p class="comment-content">{{ comment.content }}</p>
            </div>
          
          <div class="comment-info">
            <div class="info-row">
              <div class="info-item">
                <span class="info-icon">👤</span>
                <span class="info-text">{{ comment.userNickname || comment.userUsername || '未知用户' }}</span>
              </div>
              <div class="info-item">
                <span class="info-icon">📄</span>
                <span class="info-text">{{ comment.articleTitle || '未知文章' }}</span>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item">
                <span class="info-icon">🕐</span>
                <span class="info-text">{{ formatDate(comment.createdAt) }}</span>
              </div>
            </div>
          </div>
          
          <div class="comment-actions">
            <button class="btn btn-sm btn-secondary" @click="deleteComment(comment.id)">
              🗑️ 删除
            </button>
          </div>
        </div>
      </div>

      <!-- 分页控件 -->
      <div v-if="totalCommentPages > 1" class="pagination">
        <button 
          class="page-btn" 
          :disabled="commentCurrentPage === 1" 
          @click="goToCommentPage(commentCurrentPage - 1)"
        >
          上一页
        </button>
        <span class="page-info">第 {{ commentCurrentPage }} / {{ totalCommentPages }} 页</span>
        <button 
          class="page-btn" 
          :disabled="commentCurrentPage === totalCommentPages" 
          @click="goToCommentPage(commentCurrentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 消息提示 -->
    <div 
      v-if="showMessage" 
      class="message" 
      :class="messageType"
    >
      <span class="message-icon">{{ messageType === 'success' ? '✓' : '✕' }}</span>
      <span class="message-text">{{ messageText }}</span>
      <button class="message-close" @click="showMessage = false">×</button>
    </div>

    <!-- 删除确认框 -->
    <div v-if="showDeleteDialog" class="delete-dialog-overlay" @click.self="cancelDelete">
      <div class="delete-dialog">
        <div class="delete-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 9V13M12 17H12.01M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="#ef5350" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h3 class="delete-title">确认删除</h3>
        <p class="delete-message">您确定要删除{{ deleteType === 'article' ? '文章' : '评论' }}吗？</p>
        <p class="delete-warning">此操作不可撤销！</p>
        <div class="delete-buttons">
          <button class="btn btn-secondary" @click="cancelDelete">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 驳回原因弹窗 -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { get, del, API_BASE_URL } from '../../utils/api'
import AdminTabs from './AdminTabs.vue'


interface Tab {
  id: string
  label: string
  icon: string
}

interface Article {
  id: number
  title: string
  cover?: string
  category?: string
  tags?: string
  status: number
  views?: number
  likes?: number
  comments?: number
  createdAt: string
  rejectReason?: string
  [key: string]: any
}

interface Comment {
  id: number
  content: string
  status: number
  userNickname?: string
  userUsername?: string
  articleTitle?: string
  createdAt: string
  rejectReason?: string
  [key: string]: any
}

const tabs: Tab[] = [
  { id: 'articles', label: '文章管理', icon: '📄' },
  { id: 'comments', label: '评论管理', icon: '💬' }
];

const router = useRouter();
const activeTab = ref<'articles' | 'comments'>('articles');

// 文章相关
const articles = ref<Article[]>([]);
const articleSearchQuery = ref('');
const articleStatusFilter = ref('');
const articleCurrentPage = ref(1);
const articleTotalPages = ref(1);
const articleTotalElements = ref(0);
const pageSize = 15;

// 评论相关
const comments = ref<Comment[]>([]);
const commentSearchQuery = ref('');
const commentStatusFilter = ref('');
const commentCurrentPage = ref(1);
const commentTotalPages = ref(1);
const commentTotalElements = ref(0);

// 消息相关
const showMessage = ref(false);
const messageText = ref('');
const messageType = ref<'success' | 'error'>('success');

// 删除相关
const showDeleteDialog = ref(false);
const deleteType = ref<'article' | 'comment' | null>(null);
const deleteId = ref<number | null>(null);

// 驳回原因弹窗
const showFullRejectModal = ref(false);
const fullRejectReason = ref('');
const openFullRejectReason = (reason: string) => {
  fullRejectReason.value = reason;
  showFullRejectModal.value = true;
};

// 将状态字符串转为数字（文章）
const articleStatusToNumber = (statusStr: string): number | null => {
  const map: Record<string, number> = {
    'PENDING': 1,
    'APPROVED': 3,
    'REJECTED': 2
  };
  return map[statusStr] ?? null;
};

// 将状态字符串转为数字（评论）
const commentStatusToNumber = (statusStr: string): number | null => {
  const map: Record<string, number> = {
    'PENDING': 1,
    'APPROVED': 2,
    'REJECTED': 3
  };
  return map[statusStr] ?? null;
};

// 文章分页 - 使用后端分页数据
const paginatedArticles = computed(() => articles.value);
const totalArticlePages = computed(() => articleTotalPages.value);

// 评论分页 - 使用后端分页数据
const paginatedComments = computed(() => comments.value);
const totalCommentPages = computed(() => commentTotalPages.value);

// 监听筛选变化，重置页码并重新获取数据
watch([articleSearchQuery, articleStatusFilter], () => {
  articleCurrentPage.value = 1;
  fetchArticles();
});

watch([commentSearchQuery, commentStatusFilter], () => {
  commentCurrentPage.value = 1;
  fetchComments();
});

// 分页跳转 - 重新获取数据
const goToArticlePage = (page: number) => {
  if (page >= 1 && page <= totalArticlePages.value) {
    articleCurrentPage.value = page;
    fetchArticles();
  }
};

const goToCommentPage = (page: number) => {
  if (page >= 1 && page <= totalCommentPages.value) {
    commentCurrentPage.value = page;
    fetchComments();
  }
};

const getStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '待审核',
    2: '已驳回',
    3: '已发布'
  };
  return statusMap[status] || '未知';
};

const getStatusClass = (status: number): string => {
  const classMap: Record<number, string> = {
    0: 'status-draft',
    1: 'status-pending',
    2: 'status-rejected',
    3: 'status-published'
  };
  return classMap[status] || '';
};

const getCommentStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    1: '待审核',
    2: '已通过',
    3: '已驳回'
  };
  return statusMap[status] || '未知';
};

const getCommentStatusClass = (status: number): string => {
  const classMap: Record<number, string> = {
    1: 'status-pending',
    2: 'status-published',
    3: 'status-rejected'
  };
  return classMap[status] || '';
};

const formatDate = (dateStr: any): string => {
  if (!dateStr) return '';
  try {
    let date: Date;
    if (typeof dateStr === 'string') {
      date = new Date(dateStr);
    } else if (dateStr.date && dateStr.time) {
      const { year, month, day } = dateStr.date;
      const { hour, minute, second } = dateStr.time;
      date = new Date(year, month - 1, day, hour, minute, second);
    } else {
      return '';
    }
    
    if (isNaN(date.getTime())) return '';
    
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  } catch (e) {
    return '';
  }
};

// 获取图片URL
const imageUrl = (path: string) => path ? API_BASE_URL + path : '';

// 获取文章列表 - 使用后端分页
const fetchArticles = async () => {
  try {
    const params: any = {
      page: articleCurrentPage.value - 1, // 后端从0开始
      size: pageSize
    };
    if (articleSearchQuery.value) {
      params.keyword = articleSearchQuery.value;
    }
    if (articleStatusFilter.value) {
      const statusNum = articleStatusToNumber(articleStatusFilter.value);
      if (statusNum !== null) {
        params.status = statusNum.toString();
      }
    }
    const response = await get('/api/article/all/paginated', params);
    articles.value = response.content || [];
    articleTotalPages.value = response.totalPages || 1;
    articleTotalElements.value = response.totalElements || 0;
  } catch (error: any) {
    console.error('获取文章列表失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '获取文章列表失败';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  }
};

// 获取评论列表 - 使用后端分页
const fetchComments = async () => {
  try {
    const params: any = {
      page: commentCurrentPage.value - 1, // 后端从0开始
      size: pageSize
    };
    if (commentSearchQuery.value) {
      params.keyword = commentSearchQuery.value;
    }
    if (commentStatusFilter.value) {
      const statusNum = commentStatusToNumber(commentStatusFilter.value);
      if (statusNum !== null) {
        params.status = statusNum.toString();
      }
    }
    const response = await get('/api/comment/all/paginated', params);
    comments.value = response.content || [];
    commentTotalPages.value = response.totalPages || 1;
    commentTotalElements.value = response.totalElements || 0;
  } catch (error: any) {
    console.error('获取评论列表失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '获取评论列表失败';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  }
};

// 查看文章
const viewArticle = (articleId: number) => {
  router.push(`/article/${articleId}`);
};

// 删除文章
const deleteArticle = (articleId: number) => {
  deleteType.value = 'article';
  deleteId.value = articleId;
  showDeleteDialog.value = true;
};

// 删除评论
const deleteComment = (commentId: number) => {
  deleteType.value = 'comment';
  deleteId.value = commentId;
  showDeleteDialog.value = true;
};

// 确认删除
const confirmDelete = async () => {
  if (!deleteId.value || !deleteType.value) return;

  try {
    if (deleteType.value === 'article') {
      await del(`/api/article/${deleteId.value}`);
      messageText.value = '文章删除成功';
      fetchArticles(); // 重新获取数据
    } else {
      await del(`/api/comment/${deleteId.value}`);
      messageText.value = '评论删除成功';
      fetchComments(); // 重新获取数据
    }
    messageType.value = 'success';
    showMessage.value = true;
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } catch (error: any) {
    console.error('删除失败:', error);
    messageText.value = error.message || '删除失败';
    messageType.value = 'error';
    showMessage.value = true;
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } finally {
    showDeleteDialog.value = false;
    deleteType.value = null;
    deleteId.value = null;
  }
};

// 取消删除
const cancelDelete = () => {
  showDeleteDialog.value = false;
  deleteType.value = null;
  deleteId.value = null;
};

// 组件挂载
onMounted(() => {
  fetchArticles();
  fetchComments();
});

// 切换标签时重新获取数据
watch(activeTab, (newTab) => {
  if (newTab === 'articles') {
    fetchArticles();
  } else {
    fetchComments();
  }
});
</script>

<style scoped>
.content-management {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-3xl);
  box-shadow: var(--shadow);
  width: 100%;
  font-family: var(--sans);
}



.tab-content {
  min-height: 500px;
}

.search-filter {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
  align-items: center;
}

.search-input {
  flex: 1 1 200px;
  min-width: 0;
}

.filter-select {
  flex: 0 0 140px;
  width: 140px;
}

.item-count {
  font-size: 13px;
  color: var(--text-m);
  white-space: nowrap;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

/* ====== Article Card ====== */
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
  cursor: pointer;
  transition: color 0.2s;
}

.article-title:hover {
  color: var(--primary);
}

.reject-reason {
  padding: 8px 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  margin-bottom: 10px;
  font-size: 13px;
}

.reject-label {
  font-weight: 600;
  color: #dc2626;
}

.reject-text {
  color: #991b1b;
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

/* ====== Comment Card ====== */
.comment-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--bg-alt);
  border-bottom: 1px solid var(--border);
}

.comment-id {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-m);
}

.comment-status-badges {
  display: flex;
  gap: 6px;
}

.comment-body {
  padding: 12px 14px;
}

.comment-content {
  font-size: 14px;
  color: var(--text);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.comment-info {
  padding: 10px 14px;
  background: var(--bg-alt);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}

.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-m);
}

.info-icon {
  font-size: 13px;
}

.info-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-actions {
  padding: 10px 14px;
  display: flex;
  gap: 8px;
  background: var(--bg-alt);
}

/* Status Badges */
.status-badge {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.status-badge.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.status-badge.status-published {
  background: #dcfce7;
  color: #16a34a;
}

.status-badge.status-rejected {
  background: #fee2e2;
  color: #dc2626;
}



/* 消息提示样式 */
.message {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing) var(--spacing-lg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  position: fixed;
  top: var(--spacing-lg);
  right: var(--spacing-lg);
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
}

.message.success {
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
  color: white;
}

.message.error {
  background: var(--danger-gradient);
  color: white;
}

.message-close {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 删除确认框样式 */
.delete-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease-out;
}

.delete-dialog {
  background: var(--bg);
  border-radius: var(--radius-xl);
  padding: var(--spacing-3xl);
  max-width: 420px;
  width: 90%;
  text-align: center;
  box-shadow: var(--shadow-xl);
  animation: scaleIn 0.3s ease-out;
}

.delete-icon {
  margin-bottom: var(--spacing-xl);
  animation: shake 0.5s ease-in-out;
}

.delete-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-h);
  margin-bottom: var(--spacing);
}

.delete-message {
  font-size: 16px;
  color: var(--text);
  margin-bottom: var(--spacing-xs);
  line-height: 1.6;
}

.delete-warning {
  font-size: 14px;
  color: var(--warning);
  margin-bottom: var(--spacing-2xl);
}

.delete-buttons {
  display: flex;
  gap: var(--spacing);
  justify-content: center;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing);
  margin-top: var(--spacing-xl);
}

.page-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  background: var(--bg-alt);
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-m);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-management {
    padding: 20px;
  }
  
  .search-filter {
    flex-direction: column;
    flex-wrap: wrap;
  }
  
  .search-input {
    min-width: auto;
  }
  
  .item-count {
    width: 100%;
    text-align: right;
    margin-top: 8px;
  }
  
  .cards-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing);
  }

  .article-card {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 200px;
  }
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
  background: white;
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
</style>
