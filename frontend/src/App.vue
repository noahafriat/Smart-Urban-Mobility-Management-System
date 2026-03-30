<script setup lang="ts">
import { RouterView } from 'vue-router'
import NavBar from './components/NavBar.vue'
</script>

<template>
  <a href="#main-content" class="skip-link">Skip to main content</a>
  <NavBar />
  <main id="main-content" class="main-container" tabindex="-1">
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

/* Skip link: visible when keyboard-focused */
.skip-link {
  position: absolute;
  left: 0;
  top: 0;
  transform: translateY(-120%);
  z-index: 10000;
  padding: 0.65rem 1.1rem;
  background: #0f172a;
  color: #fff;
  font-weight: 700;
  font-size: 0.95rem;
  border-radius: 0 0 8px 0;
  text-decoration: none;
}

.skip-link:focus,
.skip-link:focus-visible {
  transform: translateY(0);
  outline: 3px solid #facc15;
  outline-offset: 2px;
}
</style>
