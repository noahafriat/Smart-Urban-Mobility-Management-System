<script setup lang="ts">
// Basic landing page that the user sees before logging in
</script>

<template>
  <div class="landing">
    <div class="hero">
      <h1>Smart Urban Mobility Management System</h1>
      <p>Unifying urban mobility services in one seamless platform.</p>
      <div class="cta-buttons">
        <RouterLink to="/register" class="btn primary">Get Started</RouterLink>
        <RouterLink to="/login" class="btn secondary">Sign In</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landing {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #e2e8f0 0%, #edf2f7 100%);
  text-align: center;
  padding: 2rem;
}

.hero {
  max-width: 800px;
}

h1 {
  font-size: 3.5rem;
  color: #1a202c;
  letter-spacing: -1px;
  line-height: 1.2;
  margin-bottom: 1rem;
}

p {
  font-size: 1.25rem;
  color: #4a5568;
  margin-bottom: 2.5rem;
}

.cta-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn {
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.btn.primary {
  background: #3182ce;
  color: white;
  box-shadow: 0 4px 6px rgba(49, 130, 206, 0.2);
}

.btn.primary:hover {
  background: #2b6cb0;
  transform: translateY(-2px);
  box-shadow: 0 6px 8px rgba(49, 130, 206, 0.3);
}

.btn.secondary {
  background: white;
  color: #4a5568;
  border: 1px solid #e2e8f0;
}

.btn.secondary:hover {
  background: #f7fafc;
  border-color: #cbd5e0;
}
</style>
