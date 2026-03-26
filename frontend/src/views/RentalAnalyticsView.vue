<script setup lang="ts">
/**
 * Rental & Market Analytics
 * Visible to: City Admin, System Admin, Service Providers
 */
import { onMounted } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()

function zoneCapacity(count: number, rateStr: string): number {
  if (!rateStr) return count
  const rate = parseInt(rateStr.replace('%', ''))
  if (rate === 0) return count || 100 // Fallback
  return Math.round(count / (rate / 100))
}

function formatZoneName(raw: string): string {
  // raw is "Montréal / Plateau (Scooter)"
  // we want "Plateau (Scooter)"
  return raw.split(' / ').pop() || raw
}

onMounted(() => {
  store.fetchRentals()
  store.fetchParking() // Also fetch parking for zone density
})
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <div class="scope-header">
          <span class="view-tag">Market Intelligence</span>
          <span class="scope-pill provider">Multi-Provider View</span>
        </div>
        <h1>Rental Analytics & Revenue</h1>
        <p>A consolidated view of historical rental volume, financial performance, and market distribution.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="store.fetchRentals()" :disabled="store.loading">
          {{ store.loading ? 'Syncing...' : '↻ Refresh Rental Analytics' }}
        </button>
      </div>
    </header>

    <div v-if="store.loading && !store.rentalData" class="state-msg pulse">
      Compiling rental history and financial data...
    </div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <main v-else-if="store.rentalData" class="analytics-body">
      
      <!-- ── Global Rental KPIs ── -->
      <section class="kpi-banner">
        <div class="kpi-grid">
          <div class="kpi-card-simple">
            <span class="label">Total Historical Volume</span>
            <strong class="value">{{ store.rentalData.totalRentals }}</strong>
            <span class="subtext">{{ store.rentalData.activeRentals }} current deployments</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Gross Performance</span>
            <strong class="value">${{ store.rentalData.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 0 }) }}</strong>
            <span class="subtext">Verified financial value</span>
          </div>
          <div class="kpi-card-simple">
            <span class="label">Utilization Index</span>
            <strong class="value">{{ ((store.rentalData.activeRentals / (store.rentalData.totalRentals || 1)) * 100).toFixed(1) }}%</strong>
            <span class="subtext">Active fleet pressure</span>
          </div>
        </div>
      </section>

      <div class="main-grid">
        <!-- ── Market Performance ── -->
        <div class="ops-column">
          <section class="panel-card-clean">
            <div class="panel-header">
              <h3>Market Distribution</h3>
              <p>Breakdown of volume and revenue contributions per vehicle category.</p>
            </div>
            <table class="data-table-clean">
              <thead>
                <tr>
                  <th>Vehicle Type</th>
                  <th>Total Volume</th>
                  <th>Total Revenue</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(count, type) in store.rentalData.rentalsByType" :key="type">
                  <td class="name">{{ type }}</td>
                  <td class="qty">{{ count }} sessions</td>
                  <td class="qty">${{ store.rentalData.revenueByType[type]?.toLocaleString() }}</td>
                </tr>
              </tbody>
            </table>
          </section>

          <section v-if="store.parkingData" class="panel-card-clean">
            <h3>Neighborhood Availability (Near Real-Time)</h3>
            <p>Street-level density of available vehicles in urban zones.</p>
            <div class="stat-list">
              <div v-for="(count, zone) in store.parkingData.parkedPerZone" :key="zone" class="clean-row">
                <span class="name">{{ formatZoneName(zone) }}</span>
                <strong class="qty">
                  {{ count }} / {{ zoneCapacity(count, store.parkingData.occupancyRate[zone]!) }} available
                </strong>
              </div>
            </div>
          </section>

          <section class="panel-card-clean">
            <h3>Geographic Engagement</h3>
            <div class="stat-list">
              <div v-for="(count, city) in store.rentalData.rentalsByCity" :key="city" class="clean-row">
                <span class="name">{{ city }}</span>
                <strong class="qty">{{ count }} rentals</strong>
              </div>
            </div>
          </section>
        </div>

        <!-- ── Executive Summary ── -->
        <div class="side-column">
          <section class="panel-card-clean highlight">
            <h3>Executive Briefing</h3>
            <div class="brief-list">
              <div class="brief-item">
                <span class="l">Completed Trips</span>
                <strong class="v">{{ store.rentalData.completedRentals }}</strong>
              </div>
              <div class="brief-item">
                <span class="l">Billing Fulfillment</span>
                <strong class="v">{{ ((store.rentalData.paidRentals / (store.rentalData.completedRentals || 1)) * 100).toFixed(0) }}%</strong>
              </div>
              <div class="brief-item">
                <span class="l">System Health</span>
                <strong class="v green-text">Stable</strong>
              </div>
            </div>
            <button class="btn-full" @click="store.fetchRentals()">↻ Refresh Rental Metrics</button>
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
.scope-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
.scope-pill { font-size: 0.65rem; font-weight: 800; padding: 0.2rem 0.5rem; border: 1px solid #e2e8f0; border-radius: 4px; color: #64748b; }
.scope-pill.provider { color: #10b981; border-color: #bbf7d0; background: #f0fdf4; }

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

/* ── Data Table ── */
.data-table-clean { width: 100%; border-collapse: collapse; font-size: 0.9rem; margin-top: 1rem; }
.data-table-clean th { text-align: left; padding: 0.75rem; color: #94a3b8; font-size: 0.75rem; text-transform: uppercase; border-bottom: 1px solid #f1f5f9; }
.data-table-clean td { padding: 1rem 0.75rem; border-bottom: 1px solid #f8fafc; }
.data-table-clean .name { font-weight: 700; color: #0f172a; }

.clean-row { display: flex; justify-content: space-between; padding: 0.75rem 0; font-size: 0.9rem; border-bottom: 1px solid #f8fafc; }
.clean-row .name { color: #475569; font-weight: 500; }
.clean-row .qty { font-weight: 700; color: #0f172a; }

/* ── Sidebar ── */
.brief-list { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem; }
.brief-item { display: flex; justify-content: space-between; font-size: 0.95rem; }
.brief-item .l { color: #64748b; }
.brief-item .v { font-weight: 700; color: #0f172a; }
.green-text { color: #10b981; }

.btn-full { width: 100%; padding: 0.75rem; background: #0f172a; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
.btn-full:hover { background: #1e293b; }

.state-msg { text-align: center; padding: 5rem; color: #64748b; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.pulse { animation: pulse 2s infinite; }

@media (max-width: 1024px) {
  .main-grid { grid-template-columns: 1fr; }
  .kpi-grid { grid-template-columns: 1fr; }
}
</style>
