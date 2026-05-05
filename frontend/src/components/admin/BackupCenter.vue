<template>
  <div class="backup-center">
    
    <!-- 快速操作 -->
    <div class="quick-actions">
      <div class="action-card card card-hover">
        <div class="action-icon">💾</div>
        <h3>创建备份</h3>
        <p>立即创建完整备份（数据库 + 上传文件）</p>
        <button class="btn btn-primary" @click="createBackup" :disabled="isCreating">
          <span v-if="isCreating" class="loading-spinner"></span>
          {{ isCreating ? '备份中...' : '立即备份' }}
        </button>
      </div>
      <div class="action-card card card-hover">
        <div class="action-icon">📥</div>
        <h3>上传备份</h3>
        <p>上传备份文件到备份列表</p>
        <input type="file" ref="restoreFileInput" @change="handleRestoreFile" accept=".zip" style="display: none;">
        <div v-if="isImporting" class="upload-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <div class="progress-text">{{ uploadProgress }}%</div>
        </div>
        <button v-else class="btn btn-secondary" @click="triggerRestore">
          选择文件
        </button>
      </div>
    </div>

    <!-- 备份列表 -->
    <div class="backup-list-section">
      <!-- 自动备份设置 -->
      <div class="auto-backup-section card">
        <div class="auto-backup-form">
          <div class="auto-backup-row">
            <div class="auto-backup-left">
              <div class="auto-backup-switches">
                <div class="switch-item">
                  <label class="form-label">
                    <input type="checkbox" v-model="autoBackupEnabled" class="form-checkbox">
                    启用自动备份
                  </label>
                </div>
                <div class="switch-item">
                  <label class="form-label">
                    <input type="checkbox" v-model="enableRetention" class="form-checkbox" :disabled="!autoBackupEnabled">
                    保留最新
                    <input type="number" v-model="retentionCount" min="3" max="300" class="form-input retention-input" :disabled="!autoBackupEnabled || !enableRetention">
                    份备份记录
                  </label>
                </div>
              </div>
            </div>
            <div class="auto-backup-center">
              <div class="backup-time-group">
                <label class="form-label">每天备份时间</label>
                <input type="time" v-model="backupTime" class="form-input" :disabled="!autoBackupEnabled">
              </div>
            </div>
            <div class="auto-backup-right">
              <button class="btn btn-primary" @click="saveAutoBackupSettings" :disabled="!autoBackupEnabled">保存设置</button>
            </div>
          </div>
        </div>
      </div>

      <div class="section-header">
        <h3>备份历史</h3>
        <span class="item-count">共 {{ backups.length }} 个备份</span>
      </div>
      
      <div class="backup-list">
        <div v-for="backup in backups" :key="backup.name" class="backup-item card card-hover" :class="{ 'backup-item-loading': restoringBackup === backup.name }">
          <div class="backup-info">
            <div class="backup-icon">
              <span v-if="restoringBackup !== backup.name">💾</span>
              <span v-else class="loading-spinner"></span>
            </div>
            <div class="backup-details">
              <div class="backup-name">{{ backup.name }}</div>
              <div class="backup-meta">
                <span v-if="restoringBackup !== backup.name">📅 {{ backup.createdAt }}</span>
                <span v-else class="restoring-text">⏳ 正在恢复中...</span>
                <span>📊 {{ backup.size }}</span>
              </div>
            </div>
          </div>
          <div class="backup-actions">
            <button class="btn btn-sm btn-secondary" @click="downloadBackup(backup)" :disabled="restoringBackup === backup.name">下载</button>
            <button class="btn btn-sm btn-secondary" @click="restoreBackup(backup)" :disabled="restoringBackup === backup.name">
              <span v-if="restoringBackup !== backup.name">恢复</span>
              <span v-else class="loading-spinner"></span>
            </button>
            <button class="btn btn-sm btn-danger" @click="deleteBackup(backup)" :disabled="restoringBackup === backup.name">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get, post, del } from '../../utils/api';
import { getToken } from '../../stores/auth';
import { showModal, showConfirm } from '../../stores/modal';

const restoreFileInput = ref<HTMLInputElement | null>(null);
const autoBackupEnabled = ref(false);
const backupTime = ref('02:00');
const enableRetention = ref(false);
const retentionCount = ref(7);
const isCreating = ref(false);
const isImporting = ref(false);
const uploadProgress = ref(0);
const restoringBackup = ref<string | null>(null); // 记录正在恢复的备份名称

interface Backup {
  name: string;
  size: string;
  sizeBytes: number;
  createdAt: string;
  lastModified: number;
}

const backups = ref<Backup[]>([]);

const loadBackups = async () => {
  try {
    console.log('开始加载备份列表...');
    const response = await get('/api/backup/list');
    console.log('后端返回的数据:', response);
    backups.value = response.data || [];
    console.log('backups.value:', backups.value);
  } catch (e: any) {
    console.error('加载备份列表失败:', e);
  }
};

const createBackup = async () => {
  if (isCreating.value) return;
  isCreating.value = true;
  try {
    const response = await post('/api/backup/create');
    console.log('创建备份返回:', response);
    const data = response.data;
    if (data && data.status === 'success') {
      await loadBackups();
      showModal({
        title: '成功',
        message: '备份创建成功！',
        type: 'success'
      });
    } else {
      showModal({
        title: '错误',
        message: '备份创建失败: ' + (data?.message || '未知错误'),
        type: 'error'
      });
    }
  } catch (e: any) {
    showModal({
      title: '错误',
      message: '备份创建失败: ' + (e.message || '未知错误'),
      type: 'error'
    });
    console.error('创建备份失败:', e);
  } finally {
    isCreating.value = false;
  }
};

const triggerRestore = () => {
  restoreFileInput.value?.click();
};

const handleRestoreFile = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (!target.files || target.files.length === 0) return;

  if (isImporting.value) return;
  isImporting.value = true;
  uploadProgress.value = 0;

  try {
    const file = target.files[0];
    const formData = new FormData();
    formData.append('file', file);

    const token = getToken();

    // 使用XMLHttpRequest来监听上传进度
    const result = await new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          uploadProgress.value = Math.round((e.loaded / e.total) * 100);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            resolve(JSON.parse(xhr.responseText));
          } catch {
            resolve(xhr.responseText);
          }
        } else {
          try {
            const errData = JSON.parse(xhr.responseText);
            reject(new Error(errData.message || '上传失败'));
          } catch {
            reject(new Error('上传失败'));
          }
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('网络错误'));
      });

      xhr.open('POST', '/api/backup/upload');
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }
      xhr.send(formData);
    });

    console.log('上传备份返回:', result);
    await loadBackups();
    showModal({
      title: '成功',
      message: '备份文件上传成功！',
      type: 'success'
    });
  } catch (e: any) {
    showModal({
      title: '错误',
      message: '备份上传失败: ' + (e.message || '未知错误'),
      type: 'error'
    });
    console.error('上传备份失败:', e);
  } finally {
    isImporting.value = false;
    uploadProgress.value = 0;
    if (target) target.value = '';
  }
};

const downloadBackup = async (backup: Backup) => {
  try {
    const token = getToken();
    const response = await fetch(`/api/backup/download/${encodeURIComponent(backup.name)}`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });

    if (!response.ok) {
      throw new Error('下载失败');
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = backup.name;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  } catch (e: any) {
    alert('下载失败: ' + (e.message || '未知错误'));
  }
};

const restoreBackup = async (backup: Backup) => {
  const confirmed = await showConfirm({
    title: '确认恢复',
    message: `确定要恢复备份「${backup.name}」吗？当前数据库将被覆盖！`,
    type: 'warning'
  });
  
  if (!confirmed) return;
  
  restoringBackup.value = backup.name;
  
  try {
    const response = await post(`/api/backup/restore/${encodeURIComponent(backup.name)}`);
    console.log('恢复备份返回:', response);
    const data = response.data;
    if (data && data.status === 'success') {
      await loadBackups();
      showModal({
        title: '成功',
        message: '备份恢复成功！',
        type: 'success'
      });
    } else {
      showModal({
        title: '错误',
        message: '恢复失败: ' + (data?.message || '未知错误'),
        type: 'error'
      });
    }
  } catch (e: any) {
    showModal({
      title: '错误',
      message: '恢复失败: ' + (e.message || '未知错误'),
      type: 'error'
    });
  } finally {
    restoringBackup.value = null;
  }
};

const deleteBackup = async (backup: Backup) => {
  const confirmed = await showConfirm({
    title: '确认删除',
    message: `确定要删除备份「${backup.name}」吗？`,
    type: 'warning'
  });
  
  if (!confirmed) return;
  
  try {
    await del(`/api/backup/${encodeURIComponent(backup.name)}`);
    backups.value = backups.value.filter(b => b.name !== backup.name);
    showModal({
      title: '成功',
      message: '删除成功！',
      type: 'success'
    });
  } catch (e: any) {
    showModal({
      title: '错误',
      message: '删除失败: ' + (e.message || '未知错误'),
      type: 'error'
    });
  }
};

const loadAutoBackupSettings = async () => {
  try {
    const response = await get('/api/backup/settings');
    console.log('自动备份设置:', response);
    const data = response.data;
    if (data) {
      autoBackupEnabled.value = data.enabled || false;
      backupTime.value = data.backupTime || '02:00';
      enableRetention.value = data.enableRetention || false;
      retentionCount.value = data.retentionCount || 7;
    }
  } catch (e: any) {
    console.error('加载自动备份设置失败:', e);
  }
};

const saveAutoBackupSettings = async () => {
  try {
    await post('/api/backup/settings', {
      enabled: autoBackupEnabled.value,
      backupTime: backupTime.value,
      enableRetention: enableRetention.value,
      retentionCount: retentionCount.value
    });
    showModal({
      title: '成功',
      message: '设置已保存！',
      type: 'success'
    });
  } catch (e: any) {
    showModal({
      title: '错误',
      message: '保存设置失败: ' + (e.message || '未知错误'),
      type: 'error'
    });
  }
};

onMounted(() => {
  loadBackups();
  loadAutoBackupSettings();
});
</script>

<style scoped>
.backup-center {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-3xl);
  box-shadow: var(--shadow);
  width: 100%;
  font-family: var(--sans);
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-3xl);
}

.action-card {
  text-align: center;
  padding: var(--spacing-xl);
}

.action-icon {
  font-size: 40px;
  margin-bottom: var(--spacing-sm);
}

.action-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-h);
  margin-bottom: var(--spacing-xs);
}

.action-card p {
  color: var(--text-m);
  margin-bottom: var(--spacing);
  font-size: 14px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.section-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-h);
}

.item-count {
  font-size: 14px;
  color: var(--text-m);
}

.backup-list-section {
  margin-bottom: var(--spacing-3xl);
}

.backup-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing);
  margin-bottom: var(--spacing);
}

.backup-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-xl);
}

.backup-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.backup-icon {
  font-size: 32px;
}

.backup-name {
  font-weight: 600;
  color: var(--text-h);
  margin-bottom: var(--spacing-xs);
}

.backup-meta {
  display: flex;
  gap: var(--spacing-lg);
  font-size: 14px;
  color: var(--text-m);
}

.backup-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.backup-item-loading {
  opacity: 0.8;
  pointer-events: none;
}

.restoring-text {
  color: var(--primary-color);
  font-weight: 500;
}

.upload-progress {
  margin-bottom: var(--spacing);
}

.progress-bar {
  width: 100%;
  height: 12px;
  background-color: #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: var(--spacing-xs);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  transition: width 0.3s ease;
  border-radius: 6px;
}

.progress-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-h);
  text-align: center;
}

.auto-backup-section {
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing);
}

.auto-backup-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-xl);
}

.auto-backup-left,
.auto-backup-right {
  flex: 1;
  display: flex;
  align-items: center;
}

.auto-backup-right {
  justify-content: flex-end;
}

.auto-backup-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

.auto-backup-switches {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.switch-item {
  display: flex;
  align-items: center;
  white-space: nowrap;
  min-height: 40px;
}

.switch-item .form-label {
  display: flex;
  align-items: center;
  margin-bottom: 0;
}

.switch-item .form-checkbox {
  margin-right: var(--spacing-sm);
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.backup-time-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.backup-time-group .form-label {
  margin-bottom: 0;
}

.retention-input {
  width: 60px;
  padding: var(--spacing-xs) var(--spacing-sm);
  margin: 0 var(--spacing-sm);
  text-align: center;
  border-radius: var(--radius-sm);
}

.form-label {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
  color: var(--text-h);
  font-weight: 500;
}

.form-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.form-input {
  width: 100%;
  max-width: 300px;
}

@media (max-width: 768px) {
  .backup-center {
    padding: var(--spacing-xl);
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .backup-item {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-lg);
  }

  .backup-actions {
    width: 100%;
  }

  .backup-actions .btn {
    flex: 1;
  }
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
  vertical-align: middle;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
