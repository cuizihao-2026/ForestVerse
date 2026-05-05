<template>
  <div class="ai-settings">
    <div class="settings-section">
      <div class="section-header">
        <div class="section-icon beating">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v6l4 2"/>
          </svg>
        </div>
        <div class="section-title-wrap">
          <h3 class="section-title">AI 设置</h3>
          <p class="section-desc">配置 AI 助读功能，支持多个 AI 提供商</p>
        </div>
      </div>
      
        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">AI 提供商</span>
            <span class="label-desc">选择使用的 AI 服务提供商</span>
          </div>
          <div class="input-wrapper">
            <select 
              :value="localSettings.aiProvider" 
              @change="handleProviderChange"
              class="select-input"
            >
              <option value="deepseek">DeepSeek</option>
              <option value="kimi">Kimi</option>
              <option value="qwen">千问</option>
            </select>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">API Key</span>
            <span class="label-desc">{{ currentProviderConfig.apiKeyDesc }}</span>
          </div>
          <div class="input-wrapper">
            <input 
              type="password" 
              :value="localSettings.aiApiKey" 
              @input="(e) => updateSetting('aiApiKey', (e.target as HTMLInputElement).value)"
              class="text-input"
              :placeholder="currentProviderConfig.apiKeyPlaceholder"
            />
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">API 地址</span>
            <span class="label-desc">{{ currentProviderConfig.baseUrlDesc }}</span>
          </div>
          <div class="input-wrapper">
            <input 
              type="text" 
              :value="localSettings.aiBaseUrl" 
              @input="(e) => updateSetting('aiBaseUrl', (e.target as HTMLInputElement).value)"
              class="text-input"
              :placeholder="currentProviderConfig.baseUrlPlaceholder"
            />
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">模型</span>
            <span class="label-desc">选择使用的模型</span>
          </div>
          <div class="input-wrapper">
            <select 
              :value="localSettings.aiModel" 
              @change="(e) => updateSetting('aiModel', (e.target as HTMLSelectElement).value)"
              class="select-input"
            >
              <option 
                v-for="model in currentProviderConfig.models" 
                :key="model" 
                :value="model"
              >
                {{ model }}
              </option>
            </select>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">系统提示词</span>
            <span class="label-desc">自定义 AI 的角色和行为（留空使用默认）</span>
          </div>
          <div class="input-wrapper">
            <div class="textarea-container">
              <textarea 
                :value="localSettings.aiSystemPrompt" 
                @input="(e) => updateSetting('aiSystemPrompt', (e.target as HTMLTextAreaElement).value)"
                class="textarea-input"
                placeholder="你是一个文章阅读助手，帮助用户理解文章内容..."
                rows="4"
                maxlength="500"
              />
              <span class="char-count">{{ (localSettings.aiSystemPrompt || '').length }}/500</span>
            </div>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">启用 AI 文章助读</span>
            <span class="label-desc">开启后，用户可以使用 AI 助读功能来理解文章</span>
          </div>
          <div class="toggle-wrapper">
            <label class="toggle-switch">
              <input 
                type="checkbox" 
                :checked="localSettings.aiReadingEnabled" 
                @change="(e) => updateSetting('aiReadingEnabled', (e.target as HTMLInputElement).checked)"
              />
              <span class="slider">
                <span class="slider-dot"></span>
              </span>
            </label>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">启用 AI 文章审核</span>
            <span class="label-desc">开启后，可以使用 AI 自动审核待发布文章</span>
          </div>
          <div class="toggle-wrapper">
            <label class="toggle-switch">
              <input 
                type="checkbox" 
                :checked="localSettings.aiArticleAuditEnabled" 
                @change="(e) => updateSetting('aiArticleAuditEnabled', (e.target as HTMLInputElement).checked)"
              />
              <span class="slider">
                <span class="slider-dot"></span>
              </span>
            </label>
          </div>
        </div>

        <div class="setting-item" :class="{ 'disabled': !localSettings.aiArticleAuditEnabled }">
          <div class="setting-label">
            <span class="label-text">文章全自动审核</span>
            <span class="label-desc">开启后，新文章发布时自动调用 AI 审核并处理</span>
          </div>
          <div class="toggle-wrapper">
            <label class="toggle-switch">
              <input 
                type="checkbox" 
                :checked="localSettings.aiArticleAutoAuditEnabled" 
                :disabled="!localSettings.aiArticleAuditEnabled"
                @change="(e) => updateSetting('aiArticleAutoAuditEnabled', (e.target as HTMLInputElement).checked)"
              />
              <span class="slider">
                <span class="slider-dot"></span>
              </span>
            </label>
          </div>
        </div>

        <div class="setting-item" :class="{ 'disabled': !localSettings.aiArticleAuditEnabled }">
          <div class="setting-label">
            <span class="label-text">文章审核提示词</span>
            <span class="label-desc">自定义 AI 文章审核的提示词，可使用 {title} 和 {content} 占位符（留空使用默认）</span>
          </div>
          <div class="input-wrapper">
            <div class="textarea-container">
              <textarea 
                :value="localSettings.aiArticleAuditPrompt" 
                @input="(e) => updateSetting('aiArticleAuditPrompt', (e.target as HTMLTextAreaElement).value)"
                class="textarea-input"
                placeholder="你是一名专业的文章审核员..."
                rows="4"
                maxlength="1000"
                :disabled="!localSettings.aiArticleAuditEnabled"
              />
              <span class="char-count">{{ (localSettings.aiArticleAuditPrompt || '').length }}/1000</span>
            </div>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            <span class="label-text">启用 AI 评论审核</span>
            <span class="label-desc">开启后，可以使用 AI 自动审核用户评论</span>
          </div>
          <div class="toggle-wrapper">
            <label class="toggle-switch">
              <input 
                type="checkbox" 
                :checked="localSettings.aiCommentAuditEnabled" 
                @change="(e) => updateSetting('aiCommentAuditEnabled', (e.target as HTMLInputElement).checked)"
              />
              <span class="slider">
                <span class="slider-dot"></span>
              </span>
            </label>
          </div>
        </div>

        <div class="setting-item" :class="{ 'disabled': !localSettings.aiCommentAuditEnabled }">
          <div class="setting-label">
            <span class="label-text">评论全自动审核</span>
            <span class="label-desc">开启后，新评论发布时自动调用 AI 审核并处理</span>
          </div>
          <div class="toggle-wrapper">
            <label class="toggle-switch">
              <input 
                type="checkbox" 
                :checked="localSettings.aiCommentAutoAuditEnabled" 
                :disabled="!localSettings.aiCommentAuditEnabled"
                @change="(e) => updateSetting('aiCommentAutoAuditEnabled', (e.target as HTMLInputElement).checked)"
              />
              <span class="slider">
                <span class="slider-dot"></span>
              </span>
            </label>
          </div>
        </div>

        <div class="setting-item" :class="{ 'disabled': !localSettings.aiCommentAuditEnabled }">
            <div class="setting-label">
                <span class="label-text">评论审核提示词</span>
                <span class="label-desc">自定义 AI 评论审核的提示词，可使用 {content} 占位符（留空使用默认）</span>
            </div>
            <div class="input-wrapper">
                <div class="textarea-container">
                    <textarea 
                        :value="localSettings.aiCommentAuditPrompt" 
                        @input="(e) => updateSetting('aiCommentAuditPrompt', (e.target as HTMLTextAreaElement).value)"
                        class="textarea-input"
                        placeholder="你是一名专业的评论审核员..."
                        rows="4"
                        maxlength="1000"
                        :disabled="!localSettings.aiCommentAuditEnabled"
                    />
                    <span class="char-count">{{ (localSettings.aiCommentAuditPrompt || '').length }}/1000</span>
                </div>
            </div>
        </div>

        <div class="balance-section">
            <div class="balance-header">
                <div class="section-icon" style="width: 40px; height: 40px;">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.97V5H10.9v1.69c-1.51.32-2.72 1.3-2.72 2.81 0 1.79 1.49 2.69 3.66 3.21 1.95.46 2.34 1.15 2.34 1.87 0 .53-.39 1.39-2.1 1.39-1.6 0-2.23-.72-2.32-1.64H8.04c.1 1.7 1.36 2.66 2.86 2.97V19h2.34v-1.67c1.52-.29 2.72-1.16 2.73-2.77-.01-2.2-1.9-2.96-3.66-3.42z"/>
                    </svg>
                </div>
                <div style="flex: 1;">
                    <h3 class="label-text" style="margin-bottom: 0;">查询余额</h3>
                    <p class="label-desc">查询当前AI提供商的账户余额信息</p>
                </div>
                <button 
                    class="balance-button" 
                    @click="queryBalance"
                    :disabled="queryingBalance || !localSettings.aiApiKey"
                >
                    {{ queryingBalance ? '查询中...' : '查询余额' }}
                </button>
            </div>
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { ElMessageBox } from 'element-plus';
import { get } from '../../utils/api';

interface Props {
    settings: {
        aiProvider: string;
        aiApiKey: string;
        aiBaseUrl: string;
        aiModel: string;
        aiSystemPrompt?: string;
        aiArticleAuditPrompt?: string;
        aiCommentAuditPrompt?: string;
        aiReadingEnabled?: boolean;
        aiArticleAuditEnabled?: boolean;
        aiCommentAuditEnabled?: boolean;
        aiArticleAutoAuditEnabled?: boolean;
        aiCommentAutoAuditEnabled?: boolean;
        [key: string]: any;
    };
}

interface ProviderConfig {
    apiKeyDesc: string;
    apiKeyPlaceholder: string;
    baseUrlDesc: string;
    baseUrlPlaceholder: string;
    models: string[];
    defaultBaseUrl: string;
    defaultModel: string;
}

const providerConfigs: Record<string, ProviderConfig> = {
    deepseek: {
        apiKeyDesc: '您的 DeepSeek API Key，从 https://platform.deepseek.com 获取',
        apiKeyPlaceholder: '请输入 DeepSeek API Key',
        baseUrlDesc: 'DeepSeek API 的基础 URL',
        baseUrlPlaceholder: 'https://api.deepseek.com',
        models: ['deepseek-chat', 'deepseek-reasoner', 'deepseek-v4-flash', 'deepseek-v4-pro'],
        defaultBaseUrl: 'https://api.deepseek.com',
        defaultModel: 'deepseek-chat'
    },
    kimi: {
        apiKeyDesc: '您的 Kimi API Key，从 https://platform.moonshot.cn 获取',
        apiKeyPlaceholder: '请输入 Kimi API Key',
        baseUrlDesc: 'Kimi API 的基础 URL',
        baseUrlPlaceholder: 'https://api.moonshot.cn/v1',
        models: ['kimi-k2.6', 'kimi-k2.5', 'moonshot-v1-32k', 'moonshot-v1-128k'],
        defaultBaseUrl: 'https://api.moonshot.cn/v1',
        defaultModel: 'kimi-k2.6'
    },
    qwen: {
        apiKeyDesc: '您的千问 API Key，从 https://bailian.console.aliyun.com 获取',
        apiKeyPlaceholder: '请输入千问 API Key',
        baseUrlDesc: '千问 API 的基础 URL',
        baseUrlPlaceholder: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        models: ['qwen3.6-max-preview', 'qwen3.6-plus', 'qwen3.6-flash'],
        defaultBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        defaultModel: 'qwen3.6-max-preview'
    }
};

const props = defineProps<Props>();
const emit = defineEmits<{
    update: [key: string, value: any];
}>();

const localSettings = ref({ ...props.settings });
const queryingBalance = ref(false);

const currentProviderConfig = computed(() => {
    return providerConfigs[localSettings.value.aiProvider] || providerConfigs.deepseek;
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
};

const handleProviderChange = (e: Event) => {
    const provider = (e.target as HTMLSelectElement).value;
    const config = providerConfigs[provider];
    
    // 只更新提供商相关的配置，不覆盖API Key
    const updates: Record<string, any> = {
        aiProvider: provider,
        aiBaseUrl: config.defaultBaseUrl,
        aiModel: config.defaultModel
    };
    
    // 更新本地状态
    localSettings.value = {
        ...localSettings.value,
        ...updates
    };
    
    // 发送更新事件
    emit('update', 'aiProvider', provider);
    emit('update', 'aiBaseUrl', config.defaultBaseUrl);
    emit('update', 'aiModel', config.defaultModel);
};

const queryBalance = async () => {
    try {
        queryingBalance.value = true;
        const result = await get('/api/ai/balance');
        const balanceData = result?.data || result;
        
        if (balanceData.success) {
            let message = `<div style="line-height: 1.8;">`;
            message += `<p><strong>提供商:</strong> ${balanceData.provider || '未知'}</p>`;
            if (balanceData.isAvailable !== undefined) {
                message += `<p><strong>账户状态:</strong> ${balanceData.isAvailable ? '可用' : '不可用'}</p>`;
            }
            const balance = getTotalBalanceFromResult(balanceData);
            if (balance) {
                message += `<p><strong>总余额:</strong> <span style="color: #7c3aed; font-size: 1.2em; font-weight: bold;">${balance} 元</span></p>`;
            }
            message += `</div>`;
            
            ElMessageBox.alert(message, '查询成功', {
                dangerouslyUseHTMLString: true,
                confirmButtonText: '确定'
            });
        } else {
            ElMessageBox.alert(balanceData.message || '查询失败', '错误', {
                confirmButtonText: '确定',
                type: 'error'
            });
        }
    } catch (error) {
        console.error('查询余额失败:', error);
        ElMessageBox.alert(error instanceof Error ? error.message : '查询失败，请稍后重试', '错误', {
            confirmButtonText: '确定',
            type: 'error'
        });
    } finally {
        queryingBalance.value = false;
    }
};

const getTotalBalanceFromResult = (result: any) => {
    if (!result) return null;
    
    if (result.availableBalance) {
        const num = parseFloat(result.availableBalance);
        return isNaN(num) ? result.availableBalance : num.toFixed(2);
    }
    
    const list = result.balanceInfos || result.balance_infos;
    if (list) {
        const firstItem = Array.isArray(list) ? list[0] : list;
        if (firstItem && typeof firstItem === 'object') {
            const balance = firstItem.total_balance || firstItem.balance || firstItem.value || firstItem.granted_balance || firstItem.topped_up_balance;
            if (balance) {
                const num = parseFloat(balance);
                return isNaN(num) ? balance : num.toFixed(2);
            }
        }
    }
    
    return null;
};

defineExpose({
    updateLocalSettings
});
</script>

<style scoped>
.ai-settings {
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
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  overflow: visible;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.section-icon.beating {
  animation: ai-pulse 2s ease-in-out infinite;
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.4);
}

@keyframes ai-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.section-icon svg {
  width: 24px;
  height: 24px;
  transition: transform 0.3s ease;
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
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  min-height: 72px;
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
  flex-direction: column;
  gap: 8px;
  min-width: 240px;
  align-items: flex-end;
}

.toggle-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 240px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: flex-end;
}

.text-input {
  width: 240px;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  color: #334155;
  background: #f8fafc;
  transition: all 0.2s ease;
  font-weight: 500;
}

.text-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.text-input:disabled {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.text-input::placeholder {
  color: #94a3b8;
}

.select-input {
  width: 240px;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  color: #334155;
  background: #f8fafc;
  transition: all 0.2s ease;
  cursor: pointer;
  font-weight: 500;
}

.select-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.select-input:disabled {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.textarea-container {
  position: relative;
  width: 240px;
}

.textarea-input {
  width: 100%;
  padding: 10px 14px 32px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  color: #334155;
  background: #f8fafc;
  transition: all 0.2s ease;
  font-weight: 500;
  font-family: inherit;
  resize: none;
  line-height: 1.5;
  box-sizing: border-box;
}

.textarea-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.textarea-input:disabled {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.textarea-input::placeholder {
  color: #94a3b8;
}

.char-count {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #94a3b8;
  pointer-events: none;
}

.number-input {
  width: 100px;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 16px;
  text-align: right;
  transition: all 0.2s ease;
  background: #f8fafc;
  font-weight: 500;
}

.number-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.number-input:disabled {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.setting-item.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.collapsible-content.disabled {
  pointer-events: none;
}

.input-suffix {
  font-size: 16px;
  color: #64748b;
  font-weight: 500;
}

.input-range {
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.range-track {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.collapsible-content {
  overflow: hidden;
}

/* 开关按钮样式 */
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

.balance-section {
    padding: 20px 24px;
    border-top: 1px solid #f1f5f9;
}

.balance-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
}

.balance-button {
    padding: 10px 20px;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    width: fit-content;
}

.balance-button:hover:not(:disabled) {
    background: #2563eb;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.balance-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
</style>
