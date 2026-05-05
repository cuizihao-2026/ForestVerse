<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SimpleModal from './components/common/SimpleModal.vue'
import { initModal } from './stores/modal'
import { loadSiteConfig } from './stores/siteConfig'

const route = useRoute()
const modalRef = ref<InstanceType<typeof SimpleModal> | null>(null)

// 监听路由变化，动态控制滚动条显示
const updateScrollbar = () => {
  if (route.path === '/home') {
    // 首页隐藏滚动条
    document.documentElement.classList.add('hide-scrollbar')
    document.body.classList.add('hide-scrollbar')
  } else {
    // 其他页面显示滚动条
    document.documentElement.classList.remove('hide-scrollbar')
    document.body.classList.remove('hide-scrollbar')
  }
}

watch(() => route.path, updateScrollbar, { immediate: true })

onMounted(() => {
  if (modalRef.value) {
    initModal(modalRef.value)
  }
  // 加载网站配置（包括 favicon 和页面标题）
  loadSiteConfig()
})
</script>

<template>
  <router-view />
  <SimpleModal ref="modalRef" />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  background-color: #f5f5f5;
}

/* 确保所有页面都铺满屏幕 */
html, body, #app {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
}

/* 隐藏滚动条的类 */
.hide-scrollbar {
  /* Firefox */
  scrollbar-width: none;
  /* IE and Edge */
  -ms-overflow-style: none;
}

/* Chrome, Safari 和 Opera */
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
