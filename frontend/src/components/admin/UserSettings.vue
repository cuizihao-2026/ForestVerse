<template>
  <div class="user-settings">
    <div class="settings-section">
      <div class="section-header">
        <div class="section-icon bouncing">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
          </svg>
        </div>
        <div class="section-title-wrap">
          <h3 class="section-title">用户设置</h3>
          <p class="section-desc">配置用户相关的功能选项</p>
        </div>
      </div>
      
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">开启文章审核</span>
          <span class="label-desc">用户发布的文章需要经过审核才能发布</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.articleAuditEnabled" 
              @change="(e) => updateSetting('articleAuditEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">开启评论审核</span>
          <span class="label-desc">用户发布的评论需要经过审核才能展示</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.commentAuditEnabled" 
              @change="(e) => updateSetting('commentAuditEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface Props {
  settings: {
    articleAuditEnabled: boolean;
    commentAuditEnabled: boolean;
    [key: string]: any;
  };
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [key: string, value: any];
}>();

const localSettings = ref({ ...props.settings });

// 监听 props 变化
watch(() => props.settings, (newSettings) => {
  localSettings.value = { ...newSettings };
}, { deep: true });

const updateLocalSettings = (newSettings: any) => {
  localSettings.value = { ...newSettings };
};

const updateSetting = (key: string, value: any) => {
  console.log('更新设置:', key, value);
  localSettings.value = { ...localSettings.value, [key]: value };
  emit('update', key, value);
};

defineExpose({
  updateLocalSettings
});
</script>

<style scoped>
.user-settings {
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
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.section-icon.bouncing {
  animation: bounce 2s ease-in-out infinite;
  box-shadow: 0 0 20px rgba(107, 114, 128, 0.4);
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

.section-icon svg {
  width: 24px;
  height: 24px;
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

.toggle-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 180px;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 56px;
  height: 30px;
  flex-shrink: 0;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e1;
  transition: 0.3s;
  border-radius: 30px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.slider-dot {
  position: absolute;
  content: "";
  height: 24px;
  width: 24px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

input:checked + .slider {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

input:checked + .slider .slider-dot {
  transform: translateX(26px);
}
</style>
