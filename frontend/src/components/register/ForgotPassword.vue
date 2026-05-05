<template>
  <div class="forgot-password-container">
    <div class="rope"></div>
    <div class="wooden-sign">
      <h2 class="forgot-password-title">找回密码</h2>
      <div class="forgot-password-content">
        <form class="forgot-password-form" @submit.prevent="handleSubmit" v-if="!isLoading">
          <div class="form-group">
            <label for="username">用户名</label>
            <input type="text" id="username" v-model="form.username" placeholder="请输入用户名" required />
          </div>
          <div class="form-group">
            <label for="email">邮箱</label>
            <input type="email" id="email" v-model="form.email" placeholder="请输入邮箱" required />
          </div>
          <div class="form-group">
            <label for="newPassword">新密码</label>
            <div class="password-input">
              <input :type="showPassword ? 'text' : 'password'" id="newPassword" v-model="form.newPassword" placeholder="请输入新密码" required />
              <button type="button" class="eye-button" @click="togglePassword">
                <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"></path><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"></path><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"></path><line x1="2" x2="22" y1="2" y2="22"></line></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"></path><circle cx="12" cy="12" r="3"></circle></svg>
              </button>
            </div>
          </div>
          <div class="form-group">
            <label for="confirmPassword">确认新密码</label>
            <div class="password-input">
              <input :type="showPassword ? 'text' : 'password'" id="confirmPassword" v-model="form.confirmPassword" placeholder="请确认新密码" required />
              <button type="button" class="eye-button" @click="togglePassword">
                <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"></path><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"></path><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"></path><line x1="2" x2="22" y1="2" y2="22"></line></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"></path><circle cx="12" cy="12" r="3"></circle></svg>
              </button>
            </div>
          </div>
          <div v-if="emailEnabled" class="form-group">
            <label for="emailCaptcha">邮箱验证码</label>
            <div class="captcha-input">
              <input type="text" id="emailCaptcha" v-model="form.emailCaptcha" placeholder="请输入邮箱验证码" :required="emailEnabled" />
              <button type="button" class="send-captcha-button" @click="handleSendCaptcha" :disabled="isSendingEmailCaptcha">
                {{ isSendingEmailCaptcha ? '发送中...' : (countdown > 0 ? `${countdown}秒后重发` : '发送验证码') }}
              </button>
            </div>
          </div>
          <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
          <div v-if="successMessage" class="success-message">{{ successMessage }}</div>
          <button type="submit" class="submit-button" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '提交申请' }}
          </button>
          <div class="login-link">
            想起密码了？<router-link to="/login">立即登录</router-link>
          </div>
        </form>
        <div v-else class="loading-container">
          <div class="loading-spinner"></div>
          <div class="loading-text">加载中...</div>
        </div>
      </div>
    </div>
    <HumanVerification
      :visible="isHumanVerificationVisible"
      @close="handleHumanVerificationClose"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import HumanVerification from './HumanVerification.vue'

const router = useRouter()

const showPassword = ref(false)
const form = ref({
  username: '',
  email: '',
  newPassword: '',
  confirmPassword: '',
  emailCaptcha: ''
})

const isSubmitting = ref(false)
const isSendingEmailCaptcha = ref(false)
const countdown = ref(0)
let countdownTimer: number | undefined
const isHumanVerificationVisible = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const emailEnabled = ref(true)
const captchaForgotPasswordEnabled = ref(true)
const isLoading = ref(true)

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const fetchSecurityConfig = async () => {
  try {
    const response = await fetch('/api/settings/public/security-config')
    const data = await response.json()
    emailEnabled.value = data.emailEnabled
    captchaForgotPasswordEnabled.value = data.captchaForgotPasswordEnabled
  } catch (error) {
    console.error('获取安全配置失败', error)
    // 如果获取失败，默认启用所有功能
    emailEnabled.value = true
    captchaForgotPasswordEnabled.value = true
  } finally {
    isLoading.value = false
  }
}

const handleSendCaptcha = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  
  // 如果邮箱功能启用，需要先验证
  if (emailEnabled.value) {
    const verified = await verifyUserCredentials()
    if (!verified) {
      errorMessage.value = '用户名和邮箱不匹配'
      return
    }
  }

  // 如果启用了忘记密码验证码，显示人机验证
  if (captchaForgotPasswordEnabled.value) {
    isHumanVerificationVisible.value = true
  } else if (emailEnabled.value) {
    // 如果没启用验证码但启用了邮箱，直接发送
    sendResetCode()
  }
}

let submitCallback: (() => void) | null = null

const verifyUserCredentials = async (): Promise<boolean> => {
  const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/
  if (!usernameRegex.test(form.value.username)) {
    errorMessage.value = '用户名长度应在3-20位之间，只能包含字母、数字和下划线'
    return false
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(form.value.email)) {
    errorMessage.value = '请输入正确的邮箱地址'
    return false
  }

  try {
    const response = await fetch('/api/auth/verify-user', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: form.value.username,
        email: form.value.email
      })
    })

    const data = await response.json()
    return data.success
  } catch (error) {
    return false
  }
}

const sendResetCode = async () => {
  isSendingEmailCaptcha.value = true

  try {
    const response = await fetch('/api/auth/send-reset-code', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: form.value.username,
        email: form.value.email
      })
    })

    const data = await response.json()

    if (data.success) {
      successMessage.value = '验证码已发送到您的邮箱'
      countdown.value = 60
      countdownTimer = window.setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(countdownTimer)
        }
      }, 1000)
    } else {
      errorMessage.value = data.message || '发送失败'
    }
  } catch (error) {
    errorMessage.value = '网络错误，请稍后重试'
  } finally {
    isSendingEmailCaptcha.value = false
  }
}

const handleSubmit = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  if (form.value.newPassword !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  if (form.value.newPassword.length < 6) {
    errorMessage.value = '密码长度不能少于6位'
    return
  }

  // 邮箱启用但没有输入验证码，先提示需要发送验证码
  if (emailEnabled.value && !form.value.emailCaptcha) {
    errorMessage.value = '请先点击发送验证码获取邮箱验证码'
    return
  }

  // 如果启用了忘记密码验证码但邮箱禁用了，在提交前先验证
  if (captchaForgotPasswordEnabled.value && !emailEnabled.value) {
    const verified = await verifyUserCredentials()
    if (!verified) {
      errorMessage.value = '用户名和邮箱不匹配'
      return
    }
    // 保存提交回调
    submitCallback = () => {
      performSubmit()
    }
    isHumanVerificationVisible.value = true
    return
  }

  await performSubmit()
}

const performSubmit = async () => {
  isSubmitting.value = true

  try {
    const body: any = {
      username: form.value.username,
      email: form.value.email,
      newPassword: form.value.newPassword
    }
    
    if (emailEnabled.value) {
      body.emailCaptcha = form.value.emailCaptcha
    }

    const response = await fetch('/api/auth/reset-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    })

    const data = await response.json()

    if (data.success) {
      successMessage.value = '密码重置成功！即将跳转到登录页...'
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      errorMessage.value = data.message || '重置失败'
    }
  } catch (error) {
    errorMessage.value = '网络错误，请稍后重试'
  } finally {
    isSubmitting.value = false
  }
}

const handleHumanVerificationClose = (success: boolean) => {
  isHumanVerificationVisible.value = false

  if (success) {
    // 如果有提交回调，执行它
    if (submitCallback) {
      const callback = submitCallback
      submitCallback = null
      callback()
    } else if (emailEnabled.value) {
      sendResetCode()
    }
  } else {
    submitCallback = null
  }
}

onMounted(() => {
  fetchSecurityConfig()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: sway 8s ease-in-out infinite;
  transform-origin: top center;
}

.rope {
  width: 4px;
  height: 60px;
  background: linear-gradient(to bottom, #8B4513, #A0522D);
  border-radius: 2px;
  margin-bottom: -10px;
  z-index: 1;
}

.wooden-sign {
  background-color: #D2B48C;
  border-radius: 8px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3), inset 0 0 20px rgba(0, 0, 0, 0.2);
  padding: 40px;
  width: 100%;
  max-width: 500px;
  position: relative;
  border: 2px solid #654321;
  background-image: repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.1) 0px, rgba(255, 255, 255, 0.1) 2px, rgba(255, 255, 255, 0) 2px, rgba(255, 255, 255, 0) 10px);
  opacity: 1;
  z-index: 2;
  position: relative;
}

.wooden-sign::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 8px;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.1);
  pointer-events: none;
}

.forgot-password-title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
  font-family: 'Georgia', serif;
}

.forgot-password-content {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.forgot-password-form {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
  font-family: 'Georgia', serif;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 2px solid #654321;
  border-radius: 4px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #8B4513;
  box-shadow: 0 0 0 3px rgba(139, 69, 19, 0.2);
}

.password-input {
  position: relative;
}

.password-input input {
  padding-right: 40px;
}

input[type="password"] {
  -webkit-text-security: disc;
}

.password-input input::-ms-reveal,
.password-input input::-ms-clear {
  display: none;
}

.password-input input::-webkit-inner-spin-button,
.password-input input::-webkit-outer-spin-button,
.password-input input::-webkit-contacts-auto-fill-button {
  display: none;
}

.password-input input {
  -moz-appearance: textfield;
  appearance: textfield;
}

.eye-button {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #654321;
  padding: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eye-button:hover {
  color: #8B4513;
}

.captcha-input {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input input {
  flex: 1;
}

.send-captcha-button {
  padding: 0 15px;
  height: 40px;
  background: linear-gradient(135deg, #8B4513 0%, #654321 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Georgia', serif;
  white-space: nowrap;
}

.send-captcha-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #654321 0%, #8B4513 100%);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.send-captcha-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.submit-button {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #8B4513 0%, #654321 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Georgia', serif;
  margin-top: 10px;
}

.submit-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #654321 0%, #8B4513 100%);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.submit-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.error-message {
  color: #d32f2f;
  background-color: #ffebee;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
  text-align: center;
  font-size: 14px;
}

.success-message {
  color: #388e3c;
  background-color: #e8f5e9;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 10px;
  text-align: center;
  font-size: 14px;
}

.login-link {
  margin-top: 20px;
  text-align: center;
  color: #333;
  font-family: 'Georgia', serif;
}

.login-link a {
  color: #654321;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
  color: #8B4513;
}

@keyframes sway {
  0%, 100% {
    transform: rotate(-2deg);
  }
  50% {
    transform: rotate(2deg);
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #8B4513;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #333;
  font-family: 'Georgia', serif;
  font-size: 16px;
}

@media (max-width: 768px) {
  .wooden-sign {
    padding: 30px;
    max-width: 90%;
  }

  .forgot-password-content {
    flex-direction: column;
    text-align: center;
  }

  .forgot-password-title {
    font-size: 20px;
  }
}
</style>
