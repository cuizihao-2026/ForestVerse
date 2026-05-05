<template>
  <div class="audit-center">

    <AdminTabs v-if="!auditingArticle" :tabs="tabs" v-model="activeTab" />

    <div class="tab-content">
      <div v-if="auditingArticle" class="audit-view">
        <ArticleAudit 
          :article="auditingArticle"
          @cancel="cancelAudit"
          @complete="completeAudit"
        />
      </div>
      <div v-else-if="activeTab === 'article'" class="article-audit">
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else>
          <div v-if="articles.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <h3>暂无待审核文章</h3>
            <p>目前没有需要审核的文章</p>
          </div>

          <div v-else>
            <div v-if="aiAuditEnabled" class="batch-actions">
              <button class="btn btn-info" @click="handleBatchAIAudit" :disabled="batchAuditing">
                {{ batchAuditing ? '🤖 AI批量审核中...' : '🤖 AI批量审核所有' }}
              </button>
              <span v-if="batchAuditProgress !== null" class="batch-progress">
                已审核 {{ batchAuditProgress }}/{{ articles.length }}
              </span>
            </div>

            <div class="articles-list">
              <div v-for="article in articles" :key="article.id" class="article-card card card-hover">
                <div class="article-cover" v-if="article.cover">
                  <img :src="API_BASE_URL + article.cover" :alt="article.title" class="cover-image">
                </div>
                <div class="article-content">
                  <div class="article-body">
                    <div class="article-top">
                      <h3 class="article-title">{{ article.title }}</h3>
                      <div class="article-badges">
                        <span class="article-status status-pending">待审核</span>
                        <span v-if="article.category" class="article-category">{{ article.category }}</span>
                      </div>
                    </div>
                    <div v-if="article.tags" class="article-tags">
                      <span class="tag" v-for="tag in article.tags.split(',').slice(0, 4)" :key="tag">{{ tag.trim() }}</span>
                    </div>
                    <div class="article-meta">
                      <div class="meta-left">
                        <span class="meta-item">📅 {{ formatDate(article.createdAt) }}</span>
                        <span class="meta-item">👤 用户ID: {{ article.userId }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="article-actions">
                    <button class="action-btn btn btn-sm btn-primary" @click="startAudit(article)" title="开始审核">
                      🔍 开始审核
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!auditingArticle && activeTab === 'comment'" class="comment-audit">
        <CommentAudit />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { get, post, API_BASE_URL } from '../../utils/api';
import ArticleAudit from './ArticleAudit.vue';
import CommentAudit from './CommentAudit.vue';
import AdminTabs from './AdminTabs.vue';
import { showModal, showConfirm } from '../../stores/modal';

interface Article {
  id?: number;
  userId?: number;
  title: string;
  cover?: string;
  status: number;
  rejectReason?: string;
  category?: string;
  tags?: string;
  views?: number;
  likes?: number;
  comments?: number;
  content: string;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string;
}

interface Tab {
  id: string
  label: string
  icon: string
}

const tabs: Tab[] = [
  { id: 'article', label: '文章审核', icon: '📝' },
  { id: 'comment', label: '评论审核', icon: '💬' }
]

const activeTab = ref('article');
const articles = ref<Article[]>([]);
const loading = ref(false);
const auditingArticle = ref<Article | null>(null);
const batchAuditing = ref(false);
const batchAuditProgress = ref<number | null>(null);
const aiAuditEnabled = ref(false);

const loadArticles = async () => {
  loading.value = true;
  try {
    const data = await get('/api/article/list?status=1');
    if (Array.isArray(data)) {
      articles.value = data as Article[];
    } else {
      articles.value = [];
    }
  } catch (error) {
    console.error('加载文章失败:', error);
    articles.value = [];
  } finally {
    loading.value = false;
  }
};

const loadSettings = async () => {
  try {
    const data = await get('/api/settings');
    aiAuditEnabled.value = data.aiArticleAuditEnabled || false;
  } catch (error) {
    console.error('加载设置失败:', error);
  }
};

const startAudit = async (article: Article) => {
  if (!article.id) {
    console.error('文章ID不存在');
    return;
  }
  try {
    loading.value = true;
    const articleDetail = await get(`/api/article/${article.id}`);
    auditingArticle.value = articleDetail as Article;
  } catch (error) {
    console.error('获取文章详情失败:', error);
  } finally {
    loading.value = false;
  }
};

const cancelAudit = () => {
  auditingArticle.value = null;
};

const completeAudit = async () => {
  auditingArticle.value = null;
  await loadArticles();
};

const handleBatchAIAudit = async () => {
  if (articles.value.length === 0) return;

  const confirmed = await showConfirm({
    title: '确认批量AI审核',
    message: `确定要使用AI自动审核这 ${articles.value.length} 篇文章吗？`,
    type: 'warning',
    confirmText: '确认审核',
    cancelText: '取消'
  });

  if (!confirmed) return;

  batchAuditing.value = true;
  batchAuditProgress.value = 0;

  try {
    for (let i = 0; i < articles.value.length; i++) {
      const article = articles.value[i];
      if (!article.id) continue;
      
      try {
        await post(`/api/ai/audit/${article.id}`);
      } catch (e) {
        console.error(`审核文章 ${article.id} 失败:`, e);
      }
      
      batchAuditProgress.value = i + 1;
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    showModal({
      title: '完成',
      message: '批量AI审核完成',
      type: 'success'
    });
    
    await loadArticles();
  } catch (error) {
    console.error('批量审核失败:', error);
    showModal({
      title: '失败',
      message: '批量审核失败',
      type: 'error'
    });
  } finally {
    batchAuditing.value = false;
    batchAuditProgress.value = null;
  }
};

const formatDate = (dateStr: string | undefined) => {
  if (!dateStr) return '';
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  } catch (e) {
    return '';
  }
};

const handleArticleAudited = (event: any) => {
  console.log('收到文章审核通过事件（管理后台），刷新待审核列表', event?.detail);
  loadArticles();
}

onMounted(() => {
  loadArticles();
  loadSettings();
  window.addEventListener('article-approved', handleArticleAudited);
});

onUnmounted(() => {
  window.removeEventListener('article-approved', handleArticleAudited);
});
</script>

<style scoped>
.audit-center {
  width: 100%;
  font-family: var(--sans);
  height: 100%;
}

.tab-content {
  padding: var(--spacing-md) 0;
  height: 100%;
}

.audit-view {
  height: 100%;
  margin: -40px;
  width: calc(100% + 80px);
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

.articles-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.article-card {
  display: flex;
  overflow: hidden;
}

.card-hover:hover {
  box-shadow: 0 10px 30px rgba(0,0,0,0.12);
}

.article-cover {
  width: 200px;
  flex-shrink: 0;
  background: var(--bg-alt);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.card-hover:hover .cover-image {
  transform: scale(1.05);
}

.article-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px;
}

.article-body {
  flex: 1;
}

.article-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.article-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-h);
  margin: 0;
  line-height: 1.4;
  text-align: left;
}

.article-badges {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.article-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.article-status.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.article-category {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
  white-space: nowrap;
}

.article-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  background: #f1f5f9;
  color: #64748b;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-left {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 13px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

@media (max-width: 768px) {
  .article-card {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 200px;
  }

  .article-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .article-badges {
    justify-content: flex-start;
  }

  .article-actions {
    justify-content: center;
  }

  .batch-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
