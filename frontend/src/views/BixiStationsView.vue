<script setup lang="ts">
/**
 * BixiStationsView — Citizen-facing live BIXI bike availability.
 * Fetches real-time data from the BIXI GBFS API via the SUMMS backend.
 */
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

interface BixiStation {
  stationId: string
  name: string
  address: string
  lat: number
  lon: number
  bikesAvailable: number
  docksAvailable: number
}

const stations = ref<BixiStation[]>([])
const totalStationsWithBikes = ref(0)
const syncedAt = ref<string | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')
const route = useRoute()

const filtered = computed(() => {
  const q = search.value.toLowerCase().trim()
  if (!q) return stations.value
  return stations.value.filter(s =>
    s.name.toLowerCase().includes(q) || s.address.toLowerCase().includes(q)
  )
})

onMounted(async () => {
  if (route.query.station) {
    search.value = route.query.station as string
  }
  await fetchStations()
})

async function fetchStations() {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/analytics/bixi-stations')
    stations.value = res.data.stations as BixiStation[]
    totalStationsWithBikes.value = res.data.totalStationsWithBikes ?? 0
    syncedAt.value = res.data.syncedAt ?? null
    if (res.data.error) error.value = res.data.error
  } catch (e: any) {
    error.value = 'Could not load BIXI data. Try again shortly.'
  } finally {
    loading.value = false
  }
}

function availabilityClass(bikes: number): string {
  if (bikes >= 5) return 'high'
  if (bikes >= 2) return 'med'
  return 'low'
}
</script>

<template>
  <div class="bixi-view">
    <header class="page-header">
      <div>
        <div class="provider-badge">🚲 BIXI Montréal</div>
        <h1>Live Bike Availability</h1>
        <p>Real-time bike counts at every active BIXI station in Montréal.</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="fetchStations">
        {{ loading ? 'Loading…' : '↻ Refresh' }}
      </button>
    </header>

    <div v-if="syncedAt" class="sync-bar">
      <span class="sync-dot"></span>
      Live data — last synced at {{ new Date(syncedAt).toLocaleTimeString() }} ·
      <strong>{{ totalStationsWithBikes }}</strong> stations with bikes available
    </div>

    <div class="search-bar">
      <input
        v-model="search"
        placeholder="Search by station name or address…"
        class="search-input"
        id="bixi-search"
      />
    </div>

    <div v-if="error" class="state-msg error">{{ error }}</div>
    <div v-else-if="loading && stations.length === 0" class="state-msg pulse">
      Fetching live BIXI data…
    </div>
    <div v-else-if="filtered.length === 0" class="state-msg">
      No stations match "{{ search }}". Try a different search.
    </div>

    <div v-else class="stations-grid">
      <article
        v-for="station in filtered"
        :key="station.stationId"
        class="station-card"
        :class="availabilityClass(station.bikesAvailable)"
      >
        <div class="station-header">
          <h3 class="station-name">{{ station.name }}</h3>
          <div class="bikes-badge" :class="availabilityClass(station.bikesAvailable)">
            {{ station.bikesAvailable }} 🚲
          </div>
        </div>

        <p v-if="station.address" class="station-address">📍 {{ station.address }}</p>

        <div class="station-stats">
          <div class="stat">
            <span class="stat-label">Bikes available</span>
            <strong class="stat-value bikes">{{ station.bikesAvailable }}</strong>
          </div>
          <div class="stat">
            <span class="stat-label">Open docks</span>
            <strong class="stat-value docks">{{ station.docksAvailable }}</strong>
          </div>
        </div>

        <RouterLink
          :to="`/mobility-map?lat=${station.lat}&lng=${station.lon}`"
          class="map-link"
        >
          View on Map →
        </RouterLink>
      </article>
    </div>
  </div>
</template>

<style scoped>
.bixi-view {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.provider-badge {
  display: inline-block;
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
  border-radius: 999px;
  padding: 0.2rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 700;
  margin-bottom: 0.4rem;
}

.page-header h1 {
  font-size: 2.2rem;
  color: #0f172a;
  margin: 0 0 0.35rem;
}

.page-header p {
  color: #64748b;
  margin: 0;
  font-size: 1rem;
}

.refresh-btn {
  padding: 0.65rem 1.2rem;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 8px;
  color: #166534;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.refresh-btn:hover:not(:disabled) { background: #dcfce7; }
.refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.sync-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.6rem 1rem;
}

.sync-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 6px #22c55e;
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 4px #22c55e; }
  50% { box-shadow: 0 0 12px #22c55e; }
}

.search-bar {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 0.85rem 1rem;
  font-size: 0.95rem;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15);
}

.stations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 1rem;
}

.station-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  transition: box-shadow 0.2s, transform 0.2s;
}

.station-card:hover {
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.station-card.high { border-left: 4px solid #22c55e; }
.station-card.med  { border-left: 4px solid #f59e0b; }
.station-card.low  { border-left: 4px solid #ef4444; }

.station-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.station-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.3;
  flex: 1;
}

.bikes-badge {
  padding: 0.3rem 0.65rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 800;
  white-space: nowrap;
}

.bikes-badge.high { background: #dcfce7; color: #166534; }
.bikes-badge.med  { background: #fef3c7; color: #92400e; }
.bikes-badge.low  { background: #fee2e2; color: #991b1b; }

.station-address {
  font-size: 0.82rem;
  color: #64748b;
  margin: 0;
}

.station-stats {
  display: flex;
  gap: 1rem;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.stat-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #94a3b8;
  font-weight: 700;
}

.stat-value {
  font-size: 1.4rem;
  font-weight: 800;
}

.stat-value.bikes { color: #16a34a; }
.stat-value.docks { color: #64748b; }

.map-link {
  font-size: 0.82rem;
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  margin-top: auto;
}

.map-link:hover { text-decoration: underline; }

.state-msg {
  text-align: center;
  padding: 3rem;
  color: #64748b;
  background: #fff;
  border-radius: 14px;
  border: 1px dashed #cbd5e1;
}

.state-msg.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.state-msg.pulse { animation: pulse 2s infinite; }

@keyframes pulse {
  0%, 100% { opacity: 1; } 50% { opacity: 0.5; }
}
</style>
