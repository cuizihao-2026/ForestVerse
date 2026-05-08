<script setup lang="ts">
import { computed, ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, View, List, ChatDotSquare, InfoFilled, MagicStick, ChatLineRound, Lock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { del, post } from '../../utils/api'
import { isLoggedIn, currentUser, getToken } from '../../stores/auth'
import { hasPermission } from '../../stores/permission'
import { sendFriendRequest, getFriends, getSentRequests, getReceivedRequests } from '../../utils/friends'
import CommentSection from '../common/CommentSection.vue'

const props = defineProps<{
  article: any
  headings: Array<{ id: string; level: number; text: string }>
}>()

const emit = defineEmits<{
  scrollToHeading: [id: string]
  refresh: []
}>()

const router = useRouter()
const activeTab = ref('info')
const commentCount = defineModel<number>('commentCount', { default: 0 })
const aiInput = ref('')
const aiMessages = ref<Array<{ role: 'user' | 'ai'; content: string }>>([])
const aiLoading = ref(false)
const aiEnabled = ref(false)
const streamEnabled = ref(false)
const hasAiAssistPermission = computed(() => hasPermission('ai:assist'))
const MAX_CHARACTERS = 100
const aiMessagesContainer = ref<HTMLDivElement>()  // 消息容器引用

// 渲染 Markdown（带 XSS 防护）
const renderMarkdown = (content: string) => {
  const html = marked.parse(content) as string
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'code', 'pre', 'ul', 'ol', 'li', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote'],
    ALLOWED_ATTR: ['href', 'title', 'target', 'rel']
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (aiMessagesContainer.value) {
      aiMessagesContainer.value.scrollTop = aiMessagesContainer.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动到底部
watch(aiMessages, () => {
  scrollToBottom()
}, { deep: true })

const isAlreadyFriend = ref(false)
const hasPendingRequest = ref(false)

const loadFriendStatus = async () => {
  if (!props.article?.userId || !isLoggedIn.value) return
  
  // 检查是否已经是好友
  try {
    const friendsData = await getFriends()
    if (friendsData?.data) {
      isAlreadyFriend.value = friendsData.data?.some((f: any) => f.id === props.article.userId) || false
    }
  } catch (err) {
    console.error('检查好友状态失败:', err)
  }
  
  // 检查是否有未处理的请求
  if (!isAlreadyFriend.value) {
    try {
      const [sentRequests, receivedRequests] = await Promise.all([
        getSentRequests(),
        getReceivedRequests()
      ])
      
      const hasSent = sentRequests.success && sentRequests.data?.some((r: any) => 
        r.receiverId === props.article.userId && r.status === 'PENDING'
      )
      const hasReceived = receivedRequests.success && receivedRequests.data?.some((r: any) => 
        r.senderId === props.article.userId && r.status === 'PENDING'
      )
      
      hasPendingRequest.value = hasSent || hasReceived
    } catch (err) {
      console.error('检查好友请求状态失败:', err)
    }
  }
}

const handleAuthorClick = () => {
  if (!props.article?.userId) return
  
  // 如果是自己，不做处理
  if (currentUser.value?.id === props.article.userId) return
  
  // 如果未登录，提示登录
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再添加好友', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login')
    }).catch(() => {})
    return
  }
  
  // 如果已经是好友
  if (isAlreadyFriend.value) {
    ElMessageBox.alert('您已经是对方的好友了', '提示', {
      confirmButtonText: '好的',
      type: 'info'
    })
    return
  }
  
  // 如果有未处理的请求
  if (hasPendingRequest.value) {
    ElMessageBox.alert('您和对方之间有未处理的好友请求', '提示', {
      confirmButtonText: '好的',
      type: 'info'
    })
    return
  }
  
  // 弹出添加好友请求
  ElMessageBox.prompt(
    `是否要添加 ${props.article.authorName || '该用户'} 为好友？`,
    '添加好友',
    {
      confirmButtonText: '发送请求',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入验证消息（可选）'
    }
  ).then(async ({ value }) => {
    try {
      await sendFriendRequest(props.article.userId, value || '')
      ElMessage.success('好友请求已发送')
      hasPendingRequest.value = true
    } catch (err: any) {
      console.error('发送好友请求失败:', err)
      const errorMsg = err.response?.data?.message || err.message || '发送好友请求失败'
      ElMessage.error(errorMsg)
    }
  }).catch(() => {})
}

const checkAiStatus = async () => {
  try {
    const response = await fetch('/api/settings/public/ai-config')
    const data = await response.json()
    aiEnabled.value = data.aiReadingEnabled || false
  } catch (err) {
    console.error('检查AI状态失败:', err)
    aiEnabled.value = false
  }
}

onMounted(() => {
  loadFriendStatus()
  checkAiStatus()
})

const displayDate = computed(() => {
  if (!props.article) return ''
  const d = props.article.publishedAt || props.article.createdAt
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/home')
  }
}

const toggleLike = async () => {
  if (!props.article) return

  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再进行点赞操作', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login');
    }).catch(() => {});
    return
  }

  try {
    const id = props.article.id
    if (props.article.isLiked) {
      await del(`/api/article/${id}/like`)
      props.article.likes = Math.max(0, (props.article.likes || 0) - 1)
      props.article.isLiked = false
      ElMessage.success('已取消点赞')
    } else {
      await post(`/api/article/${id}/like`)
      props.article.likes = (props.article.likes || 0) + 1
      props.article.isLiked = true
      ElMessage.success('点赞成功')
    }
  } catch (err: any) {
    console.error('点赞操作失败', err)
    const errorMsg = err.response?.data?.message || err.message || '点赞失败，请重试'
    ElMessage.error(errorMsg)
    emit('refresh')
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey) {
    // 仅按 Enter：发送
    e.preventDefault()
    sendAiMessage()
    return
  }
  if (e.key === 'Enter' && e.ctrlKey) {
    // Ctrl+Enter：换行
    return
  }
}

const extractSseData = (line: string): string | null => {
  if (line.startsWith('data: ')) return line.substring(6)
  if (line.startsWith('data:')) return line.substring(5)
  return null
}

const formatStreamText = (content: string): string => {
  // 先进行基本的 HTML 转义
  let escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/(^|\s)#{1,6}\s*/gm, '$1')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/(^|\s)[\-\*]\s+/gm, '$1• ')
    .replace(/\n/g, '<br>')
  
  // 再用 DOMPurify 进行二次消毒确保安全
  return DOMPurify.sanitize(escaped, {
    ALLOWED_TAGS: ['br'],
    ALLOWED_ATTR: []
  })
}

const sendAiMessage = async () => {
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再使用AI助读功能', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login');
    }).catch(() => {});
    return
  }
  
  if (!hasAiAssistPermission.value) {
    ElMessage.error('您的账号暂无AI助读功能权限');
    return;
  }
  
  if (!aiInput.value.trim()) return
  
  if (aiInput.value.length > MAX_CHARACTERS) {
    ElMessage.warning(`输入内容不能超过${MAX_CHARACTERS}字`)
    return
  }
  
  const userMessage = aiInput.value.trim()
  aiInput.value = ''
  
  aiMessages.value.push({ role: 'user', content: userMessage })
  const aiMessageIndex = aiMessages.value.length
  aiMessages.value.push({ role: 'ai', content: '' })
  aiLoading.value = true
  
  try {
    const convertedHistory = aiMessages.value.slice(0, -2).map(msg => ({
      role: msg.role === 'ai' ? 'assistant' : msg.role,
      content: msg.content
    }))
    
    const requestBody: any = {
      message: userMessage,
      history: convertedHistory,
      articleContent: props.article?.content || ''
    }
    
    if (streamEnabled.value) {
      await sendStreamRequest(requestBody, aiMessageIndex)
    } else {
      await sendNonStreamRequest(requestBody, aiMessageIndex)
    }
    
  } catch (err: any) {
    console.error('AI助读请求失败:', err)
    const errorMsg = err.message || 'AI助读请求失败'
    ElMessage.error(errorMsg)
    aiMessages.value = aiMessages.value.slice(0, -2)
  } finally {
    aiLoading.value = false
  }
}

const sendStreamRequest = async (requestBody: any, aiMessageIndex: number) => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify(requestBody)
  })
  
  if (!response.ok) {
    throw new Error(`请求失败: ${response.status}`)
  }
  
  const reader = response.body?.getReader()
  const decoder = new TextDecoder()
  
  if (reader) {
    let lineBuffer = ''
    let dataLines: string[] = []
    
    const processLine = (line: string) => {
      if (line === '') {
        if (dataLines.length > 0) {
          const content = dataLines.join('\n')
          if (aiMessages.value[aiMessageIndex]) {
            aiMessages.value[aiMessageIndex].content += content
          }
          dataLines = []
        }
        return
      }
      const data = extractSseData(line)
      if (data !== null) {
        dataLines.push(data)
      }
    }
    
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        if (lineBuffer) {
          processLine(lineBuffer)
        }
        if (dataLines.length > 0) {
          const content = dataLines.join('\n')
          if (aiMessages.value[aiMessageIndex]) {
            aiMessages.value[aiMessageIndex].content += content
          }
        }
        break
      }
      
      const chunk = decoder.decode(value, { stream: true })
      lineBuffer += chunk
      
      const lines = lineBuffer.split('\n')
      lineBuffer = lines.pop() || ''
      
      for (const line of lines) {
        processLine(line)
      }
    }
  }
}

const sendNonStreamRequest = async (requestBody: any, aiMessageIndex: number) => {
  const data = await post('/api/ai/chat', requestBody)
  if (data && aiMessages.value[aiMessageIndex]) {
    aiMessages.value[aiMessageIndex].content = data.data?.message || data.message || ''
  }
}
</script>

<template>
  <div class="article-sidebar">
    <!-- 返回按钮 -->
    <el-card shadow="never" class="sidebar-card back-btn-card">
      <el-button @click="goBack" type="primary" size="large" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回上一页
      </el-button>
    </el-card>

    <!-- 标签页卡片 -->
    <el-card shadow="never" class="sidebar-card tabs-card">
      <el-tabs v-model="activeTab" class="sidebar-tabs">
        <!-- 文章信息 -->
        <el-tab-pane label="文章信息" name="info">
          <template #label>
            <span class="tab-label">
              <el-icon><InfoFilled /></el-icon>
              文章信息
            </span>
          </template>
          <div class="info-tab-content">
            <!-- 作者 -->
            <div class="info-block">
              <div class="section-title">
                <el-icon><InfoFilled /></el-icon>
                作者
              </div>
              <div 
                class="author-block"
                :class="{ clickable: currentUser?.id !== article.userId }"
                @click="handleAuthorClick"
              >
                <img v-if="article.authorAvatar" :src="article.authorAvatar" :alt="article.authorName || '用户头像'" class="avatar huge border" />
                <div v-else class="avatar huge border fallback">
                  {{ (article.authorName || '匿')[0] }}
                </div>
                <div class="author-info">
                  <strong>{{ article.authorName || '匿名用户' }}</strong>
                  <span class="author-date">发布于 {{ displayDate }}</span>
                </div>
                <div v-if="currentUser?.id !== article.userId" class="add-friend-hint">
                  <el-icon><List /></el-icon>
                </div>
              </div>
            </div>

            <div class="divider"></div>

            <!-- 文章数据 -->
            <div class="info-block">
              <div class="section-title">
                <el-icon><View /></el-icon>
                文章数据
              </div>
              <div class="data-grid">
                <div class="data-item">
                  <div class="data-icon-wrapper view-icon">
                    <el-icon><View /></el-icon>
                  </div>
                  <div class="data-info">
                    <span class="data-value">{{ article.views || 0 }}</span>
                    <span class="data-label">浏览</span>
                  </div>
                </div>
                <div class="data-item">
                  <div class="data-icon-wrapper comment-icon">
                    <el-icon><ChatDotSquare /></el-icon>
                  </div>
                  <div class="data-info">
                    <span class="data-value">{{ commentCount }}</span>
                    <span class="data-label">评论</span>
                  </div>
                </div>
                <div class="data-item like-item" :class="{ liked: article.isLiked }" @click="toggleLike" role="button" tabindex="0" @keydown.enter="toggleLike" :aria-pressed="article.isLiked" :aria-label="article.isLiked ? '取消点赞' : '点赞'">
                  <div class="data-icon-wrapper like-icon">
                    <svg v-if="article.isLiked" class="heart-icon filled" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                    <svg v-else class="heart-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                  </div>
                  <div class="data-info">
                    <span class="data-value">{{ article.likes || 0 }}</span>
                    <span class="data-label">{{ article.isLiked ? '已喜欢' : '喜欢' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="divider"></div>

            <!-- 目录 -->
            <div class="info-block toc-block">
              <div class="section-title">
                <el-icon><List /></el-icon>
                目录
              </div>
              <div v-if="headings.length > 0" class="toc-list">
                <div
                  v-for="heading in headings"
                  :key="heading.id"
                  class="toc-item"
                  :class="`toc-level-${heading.level}`"
                  @click="emit('scrollToHeading', heading.id)"
                >
                  {{ heading.text }}
                </div>
              </div>
              <div v-else class="empty-toc">
                <span>暂无目录</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 评论 -->
        <el-tab-pane label="评论" name="comments">
          <template #label>
            <span class="tab-label">
              <el-icon><ChatDotSquare /></el-icon>
              评论
              <el-tag v-if="commentCount > 0" size="small" type="danger" class="comment-badge">{{ commentCount }}</el-tag>
            </span>
          </template>
          <div class="tab-content comment-tab-content">
            <CommentSection :articleId="article.id" @countChange="commentCount = $event" />
          </div>
        </el-tab-pane>

        <!-- AI助读 -->
        <el-tab-pane label="AI助读" name="ai">
          <template #label>
            <span class="tab-label">
              <el-icon><MagicStick /></el-icon>
              AI助读
            </span>
          </template>
          <div class="tab-content ai-tab-content">
            <!-- 未登录时的提示 -->
            <div v-if="!isLoggedIn" class="ai-disabled-container">
              <div class="ai-disabled-icon">
                <el-icon :size="48"><Lock /></el-icon>
              </div>
              <h3>请先登录</h3>
              <p>登录后即可使用AI助读功能</p>
              <el-button type="primary" @click="router.push('/login')">去登录</el-button>
            </div>
            <!-- 无权限时的提示 -->
            <div v-else-if="!hasAiAssistPermission" class="ai-disabled-container">
              <div class="ai-disabled-icon">
                <el-icon :size="48"><Lock /></el-icon>
              </div>
              <h3>无权限使用</h3>
              <p>您的账号暂无AI助读功能权限</p>
            </div>
            <!-- AI未启用时的提示 -->
            <div v-else-if="!aiEnabled" class="ai-disabled-container">
              <div class="ai-disabled-icon">
                <el-icon :size="48"><Lock /></el-icon>
              </div>
              <h3>AI功能未启用</h3>
              <p>请联系管理员在管理面板启用AI功能</p>
            </div>
            
            <!-- AI已启用且有权限时的聊天界面 -->
            <div v-else class="ai-chat-container" ref="aiMessagesContainer">
              <!-- 欢迎信息 -->
              <div v-if="aiMessages.length === 0" class="ai-welcome">
                <div class="ai-welcome-icon">
                  <img src="/src/assets/images/AI.jpg" alt="森小语" />
                </div>
                <h3>森小语</h3>
                <p>您好！我可以帮您：</p>
                <ul class="ai-welcome-tips">
                  <li><el-icon><ChatLineRound /></el-icon> 总结文章内容</li>
                  <li><el-icon><ChatLineRound /></el-icon> 解释关键概念</li>
                  <li><el-icon><ChatLineRound /></el-icon> 回答相关问题</li>
                  <li><el-icon><ChatLineRound /></el-icon> 提取文章要点</li>
                </ul>
                <div class="ai-quick-questions">
                  <el-button 
                    v-for="q in ['总结文章', '提取观点', '解释核心']" 
                    :key="q" 
                    size="small" 
                    @click="aiInput = q; sendAiMessage()"
                    plain
                    :disabled="q.length > MAX_CHARACTERS || aiLoading || !isLoggedIn || !hasAiAssistPermission"
                  >
                    {{ q }}
                  </el-button>
                </div>
              </div>

              <!-- 消息列表 -->
              <div v-else class="ai-messages">
                <div 
                  v-for="(msg, idx) in aiMessages" 
                  :key="idx" 
                  class="ai-message"
                  :class="msg.role"
                >
                  <div v-if="msg.role === 'ai'" class="ai-message-avatar">
                    <el-icon><MagicStick /></el-icon>
                  </div>
                  <div class="ai-message-content">
                    <div v-if="msg.role === 'ai' && aiLoading && idx === aiMessages.length - 1" class="ai-message-text">
                      <span v-if="streamEnabled" v-html="formatStreamText(msg.content)"></span>
                      <span class="typing-dots">
                        <span class="typing-dot"></span>
                        <span class="typing-dot"></span>
                        <span class="typing-dot"></span>
                      </span>
                    </div>
                    <div v-else-if="msg.role === 'ai'" class="ai-message-text" v-html="renderMarkdown(msg.content)"></div>
                    <div v-else class="ai-message-text">{{ msg.content }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入区域（仅AI启用且有权限时显示） -->
            <div v-if="aiEnabled && hasAiAssistPermission" class="ai-input-area">
              <div class="ai-input-actions">
                <el-switch
                  v-model="streamEnabled"
                  active-text="流式"
                  size="small"
                  :disabled="aiLoading"
                />
              </div>
              <el-input
                v-model="aiInput"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 5 }"
                placeholder="输入您的问题...（Enter发送）"
                @keydown="handleKeyDown"
                :disabled="aiLoading"
                class="ai-textarea"
                :maxlength="MAX_CHARACTERS"
                show-word-limit
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.article-sidebar {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  padding: 12px 12px 12px 0;
  overflow-y: auto;
  scrollbar-width: none;
}

.article-sidebar::-webkit-scrollbar {
  width: 0;
}

.sidebar-card {
  border-radius: 12px;
  flex-shrink: 0;
}

.back-btn-card {
  margin-bottom: 0;
}

.back-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 10px;
}

.tabs-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs-card :deep(.el-card__body) {
  flex: 1;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.sidebar-tabs :deep(.el-tabs__nav) {
  width: 100%;
  display: flex;
}

.sidebar-tabs :deep(.el-tabs__item) {
  flex: 1;
  text-align: center;
}

.sidebar-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.sidebar-tabs :deep(.el-tab-pane) {
  height: 100%;
  overflow-y: auto;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-content {
  padding: 16px;
}

.info-tab-content {
  padding: 12px;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  min-height: 0;
  scrollbar-width: none;
}

.info-tab-content::-webkit-scrollbar {
  width: 0;
}

.info-block {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.toc-block {
  flex: 1 1 auto;
  min-height: 200px;
  max-height: 500px;
  overflow-y: auto;
}

.toc-block::-webkit-scrollbar {
  width: 6px;
}

.toc-block::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.toc-block::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.toc-block::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
  font-size: 15px;
}

.divider {
  height: 1px;
  background: #e5e7eb;
  margin: 16px 0;
}

.comment-tab-content {
  padding: 0;
  height: 100%;
}

.ai-tab-content {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-chat-container {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 12px;
  min-height: 0;
  scrollbar-width: none;
}

.ai-chat-container::-webkit-scrollbar {
  width: 0;
}

.ai-welcome {
  text-align: center;
  padding: 24px 16px;
  color: #475569;
}

.ai-welcome-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.ai-welcome-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-welcome h3 {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.ai-welcome p {
  margin: 0 0 16px;
  font-size: 14px;
}

.ai-welcome-tips {
  list-style: none;
  padding: 0;
  margin: 0 0 20px;
  text-align: left;
}

.ai-welcome-tips li {
  padding: 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.ai-quick-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.ai-quick-questions .el-button {
  width: 100%;
  justify-content: center;
  margin: 0 !important;
}

.ai-messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-message {
  display: flex;
  gap: 12px;
}

.ai-message.ai {
  flex-direction: row;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-message.ai .ai-message-avatar {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
}

.ai-message.user .ai-message-avatar {
  background: #e5e7eb;
  color: #64748b;
}

.ai-message-content {
  max-width: 75%;
}

.ai-message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  text-align: left;
}

.ai-message.ai .ai-message-text {
  background: #f1f5f9;
  color: #334155;
  border-top-left-radius: 4px;
}

.ai-message.user .ai-message-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  border-top-right-radius: 4px;
}

/* Markdown 样式 */
.ai-message-text :deep(p) {
  margin: 0 0 8px 0;
}

.ai-message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-message-text :deep(strong) {
  font-weight: 700;
}

.ai-message-text :deep(em) {
  font-style: italic;
}

.ai-message-text :deep(ul),
.ai-message-text :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.ai-message-text :deep(li) {
  margin: 4px 0;
}

.ai-message-text :deep(code) {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}

.ai-message-text :deep(pre) {
  background: rgba(0, 0, 0, 0.08);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.ai-message-text :deep(pre code) {
  background: none;
  padding: 0;
  font-size: 13px;
}

.ai-message-text :deep(a) {
  color: #3b82f6;
  text-decoration: underline;
}

.ai-message-text :deep(h1),
.ai-message-text :deep(h2),
.ai-message-text :deep(h3) {
  font-weight: 700;
  margin: 12px 0 8px 0;
}

.ai-message-text :deep(h1) {
  font-size: 18px;
}

.ai-message-text :deep(h2) {
  font-size: 16px;
}

.ai-message-text :deep(h3) {
  font-size: 15px;
}

.ai-message-text :deep(blockquote) {
  border-left: 3px solid #cbd5e1;
  padding-left: 12px;
  margin: 8px 0;
  color: #64748b;
}

/* 打字动画 */
.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 2px;
  vertical-align: middle;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dot:nth-child(1) {
  animation-delay: 0s;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

.ai-input-area {
  flex-shrink: 0;
  margin-top: auto;
  padding: 12px 0 0;
  border-top: 1px solid #f1f5f9;
  width: 100%;
}

.ai-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.ai-input-actions :deep(.el-switch__label) {
  font-size: 12px;
}

.ai-input-actions :deep(.el-switch__label.is-active) {
  color: inherit;
}

.ai-textarea {
  width: 100%;
}

.ai-textarea :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 15px;
  line-height: 1.5;
  transition: all 0.3s ease;
  border: 2px solid #e2e8f0;
  resize: none;
  width: 100%;
}

.ai-textarea :deep(.el-textarea__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.ai-textarea :deep(.el-textarea__inner::placeholder) {
  color: #94a3b8;
}

/* AI未启用样式 */
.ai-disabled-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 20px;
  text-align: center;
}

.ai-disabled-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  margin-bottom: 20px;
}

.ai-disabled-container h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #334155;
}

.ai-disabled-container p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.ai-send-btn {
  align-self: flex-end;
  padding: 0 24px;
  height: 56px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-send-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.ai-send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.author-block {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.author-block.clickable {
  cursor: pointer;
}

.author-block.clickable:hover {
  background: #f0f9ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.author-info strong {
  font-size: 15px;
  color: #303133;
}

.author-date {
  font-size: 12px;
  color: #909399;
}

.add-friend-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #e0f2fe;
  color: #0284c7;
  opacity: 0;
  transition: all 0.3s ease;
}

.author-block.clickable:hover .add-friend-hint {
  opacity: 1;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.data-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: default;
  transition: all 0.3s ease;
  padding: 12px 8px;
  border-radius: 12px;
}

.data-item:hover {
  transform: translateY(-2px);
}

.like-item {
  cursor: pointer;
}

.like-item:hover {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
}

.like-item.liked:hover {
  background: linear-gradient(135deg, #fecaca 0%, #fca5a5 100%);
}

.data-icon-wrapper {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: all 0.3s ease;
  background: #f1f5f9;
}

.data-icon-wrapper .el-icon {
  color: #64748b;
  transition: all 0.3s ease;
  font-size: 20px;
}

.like-icon {
  background: #f1f5f9;
  transition: all 0.3s ease;
}

.heart-icon {
  width: 20px;
  height: 20px;
  transition: all 0.3s ease;
}

.heart-icon.filled {
  color: #dc2626;
}

.like-item.liked .like-icon {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  animation: pulse-heart 0.4s ease;
}

.like-item.liked .like-icon .heart-icon {
  color: #dc2626;
}

@keyframes pulse-heart {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.data-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.data-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: -0.5px;
  line-height: 1;
}

.like-item.liked .data-value {
  color: #dc2626;
}

.data-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.comment-badge {
  margin-left: 4px;
}

.toc-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.empty-toc {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;
}

.toc-item {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.toc-item:hover {
  background: #f1f5f9;
  color: #1f2937;
}

.toc-level-1 {
  font-weight: 600;
  font-size: 15px;
  padding-left: 0;
}

.toc-level-2 {
  padding-left: 12px;
}

.toc-level-3 {
  padding-left: 24px;
  font-size: 13px;
}

.toc-level-4 {
  padding-left: 36px;
  font-size: 13px;
}

.toc-level-5 {
  padding-left: 48px;
  font-size: 13px;
}

.toc-level-6 {
  padding-left: 60px;
  font-size: 13px;
}

@media (max-width: 860px) {
  .article-sidebar {
    width: 100%;
    flex-direction: column;
    gap: 12px;
    padding: 0;
  }

  .article-sidebar .back-btn-card {
    width: 100%;
  }

  .article-sidebar .back-btn-card,
  .article-sidebar .sidebar-card {
    min-width: auto;
  }

  .tabs-card {
    min-height: 500px;
  }
}
</style>
