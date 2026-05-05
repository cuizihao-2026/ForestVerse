<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { get, API_BASE_URL } from '../../utils/api'
import { View, Star, Search, Loading } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const articles = ref<any[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(0)
const searchKeyword = ref('')
const pageSize = 12

const loadArticles = async (reset: boolean = true) => {
  if (reset) {
    loading.value = true
    currentPage.value = 0
    articles.value = []
  } else {
    loadingMore.value = true
  }

  try {
    let url = '/api/article/published/paginated'
    const params: any = { page: currentPage.value, size: pageSize, skipAuth: true }

    if (searchKeyword.value && searchKeyword.value.trim()) {
      url = '/api/article/search/paginated'
      params.keyword = searchKeyword.value.trim()
    }

    const data = await get(url, params)
    
    if (reset) {
      articles.value = data.content || []
    } else {
      articles.value = [...articles.value, ...(data.content || [])]
    }
    
    hasMore.value = (data.page || 0) + 1 < (data.totalPages || 0)
  } catch (err) {
    console.error('加载文章失败', err)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  loadArticles(false)
}

const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight

  if (scrollTop + clientHeight >= scrollHeight - 200) {
    loadMore()
  }
}

const handleSearch = () => {
  loadArticles(true)
}

const clearSearch = () => {
  searchKeyword.value = ''
  loadArticles(true)
}

const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSearch()
  }
}

const handleArticleApproved = (event: any) => {
  console.log('收到文章审核通过事件，刷新首页列表', event?.detail);
  loadArticles(true);
}

const handleArticleUnpublished = (event: any) => {
  console.log('收到文章下架事件，刷新首页列表', event?.detail);
  loadArticles(true);
}

onMounted(() => {
  loadArticles(true);
  window.addEventListener('article-approved', handleArticleApproved);
  window.addEventListener('article-unpublished', handleArticleUnpublished);
  window.addEventListener('scroll', handleScroll);
})

onUnmounted(() => {
  window.removeEventListener('article-approved', handleArticleApproved);
  window.removeEventListener('article-unpublished', handleArticleUnpublished);
  window.removeEventListener('scroll', handleScroll);
})

// 监听路由变化，回到首页时清除搜索状态
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/home') {
      searchKeyword.value = ''
      loadArticles(true)
    }
  }
)

const viewArticle = (article: any) => {
  router.push(`/article/${article.id}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const imageUrl = (path: string) => path ? API_BASE_URL + path : ''
</script>

<template>
  <div class="home-main">
    <!-- Hero Banner -->
    <div class="hero-banner">
      <div class="hero-content">
        <h1 class="hero-title">探索精彩内容</h1>
        <p class="hero-desc">发现优质文章，分享你的见解与故事</p>
        <div class="search-block">
          <div class="hero-search">
            <el-input
              v-model="searchKeyword"
              :prefix-icon="Search"
              placeholder="搜索文章标题、分类、标签..."
              clearable
              @keyup="handleKeyPress"
              @clear="clearSearch"
              class="search-input"
            />
            <el-button type="primary" @click="handleSearch" class="search-button">
              搜索
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Article Grid -->
    <div class="articles-section">

      <!-- Initial Loading -->
      <div v-if="loading" class="skeleton-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
              <div style="padding: 20px">
                <el-skeleton-item variant="text" style="width: 30%; margin-bottom: 10px" />
                <el-skeleton-item variant="text" style="width: 90%" />
                <el-skeleton-item variant="text" style="width: 60%; margin-bottom: 16px" />
                <div style="display: flex; justify-content: space-between">
                  <el-skeleton-item variant="text" style="width: 30%" />
                  <el-skeleton-item variant="text" style="width: 20%" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="articles.length === 0" class="empty-wrap">
        <el-empty description="暂无文章，敬请期待" />
      </div>

      <!-- Articles -->
      <div v-else>
        <div class="articles-grid">
          <el-card
            v-for="article in articles"
            :key="article.id"
            shadow="hover"
            class="article-card"
            :body-style="{ padding: '0' }"
            @click="viewArticle(article)"
          >
            <div v-if="article.cover" class="card-cover">
              <img :src="imageUrl(article.cover)" :alt="article.title" />
            </div>
            <div v-else class="card-cover card-cover--empty">
              <el-icon :size="40" color="#cbd5e1"><View /></el-icon>
            </div>

            <div class="card-body">
              <div class="card-meta">
                <el-tag v-if="article.category" size="small" effect="plain" round>
                  {{ article.category }}
                </el-tag>
                <span class="card-date">{{ formatDate(article.createdAt) }}</span>
              </div>

              <h3 class="card-title">{{ article.title }}</h3>

              <div class="card-stats">
                <span class="stat">
                  <el-icon :size="14"><View /></el-icon>
                  {{ article.views || 0 }}
                </span>
                <span class="stat">
                  <el-icon :size="14"><Star /></el-icon>
                  {{ article.likes || 0 }}
                </span>
              </div>
            </div>
          </el-card>
        </div>

        <!-- Loading More -->
        <div v-if="loadingMore" class="load-more-wrap">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- No More -->
        <div v-else-if="!hasMore && articles.length > 0" class="load-more-wrap">
          <span class="no-more-text">没有更多文章了</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-main {
  padding-top: 68px;
  background: #fff;
}

/* ====== Hero ====== */
.hero-banner {
  background: #ffffff;
  padding: 64px 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
  border-bottom: 3px solid #e2e8f0;
}

.hero-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(30deg, #e2e8f0 12%, transparent 12.5%, transparent 87%, #e2e8f0 87.5%, #e2e8f0),
    linear-gradient(150deg, #e2e8f0 12%, transparent 12.5%, transparent 87%, #e2e8f0 87.5%, #e2e8f0),
    linear-gradient(30deg, #e2e8f0 12%, transparent 12.5%, transparent 87%, #e2e8f0 87.5%, #e2e8f0),
    linear-gradient(150deg, #e2e8f0 12%, transparent 12.5%, transparent 87%, #e2e8f0 87.5%, #e2e8f0),
    linear-gradient(60deg, #dbeafe 25%, transparent 25.5%, transparent 75%, #dbeafe 75%, #dbeafe),
    linear-gradient(60deg, #dbeafe 25%, transparent 25.5%, transparent 75%, #dbeafe 75%, #dbeafe);
  background-size: 80px 140px;
  background-position: 0 0, 0 0, 40px 70px, 40px 70px, 0 0, 40px 70px;
  opacity: 0.5;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 600px;
  margin: 0 auto;
}

.hero-title {
  font-size: 36px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.hero-desc {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 28px;
}

.search-block {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.hero-search {
  display: flex;
  gap: 18px;
  align-items: center;
}

.search-input {
  flex: 1;
}

.search-button {
  padding: 14px 32px;
  font-size: 16px;
}

/* ====== Section ====== */
.articles-section {
    width: 100%;
    padding: 48px 32px 80px;
    box-sizing: border-box;
}

/* ====== Skeleton ====== */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.skeleton-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

/* ====== Empty ====== */
.empty-wrap {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

/* ====== Grid ====== */
.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.article-card {
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e2e8f0;
}

.article-card:hover {
  transform: translateY(-4px);
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-cover {
  height: 200px;
  overflow: hidden;
  background: #f1f5f9;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s;
}

.article-card:hover .card-cover img {
  transform: scale(1.05);
}

.card-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-body {
  padding: 20px 24px 24px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.card-date {
  font-size: 13px;
  color: #94a3b8;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
  line-height: 1.45;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-stats {
  display: flex;
  gap: 16px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #94a3b8;
}

/* ====== Load More ====== */
.load-more-wrap {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px 0;
    gap: 8px;
    color: #94a3b8;
    font-size: 14px;
}

.loading-icon {
    animation: spin 1s linear infinite;
    font-size: 20px;
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

.no-more-text {
    color: #cbd5e1;
}

/* ====== Responsive ====== */
@media (max-width: 768px) {
    .hero-banner {
        padding: 48px 20px;
    }

    .hero-title {
        font-size: 26px;
    }

    .hero-desc {
        font-size: 14px;
    }

    .hero-search {
        flex-direction: column;
        width: 100%;
    }

    .search-input,
    .search-button {
        width: 100%;
    }

    .articles-section {
        padding: 32px 16px 60px;
    }

    .skeleton-grid,
    .articles-grid {
        grid-template-columns: 1fr;
    }
}
</style>
