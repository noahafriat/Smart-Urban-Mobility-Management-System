<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'

type TransitLegSummary = {
  line: string
  vehicle: string
  departureStop: string
  arrivalStop: string
  stops: number | null
  headsign: string
}

type RouteSummary = {
  duration: string
  distance: string
  startAddress: string
  endAddress: string
  departureTime: string
  arrivalTime: string
  steps: TransitLegSummary[]
}

declare global {
  interface Window {
    google?: any
    __summsGoogleMapsLoading?: Promise<any>
  }
}

const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined

const origin = ref('Concordia University, Montreal')
const destination = ref('Old Port of Montreal')
const mapElement = ref<HTMLElement | null>(null)
const mapError = ref('')
const isLoadingMap = ref(false)
const isRouting = ref(false)
const routeSummary = ref<RouteSummary | null>(null)

const quickDestinations = [
  'McGill University, Montreal',
  'Berri-UQAM Station, Montreal',
  'Montreal-Trudeau International Airport',
  'Gare Centrale, Montreal',
]

const canPlanRoute = computed(
  () => !!origin.value.trim() && !!destination.value.trim() && !!window.google,
)

let map: any = null
let directionsService: any = null
let directionsRenderer: any = null
let transitLayer: any = null

function formatTransitStep(step: any): TransitLegSummary | null {
  const details = step?.transit
  if (!details) return null

  const line =
    details.line?.short_name ||
    details.line?.name ||
    details.line?.vehicle?.name ||
    'Transit line'

  return {
    line,
    vehicle: details.line?.vehicle?.name || 'Transit',
    departureStop: details.departure_stop?.name || 'Starting stop',
    arrivalStop: details.arrival_stop?.name || 'Arrival stop',
    stops: typeof details.num_stops === 'number' ? details.num_stops : null,
    headsign: details.headsign || '',
  }
}

function buildRouteSummary(result: any): RouteSummary | null {
  const firstRoute = result?.routes?.[0]
  const firstLeg = firstRoute?.legs?.[0]
  if (!firstLeg) return null

  const transitSteps = (firstLeg.steps || [])
    .map((step: any) => formatTransitStep(step))
    .filter(Boolean) as TransitLegSummary[]

  return {
    duration: firstLeg.duration?.text || '—',
    distance: firstLeg.distance?.text || '—',
    startAddress: firstLeg.start_address || origin.value,
    endAddress: firstLeg.end_address || destination.value,
    departureTime: firstLeg.departure_time?.text || 'Now',
    arrivalTime: firstLeg.arrival_time?.text || '—',
    steps: transitSteps,
  }
}

function loadGoogleMapsScript() {
  if (window.google?.maps) {
    return Promise.resolve(window.google)
  }

  if (window.__summsGoogleMapsLoading) {
    return window.__summsGoogleMapsLoading
  }

  window.__summsGoogleMapsLoading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_API_KEY}&v=weekly`
    script.async = true
    script.defer = true
    script.onload = () => resolve(window.google)
    script.onerror = () => reject(new Error('Google Maps failed to load.'))
    document.head.appendChild(script)
  })

  return window.__summsGoogleMapsLoading
}

async function initializeMap() {
  if (!GOOGLE_MAPS_API_KEY) {
    mapError.value = 'Add VITE_GOOGLE_MAPS_API_KEY in frontend/.env.local to enable the in-app map.'
    return
  }

  if (!mapElement.value) return

  try {
    isLoadingMap.value = true
    mapError.value = ''

    await loadGoogleMapsScript()
    const google = window.google

    map = new google.maps.Map(mapElement.value, {
      center: { lat: 45.5017, lng: -73.5673 },
      zoom: 12,
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: true,
    })

    transitLayer = new google.maps.TransitLayer()
    transitLayer.setMap(map)

    directionsService = new google.maps.DirectionsService()
    directionsRenderer = new google.maps.DirectionsRenderer({
      map,
      suppressMarkers: false,
      polylineOptions: {
        strokeColor: '#7c3aed',
        strokeOpacity: 0.9,
        strokeWeight: 5,
      },
    })

    await nextTick()
    await planRoute()
  } catch (error) {
    console.error(error)
    mapError.value = 'Could not load Google Maps. Check your API key and enabled APIs.'
  } finally {
    isLoadingMap.value = false
  }
}

function planRoute() {
  if (!directionsService || !directionsRenderer || !window.google) return Promise.resolve()
  if (!origin.value.trim() || !destination.value.trim()) return Promise.resolve()

  isRouting.value = true
  mapError.value = ''

  return new Promise<void>((resolve) => {
    directionsService.route(
      {
        origin: origin.value,
        destination: destination.value,
        travelMode: window.google.maps.TravelMode.TRANSIT,
        provideRouteAlternatives: true,
        transitOptions: {
          departureTime: new Date(),
        },
      },
      (result: any, status: string) => {
        isRouting.value = false

        if (status === 'OK' && result) {
          directionsRenderer.setDirections(result)
          routeSummary.value = buildRouteSummary(result)
        } else {
          routeSummary.value = null
          mapError.value = `No transit route found (${status}). Try a more specific Montréal address.`
        }

        resolve()
      },
    )
  })
}

watch([origin, destination], () => {
  routeSummary.value = null
})

onMounted(async () => {
  await initializeMap()
})
</script>

<template>
  <div class="page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Citizen mobility tools</p>
        <h1>Public Transit Hub</h1>
        <p class="hero-text">Plan transit trips across Montréal.</p>
      </div>
    </section>

    <section class="planner-card">
      <div class="section-head">
        <h2>Transit Planner</h2>
      </div>

      <div class="planner-grid">
        <label class="field">
          <span>Starting point</span>
          <input v-model="origin" type="text" placeholder="Enter your starting point" />
        </label>

        <label class="field">
          <span>Destination</span>
          <input v-model="destination" type="text" placeholder="Enter your destination" />
        </label>
      </div>

      <div class="quick-destinations">
        <span class="quick-label">Popular destinations:</span>
        <button
          v-for="place in quickDestinations"
          :key="place"
          class="chip"
          @click="destination = place"
        >
          {{ place }}
        </button>
      </div>

      <div class="planner-actions">
        <button class="action-btn" :disabled="!canPlanRoute || isRouting" @click="planRoute">
          {{ isRouting ? 'Planning route...' : 'Plan Transit Route' }}
        </button>
      </div>

      <p v-if="!GOOGLE_MAPS_API_KEY" class="info-banner warning">
        Add your Google Maps key to <code>frontend/.env.local</code> to activate the live map.
      </p>
      <p v-else-if="isLoadingMap" class="info-banner">Loading interactive transit map…</p>
      <p v-if="mapError" class="info-banner error">{{ mapError }}</p>
    </section>

    <section class="content-grid">
      <article class="card map-card">
        <div class="section-head">
          <h2>Interactive Transit Map</h2>
        </div>

        <div ref="mapElement" class="map-canvas"></div>
      </article>

      <article class="card summary-card">
        <div class="section-head">
          <h2>Route Summary</h2>
        </div>

        <div v-if="routeSummary" class="summary-content">
          <div class="summary-stats">
            <div class="stat-box">
              <p class="stat-line"><span class="stat-label">Duration:</span> <strong>{{ routeSummary.duration }}</strong></p>
            </div>
            <div class="stat-box">
              <p class="stat-line"><span class="stat-label">Distance:</span> <strong>{{ routeSummary.distance }}</strong></p>
            </div>
            <div class="stat-box">
              <p class="stat-line"><span class="stat-label">Departure:</span> <strong>{{ routeSummary.departureTime }}</strong></p>
            </div>
            <div class="stat-box">
              <p class="stat-line"><span class="stat-label">Arrival:</span> <strong>{{ routeSummary.arrivalTime }}</strong></p>
            </div>
          </div>

          <div class="address-box">
            <div>
              <p class="address-line"><span class="stat-label">From:</span></p>
              <p>{{ routeSummary.startAddress }}</p>
            </div>
            <div>
              <p class="address-line"><span class="stat-label">To:</span></p>
              <p>{{ routeSummary.endAddress }}</p>
            </div>
          </div>

          <div v-if="routeSummary.steps.length" class="route-steps">
            <h3>Transit Legs</h3>
            <div
              v-for="(step, index) in routeSummary.steps"
              :key="`${step.line}-${index}`"
              class="step-card"
            >
              <div class="step-topline">
                <strong>{{ step.line }}</strong>
                <span>{{ step.vehicle }}</span>
              </div>
              <p>{{ step.departureStop }} → {{ step.arrivalStop }}</p>
              <p v-if="step.headsign"><strong>Direction:</strong> {{ step.headsign }}</p>
              <p v-if="step.stops !== null"><strong>Stops:</strong> {{ step.stops }}</p>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p>Trip details will appear here after you plan a route.</p>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.page {
  padding: 2rem clamp(1rem, 2vw, 2.5rem);
  width: min(96vw, 1280px);
  margin: 0 auto;
}

.hero-card,
.planner-card,
.card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding: 1.5rem;
}

.hero-card,
.planner-card {
  margin-bottom: 1.25rem;
}

.eyebrow {
  margin: 0 0 0.35rem 0;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.75rem;
  color: #7c3aed;
  font-weight: 800;
}

h1 {
  margin: 0;
  color: #1a202c;
  font-size: 2rem;
}

.hero-text,
.info-banner,
.empty-state p,
.address-box p,
.step-card p {
  color: #718096;
  line-height: 1.5;
}

.hero-text {
  max-width: 780px;
  margin-top: 0.75rem;
}

.section-head h2,
.route-steps h3 {
  margin-top: 0;
  color: #2d3748;
}

.planner-grid,
.content-grid,
.summary-stats {
  display: grid;
  gap: 1rem;
}

.planner-grid {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  margin-top: 1rem;
}

.content-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.8fr);
  align-items: start;
}

.summary-stats {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field span,
.quick-label,
.stat-label {
  font-size: 0.9rem;
  font-weight: 700;
  color: #4a5568;
}

.field input {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  padding: 0.85rem 0.95rem;
  font-size: 0.95rem;
}

.field input:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}

.quick-destinations {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 1rem;
  align-items: center;
}

.chip {
  border: 1px solid #e9d8fd;
  background: #faf5ff;
  color: #6b21a8;
  border-radius: 999px;
  padding: 0.5rem 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.planner-actions {
  margin-top: 1rem;
}

.action-btn {
  display: inline-block;
  border-radius: 10px;
  font-weight: 700;
  text-decoration: none;
  text-align: center;
  background: #7c3aed;
  color: white;
  border: none;
  padding: 0.85rem 1.15rem;
  cursor: pointer;
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.info-banner {
  margin-top: 1rem;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  background: #f8fafc;
}

.info-banner.warning {
  background: #fffaf0;
  color: #975a16;
  border: 1px solid #fbd38d;
}

.info-banner.error {
  background: #fff5f5;
  color: #c53030;
  border: 1px solid #feb2b2;
}

.map-canvas {
  width: 100%;
  height: 720px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #eef2ff, #f8fafc);
  border: 1px solid #edf2f7;
}

.summary-content {
  display: grid;
  gap: 1rem;
}

.stat-box,
.address-box,
.step-card {
  background: #f8fafc;
  border: 1px solid #edf2f7;
  border-radius: 12px;
  padding: 0.9rem;
}

.stat-line,
.address-line {
  margin: 0;
}

.address-box {
  display: grid;
  gap: 0.8rem;
}

.address-box p,
.step-card p {
  margin-bottom: 0;
}

.step-card + .step-card {
  margin-top: 0.75rem;
}

.step-topline {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  color: #2d3748;
}

.empty-state {
  padding: 1rem;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #dbe3ee;
}

code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
}

@media (max-width: 1080px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .map-canvas {
    height: 560px;
  }
}

@media (max-width: 720px) {
  .summary-stats {
    grid-template-columns: 1fr;
  }

  .map-canvas {
    height: 440px;
  }
}
</style>