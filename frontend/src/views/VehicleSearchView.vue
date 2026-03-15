<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useRentalStore } from '../stores/rentals'
import { useVehicleStore, type Vehicle } from '../stores/vehicles'

const vehicleStore = useVehicleStore()
const auth = useAuthStore()
const rentalStore = useRentalStore()
const router = useRouter()

const selectedCity = ref('All')
const activeTab = ref<'SCOOTER' | 'CAR'>('SCOOTER')

onMounted(() => {
  if (auth.user?.preferredCity) {
    selectedCity.value = auth.user.preferredCity
  }
  handleSearch()
})

function handleSearch() {
  vehicleStore.searchVehicles(selectedCity.value, 'All')
}

// Group available vehicles by location exact zone
const groupedLocations = computed(() => {
  const groups: Record<string, { city: string, zone: string, type: 'SCOOTER' | 'CAR', vehicles: Vehicle[] }> = {}

  for (const v of vehicleStore.availableVehicles) {
    const key = `${v.locationCity}|${v.locationZone}|${v.type}`
    if (!groups[key]) {
      groups[key] = { city: v.locationCity, zone: v.locationZone, type: v.type, vehicles: [] }
    }
    groups[key].vehicles.push(v)
  }

  return Object.values(groups).sort((a, b) => {
    if (a.city !== b.city) return a.city.localeCompare(b.city)
    return a.zone.localeCompare(b.zone)
  })
})

const scooterDocks = computed(() => groupedLocations.value.filter(g => g.type === 'SCOOTER'))
const carZones = computed(() => groupedLocations.value.filter(g => g.type === 'CAR'))

const expandedGroups = ref(new Set<string>())
function toggleGroup(zone: string) {
  if (expandedGroups.value.has(zone)) {
    expandedGroups.value.delete(zone)
  } else {
    expandedGroups.value.add(zone)
  }
}

function isExpanded(zone: string) {
  return expandedGroups.value.has(zone)
}

function handleReserve(vehicleId: string) {
  if (!auth.user) return
  router.push(`/vehicles/${vehicleId}/reserve`)
}

function availabilityClass(count: number): string {
  if (count >= 4) return 'high'
  if (count >= 2) return 'med'
  return 'low'
}

function getEnergyLabel(vehicle: Vehicle) {
  if (vehicle.type === 'CAR') return `${Math.round(vehicle.fuelLevel)}% fuel`
  return `${Math.round(vehicle.batteryLevel)}% battery`
}
</script>

<template>
  <div class="search-view">
    <header class="page-header">
      <h1>Find Vehicles</h1>
      <p>Locate exact scooter docks and car rental zones near you.</p>
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

      <button class="refresh-btn" @click="handleSearch" :disabled="vehicleStore.loading">
        {{ vehicleStore.loading ? 'Scanning...' : 'Refresh Feed' }}
      </button>
    </div>

    <!-- Tabs for separating Scooters and Cars -->
    <div class="tabs-control">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'SCOOTER' }" 
        @click="activeTab = 'SCOOTER'"
      >
        🛴 Scooter Docks
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'CAR' }" 
        @click="activeTab = 'CAR'"
      >
        🚗 Car Rentals
      </button>
    </div>

    <div v-if="vehicleStore.loading && groupedLocations.length === 0" class="state-msg pulse">
      Loading exact locations...
    </div>

    <div v-else-if="vehicleStore.error" class="state-msg error">
      {{ vehicleStore.error }}
    </div>

    <div v-else class="locations-area">
      <div v-if="activeTab === 'SCOOTER'" class="locations-grid">
        <div v-if="scooterDocks.length === 0" class="state-msg">
          No scooter docks found in this area right now.
        </div>
        
        <article v-for="dock in scooterDocks" :key="dock.zone" class="loc-card">
          <div class="loc-summary" @click="toggleGroup(dock.zone)">
            <div class="loc-titles">
              <h3>{{ dock.zone }}</h3>
              <span class="loc-city">{{ dock.city }}</span>
            </div>
            
            <div class="loc-stats">
              <div class="availability-badge" :class="availabilityClass(dock.vehicles.length)">
                {{ dock.vehicles.length }} available
              </div>
              <button class="expand-icon-btn" :class="{ rotated: isExpanded(dock.zone) }">
                ▼
              </button>
            </div>
          </div>
          
          <transition name="slide">
            <div v-if="isExpanded(dock.zone)" class="loc-expanded">
              <div v-for="v in dock.vehicles" :key="v.id" class="individual-vehicle">
                <div class="iv-info">
                  <span class="iv-code">ID: {{ v.vehicleCode }}</span>
                  <span class="iv-energy" :class="v.batteryLevel < 30 ? 'low-energy' : ''">
                    🔋 {{ getEnergyLabel(v) }}
                  </span>
                </div>
                <button class="iv-reserve" @click="handleReserve(v.id)">Reserve</button>
              </div>
            </div>
          </transition>
        </article>
      </div>

      <div v-if="activeTab === 'CAR'" class="locations-grid">
        <div v-if="carZones.length === 0" class="state-msg">
          No car rentals found in this area right now.
        </div>
        
        <article v-for="zone in carZones" :key="zone.zone" class="loc-card">
          <div class="loc-summary" @click="toggleGroup(zone.zone)">
            <div class="loc-titles">
              <h3>{{ zone.zone }}</h3>
              <span class="loc-city">{{ zone.city }}</span>
            </div>
            
            <div class="loc-stats">
              <div class="availability-badge car-badge" :class="availabilityClass(zone.vehicles.length)">
                {{ zone.vehicles.length }} cars nearby
              </div>
              <button class="expand-icon-btn" :class="{ rotated: isExpanded(zone.zone) }">
                ▼
              </button>
            </div>
          </div>
          
          <transition name="slide">
            <div v-if="isExpanded(zone.zone)" class="loc-expanded">
              <div v-for="v in zone.vehicles" :key="v.id" class="individual-vehicle">
                <div class="iv-info">
                  <span class="iv-code">{{ v.model }} • Plate: {{ v.licensePlate }}</span>
                  <span class="iv-energy" :class="v.fuelLevel < 25 ? 'low-energy' : ''">
                    ⛽ {{ getEnergyLabel(v) }}
                  </span>
                </div>
                <button class="iv-reserve car-reserve" @click="handleReserve(v.id)">Reserve Car</button>
              </div>
            </div>
          </transition>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-view {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 2.2rem;
  color: #0f172a;
  margin: 0 0 0.5rem;
  letter-spacing: -0.5px;
}

.page-header p {
  color: #64748b;
  font-size: 1.05rem;
  margin: 0;
}

.filters-card {
  background: white;
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02), 0 10px 15px rgba(0, 0, 0, 0.03);
  display: flex;
  gap: 1.5rem;
  align-items: flex-end;
  margin-bottom: 2rem;
  border: 1px solid #e2e8f0;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 200px;
}

.filter-group label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-group select {
  padding: 0.75rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-size: 1rem;
  outline: none;
  background-color: #f8fafc;
  cursor: pointer;
  width: 100%;
}

.refresh-btn {
  padding: 0.75rem 1.5rem;
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  height: max-content;
  transition: background 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: #e2e8f0;
}

.tabs-control {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  background: #f1f5f9;
  padding: 0.5rem;
  border-radius: 12px;
}

.tab-btn {
  flex: 1;
  padding: 1rem;
  background: transparent;
  border: none;
  border-radius: 8px;
  font-size: 1.15rem;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: #e2e8f0;
}

.tab-btn.active {
  background: white;
  color: #0f172a;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

.locations-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.loc-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.loc-card:hover {
  box-shadow: 0 6px 15px rgba(0,0,0,0.05);
}

.loc-summary {
  padding: 1.25rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  background: white;
}

.loc-summary:hover {
  background: #f8fafc;
}

.loc-titles h3 {
  margin: 0 0 0.25rem;
  font-size: 1.25rem;
  color: #0f172a;
}

.loc-city {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
}

.loc-stats {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.availability-badge {
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  font-size: 0.95rem;
  font-weight: 700;
}

.availability-badge.high { background: #dcfce7; color: #166534; }
.availability-badge.med  { background: #fef3c7; color: #92400e; }
.availability-badge.low  { background: #fee2e2; color: #991b1b; }

.car-badge.high { background: #e0e7ff; color: #3730a3; }
.car-badge.med  { background: #fef3c7; color: #92400e; }
.car-badge.low  { background: #fee2e2; color: #991b1b; }

.expand-icon-btn {
  background: none;
  border: none;
  font-size: 0.9rem;
  color: #94a3b8;
  cursor: pointer;
  transition: transform 0.2s;
}

.expand-icon-btn.rotated {
  transform: rotate(180deg);
}

.loc-expanded {
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  padding: 1rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.individual-vehicle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.iv-info {
  display: flex;
  gap: 1.5rem;
  align-items: center;
}

.iv-code {
  font-family: monospace;
  font-size: 1rem;
  color: #334155;
  background: #f1f5f9;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.iv-energy {
  font-size: 0.95rem;
  color: #166534;
  font-weight: 600;
}

.iv-energy.low-energy {
  color: #dc2626;
}

.iv-reserve {
  padding: 0.6rem 1.5rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}

.iv-reserve:hover {
  background: #2563eb;
}

.car-reserve {
  background: #6366f1;
}

.car-reserve:hover {
  background: #4f46e5;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.25s ease-out;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

.state-msg {
  text-align: center;
  padding: 3rem;
  color: #64748b;
  font-size: 1.15rem;
  background: white;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.pulse {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
