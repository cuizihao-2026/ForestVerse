<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'

type Theme = 'light' | 'dark'

const currentTheme = ref<Theme>('dark')
const previousTheme = ref<Theme>('dark')
const sunPosition = ref({ top: '80%', left: '-10%', right: 'auto', opacity: '0' })
const moonPosition = ref({ top: '10%', right: '10%', left: 'auto', opacity: '1' })

const toggleTheme = async () => {
  const oldTheme = currentTheme.value
  const newTheme = oldTheme === 'light' ? 'dark' : 'light'
  previousTheme.value = oldTheme
  currentTheme.value = newTheme
  localStorage.setItem('login-theme', newTheme)

  if (newTheme === 'light') {
    // 切换到白天：太阳从左下角升起，月亮从右上角落下到右下角
    // 第一步：先把太阳放到左下角（这个瞬间没有动画）
    sunPosition.value = { top: '80%', left: '-10%', right: 'auto', opacity: '0' }
    // 等待 DOM 更新
    await nextTick()
    // 第二步：同时让太阳升起，月亮落下
    setTimeout(() => {
      sunPosition.value = { top: '10%', right: '10%', left: 'auto', opacity: '1' }
      moonPosition.value = { top: '80%', right: '-10%', left: 'auto', opacity: '0' }
    }, 50)
  } else {
    // 切换到夜晚：月亮从左下角升起，太阳从右上角落下到右下角
    // 第一步：先把月亮放到左下角（这个瞬间没有动画）
    moonPosition.value = { top: '80%', left: '-10%', right: 'auto', opacity: '0' }
    // 等待 DOM 更新
    await nextTick()
    // 第二步：同时让月亮升起，太阳落下
    setTimeout(() => {
      moonPosition.value = { top: '10%', right: '10%', left: 'auto', opacity: '1' }
      sunPosition.value = { top: '80%', right: '-10%', left: 'auto', opacity: '0' }
    }, 50)
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('login-theme') as Theme
  if (savedTheme) {
    currentTheme.value = savedTheme
    previousTheme.value = savedTheme
    if (savedTheme === 'light') {
      sunPosition.value = { top: '10%', right: '10%', left: 'auto', opacity: '1' }
      moonPosition.value = { top: '80%', left: '-10%', right: 'auto', opacity: '0' }
    } else {
      moonPosition.value = { top: '10%', right: '10%', left: 'auto', opacity: '1' }
      sunPosition.value = { top: '80%', left: '-10%', right: 'auto', opacity: '0' }
    }
  } else {
    moonPosition.value = { top: '10%', right: '10%', left: 'auto', opacity: '1' }
    sunPosition.value = { top: '80%', left: '-10%', right: 'auto', opacity: '0' }
  }
})
</script>

<template>
  <div class="background" :class="currentTheme">
    <div class="sun-moon-container">
      <div 
        class="celestial-body sun" 
        :style="sunPosition"
      >
        <div class="sun-rays"></div>
      </div>
      <div 
        class="celestial-body moon" 
        :style="moonPosition"
      >
        <div class="moon-craters">
          <div class="crater crater-1"></div>
          <div class="crater crater-2"></div>
          <div class="crater crater-3"></div>
        </div>
      </div>
    </div>
    <div class="stars" v-if="currentTheme === 'dark'">
      <div class="star" v-for="i in 30" :key="i" :style="{ animationDelay: `${i * 0.1}s` }"></div>
    </div>
    <div class="mesh-grid"></div>
    <button class="theme-toggle" @click="toggleTheme" :title="currentTheme === 'light' ? '切换到夜晚' : '切换到白天'">
      <div class="toggle-icon">
        <span v-if="currentTheme === 'light'" class="toggle-sun">🌞</span>
        <span v-else class="toggle-moon">🌙</span>
      </div>
    </button>
    <slot></slot>
  </div>
</template>

<style scoped>
.background {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100vw;
  padding: 20px;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0;
  box-sizing: border-box;
  z-index: 1;
  overflow: hidden;
  transition: background 1.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.background.light {
  background: linear-gradient(180deg, #87CEEB 0%, #B0E0E6 30%, #E0F6FF 60%, #F0F9FF 100%);
}

.background.dark {
  background: linear-gradient(180deg, #020617 0%, #0f172a 40%, #1e293b 100%);
}

.sun-moon-container {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
}

.celestial-body {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  transition: all 2s cubic-bezier(0.4, 0, 0.2, 1);
}

.celestial-body.sun {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 50%, #FF8C00 100%);
  box-shadow: 0 0 60px rgba(255, 215, 0, 0.6), 0 0 120px rgba(255, 165, 0, 0.4);
}

.celestial-body.moon {
  background: linear-gradient(135deg, #F5F5F5 0%, #E0E0E0 50%, #C0C0C0 100%);
  box-shadow: 0 0 40px rgba(255, 255, 255, 0.5), 0 0 80px rgba(200, 200, 200, 0.3);
}

@keyframes floatSlow {
  0%, 100% { 
    transform: scale(1);
  }
  25% { 
    transform: scale(1.05);
  }
  50% { 
    transform: scale(0.95);
  }
  75% { 
    transform: scale(1.02);
  }
}

.sun-rays {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  animation: sunRotate 20s linear infinite;
}

.sun-rays::before,
.sun-rays::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 140%;
  height: 140%;
  border-radius: 50%;
  border: 2px dashed rgba(255, 215, 0, 0.3);
}

.sun-rays::after {
  width: 180%;
  height: 180%;
  border: 1px dashed rgba(255, 165, 0, 0.2);
  animation-delay: -10s;
}

@keyframes sunRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.moon-craters {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

.crater {
  position: absolute;
  border-radius: 50%;
  background: rgba(180, 180, 180, 0.4);
  box-shadow: inset 1px 1px 3px rgba(0, 0, 0, 0.2);
}

.crater-1 {
  width: 25px;
  height: 25px;
  top: 30%;
  left: 25%;
}

.crater-2 {
  width: 35px;
  height: 35px;
  top: 50%;
  right: 20%;
}

.crater-3 {
  width: 20px;
  height: 20px;
  bottom: 30%;
  left: 40%;
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.star {
  position: absolute;
  width: 4px;
  height: 4px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
  animation: twinkle 3s ease-in-out infinite;
}

.star:nth-child(1) { top: 10%; left: 15%; }
.star:nth-child(2) { top: 20%; left: 40%; }
.star:nth-child(3) { top: 15%; left: 70%; }
.star:nth-child(4) { top: 30%; left: 85%; }
.star:nth-child(5) { top: 25%; left: 5%; }
.star:nth-child(6) { top: 40%; left: 20%; }
.star:nth-child(7) { top: 45%; left: 55%; }
.star:nth-child(8) { top: 35%; left: 80%; }
.star:nth-child(9) { top: 50%; left: 10%; }
.star:nth-child(10) { top: 55%; left: 35%; }
.star:nth-child(11) { top: 60%; left: 65%; }
.star:nth-child(12) { top: 65%; left: 90%; }
.star:nth-child(13) { top: 70%; left: 25%; }
.star:nth-child(14) { top: 75%; left: 50%; }
.star:nth-child(15) { top: 80%; left: 75%; }
.star:nth-child(16) { top: 12%; left: 60%; }
.star:nth-child(17) { top: 22%; left: 92%; }
.star:nth-child(18) { top: 42%; left: 72%; }
.star:nth-child(19) { top: 62%; left: 32%; }
.star:nth-child(20) { top: 82%; left: 12%; }
.star:nth-child(21) { top: 5%; left: 30%; }
.star:nth-child(22) { top: 38%; left: 48%; }
.star:nth-child(23) { top: 58%; left: 88%; }
.star:nth-child(24) { top: 78%; left: 58%; }
.star:nth-child(25) { top: 8%; left: 82%; }
.star:nth-child(26) { top: 32%; left: 12%; }
.star:nth-child(27) { top: 52%; left: 42%; }
.star:nth-child(28) { top: 72%; left: 72%; }
.star:nth-child(29) { top: 18%; left: 52%; }
.star:nth-child(30) { top: 48%; left: 92%; }

@keyframes twinkle {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}

.mesh-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 100px 100px;
  pointer-events: none;
}

.background.light .mesh-grid {
  background-image: 
    linear-gradient(rgba(100, 150, 200, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(100, 150, 200, 0.1) 1px, transparent 1px);
}

.theme-toggle {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  font-size: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
}

.background.dark .theme-toggle {
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(20px);
  border: 2px solid rgba(148, 163, 184, 0.25);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.background.dark .theme-toggle:hover {
  transform: scale(1.1) rotate(15deg);
  box-shadow: 
    0 12px 48px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.background.light .theme-toggle {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 2px solid rgba(100, 150, 200, 0.3);
  box-shadow: 
    0 8px 32px rgba(100, 150, 200, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.background.light .theme-toggle:hover {
  transform: scale(1.1) rotate(-15deg);
  box-shadow: 
    0 12px 48px rgba(100, 150, 200, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 1);
}

.toggle-icon {
  font-size: 32px;
  line-height: 1;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-toggle:hover .toggle-icon {
  transform: scale(1.1);
}
</style>
