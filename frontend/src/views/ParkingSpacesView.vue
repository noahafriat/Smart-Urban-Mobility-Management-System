<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

interface ParkingGarage {
  id: string
  providerId?: string
  name: string
  address: string
  latitude: number
  longitude: number
  totalSpaces: number
  availableSpaces: number
}

interface EnrichedReservation {
  id: string
  garageId: string
  garageName: string
  spots: number
  status: string
  startTime: string
  endTime: string
}

const CITY_INFRA = '__CITY__'

const auth = useAuthStore()
const garages = ref<ParkingGarage[]>([])
const loading = ref(true)
const error = ref('')
const route = useRoute()
const actionMsg = ref('')
const spotsToBook = ref<Record<string, number>>({})

const myReservations = ref<EnrichedReservation[]>([])
const resLoading = ref(false)

const activeReservations = computed(() =>
  myReservations.value.filter((r) => r.status === 'ACTIVE'),
)

function labelForGarage(g: ParkingGarage) {
  if (!g.providerId || g.providerId === CITY_INFRA) return 'City facility'
  return 'Partner garage'
}

async function fetchGarages() {
  try {
    const res = await api.get<ParkingGarage[]>('/parking-garages')
    garages.value = res.data
    for (const g of res.data) {
      if (spotsToBook.value[g.id] == null) {
        spotsToBook.value[g.id] = 1
      }
    }
  } catch (err: any) {
    error.value = err.response?.data?.error ?? err.message ?? 'Failed to load parking garages'
  } finally {
    loading.value = false
  }
}

async function fetchMyReservations() {
  if (!auth.isCitizen || !auth.user?.id) {
    myReservations.value = []
    return
  }
  resLoading.value = true
  try {
    const res = await api.get<EnrichedReservation[]>(`/parking-reservations/user/${auth.user.id}`)
    myReservations.value = res.data
  } catch {
    myReservations.value = []
  } finally {
    resLoading.value = false
  }
}

onMounted(() => {
  void fetchGarages()
  void fetchMyReservations()
})

async function refreshAll() {
  loading.value = true
  error.value = ''
  await fetchGarages()
  await fetchMyReservations()
  loading.value = false
}

async function reserve(g: ParkingGarage) {
  if (!auth.user?.id) return
  actionMsg.value = ''
  const spots = Math.max(1, Math.floor(Number(spotsToBook.value[g.id]) || 1))
  try {
    await api.post('/parking-reservations', {
      userId: auth.user.id,
      garageId: g.id,
      spots,
    })
    actionMsg.value = `Reserved ${spots} spot(s) at ${g.name}.`
    await fetchGarages()
    await fetchMyReservations()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Reservation failed')
  }
}

async function completeReservation(r: EnrichedReservation) {
  if (!auth.user?.id) return
  try {
    await api.post(`/parking-reservations/${r.id}/complete`, { userId: auth.user.id })
    await fetchGarages()
    await fetchMyReservations()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Could not end stay')
  }
}

async function cancelReservation(r: EnrichedReservation) {
  if (!auth.user?.id) return
  try {
    await api.post(`/parking-reservations/${r.id}/cancel`, { userId: auth.user.id })
    await fetchGarages()
    await fetchMyReservations()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Could not cancel')
  }
}

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
        <p>
          {{ auth.isCitizen ? 'Reserve spots and track your active parking.' : 'Live capacity at every garage (same data citizens see).' }}
        </p>
      </div>
      <div class="header-actions">
        <button type="button" class="refresh-header-btn" :disabled="loading" @click="refreshAll">Refresh</button>
        <RouterLink to="/dashboard" class="back-btn">Dashboard</RouterLink>
      </div>
    </header>

    <p v-if="actionMsg" class="action-banner">{{ actionMsg }}</p>

    <section v-if="auth.isCitizen" class="my-parking panel">
      <h2>Your parking</h2>
      <p v-if="resLoading" class="muted">Loading reservations…</p>
      <p v-else-if="activeReservations.length === 0" class="muted">No active parking. Reserve a spot below.</p>
      <ul v-else class="res-list">
        <li v-for="r in activeReservations" :key="r.id" class="res-item">
          <div>
            <strong>{{ r.garageName }}</strong>
            <span class="muted">{{ r.spots }} spot(s) · since {{ r.startTime }}</span>
          </div>
          <div class="res-btns">
            <button type="button" class="btn-done" @click="completeReservation(r)">End stay</button>
            <button type="button" class="btn-cancel" @click="cancelReservation(r)">Cancel</button>
          </div>
        </li>
      </ul>
    </section>

    <div v-if="loading" class="loading-state">Loading parking data...</div>

    <div v-else-if="error" class="error-state">
      {{ error }}
      <button class="retry-btn" @click="refreshAll">Retry</button>
    </div>

    <div v-else class="garages-grid">
      <div
        v-for="garage in garages"
        :key="garage.id"
        class="garage-card"
        :class="{ 'highlighted-garage': garage.id === route.query.selectedId }"
      >
        <div class="card-top">
          <h3>{{ garage.name }}</h3>
          <span class="source-pill">{{ labelForGarage(garage) }}</span>
        </div>

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

        <div v-if="auth.isCitizen" class="reserve-row">
          <label>
            Spots
            <input
              v-model.number="spotsToBook[garage.id]"
              type="number"
              min="1"
              :max="Math.max(1, garage.availableSpaces)"
              class="spots-input"
            />
          </label>
          <button
            type="button"
            class="reserve-btn"
            :disabled="garage.availableSpaces < 1"
            @click="reserve(garage)"
          >
            Reserve
          </button>
        </div>

        <RouterLink
          :to="`/mobility-map?lat=${garage.latitude}&lng=${garage.longitude}`"
          class="map-link mt-2 block"
        >
          View on Map →
        </RouterLink>
      </div>

      <div v-if="garages.length === 0" class="empty-state">No parking spaces found.</div>
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
  margin-bottom: 1.5rem;
  background: white;
  padding: 1.5rem 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  gap: 1rem;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.refresh-header-btn {
  padding: 0.65rem 1.2rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-weight: 600;
  cursor: pointer;
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

.action-banner {
  background: #ecfdf5;
  border: 1px solid #6ee7b7;
  color: #065f46;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  margin-bottom: 1rem;
}

.panel {
  background: white;
  border-radius: 12px;
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.panel h2 {
  margin: 0 0 0.75rem;
  font-size: 1.15rem;
}

.muted {
  color: #64748b;
  font-size: 0.9rem;
}

.res-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.res-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f5f9;
}

.res-item:last-child {
  border-bottom: none;
}

.res-btns {
  display: flex;
  gap: 0.5rem;
}

.btn-done {
  padding: 0.45rem 0.85rem;
  border-radius: 8px;
  border: none;
  background: #0d9488;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel {
  padding: 0.45rem 0.85rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: white;
  font-weight: 600;
  cursor: pointer;
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

.loading-state,
.error-state,
.empty-state {
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

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.garage-card h3 {
  margin: 0;
  font-size: 1.25rem;
  color: #2d3748;
}

.source-pill {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  background: #e0e7ff;
  color: #3730a3;
  white-space: nowrap;
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

.meter-bar-fill.high {
  background: #38a169;
}
.meter-bar-fill.medium {
  background: #d69e2e;
}
.meter-bar-fill.low {
  background: #e53e3e;
}
.meter-bar-fill.empty {
  background: #cbd5e0;
}

.location-info {
  font-size: 0.85rem;
  color: #a0aec0;
  padding-top: 1rem;
  border-top: 1px solid #edf2f7;
}

.reserve-row {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.reserve-row label {
  display: flex;
  flex-direction: column;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  gap: 0.25rem;
}

.spots-input {
  width: 4rem;
  padding: 0.4rem 0.5rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.reserve-btn {
  padding: 0.5rem 1.1rem;
  border-radius: 8px;
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 700;
  cursor: pointer;
}

.reserve-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.map-link {
  font-size: 0.82rem;
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  display: inline-block;
  margin-top: 0.75rem;
}

.map-link:hover {
  text-decoration: underline;
}

.highlighted-garage {
  border: 2px solid #3b82f6 !important;
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.4) !important;
  transform: translateY(-4px);
}
</style>
