<template>
  <div class="security-settings">
    <div class="settings-section">
      <div class="section-header">
        <div class="section-icon" :class="{ 'shielding': localSettings.emailEnabled || localSettings.captchaRegisterEnabled || localSettings.captchaForgotPasswordEnabled || localSettings.captchaLoginEnabled }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <path d="M9 12l2 2 4-4z"/>
          </svg>
        </div>
        <div class="section-title-wrap">
          <h3 class="section-title">安全设置</h3>
          <p class="section-desc">保护账户安全，配置验证和认证功能</p>
        </div>
      </div>
      
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">邮箱验证码功能</span>
          <span class="label-desc">控制注册和忘记密码时的邮箱验证码功能</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.emailEnabled"
              @change="handleEmailEnabledChange"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">注册人机验证</span>
          <span class="label-desc">注册时进行人机验证（关闭邮箱验证时直接在注册时触发）</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.captchaRegisterEnabled"
              @change="handleCaptchaRegisterChange"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">忘记密码人机验证</span>
          <span class="label-desc">忘记密码时进行人机验证（关闭邮箱验证时直接在重置密码时触发）</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.captchaForgotPasswordEnabled"
              @change="handleCaptchaForgotPasswordChange"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">登录人机验证</span>
          <span class="label-desc">登录时进行人机验证</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.captchaLoginEnabled"
              @change="handleCaptchaLoginChange"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <!-- 人机验证详细参数设置 -->
      <div class="detail-settings">
        <div class="setting-item captcha-settings-toggle" @click="showCaptchaSettings = !showCaptchaSettings">
          <div class="setting-label">
            <span class="label-text">人机验证设置</span>
            <span class="label-desc">配置验证码的详细参数</span>
          </div>
          <div class="toggle-wrapper">
            <svg class="expand-icon" :class="{ expanded: showCaptchaSettings }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </div>
        </div>
        <div class="collapsible-content" :class="{ expanded: showCaptchaSettings }">
          <div class="setting-item">
            <div class="setting-label">
              <span class="label-text">噪点数量</span>
              <span class="label-desc">验证码图片中的噪点数量 (0-500)</span>
            </div>
            <div class="input-wrapper">
              <input 
                type="number" 
                :value="localSettings.captchaNoiseCount"
                @input="handleNoiseCountChange"
                min="0"
                max="500"
                class="number-input"
              />
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-label">
              <span class="label-text">干扰线数量</span>
              <span class="label-desc">验证码图片中的干扰线数量 (0-20)</span>
            </div>
            <div class="input-wrapper">
              <input 
                type="number" 
                :value="localSettings.captchaLineCount"
                @input="handleLineCountChange"
                min="0"
                max="20"
                class="number-input"
              />
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-label">
              <span class="label-text">验证码长度</span>
              <span class="label-desc">验证码字符数量 (4-8)</span>
            </div>
            <div class="input-wrapper">
              <input 
                type="number" 
                :value="localSettings.captchaCodeLength"
                @input="handleCodeLengthChange"
                min="4"
                max="8"
                class="number-input"
              />
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-label">
              <span class="label-text">区分大小写</span>
              <span class="label-desc">验证码是否区分大小写</span>
            </div>
            <div class="toggle-wrapper">
              <label class="toggle-switch">
                <input 
                  type="checkbox" 
                  :checked="localSettings.captchaCaseSensitive"
                  @change="handleCaseSensitiveChange"
                />
                <span class="slider">
                  <span class="slider-dot"></span>
                </span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-label">
              <span class="label-text">随机旋转</span>
              <span class="label-desc">验证码字符是否随机旋转</span>
            </div>
            <div class="toggle-wrapper">
              <label class="toggle-switch">
                <input 
                  type="checkbox" 
                  :checked="localSettings.captchaRotateEnabled"
                  @change="handleRotateChange"
                />
                <span class="slider">
                  <span class="slider-dot"></span>
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="status-section">
      <div class="status-header">
        <h3 class="section-title">安全状态</h3>
      </div>
      <div class="status-details">
        <div class="status-item">
          <svg class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span class="status-label">邮箱验证</span>
          <div class="status-wrapper">
            <div class="status-indicator" :class="localSettings.emailEnabled ? 'enabled' : 'disabled'">
              <span class="indicator-dot"></span>
              <span class="indicator-text">{{ localSettings.emailEnabled ? '已启用' : '已禁用' }}</span>
            </div>
          </div>
        </div>
        <div class="status-item">
          <svg class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span class="status-label">人机验证</span>
          <div class="status-wrapper">
            <div class="status-indicator" :class="captchaStatusClass">
              <span class="indicator-dot"></span>
              <span class="indicator-text">{{ captchaStatusText }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'

interface Props {
  settings: {
    emailEnabled: boolean
    captchaRegisterEnabled: boolean
    captchaForgotPasswordEnabled: boolean
    captchaLoginEnabled: boolean
    captchaNoiseCount: number
    captchaLineCount: number
    captchaCodeLength: number
    captchaCaseSensitive: boolean
    captchaRotateEnabled: boolean
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [key: string, value: any]
}>()

const localSettings = ref({ ...props.settings })
const showCaptchaSettings = ref(false)

// 计算人机验证状态
const captchaStatusClass = computed(() => {
  const { captchaRegisterEnabled, captchaForgotPasswordEnabled, captchaLoginEnabled } = localSettings.value
  const allEnabled = captchaRegisterEnabled && captchaForgotPasswordEnabled && captchaLoginEnabled
  const anyEnabled = captchaRegisterEnabled || captchaForgotPasswordEnabled || captchaLoginEnabled
  
  if (allEnabled) return 'enabled'
  if (anyEnabled) return 'partial'
  return 'disabled'
})

const captchaStatusText = computed(() => {
  const { captchaRegisterEnabled, captchaForgotPasswordEnabled, captchaLoginEnabled } = localSettings.value
  const allEnabled = captchaRegisterEnabled && captchaForgotPasswordEnabled && captchaLoginEnabled
  const anyEnabled = captchaRegisterEnabled || captchaForgotPasswordEnabled || captchaLoginEnabled
  
  if (allEnabled) return '全部启用'
  if (anyEnabled) return '部分启用'
  return '已禁用'
})

const updateLocalSettings = (newSettings: any) => {
  localSettings.value = { ...newSettings }
}

const handleEmailEnabledChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  localSettings.value.emailEnabled = target.checked
  emit('update', 'emailEnabled', target.checked)
}

const handleCaptchaRegisterChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  localSettings.value.captchaRegisterEnabled = target.checked
  emit('update', 'captchaRegisterEnabled', target.checked)
}

const handleCaptchaForgotPasswordChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  localSettings.value.captchaForgotPasswordEnabled = target.checked
  emit('update', 'captchaForgotPasswordEnabled', target.checked)
}

const handleCaptchaLoginChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  localSettings.value.captchaLoginEnabled = target.checked
  emit('update', 'captchaLoginEnabled', target.checked)
}

const handleNoiseCountChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = parseInt(target.value)
  if (!isNaN(value)) {
    const clampedValue = Math.max(0, Math.min(500, value))
    localSettings.value.captchaNoiseCount = clampedValue
    emit('update', 'captchaNoiseCount', clampedValue)
  }
}

const handleLineCountChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = parseInt(target.value)
  if (!isNaN(value)) {
    const clampedValue = Math.max(0, Math.min(20, value))
    localSettings.value.captchaLineCount = clampedValue
    emit('update', 'captchaLineCount', clampedValue)
  }
}

const handleCodeLengthChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = parseInt(target.value)
  if (!isNaN(value)) {
    const clampedValue = Math.max(4, Math.min(8, value))
    localSettings.value.captchaCodeLength = clampedValue
    emit('update', 'captchaCodeLength', clampedValue)
  }
}

const handleCaseSensitiveChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  localSettings.value.captchaCaseSensitive = target.checked
  emit('update', 'captchaCaseSensitive', target.checked)
}

const handleRotateChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  localSettings.value.captchaRotateEnabled = target.checked
  emit('update', 'captchaRotateEnabled', target.checked)
}

watch(() => props.settings, (newSettings) => {
  localSettings.value = { ...newSettings }
}, { deep: true })

defineExpose({ updateLocalSettings })
</script>

<style scoped>
.security-settings {
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
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  overflow: visible;
}

.section-icon::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(0.5);
  width: 60px;
  height: 60px;
  background: radial-gradient(circle, rgba(251, 191, 36, 1) 0%, rgba(245, 158, 11, 0.8) 30%, rgba(245, 158, 11, 0.4) 60%, rgba(245, 158, 11, 0) 100%);
  border-radius: 50%;
  z-index: -1;
  animation: ripple1 2.5s ease-out infinite;
}

.section-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(0.5);
  width: 60px;
  height: 60px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.9) 0%, rgba(245, 158, 11, 0.7) 30%, rgba(245, 158, 11, 0.3) 60%, rgba(245, 158, 11, 0) 100%);
  border-radius: 50%;
  z-index: -2;
  animation: ripple2 2.5s ease-out infinite 0.8s;
}

.section-icon.shielding {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
}

@keyframes ripple1 {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(0.5);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(1.8);
  }
}

@keyframes ripple2 {
  0% {
    opacity: 0.8;
    transform: translate(-50%, -50%) scale(0.5);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(2.2);
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
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.section-desc {
  margin: 4px 0 0 0;
  font-size: 16px;
  color: #64748b;
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

.input-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 180px;
}

.number-input {
  width: 90px;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 17px;
  text-align: center;
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

.detail-settings {
  border-top: 1px solid #e2e8f0;
}

.detail-settings .setting-item {
  padding: 20px 24px;
}

.captcha-settings-toggle {
  cursor: pointer;
  transition: background-color 0.2s;
}

.captcha-settings-toggle:hover {
  background-color: #f8fafc;
}

.expand-icon {
  width: 24px;
  height: 24px;
  color: #64748b;
  transition: transform 0.3s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.collapsible-content {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.collapsible-content.expanded {
  max-height: 600px;
}

.collapsible-content .setting-item {
  opacity: 0;
  animation: fadeIn 0.3s ease forwards;
}

.collapsible-content.expanded .setting-item {
  opacity: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.collapsible-content .setting-item:nth-child(1) {
  animation-delay: 0.05s;
}

.collapsible-content .setting-item:nth-child(2) {
  animation-delay: 0.1s;
}

.collapsible-content .setting-item:nth-child(3) {
  animation-delay: 0.15s;
}

.collapsible-content .setting-item:nth-child(4) {
  animation-delay: 0.2s;
}

.collapsible-content .setting-item:nth-child(5) {
  animation-delay: 0.25s;
}

.status-section {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 24px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.status-header .section-title {
  margin: 0;
  font-size: 16px;
}

.status-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  height: 64px;
  min-height: 64px;
  box-sizing: border-box;
}

.status-icon {
  width: 20px;
  height: 20px;
  color: #64748b;
  flex-shrink: 0;
}

.status-label {
  flex: 1;
  font-size: 16px;
  color: #475569;
  font-weight: 500;
}

.status-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 180px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 500;
}

.status-indicator.enabled {
  background: #d1fae5;
  color: #065f46;
}

.status-indicator.partial {
  background: #fef3c7;
  color: #92400e;
}

.status-indicator.disabled {
  background: #fee2e2;
  color: #991b1b;
}

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.indicator-text {
  font-size: 16px;
}
</style>
