<template>
  <div class="account-settings">
    <h2>账户设置</h2>
    <form @submit.prevent="handleSubmit" class="password-form">
      <div class="form-group">
        <label>当前密码</label>
        <div class="password-input">
          <input 
            type="password" 
            v-model="form.oldPassword" 
            class="form-input"
            placeholder="请输入当前密码"
          >
        </div>
      </div>

      <div class="form-group">
        <label>新密码</label>
        <div class="password-input">
          <input 
            type="password" 
            v-model="form.newPassword" 
            class="form-input"
            placeholder="请输入新密码"
          >
        </div>
      </div>

      <div class="form-group">
        <label>确认新密码</label>
        <div class="password-input">
          <input 
            type="password" 
            v-model="form.confirmPassword" 
            class="form-input"
            placeholder="请再次输入新密码"
          >
        </div>
      </div>

      <div class="password-strength" v-if="form.newPassword">
        <div class="strength-bar">
          <div 
            class="strength-indicator" 
            :class="{ 
              weak: passwordStrength === 'weak',
              medium: passwordStrength === 'medium',
              strong: passwordStrength === 'strong'
            }"
            :style="{ width: strengthWidth }"
          ></div>
        </div>
        <div class="strength-text">{{ getStrengthText() }}</div>
      </div>

      <!-- 消息提示 -->
      <div 
        v-if="showMessage" 
        class="message" 
        :class="messageType"
      >
        <span class="message-text">{{ messageText }}</span>
        <button class="message-close" @click="showMessage = false">×</button>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="isLoading">
          {{ isLoading ? '修改中...' : '修改密码' }}
        </button>
      </div>
    </form>

    <!-- 注销账户区域 -->
    <div class="danger-zone">
      <h3>危险操作</h3>
      <p>注销账户后，您的所有数据将被永久删除，无法恢复。</p>
      <button class="btn btn-danger" @click="showDeleteConfirm = true" :disabled="isDeleting">
        {{ isDeleting ? '注销中...' : '注销账户' }}
      </button>
    </div>

    <!-- 确认删除对话框 -->
    <div v-if="showDeleteConfirm" class="modal-overlay">
      <div class="modal">
        <h3>确认注销账户</h3>
        <p>您确定要注销账户吗？此操作无法撤销，您的所有数据将被永久删除。</p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showDeleteConfirm = false">取消</button>
          <button class="btn btn-danger" @click="handleDeleteAccount" :disabled="isDeleting">
            {{ isDeleting ? '注销中...' : '确认注销' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { currentUser as currentUserStore, logout } from '../../stores/auth';
import { showModal } from '../../stores/modal';
import { post, del } from '../../utils/api';

const router = useRouter();
const user = computed(() => currentUserStore.value);
const isLoading = ref(false);
const isDeleting = ref(false);
const showMessage = ref(false);
const showDeleteConfirm = ref(false);
const messageText = ref('');
const messageType = ref<'success' | 'error'>('success');

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const passwordStrength = computed(() => {
  const password = form.value.newPassword;
  if (!password) return 'weak';
  
  let strength = 0;
  if (password.length >= 8) strength++;
  if (/[A-Z]/.test(password)) strength++;
  if (/[0-9]/.test(password)) strength++;
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength++;
  
  if (strength <= 1) return 'weak';
  if (strength <= 2) return 'medium';
  return 'strong';
});

const strengthWidth = computed(() => {
  const strength = passwordStrength.value;
  if (strength === 'weak') return '25%';
  if (strength === 'medium') return '60%';
  return '100%';
});

const getStrengthText = () => {
  const strength = passwordStrength.value;
  if (strength === 'weak') return '密码强度：弱';
  if (strength === 'medium') return '密码强度：中等';
  return '密码强度：强';
};

const handleSubmit = async () => {
  // 表单验证
  if (!form.value.oldPassword) {
    showMessage.value = true;
    messageText.value = '请输入当前密码';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    return;
  }
  
  if (!form.value.newPassword) {
    showMessage.value = true;
    messageText.value = '请输入新密码';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    return;
  }
  
  if (form.value.newPassword !== form.value.confirmPassword) {
    showMessage.value = true;
    messageText.value = '两次输入的新密码不一致';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    return;
  }
  
  if (form.value.newPassword.length < 8) {
    showMessage.value = true;
    messageText.value = '新密码长度至少为8位';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    return;
  }
  
  if (form.value.newPassword === form.value.oldPassword) {
    showMessage.value = true;
    messageText.value = '新密码不能与旧密码相同';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    return;
  }
  
  if (!user.value) {
    showMessage.value = true;
    messageText.value = '用户信息不存在';
    messageType.value = 'error';
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    return;
  }
  
  isLoading.value = true;
  try {
    // 调用后端API
    const successMessage = await post('/api/user/change-password', {
      userId: user.value.id,
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword
    });
    
    // 显示成功消息并自动登出
    showModal({
      title: '提示',
      message: successMessage,
      type: 'success',
      onConfirm: () => {
        logout();
        router.push('/login');
      }
    });
  } catch (error: any) {
    console.error('修改失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '修改失败，请稍后重试';
    messageType.value = 'error';
    
    // 3秒后自动关闭
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } finally {
    isLoading.value = false;
  }
};

const handleDeleteAccount = async () => {
  if (!user.value) return;
  
  isDeleting.value = true;
  try {
    await del(`/api/user/${user.value.id}`);
    
    // 注销成功，显示成功提示然后清除用户信息并跳转
    showDeleteConfirm.value = false;
    showModal({
      title: '注销成功',
      message: '您的账号已成功注销，感谢您的使用！',
      type: 'success',
      onConfirm: () => {
        logout();
        router.push('/login');
      }
    });
  } catch (error: any) {
    console.error('注销失败:', error);
    showMessage.value = true;
    messageText.value = error.message || '注销失败，请稍后重试';
    messageType.value = 'error';
    
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
  } finally {
    isDeleting.value = false;
  }
};
</script>

<style scoped>
.account-settings {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.account-settings h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 30px;
  color: #333;
  text-align: center;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
}

.password-input {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.password-strength {
  margin-top: 10px;
}

.strength-bar {
  width: 100%;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 5px;
}

.strength-indicator {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 3px;
}

.strength-indicator.weak {
  background: #ff6b6b;
}

.strength-indicator.medium {
  background: #ffa502;
}

.strength-indicator.strong {
  background: #4ecdc4;
}

.strength-text {
  font-size: 12px;
  color: #666;
  text-align: right;
}

.form-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 20px;
}

.btn {
  padding: 12px 24px;
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
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.btn-secondary:hover {
  background: #e0e0e0;
  transform: translateY(-1px);
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.btn-danger:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
}

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.danger-zone {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #e0e0e0;
}

.danger-zone h3 {
  font-size: 18px;
  font-weight: 600;
  color: #ef4444;
  margin-bottom: 10px;
}

.danger-zone p {
  color: #666;
  margin-bottom: 20px;
}

/* 模态对话框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 30px;
  border-radius: 12px;
  max-width: 400px;
  width: 90%;
  text-align: center;
}

.modal h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.modal p {
  color: #666;
  margin-bottom: 25px;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.modal-actions .btn {
  min-width: 100px;
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
  .account-settings {
    padding: 20px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
  }
}
</style>
