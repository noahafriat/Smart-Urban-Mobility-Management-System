<script setup lang="ts">
/**
 * Transit Usage Analytics
 * Visible to: City Admin, System Admin
 */
import { onMounted, computed } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()

const fleetTotal = computed(() => {
  if (!store.transitData) return 0
  return store.transitData.fleetAvailable + store.transitData.fleetInUse + store.transitData.fleetMaintenance
})

function barWidth(value: number, total: number): string {
  if (total === 0) return '0%'
  return Math.round((value / total) * 100) + '%'
}

onMounted(() => {
  store.fetchTransit()
  store.fetchGateway()
})
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <span class="view-tag">City Oversight</span>
        <h1>Transit & Platform Intelligence</h1>
        <p>A panoramic view of Montréal's mobility health and platform operational status.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="store.fetchTransit()" :disabled="store.loading">
          {{ store.loading ? 'Syncing...' : '↻ Sync Transit Analytics' }}
        </button>
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

      <div class="main-grid">
        <!-- ── Left Column: Operations ── -->
        <div class="ops-column">
          
          <!-- Fleet Distribution -->
          <section class="panel-card-clean">
            <div class="panel-header">
              <h3>Fleet Distribution</h3>
              <p>Real-time vehicle status across the network.</p>
            </div>
            <div class="fleet-health-meter">
               <div class="multi-bar-smooth">
                <div class="bar-seg g" :style="{ width: barWidth(store.transitData.fleetAvailable, fleetTotal) }" title="Ready"></div>
                <div class="bar-seg b" :style="{ width: barWidth(store.transitData.fleetInUse, fleetTotal) }" title="In Use"></div>
                <div class="bar-seg o" :style="{ width: barWidth(store.transitData.fleetMaintenance, fleetTotal) }" title="Maint."></div>
              </div>
              <div class="health-legend">
                <span><i class="dot g"></i> Ready: {{ store.transitData.fleetAvailable }}</span>
                <span><i class="dot b"></i> Active: {{ store.transitData.fleetInUse }}</span>
                <span><i class="dot o"></i> Support: {{ store.transitData.fleetMaintenance }}</span>
              </div>
            </div>
          </section>

          <!-- Usage Breakdown -->
          <div class="two-col-grid">
            <section class="panel-card-clean">
              <h3>Popularity</h3>
              <div class="stat-list">
                <div v-for="(count, type) in store.transitData.tripsByType" :key="type" class="clean-row">
                  <span class="name">{{ type }}</span>
                  <strong class="qty">{{ count }}</strong>
                </div>
              </div>
            </section>

            <section class="panel-card-clean">
              <h3>Top Cities</h3>
              <div class="stat-list">
                <div v-for="(count, city) in store.transitData.tripsByCity" :key="city" class="clean-row">
                  <span class="name">{{ city }}</span>
                  <strong class="qty">{{ count }}</strong>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- ── Right Column: External Health & Finance ── -->
        <div class="side-column">
          
          <!-- BIXI Gateway -->
          <section class="panel-card-clean">
            <div class="panel-header">
              <h3>BIXI Montréal</h3>
              <p>External API Status: <span class="green-text">{{ store.gatewayData?.status ?? 'Unknown' }}</span></p>
            </div>
            <div v-if="store.gatewayLoading" class="mini-loader">Requesting...</div>
            <div v-else-if="store.gatewayData" class="brief-list">
              <div class="brief-item">
                <span class="l">Total Stations</span>
                <strong class="v">{{ store.gatewayData.totalStations }}</strong>
              </div>
              <div class="brief-item">
                <span class="l">Bikes Available</span>
                <strong class="v green-text">{{ store.gatewayData.bikesAvailable }}</strong>
              </div>
              <div class="brief-item">
                <span class="l">Latency</span>
                <strong class="v">{{ store.gatewayData.responseMs }}ms</strong>
              </div>
            </div>
            <button class="btn-full" @click="store.fetchGateway()">Refresh BIXI</button>
          </section>

          <!-- Revenue Breakdown -->
          <section class="panel-card-clean">
             <h3>Revenue by City</h3>
             <div class="stat-list">
               <div v-for="(rev, city) in store.transitData.revenueByCity" :key="city" class="clean-row">
                 <span class="name">{{ city }}</span>
                 <strong class="qty">${{ rev.toLocaleString() }}</strong>
               </div>
             </div>
          </section>

          <section class="panel-card-clean highlight">
            <h3>System Health</h3>
            <div class="brief-list">
              <div class="brief-item">
                <span class="l">Avg Trip Time</span>
                <strong class="v">{{ store.transitData.avgTripDurationMinutes }}m</strong>
              </div>
              <div class="brief-item">
                <span class="l">Historical Volume</span>
                <strong class="v">{{ store.transitData.totalTrips }}</strong>
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
.kpi-card-simple .value { font-size: 2.25rem; font-weight: 800; color: #0f172a; line-height: 1; margin-bottom: 0.5rem; }
.kpi-card-simple .subtext { font-size: 0.8rem; color: #94a3b8; }

/* ── Layout ── */
.main-grid { display: grid; grid-template-columns: 1fr 340px; gap: 2rem; }
.panel-card-clean { padding: 1.5rem; border: 1px solid #f1f5f9; border-radius: 12px; margin-bottom: 1.5rem; background: #fff; }
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

/* ── Stat Lists ── */
.two-col-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.clean-row { display: flex; justify-content: space-between; padding: 0.75rem 0; border-bottom: 1px solid #f8fafc; font-size: 0.9rem; }
.clean-row:last-child { border-bottom: none; }
.clean-row .name { color: #475569; font-weight: 500; }
.clean-row .qty { font-weight: 700; color: #0f172a; }

/* ── Sidebar ── */
.brief-list { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem; }
.brief-item { display: flex; justify-content: space-between; font-size: 0.95rem; }
.brief-item .l { color: #64748b; }
.brief-item .v { font-weight: 700; color: #0f172a; }
.green-text { color: #10b981; }

.btn-full { width: 100%; padding: 0.6rem; background: #0f172a; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
.btn-full:hover { background: #1e293b; }

.panel-card-clean.highlight { background: #f8fafc; border-color: #e2e8f0; }

.mini-loader { font-size: 0.8rem; color: #64748b; padding: 1rem 0; }

.state-msg { text-align: center; padding: 5rem; color: #64748b; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.pulse { animation: pulse 2s infinite; }

@media (max-width: 1024px) {
  .main-grid { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: 1fr; }
}
</style>
