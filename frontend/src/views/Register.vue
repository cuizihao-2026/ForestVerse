<template>
  <Background>
    <div class="register-container">
      <RegisterForm />
    </div>
  </Background>
</template>

<script setup lang="ts">
import Background from '../components/login/Background.vue'
import RegisterForm from '../components/register/RegisterForm.vue'
</script>

<style scoped>
.register-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}
</style>