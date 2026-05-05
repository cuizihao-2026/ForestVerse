<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get } from '../utils/api'
import { View } from '@element-plus/icons-vue'
import Navbar from '../components/home/Navbar.vue'
import RichContentView from '../components/common/RichContentView.vue'
import ArticleHero from '../components/article/ArticleHero.vue'
import ArticleSidebar from '../components/article/ArticleSidebar.vue'

const route = useRoute()
const router = useRouter()

const article = ref<any>(null)
const loading = ref(true)
const error = ref('')
const coverExpanded = ref(false)
const richContentViewRef = ref()
const commentCount = ref(0)
const isArticleUnpublished = ref(false)
const unpublishedInfo = ref<any>(null)

const headings = computed(() => {
  if (richContentViewRef.value?.headings) {
    return richContentViewRef.value.headings
  }
  if (!article.value?.content) return []
  const result: Array<{ id: string; level: number; text: string }> = []
  let idCounter = 0
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = article.value.content
  const headingTags = tempDiv.querySelectorAll('h1, h2, h3, h4, h5, h6')
  headingTags.forEach((h) => {
    idCounter++
    result.push({
      id: `heading-${idCounter}`,
      level: parseInt(h.tagName.charAt(1)),
      text: h.textContent || ''
    })
  })
  return result
})

const scrollToHeading = (id: string) => {
  const element = document.getElementById(id)
  const articleMain = document.querySelector('.article-main')
  if (element && articleMain) {
    articleMain.scrollTo({
      top: element.offsetTop - 20,
      behavior: 'smooth'
    })
  }
}

const loadArticle = async () => {
  loading.value = true
  error.value = ''
  isArticleUnpublished.value = false
  unpublishedInfo.value = null
  
  try {
    const id = route.params.id
    article.value = await get(`/api/article/${id}`)
  } catch (err: any) {
    const errMsg = err?.message || ''
    if (errMsg.includes('文章尚未发布') || errMsg.includes('已被驳回')) {
      isArticleUnpublished.value = true
      unpublishedInfo.value = { articleTitle: '', newStatus: -1 }
    } else {
      error.value = errMsg || '加载文章失败'
    }
  } finally {
    loading.value = false
  }
}

const handleArticleApproved = (event: any) => {
  const { articleId } = event.detail;
  console.log('收到文章审核通过事件:', { articleId, currentArticleId: route.params.id });
  if (Number(articleId) === Number(route.params.id)) {
    console.log('文章审核通过，刷新文章');
    loadArticle();
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '草稿'
    case 1:
      return '待审核'
    case 2:
      return '已驳回'
    case 3:
      return '已发布'
    default:
      return '未知状态'
  }
}

const handleArticleUnpublished = (event: any) => {
  const { articleId, articleTitle, newStatus } = event.detail;
  console.log('收到文章下架事件:', { articleId, currentArticleId: route.params.id, articleTitle, newStatus });
  if (Number(articleId) === Number(route.params.id)) {
    console.log('当前文章已下架');
    isArticleUnpublished.value = true;
    unpublishedInfo.value = { articleTitle, newStatus };
  }
};

onMounted(() => {
  loadArticle();
  window.addEventListener('article-approved', handleArticleApproved);
  window.addEventListener('article-unpublished', handleArticleUnpublished);
});

onUnmounted(() => {
  window.removeEventListener('article-approved', handleArticleApproved);
  window.removeEventListener('article-unpublished', handleArticleUnpublished);
});
</script>

<template>
  <div class="article-page">
    <Navbar />

    <!-- Article Unpublished -->
    <div v-if="isArticleUnpublished" class="unpublished-wrap">
      <el-result
        icon="warning"
        title="文章已下架"
        :sub-title="unpublishedInfo?.articleTitle ? 
          `文章「${unpublishedInfo?.articleTitle}」当前状态: ${getStatusText(unpublishedInfo?.newStatus)}` : 
          '该文章当前无法访问'"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="loading-wrap">
      <el-icon class="loading-icon" :size="32"><View /></el-icon>
      <p>加载文章中...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-wrap">
      <p>{{ error }}</p>
      <el-button type="primary" @click="loadArticle">重试</el-button>
    </div>

    <!-- Article Layout -->
    <div v-else class="article-layout">
      <!-- 正文区 -->
      <div class="article-main">
        <!-- 封面 -->
        <ArticleHero
          v-if="article.cover"
          :cover="article.cover"
          :title="article.title"
          :category="article.category"
          @toggle="coverExpanded = !coverExpanded"
        />

        <!-- 标题区 -->
        <div class="title-section">
          <el-tag v-if="article.category && !article.cover" size="small" effect="plain" round class="title-cat">
            {{ article.category }}
          </el-tag>
          <h1 class="page-title">{{ article.title }}</h1>
        </div>

        <!-- 正文 -->
        <div class="article-body">
          <RichContentView ref="richContentViewRef" :content="article.content" />
        </div>

        <!-- 底部标签 -->
        <div v-if="article.tags" class="tags-row">
          <el-tag
            v-for="tag in article.tags.split(',').map((t: string) => t.trim()).filter((t: string) => t)"
            :key="tag"
            size="small"
            effect="plain"
            round
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 侧边栏 -->
      <ArticleSidebar
        :article="article"
        :headings="headings"
        v-model:commentCount="commentCount"
        @scroll-to-heading="scrollToHeading"
        @refresh="loadArticle"
      />
    </div>
  </div>
</template>

<style scoped>
.article-page {
  height: 100vh;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.loading-wrap,
.error-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 16px;
  color: #909399;
  font-size: 15px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.article-layout {
  display: flex;
  width: 100%;
  flex: 1;
  padding: 12px 32px 12px;
  gap: 24px;
  align-items: stretch;
  box-sizing: border-box;
  margin-top: 68px;
  overflow: hidden;
}

.article-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  overflow-y: auto;
  height: 100%;
  border: 2px solid #e0e7ff;
  box-sizing: border-box;
}

.title-section {
  padding: 36px 36px 0;
  border-bottom: 1px solid #e8ecf1;
  margin-bottom: 32px;
  padding-bottom: 24px;
}

.title-cat {
  display: block;
  margin-bottom: 16px;
  width: fit-content;
}

.page-title {
  font-size: 32px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  line-height: 1.3;
  letter-spacing: -0.5px;
}

.article-body {
  padding: 32px 36px;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 36px 20px;
}

@media (max-width: 860px) {
  .article-layout {
    flex-direction: column;
    padding: 12px;
  }

  .hero { height: 240px; }

  .title-section {
    padding: 24px 20px 20px;
  }

  .page-title {
    font-size: 24px;
  }

  .article-body {
    padding: 24px 20px;
    font-size: 16px;
  }

  .tags-row {
    padding: 0 20px 16px;
  }
}
</style>
