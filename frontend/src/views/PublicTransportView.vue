<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { api } from '../api'

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

/** Top-level page tabs: trip planner vs STM official schedules */
const activePageTab = ref<'planner' | 'stm'>('planner')

const stmResources = [
  {
    title: 'STM trip planner',
    desc: 'Official STM site: plan metro and bus trips, fares, and general information.',
    href: 'https://www.stm.info/en',
    icon: '🗺️',
  },
  {
    title: 'Metro network & schedules',
    desc: 'Lines, stations, and metro timetables from the STM.',
    href: 'https://www.stm.info/en/info/networks/metro',
    icon: '🚇',
  },
  {
    title: 'Bus network & schedules',
    desc: 'Bus routes and schedule information.',
    href: 'https://www.stm.info/en/info/networks/bus',
    icon: '🚌',
  },
  {
    title: 'Service status & alerts',
    desc: 'Delays, detours, and elevator outages.',
    href: 'https://www.stm.info/en/info/service-updates',
    icon: '⚠️',
  },
] as const

const metroLines = [
  { name: 'Green', route: 'Angrignon ↔ Honoré-Beaugrand', color: '#008E4F' },
  { name: 'Orange', route: 'Côte-Vertu ↔ Montmorency', color: '#F07A29' },
  { name: 'Yellow', route: 'Berri-UQAM ↔ Longueuil', color: '#F8D200' },
  { name: 'Blue', route: 'Snowdon ↔ Saint-Michel', color: '#0083C9' },
] as const

/** STM iBUS trip-updates proxy (backend needs stm.api.key) */
interface BusDepartureRow {
  stopId: string
  stopName?: string | null
  departureTime: string
  departureTimeLocal: string
  delaySeconds?: number
  tripId?: string
  directionId?: number
  directionLabel?: string | null
  wheelchairBoarding?: number
  wheelchairAccessible?: boolean
}

interface BusDeparturesResponse {
  configured?: boolean
  message?: string
  stmDeveloperPortal?: string
  stmBusSchedules?: string
  line?: string
  stopCode?: string | null
  accessibleOnly?: boolean
  directionLabels?: Record<string, string>
  syncedAt?: string
  count?: number
  departures?: BusDepartureRow[]
  hint?: string
  error?: string
}

interface BusStopRow {
  stopId: string
  label: string
  code?: string | null
  stopName?: string | null
  wheelchairBoarding?: number
  wheelchairAccessible?: boolean
}

interface BusStopsResponse {
  configured?: boolean
  message?: string
  stmDeveloperPortal?: string
  line?: string
  accessibleOnly?: boolean
  directionLabels?: Record<string, string>
  syncedAt?: string
  count?: number
  stops?: BusStopRow[]
  hint?: string
  error?: string
}

const busLine = ref('')
/** Restrict STM bus stops and departures to GTFS {@code wheelchair_boarding = 1}. */
const busAccessibleOnly = ref(false)
const busStopsList = ref<BusStopRow[]>([])
const selectedBusStopId = ref('')
const busLoading = ref(false)
const busStopsLoading = ref(false)
const busError = ref<string | null>(null)
const busStopsHint = ref<string | null>(null)
/** STM key missing / not subscribed (from stops or departures endpoint). */
const stmBlocked = ref<BusDeparturesResponse | null>(null)
const busResult = ref<BusDeparturesResponse | null>(null)

let busStopsDebounce: ReturnType<typeof setTimeout> | null = null

async function fetchBusStops() {
  const line = busLine.value.trim()
  busStopsList.value = []
  busStopsHint.value = null
  busError.value = null
  stmBlocked.value = null

  if (!line) {
    return
  }

  busStopsLoading.value = true
  try {
    const params = new URLSearchParams({ line })
    if (busAccessibleOnly.value) {
      params.set('accessibleOnly', 'true')
    }
    const res = await api.get<BusStopsResponse>(`/stm/bus-stops?${params.toString()}`)
    const data = res.data
    if (data.configured === false) {
      stmBlocked.value = {
        configured: false,
        message: data.message,
        stmDeveloperPortal: data.stmDeveloperPortal,
        stmBusSchedules: 'https://www.stm.info/en/info/networks/bus',
      }
      return
    }
    if (data.error) {
      busError.value = String(data.error)
      return
    }
    busStopsList.value = data.stops ?? []
    const sel = selectedBusStopId.value.trim()
    if (sel && !busStopsList.value.some((s) => s.stopId === sel)) {
      selectedBusStopId.value = ''
      busResult.value = null
    }
    if (busStopsList.value.length === 0 && data.hint) {
      busStopsHint.value = data.hint
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { error?: string } } }
    busError.value = err.response?.data?.error ?? 'Could not load stops for this line.'
  } finally {
    busStopsLoading.value = false
  }
}

async function fetchBusDepartures() {
  const line = busLine.value.trim()
  const stop = selectedBusStopId.value.trim()
  if (!line) {
    busError.value = 'Enter a bus line number (e.g. 165).'
    return
  }
  if (!stop) {
    busError.value = 'Choose a stop from the list.'
    return
  }
  busLoading.value = true
  busError.value = null
  busResult.value = null
  stmBlocked.value = null
  try {
    const params = new URLSearchParams({ line, stopCode: stop })
    if (busAccessibleOnly.value) {
      params.set('accessibleOnly', 'true')
    }
    const res = await api.get<BusDeparturesResponse>(`/stm/bus-departures?${params.toString()}`)
    if (res.data.configured === false) {
      stmBlocked.value = res.data
    } else {
      busResult.value = res.data
    }
    if (res.data.error) {
      busError.value = String(res.data.error)
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { error?: string } } }
    busError.value = err.response?.data?.error ?? 'Could not load bus departures.'
  } finally {
    busLoading.value = false
  }
}

watch(busLine, () => {
  if (busStopsDebounce) {
    clearTimeout(busStopsDebounce)
    busStopsDebounce = null
  }
  busStopsList.value = []
  selectedBusStopId.value = ''
  busResult.value = null
  busStopsHint.value = null
  busError.value = null
  stmBlocked.value = null

  const line = busLine.value.trim()
  if (!line) {
    return
  }
  busStopsDebounce = setTimeout(() => {
    busStopsDebounce = null
    void fetchBusStops()
  }, 450)
})

watch(selectedBusStopId, (id) => {
  if (!id.trim()) {
    busResult.value = null
    busError.value = null
    return
  }
  void fetchBusDepartures()
})

watch(busAccessibleOnly, () => {
  busResult.value = null
  if (!busLine.value.trim()) {
    return
  }
  void fetchBusStops().then(() => {
    if (selectedBusStopId.value.trim()) {
      void fetchBusDepartures()
    }
  })
})

const selectedStopLabel = computed(() => {
  const id = selectedBusStopId.value.trim()
  if (!id) return ''
  return busStopsList.value.find((s) => s.stopId === id)?.label ?? id
})

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
      <p class="hero-text">
        Plan trips in-app or open official STM schedules and the STM trip planner in a new tab.
      </p>
    </section>

    <!-- ── Page tabs (Trip planner | STM schedules) ── -->
    <div class="page-tabs" role="tablist" aria-label="Public transport sections">
      <button
        type="button"
        role="tab"
        class="page-tab"
        :class="{ active: activePageTab === 'planner' }"
        :aria-selected="activePageTab === 'planner'"
        @click="activePageTab = 'planner'"
      >
        Trip planner
      </button>
      <button
        type="button"
        role="tab"
        class="page-tab"
        :class="{ active: activePageTab === 'stm' }"
        :aria-selected="activePageTab === 'stm'"
        @click="activePageTab = 'stm'"
      >
        STM schedule
      </button>
    </div>

    <!-- ═══════════ STM official schedules tab ═══════════ -->
    <section v-show="activePageTab === 'stm'" class="stm-panel">
      <div class="stm-intro card">
        <h2 class="section-title">STM (Montréal) — schedules &amp; tools</h2>
        <p class="stm-lead">
          Schedules and real-time data are maintained by the
          <strong>Société de transport de Montréal</strong>. Use the links below for official metro and bus
          timetables, maps, and alerts. SUMMS opens these in a new browser tab.
        </p>
      </div>

      <div class="bus-departures card">
        <h2 class="section-title">Bus line — next departures</h2>
        <p class="stm-lead bus-departures-lead">
          Real-time times come from STM’s <strong>GTFS-Realtime trip updates</strong> (iBUS). Enter a bus line; we load
          stops that currently have upcoming data for that line. When the feed includes a trip
          <strong>direction</strong>, we show it (North/South/East/West from the static schedule when we can match the
          line). Stop names and the <strong>wheelchair-accessible filter</strong> use STM’s static GTFS field
          <code>wheelchair_boarding</code> = 1.
        </p>
        <label class="bus-accessible-filter">
          <input v-model="busAccessibleOnly" type="checkbox" />
          <span>Wheelchair-accessible stops only</span>
        </label>
        <div class="bus-search-row">
          <label class="field bus-field">
            <span class="field-label">Bus line</span>
            <input v-model="busLine" type="text" inputmode="numeric" placeholder="e.g. 165" maxlength="6" />
          </label>
          <label class="field bus-field bus-field-grow">
            <span class="field-label">Stop</span>
            <select
              v-model="selectedBusStopId"
              class="bus-select"
              :disabled="!busLine.trim() || busStopsLoading || !busStopsList.length"
            >
              <option value="" disabled>
                {{
                  !busLine.trim()
                    ? 'Enter a line first'
                    : busStopsLoading
                      ? 'Loading stops…'
                      : busStopsList.length
                        ? 'Choose a stop'
                        : 'No stops in the live feed for this line'
                }}
              </option>
              <option v-for="s in busStopsList" :key="s.stopId" :value="s.stopId">
                {{ s.wheelchairAccessible ? `${s.label} ♿` : s.label }}
              </option>
            </select>
          </label>
          <div class="bus-search-actions">
            <button
              type="button"
              class="action-btn secondary-bus-btn"
              :disabled="busLoading || !selectedBusStopId.trim()"
              @click="fetchBusDepartures"
            >
              {{ busLoading ? 'Loading…' : 'Refresh times' }}
            </button>
          </div>
        </div>
        <p v-if="busError" class="info-banner error">{{ busError }}</p>
        <p v-if="busStopsHint" class="info-banner">{{ busStopsHint }}</p>
        <div v-if="stmBlocked && stmBlocked.configured === false" class="info-banner warning">
          <strong>STM API key not configured.</strong>
          {{ stmBlocked.message }}
          <template v-if="stmBlocked.stmDeveloperPortal">
            <br />
            <a :href="stmBlocked.stmDeveloperPortal" target="_blank" rel="noopener noreferrer">STM developer portal</a>
            ·
            <a v-if="stmBlocked.stmBusSchedules" :href="stmBlocked.stmBusSchedules" target="_blank" rel="noopener noreferrer">Bus schedules on stm.info</a>
          </template>
        </div>
        <div v-if="busResult?.configured && busResult.departures?.length" class="bus-results">
          <p class="bus-results-meta">
            Line <strong>{{ busResult.line }}</strong>
            <template v-if="selectedStopLabel"> · <strong>{{ selectedStopLabel }}</strong></template>
            <template v-if="busResult.syncedAt"> · Updated {{ busResult.syncedAt }}</template>
          </p>
          <p
            v-if="busResult.directionLabels && Object.keys(busResult.directionLabels).length"
            class="bus-dir-legend"
          >
            Trip directions on this line:
            <template v-for="(name, id) in busResult.directionLabels" :key="id">
              <span class="bus-dir-pill"><strong>{{ id }}</strong> = {{ name }}</span>
            </template>
          </p>
          <table class="bus-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Stop</th>
                <th>Accessible</th>
                <th>Direction</th>
                <th>Delay</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in busResult.departures" :key="i">
                <td class="bus-time">{{ row.departureTimeLocal }}</td>
                <td class="bus-stop-cell">
                  <div class="bus-stop-primary">{{ row.stopName || row.stopId }}</div>
                  <div v-if="row.stopName" class="bus-stop-sub">ID {{ row.stopId }}</div>
                </td>
                <td class="bus-a11y-cell">
                  <span v-if="row.wheelchairAccessible" class="bus-a11y-yes" title="STM GTFS: wheelchair_boarding = 1"
                    >Yes</span
                  >
                  <span v-else class="bus-a11y-na">—</span>
                </td>
                <td class="bus-dir">
                  <span v-if="row.directionLabel">{{ row.directionLabel }}</span>
                  <span v-else-if="row.directionId === 0 || row.directionId === 1">{{ row.directionId }}</span>
                  <span v-else>—</span>
                </td>
                <td>
                  <span v-if="row.delaySeconds != null && row.delaySeconds !== 0">{{ row.delaySeconds }}s</span>
                  <span v-else>—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else-if="busResult?.configured && busResult.hint" class="info-banner">{{ busResult.hint }}</div>
      </div>

      <div class="stm-grid">
        <a
          v-for="item in stmResources"
          :key="item.href"
          class="stm-card"
          :href="item.href"
          target="_blank"
          rel="noopener noreferrer"
        >
          <span class="stm-card-icon" aria-hidden="true">{{ item.icon }}</span>
          <span class="stm-card-title">{{ item.title }}</span>
          <span class="stm-card-desc">{{ item.desc }}</span>
          <span class="stm-card-cta">Open on stm.info →</span>
        </a>
      </div>

      <div class="metro-ref card">
        <h3 class="metro-ref-title">Metro lines (quick reference)</h3>
        <ul class="metro-ref-list">
          <li v-for="line in metroLines" :key="line.name" class="metro-ref-row">
            <span class="metro-swatch" :style="{ background: line.color }" />
            <span class="metro-name">Line {{ line.name }}</span>
            <span class="metro-route">{{ line.route }}</span>
          </li>
        </ul>
      </div>
    </section>

    <!-- ═══════════ In-app trip planner tab ═══════════ -->
    <div v-show="activePageTab === 'planner'">
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

      <p class="planner-transit-note">
        Google’s in-app transit directions do not expose a wheelchair-only route mode here. For STM bus times filtered by
        accessible stops, use the <strong>STM schedule</strong> tab and the checkbox below the intro.
      </p>

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
  opacity: 0.9;
  font-size: 0.98rem;
  max-width: 640px;
  line-height: 1.5;
}

/* Page-level tabs (below hero, on page background) */
.page-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.1rem;
  flex-wrap: wrap;
}

.page-tab {
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #475569;
  font-weight: 700;
  font-size: 0.92rem;
  padding: 0.55rem 1.15rem;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s, color 0.15s;
}

.page-tab:hover {
  border-color: #c4b5fd;
  color: #4f46e5;
}

.page-tab.active {
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}

/* STM schedules tab */
.stm-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stm-intro .stm-lead {
  margin: 0;
  font-size: 0.95rem;
  color: #4a5568;
  line-height: 1.55;
}

.bus-departures-lead {
  margin-bottom: 1rem;
}

.bus-accessible-filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 1rem;
  font-size: 0.92rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  user-select: none;
}

.bus-accessible-filter input {
  width: 1.05rem;
  height: 1.05rem;
  accent-color: #7c3aed;
  cursor: pointer;
}

.bus-search-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem 1rem;
  align-items: end;
  margin-bottom: 0.75rem;
}

.bus-field-grow {
  min-width: min(100%, 220px);
}

.bus-select {
  border: 1.5px solid #dbe3ee;
  border-radius: 10px;
  padding: 0.75rem 0.85rem;
  font-size: 0.95rem;
  width: 100%;
  box-sizing: border-box;
  background: #fff;
  color: #1a202c;
  cursor: pointer;
}

.bus-select:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}

.bus-select:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  background: #f8fafc;
}

.secondary-bus-btn {
  background: #fff !important;
  color: #4f46e5 !important;
  border: 1.5px solid #c4b5fd !important;
}

.secondary-bus-btn:hover:not(:disabled) {
  background: #f5f3ff !important;
}

.bus-field .field-label {
  font-size: 0.82rem;
  font-weight: 700;
  color: #4a5568;
}

.bus-field input {
  border: 1.5px solid #dbe3ee;
  border-radius: 10px;
  padding: 0.75rem 0.85rem;
  font-size: 0.95rem;
  width: 100%;
  box-sizing: border-box;
}

.bus-field input:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}

.bus-search-actions {
  display: flex;
  align-items: center;
}

.bus-search-actions .action-btn {
  width: 100%;
  justify-content: center;
}

.bus-results-meta {
  margin: 0 0 0.65rem;
  font-size: 0.88rem;
  color: #64748b;
}

.bus-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.bus-table th,
.bus-table td {
  text-align: left;
  padding: 0.55rem 0.65rem;
  border-bottom: 1px solid #edf2f7;
}

.bus-table th {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
  font-weight: 700;
}

.bus-time {
  font-weight: 800;
  color: #1a202c;
  white-space: nowrap;
}

.bus-stop-cell {
  min-width: 0;
}

.bus-stop-primary {
  font-weight: 700;
  color: #1a202c;
  font-size: 0.9rem;
  line-height: 1.35;
}

.bus-stop-sub {
  font-family: ui-monospace, monospace;
  font-size: 0.72rem;
  color: #94a3b8;
  margin-top: 0.2rem;
  word-break: break-all;
}

.bus-a11y-cell {
  white-space: nowrap;
  font-size: 0.82rem;
}

.bus-a11y-yes {
  font-weight: 800;
  color: #047857;
}

.bus-a11y-na {
  color: #94a3b8;
}

.bus-dir {
  font-size: 0.85rem;
  font-weight: 700;
  color: #334155;
  text-align: left;
  white-space: nowrap;
}

.bus-dir-legend {
  margin: 0 0 0.65rem;
  font-size: 0.8rem;
  color: #64748b;
  line-height: 1.5;
}

.bus-dir-pill {
  display: inline-block;
  margin: 0.15rem 0.5rem 0 0;
  padding: 0.12rem 0.45rem;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 0.78rem;
}

.stm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.85rem;
}

.stm-card {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  background: #fff;
  border-radius: 14px;
  padding: 1.1rem 1.15rem;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
}

.stm-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1);
  border-color: #c4b5fd;
}

.stm-card-icon {
  font-size: 1.35rem;
}

.stm-card-title {
  font-size: 1.02rem;
  font-weight: 800;
  color: #1a202c;
}

.stm-card-desc {
  font-size: 0.88rem;
  color: #64748b;
  line-height: 1.45;
  flex: 1;
}

.stm-card-cta {
  font-size: 0.82rem;
  font-weight: 700;
  color: #6d28d9;
  margin-top: 0.25rem;
}

.metro-ref-title {
  margin: 0 0 0.85rem;
  font-size: 1rem;
  font-weight: 800;
  color: #1a202c;
}

.metro-ref-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.metro-ref-row {
  display: grid;
  grid-template-columns: auto 100px 1fr;
  align-items: center;
  gap: 0.65rem;
  font-size: 0.88rem;
  color: #334155;
}

.metro-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  flex-shrink: 0;
}

.metro-name {
  font-weight: 800;
  color: #0f172a;
}

.metro-route {
  color: #64748b;
}

@media (max-width: 560px) {
  .metro-ref-row {
    grid-template-columns: auto 1fr;
    grid-template-rows: auto auto;
  }
  .metro-name {
    grid-column: 2;
  }
  .metro-route {
    grid-column: 1 / -1;
    padding-left: calc(12px + 0.65rem);
  }
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

.planner-transit-note {
  margin: 1rem 0 0;
  padding: 0.75rem 1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.5;
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