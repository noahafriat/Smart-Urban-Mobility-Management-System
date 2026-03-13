<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const errorMsg = ref('')

async function submit() {
  errorMsg.value = ''
  if (!email.value || !password.value) {
    errorMsg.value = 'Please enter both email and password.'
    return
  }

  try {
    await auth.login(email.value, password.value)
    router.push('/dashboard')
  } catch (e: any) {
    errorMsg.value = typeof e === 'string' ? e : 'Invalid credentials. Please try again.'
  }
}

// Quick demo login method
function demoLogin(demoEmail: string, demoPass: string) {
  email.value = demoEmail
  password.value = demoPass
  submit()
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>Welcome Back</h1>
      <p class="subtitle">Sign in to your SUMMS account</p>

      <div v-if="errorMsg" class="error-msg" role="alert">
        {{ errorMsg }}
      </div>

      <form @submit.prevent="submit" novalidate>
        <div class="field">
          <label for="login-email">Email Address</label>
          <input
            id="login-email"
            v-model="email"
            type="email"
            placeholder="alice@summs.ca"
            required
            autocomplete="email"
          />
        </div>

        <div class="field">
          <label for="login-password">Password</label>
          <input
            id="login-password"
            v-model="password"
            type="password"
            placeholder="••••••••"
            required
            autocomplete="current-password"
          />
        </div>

        <button type="submit" :disabled="auth.loading" class="primary-btn">
          {{ auth.loading ? 'Signing in…' : 'Sign In' }}
        </button>
      </form>

      <div class="demo-accounts">
        <p class="demo-title">Test Accounts:</p>
        <div class="chip-container">
          <button @click="demoLogin('alice@summs.ca', 'password123')" type="button" class="chip citizen">Citizen</button>
          <button @click="demoLogin('bixi@summs.ca', 'bixi123')" type="button" class="chip provider">Provider</button>
          <button @click="demoLogin('admin@summs.ca', 'admin123')" type="button" class="chip admin">City Admin</button>
          <button @click="demoLogin('sys@summs.ca', 'sys123')" type="button" class="chip sys">Sys Admin</button>
        </div>
      </div>

      <p class="footer-link">
        Don't have an account?
        <RouterLink to="/register">Create one now</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4f8;
  padding: 2rem 1rem;
}

.login-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 400px;
}

h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.25rem;
  color: #1a202c;
}

.subtitle {
  color: #718096;
  margin: 0 0 1.75rem;
  font-size: 0.9rem;
}

.error-msg {
  background: #fff5f5;
  border: 1px solid #feb2b2;
  color: #c53030;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  font-size: 0.875rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #4a5568;
}

input {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  transition: border-color 0.2s;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}

input:focus {
  border-color: #4299e1;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.15);
}

.primary-btn {
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: #4299e1;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.primary-btn:hover:not(:disabled) {
  background: #3182ce;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.demo-accounts {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e2e8f0;
}

.demo-title {
  font-size: 0.85rem;
  color: #718096;
  margin-bottom: 0.75rem;
  text-align: center;
}

.chip-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.chip {
  padding: 0.4rem 0.8rem;
  border-radius: 16px;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s, opacity 0.2s;
}

.chip:hover {
  transform: scale(1.05);
  opacity: 0.9;
}

.chip.citizen { background: #eebfbf; color: #822727; }
.chip.provider { background: #c6f6d5; color: #22543d; }
.chip.admin { background: #bee3f8; color: #2a4365; }
.chip.sys { background: #e9d8fd; color: #44337a; }

.footer-link {
  text-align: center;
  margin-top: 1.75rem;
  font-size: 0.875rem;
  color: #718096;
}

.footer-link a {
  color: #4299e1;
  font-weight: 600;
  text-decoration: none;
}
</style>
