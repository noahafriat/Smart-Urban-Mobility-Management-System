<script setup lang="ts">
/**
 * Transit Usage Analytics
 * Visible to: City Admin, System Admin
 */
import { onMounted } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()

onMounted(() => {
  store.fetchTransit()
  store.fetchGateway()
})
</script>

<template>
  <div class="analytics-view">
    <header class="page-header">
      <h1>Transit Usage Analytics</h1>
      <p>Platform-wide ridership patterns and fleet health.</p>
    </header>

    <div v-if="store.loading" class="state-msg pulse">Loading analytics…</div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <template v-else-if="store.transitData">

      <!-- ══ RENTAL BUSINESS METRICS (Demo Panel 1) ══ -->
      <section class="demo-panel rental-panel">
        <div class="demo-panel-header">
          <div>
            <span class="demo-tag">Rental Analytics</span>
            <h2>Live Rental Business Metrics</h2>
            <p>Domain layer processing raw rental events into actionable metrics.</p>
          </div>
        </div>
        <div class="demo-kpi-row">
          <div class="demo-kpi orange">
            <span class="demo-kpi-label">Active Rentals</span>
            <strong class="demo-kpi-value">{{ store.transitData.activeRentals }}</strong>
            <span class="demo-kpi-sub">vehicles in use right now</span>
          </div>
          <div class="demo-kpi blue">
            <span class="demo-kpi-label">Today's Trips</span>
            <strong class="demo-kpi-value">{{ store.transitData.todayTrips }}</strong>
            <span class="demo-kpi-sub">completed trips today</span>
          </div>
          <div class="demo-kpi green" v-if="Object.keys(store.transitData.revenueByCity).length > 0">
            <span class="demo-kpi-label">Revenue per City</span>
            <div class="city-rev-list">
              <div v-for="(rev, city) in store.transitData.revenueByCity" :key="city" class="city-rev-row">
                <span>{{ city }}</span>
                <strong>${{ rev.toFixed(2) }}</strong>
              </div>
            </div>
          </div>
          <div class="demo-kpi green" v-else>
            <span class="demo-kpi-label">Revenue per City</span>
            <strong class="demo-kpi-value">—</strong>
            <span class="demo-kpi-sub">complete a rental to see data</span>
          </div>
        </div>
      </section>

      <!-- ══ BIXI GATEWAY HEALTH (Demo Panel 2) ══ -->
      <section class="demo-panel gateway-panel">
        <div class="demo-panel-header">
          <div>
            <span class="demo-tag gateway">External Service</span>
            <h2>BIXI Montréal Gateway Health</h2>
            <p>Live probe of the BIXI GBFS API — measures connectivity and data sync.</p>
          </div>
          <button class="refresh-gateway-btn" :disabled="store.gatewayLoading" @click="store.fetchGateway()">
            {{ store.gatewayLoading ? 'Checking…' : '↻ Re-check' }}
          </button>
        </div>

        <div v-if="store.gatewayLoading" class="gateway-loading">Probing BIXI API…</div>
        <div v-else-if="store.gatewayData" class="gateway-body">
          <div class="status-indicator" :class="store.gatewayData.status.toLowerCase()">
            <span class="status-dot"></span>
            <span class="status-text">{{ store.gatewayData.status }}</span>
          </div>
          <div class="gateway-stats">
            <div class="gw-stat">
              <span class="gw-stat-label">Response Time</span>
              <strong class="gw-stat-value" :class="store.gatewayData.responseMs > 1000 ? 'slow' : ''">
                {{ store.gatewayData.responseMs }} ms
              </strong>
            </div>
            <div class="gw-stat" v-if="store.gatewayData.totalStations">
              <span class="gw-stat-label">Stations Synced</span>
              <strong class="gw-stat-value">{{ store.gatewayData.totalStations }}</strong>
            </div>
            <div class="gw-stat" v-if="store.gatewayData.activeStations">
              <span class="gw-stat-label">Active Stations</span>
              <strong class="gw-stat-value">{{ store.gatewayData.activeStations }}</strong>
            </div>
            <div class="gw-stat" v-if="store.gatewayData.bikesAvailable">
              <span class="gw-stat-label">Bikes Available</span>
              <strong class="gw-stat-value">{{ store.gatewayData.bikesAvailable }}</strong>
            </div>
          </div>
          <div v-if="store.gatewayData.syncedAt" class="synced-at">
            Last synced: {{ new Date(store.gatewayData.syncedAt).toLocaleTimeString() }}
          </div>
          <div v-if="store.gatewayData.error" class="gw-error">{{ store.gatewayData.error }}</div>
        </div>
      </section>
      <!-- KPI Row -->
      <div class="kpi-grid">
        <div class="kpi-card blue">
          <span class="kpi-label">Total Trips</span>
          <strong class="kpi-value">{{ store.transitData.totalTrips }}</strong>
        </div>
        <div class="kpi-card green">
          <span class="kpi-label">Total Revenue</span>
          <strong class="kpi-value">${{ store.transitData.totalRevenue.toFixed(2) }}</strong>
        </div>
        <div class="kpi-card purple">
          <span class="kpi-label">Avg Trip Duration</span>
          <strong class="kpi-value">{{ store.transitData.avgTripDurationMinutes }} min</strong>
        </div>
        <div class="kpi-card slate">
          <span class="kpi-label">Registered Users</span>
          <strong class="kpi-value">{{ store.transitData.totalUsers }}</strong>
        </div>
      </div>

      <!-- Fleet Status -->
      <section class="section-card">
        <h2>Fleet Availability</h2>
        <div class="bar-group">
          <div class="bar-row">
            <span>Available</span>
            <div class="bar-track">
              <div class="bar-fill green" :style="{ width: barWidth(store.transitData.fleetAvailable, store.transitData.fleetAvailable + store.transitData.fleetInUse + store.transitData.fleetMaintenance) }"></div>
            </div>
            <span class="bar-count">{{ store.transitData.fleetAvailable }}</span>
          </div>
          <div class="bar-row">
            <span>In Use</span>
            <div class="bar-track">
              <div class="bar-fill blue" :style="{ width: barWidth(store.transitData.fleetInUse, store.transitData.fleetAvailable + store.transitData.fleetInUse + store.transitData.fleetMaintenance) }"></div>
            </div>
            <span class="bar-count">{{ store.transitData.fleetInUse }}</span>
          </div>
          <div class="bar-row">
            <span>Maintenance</span>
            <div class="bar-track">
              <div class="bar-fill orange" :style="{ width: barWidth(store.transitData.fleetMaintenance, store.transitData.fleetAvailable + store.transitData.fleetInUse + store.transitData.fleetMaintenance) }"></div>
            </div>
            <span class="bar-count">{{ store.transitData.fleetMaintenance }}</span>
          </div>
        </div>
      </section>

      <div class="two-col">
        <!-- Trips by Vehicle Type -->
        <section class="section-card">
          <h2>Trips by Vehicle Type</h2>
          <div v-if="Object.keys(store.transitData.tripsByType).length === 0" class="empty-msg">No completed trips yet.</div>
          <div v-else class="bar-group">
            <div v-for="(count, type) in store.transitData.tripsByType" :key="type" class="bar-row">
              <span>{{ type }}</span>
              <div class="bar-track">
                <div class="bar-fill blue" :style="{ width: barWidthMap(type, store.transitData.tripsByType) }"></div>
              </div>
              <span class="bar-count">{{ count }}</span>
            </div>
          </div>
        </section>

        <!-- Trips by City -->
        <section class="section-card">
          <h2>Trips by City</h2>
          <div v-if="Object.keys(store.transitData.tripsByCity).length === 0" class="empty-msg">No completed trips yet.</div>
          <div v-else class="bar-group">
            <div v-for="(count, city) in store.transitData.tripsByCity" :key="city" class="bar-row">
              <span>{{ city }}</span>
              <div class="bar-track">
                <div class="bar-fill purple" :style="{ width: barWidthMap(city, store.transitData.tripsByCity) }"></div>
              </div>
              <span class="bar-count">{{ count }}</span>
            </div>
          </div>
        </section>
      </div>

      <!-- Observer Event Log from Singleton AnalyticsEngine -->
      <section class="section-card">
        <h2>System Event Log</h2>
        <div v-if="store.transitData.recentEvents.length === 0" class="empty-msg">No events recorded yet. Complete a rental to populate this.</div>
        <ul v-else class="event-log">
          <li v-for="(evt, i) in [...store.transitData.recentEvents].reverse()" :key="i">{{ evt }}</li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script lang="ts">
function barWidth(value: number, total: number): string {
  if (total === 0) return '0%'
  return Math.round((value / total) * 100) + '%'
}

function barWidthMap(key: string, map: Record<string, number>): string {
  const max = Math.max(...Object.values(map))
  return barWidth(map[key] ?? 0, max)
}
</script>

<style scoped>
.analytics-view {
  padding: 2rem clamp(1.25rem, 2.5vw, 2.75rem);
  width: min(96vw, 1680px);
  margin: 0 auto;
  display: grid;
  gap: 1.75rem;
}

.page-header h1 {
  font-size: 2.2rem;
  color: #0f172a;
  margin: 0 0 0.4rem;
}

.page-header p {
  color: #64748b;
  margin: 0;
  font-size: 1.05rem;
}

.uc-tag {
  display: inline-block;
  background: #e0f2fe;
  color: #0369a1;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  margin-left: 0.4rem;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.15rem;
}

.kpi-card {
  border-radius: 14px;
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.kpi-card.blue { background: linear-gradient(135deg, #dbeafe, #bfdbfe); }
.kpi-card.green { background: linear-gradient(135deg, #dcfce7, #bbf7d0); }
.kpi-card.purple { background: linear-gradient(135deg, #ede9fe, #ddd6fe); }
.kpi-card.slate { background: linear-gradient(135deg, #f1f5f9, #e2e8f0); }

.kpi-label {
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: #475569;
}

.kpi-value {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
}

/* ── Demo Panels ─────────────────────────────────────── */
.demo-panel {
  background: #fff;
  border: 2px solid #e2e8f0;
  border-radius: 16px;
  padding: 1.5rem;
  display: grid;
  gap: 1.25rem;
}

.rental-panel { border-color: #bfdbfe; background: linear-gradient(160deg, #f0f9ff, #fff); }
.gateway-panel { border-color: #bbf7d0; background: linear-gradient(160deg, #f0fdf4, #fff); }

.demo-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.demo-panel-header h2 {
  margin: 0.25rem 0 0.2rem;
  font-size: 1.15rem;
  color: #0f172a;
}

.demo-panel-header p {
  margin: 0;
  font-size: 0.88rem;
  color: #64748b;
}

.demo-tag {
  display: inline-block;
  background: #dbeafe;
  color: #1d4ed8;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.demo-tag.gateway {
  background: #dcfce7;
  color: #166534;
}

.demo-kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.15rem;
}

.demo-kpi {
  border-radius: 12px;
  padding: 1rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.demo-kpi.orange { background: #fff7ed; border: 1px solid #fed7aa; }
.demo-kpi.blue   { background: #eff6ff; border: 1px solid #bfdbfe; }
.demo-kpi.green  { background: #f0fdf4; border: 1px solid #bbf7d0; }

.demo-kpi-label {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: #64748b;
}

.demo-kpi-value {
  font-size: 2.2rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
}

.demo-kpi-sub {
  font-size: 0.8rem;
  color: #94a3b8;
}

.city-rev-list {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-top: 0.25rem;
}

.city-rev-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.92rem;
  color: #0f172a;
  font-weight: 500;
}

/* Gateway panel */
.refresh-gateway-btn {
  padding: 0.5rem 1rem;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 8px;
  color: #166534;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.88rem;
  white-space: nowrap;
  transition: background 0.2s;
}
.refresh-gateway-btn:hover:not(:disabled) { background: #dcfce7; }
.refresh-gateway-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.gateway-loading {
  color: #64748b;
  font-size: 0.9rem;
  animation: pulse 2s infinite;
}

.gateway-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-weight: 700;
  font-size: 1rem;
  width: fit-content;
}

.status-indicator.up     { background: #dcfce7; color: #166534; }
.status-indicator.degraded { background: #fef3c7; color: #92400e; }
.status-indicator.down   { background: #fee2e2; color: #991b1b; }

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-indicator.up .status-dot     { background: #22c55e; box-shadow: 0 0 6px #22c55e; animation: pulse-dot 2s infinite; }
.status-indicator.degraded .status-dot { background: #f59e0b; }
.status-indicator.down .status-dot   { background: #ef4444; }

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 4px #22c55e; }
  50% { box-shadow: 0 0 12px #22c55e; }
}

.gateway-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.gw-stat {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.65rem 1rem;
  min-width: 120px;
}

.gw-stat-label {
  display: block;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
  font-weight: 700;
  margin-bottom: 0.2rem;
}

.gw-stat-value {
  font-size: 1.4rem;
  font-weight: 800;
  color: #0f172a;
}

.gw-stat-value.slow { color: #f97316; }

.synced-at {
  font-size: 0.8rem;
  color: #94a3b8;
}

.gw-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 0.6rem 0.9rem;
  font-size: 0.85rem;
  color: #991b1b;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 1.5rem;
}

.section-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1.7rem;
}

.section-card h2 {
  margin: 0 0 1.25rem;
  font-size: 1.1rem;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge {
  font-size: 0.68rem;
  background: #fef3c7;
  color: #92400e;
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.bar-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.bar-row {
  display: grid;
  grid-template-columns: 110px 1fr 40px;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.9rem;
  color: #334155;
}

.bar-track {
  background: #f1f5f9;
  border-radius: 999px;
  height: 10px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s ease;
}

.bar-fill.green { background: #22c55e; }
.bar-fill.blue { background: #3b82f6; }
.bar-fill.orange { background: #f97316; }
.bar-fill.purple { background: #8b5cf6; }

.bar-count {
  text-align: right;
  font-weight: 700;
  color: #0f172a;
}

.event-log {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 280px;
  overflow-y: auto;
}

.event-log li {
  background: #f8fafc;
  border-left: 3px solid #3b82f6;
  padding: 0.5rem 0.75rem;
  border-radius: 0 8px 8px 0;
  font-size: 0.82rem;
  font-family: monospace;
  color: #334155;
}

.state-msg {
  text-align: center;
  padding: 3rem;
  color: #64748b;
  background: #fff;
  border-radius: 14px;
}

.state-msg.error { color: #dc2626; background: #fef2f2; }
.state-msg.pulse { animation: pulse 2s infinite; }

.empty-msg {
  color: #94a3b8;
  font-size: 0.9rem;
}

@keyframes pulse {
  0%, 100% { opacity: 1; } 50% { opacity: .5; }
}

@media (max-width: 1024px) {
  .analytics-view {
    width: 100%;
    padding: 1.25rem 1rem 1.75rem;
  }
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
