<script setup lang="ts">
import { ref } from 'vue'
import { API_BASE_URL } from '../../utils/api'

const props = defineProps<{
  cover: string
  title: string
  category?: string
}>()

const emit = defineEmits<{
  toggle: []
}>()

const isFrameMode = ref(false)

const toggleMode = () => {
  isFrameMode.value = !isFrameMode.value
}

const imageUrl = (path: string) => path ? API_BASE_URL + path : ''
</script>

<template>
  <div class="hero" :class="{ 'frame-mode': isFrameMode }" @click="toggleMode">
    <img :src="imageUrl(props.cover)" :alt="props.title" class="hero-img" />
    <div class="hero-overlay">
      <el-tag v-if="props.category" effect="dark" size="small" round class="hero-cat">
        {{ props.category }}
      </el-tag>
      <div class="mode-hint">
        {{ isFrameMode ? '点击切换缩略图' : '点击切换相框' }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  height: 360px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f7fa;
}

.hero.frame-mode {
  background: repeating-linear-gradient(
    45deg,
    #f0f2f5,
    #f0f2f5 10px,
    #e8ecf1 10px,
    #e8ecf1 20px
  );
}

.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.35s ease;
}

.hero.frame-mode .hero-img {
  object-fit: contain;
  padding: 24px;
  box-sizing: border-box;
}

.hero:not(.frame-mode):hover .hero-img {
  transform: scale(1.02);
}

.hero-overlay {
  position: absolute;
  z-index: 2;
  inset: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px;
}

.hero-cat {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
}

.mode-hint {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.hero:hover .mode-hint {
  opacity: 1;
}

@media (max-width: 860px) {
  .hero {
    height: 240px;
  }
  
  .hero.frame-mode .hero-img {
    padding: 16px;
  }
}
</style>
