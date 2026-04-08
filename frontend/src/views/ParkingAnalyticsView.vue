<script setup lang="ts">
/**
 * Parking & Infrastructure Analytics (Stationary garages)
 * Visible to: System Admin (full scope), City Admin and parking operators (same KPIs, own garages only).
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { useAnalyticsStore } from '../stores/analytics'
import { useAuthStore } from '../stores/auth'

const CITY_SCOPE = '__CITY__'

interface UserOption {
  id: string
  name: string
  email: string
  role?: string
  providerType?: string
}

const store = useAnalyticsStore()
const auth = useAuthStore()
const route = useRoute()

const users = ref<UserOption[]>([])
const usersLoadError = ref<string | null>(null)

const providerSearch = ref('')
const selectedGarageScopeId = ref('')

const parkingProviders = computed(() =>
  users.value.filter((u) => u.role === 'MOBILITY_PROVIDER' && u.providerType === 'PARKING'),
)

const filteredParkingProviders = computed(() => {
  const q = providerSearch.value.trim().toLowerCase()
  let list = [...parkingProviders.value].sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }),
  )
  if (!q) return list
  return list.filter(
    (p) => p.name.toLowerCase().includes(q) || (p.email ?? '').toLowerCase().includes(q),
  )
})

const portfolioLocked = computed(() => auth.canManageParkingGarages)

const scopeLabel = computed(() => {
  if (portfolioLocked.value && auth.user) {
    return auth.isCityAdmin
      ? `${auth.user.name} (municipal facilities)`
      : `${auth.user.name} (your facilities)`
  }
  if (selectedGarageScopeId.value === '') return 'Everyone'
  if (selectedGarageScopeId.value === CITY_SCOPE) return 'City facilities'
  const p = parkingProviders.value.find((x) => x.id === selectedGarageScopeId.value)
  return p?.name ?? 'Selected provider'
})

function syncSearchLabelToScope(scopeId: string) {
  if (scopeId === '') {
    providerSearch.value = ''
    return
  }
  if (scopeId === CITY_SCOPE) {
    providerSearch.value = 'City facilities'
    return
  }
  const p = parkingProviders.value.find((x) => x.id === scopeId) ?? null
  providerSearch.value = p ? p.name : ''
}

function truncateAddr(addr: string, maxLen = 50): string {
  if (addr.length <= maxLen) return addr
  return `${addr.slice(0, maxLen)}…`
}

/** Prefer API breakdown; otherwise derive from garageDetails so charts work with older backends or empty arrays. */
const parkingAreaRows = computed(() => {
  const data = store.parkingData
  if (!data) return []

  const raw = data.parkingAreaBreakdown
  if (Array.isArray(raw) && raw.length > 0) {
    return raw.map((r) => {
      const spotsTaken = Number(r.spotsTaken) || 0
      const flat = Number(r.flatRate) || 0
      const implied =
        typeof r.occupancyImpliedRevenue === 'number' && Number.isFinite(r.occupancyImpliedRevenue)
          ? r.occupancyImpliedRevenue
          : Math.round(spotsTaken * flat * 100) / 100
      return {
        garageId: String(r.garageId ?? ''),
        areaName: String(r.areaName ?? ''),
        addressSnippet: r.addressSnippet != null ? String(r.addressSnippet) : '',
        spotsTaken,
        spotsTotal: Number(r.spotsTotal) || 0,
        activeReservationSpots: Number(r.activeReservationSpots) || 0,
        revenue: Number(r.revenue) || 0,
        occupancyImpliedRevenue: implied,
        flatRate: flat,
        capacityUtilizationPercent: Number(r.capacityUtilizationPercent) || 0,
        allTimeReservations: Number(r.allTimeReservations) || 0,
      }
    })
  }

  const gd = data.garageDetails ?? []
  const revMap = data.parkingRevenueByGarageId ?? {}
  const sessionsMap = data.parkingSessionsByGarageId ?? {}
  if (gd.length === 0) return []

  return gd.map((g) => {
    const tot = Number(g.totalSpaces) || 0
    const avail = Number(g.availableSpaces) || 0
    const spotsTaken = Math.max(0, tot - avail)
    const flat = Number(g.flatRate) || 0
    const implied = Math.round(spotsTaken * flat * 100) / 100
    const rev = typeof revMap[g.id] === 'number' ? revMap[g.id]! : 0
    const allTimeReservations =
      typeof sessionsMap[g.id] === 'number' ? Math.round(sessionsMap[g.id]!) : 0
    const addr = g.address ? truncateAddr(String(g.address)) : ''
    const util = tot === 0 ? 0 : Math.round((spotsTaken * 1000) / tot) / 10
    return {
      garageId: String(g.id ?? ''),
      areaName: String(g.name ?? ''),
      addressSnippet: addr,
      spotsTaken,
      spotsTotal: tot,
      activeReservationSpots: 0,
      revenue: rev,
      occupancyImpliedRevenue: implied,
      flatRate: flat,
      capacityUtilizationPercent: util,
      allTimeReservations,
    }
  })
})

const maxAllTimeReservations = computed(() =>
  parkingAreaRows.value.reduce((m, r) => Math.max(m, r.allTimeReservations), 0),
)
const maxRecordedRevenue = computed(() =>
  parkingAreaRows.value.reduce((m, r) => Math.max(m, r.revenue), 0),
)
const maxActiveSpots = computed(() =>
  parkingAreaRows.value.reduce((m, r) => Math.max(m, r.activeReservationSpots), 0),
)

function barPct(value: number, max: number): number {
  if (!Number.isFinite(value) || value <= 0) return 0
  const cap = !max || !Number.isFinite(max) ? 1 : max
  return Math.min(100, (value / cap) * 100)
}

async function refreshParking() {
  const uid = auth.user?.id
  if (portfolioLocked.value && uid) {
    await store.fetchParking(undefined, uid, uid)
    return
  }
  const g = selectedGarageScopeId.value
  await store.fetchParking(undefined, g === '' ? undefined : g)
}

function selectGarageScope(scopeId: string) {
  if (portfolioLocked.value) return
  selectedGarageScopeId.value = scopeId
  syncSearchLabelToScope(scopeId)
  void store.fetchParking(undefined, scopeId === '' ? undefined : scopeId)
}

function onProviderSearchInput(ev: Event) {
  if (portfolioLocked.value) return
  if (!(ev instanceof InputEvent) || !ev.isTrusted) return
  if (!providerSearch.value.trim()) {
    selectedGarageScopeId.value = ''
    void store.fetchParking(undefined, undefined)
  }
}

async function loadUsers() {
  usersLoadError.value = null
  try {
    const res = await api.get<UserOption[]>('/users')
    const raw = res.data as unknown[]
    users.value = raw.map((row: any) => ({
      id: String(row.id),
      name: String(row.name ?? 'Unknown'),
      email: String(row.email ?? ''),
      role: String(row.role ?? ''),
      providerType: row.providerType != null ? String(row.providerType) : undefined,
    }))
  } catch {
    users.value = []
    usersLoadError.value = 'Could not load users for garage scope.'
  }
}

function refetchIfParkingTabVisible() {
  if (route.path !== '/analytics/parking') return
  if (document.visibilityState !== 'visible') return
  void refreshParking()
}

onMounted(async () => {
  document.addEventListener('visibilitychange', refetchIfParkingTabVisible)
  if (portfolioLocked.value && auth.user) {
    selectedGarageScopeId.value = auth.user.id
    providerSearch.value = auth.user.name ?? ''
    await refreshParking()
    return
  }
  await loadUsers()
  await refreshParking()
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', refetchIfParkingTabVisible)
})
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <span class="view-tag">Infrastructure Oversight</span>
        <div class="scope-header-row">
          <span class="scope-pill parking">Garage scope</span>
        </div>
        <h1>Stationary Parking Infrastructure</h1>
        <p>
          {{
            portfolioLocked
              ? auth.isCityAdmin
                ? 'Real-time capacity for municipal garages you manage.'
                : 'Real-time capacity for your registered garages.'
              : 'Real-time capacity for garages — filter by city-owned facilities or a parking operator.'
          }}
        </p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" :disabled="store.loading" @click="refreshParking()">
          <span v-if="store.loading">Refreshing...</span>
          <span v-else>↻ Update Data</span>
        </button>
      </div>
    </header>

    <div v-if="store.loading && !store.parkingData" class="state-msg pulse">
      Interrogating infrastructure sensors...
    </div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <main v-else-if="store.parkingData" class="analytics-body">
      <section v-if="portfolioLocked" class="scope-toolbar panel-card-clean">
        <div class="scope-toolbar-grid">
          <div class="scope-field">
            <label class="field-label">{{ auth.isCityAdmin ? 'Municipal facilities' : 'Your facilities' }}</label>
            <p class="field-hint">
              <template v-if="auth.isCityAdmin">
                Same analytics layout as system oversight, but <strong>only municipal garages on your account</strong> are
                included. Private parking operators are not shown.
              </template>
              <template v-else>
                This view matches the city oversight dashboard but is <strong>limited to garages you operate</strong>.
                Other operators’ data is not shown.
              </template>
            </p>
          </div>
          <div class="scope-meta">
            <span class="meta-label">Scope</span>
            <strong class="meta-value">{{ scopeLabel }}</strong>
          </div>
        </div>
      </section>

      <section v-else-if="auth.isSysAdmin" class="scope-toolbar panel-card-clean">
        <div class="scope-toolbar-grid">
          <div class="scope-field">
            <label class="field-label" for="parking-provider-search">Garage operator scope</label>
            <p class="field-hint">
              Choose <strong>Everyone</strong> for all garages, <strong>City facilities</strong> for municipal
              inventory only, or search for a parking provider (leave blank for Everyone).
            </p>

            <input
              id="parking-provider-search"
              v-model="providerSearch"
              type="search"
              class="input-search"
              placeholder="Search parking provider by name or email…"
              autocomplete="off"
              @input="onProviderSearchInput"
            />

            <div class="provider-suggestions" role="listbox">
              <button
                type="button"
                class="provider-option"
                :class="{ active: selectedGarageScopeId === '' }"
                @click="selectGarageScope('')"
              >
                Everyone
              </button>
              <button
                type="button"
                class="provider-option"
                :class="{ active: selectedGarageScopeId === CITY_SCOPE }"
                @click="selectGarageScope(CITY_SCOPE)"
              >
                <span class="provider-option-name">City facilities</span>
                <span class="provider-option-email">Municipal / platform garages</span>
              </button>
              <button
                v-for="p in filteredParkingProviders.slice(0, 8)"
                :key="p.id"
                type="button"
                class="provider-option"
                :class="{ active: selectedGarageScopeId === p.id }"
                @click="selectGarageScope(p.id)"
              >
                <span class="provider-option-name">{{ p.name }}</span>
                <span class="provider-option-email">{{ p.email }}</span>
              </button>
              <div v-if="filteredParkingProviders.length === 0 && providerSearch.trim()" class="provider-empty">
                No matching parking providers.
              </div>
            </div>

            <p v-if="usersLoadError" class="field-warn">{{ usersLoadError }}</p>
          </div>
          <div class="scope-meta">
            <span class="meta-label">Scope</span>
            <strong class="meta-value">{{ scopeLabel }}</strong>
          </div>
        </div>
      </section>

      <section class="kpi-banner">
        <div class="kpi-grid">
          <div class="kpi-card-simple">
            <span class="label">Total spots taken</span>
            <strong class="value">{{ store.parkingData.totalParkingSpotsTaken ?? 0 }}</strong>
            <span class="subtext"
              >{{ store.parkingData.activeParkingSpotsInScope ?? 0 }} actively held in live reservations</span
            >
          </div>
          <div class="kpi-card-simple">
            <span class="label">Parking revenue</span>
            <strong class="value"
              >${{
                (store.parkingData.totalParkingRevenue ?? 0).toLocaleString(undefined, {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })
              }}</strong
            >
            <span class="subtext">Across garages in this scope</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Capacity in use</span>
            <strong class="value">{{ (store.parkingData.parkingCapacityUtilizationPercent ?? 0).toFixed(1) }}%</strong>
            <span class="subtext"
              >{{ store.parkingData.totalParkingSpotsTaken ?? 0 }} / {{ store.parkingData.totalGarageSpaces }} spaces in
              scope</span
            >
          </div>
          <div class="kpi-card-simple">
            <span class="label">Paid sessions</span>
            <strong class="value">{{ store.parkingData.paidParkingSessions ?? 0 }}</strong>
            <span class="subtext"
              >Avg. ${{
                (store.parkingData.averageParkingPayment ?? 0).toLocaleString(undefined, {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })
              }}
              per session · {{ store.parkingData.totalGarages }} garage(s)</span
            >
          </div>
        </div>
      </section>

      <div class="charts-grid">
        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>All-time reservations by facility</h3>
            <p>Count of non-cancelled parking reservations per garage in this scope (active and completed).</p>
          </div>
          <div v-if="parkingAreaRows.length === 0" class="empty-state">No garages in this scope.</div>
          <div v-else class="bar-chart">
            <div v-for="row in parkingAreaRows" :key="'s-' + row.garageId" class="bar-row">
              <span class="bar-label" :title="row.areaName">{{ row.areaName }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-volume"
                  :style="{ width: barPct(row.allTimeReservations, maxAllTimeReservations) + '%' }"
                />
              </div>
              <span class="bar-value">{{ row.allTimeReservations }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Revenue by facility</h3>
            <p>Payments from non-cancelled reservations, per garage in this scope.</p>
          </div>
          <div v-if="parkingAreaRows.length === 0" class="empty-state">No garages in this scope.</div>
          <div v-else class="bar-chart">
            <div v-for="row in parkingAreaRows" :key="'r-' + row.garageId" class="bar-row">
              <span class="bar-label" :title="row.areaName">{{ row.areaName }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-revenue"
                  :style="{ width: barPct(row.revenue, maxRecordedRevenue) + '%' }"
                />
              </div>
              <span class="bar-value"
                >${{
                  row.revenue.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })
                }}</span
              >
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Active reservation spots</h3>
            <p>Spots currently tied to ACTIVE reservations per facility.</p>
          </div>
          <div v-if="parkingAreaRows.length === 0" class="empty-state">No garages in this scope.</div>
          <div v-else class="bar-chart">
            <div v-for="row in parkingAreaRows" :key="'a-' + row.garageId" class="bar-row">
              <span class="bar-label" :title="row.areaName">{{ row.areaName }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-city"
                  :style="{ width: barPct(row.activeReservationSpots, maxActiveSpots) + '%' }"
                />
              </div>
              <span class="bar-value">{{ row.activeReservationSpots }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Utilization by facility</h3>
            <p>Share of each garage’s capacity that is currently occupied.</p>
          </div>
          <div v-if="parkingAreaRows.length === 0" class="empty-state">No garages in this scope.</div>
          <div v-else class="bar-chart">
            <div v-for="row in parkingAreaRows" :key="'u-' + row.garageId" class="bar-row">
              <span class="bar-label" :title="row.areaName">{{ row.areaName }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-top"
                  :style="{ width: barPct(row.capacityUtilizationPercent, 100) + '%' }"
                />
              </div>
              <span class="bar-value">{{ row.capacityUtilizationPercent.toFixed(1) }}%</span>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.analytics-shell {
  padding: 2rem clamp(1rem, 5vw, 4rem);
  max-width: 1200px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
  color: #334155;
}

.page-header {
  margin-bottom: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.header-main h1 {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 0.5rem;
  letter-spacing: -0.02em;
}

.header-main p {
  color: #64748b;
  font-size: 1rem;
  margin: 0;
}

.view-tag {
  font-size: 0.7rem;
  font-weight: 700;
  color: #3b82f6;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
  display: block;
}

.scope-header-row {
  margin-bottom: 0.35rem;
}

.scope-pill.parking {
  display: inline-block;
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
}

.btn-refresh {
  padding: 0.6rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 600;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #334155;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}

.btn-refresh:hover:not(:disabled) {
  background: #f8fafc;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.scope-toolbar {
  margin-bottom: 1.5rem;
}

.scope-toolbar-grid {
  display: grid;
  grid-template-columns: 1fr minmax(180px, 240px);
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 768px) {
  .scope-toolbar-grid {
    grid-template-columns: 1fr;
  }
}

.field-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0.35rem;
}

.field-hint {
  font-size: 0.8rem;
  color: #94a3b8;
  margin: 0 0 0.75rem;
  line-height: 1.4;
}

.input-search {
  display: block;
  width: 100%;
  padding: 0.6rem 0.75rem;
  font-size: 0.9rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #0f172a;
  box-sizing: border-box;
}

.field-warn {
  font-size: 0.8rem;
  color: #b45309;
  margin: 0.5rem 0 0;
}

.scope-meta {
  padding: 1rem;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}

.provider-suggestions {
  margin-top: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  overflow: auto;
  max-height: 260px;
}

.provider-option {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 0.9rem;
  text-align: left;
}

.provider-option:hover {
  background: #f8fafc;
}

.provider-option.active {
  background: #eef2ff;
}

.provider-option-name {
  font-weight: 800;
  color: #0f172a;
}

.provider-option-email {
  font-size: 0.75rem;
  color: #94a3b8;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.provider-empty {
  padding: 0.75rem;
  color: #94a3b8;
  font-size: 0.85rem;
  background: #f8fafc;
}

.meta-label {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #94a3b8;
  display: block;
  margin-bottom: 0.35rem;
}

.meta-value {
  font-size: 1.1rem;
  color: #0f172a;
  display: block;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
  margin-bottom: 2rem;
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}

.kpi-card-simple {
  padding: 1.25rem;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.kpi-card-simple .label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.5rem;
}

.kpi-card-simple .value {
  font-size: 1.65rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  margin-bottom: 0.5rem;
}

.kpi-card-simple .subtext {
  font-size: 0.78rem;
  color: #94a3b8;
  line-height: 1.35;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

.panel-span-2 {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .panel-span-2 {
    grid-column: 1;
  }
}

.panel-card-clean {
  padding: 1.5rem;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  background: #fff;
}

.panel-card-clean h3 {
  font-size: 1.1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 1rem;
}

.panel-header p {
  font-size: 0.9rem;
  color: #64748b;
  margin: -0.5rem 0 1rem;
  line-height: 1.45;
}

.stat-list {
  display: flex;
  flex-direction: column;
}

.clean-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 0.75rem 0;
  font-size: 0.9rem;
  border-bottom: 1px solid #f8fafc;
}

.clean-row:last-child {
  border-bottom: none;
}

.clean-row .name {
  color: #475569;
  font-weight: 600;
}

.clean-row .qty {
  font-weight: 700;
  color: #0f172a;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.addr-snippet {
  font-weight: 500;
  color: #94a3b8;
  font-size: 0.82rem;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.bar-row {
  display: grid;
  grid-template-columns: minmax(0, 120px) 1fr auto;
  gap: 0.65rem;
  align-items: center;
  font-size: 0.85rem;
}

.bar-label {
  color: #475569;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  height: 10px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.35s ease;
  min-width: 2px;
}

.bar-volume {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

.bar-revenue {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.bar-city {
  background: linear-gradient(90deg, #8b5cf6, #a78bfa);
}

.bar-top {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.bar-value {
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  min-width: 3.5rem;
  text-align: right;
}

.state-msg {
  text-align: center;
  padding: 5rem;
  color: #64748b;
}

.empty-state {
  padding: 2rem 1rem;
  text-align: center;
  color: #94a3b8;
  font-size: 0.9rem;
  background: #f8fafc;
  border-radius: 8px;
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

.pulse {
  animation: pulse 2s infinite;
}

.state-msg.error {
  color: #be123c;
}
</style>
