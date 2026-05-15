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
          <div class="sidebar-tabs">
            <button 
              class="sidebar-tab"
              :class="{ active: activeTab === 'settings' }"
              @click="activeTab = 'settings'"
            >
              文章设置
            </button>
            <button 
              v-if="canUseAiWriting()"
              class="sidebar-tab"
              :class="{ active: activeTab === 'ai' }"
              @click="activeTab = 'ai'"
            >
              AI 写作助手
            </button>
          </div>
          
          <div class="tab-content">
            <!-- 设置面板 -->
            <div v-show="activeTab === 'settings'" class="settings-panel">
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
                <select v-model="formData.category" class="form-input sidebar-input">
                  <option value="">无分类</option>
                  <option v-for="cat in categories" :key="cat.id" :value="cat.name">{{ cat.name }}</option>
                </select>
              </section>
              
              <section class="sidebar-section">
                <h3 class="section-title">标签 <span class="section-sub">最多5个，按回车或逗号添加</span></h3>
                <div class="tags-input-wrap">
                  <div class="tags-list">
                    <span v-for="(tag, i) in tagList" :key="i" class="tag-chip">
                      {{ tag }}
                      <button class="tag-remove" @click="removeTag(i)">&times;</button>
                    </span>
                    <input
                      v-if="tagList.length < 5"
                      v-model="tagInput"
                      class="tag-input"
                      placeholder="输入标签"
                      @keydown="handleTagKeydown"
                      @blur="addTagFromInput"
                    />
                  </div>
                </div>
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
            </div>
            
            <!-- AI 写作助手面板 -->
            <div v-show="activeTab === 'ai'" class="ai-panel">
              <div class="ai-header">
                <div class="ai-icon">
                  <img src="/src/assets/images/AI.jpg" alt="森小语" class="ai-avatar-img" />
                </div>
                <div class="ai-title">
                  <h3>森小语</h3>
                  <p>您的 AI 写作助手</p>
                </div>
                <button class="quick-prompts-toggle" @click="showPrompts = !showPrompts">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="8" y1="6" x2="21" y2="6"></line>
                    <line x1="8" y1="12" x2="21" y2="12"></line>
                    <line x1="8" y1="18" x2="21" y2="18"></line>
                    <line x1="3" y1="6" x2="3.01" y2="6"></line>
                    <line x1="3" y1="12" x2="3.01" y2="12"></line>
                    <line x1="3" y1="18" x2="3.01" y2="18"></line>
                  </svg>
                </button>
              </div>
              
              <!-- 快速提示下拉框 -->
              <div class="prompts-dropdown" :class="{ show: showPrompts }">
                <div class="prompts-dropdown-content">
                  <button class="prompt-item" @click="sendQuickPrompt('continue')">
                    <svg class="prompt-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9"></path>
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                    </svg>
                    <span class="prompt-item-text">续写文章</span>
                  </button>
                  <button class="prompt-item" @click="sendQuickPrompt('polish')">
                    <svg class="prompt-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 3l1.912 5.813a2 2 0 0 0 1.275 1.275L21 12l-5.813 1.912a2 2 0 0 0-1.275 1.275L12 21l-1.912-5.813a2 2 0 0 0-1.275-1.275L3 12l5.813-1.912a2 2 0 0 0 1.275-1.275L12 3z"></path>
                    </svg>
                    <span class="prompt-item-text">润色优化</span>
                  </button>
                  <button class="prompt-item" @click="sendQuickPrompt('summarize')">
                    <svg class="prompt-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                      <polyline points="14 2 14 8 20 8"></polyline>
                      <line x1="16" y1="13" x2="8" y2="13"></line>
                      <line x1="16" y1="17" x2="8" y2="17"></line>
                      <polyline points="10 9 9 9 8 9"></polyline>
                    </svg>
                    <span class="prompt-item-text">内容摘要</span>
                  </button>
                  <button class="prompt-item" @click="sendQuickPrompt('translate')">
                    <svg class="prompt-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M5 8l6 6"></path>
                      <path d="M4 14l6-6 2-3"></path>
                      <path d="M2 5h12"></path>
                      <path d="M7 2h1"></path>
                      <path d="M22 22l-5-10-5 10"></path>
                      <path d="M14 18h6"></path>
                    </svg>
                    <span class="prompt-item-text">翻译成英文</span>
                  </button>
                  <button class="prompt-item" @click="sendQuickPrompt('outline')">
                    <svg class="prompt-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="8" y1="6" x2="21" y2="6"></line>
                      <line x1="8" y1="12" x2="21" y2="12"></line>
                      <line x1="8" y1="18" x2="21" y2="18"></line>
                      <line x1="3" y1="6" x2="3.01" y2="6"></line>
                      <line x1="3" y1="12" x2="3.01" y2="12"></line>
                      <line x1="3" y1="18" x2="3.01" y2="18"></line>
                    </svg>
                    <span class="prompt-item-text">生成大纲</span>
                  </button>
                  <button class="prompt-item" @click="sendQuickPrompt('title')">
                    <svg class="prompt-item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                      <line x1="7" y1="7" x2="7.01" y2="7"></line>
                    </svg>
                    <span class="prompt-item-text">标题建议</span>
                  </button>
                </div>
              </div>
              
              <div class="ai-chat-container" ref="aiMessagesContainer">
                <!-- 欢迎信息 -->
                <div v-if="aiMessages.length === 0" class="ai-welcome">
                  <div class="ai-welcome-icon">
                    <img src="/src/assets/images/AI.jpg" alt="森小语" />
                  </div>
                  <h3>森小语</h3>
                  <p>您好！我是您的AI写作助手，可以帮您：</p>
                  <ul class="ai-welcome-tips">
                    <li><ChatLineRound /> 续写文章</li>
                    <li><ChatLineRound /> 润色优化</li>
                    <li><ChatLineRound /> 内容总结</li>
                    <li><ChatLineRound /> 生成大纲</li>
                  </ul>
                  <div class="ai-quick-questions">
                    <button class="quick-question-btn" @click="sendQuickPrompt('continue')" :disabled="aiLoading">
                      续写文章
                    </button>
                    <button class="quick-question-btn" @click="sendQuickPrompt('polish')" :disabled="aiLoading">
                      润色优化
                    </button>
                    <button class="quick-question-btn" @click="sendQuickPrompt('outline')" :disabled="!formData.title || aiLoading">
                      生成大纲
                    </button>
                  </div>
                </div>

                <!-- 消息列表 -->
                <div v-else class="ai-messages">
                  <div 
                    v-for="(msg, index) in aiMessages" 
                    :key="index" 
                    class="ai-message"
                    :class="msg.role"
                  >
                    <div v-if="msg.role === 'ai'" class="ai-message-avatar">
                      <img src="/src/assets/images/AI.jpg" alt="森小语" class="ai-avatar-img" />
                    </div>
                    <div class="ai-message-content">
                      <div v-if="msg.role === 'ai' && aiLoading && index === aiMessages.length - 1" class="ai-message-text">
                        <span v-if="streamEnabled" v-html="formatStreamText(msg.content)"></span>
                        <span class="typing-dots">
                          <span class="typing-dot"></span>
                          <span class="typing-dot"></span>
                          <span class="typing-dot"></span>
                        </span>
                      </div>
                      <div v-else-if="msg.role === 'ai'" class="ai-message-text" v-html="renderMarkdown(msg.content)"></div>
                      <div v-else class="ai-message-text">{{ msg.content }}</div>
                      <!-- 消息操作按钮 -->
                      <div v-if="msg.role === 'ai' && !aiLoading" class="message-actions">
                        <button class="action-btn" @click="insertContent(msg.content)" title="插入到编辑器">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <polyline points="14 2 14 8 20 8"/>
                            <line x1="12" y1="18" x2="12" y2="12"/>
                            <line x1="9" y1="15" x2="15" y2="15"/>
                          </svg>
                          插入
                        </button>
                        <button class="action-btn" @click="copyContent(msg.content)" title="复制">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                          </svg>
                          复制
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="ai-input-area">
                <div class="ai-input-actions">
                  <label class="stream-toggle">
                    <input type="checkbox" v-model="streamEnabled" :disabled="aiLoading" />
                    <span class="toggle-switch"></span>
                    <span class="toggle-label">流式输出</span>
                  </label>
                </div>
                <div class="ai-textarea-wrapper">
                  <textarea 
                    v-model="aiInput"
                    class="ai-input"
                    placeholder="输入您的问题...（Enter发送）"
                    @keydown="handleAiInputKeydown"
                    :disabled="aiLoading"
                    maxlength="100"
                  ></textarea>
                  <span class="ai-word-count">{{ aiInput.length }}/100</span>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { post, put, uploadFile, API_BASE_URL, get } from '../../utils/api'
import { showModal, showConfirm } from '../../stores/modal'
import { getToken } from '../../stores/auth'
import { hasPermission } from '../../stores/permission'
import { ChatLineRound } from '@element-plus/icons-vue'
import RichTextEditor from '../common/RichTextEditor.vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

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

const MAX_TAGS = 5
const tagInput = ref('')

const tagList = computed({
  get: () => {
    if (!formData.value.tags) return []
    return formData.value.tags.split(',').map(t => t.trim()).filter(t => t.length > 0)
  },
  set: (tags: string[]) => {
    formData.value.tags = tags.join(',')
  }
})

const addTagFromInput = () => {
  const tag = tagInput.value.trim()
  if (!tag) return
  if (tag.length > 20) return
  if (tagList.value.length >= MAX_TAGS) return
  if (tagList.value.includes(tag)) {
    tagInput.value = ''
    return
  }
  tagList.value = [...tagList.value, tag]
  tagInput.value = ''
}

const handleTagKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    addTagFromInput()
  } else if (e.key === 'Backspace' && tagInput.value === '' && tagList.value.length > 0) {
    tagList.value = tagList.value.slice(0, -1)
  }
}

const removeTag = (index: number) => {
  const list = [...tagList.value]
  list.splice(index, 1)
  tagList.value = list
}

// 渲染 Markdown（带 XSS 防护）
const renderMarkdown = (content: string) => {
  const html = marked.parse(content) as string
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'code', 'pre', 'ul', 'ol', 'li', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote'],
    ALLOWED_ATTR: ['href', 'title', 'target', 'rel']
  })
}

// 格式化流式文本
const formatStreamText = (content: string): string => {
  let escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/(^|\s)#{1,6}\s*/gm, '$1')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/(^|\s)[\-\*]\s+/gm, '$1• ')
    .replace(/\n/g, '<br>')

  return DOMPurify.sanitize(escaped, {
    ALLOWED_TAGS: ['br'],
    ALLOWED_ATTR: []
  })
}

// AI 相关状态
const activeTab = ref<'settings' | 'ai'>('settings')
const aiInput = ref('')
const aiMessages = ref<{ role: 'user' | 'ai', content: string }[]>([])
const aiLoading = ref(false)
const streamEnabled = ref(false)
const aiMessagesContainer = ref<HTMLDivElement>()
const showPrompts = ref(false)
const writingEnabled = ref(false)

const hasAiWritingPermission = computed(() => hasPermission('ai.writing'))

const checkWritingEnabled = async () => {
  try {
    const response = await fetch(API_BASE_URL + '/api/settings/public/ai-config')
    const data = await response.json()
    writingEnabled.value = data.aiWritingEnabled === true
    if (!canUseAiWriting() && activeTab.value === 'ai') {
      activeTab.value = 'settings'
    }
  } catch {
    writingEnabled.value = false
  }
}

const canUseAiWriting = () => writingEnabled.value && hasAiWritingPermission.value

interface Category {
  id: number
  name: string
}

const categories = ref<Category[]>([])

const loadCategories = async () => {
  try {
    const data = await get('/api/categories')
    categories.value = data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
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

// 提取SSE数据
const extractSseData = (line: string): string | null => {
  if (line.startsWith('data: ')) return line.substring(6)
  if (line.startsWith('data:')) return line.substring(5)
  return null
}

const sendQuickPrompt = async (type: string) => {
  if (aiLoading.value) return
  
  // 检查必要条件
  let content = ''
  if (formData.value.content) {
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = formData.value.content
    content = tempDiv.textContent || tempDiv.innerText || ''
  }
  
  let needContent = ['continue', 'polish', 'summarize', 'translate', 'title'].includes(type)
  let needTitle = ['outline'].includes(type)
  
  if (needContent && !content) {
    aiInput.value = '请先写一些内容，我才能帮你。'
    sendMessage()
    return
  }
  
  if (needTitle && !formData.value.title) {
    aiInput.value = '请先输入文章标题，我才能帮你生成大纲。'
    sendMessage()
    return
  }
  
  // 构造用户可见的提示
  let userPrompt = ''
  switch (type) {
    case 'continue':
      userPrompt = '请帮我续写文章'
      break
    case 'polish':
      userPrompt = '请帮我润色优化文章'
      break
    case 'summarize':
      userPrompt = '请帮我总结文章核心内容'
      break
    case 'translate':
      userPrompt = '请帮我翻译成中文'
      break
    case 'outline':
      userPrompt = `请帮我围绕"${formData.value.title}"生成写作大纲`
      break
    case 'title':
      userPrompt = '请帮我推荐几个标题'
      break
  }
  
  // 添加用户消息
  aiMessages.value.push({
    role: 'user',
    content: userPrompt
  })
  
  // 添加空的助手消息
  const aiMessageIndex = aiMessages.value.length
  aiMessages.value.push({
    role: 'ai',
    content: ''
  })
  aiLoading.value = true

  try {
    if (streamEnabled.value) {
      await sendStreamRequest(type, aiMessageIndex)
    } else {
      await sendNonStreamRequest(type, aiMessageIndex)
    }
  } catch (error) {
    console.error('AI 写作助手失败:', error)
    aiMessages.value = aiMessages.value.slice(0, -2)
  } finally {
    aiLoading.value = false
  }
}

const sendMessage = async () => {
  if (!aiInput.value.trim() || aiLoading.value) return
  
  const userMessage = aiInput.value.trim()
  aiInput.value = ''
  
  // 添加用户消息
  aiMessages.value.push({
    role: 'user',
    content: userMessage
  })
  
  // 添加空的助手消息
  const aiMessageIndex = aiMessages.value.length
  aiMessages.value.push({
    role: 'ai',
    content: ''
  })
  aiLoading.value = true
  
  try {
    if (streamEnabled.value) {
      await sendStreamRequest('default', aiMessageIndex, userMessage)
    } else {
      await sendNonStreamRequest('default', aiMessageIndex, userMessage)
    }
  } catch (error) {
    console.error('AI 聊天失败:', error)
    aiMessages.value = aiMessages.value.slice(0, -2)
  } finally {
    aiLoading.value = false
  }
}

const sendStreamRequest = async (taskType: string, aiMessageIndex: number, userPrompt?: string) => {
  const token = getToken()
  
  let content = ''
  if (formData.value.content) {
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = formData.value.content
    content = tempDiv.textContent || tempDiv.innerText || ''
  }
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  
  const response = await fetch(API_BASE_URL + '/api/ai/writing-assist/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify({
      taskType: taskType,
      title: formData.value.title,
      content: content,
      userPrompt: userPrompt || ''
    })
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
          const data = dataLines.join('\n')
          if (aiMessages.value[aiMessageIndex]) {
            aiMessages.value[aiMessageIndex].content += data
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
          const data = dataLines.join('\n')
          if (aiMessages.value[aiMessageIndex]) {
            aiMessages.value[aiMessageIndex].content += data
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

const sendNonStreamRequest = async (taskType: string, aiMessageIndex: number, userPrompt?: string) => {
  let content = ''
  if (formData.value.content) {
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = formData.value.content
    content = tempDiv.textContent || tempDiv.innerText || ''
  }
  
  const response = await post('/api/ai/writing-assist', {
    taskType: taskType,
    title: formData.value.title,
    content: content,
    userPrompt: userPrompt || ''
  })
  
  if (response && aiMessages.value[aiMessageIndex]) {
    aiMessages.value[aiMessageIndex].content = response.data?.result || ''
  }
}

const handleAiInputKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const insertContent = (content: string) => {
  // 将 Markdown 转换为简单的 HTML
  let html = content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
    .replace(/^### (.*?)$/gm, '<h3>$1</h3>')
    .replace(/^## (.*?)$/gm, '<h2>$1</h2>')
    .replace(/^# (.*?)$/gm, '<h1>$1</h1>')
    .replace(/^- (.*?)$/gm, '<li>$1</li>')
    .replace(/(<li>.*?<\/li>)/s, '<ul>$1</ul>')
  
  // 插入到编辑器
  formData.value.content += (formData.value.content ? '<br><br>' : '') + html
  
  showModal({
    title: '成功',
    message: '内容已插入到编辑器',
    type: 'success'
  })
}

const copyContent = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    showModal({
      title: '成功',
      message: '内容已复制到剪贴板',
      type: 'success'
    })
  } catch (error) {
    showModal({
      title: '错误',
      message: '复制失败',
      type: 'error'
    })
  }
}

onMounted(() => {
  loadSettings()
  checkWritingEnabled()
  loadCategories()
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
  width: 380px;
  background: #f8fafc;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.editor-sidebar::-webkit-scrollbar {
  width: 0;
}

/* 侧边栏标签 */
.sidebar-tabs {
  display: flex;
  background: white;
  border-bottom: 1px solid var(--border);
  padding: 8px;
  gap: 4px;
}

.sidebar-tab {
  flex: 1;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  position: relative;
}

.sidebar-tab:hover {
  background: #f1f5f9;
  color: #3b82f6;
}

.sidebar-tab.active {
  background: #eff6ff;
  color: #3b82f6;
  font-weight: 600;
}

.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-panel {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

/* AI 面板样式 */
.ai-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fafbfc;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.ai-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.ai-icon .ai-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-title h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.ai-title p {
  margin: 1px 0 0 0;
  font-size: 11px;
  color: #6b7280;
}

.quick-prompts-toggle {
  margin-left: auto;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
}

.quick-prompts-toggle:hover {
  background: #e5e7eb;
  color: #374151;
}

.quick-prompts-toggle svg {
  width: 16px;
  height: 16px;
}

.prompts-dropdown {
  position: relative;
}

.prompts-dropdown-content {
  position: absolute;
  top: calc(100% + 4px);
  right: 8px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  width: 180px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 50;
}

.prompts-dropdown.show .prompts-dropdown-content {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.prompt-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  background: transparent;
  border: none;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 13px;
  color: #374151;
}

.prompt-item:hover {
  background: #f3f4f6;
}

.prompt-item-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #6b7280;
}

.prompt-item:hover .prompt-item-icon {
  color: #3b82f6;
}

.prompt-item-text {
  flex: 1;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scrollbar-width: thin;
  scrollbar-color: #e2e8f0 transparent;
}

.ai-messages::-webkit-scrollbar {
  width: 6px;
}

.ai-messages::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 3px;
}

.ai-message {
  display: flex;
  gap: 12px;
  animation: messageIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #e5e7eb;
}

.ai-input-actions {
  display: flex;
  justify-content: flex-end;
}

.stream-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.stream-toggle input {
  display: none;
}

.toggle-switch {
  position: relative;
  width: 36px;
  height: 20px;
  background: #e5e7eb;
  border-radius: 10px;
  transition: all 0.3s;
}

.toggle-switch::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stream-toggle input:checked + .toggle-switch {
  background: #3b82f6;
}

.stream-toggle input:checked + .toggle-switch::before {
  transform: translateX(16px);
}

.toggle-label {
  font-size: 12px;
  color: #64748b;
}

.stream-toggle input:disabled + .toggle-switch {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-textarea-wrapper {
  position: relative;
}

.ai-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: all 0.2s;
  max-height: 120px;
  line-height: 1.5;
  min-height: 60px;
}

.ai-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.ai-input::placeholder {
  color: #94a3b8;
}

.ai-input:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}

.ai-word-count {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #9ca3af;
}

/* 原有样式 */
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

.section-sub {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
  text-transform: none;
  letter-spacing: normal;
}

.tags-input-wrap {
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  padding: 6px 10px;
  background: white;
  transition: border-color 0.2s;
}

.tags-input-wrap:focus-within {
  border-color: #3b82f6;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: none;
  background: transparent;
  color: #93c5fd;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  border-radius: 50%;
  transition: all 0.15s;
}

.tag-remove:hover {
  background: #2563eb;
  color: white;
}

.tag-input {
  flex: 1;
  min-width: 80px;
  border: none;
  outline: none;
  padding: 4px 2px;
  font-size: 13px;
  color: #374151;
  background: transparent;
}

.tag-input::placeholder {
  color: #94a3b8;
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

.sidebar-input:not([type="text"]) {
  appearance: auto;
  cursor: pointer;
  background: white;
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

@media (max-width: 1024px) {
  .editor-sidebar {
    width: 340px;
  }
  
  .prompts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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
    height: 500px;
  }
  
  .prompts-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* AI欢迎提示列表图标样式 */
.ai-welcome-tips li svg {
  width: 16px;
  height: 16px;
}

/* 消息气泡样式 */
.ai-message.ai {
  flex-direction: row;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message.ai .ai-message-avatar {
  background: transparent;
}

.ai-message-content {
  max-width: 75%;
}

.ai-message.ai .ai-message-text {
  background: #f1f5f9;
  color: #334155;
  border-top-left-radius: 4px;
}

.ai-message.user .ai-message-text {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  border-top-right-radius: 4px;
}

/* 消息操作按钮 */
.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #2563eb;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

/* Markdown 样式 */
.ai-message-text :deep(p) {
  margin: 0 0 10px 0;
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
  margin: 10px 0;
  padding-left: 24px;
}

.ai-message-text :deep(li) {
  margin: 6px 0;
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
  margin: 10px 0;
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
  margin: 10px 0;
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

/* 聊天容器样式 */
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

.quick-question-btn {
  width: 100%;
  padding: 10px 16px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-question-btn:hover:not(:disabled) {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #2563eb;
}

.quick-question-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-message-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: transparent;
}

.ai-message-avatar .ai-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-message-text {
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  text-align: left;
}
</style>
