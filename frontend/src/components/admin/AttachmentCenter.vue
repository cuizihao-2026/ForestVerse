<template>
  <div class="attachment-center">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="attachment-body">
      <!-- 标签页导航 -->
      <div class="tabs-nav">
        <button 
          v-for="tab in folderTabs" 
          :key="tab.id"
          :class="['tab-btn', { active: activeTab === tab.id }]"
          @click="switchTab(tab.id)"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <!-- 文件展示区域 -->
      <div class="files-section">
        <div v-if="files.length === 0" class="empty-state">
          <h3>暂无文件</h3>
          <p>当前文件夹中还没有文件</p>
        </div>

        <div v-else class="files-grid">
          <div v-for="(file, index) in files" :key="index" class="file-card">
            <div class="file-preview">
              <img v-if="isImage(file.name)" :src="apiBaseUrl + file.url" :alt="file.name" class="preview-image" />
              <div v-else class="file-icon">
                <span class="icon">{{ getFileIcon(file.name) }}</span>
              </div>
              <div class="preview-overlay">
                <button class="overlay-btn" @click.stop="deleteFile(file)" title="删除">
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页控件 -->
        <div v-if="totalPages > 1" class="pagination">
          <button 
            class="page-btn" 
            :disabled="currentPage === 0" 
            @click="goToPage(currentPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页 (共 {{ totalElements }} 个文件)</span>
          <button 
            class="page-btn" 
            :disabled="currentPage === totalPages - 1" 
            @click="goToPage(currentPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { get, del, API_BASE_URL } from '../../utils/api'
import { showModal } from '../../stores/modal'

const apiBaseUrl = API_BASE_URL

interface FileInfo {
  name: string
  path: string
  url: string
  size: number
  sizeFormatted: string
  createdTime: string
  modifiedTime: string
  directory: string
}

interface FolderTab {
  id: string
  label: string
  icon?: string
  count: number
}

const files = ref<FileInfo[]>([])
const loading = ref(false)
const activeTab = ref<string>('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 20

// 缓存的目录统计信息
const folderStats = ref<Map<string, number>>(new Map())

const getDisplayDirectoryName = (dir: string): string => {
  if (!dir) return '其他'
  const lower = dir.toLowerCase()
  if (lower === 'avatars' || lower === 'avatar' || lower === 'user-avatars') return '用户头像'
  if (lower === 'covers' || lower === 'cover' || lower === 'article-covers') return '文章封面'
  if (lower === 'chat-images' || lower === 'chatimages') return '聊天图片'
  if (lower === 'site-ui') return '网站UI'
  return '其他'
}

const folderTabs = computed<FolderTab[]>(() => {
  const tabs: FolderTab[] = []

  Array.from(folderStats.value.keys()).sort().forEach(dir => {
    tabs.push({
      id: dir,
      label: getDisplayDirectoryName(dir),
      count: folderStats.value.get(dir) || 0
    })
  })

  return tabs
})

const isImage = (fileName: string): boolean => {
  const ext = fileName.toLowerCase()
  return ext.endsWith('.jpg') || ext.endsWith('.jpeg') || 
         ext.endsWith('.png') || ext.endsWith('.gif') || 
         ext.endsWith('.webp') || ext.endsWith('.ico')
}

const getFileIcon = (fileName: string): string => {
  const ext = fileName.toLowerCase()
  if (ext.endsWith('.pdf')) return 'PDF'
  if (ext.endsWith('.doc') || ext.endsWith('.docx')) return 'DOC'
  if (ext.endsWith('.xls') || ext.endsWith('.xlsx')) return 'XLS'
  if (ext.endsWith('.ppt') || ext.endsWith('.pptx')) return 'PPT'
  if (ext.endsWith('.txt')) return 'TXT'
  if (ext.endsWith('.zip') || ext.endsWith('.rar')) return 'ZIP'
  if (ext.endsWith('.mp4') || ext.endsWith('.webm')) return 'MP4'
  if (ext.endsWith('.mp3')) return 'MP3'
  return '文件'
}

const loadFolderStats = async () => {
  try {
    // 先加载所有文件一次以获取目录统计
    const result = await get('/api/file/list', { page: 0, size: 9999 })
    const allFiles = (result.content || []) as FileInfo[]
    
    const stats = new Map<string, number>()
    allFiles.forEach((file: FileInfo) => {
      const dir = file.directory || ''
      stats.set(dir, (stats.get(dir) || 0) + 1)
    })
    folderStats.value = stats
  } catch (error) {
    console.error('加载目录统计失败:', error)
  }
}

const loadFiles = async (page: number = 0, directory?: string) => {
  loading.value = true
  try {
    const params: any = { page, size: pageSize }
    if (directory) {
      params.directory = directory
    }
    
    const result = await get('/api/file/list', params)
    files.value = (result.content || []) as FileInfo[]
    totalPages.value = result.totalPages || 0
    totalElements.value = result.totalElements || 0
    currentPage.value = result.page || 0
  } catch (error) {
    console.error('加载文件失败:', error)
    showModal({
      title: '失败',
      message: '加载文件失败',
      type: 'error'
    })
  } finally {
    loading.value = false
  }
}

const switchTab = (tabId: string) => {
  activeTab.value = tabId
  currentPage.value = 0
  loadFiles(0, tabId)
}

const goToPage = (page: number) => {
  if (page >= 0 && page < totalPages.value) {
    loadFiles(page, activeTab.value || undefined)
  }
}

const deleteFile = async (file: FileInfo) => {
  showModal({
    title: '确认删除',
    message: '确定要删除该文件吗？',
    type: 'warning',
    onConfirm: async () => {
      try {
        await del(`/api/file/delete?path=${encodeURIComponent(file.path)}`)
        // 重新加载目录统计和文件列表
        await loadFolderStats()
        await loadFiles(currentPage.value, activeTab.value || undefined)
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

onMounted(async () => {
  // 先加载目录统计
  await loadFolderStats()
  // 如果有目录，默认选择第一个
  if (folderStats.value.size > 0) {
    const firstDir = Array.from(folderStats.value.keys())[0]
    activeTab.value = firstDir
    await loadFiles(0, firstDir)
  }
})
</script>

<style scoped>
.attachment-center {
  width: 100%;
  font-family: var(--sans);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3xl) var(--spacing-xl);
}

.attachment-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
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

/* 标签页导航 */
.tabs-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  padding: 6px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  flex-wrap: wrap;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: #64748b;
  transition: all 0.25s ease;
  font-family: var(--sans);
}

.tab-btn:hover {
  background: #e2e8f0;
  color: #334155;
}

.tab-btn.active {
  background: white;
  color: #1e40af;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.tab-icon {
  font-size: 18px;
}

.tab-count {
  background: #e2e8f0;
  color: #475569;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.tab-btn.active .tab-count {
  background: #dbeafe;
  color: #1e40af;
}

.files-section {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  min-height: 200px;
}

.files-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.file-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: all 0.3s;
}

.file-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  border-color: #cbd5e1;
}

.file-preview {
  position: relative;
  width: 100%;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-image: 
    linear-gradient(45deg, #f8fafc 25%, transparent 25%),
    linear-gradient(-45deg, #f8fafc 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, #f8fafc 75%),
    linear-gradient(-45deg, transparent 75%, #f8fafc 75%);
  background-size: 12px 12px;
  background-position: 0 0, 0 6px, 6px -6px, -6px 0px;
  background-color: #ffffff;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  padding: 12px;
  box-sizing: border-box;
  transition: transform 0.3s;
}

.file-card:hover .preview-image {
  transform: scale(1.05);
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.file-card:hover .preview-overlay {
  opacity: 1;
}

.overlay-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  color: white;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.overlay-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: white;
  transform: scale(1.1);
}

.file-icon {
  font-size: 56px;
  opacity: 0.5;
}

/* 分页控件样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

.page-btn {
  padding: 10px 20px;
  border: 1px solid #e2e8f0;
  background: white;
  color: #374151;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.page-btn:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #3b82f6;
  color: #3b82f6;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
}

@media (max-width: 768px) {
  .files-grid {
    grid-template-columns: 1fr;
  }

  .tabs-nav {
    overflow-x: auto;
  }
}
</style>
