<template>
  <div class="profile-editor">
    <div class="profile-content">
      <aside class="user-sidebar">
        <div class="avatar-wrapper">
          <img 
            v-if="form.avatar"
            :src="form.avatar" 
            :alt="form.nickname || '用户头像'"
            class="avatar-image avatar"
            width="100"
            height="100"
          />
          <div 
            v-else
            class="avatar-image avatar fallback huge"
          >
            {{ avatarInitial }}
          </div>
          <button type="button" class="avatar-btn" @click="triggerFileInput" aria-label="更换头像">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
              <circle cx="12" cy="13" r="4"></circle>
            </svg>
          </button>
        </div>
        <div class="user-info">
          <input 
            type="text" 
            v-model="form.nickname" 
            class="user-name-input"
            placeholder="未设置昵称"
            autocomplete="nickname"
          />
        </div>
        <div class="user-details">
          <div class="detail-item">
            <span class="detail-label">用户名</span>
            <span class="detail-value">{{ form.username }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">邮箱</span>
            <span class="detail-value">{{ form.email }}</span>
          </div>
        </div>
        <input 
          type="file" 
          @change="handleAvatarUpload" 
          class="avatar-input"
          accept="image/*"
          ref="avatarInput"
        />
      </aside>

      <main class="form-area">
        <div class="form-group flex-1 bio-group">
          <textarea 
            v-model="form.bio" 
            class="input bio-input"
            placeholder="向大家介绍一下自己..."
            maxlength="200"
          ></textarea>
          <span class="bio-counter" :class="{ 'near-limit': form.bio.length > 180 }">{{ form.bio.length }}/200</span>
        </div>

        <div class="actions">
          <button type="button" class="btn" :disabled="isLoading" @click="handleSubmit">
            {{ isLoading ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </main>
    </div>

    <Transition name="slide">
      <div 
        v-if="showMessage" 
        class="message"
        :class="messageType"
        role="alert"
      >
        {{ messageText }}
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { currentUser as currentUserStore, updateCurrentUser, fetchCurrentUser } from '../../stores/auth';
import { put, post, API_BASE_URL } from '../../utils/api';

const avatarInitial = computed(() => {
  const name = form.value.nickname || form.value.username || '?';
  return name.charAt(0).toUpperCase();
});

const user = computed(() => currentUserStore.value);
const isLoading = ref(false);
const avatarInput = ref<HTMLInputElement | null>(null);
const showMessage = ref(false);
const messageText = ref('');
const messageType = ref<'success' | 'error'>('success');

const form = ref({
  username: '',
  nickname: '',
  email: '',
  bio: '',
  avatar: ''
});

const initForm = () => {
  if (user.value) {
    form.value = {
      username: user.value.username || '',
      nickname: user.value.nickname || '',
      email: user.value.email || '',
      bio: user.value.bio || '',
      avatar: user.value.avatar || ''
    };
  }
};

onMounted(async () => {
  if (user.value) {
    initForm();
  }
  await fetchCurrentUser();
});

watch(user, () => {
  initForm();
}, { deep: true });

const handleSubmit = async () => {
  isLoading.value = true;
  try {
    if (!user.value) {
      throw new Error('用户信息不存在');
    }
    
    const updateData = {
      id: user.value.id,
      nickname: form.value.nickname,
      bio: form.value.bio,
      avatar: form.value.avatar
    };
    
    const updatedUser = await put('/api/user/profile', updateData);
    updateCurrentUser(updatedUser);
    
    messageText.value = '个人资料更新成功！';
    messageType.value = 'success';
    showMessage.value = true;
    
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } catch (error: any) {
    console.error('更新失败:', error);
    messageText.value = error.message || '更新失败，请稍后重试';
    messageType.value = 'error';
    showMessage.value = true;
    
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } finally {
    isLoading.value = false;
  }
};

const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];

    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.ico'];
    const maxSize = 10 * 1024 * 1024;
    const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

    if (!allowedExtensions.includes(extension)) {
      messageText.value = '不支持的图片格式';
      messageType.value = 'error';
      showMessage.value = true;
      setTimeout(() => { showMessage.value = false; }, 3000);
      return;
    }

    if (file.size > maxSize) {
      messageText.value = '图片大小不能超过10MB';
      messageType.value = 'error';
      showMessage.value = true;
      setTimeout(() => { showMessage.value = false; }, 3000);
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      const data = await post('/api/file/upload/avatar', formData);
      form.value.avatar = API_BASE_URL + data;

      messageText.value = '头像上传成功！';
      messageType.value = 'success';
      showMessage.value = true;
      setTimeout(() => { showMessage.value = false; }, 3000);
    } catch (error: any) {
      console.error('上传错误:', error);
      messageText.value = error.message || '头像上传失败';
      messageType.value = 'error';
      showMessage.value = true;
      setTimeout(() => { showMessage.value = false; }, 3000);
    }
  }
};

const triggerFileInput = () => {
  avatarInput.value?.click();
};
</script>

<style scoped>
.profile-editor {
  padding: 24px;
}

.profile-content {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 32px;
}

.user-sidebar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
}

.avatar-wrapper {
  position: relative;
}

.avatar-image {
  width: 100px;
  height: 100px;
  /* 使用通用的.avatar样式，这里只需要确保尺寸正确 */
}

.avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #3b82f6;
  border: 2px solid #fff;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-btn:hover {
  background: #2563eb;
}

.icon {
  width: 14px;
  height: 14px;
}

.user-info {
  text-align: center;
  width: 100%;
}

.user-name-input {
  width: 100%;
  padding: 8px 12px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  text-align: center;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  outline: none;
  transition: all 0.2s;
}

.user-name-input:focus {
  border-color: #3b82f6;
  background: #fff;
}

.user-name-input::placeholder {
  color: #9ca3af;
  font-weight: normal;
}

.user-details {
  width: 100%;
  padding: 16px 0;
  border-top: 1px solid #f3f4f6;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
}

.detail-item:not(:last-child) {
  border-bottom: 1px solid #f3f4f6;
}

.detail-label {
  font-size: 11px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-value {
  font-size: 13px;
  color: #374151;
}

.avatar-input {
  display: none;
}

.form-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.flex-1 {
  flex: 1;
}

.label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.input {
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  color: #111827;
  background: #fff;
}

.input:focus {
  outline: none;
  border-color: #3b82f6;
}

.input.textarea {
  resize: vertical;
  flex: 1;
  min-height: 120px;
}

.bio-group {
  position: relative;
  flex: 1;
}

.bio-input {
  width: 100%;
  height: 100%;
  padding: 16px;
  padding-bottom: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  background: #fafbfc;
  resize: none;
  transition: all 0.2s;
}

.bio-input:focus {
  outline: none;
  border-color: #d1d5db;
  background: #fff;
}

.bio-input::placeholder {
  color: #9ca3af;
}

.bio-counter {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-size: 11px;
  color: #d1d5db;
  font-weight: 400;
  pointer-events: none;
}

.bio-counter.near-limit {
  color: #f59e0b;
}

.actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.btn {
  padding: 10px 24px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn:hover:not(:disabled) {
  background: #2563eb;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 10px 18px;
  border-radius: 6px;
  font-size: 13px;
  color: white;
  z-index: 1000;
}

.message.success {
  background: #10b981;
}

.message.error {
  background: #ef4444;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 768px) {
  .profile-content {
    grid-template-columns: 1fr;
  }

  .user-sidebar {
    flex-direction: column;
    gap: 16px;
  }

  .user-details {
    display: block;
  }

  .form-area {
    flex: 1;
  }

  .actions {
    justify-content: stretch;
  }

  .btn {
    width: 100%;
  }
}
</style>
