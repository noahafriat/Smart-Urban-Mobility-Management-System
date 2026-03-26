<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

import { api } from '../api'

interface ParkingGarage {
  id: string
  name: string
  address: string
  latitude: number
  longitude: number
  totalSpaces: number
  availableSpaces: number
}

const garages = ref<ParkingGarage[]>([])
const loading = ref(true)
const error = ref('')
const route = useRoute()

const fetchGarages = async () => {
  try {
    const res = await api.get('/parking-garages')
    garages.value = res.data
  } catch (err: any) {
    error.value = err.message || 'Failed to load parking garages'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchGarages()
})

const getAvailabilityClass = (available: number, total: number) => {
  if (total === 0) return 'empty'
  const ratio = available / total
  if (ratio > 0.5) return 'high'
  if (ratio > 0.1) return 'medium'
  return 'low'
}
</script>

<template>
  <div class="parking-page">
    <header class="page-header">
      <div class="header-content">
        <h1>🅿️ Parking Spaces</h1>
        <p>Find available parking spots across the city</p>
      </div>
      <RouterLink to="/dashboard" class="back-btn">Back to Dashboard</RouterLink>
    </header>

    <div v-if="loading" class="loading-state">
      Loading parking data...
    </div>

    <div v-else-if="error" class="error-state">
      {{ error }}
      <button @click="fetchGarages" class="retry-btn">Retry</button>
    </div>

    <div v-else class="garages-grid">
      <div 
        v-for="garage in garages" 
        :key="garage.id" 
        class="garage-card"
        :class="{ 'highlighted-garage': garage.id === route.query.selectedId }"
      >
        <h3>{{ garage.name }}</h3>
        
        <div class="availability-meter">
          <div class="meter-text">
            <span class="count">{{ garage.availableSpaces }}</span>
            <span class="total">/ {{ garage.totalSpaces }} available</span>
          </div>
          <div class="meter-bar-bg">
            <div 
              class="meter-bar-fill" 
              :class="getAvailabilityClass(garage.availableSpaces, garage.totalSpaces)"
              :style="{ width: `${(garage.availableSpaces / garage.totalSpaces) * 100}%` }"
            ></div>
          </div>
        </div>
        
        <div class="location-info">
          📍 {{ garage.address || `${garage.latitude.toFixed(4)}, ${garage.longitude.toFixed(4)}` }}
        </div>
        <RouterLink 
          :to="`/mobility-map?lat=${garage.latitude}&lng=${garage.longitude}`" 
          class="map-link mt-2 block"
        >
          View on Map →
        </RouterLink>
      </div>
      
      <div v-if="garages.length === 0" class="empty-state">
        No parking spaces found.
      </div>
    </div>
  </div>
</template>

<style scoped>
.parking-page {
  padding: 2rem clamp(1rem, 2vw, 2.5rem);
  width: min(96vw, 1200px);
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2.5rem;
  background: white;
  padding: 1.5rem 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.page-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #1a202c;
  font-weight: 700;
}

.page-header p {
  margin: 0;
  color: #718096;
}

.back-btn {
  padding: 0.75rem 1.5rem;
  background: #edf2f7;
  color: #2d3748;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #e2e8f0;
}

.loading-state, .error-state, .empty-state {
  text-align: center;
  padding: 4rem;
  background: white;
  border-radius: 12px;
  color: #718096;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.error-state {
  color: #c53030;
  background: #fff5f5;
  border: 1px solid #fed7d7;
}

.retry-btn {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  background: #c53030;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #9b2c2c;
}

.garages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.garage-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s;
}

.garage-card:hover {
  transform: translateY(-4px);
}

.garage-card h3 {
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  color: #2d3748;
}

.availability-meter {
  margin-bottom: 1.5rem;
}

.meter-text {
  margin-bottom: 0.5rem;
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.count {
  font-size: 1.5rem;
  font-weight: 700;
  color: #2b6cb0;
}

.total {
  color: #718096;
  font-size: 0.9rem;
}

.meter-bar-bg {
  height: 8px;
  background: #edf2f7;
  border-radius: 4px;
  overflow: hidden;
}

.meter-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.meter-bar-fill.high { background: #38a169; }     /* Green */
.meter-bar-fill.medium { background: #d69e2e; }   /* Yellow */
.meter-bar-fill.low { background: #e53e3e; }      /* Red */
.meter-bar-fill.empty { background: #cbd5e0; }    /* Gray */

.location-info {
  font-size: 0.85rem;
  color: #a0aec0;
  padding-top: 1rem;
  border-top: 1px solid #edf2f7;
}

.map-link {
  font-size: 0.82rem;
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  display: inline-block;
  margin-top: 0.75rem;
}

.map-link:hover { text-decoration: underline; }

/* Highlight effect for deep-linked selection from map */
.highlighted-garage {
  border: 2px solid #3b82f6 !important;
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.4) !important;
  transform: translateY(-4px); /* Keep it lifted */
}
</style>
