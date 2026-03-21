<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from '../api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { Vehicle } from '../stores/vehicles'

interface BixiStation {
  stationId: string
  name: string
  address: string
  lat: number
  lon: number
  bikesAvailable: number
  docksAvailable: number
}

interface ScooterDock {
  id: string
  city: string
  zone: string
  lat: number
  lon: number
  parkedCount: number
  avgBattery: number
}

const mapRoot = ref<HTMLElement | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const vehicles = ref<Vehicle[]>([])
const stations = ref<BixiStation[]>([])

const showCars = ref(true)
const showScooters = ref(true)
const showBixi = ref(true)

let map: L.Map | null = null
let carsLayer: L.LayerGroup | null = null
let scootersLayer: L.LayerGroup | null = null
let bixiLayer: L.LayerGroup | null = null
const focusZoomLevel = 16

const cars = computed(() => vehicles.value.filter(v => v.type === 'CAR'))
const scooters = computed(() => vehicles.value.filter(v => v.type === 'SCOOTER'))
const scooterDocks = computed<ScooterDock[]>(() => {
  const grouped = new Map<string, ScooterDock>()
  for (const scooter of scooters.value) {
    const key = `${scooter.locationCity}|${scooter.locationZone}`
    const existing = grouped.get(key)
    if (!existing) {
      grouped.set(key, {
        id: key,
        city: scooter.locationCity,
        zone: scooter.locationZone,
        lat: scooter.latitude,
        lon: scooter.longitude,
        parkedCount: 1,
        avgBattery: scooter.batteryLevel,
      })
      continue
    }
    existing.parkedCount += 1
    existing.avgBattery += scooter.batteryLevel
  }
  return Array.from(grouped.values()).map(dock => ({
    ...dock,
    avgBattery: dock.avgBattery / dock.parkedCount,
  }))
})

onMounted(async () => {
  await fetchData()
  await nextTick()
  initMap()
  renderLayers()
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const [vehicleRes, bixiRes] = await Promise.all([
      api.get('/vehicles/search'),
      api.get('/analytics/bixi-stations'),
    ])
    vehicles.value = vehicleRes.data as Vehicle[]
    stations.value = (bixiRes.data?.stations ?? []) as BixiStation[]
  } catch {
    error.value = 'Could not load mobility map data.'
  } finally {
    loading.value = false
  }
}

function initMap() {
  if (!mapRoot.value || map) return
  map = L.map(mapRoot.value, {
    center: [45.5019, -73.5674],
    zoom: 12,
  })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)
}

function renderLayers() {
  if (!map) return
  if (carsLayer) map.removeLayer(carsLayer)
  if (scootersLayer) map.removeLayer(scootersLayer)
  if (bixiLayer) map.removeLayer(bixiLayer)

  carsLayer = L.layerGroup()
  scootersLayer = L.layerGroup()
  bixiLayer = L.layerGroup()

  for (const car of cars.value) {
    const marker = L.circleMarker([car.latitude, car.longitude], {
      radius: 7,
      color: '#1d4ed8',
      fillColor: '#60a5fa',
      fillOpacity: 0.9,
      weight: 2,
    }).bindPopup(
      `<strong>${car.vehicleCode}</strong><br>${car.model ?? 'Car'}<br>${car.locationCity} / ${car.locationZone}`
    )
    marker.on('click', () => focusOnMarker(car.latitude, car.longitude))
    carsLayer.addLayer(marker)
  }

  for (const dock of scooterDocks.value) {
    const marker = L.circleMarker([dock.lat, dock.lon], {
      radius: 9,
      color: '#7c3aed',
      fillColor: '#a78bfa',
      fillOpacity: 0.9,
      weight: 2,
    }).bindPopup(
      `<strong>${dock.zone}</strong><br>${dock.city}<br>Scooters parked: ${dock.parkedCount}<br>Avg battery: ${Math.round(dock.avgBattery)}%`
    )
    marker.on('click', () => focusOnMarker(dock.lat, dock.lon))
    scootersLayer.addLayer(marker)
  }

  for (const station of stations.value) {
    const marker = L.circleMarker([station.lat, station.lon], {
      radius: 8,
      color: '#15803d',
      fillColor: '#4ade80',
      fillOpacity: 0.9,
      weight: 2,
    }).bindPopup(
      `<strong>${station.name}</strong><br>Bikes: ${station.bikesAvailable}<br>Docks: ${station.docksAvailable}`
    )
    marker.on('click', () => focusOnMarker(station.lat, station.lon))
    bixiLayer.addLayer(marker)
  }

  if (showCars.value) carsLayer.addTo(map)
  if (showScooters.value) scootersLayer.addTo(map)
  if (showBixi.value) bixiLayer.addTo(map)
}

function focusOnMarker(lat: number, lon: number) {
  if (!map) return
  map.setView([lat, lon], focusZoomLevel, { animate: true })
}

function toggleLayer() {
  renderLayers()
}

async function refreshMapData() {
  await fetchData()
  renderLayers()
}
</script>

<template>
  <div class="mobility-map-view">
    <header class="header">
      <div>
        <h1>Montreal Mobility Map</h1>
        <p>Cars, scooters, and BIXI stations in one live view.</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="refreshMapData">
        {{ loading ? 'Loading…' : 'Refresh' }}
      </button>
    </header>

    <div class="controls">
      <label><input v-model="showCars" type="checkbox" @change="toggleLayer" /> Cars ({{ cars.length }})</label>
      <label><input v-model="showScooters" type="checkbox" @change="toggleLayer" /> Scooter docks ({{ scooterDocks.length }})</label>
      <label><input v-model="showBixi" type="checkbox" @change="toggleLayer" /> BIXI stations ({{ stations.length }})</label>
    </div>

    <div v-if="error" class="state error">{{ error }}</div>
    <div v-else-if="loading" class="state">Loading map data…</div>
    <div ref="mapRoot" class="map-root" />
  </div>
</template>

<style scoped>
.mobility-map-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  display: grid;
  gap: 1rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.header h1 {
  margin: 0 0 0.35rem;
  color: #0f172a;
}

.header p {
  margin: 0;
  color: #64748b;
}

.refresh-btn {
  padding: 0.6rem 1rem;
  border: 1px solid #99f6e4;
  border-radius: 10px;
  background: #f0fdfa;
  color: #0f766e;
  font-weight: 700;
  cursor: pointer;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.85rem 1rem;
}

.controls label {
  color: #334155;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.map-root {
  width: 100%;
  height: 65vh;
  min-height: 500px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

.state {
  text-align: center;
  padding: 1rem;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
}

.state.error {
  background: #fff1f2;
  color: #be123c;
  border: 1px solid #fecdd3;
}
</style>
