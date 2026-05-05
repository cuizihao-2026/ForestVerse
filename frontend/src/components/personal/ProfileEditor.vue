<template>
  <div class="profile-editor">
    <h2>个人资料</h2>
    <form @submit.prevent="handleSubmit" class="profile-form">
      <div class="form-group">
        <label>用户名</label>
        <input 
          type="text" 
          v-model="form.username" 
          disabled 
          class="form-input disabled"
        >
      </div>

      <div class="form-group">
        <label>昵称</label>
        <input 
          type="text" 
          v-model="form.nickname" 
          class="form-input"
          placeholder="请输入昵称"
        >
      </div>

      <div class="form-group">
        <label>邮箱</label>
        <input 
          type="email" 
          v-model="form.email" 
          class="form-input disabled"
          disabled
          placeholder="请输入邮箱"
        >
      </div>

      <div class="form-group">
        <label>个人简介</label>
        <textarea 
          v-model="form.bio" 
          class="form-input textarea"
          placeholder="介绍一下自己吧..."
          rows="4"
          maxlength="200"
        ></textarea>
        <div class="char-count">
          {{ form.bio.length }}/200
        </div>
      </div>

      <div class="form-group">
        <label>头像</label>
        <div class="avatar-upload">
          <div class="current-avatar">
            <img :src="form.avatar || 'https://neeko-copilot.bytedance.net/api/text2image?prompt=user%20avatar%20placeholder&size=128x128'" alt="当前头像">
          </div>
          <input 
            type="file" 
            @change="handleAvatarUpload" 
            class="avatar-input"
            accept="image/*"
            ref="avatarInput"
          >
          <button type="button" class="upload-btn" @click="triggerFileInput">
            更换头像
          </button>
        </div>
      </div>

      <!-- 消息提示 -->
      <div 
        v-if="showMessage" 
        class="message" 
        :class="messageType"
      >
        <span class="message-icon">{{ messageType === 'success' ? '✅' : '❌' }}</span>
        <span class="message-text">{{ messageText }}</span>
        <button class="message-close" @click="showMessage = false">×</button>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="isLoading">
          {{ isLoading ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { currentUser as currentUserStore, updateCurrentUser, fetchCurrentUser } from '../../stores/auth';
import { put, post, API_BASE_URL } from '../../utils/api';

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
  // 先尝试从 store 获取
  if (user.value) {
    initForm();
  }
  
  // 然后从后端获取最新的用户信息
  await fetchCurrentUser();
});

// 监听用户信息变化
watch(user, () => {
  initForm();
}, { deep: true });

const handleSubmit = async () => {
  isLoading.value = true;
  try {
    if (!user.value) {
      throw new Error('用户信息不存在');
    }
    
    // 准备更新数据
    const updateData = {
      id: user.value.id,
      nickname: form.value.nickname,
      bio: form.value.bio,
      avatar: form.value.avatar
    };
    
    // 调用后端API
    const updatedUser = await put('/api/user/profile', updateData);
    console.log('更新后的用户信息:', updatedUser);
    
    // 更新本地状态
    updateCurrentUser(updatedUser);
    
    // 显示成功消息
    messageText.value = '个人资料更新成功！';
    messageType.value = 'success';
    showMessage.value = true;
    
    // 3秒后自动关闭
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } catch (error: any) {
    console.error('更新失败:', error);
    messageText.value = error.message || '更新失败，请稍后重试';
    messageType.value = 'error';
    showMessage.value = true;
    
    // 3秒后自动关闭
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
      messageText.value = '不支持的图片格式，仅支持：jpg、jpeg、png、gif、webp、ico';
      messageType.value = 'error';
      showMessage.value = true;
      setTimeout(() => { showMessage.value = false; }, 3000);
      return;
    }

    if (file.size > maxSize) {
      messageText.value = '图片大小不能超过5MB';
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
      messageText.value = error.message || '头像上传失败，请稍后重试';
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
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  width: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.profile-editor h2 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 40px;
  color: #333;
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-group label {
  font-size: 16px;
  font-weight: 500;
  color: #555;
}

.form-input {
  padding: 16px 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f9f9f9;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: white;
}

.form-input.disabled {
  background: #f5f5f5;
  cursor: not-allowed;
  color: #999;
}

.form-input.textarea {
  resize: vertical;
  min-height: 120px;
  font-size: 16px;
  line-height: 1.6;
}

.char-count {
  font-size: 14px;
  color: #999;
  text-align: right;
  margin-top: 5px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 30px;
  flex-wrap: wrap;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px dashed #ddd;
}

.current-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #667eea;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.current-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-input {
  display: none;
}

.upload-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.form-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.btn {
  padding: 14px 32px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 消息提示样式 */
.message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
}

.message.success {
  background: linear-gradient(135deg, #66bb6a 0%, #43a047 100%);
  color: white;
}

.message.error {
  background: linear-gradient(135deg, #ef5350 0%, #e53935 100%);
  color: white;
}

.message-content {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
}

.message-close {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-editor {
    padding: 20px;
  }
  
  .profile-editor h2 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .form-group {
    gap: 8px;
  }
  
  .form-input {
    padding: 12px 16px;
  }
  
  .avatar-upload {
    gap: 20px;
    padding: 15px;
  }
  
  .current-avatar {
    width: 80px;
    height: 80px;
  }
  
  .upload-btn {
    padding: 10px 20px;
    font-size: 14px;
  }
  
  .form-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  
  .btn {
    padding: 12px 24px;
  }
}
</style>