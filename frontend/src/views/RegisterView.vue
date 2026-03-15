<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth   = useAuthStore()
const router = useRouter()

const form = ref({
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  role: 'CITIZEN',
  preferredMobilityType: 'CAR',
})
const errorMsg = ref('')
const loading  = ref(false)

async function submit() {
  errorMsg.value = ''

  if (form.value.password !== form.value.confirmPassword) {
    errorMsg.value = 'Passwords do not match.'
    return
  }
  if (form.value.password.length < 6) {
    errorMsg.value = 'Password must be at least 6 characters.'
    return
  }

  loading.value = true
  try {
    await auth.register({
      name:     form.value.name,
      email:    form.value.email,
      password: form.value.password,
      role:     form.value.role,
      phone:    form.value.phone,
      preferredMobilityType: form.value.role === 'MOBILITY_PROVIDER' ? form.value.preferredMobilityType : undefined,
    })
    // Ensure they know to log in with their new credentials
    window.alert('Account created successfully! Please log in.')
    router.push('/login')
  } catch (e: any) {
    errorMsg.value = typeof e === 'string' ? e : 'Registration failed. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h1>Create Account</h1>
      <p class="subtitle">Join SUMMS and start commuting smarter.</p>

      <div v-if="errorMsg" class="error-msg" role="alert">
        {{ errorMsg }}
      </div>

      <form id="form-register" @submit.prevent="submit" novalidate>
        <div class="field">
          <label for="reg-name">Full Name</label>
          <input
            id="reg-name"
            v-model="form.name"
            type="text"
            placeholder="Alice Martin"
            required
            autocomplete="name"
          />
        </div>

        <div class="field">
          <label for="reg-email">Email Address</label>
          <input
            id="reg-email"
            v-model="form.email"
            type="email"
            placeholder="you@example.com"
            required
            autocomplete="email"
          />
        </div>

        <div class="field">
          <label for="reg-phone">Phone (optional)</label>
          <input
            id="reg-phone"
            v-model="form.phone"
            type="tel"
            placeholder="+1 514 000 0000"
            autocomplete="tel"
          />
        </div>

        <div class="field">
          <label for="reg-role">Account Type</label>
          <select id="reg-role" v-model="form.role">
            <option value="CITIZEN">Citizen / Commuter</option>
            <option value="MOBILITY_PROVIDER">Mobility Service Provider</option>
            <option value="CITY_ADMIN">City Administrator</option>
          </select>
        </div>

        <div v-if="form.role === 'MOBILITY_PROVIDER'" class="field">
          <label for="reg-provider-type">Provider Type</label>
          <select id="reg-provider-type" v-model="form.preferredMobilityType">
            <option value="CAR">Car Provider</option>
            <option value="SCOOTER">Scooter Provider</option>
          </select>
        </div>

        <div class="field">
          <label for="reg-password">Password</label>
          <input
            id="reg-password"
            v-model="form.password"
            type="password"
            placeholder="Min. 6 characters"
            required
            autocomplete="new-password"
          />
        </div>

        <div class="field">
          <label for="reg-confirm">Confirm Password</label>
          <input
            id="reg-confirm"
            v-model="form.confirmPassword"
            type="password"
            placeholder="Repeat your password"
            required
            autocomplete="new-password"
          />
        </div>

        <button id="btn-register-submit" type="submit" :disabled="loading">
          {{ loading ? 'Creating account…' : 'Create Account' }}
        </button>
      </form>

      <p class="footer-link">
        Already have an account?
        <RouterLink to="/login">Sign in</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f4f8;
  padding: 2rem 1rem;
}

.register-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 440px;
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
  gap: 1rem;
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

input, select {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.6rem 0.9rem;
  font-size: 0.95rem;
  transition: border-color 0.2s;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}

input:focus, select:focus {
  border-color: #4299e1;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.15);
}

button[type="submit"] {
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

button[type="submit"]:hover:not(:disabled) {
  background: #3182ce;
}

button[type="submit"]:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.footer-link {
  text-align: center;
  margin-top: 1.25rem;
  font-size: 0.875rem;
  color: #718096;
}

.footer-link a {
  color: #4299e1;
  font-weight: 600;
  text-decoration: none;
}
</style>
