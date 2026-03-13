<script setup lang="ts">
import { RouterView } from 'vue-router'
import NavBar from './components/NavBar.vue'
</script>

<template>
  <NavBar />
  <main class="main-container">
    <RouterView />
  </main>
</template>

<style>
/* Global CSS for the app */
body, html {
  margin: 0;
  padding: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background-color: #f7fafc;
  color: #2d3748;
}

* {
  box-sizing: border-box;
}

.main-container {
  min-height: calc(100vh - 64px); 
}
</style>
