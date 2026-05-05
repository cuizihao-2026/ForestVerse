<template>
  <div class="settings-container">
    <!-- 标签页导航 -->
    <AdminTabs :tabs="tabs" v-model="activeTab" />
    
    <!-- 保存成功提示 -->
    <transition name="fade-slide">
      <div v-if="showSaveSuccess" class="success-message">
        <svg class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        设置保存成功！
      </div>
    </transition>

    <!-- 保存/取消按钮 -->
    <transition name="fade-slide">
      <div v-if="hasUnsavedChanges" class="action-buttons">
        <button class="btn btn-cancel" @click="cancelChanges">
          <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
          取消
        </button>
        <button class="btn btn-save" @click="saveSettings">
          <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
            <polyline points="17 21 17 13 7 13 7 21"/>
            <polyline points="7 3 7 8 15 8"/>
          </svg>
          保存
        </button>
      </div>
    </transition>
    
    <!-- 标签页内容 -->
    <div class="tab-content">
      <!-- 心跳设置 -->
      <HeartbeatSettings
        v-if="activeTab === 'heartbeat'"
        ref="heartbeatSettingsRef"
        :settings="editingSettings"
        @update="handleSettingsUpdate"
      />
      
      <!-- 安全设置 -->
      <SecuritySettings
        v-else-if="activeTab === 'security'"
        ref="securitySettingsRef"
        :settings="editingSettings"
        @update="handleSettingsUpdate"
      />
      
      <!-- 邮件配置 -->
      <EmailSettings
        v-else-if="activeTab === 'email'"
        ref="emailSettingsRef"
        :settings="editingSettings"
        @update="handleSettingsUpdate"
      />
      
      <!-- AI 设置 -->
      <AISettings
        v-else-if="activeTab === 'ai'"
        ref="aiSettingsRef"
        :settings="editingSettings"
        @update="handleSettingsUpdate"
      />
      
      <!-- 用户设置 -->
      <UserSettings
        v-else-if="activeTab === 'user'"
        ref="userSettingsRef"
        :settings="editingSettings"
        @update="handleSettingsUpdate"
      />
      
      <!-- 网站设置 -->
      <SiteSettings
        v-else-if="activeTab === 'site'"
        ref="siteSettingsRef"
        :settings="editingSettings"
        @update="handleSettingsUpdate"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import HeartbeatSettings from './HeartbeatSettings.vue';
import EmailSettings from './EmailSettings.vue';
import SecuritySettings from './SecuritySettings.vue';
import UserSettings from './UserSettings.vue';
import SiteSettings from './SiteSettings.vue';
import AISettings from './AISettings.vue';
import AdminTabs from './AdminTabs.vue';
import { get, put } from '../../utils/api';
import { restartHeartbeat } from '../../stores/websocket';
import { updateSiteConfig } from '../../stores/siteConfig';

interface WebsiteSettings {
  heartbeatEnabled: boolean;
  heartbeatRate: number;
  heartbeatTimeout: number;
  emailEnabled: boolean;
  emailFrom: string;
  verificationCodeLength: number;
  verificationCodeExpireMinutes: number;
  smtpHost: string;
  smtpPort: number;
  smtpUsername: string;
  smtpPassword: string;
  smtpSsl: boolean;
  captchaRegisterEnabled: boolean;
  captchaForgotPasswordEnabled: boolean;
  captchaLoginEnabled: boolean;
  captchaNoiseCount: number;
  captchaLineCount: number;
  captchaCodeLength: number;
  captchaCaseSensitive: boolean;
  captchaRotateEnabled: boolean;
  articleAuditEnabled: boolean;
  commentAuditEnabled: boolean;
  siteTitle: string;
  siteIcon: string;
  siteFavicon: string;
  aiEnabled: boolean;
  aiProvider: string;
  aiApiKey: string;
  aiBaseUrl: string;
  aiModel: string;
  aiSystemPrompt: string;
  aiArticleAuditPrompt: string;
  aiCommentAuditPrompt: string;
  aiReadingEnabled: boolean;
  aiArticleAuditEnabled: boolean;
  aiCommentAuditEnabled: boolean;
  aiArticleAutoAuditEnabled: boolean;
  aiCommentAutoAuditEnabled: boolean;
}

interface Tab {
  id: string
  label: string
  icon: string
}

const tabs: Tab[] = [
  { id: 'heartbeat', label: '心跳设置', icon: '💓' },
  { id: 'security', label: '安全设置', icon: '🔒' },
  { id: 'email', label: '邮件配置', icon: '📧' },
  { id: 'ai', label: 'AI 设置', icon: '🤖' },
  { id: 'user', label: '用户设置', icon: '👤' },
  { id: 'site', label: '网站设置', icon: '⚙️' }
]

const activeTab = ref<string>('heartbeat')
const originalSettings = ref<WebsiteSettings>({
  heartbeatEnabled: true,
  heartbeatRate: 30,
  heartbeatTimeout: 90,
  emailEnabled: true,
  emailFrom: '',
  verificationCodeLength: 6,
  verificationCodeExpireMinutes: 10,
  smtpHost: '',
  smtpPort: 0,
  smtpUsername: '',
  smtpPassword: '',
  smtpSsl: false,
  captchaRegisterEnabled: true,
  captchaForgotPasswordEnabled: true,
  captchaLoginEnabled: true,
  captchaNoiseCount: 150,
  captchaLineCount: 5,
  captchaCodeLength: 4,
  captchaCaseSensitive: false,
  captchaRotateEnabled: false,
  articleAuditEnabled: false,
  commentAuditEnabled: false,
  siteTitle: '',
  siteIcon: '',
  siteFavicon: '',
  aiEnabled: false,
  aiProvider: 'deepseek',
  aiApiKey: '',
  aiBaseUrl: 'https://api.deepseek.com',
  aiModel: 'deepseek-chat',
  aiSystemPrompt: '',
  aiArticleAuditPrompt: '',
  aiCommentAuditPrompt: '',
  aiReadingEnabled: false,
  aiArticleAuditEnabled: false,
  aiCommentAuditEnabled: false,
  aiArticleAutoAuditEnabled: false,
  aiCommentAutoAuditEnabled: false
})

const editingSettings = ref<WebsiteSettings>({ ...originalSettings.value })
const showSaveSuccess = ref(false)
const heartbeatSettingsRef = ref()
const securitySettingsRef = ref()
const emailSettingsRef = ref()
const aiSettingsRef = ref()
const userSettingsRef = ref()
const siteSettingsRef = ref()

const hasUnsavedChanges = computed(() => {
  return (
    editingSettings.value.heartbeatEnabled !== originalSettings.value.heartbeatEnabled ||
    editingSettings.value.heartbeatRate !== originalSettings.value.heartbeatRate ||
    editingSettings.value.heartbeatTimeout !== originalSettings.value.heartbeatTimeout ||
    editingSettings.value.emailEnabled !== originalSettings.value.emailEnabled ||
    editingSettings.value.emailFrom !== originalSettings.value.emailFrom ||
    editingSettings.value.verificationCodeLength !== originalSettings.value.verificationCodeLength ||
    editingSettings.value.verificationCodeExpireMinutes !== originalSettings.value.verificationCodeExpireMinutes ||
    editingSettings.value.smtpHost !== originalSettings.value.smtpHost ||
    editingSettings.value.smtpPort !== originalSettings.value.smtpPort ||
    editingSettings.value.smtpUsername !== originalSettings.value.smtpUsername ||
    editingSettings.value.smtpPassword !== originalSettings.value.smtpPassword ||
    editingSettings.value.smtpSsl !== originalSettings.value.smtpSsl ||
    editingSettings.value.captchaRegisterEnabled !== originalSettings.value.captchaRegisterEnabled ||
    editingSettings.value.captchaForgotPasswordEnabled !== originalSettings.value.captchaForgotPasswordEnabled ||
    editingSettings.value.captchaLoginEnabled !== originalSettings.value.captchaLoginEnabled ||
    editingSettings.value.captchaNoiseCount !== originalSettings.value.captchaNoiseCount ||
    editingSettings.value.captchaLineCount !== originalSettings.value.captchaLineCount ||
    editingSettings.value.captchaCodeLength !== originalSettings.value.captchaCodeLength ||
    editingSettings.value.captchaCaseSensitive !== originalSettings.value.captchaCaseSensitive ||
    editingSettings.value.captchaRotateEnabled !== originalSettings.value.captchaRotateEnabled ||
    editingSettings.value.articleAuditEnabled !== originalSettings.value.articleAuditEnabled ||
    editingSettings.value.commentAuditEnabled !== originalSettings.value.commentAuditEnabled ||
    editingSettings.value.siteTitle !== originalSettings.value.siteTitle ||
    editingSettings.value.siteIcon !== originalSettings.value.siteIcon ||
    editingSettings.value.siteFavicon !== originalSettings.value.siteFavicon ||
    editingSettings.value.aiEnabled !== originalSettings.value.aiEnabled ||
    editingSettings.value.aiProvider !== originalSettings.value.aiProvider ||
    editingSettings.value.aiApiKey !== originalSettings.value.aiApiKey ||
    editingSettings.value.aiBaseUrl !== originalSettings.value.aiBaseUrl ||
    editingSettings.value.aiModel !== originalSettings.value.aiModel ||
    editingSettings.value.aiSystemPrompt !== originalSettings.value.aiSystemPrompt ||
    editingSettings.value.aiArticleAuditPrompt !== originalSettings.value.aiArticleAuditPrompt ||
    editingSettings.value.aiCommentAuditPrompt !== originalSettings.value.aiCommentAuditPrompt ||
    editingSettings.value.aiReadingEnabled !== originalSettings.value.aiReadingEnabled ||
    editingSettings.value.aiArticleAuditEnabled !== originalSettings.value.aiArticleAuditEnabled ||
    editingSettings.value.aiCommentAuditEnabled !== originalSettings.value.aiCommentAuditEnabled ||
    editingSettings.value.aiArticleAutoAuditEnabled !== originalSettings.value.aiArticleAutoAuditEnabled ||
    editingSettings.value.aiCommentAutoAuditEnabled !== originalSettings.value.aiCommentAutoAuditEnabled
  )
})

const loadSettings = async () => {
  try {
    const data = await get('/api/settings');
    originalSettings.value = { ...originalSettings.value, ...data };
    editingSettings.value = { ...originalSettings.value, ...data };
    
    // 同步到 store
    updateSiteConfig({
      siteTitle: data.siteTitle || '',
      siteIcon: data.siteIcon || '',
      siteFavicon: data.siteFavicon || ''
    });
    
    // 更新子组件
    if (heartbeatSettingsRef.value?.updateLocalSettings) {
      heartbeatSettingsRef.value.updateLocalSettings(editingSettings.value);
    }
    if (securitySettingsRef.value?.updateLocalSettings) {
      securitySettingsRef.value.updateLocalSettings(editingSettings.value);
    }
    if (emailSettingsRef.value?.updateLocalSettings) {
      emailSettingsRef.value.updateLocalSettings(editingSettings.value);
    }
    if (aiSettingsRef.value?.updateLocalSettings) {
      aiSettingsRef.value.updateLocalSettings(editingSettings.value);
    }
    if (userSettingsRef.value?.updateLocalSettings) {
      userSettingsRef.value.updateLocalSettings(editingSettings.value);
    }
    if (siteSettingsRef.value?.updateLocalSettings) {
      siteSettingsRef.value.updateLocalSettings(editingSettings.value);
    }
  } catch (error) {
    console.error('加载设置失败:', error);
  }
}

const handleSettingsUpdate = (key: string, value: any) => {
  editingSettings.value = { ...editingSettings.value, [key]: value }
}

const saveSettings = async () => {
  try {
    await put('/api/settings', editingSettings.value);
    
    const heartbeatChanged =
      originalSettings.value.heartbeatEnabled !== editingSettings.value.heartbeatEnabled ||
      originalSettings.value.heartbeatRate !== editingSettings.value.heartbeatRate ||
      originalSettings.value.heartbeatTimeout !== editingSettings.value.heartbeatTimeout;

    originalSettings.value = { ...editingSettings.value };
    
    // 同步到 store
    updateSiteConfig({
      siteTitle: editingSettings.value.siteTitle,
      siteIcon: editingSettings.value.siteIcon,
      siteFavicon: editingSettings.value.siteFavicon
    });
    
    if (heartbeatChanged) {
      restartHeartbeat()
    }
    
    showSaveSuccess.value = true;
    await nextTick();
    
    setTimeout(() => {
      showSaveSuccess.value = false;
    }, 3000);
  } catch (error) {
    console.error('保存设置失败:', error);
    alert('保存失败: ' + (error as any).message);
  }
}

const cancelChanges = () => {
  editingSettings.value = { ...originalSettings.value }
  if (heartbeatSettingsRef.value?.updateLocalSettings) {
    heartbeatSettingsRef.value.updateLocalSettings(editingSettings.value)
  }
  if (securitySettingsRef.value?.updateLocalSettings) {
    securitySettingsRef.value.updateLocalSettings(editingSettings.value)
  }
  if (emailSettingsRef.value?.updateLocalSettings) {
    emailSettingsRef.value.updateLocalSettings(editingSettings.value)
  }
  if (aiSettingsRef.value?.updateLocalSettings) {
    aiSettingsRef.value.updateLocalSettings(editingSettings.value)
  }
  if (userSettingsRef.value?.updateLocalSettings) {
    userSettingsRef.value.updateLocalSettings(editingSettings.value)
  }
  if (siteSettingsRef.value?.updateLocalSettings) {
    siteSettingsRef.value.updateLocalSettings(editingSettings.value)
  }
}

const handleWsStatusChange = (event: CustomEvent) => {
  if (heartbeatSettingsRef.value?.handleWsStatusChange) {
    heartbeatSettingsRef.value.handleWsStatusChange(event)
  }
}

onMounted(() => {
  loadSettings()
  window.addEventListener('ws-status-changed', handleWsStatusChange as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('ws-status-changed', handleWsStatusChange as EventListener)
})
</script>

<style scoped>
.settings-container {
  animation: fadeInUp 0.6s ease-out;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}



.tab-content {
  min-height: 300px;
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.success-message {
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid #6ee7b7;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

.success-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.btn-save {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.btn-save:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
}

.btn-save:active {
  transform: translateY(0);
}

.btn-cancel {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.btn-cancel:hover {
  background: #e2e8f0;
  color: #475569;
}

/* 占位符内容 */
.placeholder-content {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  opacity: 0.3;
}

.placeholder-icon svg {
  width: 100%;
  height: 100%;
}

.placeholder-content h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #475569;
}

.placeholder-content p {
  font-size: 14px;
  color: #94a3b8;
}
</style>
