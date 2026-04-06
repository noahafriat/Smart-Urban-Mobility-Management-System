<script setup lang="ts">
/**
 * Parking & Infrastructure Analytics (Stationary garages)
 * Visible to: City Admin, System Admin — scope by city facilities or parking provider (like rental analytics).
 */
import { computed, onMounted, ref } from 'vue'
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

const scopeLabel = computed(() => {
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

function availabilityColor(available: number, total: number): string {
  const percent = total === 0 ? 0 : (available / total) * 100
  if (percent > 40) return 'green'
  if (percent > 15) return 'orange'
  return 'red'
}

function pctFull(g: { totalSpaces: number; availableSpaces: number }): string {
  if (!g.totalSpaces) return '—'
  return (((g.totalSpaces - g.availableSpaces) / g.totalSpaces) * 100).toFixed(0)
}

async function refreshParking() {
  const g = selectedGarageScopeId.value
  await store.fetchParking(undefined, g === '' ? undefined : g)
}

function selectGarageScope(scopeId: string) {
  selectedGarageScopeId.value = scopeId
  syncSearchLabelToScope(scopeId)
  void store.fetchParking(undefined, scopeId === '' ? undefined : scopeId)
}

function onProviderSearchInput(ev: Event) {
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

onMounted(async () => {
  await loadUsers()
  await refreshParking()
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
        <p>Real-time capacity for garages — filter by city-owned facilities or a parking operator.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" :disabled="store.loading" @click="refreshParking()">
          <span v-if="store.loading">Refreshing...</span>
          <span v-else>↻ Sync Sensors</span>
        </button>
      </div>
    </header>

    <div v-if="store.loading && !store.parkingData" class="state-msg pulse">
      Interrogating infrastructure sensors...
    </div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <main v-else-if="store.parkingData" class="analytics-body">
      <section v-if="auth.isAdmin" class="scope-toolbar panel-card-clean">
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

      <!-- ── Capacity KPIs ── -->
      <section class="kpi-banner">
        <div class="kpi-grid">
          <div class="kpi-card-simple">
            <span class="label">Garages in scope</span>
            <strong class="value">{{ store.parkingData.totalGarages }}</strong>
            <span class="subtext">Facilities counted</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Total capacity</span>
            <strong class="value">{{ store.parkingData.totalGarageSpaces }}</strong>
            <span class="subtext">Spots monitored</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Current availability</span>
            <strong class="value">{{ store.parkingData.totalAvailableGarageSpaces }}</strong>
            <span class="subtext">Open slots</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Occupancy (scope)</span>
            <strong class="value">{{ store.parkingData.garageUtilizationRate }}</strong>
            <span class="subtext">Load in current filter</span>
          </div>
        </div>
      </section>

      <div class="main-grid">
        <div class="ops-column">
          <section class="panel-card-clean">
            <div class="panel-header">
              <h3>Facility capacity</h3>
              <p>Live occupancy for garages in the selected scope.</p>
            </div>
            <div class="garage-list-clean">
              <div
                v-for="garage in store.parkingData.garageDetails"
                :key="`${selectedGarageScopeId}-${garage.id}`"
                class="garage-row"
              >
                <div class="info">
                  <span class="name">{{ garage.name }}</span>
                  <span class="meta">{{ garage.availableSpaces }} / {{ garage.totalSpaces }} spots available</span>
                </div>
                <div
                  class="load-badge"
                  :class="availabilityColor(garage.availableSpaces, garage.totalSpaces)"
                >
                  {{ pctFull(garage) }}% Full
                </div>
              </div>
              <div v-if="store.parkingData.garageDetails.length === 0" class="empty-state">
                No garages in this scope.
              </div>
            </div>
          </section>
        </div>
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
  flex-wrap: wrap;
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

.header-actions {
  display: flex;
  align-items: center;
}

.btn-refresh {
  padding: 0.6rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 600;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: 0.2s;
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

.main-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2rem;
}

.panel-card-clean {
  padding: 1.5rem;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  margin-bottom: 1.5rem;
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

.garage-list-clean {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.garage-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #f8fafc;
}

.garage-row:last-child {
  border-bottom: none;
}

.garage-row .info {
  display: flex;
  flex-direction: column;
}

.garage-row .name {
  font-weight: 700;
  color: #0f172a;
}

.garage-row .meta {
  font-size: 0.8rem;
  color: #64748b;
}

.load-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
}

.load-badge.green {
  background: #f0fdf4;
  color: #10b981;
}

.load-badge.orange {
  background: #fffbeb;
  color: #f59e0b;
}

.load-badge.red {
  background: #fef2f2;
  color: #ef4444;
}

.state-msg {
  text-align: center;
  padding: 5rem;
  color: #64748b;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #94a3b8;
  font-style: italic;
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
