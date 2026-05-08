<script setup lang="ts">
import { ref } from 'vue'
import Navbar from '../components/home/Navbar.vue'
import FriendsSidebar from '../components/friends/FriendsSidebar.vue'
import ChatWindow from '../components/friends/ChatWindow.vue'
import { MoreFilled } from '@element-plus/icons-vue'

const currentChat = ref<any>(null)
const sidebarOpen = ref(false)

const formatMessageTime = (timeStr: string) => {
  try {
    const date = new Date(timeStr)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  } catch {
    return '刚刚'
  }
}

const handleSendMessage = (message: any) => {
  if (currentChat.value) {
    currentChat.value.messages.push(message)
    currentChat.value.lastMessage = message.content
    currentChat.value.lastTime = formatMessageTime(message.createdAt)
  }
}
</script>

<template>
  <div class="friends-page">
    <Navbar />
    <!-- 移动端侧边栏遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>
    <div class="friends-main">
      <!-- 移动端侧边栏开关按钮 -->
      <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen">
        <el-icon><MoreFilled /></el-icon>
      </button>
      <!-- 侧边栏 -->
      <div class="sidebar-wrapper" :class="{ 'sidebar-open': sidebarOpen }">
        <FriendsSidebar v-model="currentChat" @close-sidebar="sidebarOpen = false" />
      </div>
      <ChatWindow :chat="currentChat" @send="handleSendMessage" />
    </div>
  </div>
</template>

<style scoped>
.friends-page {
  height: 100vh;
  overflow: hidden;
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  padding-top: 68px;
  box-sizing: border-box;
}

.friends-main {
  display: flex;
  height: calc(100vh - 68px);
  overflow: hidden;
  background: #fff;
  position: relative;
}

.sidebar-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  position: fixed;
  bottom: 80px;
  right: 20px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #6b7280;
  color: #fff;
  border: none;
  cursor: pointer;
  z-index: 999;
  box-shadow: 0 4px 12px rgba(107, 114, 128, 0.4);
  transition: all 0.3s ease;
}

.sidebar-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(107, 114, 128, 0.5);
  background: #4b5563;
}

.sidebar-toggle .el-icon {
  font-size: 24px;
}

.sidebar-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 997;
  animation: fadeIn 0.3s ease;
}

.sidebar-wrapper {
  display: block;
}

@media (max-width: 860px) {
  .sidebar-toggle {
    display: flex;
  }

  .sidebar-overlay {
    display: block;
  }

  .sidebar-wrapper {
    position: fixed;
    top: 0;
    left: -100%;
    width: 90%;
    max-width: 360px;
    height: 100vh;
    background: #fafafa;
    z-index: 998;
    transition: left 0.3s ease;
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
    padding-top: 68px;
    box-sizing: border-box;
    overflow-y: auto;
  }

  .sidebar-wrapper.sidebar-open {
    left: 0;
  }

  .sidebar-wrapper .sidebar {
    width: 100%;
    border-right: none;
    border-radius: 0;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
