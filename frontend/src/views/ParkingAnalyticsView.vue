<script setup lang="ts">
/**
 * Parking & Infrastructure Analytics (Stationary only)
 * Visible to: City Admin, System Admin
 */
import { onMounted } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()

function availabilityColor(available: number, total: number): string {
  const percent = (available / total) * 100
  if (percent > 40) return 'green'
  if (percent > 15) return 'orange'
  return 'red'
}

onMounted(() => {
  store.fetchParking()
})
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <span class="view-tag">Infrastructure Oversight</span>
        <h1>Stationary Parking Infrastructure</h1>
        <p>Real-time monitoring and capacity analysis of formal parking facilities and garages.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="store.fetchParking()" :disabled="store.loading">
          {{ store.loading ? 'Syncing...' : '↻ Sync Parking Analytics' }}
        </button>
      </div>
    </header>

    <div v-if="store.loading && !store.parkingData" class="state-msg pulse">
      Interrogating infrastructure sensors...
    </div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <main v-else-if="store.parkingData" class="analytics-body">
      
      <!-- ── Capacity KPIs ── -->
      <section class="kpi-banner">
        <div class="kpi-grid">
          <div class="kpi-card-simple">
            <span class="label">Total Garage Capacity</span>
            <strong class="value">{{ store.parkingData.totalGarageSpaces }}</strong>
            <span class="subtext">Spots monitored</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Current Availability</span>
            <strong class="value">{{ store.parkingData.totalAvailableGarageSpaces }}</strong>
            <span class="subtext">Verified open slots</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Global Occupancy</span>
            <strong class="value">{{ store.parkingData.garageUtilizationRate }}</strong>
            <span class="subtext">System-wide load</span>
          </div>
        </div>
      </section>

      <div class="main-grid">
        <!-- ── Garage Matrix ── -->
        <div class="ops-column">
          <section class="panel-card-clean">
            <div class="panel-header">
              <h3>Facility Capacity Matrix</h3>
              <p>Live occupancy states for all connected parking nodes.</p>
            </div>
            <div class="garage-list-clean">
              <div v-for="garage in store.parkingData.garageDetails" :key="garage.id" class="garage-row">
                <div class="info">
                  <span class="name">{{ garage.name }}</span>
                  <span class="meta">{{ garage.availableSpaces }} / {{ garage.totalSpaces }} spots available</span>
                </div>
                <div class="load-badge" :class="availabilityColor(garage.availableSpaces, garage.totalSpaces)">
                  {{ (( (garage.totalSpaces - garage.availableSpaces) / garage.totalSpaces ) * 100).toFixed(0) }}% Full
                </div>
              </div>
              <div v-if="store.parkingData.garageDetails.length === 0" class="empty-state">No monitored facilities found.</div>
            </div>
          </section>
        </div>

        <!-- ── Facility Summary ── -->
        <div class="side-column">
          <section class="panel-card-clean highlight">
            <h3>Infrastructure Health</h3>
            <div class="brief-list">
              <div class="brief-item">
                <span class="l">Monitored Facilities</span>
                <strong class="v">{{ store.parkingData.garageDetails.length }}</strong>
              </div>
              <div class="brief-item">
                <span class="l">Critical Loads</span>
                <strong class="v" :class="{ 'red-text': store.parkingData.garageDetails.some(g => (g.availableSpaces/g.totalSpaces) < 0.1) }">
                  {{ store.parkingData.garageDetails.filter(g => (g.availableSpaces/g.totalSpaces) < 0.1).length }}
                </strong>
              </div>
              <div class="brief-item">
                <span class="l">Sensor Status</span>
                <strong class="v green-text">Online</strong>
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

.page-header { margin-bottom: 3rem; display: flex; justify-content: space-between; align-items: top; }
.header-main h1 { font-size: 2rem; font-weight: 800; color: #0f172a; margin: 0 0 0.5rem; letter-spacing: -0.02em; }
.header-main p { color: #64748b; font-size: 1rem; margin: 0; }
.view-tag { font-size: 0.7rem; font-weight: 700; color: #3b82f6; text-transform: uppercase; margin-bottom: 0.5rem; display: block; }

.btn-refresh { padding: 0.6rem 1.25rem; font-size: 0.9rem; font-weight: 600; border: 1px solid #e2e8f0; border-radius: 8px; background: white; cursor: pointer; transition: 0.2s; }
.btn-refresh:hover { background: #f8fafc; }

/* ── KPIs ── */
.kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.5rem; margin-bottom: 3rem; }
.kpi-card-simple { padding: 1.5rem; border: 1px solid #e2e8f0; border-radius: 12px; display: flex; flex-direction: column; background: #fff; }
.kpi-card-simple .label { font-size: 0.8rem; font-weight: 600; color: #64748b; margin-bottom: 0.5rem; }
.kpi-card-simple .value { font-size: 2rem; font-weight: 800; color: #0f172a; line-height: 1; margin-bottom: 0.5rem; }
.kpi-card-simple .subtext { font-size: 0.8rem; color: #94a3b8; }

/* ── Layout ── */
.main-grid { display: grid; grid-template-columns: 1fr 340px; gap: 2rem; }
.panel-card-clean { padding: 1.5rem; border: 1px solid #f1f5f9; border-radius: 12px; margin-bottom: 1.5rem; background: #fff; }
.panel-card-clean h3 { font-size: 1.1rem; font-weight: 700; color: #0f172a; margin: 0 0 1.25rem; }
.panel-card-clean p { font-size: 0.9rem; color: #64748b; margin: -1rem 0 1.5rem; }

/* ── Garage List ── */
.garage-list-clean { display: flex; flex-direction: column; gap: 0.5rem; }
.garage-row { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border-bottom: 1px solid #f8fafc; }
.garage-row:last-child { border-bottom: none; }
.garage-row .info { display: flex; flex-direction: column; }
.garage-row .name { font-weight: 700; color: #0f172a; }
.garage-row .meta { font-size: 0.8rem; color: #64748b; }
.load-badge { padding: 0.25rem 0.6rem; border-radius: 4px; font-size: 0.75rem; font-weight: 700; }
.load-badge.green { background: #f0fdf4; color: #10b981; }
.load-badge.orange { background: #fffbeb; color: #f59e0b; }
.load-badge.red { background: #fef2f2; color: #ef4444; }

/* ── Sidebar ── */
.brief-list { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem; }
.brief-item { display: flex; justify-content: space-between; font-size: 0.95rem; }
.brief-item .l { color: #64748b; }
.brief-item .v { font-weight: 700; color: #0f172a; }
.green-text { color: #10b981; }
.red-text { color: #ef4444; }

.panel-card-clean.highlight { background: #f8fafc; border-color: #e2e8f0; }

.state-msg { text-align: center; padding: 5rem; color: #64748b; }
.empty-state { text-align: center; padding: 2rem; color: #94a3b8; font-style: italic; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.pulse { animation: pulse 2s infinite; }

@media (max-width: 1024px) {
  .main-grid { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: 1fr; }
}
</style>
