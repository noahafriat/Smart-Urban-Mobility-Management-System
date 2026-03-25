<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'

/* ------------------------------------------------------------------ */
/*  Types                                                               */
/* ------------------------------------------------------------------ */

type StepKind = 'WALKING' | 'TRANSIT'

type DirectionStep = {
  kind: StepKind
  instruction: string       // human-readable HTML instruction
  distance: string
  duration: string
  // transit-only
  line?: string
  vehicle?: string
  vehicleIcon?: string      // Google transit icon URL
  color?: string            // line colour hex
  textColor?: string
  departureStop?: string
  arrivalStop?: string
  departureTime?: string
  arrivalTime?: string
  numStops?: number
  headsign?: string
}

type Route = {
  index: number
  duration: string
  distance: string
  startAddress: string
  endAddress: string
  departureTime: string
  arrivalTime: string
  steps: DirectionStep[]
  summary: string
}

declare global {
  interface Window {
    google?: any
    __summsGoogleMapsLoading?: Promise<any>
  }
}

/* ------------------------------------------------------------------ */
/*  State                                                               */
/* ------------------------------------------------------------------ */

const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined

const origin      = ref('')
const destination = ref('')
const mapElement  = ref<HTMLElement | null>(null)
const mapError    = ref('')
const isLoadingMap = ref(false)
const isRouting   = ref(false)
const routes      = ref<Route[]>([])
const activeRouteIndex = ref(0)

const canPlanRoute = computed(
  () => !!origin.value.trim() && !!destination.value.trim() && !!window.google,
)

const activeRoute = computed(() => routes.value[activeRouteIndex.value] ?? null)

/* ------------------------------------------------------------------ */
/*  Google Maps refs                                                    */
/* ------------------------------------------------------------------ */

let map: any               = null
let directionsService: any = null
let directionsRenderer: any = null
let transitLayer: any      = null

/* ------------------------------------------------------------------ */
/*  Helpers                                                             */
/* ------------------------------------------------------------------ */

const VEHICLE_EMOJI: Record<string, string> = {
  BUS:       '🚌',
  SUBWAY:    '🚇',
  METRO:     '🚇',
  TRAIN:     '🚆',
  TRAM:      '🚊',
  RAIL:      '🚆',
  FERRY:     '⛴️',
  CABLE_CAR: '🚡',
    OTHER:     '🚐',
}

function vehicleEmoji(name?: string): string {
  if (!name) return '🚐'
  const key = name.toUpperCase().replace(/ /g, '_')
  return (VEHICLE_EMOJI[key] ?? VEHICLE_EMOJI.OTHER) as string
}

function stripHtml(html: string): string {
  return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
}

function parseStep(raw: any): DirectionStep {
  const isTransit = raw.travel_mode === 'TRANSIT'
  const td        = raw.transit

  if (isTransit && td) {
    const vehicleName = td.line?.vehicle?.name ?? 'Transit'
    const lineName    = td.line?.short_name ?? td.line?.name ?? vehicleName
    const color       = td.line?.color ? `#${td.line.color}` : '#7c3aed'
    const textColor   = td.line?.text_color ? `#${td.line.text_color}` : '#ffffff'

    return {
      kind:          'TRANSIT',
      instruction:   `Board ${vehicleName} ${lineName}`,
      distance:      raw.distance?.text ?? '',
      duration:      raw.duration?.text ?? '',
      line:          lineName,
      vehicle:       vehicleName,
      vehicleIcon:   td.line?.vehicle?.icon ?? '',
      color,
      textColor,
      departureStop: td.departure_stop?.name,
      arrivalStop:   td.arrival_stop?.name,
      departureTime: td.departure_time?.text,
      arrivalTime:   td.arrival_time?.text,
      numStops:      typeof td.num_stops === 'number' ? td.num_stops : undefined,
      headsign:      td.headsign,
    }
  }

  return {
    kind:        'WALKING',
    instruction: stripHtml(raw.html_instructions ?? raw.instructions ?? 'Walk'),
    distance:    raw.distance?.text ?? '',
    duration:    raw.duration?.text ?? '',
  }
}

function parseRoute(raw: any, index: number): Route {
  const leg   = raw.legs?.[0]
  const steps = (leg?.steps ?? []).map(parseStep)

  return {
    index,
    duration:      leg?.duration?.text ?? '—',
    distance:      leg?.distance?.text ?? '—',
    startAddress:  leg?.start_address ?? origin.value,
    endAddress:    leg?.end_address ?? destination.value,
    departureTime: leg?.departure_time?.text ?? 'Now',
    arrivalTime:   leg?.arrival_time?.text ?? '—',
    steps,
    summary:       raw.summary ?? `Route ${index + 1}`,
  }
}

/* ------------------------------------------------------------------ */
/*  Google Maps init                                                    */
/* ------------------------------------------------------------------ */

function loadGoogleMapsScript() {
  if (window.google?.maps) return Promise.resolve(window.google)
  if (window.__summsGoogleMapsLoading) return window.__summsGoogleMapsLoading

  window.__summsGoogleMapsLoading = new Promise((resolve, reject) => {
    const script    = document.createElement('script')
    script.src      = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_API_KEY}&v=weekly`
    script.async    = true
    script.defer    = true
    script.onload   = () => resolve(window.google)
    script.onerror  = () => reject(new Error('Google Maps failed to load.'))
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
    mapError.value     = ''

    await loadGoogleMapsScript()
    const google = window.google

    map = new google.maps.Map(mapElement.value, {
      center:             { lat: 45.5017, lng: -73.5673 },
      zoom:               12,
      mapTypeControl:     false,
      streetViewControl:  false,
      fullscreenControl:  true,
      styles: [
        { featureType: 'transit', elementType: 'all', stylers: [{ visibility: 'simplified' }] },
      ],
    })

    transitLayer = new google.maps.TransitLayer()
    transitLayer.setMap(map)

    directionsService  = new google.maps.DirectionsService()
    directionsRenderer = new google.maps.DirectionsRenderer({
      map,
      suppressMarkers:  false,
      polylineOptions: {
        strokeColor:   '#7c3aed',
        strokeOpacity: 0.9,
        strokeWeight:  5,
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

/* ------------------------------------------------------------------ */
/*  Routing                                                             */
/* ------------------------------------------------------------------ */

async function planRoute() {
  if (!directionsService || !directionsRenderer || !window.google) return
  if (!origin.value.trim() || !destination.value.trim()) return

  isRouting.value    = true
  mapError.value     = ''
  routes.value       = []
  activeRouteIndex.value = 0

  return new Promise<void>((resolve) => {
    directionsService.route(
      {
        origin:                   origin.value,
        destination:              destination.value,
        travelMode:               window.google.maps.TravelMode.TRANSIT,
        provideRouteAlternatives: true,
        transitOptions:           { departureTime: new Date() },
      },
      (result: any, status: string) => {
        isRouting.value = false

        if (status === 'OK' && result) {
          directionsRenderer.setDirections(result)
          routes.value = (result.routes ?? []).map((r: any, i: number) => parseRoute(r, i))
        } else {
          routes.value   = []
          mapError.value = `No transit route found (${status}). Try a more specific Montréal address.`
        }

        resolve()
      },
    )
  })
}

function selectRoute(index: number) {
  activeRouteIndex.value = index
  if (directionsRenderer) {
    directionsRenderer.setRouteIndex(index)
  }
}

/* ------------------------------------------------------------------ */
/*  Watchers / lifecycle                                                */
/* ------------------------------------------------------------------ */

watch([origin, destination], () => {
  routes.value = []
})

onMounted(async () => {
  await initializeMap()
})
</script>

<template>
  <div class="page">

    <!-- ── Hero ── -->
    <section class="hero-card">
      <p class="eyebrow">Citizen mobility tools</p>
      <h1>Public Transport Hub</h1>
    </section>

    <!-- ── Planner inputs ── -->
    <section class="planner-card">
      <h2 class="section-title">Trip Planner</h2>

      <div class="planner-grid">
        <label class="field">
          <span class="field-label">🔵 From</span>
          <input v-model="origin" type="text" placeholder="Enter your starting point" />
        </label>

        <div class="swap-col">
          <button class="swap-btn" title="Swap" @click="[origin, destination] = [destination, origin]">⇅</button>
        </div>

        <label class="field">
          <span class="field-label">📍 To</span>
          <input v-model="destination" type="text" placeholder="Enter your destination" />
        </label>
      </div>

      <div class="planner-actions">
        <button class="action-btn" :disabled="!canPlanRoute || isRouting" @click="planRoute">
          <span v-if="isRouting">⏳ Searching…</span>
          <span v-else>🔍 Get Directions</span>
        </button>
      </div>

      <p v-if="!GOOGLE_MAPS_API_KEY" class="info-banner warning">
        Add your Google Maps key to <code>frontend/.env.local</code> to activate the live map.
      </p>
      <p v-else-if="isLoadingMap" class="info-banner">Loading interactive transit map…</p>
      <p v-if="mapError" class="info-banner error">{{ mapError }}</p>
    </section>

    <!-- ── Route alternatives selector ── -->
    <div v-if="routes.length > 1" class="route-tabs">
      <button
        v-for="r in routes"
        :key="r.index"
        class="route-tab"
        :class="{ active: r.index === activeRouteIndex }"
        @click="selectRoute(r.index)"
      >
        <span class="rt-label">Option {{ r.index + 1 }}</span>
        <span class="rt-time">{{ r.duration }}</span>
        <span class="rt-arrive">Arrive {{ r.arrivalTime }}</span>
      </button>
    </div>

    <!-- ── Main grid: map + directions ── -->
    <section class="content-grid">

      <!-- Map -->
      <article class="card map-card">
        <h2 class="section-title">Interactive Transit Map</h2>
        <div ref="mapElement" class="map-canvas"></div>
      </article>

      <!-- Directions panel -->
      <article class="card directions-card">
        <h2 class="section-title">Directions &amp; Schedule</h2>

        <!-- Empty state -->
        <div v-if="!activeRoute" class="empty-state">
          <div class="empty-icon">🗺️</div>
          <p>Enter a start and destination above, then tap <strong>Get Directions</strong>.</p>
        </div>

        <!-- Route detail -->
        <div v-else class="route-detail">

          <!-- Summary bar -->
          <div class="trip-summary">
            <div class="ts-row">
              <div class="ts-addr">
                <span class="ts-dot from-dot"></span>
                <span>{{ activeRoute.startAddress }}</span>
              </div>
              <div class="ts-addr">
                <span class="ts-dot to-dot"></span>
                <span>{{ activeRoute.endAddress }}</span>
              </div>
            </div>
            <div class="ts-stats">
              <div class="ts-stat">
                <span class="ts-icon">⏱</span>
                <div>
                  <div class="ts-val">{{ activeRoute.duration }}</div>
                  <div class="ts-sub">Total time</div>
                </div>
              </div>
              <div class="ts-stat">
                <span class="ts-icon">📏</span>
                <div>
                  <div class="ts-val">{{ activeRoute.distance }}</div>
                  <div class="ts-sub">Distance</div>
                </div>
              </div>
              <div class="ts-stat">
                <span class="ts-icon">🕐</span>
                <div>
                  <div class="ts-val">{{ activeRoute.departureTime }}</div>
                  <div class="ts-sub">Depart</div>
                </div>
              </div>
              <div class="ts-stat">
                <span class="ts-icon">🏁</span>
                <div>
                  <div class="ts-val">{{ activeRoute.arrivalTime }}</div>
                  <div class="ts-sub">Arrive</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Step-by-step timeline -->
          <div class="timeline">
            <div
              v-for="(step, i) in activeRoute.steps"
              :key="i"
              class="tl-item"
              :class="step.kind.toLowerCase()"
            >

              <!-- connector line -->
              <div class="tl-track">
                <div
                  class="tl-dot"
                  :style="step.kind === 'TRANSIT' ? { background: step.color } : {}"
                ></div>
                <div
                  v-if="i < activeRoute.steps.length - 1"
                  class="tl-line"
                  :style="step.kind === 'TRANSIT' ? { background: step.color } : {}"
                ></div>
              </div>

              <!-- content -->
              <div class="tl-body">

                <!-- WALKING step -->
                <template v-if="step.kind === 'WALKING'">
                  <div class="tl-header walk">
                    <span class="tl-mode-badge walk-badge">🚶 Walk</span>
                    <span class="tl-meta">{{ step.duration }} · {{ step.distance }}</span>
                  </div>
                  <p class="tl-instruction">{{ step.instruction }}</p>
                </template>

                <!-- TRANSIT step -->
                <template v-else>
                  <div class="tl-header transit">
                    <span
                      class="tl-mode-badge transit-badge"
                      :style="{ background: step.color, color: step.textColor }"
                    >
                      {{ vehicleEmoji(step.vehicle) }} {{ step.line }}
                    </span>
                    <span class="tl-meta">{{ step.duration }} · {{ step.distance }}</span>
                  </div>

                  <div class="schedule-block">
                    <!-- Departure row -->
                    <div class="sched-row depart-row">
                      <div class="sched-time">{{ step.departureTime }}</div>
                      <div class="sched-stop">
                        <span class="stop-dot depart-dot"></span>
                        <span class="stop-name">{{ step.departureStop }}</span>
                      </div>
                    </div>

                    <!-- Stops count -->
                    <div v-if="step.numStops !== undefined" class="sched-mid">
                      <div class="sched-pipe"></div>
                      <span class="sched-stops-label">
                        {{ step.numStops }} stop{{ step.numStops !== 1 ? 's' : '' }}
                        <span v-if="step.headsign" class="headsign">→ {{ step.headsign }}</span>
                      </span>
                    </div>

                    <!-- Arrival row -->
                    <div class="sched-row arrive-row">
                      <div class="sched-time">{{ step.arrivalTime }}</div>
                      <div class="sched-stop">
                        <span class="stop-dot arrive-dot"></span>
                        <span class="stop-name">{{ step.arrivalStop }}</span>
                      </div>
                    </div>
                  </div>
                </template>

              </div>
            </div>

            <!-- Destination marker -->
            <div v-if="activeRoute" class="tl-item destination">
              <div class="tl-track">
                <div class="tl-dot dest-dot"></div>
              </div>
              <div class="tl-body">
                <div class="tl-header">
                  <span class="tl-mode-badge dest-badge">🏁 Arrive</span>
                  <span class="tl-meta">{{ activeRoute.arrivalTime }}</span>
                </div>
                <p class="tl-instruction">{{ activeRoute.endAddress }}</p>
              </div>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
/* ------------------------------------------------------------------ */
/*  Page shell                                                           */
/* ------------------------------------------------------------------ */
.page {
  padding: 2rem clamp(1rem, 2vw, 2.5rem);
  width: min(96vw, 1360px);
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
}

/* ------------------------------------------------------------------ */
/*  Hero                                                                */
/* ------------------------------------------------------------------ */
.hero-card {
  background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%);
  border-radius: 18px;
  padding: 2rem 2rem 1.75rem;
  margin-bottom: 1.25rem;
  color: white;
}

.eyebrow {
  margin: 0 0 0.4rem;
  text-transform: uppercase;
  letter-spacing: 0.09em;
  font-size: 0.72rem;
  font-weight: 800;
  opacity: 0.85;
}

h1 {
  margin: 0 0 0.5rem;
  font-size: 2.1rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.hero-text {
  margin: 0;
  opacity: 0.85;
  font-size: 1rem;
  max-width: 600px;
}

/* ------------------------------------------------------------------ */
/*  Planner card                                                        */
/* ------------------------------------------------------------------ */
.planner-card,
.card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  padding: 1.5rem;
}

.planner-card {
  margin-bottom: 1rem;
}

.section-title {
  margin: 0 0 1.1rem;
  font-size: 1.05rem;
  font-weight: 700;
  color: #1a202c;
}

.planner-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 0.75rem;
  align-items: end;
  margin-bottom: 1rem;
}

.field {
  display: grid;
  gap: 0.4rem;
}

.field-label {
  font-size: 0.82rem;
  font-weight: 700;
  color: #4a5568;
}

.field input {
  border: 1.5px solid #dbe3ee;
  border-radius: 10px;
  padding: 0.8rem 0.9rem;
  font-size: 0.95rem;
  width: 100%;
  box-sizing: border-box;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.field input:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}

.swap-col {
  display: flex;
  align-items: center;
  padding-bottom: 0.1rem;
}

.swap-btn {
  background: #f3f0ff;
  border: 1px solid #ddd6fe;
  color: #6d28d9;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.swap-btn:hover {
  background: #ede9fe;
}

.planner-actions {
  margin-top: 0.5rem;
}

.action-btn {
  border-radius: 10px;
  font-weight: 700;
  font-size: 0.95rem;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  color: white;
  border: none;
  padding: 0.85rem 1.5rem;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.1s;
}

.action-btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.info-banner {
  margin-top: 0.9rem;
  padding: 0.8rem 1rem;
  border-radius: 10px;
  background: #f8fafc;
  font-size: 0.9rem;
  color: #4a5568;
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

code {
  font-family: ui-monospace, monospace;
  background: #f3f4f6;
  padding: 0.1em 0.35em;
  border-radius: 4px;
  font-size: 0.9em;
}

/* ------------------------------------------------------------------ */
/*  Route selector tabs                                                 */
/* ------------------------------------------------------------------ */
.route-tabs {
  display: flex;
  gap: 0.6rem;
  margin-bottom: 1rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
}

.route-tab {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  padding: 0.65rem 1rem;
  cursor: pointer;
  min-width: 130px;
  text-align: left;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.route-tab.active {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}

.rt-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #718096;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.rt-time {
  font-size: 1.05rem;
  font-weight: 800;
  color: #1a202c;
}

.rt-arrive {
  font-size: 0.82rem;
  color: #718096;
}

/* ------------------------------------------------------------------ */
/*  Content grid                                                        */
/* ------------------------------------------------------------------ */
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(360px, 0.7fr);
  gap: 1rem;
  align-items: start;
}

.map-card,
.directions-card {
  position: sticky;
  top: 1rem;
}

.map-canvas {
  width: 100%;
  height: 680px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #eef2ff, #f8fafc);
  border: 1px solid #edf2f7;
}

/* ------------------------------------------------------------------ */
/*  Directions card                                                     */
/* ------------------------------------------------------------------ */
.directions-card {
  max-height: calc(100vh - 3rem);
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2.5rem 1rem;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #dbe3ee;
  text-align: center;
  color: #718096;
}

.empty-icon {
  font-size: 2.5rem;
}

/* ------------------------------------------------------------------ */
/*  Trip summary banner                                                 */
/* ------------------------------------------------------------------ */
.trip-summary {
  background: #f8fafc;
  border-radius: 14px;
  padding: 1rem 1.1rem;
  margin-bottom: 1.25rem;
  border: 1px solid #edf2f7;
}

.ts-row {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-bottom: 1rem;
}

.ts-addr {
  display: flex;
  align-items: flex-start;
  gap: 0.55rem;
  font-size: 0.875rem;
  color: #2d3748;
}

.ts-dot {
  flex-shrink: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 3px;
}

.from-dot { background: #3b82f6; }
.to-dot   { background: #10b981; }

.ts-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.5rem;
}

.ts-stat {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.55rem 0.6rem;
}

.ts-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.ts-val {
  font-size: 0.82rem;
  font-weight: 700;
  color: #1a202c;
  white-space: nowrap;
}

.ts-sub {
  font-size: 0.68rem;
  color: #718096;
  margin-top: 0.05rem;
}

/* ------------------------------------------------------------------ */
/*  Timeline                                                            */
/* ------------------------------------------------------------------ */
.timeline {
  display: flex;
  flex-direction: column;
}

.tl-item {
  display: flex;
  gap: 0.75rem;
  min-height: 2rem;
}

.tl-track {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 20px;
  padding-top: 4px;
}

.tl-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e0;
  border: 2px solid white;
  box-shadow: 0 0 0 2px #cbd5e0;
  flex-shrink: 0;
  z-index: 1;
}

.tl-item.transit .tl-dot {
  width: 14px;
  height: 14px;
}

.dest-dot {
  background: #10b981 !important;
  box-shadow: 0 0 0 2px #10b981 !important;
}

.tl-line {
  flex: 1;
  width: 3px;
  background: #e2e8f0;
  margin: 3px 0;
  min-height: 1rem;
}

.tl-body {
  flex: 1;
  padding-bottom: 1.25rem;
}

.tl-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-bottom: 0.4rem;
}

.tl-mode-badge {
  font-size: 0.78rem;
  font-weight: 700;
  border-radius: 999px;
  padding: 0.25rem 0.65rem;
  white-space: nowrap;
}

.walk-badge {
  background: #ebf8ff;
  color: #2b6cb0;
}

.transit-badge {
  /* color + bg set via inline :style binding */
  opacity: 1;
}

.dest-badge {
  background: #f0fff4;
  color: #276749;
}

.tl-meta {
  font-size: 0.78rem;
  color: #718096;
  font-weight: 600;
}

.tl-instruction {
  font-size: 0.88rem;
  color: #2d3748;
  margin: 0;
  line-height: 1.5;
}

/* ------------------------------------------------------------------ */
/*  Schedule block (transit legs)                                       */
/* ------------------------------------------------------------------ */
.schedule-block {
  background: #fcfcfe;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.75rem 0.9rem;
  margin-top: 0.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.sched-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sched-time {
  font-size: 0.85rem;
  font-weight: 800;
  color: #1a202c;
  min-width: 52px;
  text-align: right;
  letter-spacing: -0.01em;
}

.sched-stop {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
  color: #2d3748;
}

.stop-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.depart-dot { background: #7c3aed; }
.arrive-dot { background: #10b981; }

.stop-name {
  font-weight: 600;
}

.sched-mid {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-left: 52px;
}

.sched-pipe {
  width: 2px;
  height: 22px;
  background: #ddd6fe;
  border-radius: 2px;
  margin-left: 3px;
}

.sched-stops-label {
  font-size: 0.78rem;
  color: #718096;
  font-weight: 600;
}

.headsign {
  color: #6d28d9;
  font-style: italic;
}

/* ------------------------------------------------------------------ */
/*  Responsive                                                          */
/* ------------------------------------------------------------------ */
@media (max-width: 1080px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .map-card,
  .directions-card {
    position: static;
  }

  .directions-card {
    max-height: none;
  }

  .map-canvas { height: 520px; }
}

@media (max-width: 720px) {
  .planner-grid {
    grid-template-columns: 1fr;
  }

  .swap-col { display: none; }

  .ts-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .map-canvas { height: 400px; }
}
</style>