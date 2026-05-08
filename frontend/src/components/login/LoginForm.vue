<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CuteCharacter from './CuteCharacter.vue'
import HumanVerification from '../register/HumanVerification.vue'
import { login, checkLoginStatus } from '../../stores/auth'

const router = useRouter()

const form = reactive({
  username: '',
  password: ''
})

const isLoading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const captchaLoginEnabled = ref(true)
const isHumanVerificationVisible = ref(false)
let loginCallback: (() => void) | null = null

const togglePasswordVisibility = () => {
  console.log('Before toggle:', showPassword.value)
  showPassword.value = !showPassword.value
  console.log('After toggle:', showPassword.value)
}

const fetchSecurityConfig = async () => {
  try {
    const response = await fetch('/api/settings/public/security-config')
    const data = await response.json()
    captchaLoginEnabled.value = data.captchaLoginEnabled
  } catch (error) {
    console.error('获取安全配置失败', error)
    captchaLoginEnabled.value = true
  }
}

const handleLogin = async () => {
  if (!form.username || !form.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  errorMessage.value = ''

  // 如果启用了登录验证码，先验证
  if (captchaLoginEnabled.value) {
    loginCallback = () => {
      performLogin()
    }
    isHumanVerificationVisible.value = true
    return
  }

  await performLogin()
}

const performLogin = async () => {
  isLoading.value = true
  errorMessage.value = ''
  
  try {
    await login(form.username, form.password)
    // 登录成功后延时半秒再跳转首页
    await new Promise(resolve => setTimeout(resolve, 500))
    // 跳转到首页
    router.push('/home')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请检查用户名和密码'
  } finally {
    isLoading.value = false
  }
}

const handleHumanVerificationClose = (success: boolean) => {
  isHumanVerificationVisible.value = false

  if (success && loginCallback) {
    const callback = loginCallback
    loginCallback = null
    callback()
  }
}

onMounted(() => {
  // 清理所有可能的旧状态，避免错误显示"其他设备登录"
  checkLoginStatus()
  fetchSecurityConfig()
})
</script>

<template>
  <div class="wooden-sign-container">
    <!-- 绳子 -->
    <div class="rope"></div>
    <!-- 木牌 -->
    <div class="wooden-sign">
      <h1 class="login-title">用户登录</h1>
      
      <div class="login-content">
        <!-- 可爱角色 -->
        <div class="character-container">
          <CuteCharacter :showPassword="showPassword" />
        </div>
        
        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="username">用户名</label>
            <input 
              type="text" 
              id="username" 
              v-model="form.username" 
              placeholder="请输入用户名" 
              class="form-input"
              autocomplete="username"
            />
          </div>
          
          <div class="form-group">
            <label for="password">密码</label>
            <div class="password-input-container">
              <input 
                :type="showPassword ? 'text' : 'password'" 
                id="password" 
                v-model="form.password" 
                placeholder="请输入密码" 
                class="form-input password-input"
                autocomplete="current-password"
              />
              <button 
                type="button" 
                class="password-toggle-button" 
                @click="togglePasswordVisibility"
                aria-label="切换密码可见性"
              >
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/>
                  <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/>
                  <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/>
                  <line x1="2" x2="22" y1="2" y2="22"/>
                </svg>
              </button>
            </div>
          </div>
          
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>
          
          <button 
            type="submit" 
            class="login-button" 
            :disabled="isLoading"
          >
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>
      
      <div class="login-footer">
        <router-link to="/forgot-password" class="footer-link">忘记密码？</router-link>
        <span class="footer-divider">|</span>
        <router-link to="/register" class="footer-link">注册账号</router-link>
      </div>
    </div>
  </div>

  <!-- 人机验证组件 -->
  <HumanVerification
    :visible="isHumanVerificationVisible"
    @close="handleHumanVerificationClose"
  />
</template>

<style scoped>
.wooden-sign-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
  animation: sway 8s ease-in-out infinite;
  transform-origin: top center;
}

/* 绳子 */
.rope {
  width: 4px;
  height: 60px;
  background: linear-gradient(to bottom, #8B4513, #A0522D);
  border-radius: 2px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  margin-bottom: -10px;
  z-index: 1;
}

/* 木牌 */
.wooden-sign {
  background-color: #D2B48C;
  border-radius: 8px;
  box-shadow: 
    0 10px 20px rgba(0, 0, 0, 0.3),
    inset 0 0 20px rgba(0, 0, 0, 0.2);
  padding: 40px;
  width: 100%;
  max-width: 500px;
  position: relative;
  border: 2px solid #654321;
  /* 木纹效果 */
  background-image: 
    repeating-linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.1) 0px,
      rgba(255, 255, 255, 0.1) 2px,
      rgba(255, 255, 255, 0) 2px,
      rgba(255, 255, 255, 0) 10px
    );
  opacity: 1;
  z-index: 2;
  position: relative;
}

/* 木牌边缘效果 */
.wooden-sign::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 8px;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.3);
  pointer-events: none;
}

.login-title {
  text-align: center;
  color: #332211;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 700;
  text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
  font-family: 'Georgia', serif;
}

.login-content {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.character-container {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160px;
  height: 200px;
}

.login-form {
  flex: 1;
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
  font-weight: 600;
  color: #332211;
  font-family: 'Georgia', serif;
}

.form-input {
  padding: 12px 16px;
  border: 2px solid #8B4513;
  border-radius: 6px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2);
}

.form-input:focus {
  outline: none;
  border-color: #654321;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3), 0 0 0 2px rgba(139, 69, 19, 0.3);
  background: rgba(255, 255, 255, 0.95);
}

/* 密码输入框容器 */
.password-input-container {
  position: relative;
  display: flex;
  align-items: center;
}

/* 密码输入框 */
.password-input {
  flex: 1;
  padding-right: 50px; /* 为眼睛图标留出空间 */
}

/* 只有当input类型为password时才显示圆点 */
input[type="password"] {
  -webkit-text-security: disc;
}

/* 隐藏浏览器默认的密码可见性按钮 */
.password-input::-ms-reveal,
.password-input::-ms-clear {
  display: none;
}

/* 隐藏WebKit浏览器默认的密码可见性按钮 */
.password-input::-webkit-inner-spin-button,
.password-input::-webkit-outer-spin-button,
.password-input::-webkit-contacts-auto-fill-button {
  display: none;
}

/* 隐藏Firefox浏览器默认的密码可见性按钮 */
.password-input {
  -moz-appearance: textfield;
  appearance: textfield;
}

/* 密码切换按钮 */
.password-toggle-button {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #8B4513;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  pointer-events: auto;
}

.password-toggle-button:hover {
  background: rgba(139, 69, 19, 0.1);
  color: #654321;
}

.password-toggle-button:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(139, 69, 19, 0.3);
}

.error-message {
  background: rgba(220, 53, 69, 0.2);
  color: #721c24;
  padding: 10px;
  border-radius: 6px;
  font-size: 14px;
  text-align: center;
  border: 1px solid rgba(220, 53, 69, 0.3);
}

.login-button {
  background: linear-gradient(135deg, #8B4513 0%, #654321 100%);
  color: white;
  border: 2px solid #5D4037;
  border-radius: 6px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  font-family: 'Georgia', serif;
}

.login-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #A0522D 0%, #8B4513 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.login-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #332211;
  font-family: 'Georgia', serif;
}

.footer-link {
  color: #332211;
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 500;
}

.footer-link:hover {
  color: #1a1a1a;
  text-decoration: underline;
}

.footer-divider {
  margin: 0 10px;
  color: rgba(51, 34, 17, 0.5);
}

/* 随风摆动动画 */
@keyframes sway {
  0% {
    transform: rotate(-2deg);
  }
  50% {
    transform: rotate(2deg);
  }
  100% {
    transform: rotate(-2deg);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .wooden-sign {
    padding: 30px 20px;
  }
  
  .login-title {
    font-size: 20px;
  }
  
  .login-content {
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }
  
  .character-container {
    order: -1;
  }
  
  .form-input {
    padding: 10px 14px;
  }
  
  .login-button {
    padding: 12px;
  }
  
  .rope {
    height: 40px;
  }
}
</style>