<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useRentalStore } from '../stores/rentals'
import { useVehicleStore, type Vehicle } from '../stores/vehicles'

const vehicleStore = useVehicleStore()
const auth = useAuthStore()
const rentalStore = useRentalStore()
const router = useRouter()

const selectedCity = ref('All')
const selectedType = ref('All')

onMounted(() => {
  if (auth.user?.preferredCity) {
    selectedCity.value = auth.user.preferredCity
  }
  if (auth.user?.preferredMobilityType) {
    selectedType.value = auth.user.preferredMobilityType
  }
  handleSearch()
})

function handleSearch() {
  vehicleStore.searchVehicles(selectedCity.value, selectedType.value)
}

function getTypeIcon(type: string) {
  if (type === 'BIKE') return '🚲'
  if (type === 'SCOOTER') return '🛴'
  if (type === 'CAR') return '🚗'
  return '📍'
}

function getEnergyLabel(vehicle: Vehicle) {
  if (vehicle.type === 'CAR') return `${vehicle.fuelLevel}% fuel`
  return `${vehicle.batteryLevel}% battery`
}

async function handleReserve(vehicleId: string) {
  if (!auth.user) return

  try {
    await rentalStore.reserve(auth.user.id, vehicleId)
    router.push('/rentals')
  } catch (error) {
    window.alert('Failed to reserve. Another user may have just taken this vehicle.')
  }
}
</script>

<template>
  <div class="search-view">
    <header class="page-header">
      <h1>Vehicle Search</h1>
      <p>Find nearby vehicles available for your commute.</p>
    </header>

    <div class="filters-card">
      <div class="filter-group">
        <label>Location (City)</label>
        <select v-model="selectedCity" @change="handleSearch">
          <option value="All">All Cities</option>
          <option value="Montreal">Montreal</option>
          <option value="Laval">Laval</option>
        </select>
      </div>

      <div class="filter-group">
        <label>Vehicle Type</label>
        <select v-model="selectedType" @change="handleSearch">
          <option value="All">All Types</option>
          <option value="BIKE">Bike</option>
          <option value="SCOOTER">Scooter</option>
          <option value="CAR">Car</option>
        </select>
      </div>

      <button class="refresh-btn" @click="handleSearch">Refresh Feed</button>
    </div>

    <div v-if="vehicleStore.loading" class="state-msg pulse">
      Scanning the area for vehicles...
    </div>

    <div v-else-if="vehicleStore.error" class="state-msg error">
      {{ vehicleStore.error }}
    </div>

    <div v-else-if="vehicleStore.availableVehicles.length === 0" class="state-msg">
      No vehicles found matching your criteria. Try expanding the search!
    </div>

    <div v-else class="vehicle-grid">
      <div v-for="vehicle in vehicleStore.availableVehicles" :key="vehicle.id" class="vehicle-card">
        <div class="v-header">
          <span class="v-icon">{{ getTypeIcon(vehicle.type) }}</span>
          <span class="v-type">{{ vehicle.type }}</span>
          <span class="v-battery">{{ getEnergyLabel(vehicle) }}</span>
        </div>

        <div class="v-body">
          <p class="v-location"><strong>Location:</strong> {{ vehicle.locationCity }} / {{ vehicle.locationZone }}</p>
          <div class="price-tags">
            <span class="tag base">Unlock: ${{ vehicle.basePrice.toFixed(2) }}</span>
            <span class="tag rate">${{ vehicle.pricePerMinute.toFixed(2) }}/min</span>
          </div>
          <p v-if="vehicle.licensePlate" class="v-license"><strong>License:</strong> {{ vehicle.licensePlate }}</p>
        </div>

        <div class="v-footer">
          <button
            class="reserve-btn"
            :disabled="rentalStore.loading"
            @click="handleReserve(vehicle.id)"
          >
            Reserve Vehicle
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-view {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 2.5rem;
  color: #1a202c;
  margin: 0 0 0.5rem;
  letter-spacing: -1px;
}

.page-header p {
  color: #718096;
  font-size: 1.1rem;
  margin: 0;
}

.filters-card {
  background: white;
  padding: 1.5rem 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02), 0 10px 15px rgba(0, 0, 0, 0.03);
  display: flex;
  gap: 1.5rem;
  align-items: flex-end;
  margin-bottom: 2.5rem;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 200px;
  flex: 1;
}

.filter-group label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #4a5568;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-group select {
  padding: 0.85rem 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 1rem;
  outline: none;
  background-color: #f7fafc;
  cursor: pointer;
}

.refresh-btn {
  padding: 0.85rem 2rem;
  background: #edf2f7;
  color: #2d3748;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  height: max-content;
}

.state-msg {
  text-align: center;
  padding: 4rem;
  color: #718096;
  font-size: 1.25rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
}

.state-msg.error {
  color: #c53030;
  background: #fff5f5;
  border: 1px solid #fed7d7;
}

.pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.5;
  }
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 2rem;
}

.vehicle-card {
  background: white;
  border-radius: 16px;
  padding: 1.75rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02), 0 10px 15px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  border: 1px solid #edf2f7;
}

.v-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #edf2f7;
}

.v-icon {
  min-width: 2.4rem;
  min-height: 2.4rem;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: #ebf4ff;
  color: #2b6cb0;
  font-size: 0.8rem;
  font-weight: 800;
}

.v-type {
  font-weight: 800;
  color: #1a202c;
  flex: 1;
  font-size: 1.25rem;
  letter-spacing: -0.5px;
}

.v-battery {
  font-size: 0.85rem;
  color: #276749;
  font-weight: 700;
  background: #c6f6d5;
  padding: 0.4rem 0.75rem;
  border-radius: 20px;
}

.v-body {
  flex: 1;
}

.v-location {
  color: #4a5568;
  font-size: 1.05rem;
  margin-bottom: 1.25rem;
}

.price-tags {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.tag {
  padding: 0.4rem 0.8rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
}

.tag.base {
  background: #ebf8ff;
  color: #2b6cb0;
}

.tag.rate {
  background: #faf5ff;
  color: #553c9a;
}

.v-license {
  font-family: monospace;
  background: #edf2f7;
  padding: 0.5rem;
  border-radius: 6px;
  display: inline-block;
  font-size: 0.9rem;
  color: #1a202c;
}

.v-footer {
  margin-top: 1.5rem;
}

.reserve-btn {
  width: 100%;
  padding: 1rem;
  background: #3182ce;
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
}

.reserve-btn:disabled {
  background: #cbd5e0;
  color: #718096;
  cursor: not-allowed;
}
</style>
