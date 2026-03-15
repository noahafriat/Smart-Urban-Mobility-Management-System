<script setup lang="ts">
/**
 * Transit Usage Analytics
 * Visible to: City Admin, System Admin
 */
import { onMounted } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()

onMounted(() => store.fetchTransit())
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
        <h2>System Event Log <span class="badge">Singleton Observer</span></h2>
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
  padding: 2rem;
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  gap: 1.5rem;
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
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 1rem;
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

.two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.section-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1.5rem;
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
</style>
