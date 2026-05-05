<template>
  <div class="user-stats">
    <div class="overview-grid">
      <div class="overview-item clickable" @click="handleViewAll">
        <div class="overview-value">{{ userCount }}</div>
        <div class="overview-label">用户总数</div>
        <span class="view-hint">查看 →</span>
      </div>
      <div class="overview-item clickable" @click="handleViewOnline">
        <div class="overview-value">{{ onlineUserCount }}</div>
        <div class="overview-label">在线用户</div>
        <span class="view-hint">查看 →</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  userCount: number;
  onlineUserCount: number;
}>();

const emit = defineEmits<{
  (e: 'viewOnline'): void;
  (e: 'viewAll'): void;
}>();

const handleViewOnline = () => {
  emit('viewOnline');
};

const handleViewAll = () => {
  emit('viewAll');
};
</script>

<style scoped>
.user-stats {
  margin-bottom: 40px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.overview-item {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
}

.overview-item.clickable {
  cursor: pointer;
}

.overview-item.clickable:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.overview-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.overview-label {
  font-size: 13px;
  color: #9ca3af;
}

.view-hint {
  display: inline-block;
  margin-top: 8px;
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
  
  .overview-value {
    font-size: 24px;
  }
}
</style>
