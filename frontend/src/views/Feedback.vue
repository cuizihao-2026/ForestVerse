<script setup lang="ts">
import { ref } from 'vue'
import Navbar from '../components/home/Navbar.vue'
import FeedbackSidebar from '../components/feedback/FeedbackSidebar.vue'
import FeedbackForm from '../components/feedback/FeedbackForm.vue'

const selectedFeedback = ref<any>(null)
const sidebarRef = ref<any>(null)

const handleSelectFeedback = (feedback: any) => {
  selectedFeedback.value = feedback
}

const handleNewFeedback = () => {
  selectedFeedback.value = null
}

const handleSubmitted = () => {
  selectedFeedback.value = null
  sidebarRef.value?.refresh()
}
</script>

<template>
  <div class="feedback-page">
    <Navbar />
    <div class="feedback-main">
      <div class="sidebar-wrapper">
        <FeedbackSidebar 
          ref="sidebarRef"
          @select-feedback="handleSelectFeedback" 
          @new-feedback="handleNewFeedback" 
        />
      </div>
      <FeedbackForm 
        :selected-feedback="selectedFeedback" 
        @submitted="handleSubmitted"
      />
    </div>
  </div>
</template>

<style scoped>
.feedback-page {
  height: 100vh;
  overflow: hidden;
  background: #f5f5f5;
  font-family: var(--sans);
  padding-top: 68px;
  box-sizing: border-box;
}

.feedback-main {
  display: flex;
  height: calc(100vh - 68px);
  overflow: hidden;
  background: #fff;
  position: relative;
}

.sidebar-wrapper {
  display: block;
}
</style>
