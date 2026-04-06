<script setup lang="ts">
/**
 * Transit Usage Analytics
 * Visible to: System Admin and City Admin
 */
import { computed, onMounted } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()

const fleetTotal = computed(() => {
  if (!store.transitData) return 0
  return store.transitData.fleetAvailable + store.transitData.fleetInUse + store.transitData.fleetMaintenance
})

function barPct(value: number, max: number): number {
  if (!max || !Number.isFinite(max)) return 0
  return Math.min(100, (value / max) * 100)
}

const tripsByTypeRows = computed(() => {
  const m = store.transitData?.tripsByType ?? {}
  return Object.entries(m)
    .map(([type, count]) => ({ type, count }))
    .sort((a, b) => b.count - a.count)
})

const maxTripsByType = computed(() =>
  tripsByTypeRows.value.reduce((m, row) => Math.max(m, row.count), 0),
)

const tripsByCityRows = computed(() => {
  const m = store.transitData?.tripsByCity ?? {}
  return Object.entries(m)
    .map(([city, count]) => ({ city, count }))
    .sort((a, b) => b.count - a.count)
})

const maxTripsByCity = computed(() =>
  tripsByCityRows.value.reduce((m, row) => Math.max(m, row.count), 0),
)

const revenueByCityRows = computed(() => {
  const m = store.transitData?.revenueByCity ?? {}
  return Object.entries(m)
    .map(([city, revenue]) => ({ city, revenue }))
    .sort((a, b) => b.revenue - a.revenue)
})

const maxRevenueByCity = computed(() =>
  revenueByCityRows.value.reduce((m, row) => Math.max(m, row.revenue), 0),
)

onMounted(() => {
  store.fetchTransit()
  store.fetchGateway()
})
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <span class="view-tag">City Mobility</span>
        <h1>Transit Analytics</h1>
        <p>A cleaner snapshot of ridership, fleet status, revenue, and gateway health.</p>
      </div>
    </header>

    <div v-if="store.loading && !store.transitData" class="state-msg pulse">
      Analyzing city-wide mobility patterns...
    </div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <main v-else-if="store.transitData" class="analytics-body">
      
      <!-- ── Primary Mobility KPIs ── -->
      <section class="kpi-banner">
        <div class="kpi-grid">
          <div class="kpi-card-simple">
            <span class="label">Total Trips Today</span>
            <strong class="value">{{ store.transitData.todayTrips }}</strong>
            <span class="subtext">{{ store.transitData.activeRentals }} live rentals</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Gross Revenue</span>
            <strong class="value">${{ store.transitData.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 0 }) }}</strong>
            <span class="subtext">Accumulated value</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">System Reach</span>
            <strong class="value">{{ store.transitData.totalUsers }}</strong>
            <span class="subtext">Registered citizens</span>
          </div>
        </div>
      </section>

      <div class="charts-grid">
        <section class="panel-card-clean panel-span-2">
          <div class="panel-header">
            <h3>Fleet status</h3>
            <p>Current split of available, in-use, and maintenance vehicles.</p>
          </div>
          <div class="fleet-health-meter">
            <div class="multi-bar-smooth">
              <div class="bar-seg g" :style="{ width: barPct(store.transitData.fleetAvailable, fleetTotal) + '%' }" title="Available"></div>
              <div class="bar-seg b" :style="{ width: barPct(store.transitData.fleetInUse, fleetTotal) + '%' }" title="In Use"></div>
              <div class="bar-seg o" :style="{ width: barPct(store.transitData.fleetMaintenance, fleetTotal) + '%' }" title="Maintenance"></div>
            </div>
            <div class="health-legend">
              <span><i class="dot g"></i> Available: {{ store.transitData.fleetAvailable }}</span>
              <span><i class="dot b"></i> In Use: {{ store.transitData.fleetInUse }}</span>
              <span><i class="dot o"></i> Maintenance: {{ store.transitData.fleetMaintenance }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Trips by type</h3>
          </div>
          <div v-if="tripsByTypeRows.length === 0" class="empty-state">No trip type breakdown.</div>
          <div v-else class="bar-chart">
            <div v-for="row in tripsByTypeRows" :key="row.type" class="bar-row">
              <span class="bar-label" :title="row.type">{{ row.type }}</span>
              <div class="bar-track">
                <div class="bar-fill bar-type" :style="{ width: barPct(row.count, maxTripsByType) + '%' }" />
              </div>
              <span class="bar-value">{{ row.count }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Trips by city</h3>
          </div>
          <div v-if="tripsByCityRows.length === 0" class="empty-state">No city trip breakdown.</div>
          <div v-else class="bar-chart">
            <div v-for="row in tripsByCityRows" :key="row.city" class="bar-row">
              <span class="bar-label" :title="row.city">{{ row.city }}</span>
              <div class="bar-track">
                <div class="bar-fill bar-city" :style="{ width: barPct(row.count, maxTripsByCity) + '%' }" />
              </div>
              <span class="bar-value">{{ row.count }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>Revenue by city</h3>
          </div>
          <div v-if="revenueByCityRows.length === 0" class="empty-state">No city revenue breakdown.</div>
          <div v-else class="bar-chart">
            <div v-for="row in revenueByCityRows" :key="row.city" class="bar-row">
              <span class="bar-label" :title="row.city">{{ row.city }}</span>
              <div class="bar-track">
                <div class="bar-fill bar-revenue" :style="{ width: barPct(row.revenue, maxRevenueByCity) + '%' }" />
              </div>
              <span class="bar-value">${{ row.revenue.toLocaleString(undefined, { maximumFractionDigits: 0 }) }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card-clean">
          <div class="panel-header">
            <h3>BIXI gateway</h3>
            <p>Live external feed health and responsiveness.</p>
          </div>
          <div v-if="store.gatewayLoading" class="mini-loader">Requesting gateway...</div>
          <div v-else-if="store.gatewayData" class="brief-list">
            <div class="brief-item">
              <span class="l">Status</span>
              <strong
                class="v"
                :class="{
                  'green-text': store.gatewayData.status === 'UP',
                  'orange-text': store.gatewayData.status === 'DEGRADED',
                  'red-text': store.gatewayData.status === 'DOWN',
                }"
              >
                {{ store.gatewayData.status }}
              </strong>
            </div>
            <div class="brief-item">
              <span class="l">Total stations</span>
              <strong class="v">{{ store.gatewayData.totalStations ?? '-' }}</strong>
            </div>
            <div class="brief-item">
              <span class="l">Bikes available</span>
              <strong class="v">{{ store.gatewayData.bikesAvailable ?? '-' }}</strong>
            </div>
            <div class="brief-item">
              <span class="l">Latency</span>
              <strong class="v">{{ store.gatewayData.responseMs }}ms</strong>
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

.page-header { margin-bottom: 2rem; }
.header-main h1 { font-size: 2rem; font-weight: 800; color: #0f172a; margin: 0 0 0.5rem; letter-spacing: -0.02em; }
.header-main p { color: #64748b; font-size: 1rem; margin: 0; }
.view-tag { font-size: 0.7rem; font-weight: 700; color: #3b82f6; text-transform: uppercase; margin-bottom: 0.5rem; display: block; }

/* ── KPIs ── */
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.25rem; margin-bottom: 1.5rem; }
.kpi-card-simple { padding: 1.5rem; border: 1px solid #e2e8f0; border-radius: 12px; display: flex; flex-direction: column; background: #fff; }
.kpi-card-simple .label { font-size: 0.8rem; font-weight: 600; color: #64748b; margin-bottom: 0.5rem; }
.kpi-card-simple .value { font-size: 1.95rem; font-weight: 800; color: #0f172a; line-height: 1; margin-bottom: 0.5rem; }
.kpi-card-simple .subtext { font-size: 0.8rem; color: #94a3b8; }

/* ── Layout ── */
.charts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.25rem; }
.panel-span-2 { grid-column: 1 / -1; }
.panel-card-clean { padding: 1.5rem; border: 1px solid #f1f5f9; border-radius: 12px; background: #fff; }
.panel-card-clean h3 { font-size: 1.1rem; font-weight: 700; color: #0f172a; margin: 0 0 1.25rem; }
.panel-card-clean p { font-size: 0.9rem; color: #64748b; margin: -1rem 0 1.5rem; }

/* ── Fleet Meter ── */
.multi-bar-smooth { height: 8px; background: #f1f5f9; border-radius: 4px; display: flex; overflow: hidden; margin-bottom: 1rem; }
.bar-seg { height: 100%; transition: 0.6s ease; }
.bar-seg.g { background: #10b981; }
.bar-seg.b { background: #3b82f6; }
.bar-seg.o { background: #f59e0b; }

.health-legend { display: flex; gap: 1.5rem; font-size: 0.85rem; color: #64748b; }
.health-legend .dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; margin-right: 6px; vertical-align: middle; }
.dot.g { background: #10b981; }
.dot.b { background: #3b82f6; }
.dot.o { background: #f59e0b; }

/* ── Bar Charts ── */
.bar-chart { display: flex; flex-direction: column; gap: 0.65rem; }
.bar-row { display: grid; grid-template-columns: minmax(0, 120px) 1fr auto; gap: 0.65rem; align-items: center; font-size: 0.85rem; }
.bar-label { color: #475569; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track { height: 10px; background: #f1f5f9; border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; transition: width 0.35s ease; min-width: 2px; }
.bar-type { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.bar-city { background: linear-gradient(90deg, #8b5cf6, #a78bfa); }
.bar-revenue { background: linear-gradient(90deg, #10b981, #34d399); }
.bar-value { font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; min-width: 3.5rem; text-align: right; }

/* ── Sidebar ── */
.brief-list { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem; }
.brief-item { display: flex; justify-content: space-between; font-size: 0.95rem; }
.brief-item .l { color: #64748b; }
.brief-item .v { font-weight: 700; color: #0f172a; }
.green-text { color: #10b981; }
.orange-text { color: #f59e0b; }
.red-text { color: #ef4444; }

.mini-loader { font-size: 0.8rem; color: #64748b; padding: 1rem 0; }
.empty-state { text-align: center; padding: 2rem; color: #94a3b8; font-style: italic; }

.state-msg { text-align: center; padding: 5rem; color: #64748b; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.pulse { animation: pulse 2s infinite; }

@media (max-width: 1024px) {
  .charts-grid { grid-template-columns: 1fr; }
  .panel-span-2 { grid-column: 1; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .kpi-grid { grid-template-columns: 1fr; }
  .bar-row { grid-template-columns: minmax(0, 90px) 1fr auto; }
}
</style>
