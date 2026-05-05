<template>
  <div class="dashboard-stats">
    <!-- 系统资源在上 -->
    <div class="resource-section">
      <div class="resource-card">
        <div class="resource-item">
          <div class="resource-icon">💻</div>
          <div class="resource-info">
            <div class="resource-label">CPU 使用率</div>
            <div class="resource-sub">{{ safeServerStats.availableProcessors }} 核心</div>
          </div>
          <div class="resource-metric">
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: safeServerStats.cpuPercent + '%' }"></div>
            </div>
            <div class="metric-value">{{ safeServerStats.cpuPercent.toFixed(1) }}%</div>
          </div>
        </div>
        <div class="resource-item">
          <div class="resource-icon">🧠</div>
          <div class="resource-info">
            <div class="resource-label">系统内存</div>
            <div class="resource-sub">{{ safeServerStats.usedMemory.toFixed(0) }} / {{ safeServerStats.totalMemory.toFixed(0) }} MB</div>
          </div>
          <div class="resource-metric">
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: safeServerStats.memoryPercent + '%' }"></div>
            </div>
            <div class="metric-value">{{ safeServerStats.memoryPercent.toFixed(1) }}%</div>
          </div>
        </div>
        <div class="resource-item">
          <div class="resource-icon">☕</div>
          <div class="resource-info">
            <div class="resource-label">Java Heap</div>
            <div class="resource-sub">{{ safeServerStats.heapMemoryUsed.toFixed(0) }} / {{ safeServerStats.heapMemoryMax.toFixed(0) }} MB</div>
          </div>
          <div class="resource-metric">
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: safeServerStats.heapMemoryPercent + '%' }"></div>
            </div>
            <div class="metric-value">{{ safeServerStats.heapMemoryPercent.toFixed(1) }}%</div>
          </div>
        </div>
        <div class="resource-item">
          <div class="resource-icon">⚡</div>
          <div class="resource-info">
            <div class="resource-label">系统负载</div>
            <div class="resource-sub">{{ safeServerStats.systemLoadText }}</div>
          </div>
          <div class="resource-metric">
            <div class="load-indicator">
              <span class="load-dot"></span>
              正常
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计信息在下 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ safeStats.publishedArticles }}</div>
          <div class="stat-label">已发布文章</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">⏳</div>
        <div class="stat-info">
          <div class="stat-value">{{ safeStats.pendingCount }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ safeStats.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card clickable" @click="handleViewOnline">
        <div class="stat-icon">👤</div>
        <div class="stat-info">
          <div class="stat-value">{{ safeStats.onlineUsers }}</div>
          <div class="stat-label">在线用户</div>
        </div>
        <span class="view-hint">查看 →</span>
      </div>
    </div>

    <!-- 强制刷新按钮 -->
    <div class="action-row">
      <button class="btn-force-refresh" @click="handleForceRefresh" :disabled="isRefreshing">
        <span v-if="isRefreshing" class="loading-spinner"></span>
        {{ isRefreshing ? '刷新中...' : '强制刷新' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { post } from '../../utils/api';
import { showModal } from '../../stores/modal';

const props = defineProps<{
  stats: {
    publishedArticles?: number;
    pendingCount?: number;
    totalUsers?: number;
    onlineUsers?: number;
  };
  serverStats: {
    cpuPercent?: number;
    memoryPercent?: number;
    usedMemory?: number;
    totalMemory?: number;
    heapMemoryPercent?: number;
    heapMemoryUsed?: number;
    heapMemoryMax?: number;
    availableProcessors?: number;
    systemLoad?: number;
  };
}>();

const emit = defineEmits<{
  (e: 'viewOnline'): void;
}>();

const isRefreshing = ref(false);

const safeStats = computed(() => ({
  publishedArticles: props.stats.publishedArticles ?? 0,
  pendingCount: props.stats.pendingCount ?? 0,
  totalUsers: props.stats.totalUsers ?? 0,
  onlineUsers: props.stats.onlineUsers ?? 0
}));

const safeServerStats = computed(() => {
  const cpuPercent = props.serverStats.cpuPercent ?? 0;
  const memoryPercent = props.serverStats.memoryPercent ?? 0;
  const heapMemoryPercent = props.serverStats.heapMemoryPercent ?? 0;
  const systemLoad = props.serverStats.systemLoad ?? 0;
  
  return {
    cpuPercent: Math.min(cpuPercent, 100),
    memoryPercent: Math.min(memoryPercent, 100),
    usedMemory: props.serverStats.usedMemory ?? 0,
    totalMemory: props.serverStats.totalMemory ?? 0,
    heapMemoryPercent: Math.min(heapMemoryPercent, 100),
    heapMemoryUsed: props.serverStats.heapMemoryUsed ?? 0,
    heapMemoryMax: props.serverStats.heapMemoryMax ?? 0,
    availableProcessors: props.serverStats.availableProcessors ?? 4,
    systemLoad,
    systemLoadText: systemLoad > 0 ? systemLoad.toFixed(2) : '-'
  };
});

const handleViewOnline = () => {
  emit('viewOnline');
};

const handleForceRefresh = async () => {
  try {
    isRefreshing.value = true;
    const result = await post('/api/monitor/force-refresh');
    showModal({
      title: '成功',
      message: `已向 ${result.data?.onlineUsers || 0} 位在线用户发送刷新通知`,
      type: 'success'
    });
  } catch (e: any) {
    showModal({
      title: '错误',
      message: e.message || '发送刷新通知失败',
      type: 'error'
    });
  } finally {
    isRefreshing.value = false;
  }
};
</script>

<style scoped>
.dashboard-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
  position: relative;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.stat-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #9ca3af;
}

.view-hint {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.2s;
}

.stat-card.clickable:hover .view-hint {
  opacity: 1;
}

.resource-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resource-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
}

.resource-item {
  flex: 1;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
  border-right: 1px solid #f3f4f6;
  text-align: center;
}

.resource-item:last-child {
  border-right: none;
}

.resource-item:hover {
  background: #f9fafb;
}

.resource-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.resource-info {
  flex: 1;
  min-width: 0;
}

.resource-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}

.resource-sub {
  font-size: 12px;
  color: #9ca3af;
}

.resource-metric {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.metric-bar {
  width: 120px;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.metric-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.metric-value {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  min-width: 50px;
  text-align: right;
}

.load-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #10b981;
  font-weight: 500;
}

.load-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.action-row {
  display: flex;
  justify-content: flex-start;
}

.btn-force-refresh {
  padding: 12px 32px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.btn-force-refresh:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  background: #2563eb;
}

.btn-force-refresh:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .resource-card {
    flex-direction: column;
  }
  
  .resource-item {
    flex-direction: row;
    border-right: none;
    border-bottom: 1px solid #f3f4f6;
  }
  
  .resource-item:last-child {
    border-bottom: none;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>
