<script setup lang="ts">
import Navbar from '../components/home/Navbar.vue'
import ArticleList from '../components/home/ArticleList.vue'
</script>

<template>
  <div class="home-page">
    <Navbar />
    <ArticleList />
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
