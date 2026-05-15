<template>
  <div class="human-verification-overlay" v-if="visible">
    <div class="verification-modal">
      <h3 class="modal-title">人机验证</h3>
      <div class="modal-content">
        <p>请完成以下验证，以确认您不是机器人</p>
        <div class="captcha-container">
          <button type="button" class="captcha-image" @click="refreshCaptcha" aria-label="点击刷新验证码">
            <img v-if="captchaImage" :src="captchaImage" alt="验证码图片" />
            <span v-else>加载中...</span>
          </button>
          <input
            type="text"
            v-model="userInput"
            placeholder="请输入图形验证码"
            class="captcha-input"
            @keyup.enter="verify"
            autocomplete="off"
          />
        </div>
        <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>
      </div>
      <div class="modal-actions">
        <button class="cancel-button" @click="cancel">取消</button>
        <button class="confirm-button" @click="verify" :disabled="isVerifying">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close', success: boolean): void
}>()

const userInput = ref('')
const captchaImage = ref('')
const token = ref('')
const errorMessage = ref('')
const isVerifying = ref(false)

const fetchCaptcha = async () => {
  try {
    const response = await fetch('/api/auth/captcha')
    const data = await response.json()

    if (data.success) {
      captchaImage.value = data.data.image
      token.value = data.data.token
      errorMessage.value = ''
    } else {
      errorMessage.value = '获取验证码失败'
    }
  } catch (error) {
    errorMessage.value = '网络错误，请重试'
  }
}

const refreshCaptcha = () => {
  userInput.value = ''
  fetchCaptcha()
}

const verify = async () => {
  if (!userInput.value) {
    errorMessage.value = '请输入验证码'
    return
  }

  if (!token.value) {
    errorMessage.value = '验证码已过期，请刷新'
    return
  }

  isVerifying.value = true
  errorMessage.value = ''

  try {
    const response = await fetch('/api/auth/verify-captcha', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: token.value,
        code: userInput.value
      })
    })

    const data = await response.json()

    if (data.success) {
      emit('close', true)
    } else {
      errorMessage.value = data.message || '验证码错误'
      refreshCaptcha()
    }
  } catch (error) {
    errorMessage.value = '网络错误，请重试'
  } finally {
    isVerifying.value = false
  }
}

const cancel = () => {
  emit('close', false)
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    userInput.value = ''
    errorMessage.value = ''
    fetchCaptcha()
  }
})
</script>

<style scoped>
.human-verification-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.verification-modal {
  background: #D2B48C;
  border-radius: 8px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3);
  padding: 30px;
  width: 100%;
  max-width: 400px;
  border: 2px solid #654321;
  background-image: repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.1) 0px, rgba(255, 255, 255, 0.1) 2px, rgba(255, 255, 255, 0) 2px, rgba(255, 255, 255, 0) 10px);
}

.modal-title {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
  font-size: 20px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
  font-family: var(--sans);
}

.modal-content {
  margin-bottom: 20px;
}

.modal-content p {
  color: #333;
  margin-bottom: 20px;
  font-family: var(--sans);
}

.captcha-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.captcha-image {
  width: 100%;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 5px;
}

.captcha-image:hover {
  transform: scale(1.02);
}

.captcha-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  transform: scale(1.5);
}

.captcha-input {
  padding: 12px;
  border: 2px solid #654321;
  border-radius: 4px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.captcha-input:focus {
  outline: none;
  border-color: #8B4513;
  box-shadow: 0 0 0 3px rgba(139, 69, 19, 0.2);
}

.error-text {
  color: #d32f2f;
  font-size: 14px;
  margin-top: 10px;
  text-align: center;
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.cancel-button, .confirm-button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: var(--sans);
}

.cancel-button {
  background: #ccc;
  color: #333;
}

.cancel-button:hover {
  background: #bbb;
}

.confirm-button {
  background: linear-gradient(135deg, #8B4513 0%, #654321 100%);
  color: white;
}

.confirm-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #654321 0%, #8B4513 100%);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.confirm-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
