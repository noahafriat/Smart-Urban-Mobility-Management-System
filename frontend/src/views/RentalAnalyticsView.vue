<script setup lang="ts">
/**
 * Rental & Market Analytics
 * Visible to: City Admin, System Admin, Service Providers
 */
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../api'
import { useAnalyticsStore } from '../stores/analytics'
import { useAuthStore } from '../stores/auth'

interface UserOption {
  id: string
  name: string
  email: string
  role?: string
  // Used only for provider-scoping in admin UI
  hasCar?: boolean
  hasScooter?: boolean
}

const store = useAnalyticsStore()
const auth = useAuthStore()

const users = ref<UserOption[]>([])
const usersLoadError = ref<string | null>(null)

const providers = ref<UserOption[]>([])
const providerSearch = ref('')
const selectedProviderId = ref('')
const providersLoadError = ref<string | null>(null)

const loggedProviderHasCar = ref(true)
const loggedProviderHasScooter = ref(true)

const filteredProviders = computed(() => {
  const q = providerSearch.value.trim().toLowerCase()

  let list = [...providers.value]

  list.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
  if (!q) return list

  return list.filter(
    (p) => p.name.toLowerCase().includes(q) || (p.email ?? '').toLowerCase().includes(q),
  )
})

const selectedProvider = computed(() => providers.value.find((p) => p.id === selectedProviderId.value) ?? null)

watch(
  () => selectedProviderId.value,
  (id) => {
    const p = providers.value.find((x) => x.id === id) ?? null
    providerSearch.value = p ? p.name : ''
  },
)

function isScooterZoneKey(zoneKey: string): boolean {
  // Parking keys look like: "City / Zone (Scooter)" or "City / Zone (ModelName)"
  return zoneKey.trim().endsWith('(Scooter)')
}

const showCarParking = computed(() => {
  if (auth.isProvider) return loggedProviderHasCar.value
  if (!selectedProviderId.value) return true // Everyone => show both types
  const provider = providers.value.find((p) => p.id === selectedProviderId.value)
  return provider?.hasCar ?? true
})

const showScooterParking = computed(() => {
  if (auth.isProvider) return loggedProviderHasScooter.value
  if (!selectedProviderId.value) return true
  const provider = providers.value.find((p) => p.id === selectedProviderId.value)
  return provider?.hasScooter ?? true
})

const filteredParkedPerZoneEntries = computed(() => {
  const parkedPerZone = store.parkingData?.parkedPerZone ?? {}
  return Object.entries(parkedPerZone).filter(([zoneKey]) => {
    const scooter = isScooterZoneKey(zoneKey)
    return scooter ? showScooterParking.value : showCarParking.value
  })
})

const distributionRows = computed(() => {
  const r = store.rentalData
  if (!r) return []
  const keys = new Set([...Object.keys(r.rentalsByType ?? {}), ...Object.keys(r.revenueByType ?? {})])
  return Array.from(keys)
    .map((type) => ({
      type,
      volume: r.rentalsByType?.[type] ?? 0,
      revenue: r.revenueByType?.[type] ?? 0,
    }))
    .sort((a, b) => b.volume - a.volume)
})

const maxDistributionVolume = computed(() =>
  distributionRows.value.reduce((m, row) => Math.max(m, row.volume), 0),
)
const maxDistributionRevenue = computed(() =>
  distributionRows.value.reduce((m, row) => Math.max(m, row.revenue), 0),
)

const cityRows = computed(() => {
  const r = store.rentalData?.rentalsByCity
  if (!r) return []
  return Object.entries(r)
    .map(([city, count]) => ({ city, count }))
    .sort((a, b) => b.count - a.count)
})

const maxCityCount = computed(() =>
  cityRows.value.reduce((m, row) => Math.max(m, row.count), 0),
)

const topVehicleRows = computed(() => {
  const r = store.rentalData?.topVehicles
  if (!r) return []
  return Object.entries(r)
    .map(([code, count]) => ({ code, count }))
    .sort((a, b) => b.count - a.count)
})

const maxTopVehicle = computed(() =>
  topVehicleRows.value.reduce((m, row) => Math.max(m, row.count), 0),
)

const scopeLabel = computed(() => {
  if (auth.isProvider) return 'Your fleet'

  const provider = providers.value.find((x) => x.id === selectedProviderId.value)
  if (provider) return provider.name
  return 'Everyone'
})

function barPct(value: number, max: number): number {
  if (!max || !Number.isFinite(max)) return 0
  return Math.min(100, (value / max) * 100)
}


function formatZoneName(raw: string): string {
  return raw.split(' / ').pop() || raw
}

async function refreshRentals() {
  if (auth.isProvider) {
    // Mobility providers see only their own fleet stats
    const providerId = auth.user?.id
    await store.fetchRentals(providerId, undefined)
    await store.fetchParking(providerId)
    return
  }

  // City/Sys admins can drill into a provider (or view the platform)
  const providerId = selectedProviderId.value || undefined
  await store.fetchRentals(providerId, undefined)
  await store.fetchParking(providerId)
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
    }))
  } catch {
    users.value = []
    usersLoadError.value = 'Could not load user list for rider scope.'
  }
}

async function loadProviders() {
  providersLoadError.value = null

  try {
    const mobilityProviders = users.value.filter((u) => u.role === 'MOBILITY_PROVIDER')
    const enriched = await Promise.all(
      mobilityProviders.map(async (p) => {
        try {
          const res = await api.get(`/vehicles/provider/${p.id}/summary`)
          const byType = res.data?.byType as Record<string, number> | undefined
          const hasScooter = (byType?.['Scooter'] ?? 0) > 0
          const hasCar = Object.entries(byType ?? {}).some(([k, v]) => k !== 'Scooter' && (v ?? 0) > 0)
          return { ...p, hasCar, hasScooter }
        } catch {
          // If summary fails, still include provider in "All providers" mode.
          return { ...p, hasCar: false, hasScooter: false }
        }
      }),
    )

    providers.value = enriched
  } catch {
    providers.value = []
    providersLoadError.value = 'Could not load provider fleet summaries.'
  }
}

async function loadLoggedProviderTypes(providerId: string) {
  // Used when the logged-in user is a mobility provider so we can scope parking availability
  // to only the vehicle types they actually offer.
  try {
    const res = await api.get(`/vehicles/provider/${providerId}/summary`)
    const byType = res.data?.byType as Record<string, number> | undefined
    loggedProviderHasScooter.value = (byType?.['Scooter'] ?? 0) > 0
    loggedProviderHasCar.value = Object.entries(byType ?? {}).some(([k, v]) => k !== 'Scooter' && (v ?? 0) > 0)
  } catch {
    loggedProviderHasCar.value = true
    loggedProviderHasScooter.value = true
  }
}

function onProviderScopeChange() {
  void refreshRentals()
}

function selectProvider(providerId: string) {
  selectedProviderId.value = providerId
  void refreshRentals()
}

function onProviderSearchInput() {
  if (!providerSearch.value.trim()) {
    selectedProviderId.value = ''
    void refreshRentals()
  }
}

onMounted(async () => {
  // Mobility providers: locked to their own fleet stats (no rider/provider pickers)
  if (auth.isProvider) {
    const providerId = auth.user?.id
    if (providerId) await loadLoggedProviderTypes(providerId)
    await refreshRentals()
    return
  }

  // City/Sys admins: provider picker
  await loadUsers()
  await loadProviders()
  await refreshRentals()
})
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <div class="scope-header">
          <span class="scope-pill provider">{{ auth.isProvider ? 'Your fleet' : 'Multi-provider' }}</span>
        </div>
        <h1>Rental Analytics & Revenue</h1>
        <p>Rental volume, revenue, and geography — filter by provider or view the full platform.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" :disabled="store.loading" @click="refreshRentals()">
          <span v-if="store.loading">Refreshing...</span>
          <span v-else>↻ Sync Fleet</span>
        </button>
      </div>
    </header>

    <div v-if="store.loading && !store.rentalData" class="state-msg pulse">
      Compiling rental history and financial data...
    </div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <main v-else-if="store.rentalData" class="analytics-body">
      <section class="scope-toolbar panel-card-clean">
        <div class="scope-toolbar-grid">
          <div class="scope-field">
            <template v-if="auth.isAdmin">
              <label class="field-label" for="provider-search">Provider scope</label>
              <p class="field-hint">
                Choose a mobility provider to scope rentals and neighborhood availability (leave blank for Everyone).
              </p>

              <input
                id="provider-search"
                v-model="providerSearch"
                type="search"
                class="input-search"
                placeholder="Search provider by name or email…"
                autocomplete="off"
                @input="onProviderSearchInput"
              />

              <div v-if="providers.length > 0" class="provider-suggestions" role="listbox">
                <button
                  type="button"
                  class="provider-option"
                  :class="{ active: !selectedProviderId }"
                  @click="selectProvider('')"
                >
                  Everyone
                </button>
                <button
                  v-for="p in filteredProviders.slice(0, 8)"
                  :key="p.id"
                  type="button"
                  class="provider-option"
                  :class="{ active: selectedProviderId === p.id }"
                  @click="selectProvider(p.id)"
                >
                  <span class="provider-option-name">{{ p.name }}</span>
                  <span class="provider-option-email">{{ p.email }}</span>
                </button>
                <div v-if="filteredProviders.length === 0" class="provider-empty">
                  No matching providers.
                </div>
              </div>

              <p v-if="providersLoadError" class="field-warn">{{ providersLoadError }}</p>
            </template>

            <template v-else>
              <label class="field-label">Your scope</label>
              <p class="field-hint">Mobility providers can only view analytics for their own fleet.</p>
            </template>
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
            <span class="label">Total rentals</span>
            <strong class="value">{{ store.rentalData.totalRentals }}</strong>
            <span class="subtext">{{ store.rentalData.activeRentals }} active in scope</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Revenue (completed / paid)</span>
            <strong class="value">${{ store.rentalData.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</strong>
            <span class="subtext">From finished trips in scope</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Fleet Activity</span>
            <strong class="value">{{ ((store.rentalData.activeRentals / (store.rentalData.fleetSize || 1)) * 100).toFixed(1) }}%</strong>
            <span class="subtext">Of active fleet in scope</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Billing rate</span>
            <strong class="value">{{ ((store.rentalData.paidRentals / (store.rentalData.completedRentals || 1)) * 100).toFixed(0) }}%</strong>
            <span class="subtext">Paid vs completed ({{ store.rentalData.paidRentals }} / {{ store.rentalData.completedRentals }})</span>
          </div>
        </div>
      </section>

      <div class="charts-grid">
        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Volume by vehicle type</h3>
            <p>How many rentals each vehicle category has in the selected scope.</p>
          </div>
          <div v-if="distributionRows.length === 0" class="empty-state">No rental data in this scope.</div>
          <div v-else class="bar-chart">
            <div v-for="row in distributionRows" :key="row.type" class="bar-row">
              <span class="bar-label" :title="row.type">{{ row.type }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-volume"
                  :style="{ width: barPct(Number(row.volume), maxDistributionVolume) + '%' }"
                />
              </div>
              <span class="bar-value">{{ row.volume }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Revenue by vehicle type</h3>
            <p>Total revenue by vehicle category from completed and paid rentals in this scope.</p>
          </div>
          <div v-if="distributionRows.length === 0" class="empty-state">No rental data in this scope.</div>
          <div v-else class="bar-chart">
            <div v-for="row in distributionRows" :key="'r-' + row.type" class="bar-row">
              <span class="bar-label" :title="row.type">{{ row.type }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-revenue"
                  :style="{ width: barPct(row.revenue, maxDistributionRevenue) + '%' }"
                />
              </div>
              <span class="bar-value">${{ row.revenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Rentals by city</h3>
            <p>Where vehicles were based when rented.</p>
          </div>
          <div v-if="cityRows.length === 0" class="empty-state">No geographic breakdown.</div>
          <div v-else class="bar-chart">
            <div v-for="row in cityRows" :key="row.city" class="bar-row">
              <span class="bar-label" :title="row.city">{{ row.city }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-city"
                  :style="{ width: barPct(row.count, maxCityCount) + '%' }"
                />
              </div>
              <span class="bar-value">{{ row.count }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Top vehicles</h3>
            <p>Most frequently rented vehicle codes in scope.</p>
          </div>
          <div v-if="topVehicleRows.length === 0" class="empty-state">No ranked vehicles.</div>
          <div v-else class="bar-chart">
            <div v-for="row in topVehicleRows" :key="row.code" class="bar-row top-vehicles">
              <span class="bar-label mono" :title="row.code">{{ row.code }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill bar-top"
                  :style="{ width: barPct(row.count, maxTopVehicle) + '%' }"
                />
              </div>
              <span class="bar-value">{{ row.count }}</span>
            </div>
          </div>
        </section>

        <section v-if="store.parkingData" class="panel-card-clean panel-span-2">
          <div class="panel-header">
            <h3>Neighborhood availability</h3>
            <p>
              How many vehicles are currently available near each zone (based on vehicles marked `AVAILABLE` at their
              latest location). When you pick a provider, this is scoped to that provider; otherwise it shows
              everyone.
            </p>
          </div>
          <div class="stat-list">
            <div v-for="[zone, count] in filteredParkedPerZoneEntries" :key="zone" class="clean-row">
              <span class="name">{{ formatZoneName(zone) }}</span>
              <strong class="qty">
                {{ count }} / {{ store.parkingData.totalPerZone[zone] }} available
              </strong>
            </div>
            <div v-if="filteredParkedPerZoneEntries.length === 0" class="empty-state">
              No neighborhood availability for the selected provider types.
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

.scope-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.scope-pill {
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.2rem 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  color: #64748b;
}

.scope-pill.provider {
  color: #10b981;
  border-color: #bbf7d0;
  background: #f0fdf4;
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

.input-search,
.select-rider {
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

.select-rider {
  margin-top: 0.5rem;
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

.divider-space {
  height: 0.9rem;
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
  margin-bottom: 0.5rem;
}

.meta-trace {
  font-size: 0.7rem;
  color: #94a3b8;
  font-family: ui-monospace, monospace;
  word-break: break-all;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.5rem;
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

.empty-state {
  padding: 2rem 1rem;
  text-align: center;
  color: #94a3b8;
  font-size: 0.9rem;
  background: #f8fafc;
  border-radius: 8px;
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

.bar-label.mono {
  font-family: ui-monospace, monospace;
  font-size: 0.8rem;
}

.bar-row.top-vehicles {
  grid-template-columns: minmax(0, 160px) 1fr auto;
}

.bar-row.top-vehicles .bar-label.mono {
  font-size: 0.72rem;
}

.bar-row.top-vehicles .bar-label {
  overflow: visible;
  text-overflow: clip;
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

.data-table-clean {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.data-table-clean th {
  text-align: left;
  padding: 0.75rem;
  color: #94a3b8;
  font-size: 0.75rem;
  text-transform: uppercase;
  border-bottom: 1px solid #f1f5f9;
}

.data-table-clean td {
  padding: 0.85rem 0.75rem;
  border-bottom: 1px solid #f8fafc;
}

.data-table-clean .name {
  font-weight: 700;
  color: #0f172a;
}

.data-table-clean .qty {
  color: #475569;
}

.clean-row {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  font-size: 0.9rem;
  border-bottom: 1px solid #f8fafc;
}

.clean-row .name {
  color: #475569;
  font-weight: 500;
}

.clean-row .qty {
  font-weight: 700;
  color: #0f172a;
}

.state-msg {
  text-align: center;
  padding: 5rem;
  color: #64748b;
}

.state-msg.error {
  color: #b91c1c;
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
</style>
