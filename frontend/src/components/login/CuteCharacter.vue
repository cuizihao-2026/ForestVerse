<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  showPassword: {
    type: Boolean,
    default: false
  }
})

const characterRef = ref<HTMLElement | null>(null)
const eyeLeftRef = ref<HTMLElement | null>(null)
const eyeRightRef = ref<HTMLElement | null>(null)
const isBlinking = ref(false)

// 监听showPassword和isBlinking的变化，当角色闭眼时重置眼睛位置
watch(
  [() => props.showPassword, () => isBlinking.value],
  ([newShowPassword, newIsBlinking]) => {
    if (newShowPassword || newIsBlinking) {
      resetEyePosition()
    }
  }
)

const handleMouseMove = (event: MouseEvent) => {
  // 当角色闭眼时，不追踪鼠标
  if (props.showPassword || isBlinking.value) {
    // 当角色闭眼时，眼睛回到初始位置
    resetEyePosition()
    return
  }
  
  if (!characterRef.value || !eyeLeftRef.value || !eyeRightRef.value) return
  
  // 计算角色元素的位置和尺寸
  const rect = characterRef.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  
  // 计算鼠标相对于角色中心的位置
  const mouseX = event.clientX
  const mouseY = event.clientY
  
  // 计算角度和距离
  const deltaX = mouseX - centerX
  const deltaY = mouseY - centerY
  const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY)
  
  // 限制眼睛移动范围
  const maxDistance = 8
  const maxMoveY = 5 // 限制垂直移动范围，防止眼睛与嘴巴重合
  const moveX = (deltaX / distance) * Math.min(distance, maxDistance)
  const moveY = Math.max(-maxMoveY, Math.min(maxMoveY, (deltaY / distance) * Math.min(distance, maxDistance)))
  
  // 更新眼睛位置
  if (eyeLeftRef.value) {
    eyeLeftRef.value.style.transform = `translate(${moveX}px, ${moveY}px)`
  }
  if (eyeRightRef.value) {
    eyeRightRef.value.style.transform = `translate(${moveX}px, ${moveY}px)`
  }
}

// 重置眼睛位置到初始状态（带过渡效果）
const resetEyePosition = () => {
  if (eyeLeftRef.value) {
    eyeLeftRef.value.style.transition = 'transform 0.5s ease-out'
    eyeLeftRef.value.style.transform = 'translate(0, 0)'
    // 动画结束后清除过渡效果，避免影响后续的鼠标追踪
    setTimeout(() => {
      if (eyeLeftRef.value) {
        eyeLeftRef.value.style.transition = 'transform 0.1s ease'
      }
    }, 500)
  }
  if (eyeRightRef.value) {
    eyeRightRef.value.style.transition = 'transform 0.5s ease-out'
    eyeRightRef.value.style.transform = 'translate(0, 0)'
    // 动画结束后清除过渡效果，避免影响后续的鼠标追踪
    setTimeout(() => {
      if (eyeRightRef.value) {
        eyeRightRef.value.style.transition = 'transform 0.1s ease'
      }
    }, 500)
  }
}

const startBlinking = () => {
  setInterval(() => {
    if (!props.showPassword) {
      isBlinking.value = true
      setTimeout(() => {
        isBlinking.value = false
      }, 200)
    }
  }, 3000)
}

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
  startBlinking()
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<template>
  <div class="photo-frame">
    <div class="frame-border">
      <div class="frame-inner">
        <div class="cute-character" ref="characterRef">
          <!-- 头部 -->
          <div class="character-head">
            <!-- 眼睛 -->
            <div class="character-eyes" :class="{ 'sleeping': showPassword }">
              <div class="eye left" ref="eyeLeftRef">
                <div class="eye-inner" :class="{ 'closed': showPassword || isBlinking }"></div>
              </div>
              <div class="eye right" ref="eyeRightRef">
                <div class="eye-inner" :class="{ 'closed': showPassword || isBlinking }"></div>
              </div>
            </div>
            <!-- 嘴巴 -->
            <div class="character-mouth" :class="{ 'sleeping': showPassword }">
              <div class="mouth-shape"></div>
              <div class="zZZ" v-if="showPassword">zZZ</div>
            </div>
          </div>
          <!-- 身子 -->
          <div class="character-body">
            <div class="body-shape"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 相片框 */
.photo-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 180px;
}

.frame-border {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #D2B48C 0%, #8B4513 100%);
  border-radius: 8px;
  box-shadow: 
    0 4px 8px rgba(0, 0, 0, 0.3),
    inset 0 0 10px rgba(0, 0, 0, 0.2);
  padding: 10px;
  border: 2px solid #654321;
  position: relative;
}

/* 相片框内部 */
.frame-inner {
  width: 100%;
  height: 100%;
  background: #F5F5F5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}

/* 相片框装饰 */
.frame-border::before {
  content: '';
  position: absolute;
  top: -5px;
  left: -5px;
  right: -5px;
  bottom: -5px;
  background: linear-gradient(45deg, transparent 48%, #8B4513 49%, #8B4513 51%, transparent 52%);
  border-radius: 12px;
  z-index: -1;
}

.cute-character {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.character-head {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #FFDBAC 0%, #F4A460 100%);
  border-radius: 50%;
  position: relative;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  border: 2px solid #F4A460;
  z-index: 1;
}

/* 身子 */
.character-body {
  margin-top: -10px; /* 与头部重叠一点，看起来更自然 */
  z-index: 0;
}

.body-shape {
  width: 45px;
  height: 60px;
  background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
  border-radius: 30px 30px 0 0;
  position: relative;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  border: 2px solid #357ABD;
}

/* 衣服领子 */
.body-shape::before {
  content: '';
  position: absolute;
  top: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 22px;
  height: 10px;
  background: white;
  border-radius: 4px 4px 0 0;
  border: 1px solid #357ABD;
}

/* 衣服纽扣 */
.body-shape::after {
  content: '';
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background: white;
  border-radius: 50%;
  border: 1px solid #357ABD;
  box-shadow: 0 10px 0 white, 0 10px 0 1px #357ABD;
}

.character-eyes {
  position: absolute;
  top: 15px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  padding: 0 12px;
}

.eye {
  width: 12px;
  height: 12px;
  background: white;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  border: 1px solid #333;
  transition: transform 0.1s ease;
}

.eye-inner {
  width: 6px;
  height: 6px;
  background: #333;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: all 0.3s ease;
}

.eye-inner.closed {
  height: 2px;
  border-radius: 1px;
  background: #333;
}

.character-mouth {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  transition: all 0.3s ease;
}

.mouth-shape {
  width: 16px;
  height: 8px;
  border: 2px solid #333;
  border-top: none;
  border-radius: 0 0 20px 20px;
  transition: all 0.3s ease;
}

.character-mouth.sleeping .mouth-shape {
  width: 12px;
  height: 4px;
  border-radius: 0 0 10px 10px;
}

/* zZZ动画 */
.zZZ {
  position: absolute;
  top: -50px;
  right: -30px;
  font-size: 14px;
  font-weight: bold;
  color: #333;
  animation: floatUp 2s ease-in-out infinite;
}

@keyframes floatUp {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0.8);
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(-20px) scale(1.2);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .character-head {
    width: 60px;
    height: 60px;
  }
  
  .character-eyes {
    top: 18px;
    padding: 0 10px;
  }
  
  .eye {
    width: 12px;
    height: 12px;
  }
  
  .eye-inner {
    width: 6px;
    height: 6px;
  }
  
  .mouth-shape {
    width: 16px;
    height: 8px;
  }
}
</style>