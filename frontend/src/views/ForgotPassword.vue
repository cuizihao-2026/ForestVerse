<template>
  <div class="forgot-password-view">
    <Background />
    <div class="content-container">
      <ForgotPassword />
    </div>
  </div>
</template>

<script setup lang="ts">
import Background from '../components/login/Background.vue'
import ForgotPassword from '../components/register/ForgotPassword.vue'
</script>

<style scoped>
.forgot-password-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.content-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 10;
  padding: 20px;
}

@media (max-width: 768px) {
  .content-container {
    padding: 10px;
  }
}
</style>
