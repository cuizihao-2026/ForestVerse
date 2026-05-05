<template>
  <div class="site-settings">
    <div class="settings-section">
      <div class="section-header">
        <div class="section-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06-.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
          </svg>
        </div>
        <div class="section-title-wrap">
          <h3 class="section-title">网站基本设置</h3>
          <p class="section-desc">配置网站的标题和图标</p>
        </div>
      </div>
      
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">网站标题</span>
          <span class="label-desc">显示在浏览器标签和网站顶部的标题</span>
        </div>
        <div class="input-wrapper">
          <input 
            type="text" 
            :value="localSettings.siteTitle" 
            @input="(e) => updateSetting('siteTitle', (e.target as HTMLInputElement).value)"
            class="form-input"
            placeholder="请输入网站标题"
          />
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">网站图标</span>
          <span class="label-desc">上传网站图标图片，推荐尺寸 64x64 或 128x128</span>
        </div>
        <div class="icon-upload-wrapper">
          <div class="current-icon-preview">
            <img v-if="fullSiteIconUrl" :src="fullSiteIconUrl" alt="网站图标" class="icon-image">
            <span v-else class="icon-emoji">🖼️</span>
          </div>
          <div class="icon-actions">
            <input 
              type="file" 
              ref="iconFileInput"
              @change="handleIconUpload"
              class="icon-file-input"
              accept="image/*"
            >
            <button class="upload-btn" @click="triggerIconFileInput">
              上传图片
            </button>
            <button v-if="localSettings.siteIcon" class="clear-btn" @click="clearSiteIcon">
              清除
            </button>
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">网站 Favicon</span>
          <span class="label-desc">显示在浏览器标签页的小图标，推荐使用 16x16 或 32x32 的 ico/png 文件</span>
        </div>
        <div class="icon-upload-wrapper">
          <div class="current-icon-preview">
            <img v-if="fullSiteFaviconUrl" :src="fullSiteFaviconUrl" alt="Favicon" class="icon-image">
            <span v-else class="icon-emoji">🖼️</span>
          </div>
          <div class="icon-actions">
            <input 
              type="file" 
              ref="faviconFileInput"
              @change="handleFaviconUpload"
              class="icon-file-input"
              accept=".ico,.png,.jpg,.jpeg,.gif,.webp,.svg"
            >
            <button class="upload-btn" @click="triggerFaviconFileInput">
              上传 Favicon
            </button>
            <button v-if="localSettings.siteFavicon" class="clear-btn" @click="clearFavicon">
              清除
            </button>
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">预览</span>
          <span class="label-desc">当前设置的预览效果</span>
        </div>
        <div class="preview-wrapper">
          <div class="site-preview">
            <img v-if="fullSiteIconUrl" :src="fullSiteIconUrl" alt="网站图标" class="preview-icon-image">
            <span v-else class="preview-icon">🖼️</span>
            <span v-if="localSettings.siteTitle" class="preview-title">{{ localSettings.siteTitle }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { uploadFile, API_BASE_URL } from '../../utils/api';
import { updateSiteConfig, setBrowserFavicon as setBrowserFaviconFromStore } from '../../stores/siteConfig';

interface Props {
  settings: {
    siteTitle: string;
    siteIcon: string;
    siteFavicon: string;
    [key: string]: any;
  };
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [key: string, value: any];
}>();

const localSettings = ref({ ...props.settings });
const iconFileInput = ref<HTMLInputElement | null>(null);
const faviconFileInput = ref<HTMLInputElement | null>(null);

// 计算完整的图片 URL
const fullSiteIconUrl = computed(() => {
  if (!localSettings.value.siteIcon) return '';
  if (localSettings.value.siteIcon.startsWith('http://') || localSettings.value.siteIcon.startsWith('https://')) {
    return localSettings.value.siteIcon;
  }
  return `${API_BASE_URL}${localSettings.value.siteIcon}`;
});

const fullSiteFaviconUrl = computed(() => {
  if (!localSettings.value.siteFavicon) return '';
  if (localSettings.value.siteFavicon.startsWith('http://') || localSettings.value.siteFavicon.startsWith('https://')) {
    return localSettings.value.siteFavicon;
  }
  return `${API_BASE_URL}${localSettings.value.siteFavicon}`;
});

watch(() => props.settings, (newSettings) => {
  localSettings.value = { ...newSettings };
}, { deep: true });

const updateLocalSettings = (newSettings: any) => {
  localSettings.value = { ...newSettings };
};

const updateSetting = (key: string, value: any) => {
  localSettings.value = { ...localSettings.value, [key]: value };
  emit('update', key, value);
  // 同时更新 store，让其他组件也能同步获取到最新的配置
  if (key === 'siteTitle' || key === 'siteIcon' || key === 'siteFavicon') {
    updateSiteConfig({ [key]: value });
  }
};

const triggerIconFileInput = () => {
  iconFileInput.value?.click();
};

const triggerFaviconFileInput = () => {
  faviconFileInput.value?.click();
};

const setBrowserFavicon = (url: string) => {
  setBrowserFaviconFromStore(url);
};

const clearFavicon = () => {
  updateSetting('siteFavicon', '');
  // 移除所有已存在的 favicon links
  const existingLinks = document.querySelectorAll("link[rel~='icon']");
  existingLinks.forEach(link => link.remove());
};

const clearSiteIcon = () => {
  updateSetting('siteIcon', '');
};

const handleIconUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];

    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.ico', '.svg'];
    const maxSize = 5 * 1024 * 1024;
    const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

    if (!allowedExtensions.includes(extension)) {
      ElMessage.warning('不支持的图片格式，仅支持：jpg、jpeg、png、gif、webp、ico、svg');
      return;
    }

    if (file.size > maxSize) {
      ElMessage.warning('图片大小不能超过5MB');
      return;
    }

    try {
      const data = await uploadFile(file, 'site-ui');
      updateSetting('siteIcon', data);
      ElMessage.success('图标上传成功！');
    } catch (error: any) {
      console.error('上传错误:', error);
      ElMessage.error('图标上传失败，请稍后重试');
    }
  }
};

const handleFaviconUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];

    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.ico', '.svg'];
    const maxSize = 2 * 1024 * 1024;
    const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

    if (!allowedExtensions.includes(extension)) {
      ElMessage.warning('不支持的图片格式，仅支持：jpg、jpeg、png、gif、webp、ico、svg');
      return;
    }

    if (file.size > maxSize) {
      ElMessage.warning('图片大小不能超过2MB');
      return;
    }

    try {
      const data = await uploadFile(file, 'site-ui');
      updateSetting('siteFavicon', data);
      // 立即更新浏览器的 favicon
      setBrowserFavicon(data);
      ElMessage.success('Favicon 上传成功！');
    } catch (error: any) {
      console.error('上传错误:', error);
      ElMessage.error('Favicon 上传失败，请稍后重试');
    }
  }
};

defineExpose({
  updateLocalSettings
});
</script>

<style scoped>
.site-settings {
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.settings-section {
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.section-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.section-icon svg {
  width: 24px;
  height: 24px;
  animation: gearSpin 3s linear infinite;
}

@keyframes gearSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.section-title-wrap {
  flex: 1;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.section-desc {
  font-size: 16px;
  color: #64748b;
  margin: 4px 0 0 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  flex: 1;
  margin-right: 20px;
}

.label-text {
  display: block;
  font-size: 17px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 6px;
}

.label-desc {
  display: block;
  font-size: 15px;
  color: #64748b;
  line-height: 1.5;
}

.input-wrapper {
  display: flex;
  align-items: center;
  min-width: 280px;
}

.icon-upload-wrapper {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 280px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.current-icon-preview {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 2px solid #e2e8f0;
  overflow: hidden;
  flex-shrink: 0;
}

.icon-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.icon-emoji {
  font-size: 32px;
}

.icon-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.icon-file-input {
  display: none;
}

.upload-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.clear-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
}

.clear-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.4);
}

.emoji-input {
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 15px;
  color: #334155;
  background: white;
  transition: all 0.3s ease;
}

.emoji-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 15px;
  color: #334155;
  background: white;
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: #94a3b8;
}

.preview-wrapper {
  display: flex;
  align-items: center;
  min-width: 280px;
}

.site-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
}

.preview-icon {
  font-size: 28px;
}

.preview-icon-image {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.preview-title {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.3px;
}
</style>
