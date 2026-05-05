<template>
  <div class="email-settings">
    <div class="settings-section">
      <div class="section-header">
        <div class="section-icon" :class="{ 'active': true }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
            <polyline points="22,6 12,13 2,6"/>
          </svg>
        </div>
        <div class="section-title-wrap">
          <h3 class="section-title">邮件服务配置</h3>
          <p class="section-desc">管理邮件发送功能，包括验证码和通知</p>
        </div>
      </div>
      
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">SMTP 服务器</span>
          <span class="label-desc">邮件服务器地址，如 smtp.163.com</span>
        </div>
        <div class="input-wrapper">
          <div class="input-group">
            <input 
              type="text" 
              :value="localSettings.smtpHost" 
              @change="(e) => updateSetting('smtpHost', (e.target as HTMLInputElement).value)"
              class="text-input"
              placeholder="smtp.example.com"
            />
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">SMTP 端口</span>
          <span class="label-desc">常用端口：465 (SSL)、587 (STARTTLS)、25</span>
        </div>
        <div class="input-wrapper">
          <div class="input-group">
            <input 
              type="number" 
              :value="localSettings.smtpPort" 
              @change="(e) => updateSetting('smtpPort', parseInt((e.target as HTMLInputElement).value, 10))"
              class="text-input"
            />
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">SMTP 用户名</span>
          <span class="label-desc">用于登录邮件服务器的认证账号</span>
        </div>
        <div class="input-wrapper">
          <div class="input-group">
            <input 
              type="text" 
              :value="localSettings.smtpUsername" 
              @change="(e) => updateSetting('smtpUsername', (e.target as HTMLInputElement).value)"
              class="text-input"
              placeholder="your@email.com"
            />
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">SMTP 密码</span>
          <span class="label-desc">邮箱密码或授权码</span>
        </div>
        <div class="input-wrapper">
            <input 
              type="password" 
              :value="localSettings.smtpPassword" 
              @input="(e) => updateSetting('smtpPassword', (e.target as HTMLInputElement).value)"
              class="text-input"
              placeholder="••••••••"
              oncopy="return false"
              oncut="return false"
            />
          </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">启用 SSL</span>
          <span class="label-desc">使用 SSL/TLS 加密连接，推荐开启</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.smtpSsl" 
              @change="(e) => updateSetting('smtpSsl', (e.target as HTMLInputElement).checked)"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">发件人地址</span>
          <span class="label-desc">显示在邮件发件人位置的地址</span>
        </div>
        <div class="input-wrapper">
          <div class="input-group">
            <input 
              type="text" 
              :value="localSettings.emailFrom" 
              @change="handleEmailFromChange($event)"
              class="text-input"
              placeholder="系统通知 &lt;your@email.com&gt;"
            />
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">验证码长度</span>
          <span class="label-desc">生成的验证码位数（4-12位）</span>
        </div>
        <div class="input-wrapper">
          <div class="input-group suffix-group">
            <input 
              type="number" 
              :value="localSettings.verificationCodeLength" 
              :min="4" 
              :max="12"
              @change="handleCodeLengthChange($event)"
              class="text-input suffix-input"
            />
            <span class="input-suffix">位</span>
          </div>
          <div class="input-range">
            <div 
              class="range-track" :style="{ width: ((localSettings.verificationCodeLength - 4) / 8 * 100) + '%' }"></div>
          </div>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">验证码有效期</span>
          <span class="label-desc">验证码的有效时间（1-60分钟）</span>
        </div>
        <div class="input-wrapper">
          <div class="input-group suffix-group">
            <input 
              type="number" 
              :value="localSettings.verificationCodeExpireMinutes" 
              :min="1" 
              :max="60"
              @change="handleExpireTimeChange($event)"
              class="text-input suffix-input"
            />
            <span class="input-suffix">分钟</span>
          </div>
          <div class="input-range">
            <div 
              class="range-track" :style="{ width: ((localSettings.verificationCodeExpireMinutes - 1) / 59 * 100) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="test-section">
      <div class="test-header">
        <h3 class="section-title">测试邮件发送</h3>
        <button class="test-btn" @click="sendTestEmail" :disabled="isSending">
          {{ isSending ? '发送中...' : '发送测试邮件' }}
        </button>
      </div>
      <div class="test-input">
        <input 
          type="email" 
          v-model="testEmail"
          placeholder="请输入收件人邮箱"
          class="text-input"
          :class="{ 'error': showError }"
        />
      </div>
      <!-- 错误/成功提示 -->
      <transition name="slide-fade">
        <div v-if="showMessage" class="message-box" :class="messageType">
          <div class="message-icon">
            <svg v-if="messageType === 'error'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="15" y1="9" x2="9" y2="15"/>
              <line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
            <svg v-else-if="messageType === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22,4 12,14.01 9,11.01"/>
            </svg>
          </div>
          <span class="message-text">{{ messageText }}</span>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { getToken } from '../../stores/auth';

interface Props {
  settings: {
    emailEnabled: boolean;
    emailFrom: string;
    verificationCodeLength: number;
    verificationCodeExpireMinutes: number;
    smtpHost: string;
    smtpPort: number;
    smtpUsername: string;
    smtpPassword: string;
    smtpSsl: boolean;
  };
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [key: string, value: any];
}>();

const localSettings = ref({ ...props.settings });
const testEmail = ref('');
const isSending = ref(false);
const showMessage = ref(false);
const showError = ref(false);
const messageText = ref('');
const messageType = ref<'success' | 'error'>('error');

const showNotification = (message: string, type: 'success' | 'error' = 'error') => {
  messageText.value = message;
  messageType.value = type;
  showError.value = type === 'error';
  showMessage.value = true;
  setTimeout(() => {
    showMessage.value = false;
    showError.value = false;
  }, 4000);
};

const updateSetting = (key: string, value: any) => {
  localSettings.value = { ...localSettings.value, [key]: value };
  emit('update', key, value);
};

const handleEmailFromChange = (event: any) => {
  const value = event.target.value;
  if (value) {
    updateSetting('emailFrom', value);
  }
};

const handleCodeLengthChange = (event: any) => {
  let value = parseInt(event.target.value, 10);
  if (!isNaN(value)) {
    value = Math.max(4, Math.min(12, value));
    updateSetting('verificationCodeLength', value);
  }
};

const handleExpireTimeChange = (event: any) => {
  let value = parseInt(event.target.value, 10);
  if (!isNaN(value)) {
    value = Math.max(1, Math.min(60, value));
    updateSetting('verificationCodeExpireMinutes', value);
  }
};

const sendTestEmail = async () => {
  if (!testEmail.value) {
    showNotification('请输入收件人邮箱');
    return;
  }

  isSending.value = true;
  try {
    const response = await fetch('/api/settings/test-email', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({ to: testEmail.value })
    });

    if (response.ok) {
      showNotification('测试邮件发送成功！', 'success');
    } else {
      const errorText = await response.text();
      showNotification('测试邮件发送失败: ' + errorText);
    }
  } catch (error) {
    showNotification('测试邮件发送失败');
  } finally {
    isSending.value = false;
  }
};

defineExpose({
  updateLocalSettings: (newSettings: any) => {
    localSettings.value = { ...newSettings };
  }
});
</script>

<style scoped>
.email-settings {
  display: flex;
  flex-direction: column;
  gap: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 输入框错误状态 */
.text-input.error {
  border-color: #ef4444;
  background: #fef2f2;
}

/* 消息提示框样式 */
.message-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  margin-top: 12px;
}

.message-box.error {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border: 1px solid #fecaca;
  color: #991b1b;
}

.message-box.success {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border: 1px solid #bbf7d0;
  color: #166534;
}

.message-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.message-icon svg {
  width: 100%;
  height: 100%;
}

.message-text {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
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
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.section-icon.active {
  animation: pulse-envelope 2s ease-in-out infinite;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.4);
}

@keyframes pulse-envelope {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.section-icon.active svg {
  animation: bounce 1s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
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
  min-width: 0;
}

.label-text {
  display: block;
  font-size: 17px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 4px;
}

.label-desc {
  display: block;
  font-size: 14px;
  color: #64748b;
  line-height: 1.4;
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
  transition: all 0.2s ease;
  background: #f8fafc;
  font-weight: 500;
}

.text-input:focus {
  outline: none;
  border-color: #3b82f6;
  background: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
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

.suffix-group {
  width: 240px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.suffix-input {
  flex: 1;
  width: 100% !important;
  text-align: right;
}

.input-suffix {
  font-size: 15px;
  color: #64748b;
  font-weight: 500;
  white-space: nowrap;
  width: 50px;
  text-align: left;
}

.input-range {
  width: 240px;
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

.test-section {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 24px;
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.test-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.test-btn:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.test-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.test-input .text-input {
  width: 100%;
  box-sizing: border-box;
}
</style>
