<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { Vehicle } from '../stores/vehicles'
import { useAuthStore } from '../stores/auth'

/** Municipal garages: missing id, legacy `__CITY__`, or seeded city admin id. */
function isMunicipalGarage(providerId?: string | null): boolean {
  return !providerId || providerId === '__CITY__' || providerId === 'city-admin-id'
}

interface BixiStation {
  stationId: string
  name: string
  address: string
  lat: number
  lon: number
  bikesAvailable: number
  docksAvailable: number
}

interface ParkingGarage {
  id: string
  providerId?: string
  name: string
  address?: string
  latitude: number
  longitude: number
  totalSpaces: number
  availableSpaces: number
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
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref<string | null>(null)
const vehicles = ref<Vehicle[]>([])
const stations = ref<BixiStation[]>([])
const garages = ref<ParkingGarage[]>([])
const auth = useAuthStore()

const showCars = ref(true)
const showScooters = ref(true)
const showBixi = ref(true)
const showGarages = ref(true)
/** Parking provider: toggle own vs everyone else’s garages (separate layers & colours). */
const showMyParkingGarages = ref(true)
const showOtherParkingGarages = ref(true)

let map: L.Map | null = null
let carsLayer: L.LayerGroup | null = null
let scootersLayer: L.LayerGroup | null = null
let bixiLayer: L.LayerGroup | null = null
let garageLayer: L.LayerGroup | null = null
let myParkingLayer: L.LayerGroup | null = null
let otherParkingLayer: L.LayerGroup | null = null
const focusZoomLevel = 16

/** City admin or parking provider: split map into “mine” vs everyone else’s garages. */
const garagePortfolioViewer = computed(
  () =>
    !!auth.user?.id &&
    (auth.user?.role === 'CITY_ADMIN' ||
      (auth.user?.providerType === 'PARKING' && auth.user?.role === 'MOBILITY_PROVIDER')),
)

const myParkingGarages = computed(() => {
  const uid = auth.user?.id
  if (!garagePortfolioViewer.value || !uid) return []
  return garages.value.filter((g) => g.providerId === uid)
})

const otherParkingGarages = computed(() => {
  const uid = auth.user?.id
  if (!garagePortfolioViewer.value || !uid) return []
  return garages.value.filter((g) => g.providerId !== uid)
})

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
  // Lock layers for providers
  if (auth.isProvider) {
    if (auth.user?.providerType === 'CAR') {
      showScooters.value = false
      showBixi.value = false
      showGarages.value = false
    } else if (auth.user?.providerType === 'SCOOTER') {
      showCars.value = false
      showGarages.value = false
      showBixi.value = false
    } else if (auth.user?.providerType === 'PARKING') {
      showCars.value = false
      showScooters.value = false
      showBixi.value = false
      showMyParkingGarages.value = true
      showOtherParkingGarages.value = true
    }
  } else if (auth.user?.role === 'CITY_ADMIN') {
    showCars.value = false
    showScooters.value = false
    showBixi.value = false
    showMyParkingGarages.value = true
    showOtherParkingGarages.value = true
  }

  await fetchData()
  await nextTick()
  initMap()
  renderLayers()
  
  const qLat = parseFloat(route.query.lat as string)
  const qLng = parseFloat(route.query.lng as string)
  if (!isNaN(qLat) && !isNaN(qLng)) {
    focusOnMarker(qLat, qLng)
    // Extra zoom if opened from specific node
    map?.setView([qLat, qLng], 18, { animate: true })
  }
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
    const [vehicleRes, bixiRes, garageRes] = await Promise.all([
      api.get('/vehicles/search'),
      api.get('/analytics/bixi-stations'),
      api.get('/parking-garages'),
    ])
    vehicles.value = vehicleRes.data as Vehicle[]
    stations.value = (bixiRes.data?.stations ?? []) as BixiStation[]
    garages.value = garageRes.data as ParkingGarage[]
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

  // SPA Navigation for Leaflet Popups
  mapRoot.value?.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (target.classList.contains('popup-btn')) {
      const path = target.getAttribute('data-path')
      if (path) router.push(path)
    }
  })
}

function renderLayers() {
  if (!map) return
  if (carsLayer) map.removeLayer(carsLayer)
  if (scootersLayer) map.removeLayer(scootersLayer)
  if (bixiLayer) map.removeLayer(bixiLayer)
  if (garageLayer) map.removeLayer(garageLayer)
  if (myParkingLayer) map.removeLayer(myParkingLayer)
  if (otherParkingLayer) map.removeLayer(otherParkingLayer)

  const qLat = parseFloat(route.query.lat as string)
  const qLng = parseFloat(route.query.lng as string)
  let targetMarker: L.CircleMarker | null = null

  carsLayer = L.layerGroup()
  scootersLayer = L.layerGroup()
  bixiLayer = L.layerGroup()
  garageLayer = L.layerGroup()
  myParkingLayer = L.layerGroup()
  otherParkingLayer = L.layerGroup()

  for (const car of cars.value) {
    const marker = L.circleMarker([car.latitude, car.longitude], {
      radius: 7,
      color: '#1d4ed8',
      fillColor: '#60a5fa',
      fillOpacity: 0.9,
      weight: 2,
    }).bindPopup(
      `<strong>${car.vehicleCode}</strong><br>${car.model ?? 'Car'}<br>${car.locationCity} / ${car.locationZone}<br><br><button data-path="/vehicles?selectedId=${car.id}&tab=CAR&city=${encodeURIComponent(car.locationCity)}" class="popup-btn">View Details →</button>`
    )
    marker.on('click', () => focusOnMarker(car.latitude, car.longitude))
    carsLayer.addLayer(marker)
    if (car.latitude === qLat && car.longitude === qLng && showCars.value) targetMarker = marker
  }

  for (const dock of scooterDocks.value) {
    const marker = L.circleMarker([dock.lat, dock.lon], {
      radius: 9,
      color: '#7c3aed',
      fillColor: '#a78bfa',
      fillOpacity: 0.9,
      weight: 2,
    }).bindPopup(
      `<strong>${dock.zone}</strong><br>${dock.city}<br>Scooters parked: ${dock.parkedCount}<br>Avg battery: ${Math.round(dock.avgBattery)}%<br><br><button data-path="/vehicles?selectedZone=${encodeURIComponent(dock.zone)}&tab=SCOOTER&city=${encodeURIComponent(dock.city)}" class="popup-btn">View Details →</button>`
    )
    marker.on('click', () => focusOnMarker(dock.lat, dock.lon))
    scootersLayer.addLayer(marker)
    if (dock.lat === qLat && dock.lon === qLng && showScooters.value) targetMarker = marker
  }

  for (const station of stations.value) {
    const marker = L.circleMarker([station.lat, station.lon], {
      radius: 8,
      color: '#15803d',
      fillColor: '#4ade80',
      fillOpacity: 0.9,
      weight: 2,
    }).bindPopup(
      `<strong>${station.name}</strong><br>Bikes: ${station.bikesAvailable}<br>Docks: ${station.docksAvailable}<br><br><button data-path="/bixi?station=${encodeURIComponent(station.name)}" class="popup-btn">View Details →</button>`
    )
    marker.on('click', () => focusOnMarker(station.lat, station.lon))
    bixiLayer.addLayer(marker)
    if (station.lat === qLat && station.lon === qLng && showBixi.value) targetMarker = marker
  }

  function bindGaragePopup(garage: ParkingGarage, label: string) {
    const src = isMunicipalGarage(garage.providerId) ? 'City' : 'Partner'
    const addr = garage.address ? `<br><small>${garage.address}</small>` : ''
    return `<strong>${garage.name}</strong> <span style="opacity:.75">(${label} · ${src})</span>${addr}<br>Available: ${garage.availableSpaces} / ${garage.totalSpaces}<br><br><button data-path="/parking-spaces?selectedId=${garage.id}" class="popup-btn">View details →</button>`
  }

  if (garagePortfolioViewer.value) {
    for (const garage of myParkingGarages.value) {
      const marker = L.circleMarker([garage.latitude, garage.longitude], {
        radius: 9,
        color: '#0369a1',
        fillColor: '#38bdf8',
        fillOpacity: 0.95,
        weight: 2,
      }).bindPopup(bindGaragePopup(garage, 'Your garage'))
      marker.on('click', () => focusOnMarker(garage.latitude, garage.longitude))
      myParkingLayer!.addLayer(marker)
      if (
        garage.latitude === qLat &&
        garage.longitude === qLng &&
        showMyParkingGarages.value
      ) {
        targetMarker = marker
      }
    }
    for (const garage of otherParkingGarages.value) {
      const marker = L.circleMarker([garage.latitude, garage.longitude], {
        radius: 8,
        color: '#9a3412',
        fillColor: '#fb923c',
        fillOpacity: 0.9,
        weight: 2,
      }).bindPopup(bindGaragePopup(garage, 'Other operator'))
      marker.on('click', () => focusOnMarker(garage.latitude, garage.longitude))
      otherParkingLayer!.addLayer(marker)
      if (
        garage.latitude === qLat &&
        garage.longitude === qLng &&
        showOtherParkingGarages.value
      ) {
        targetMarker = marker
      }
    }
  } else {
    for (const garage of garages.value) {
      const src = isMunicipalGarage(garage.providerId) ? 'City' : 'Partner'
      const addr = garage.address ? `<br><small>${garage.address}</small>` : ''
      const marker = L.circleMarker([garage.latitude, garage.longitude], {
        radius: 8,
        color: '#b91c1c',
        fillColor: '#f87171',
        fillOpacity: 0.9,
        weight: 2,
      }).bindPopup(
        `<strong>${garage.name}</strong> <span style="opacity:.75">(${src})</span>${addr}<br>Available: ${garage.availableSpaces} / ${garage.totalSpaces}<br><br><button data-path="/parking-spaces?selectedId=${garage.id}" class="popup-btn">View details →</button>`
      )
      marker.on('click', () => focusOnMarker(garage.latitude, garage.longitude))
      garageLayer.addLayer(marker)
      if (garage.latitude === qLat && garage.longitude === qLng && showGarages.value) {
        targetMarker = marker
      }
    }
  }

  if (showCars.value) carsLayer.addTo(map)
  if (showScooters.value) scootersLayer.addTo(map)
  if (showBixi.value) bixiLayer.addTo(map)
  if (garagePortfolioViewer.value) {
    if (showMyParkingGarages.value) myParkingLayer.addTo(map)
    if (showOtherParkingGarages.value) otherParkingLayer.addTo(map)
  } else if (showGarages.value) {
    garageLayer.addTo(map)
  }

  if (targetMarker) {
    setTimeout(() => {
      targetMarker?.openPopup()
    }, 450)
  }
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
        <h1>Mobility Map</h1>
        <p>
          <template v-if="garagePortfolioViewer">
            Garages you manage are cyan; other operators are orange. Toggle each group below.
          </template>
          <template v-else>Cars, scooters, BIXI, and parking in one live view.</template>
        </p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="refreshMapData">
        {{ loading ? 'Loading…' : 'Refresh' }}
      </button>
    </header>

    <div class="controls">
      <label v-if="!auth.isProvider || auth.user?.providerType === 'CAR'"><input v-model="showCars" type="checkbox" @change="toggleLayer" /> <span class="dot car"></span> Cars ({{ cars.length }})</label>
      <label v-if="!auth.isProvider || auth.user?.providerType === 'SCOOTER'"><input v-model="showScooters" type="checkbox" @change="toggleLayer" /> <span class="dot scooter"></span> Scooter docks ({{ scooterDocks.length }})</label>
      <label v-if="!auth.isProvider"><input v-model="showBixi" type="checkbox" @change="toggleLayer" /> <span class="dot bixi"></span> BIXI stations ({{ stations.length }})</label>
      <template v-if="garagePortfolioViewer">
        <label>
          <input v-model="showMyParkingGarages" type="checkbox" @change="toggleLayer" />
          <span class="dot garage-mine"></span> My garages ({{ myParkingGarages.length }})
        </label>
        <label>
          <input v-model="showOtherParkingGarages" type="checkbox" @change="toggleLayer" />
          <span class="dot garage-other"></span> City &amp; other providers ({{ otherParkingGarages.length }})
        </label>
      </template>
      <label v-else-if="!auth.isProvider"><input v-model="showGarages" type="checkbox" @change="toggleLayer" /> <span class="dot garage"></span> Parking garages ({{ garages.length }})</label>
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
  font-size: 0.95rem;
  display: flex;
  align-items: center;
  font-weight: 500;
  gap: 0.5rem;
}

.dot { height: 12px; width: 12px; border-radius: 50%; display: inline-block; border: 2px solid; }
.dot.car { background: #60a5fa; border-color: #1d4ed8; }
.dot.scooter { background: #a78bfa; border-color: #7c3aed; }
.dot.bixi { background: #4ade80; border-color: #15803d; }
.dot.garage { background: #f87171; border-color: #b91c1c; }
.dot.garage-mine { background: #38bdf8; border-color: #0369a1; }
.dot.garage-other { background: #fb923c; border-color: #9a3412; }

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

:deep(.popup-btn) {
  display: inline-block;
  background: none;
  border: none;
  padding: 0;
  color: #3b82f6;
  text-decoration: none;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  font-family: inherit;
}

:deep(.popup-btn:hover) {
  text-decoration: underline;
}
</style>
