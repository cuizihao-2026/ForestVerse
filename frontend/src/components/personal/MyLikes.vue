<template>
  <div class="my-likes">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else>
      <div v-if="articles.length === 0" class="empty-state">
        <div class="empty-icon">❤️</div>
        <h3>暂无喜欢的文章</h3>
        <p>去探索一下吧！</p>
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
              <button class="action-btn btn btn-primary" @click="viewArticle(article)" title="查看">
                👁️ 查看文章
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
}
</style>