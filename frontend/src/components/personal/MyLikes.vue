<template>
  <div class="my-likes">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else>
      <div v-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
          </svg>
        </div>
        <h3>暂无喜欢的文章</h3>
        <p>去探索一下吧！</p>
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
                查看文章
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { get, API_BASE_URL } from '../../utils/api';

const router = useRouter();

interface Article {
  id?: number;
  userId?: number;
  title: string;
  cover?: string;
  status?: number;
  category?: string;
  tags?: string;
  views?: number;
  likes?: number;
  comments?: number;
  content?: string;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string;
}

const articles = ref<Article[]>([]);
const loading = ref(false);

const formatRelativeTime = (dateStr: any): string => {
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

    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    const months = Math.floor(days / 30);
    const years = Math.floor(days / 365);

    if (seconds < 60) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 30) return `${days}天前`;
    if (months < 12) return `${months}个月前`;
    return `${years}年前`;
  } catch (e) {
    return '';
  }
};

const loadLikedArticles = async () => {
  loading.value = true;
  try {
    const data = await get('/api/article/my/liked');
    if (Array.isArray(data)) {
      articles.value = data;
    } else {
      articles.value = [];
    }
  } catch (error) {
    articles.value = [];
  } finally {
    loading.value = false;
  }
};

const viewArticle = (article: Article) => {
  router.push(`/article/${article.id}`);
};

onMounted(() => {
  loadLikedArticles();
});
</script>

<style scoped>
.my-likes {
  width: 100%;
  font-family: var(--sans);
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
}
</style>