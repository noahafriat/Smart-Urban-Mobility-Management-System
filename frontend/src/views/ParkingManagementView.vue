<script setup lang="ts">
import ParkingGarageManager from '../components/ParkingGarageManager.vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

// Security check: Only parking providers or city admins allowed
if (!auth.canManageParkingGarages) {
  router.push('/dashboard')
}
</script>

<template>
  <div class="parking-management fade-in" v-if="auth.user">
    <header class="page-header">
      <div class="breadcrumb">
        <RouterLink to="/dashboard">Dashboard</RouterLink> / 
        <span>{{ auth.isCityAdmin ? 'Municipal Parking Facilities' : 'Garage Operations' }}</span>
      </div>
      <h1>{{ auth.isCityAdmin ? 'Municipal Parking' : 'Parking Management' }}</h1>
      <p>
        {{ 
          auth.isCityAdmin 
            ? 'Add and edit city-owned garages, update capacity, and monitor live availability.' 
            : 'Manage your private garage locations, set capacity, and track occupancy.' 
        }}
      </p>
    </header>

    <main class="management-card">
      <ParkingGarageManager :provider-id="auth.user.id" />
    </main>
  </div>
</template>

<style scoped>
.parking-management {
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
