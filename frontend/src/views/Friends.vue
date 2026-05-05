<script setup lang="ts">
import { ref } from 'vue'
import Navbar from '../components/home/Navbar.vue'
import FriendsSidebar from '../components/friends/FriendsSidebar.vue'
import ChatWindow from '../components/friends/ChatWindow.vue'

const currentChat = ref<any>(null)

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
    <div class="friends-main">
      <FriendsSidebar v-model="currentChat" />
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
}
</style>
