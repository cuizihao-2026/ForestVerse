<template>
  <div class="resource-stats">
    <div class="resource-card">
      <div class="resource-item">
        <div class="resource-icon">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="resource-info">
          <div class="resource-label">CPU 使用率</div>
          <div class="resource-sub">{{ (serverStats.availableProcessors || 4) }} 核心</div>
        </div>
        <div class="resource-metric">
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: Math.min(serverStats.cpuPercent || 0, 100) + '%' }"></div>
          </div>
          <div class="metric-value">{{ (serverStats.cpuPercent || 0).toFixed(1) }}%</div>
        </div>
      </div>
      <div class="resource-item">
        <div class="resource-icon">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="resource-info">
          <div class="resource-label">系统内存</div>
          <div class="resource-sub">{{ (serverStats.usedMemory || 0).toFixed(0) }} / {{ (serverStats.totalMemory || 0).toFixed(0) }} MB</div>
        </div>
        <div class="resource-metric">
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: Math.min(serverStats.memoryPercent || 0, 100) + '%' }"></div>
          </div>
          <div class="metric-value">{{ (serverStats.memoryPercent || 0).toFixed(1) }}%</div>
        </div>
      </div>
      <div class="resource-item">
        <div class="resource-icon">
          <el-icon><Box /></el-icon>
        </div>
        <div class="resource-info">
          <div class="resource-label">Java Heap</div>
          <div class="resource-sub">{{ (serverStats.heapMemoryUsed || 0).toFixed(0) }} / {{ (serverStats.heapMemoryMax || 0).toFixed(0) }} MB</div>
        </div>
        <div class="resource-metric">
          <div class="metric-bar">
            <div class="metric-fill" :style="{ width: Math.min(serverStats.heapMemoryPercent || 0, 100) + '%' }"></div>
          </div>
          <div class="metric-value">{{ (serverStats.heapMemoryPercent || 0).toFixed(1) }}%</div>
        </div>
      </div>
      <div class="resource-item">
        <div class="resource-icon">
          <el-icon><Lightning /></el-icon>
        </div>
        <div class="resource-info">
          <div class="resource-label">系统负载</div>
          <div class="resource-sub">{{ serverStats.systemLoad > 0 ? serverStats.systemLoad.toFixed(2) : '-' }}</div>
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
</template>

<script setup lang="ts">
import { Cpu, Odometer, Box, Lightning } from '@element-plus/icons-vue'

const props = defineProps<{
  serverStats: {
    cpuPercent: number;
    memoryPercent: number;
    usedMemory: number;
    totalMemory: number;
    heapMemoryPercent: number;
    heapMemoryUsed: number;
    heapMemoryMax: number;
    availableProcessors: number;
    systemLoad: number;
  };
}>();
</script>

<style scoped>
.resource-stats {
  margin-bottom: 40px;
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
  display: flex;
  align-items: center;
  justify-content: center;
}

.resource-icon .el-icon {
  width: 28px;
  height: 28px;
  color: #64748b;
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

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@media (max-width: 768px) {
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
</style>
