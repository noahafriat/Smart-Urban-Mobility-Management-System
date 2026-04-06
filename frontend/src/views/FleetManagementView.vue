<script setup lang="ts">
import ProviderFleetManager from '../components/ProviderFleetManager.vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

// Security check: Only car or scooter providers allowed
if (!auth.isProvider || auth.isParkingProvider) {
  router.push('/dashboard')
}
</script>

<template>
  <div class="fleet-management fade-in" v-if="auth.user">
    <header class="page-header">
      <div class="breadcrumb">
        <RouterLink to="/dashboard">Dashboard</RouterLink> / 
        <span>Fleet Operations</span>
      </div>
      <h1>Fleet Management</h1>
      <p>
        Manage your fleet of 
        {{ auth.user.providerType === 'CAR' ? 'cars' : 'scooters' }}, 
        update vehicle status, and handle maintenance.
      </p>
    </header>

    <main class="management-card">
      <ProviderFleetManager :provider-id="auth.user.id" />
    </main>
  </div>
</template>

<style scoped>
.fleet-management {
  padding: 2rem clamp(1rem, 5vw, 4rem);
  max-width: 1400px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
}

.page-header {
  margin-bottom: 2rem;
}

.breadcrumb {
  font-size: 0.9rem;
  color: #64748b;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.breadcrumb a {
  color: #3b82f6;
  text-decoration: none;
}

h1 {
  font-size: 2.5rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 0.5rem;
}

p {
  font-size: 1.1rem;
  color: #64748b;
}

.management-card {
  background: white;
  border: 1px solid #f1f5f9;
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.04);
}

.fade-in { animation: fadeIn 0.6s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
