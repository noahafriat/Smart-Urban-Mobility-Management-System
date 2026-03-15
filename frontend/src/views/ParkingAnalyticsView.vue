<script setup lang="ts">
/**
 * Parking / Zone Utilization Analytics
 * Visible to: City Admin, System Admin
 */
import { onMounted } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'

const store = useAnalyticsStore()
onMounted(() => store.fetchParking())
</script>

<template>
  <div class="analytics-view">
    <header class="page-header">
      <h1>Parking & Zone Analytics</h1>
      <p>Real-time vehicle occupancy and zone utilization across the city.</p>
    </header>

    <div v-if="store.loading" class="state-msg pulse">Loading parking analytics…</div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <template v-else-if="store.parkingData">
      <!-- KPI Row -->
      <div class="kpi-grid">
        <div class="kpi-card blue">
          <span class="kpi-label">Total Vehicles</span>
          <strong class="kpi-value">{{ store.parkingData.totalVehicles }}</strong>
        </div>
        <div class="kpi-card green">
          <span class="kpi-label">Parked in Zones</span>
          <strong class="kpi-value">{{ store.parkingData.totalAvailableInZones }}</strong>
        </div>
        <div class="kpi-card orange">
          <span class="kpi-label">Utilization Rate</span>
          <strong class="kpi-value">{{ store.parkingData.overallUtilizationRate }}</strong>
        </div>
        <div class="kpi-card red" v-if="Object.values(store.parkingData.maintenancePerCity).reduce((a, b) => a + b, 0) > 0">
          <span class="kpi-label">In Maintenance</span>
          <strong class="kpi-value">{{ Object.values(store.parkingData.maintenancePerCity).reduce((a, b) => a + b, 0) }}</strong>
        </div>
      </div>

      <div class="two-col">
        <!-- Occupancy Rate per Zone -->
        <section class="section-card">
          <h2>Zone Occupancy Rate</h2>
          <p class="section-sub">Percentage of vehicles in each zone that are currently in use or unavailable.</p>
          <div class="bar-group">
            <div v-for="(rate, zone) in store.parkingData.occupancyRate" :key="zone" class="bar-row wide">
              <span class="zone-label" :title="zone">{{ zone }}</span>
              <div class="bar-track">
                <div class="bar-fill"
                     :class="occupancyColor(rate)"
                     :style="{ width: rate }">
                </div>
              </div>
              <span class="bar-count">{{ rate }}</span>
            </div>
          </div>
        </section>

        <!-- Parked per Zone -->
        <section class="section-card">
          <h2>Available Vehicles per Zone</h2>
          <p class="section-sub">Number of vehicles currently parked and ready for rental.</p>
          <div v-if="Object.keys(store.parkingData.parkedPerZone).length === 0" class="empty-msg">
            No available vehicles in any zone.
          </div>
          <div v-else class="bar-group">
            <div v-for="(count, zone) in store.parkingData.parkedPerZone" :key="zone" class="bar-row wide">
              <span class="zone-label" :title="zone">{{ zone }}</span>
              <div class="bar-track">
                <div class="bar-fill green" :style="{ width: barWidthMap(zone, store.parkingData.parkedPerZone) }"></div>
              </div>
              <span class="bar-count">{{ count }}</span>
            </div>
          </div>
        </section>
      </div>

      <!-- Maintenance per City -->
      <section class="section-card" v-if="Object.keys(store.parkingData.maintenancePerCity).length > 0">
        <h2>Vehicles in Maintenance by City</h2>
        <div class="bar-group">
          <div v-for="(count, city) in store.parkingData.maintenancePerCity" :key="city" class="bar-row">
            <span>{{ city }}</span>
            <div class="bar-track">
              <div class="bar-fill orange" :style="{ width: barWidthMap(city, store.parkingData.maintenancePerCity) }"></div>
            </div>
            <span class="bar-count">{{ count }}</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script lang="ts">
function barWidthMap(key: string, map: Record<string, number>): string {
  const max = Math.max(...Object.values(map))
  if (max === 0) return '0%'
  return Math.round(((map[key] ?? 0) / max) * 100) + '%'
}

function occupancyColor(rate: string): string {
  const pct = parseInt(rate)
  if (pct >= 75) return 'red'
  if (pct >= 40) return 'orange'
  return 'green'
}
</script>

<style scoped>
.analytics-view { padding: 2rem; max-width: 1100px; margin: 0 auto; display: grid; gap: 1.5rem; }
.page-header h1 { font-size: 2.2rem; color: #0f172a; margin: 0 0 0.4rem; }
.page-header p { color: #64748b; margin: 0; font-size: 1.05rem; }
.uc-tag { display: inline-block; background: #e0f2fe; color: #0369a1; padding: 0.15rem 0.5rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; margin-left: 0.4rem; }
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 1rem; }
.kpi-card { border-radius: 14px; padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 0.35rem; }
.kpi-card.blue { background: linear-gradient(135deg, #dbeafe, #bfdbfe); }
.kpi-card.green { background: linear-gradient(135deg, #dcfce7, #bbf7d0); }
.kpi-card.orange { background: linear-gradient(135deg, #ffedd5, #fed7aa); }
.kpi-card.red { background: linear-gradient(135deg, #fee2e2, #fecaca); }
.kpi-label { font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em; color: #475569; }
.kpi-value { font-size: 2rem; font-weight: 800; color: #0f172a; }
.two-col { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }
.section-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 1.5rem; }
.section-card h2 { margin: 0 0 0.4rem; font-size: 1.1rem; color: #0f172a; }
.section-sub { margin: 0 0 1.25rem; font-size: 0.87rem; color: #64748b; }
.bar-group { display: flex; flex-direction: column; gap: 0.75rem; }
.bar-row { display: grid; grid-template-columns: 110px 1fr 40px; align-items: center; gap: 0.75rem; font-size: 0.9rem; color: #334155; }
.bar-row.wide { grid-template-columns: 160px 1fr 45px; }
.zone-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.85rem; }
.bar-track { background: #f1f5f9; border-radius: 999px; height: 10px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; transition: width 0.5s ease; }
.bar-fill.green { background: #22c55e; }
.bar-fill.orange { background: #f97316; }
.bar-fill.red { background: #ef4444; }
.bar-count { text-align: right; font-weight: 700; color: #0f172a; }
.state-msg { text-align: center; padding: 3rem; color: #64748b; background: #fff; border-radius: 14px; }
.state-msg.error { color: #dc2626; background: #fef2f2; }
.state-msg.pulse { animation: pulse 2s infinite; }
.empty-msg { color: #94a3b8; font-size: 0.9rem; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .5; } }
</style>
