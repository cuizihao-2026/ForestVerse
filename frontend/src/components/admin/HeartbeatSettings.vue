<template>
  <div class="heartbeat-settings">
    <div class="settings-section">
      <div class="section-header">
        <div class="section-icon" :class="{ 'beating': localSettings.heartbeatEnabled }">
          <svg viewBox="0 0 24 24" fill="currentColor" stroke="none">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </div>
        <div class="section-title-wrap">
          <h3 class="section-title">WebSocket 心跳设置</h3>
          <p class="section-desc">实时检测连接状态，确保通信稳定可靠</p>
        </div>
      </div>
      
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">启用心跳检测</span>
          <span class="label-desc">定期发送心跳包，让连接始终保持活跃状态</span>
        </div>
        <div class="toggle-wrapper">
          <label class="toggle-switch">
            <input 
              type="checkbox" 
              :checked="localSettings.heartbeatEnabled" 
              @change="(e) => updateSetting('heartbeatEnabled', (e.target as HTMLInputElement).checked)"
            />
            <span class="slider">
              <span class="slider-dot"></span>
            </span>
          </label>
        </div>
      </div>

      <div class="collapsible-content" :class="{ 'disabled': !localSettings.heartbeatEnabled }">
        <div class="setting-item" :class="{ 'disabled': !localSettings.heartbeatEnabled }">
          <div class="setting-label">
            <span class="label-text">心跳频率</span>
            <span class="label-desc">控制心跳包发送的时间间隔（15-60秒）</span>
          </div>
          <div class="input-wrapper">
            <div class="input-group">
              <input 
                type="number" 
                :value="localSettings.heartbeatRate" 
                :min="15" 
                :max="60"
                :disabled="!localSettings.heartbeatEnabled"
                @change="handleRateChange($event)"
                class="number-input"
              />
              <span class="input-suffix">秒</span>
            </div>
            <div class="input-range">
              <div 
                class="range-track" :style="{ width: ((localSettings.heartbeatRate - 15) / 45 * 100) + '%' }"></div>
            </div>
          </div>
        </div>

        <div class="setting-item" :class="{ 'disabled': !localSettings.heartbeatEnabled }">
          <div class="setting-label">
            <span class="label-text">超时时间</span>
            <span class="label-desc">设定心跳响应的最大等待时间（{{ minTimeout }}-180秒）</span>
          </div>
          <div class="input-wrapper">
            <div class="input-group">
              <input 
                type="number" 
                :value="localSettings.heartbeatTimeout" 
                :min="minTimeout" 
                :max="180"
                :disabled="!localSettings.heartbeatEnabled"
                @change="handleTimeoutChange($event)"
                class="number-input"
              />
              <span class="input-suffix">秒</span>
            </div>
            <div class="input-range">
              <div 
                class="range-track" :style="{ width: ((localSettings.heartbeatTimeout - minTimeout) / (180 - minTimeout) * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="status-section">
      <div class="status-header">
        <h3 class="section-title">连接状态</h3>
      </div>
      <div class="status-details">
        <div class="status-item">
          <svg class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M5 12.55a11 11 0 0 1 14 0"/>
            <path d="M1.42 9a16 16 0 0 1 21.16 0"/>
            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
            <circle cx="12" cy="20" r="1"/>
          </svg>
          <span class="status-label">WebSocket 连接</span>
          <div class="status-wrapper">
            <div class="status-indicator" :class="wsStatus">
              <span class="indicator-dot"></span>
              <span class="indicator-text">{{ wsStatusText }}</span>
            </div>
          </div>
        </div>
        <div class="status-item">
          <svg class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12,6 12,12 16,14"/>
          </svg>
          <span class="status-label">最后心跳</span>
          <div class="status-wrapper">
            <span class="status-value">{{ lastHeartbeat || '未获取' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { checkWsStatus, getWsStatus } from '../../stores/websocket';

interface Props {
  settings: {
    heartbeatEnabled: boolean;
    heartbeatRate: number;
    heartbeatTimeout: number;
  };
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [key: string, value: any];
}>();

// 本地状态，用于即时反馈
const localSettings = ref({ ...props.settings });
const wsStatus = ref('disconnected');
const lastHeartbeat = ref('');
let statusCheckTimer: number | null = null;

const wsStatusText = computed(() => {
  const statusMap: Record<string, string> = {
    'connected': '已连接',
    'connecting': '连接中',
    'disconnecting': '断开中',
    'disconnected': '已断开'
  };
  return statusMap[wsStatus.value] || '未知';
});

const minTimeout = computed(() => {
  return localSettings.value.heartbeatRate + 10;
});

// 监听props变化
const updateLocalSettings = (newSettings: any) => {
  localSettings.value = { ...newSettings };
};

// 更新单个设置
const updateSetting = (key: string, value: any) => {
  localSettings.value = { ...localSettings.value, [key]: value };
  emit('update', key, value);
};

// 处理心跳频率变更
const handleRateChange = (event: any) => {
  let value = parseInt(event.target.value, 10);
  if (!isNaN(value)) {
    // 强制限制在合法范围内
    value = Math.max(15, Math.min(60, value));
    updateSetting('heartbeatRate', value);
    // 检查超时是否需要调整
    if (localSettings.value.heartbeatTimeout < value + 10) {
      const newTimeout = value + 10;
      localSettings.value.heartbeatTimeout = Math.min(newTimeout, 180);
      emit('update', 'heartbeatTimeout', localSettings.value.heartbeatTimeout);
    }
  }
};

// 处理超时时间变更
const handleTimeoutChange = (event: any) => {
  let value = parseInt(event.target.value, 10);
  const requiredMin = minTimeout.value;
  if (!isNaN(value)) {
    // 强制限制在合法范围内
    value = Math.max(requiredMin, Math.min(180, value));
    updateSetting('heartbeatTimeout', value);
  }
};

const handleWsStatusChange = (event: CustomEvent) => {
  wsStatus.value = event.detail.status
  if (event.detail.lastHeartbeat) {
    lastHeartbeat.value = event.detail.lastHeartbeat
  }
}

defineExpose({
  updateLocalSettings,
  handleWsStatusChange
})

// 组件挂载时启动状态检查
onMounted(() => {
  // 获取初始状态
  const initialStatus = getWsStatus()
  wsStatus.value = initialStatus.status
  if (initialStatus.lastHeartbeat) {
    lastHeartbeat.value = initialStatus.lastHeartbeat
  }
  window.addEventListener('ws-status-changed', handleWsStatusChange as EventListener)
  statusCheckTimer = window.setInterval(() => {
    checkWsStatus()
  }, 1000)
})

// 组件卸载时停止状态检查
onUnmounted(() => {
  window.removeEventListener('ws-status-changed', handleWsStatusChange as EventListener)
  if (statusCheckTimer !== null) {
    clearInterval(statusCheckTimer)
    statusCheckTimer = null
  }
});
</script>

<style scoped>
.heartbeat-settings {
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
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.section-icon.beating {
  animation: heartbeat 1.2s ease-in-out infinite;
  box-shadow: 0 0 20px rgba(239, 68, 68, 0.4);
}

.section-icon.beating svg {
  animation: heartbeat-pulse 1.2s ease-in-out infinite;
}

@keyframes heartbeat {
  0%, 100% {
    transform: scale(1);
  }
  10% {
    transform: scale(1.08);
  }
  20% {
    transform: scale(1);
  }
  30% {
    transform: scale(1.05);
  }
  40% {
    transform: scale(1);
  }
}

@keyframes heartbeat-pulse {
  0%, 100% {
    opacity: 1;
  }
  10% {
    opacity: 0.8;
  }
  20% {
    opacity: 1;
  }
  30% {
    opacity: 0.9;
  }
  40% {
    opacity: 1;
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
  flex-direction: column;
  gap: 12px;
  min-width: 180px;
}

.toggle-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 180px;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.number-input {
  width: 90px;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 17px;
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

.number-input.error {
  border-color: #ef4444;
  background: #fef2f2;
}

.number-input.error:focus {
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.1);
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

.validation-error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  font-size: 13px;
  color: #dc2626;
  font-weight: 500;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 过渡动画 */
.fade-expand-enter-active,
.fade-expand-leave-active {
  transition: all 0.3s ease;
}

.fade-expand-enter-from,
.fade-expand-leave-to {
  opacity: 0;
  max-height: 0;
  overflow: hidden;
}

.collapsible-content {
  overflow: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 状态区域 */
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

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 500;
}

.status-indicator.connected {
  background: #d1fae5;
  color: #065f46;
}

.status-indicator.connecting {
  background: #fef3c7;
  color: #92400e;
}

.status-indicator.disconnecting {
  background: #fed7aa;
  color: #9a3412;
}

.status-indicator.disconnected {
  background: #fee2e2;
  color: #991b1b;
}

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-indicator.connected .indicator-dot {
  background: #10b981;
}

.status-indicator.connecting .indicator-dot {
  background: #f59e0b;
}

.status-indicator.disconnecting .indicator-dot {
  background: #f97316;
}

.status-indicator.disconnected .indicator-dot {
  background: #ef4444;
  animation: none;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
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

.status-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 180px;
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

.status-value {
  font-size: 16px;
  color: #1e293b;
  font-weight: 600;
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
</style>
