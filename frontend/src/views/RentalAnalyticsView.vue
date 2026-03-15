<script setup lang="ts">
/**
 * Rental Service Analytics
 * - Admins: see the full platform
 * - Providers: scoped to their own fleet only
 */
import { onMounted, computed } from 'vue'
import { useAnalyticsStore } from '../stores/analytics'
import { useAuthStore } from '../stores/auth'

const store = useAnalyticsStore()
const auth = useAuthStore()

const isProvider = computed(() => auth.isProvider)

onMounted(() => {
  if (auth.isProvider && auth.user) {
    store.fetchRentals(auth.user.id)
  } else {
    store.fetchRentals()
  }
})
</script>

<template>
  <div class="analytics-view">
    <header class="page-header">
      <h1>Rental Service Analytics</h1>
      <p>
        {{ isProvider ? 'Your fleet rental activity.' : 'Platform-wide rental activity.' }}
      </p>
    </header>

    <div v-if="store.loading" class="state-msg pulse">Loading rental analytics…</div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <template v-else-if="store.rentalData">
      <div v-if="!isProvider" class="scope-badge">
        Viewing: <strong>Full Platform</strong>
      </div>
      <div v-else class="scope-badge provider">
        Viewing: <strong>Your Fleet Only</strong>
      </div>

      <!-- KPI Row -->
      <div class="kpi-grid">
        <div class="kpi-card blue">
          <span class="kpi-label">Total Rentals</span>
          <strong class="kpi-value">{{ store.rentalData.totalRentals }}</strong>
        </div>
        <div class="kpi-card orange">
          <span class="kpi-label">Active Now</span>
          <strong class="kpi-value">{{ store.rentalData.activeRentals }}</strong>
        </div>
        <div class="kpi-card green">
          <span class="kpi-label">Total Revenue</span>
          <strong class="kpi-value">${{ store.rentalData.totalRevenue.toFixed(2) }}</strong>
        </div>
        <div class="kpi-card purple">
          <span class="kpi-label">Paid Invoices</span>
          <strong class="kpi-value">{{ store.rentalData.paidRentals }}</strong>
        </div>
      </div>

      <div class="two-col">
        <!-- Rentals by Type -->
        <section class="section-card">
          <h2>Rentals by Vehicle Type</h2>
          <div v-if="Object.keys(store.rentalData.rentalsByType).length === 0" class="empty-msg">
            No rentals recorded yet.
          </div>
          <div v-else class="bar-group">
            <div v-for="(count, type) in store.rentalData.rentalsByType" :key="type" class="bar-row">
              <span>{{ type }}</span>
              <div class="bar-track">
                <div class="bar-fill blue" :style="{ width: barWidthMap(type, store.rentalData.rentalsByType) }"></div>
              </div>
              <span class="bar-count">{{ count }}</span>
            </div>
          </div>
        </section>

        <!-- Revenue by Type -->
        <section class="section-card">
          <h2>Revenue by Vehicle Type</h2>
          <div v-if="Object.keys(store.rentalData.revenueByType).length === 0" class="empty-msg">
            No revenue recorded yet.
          </div>
          <div v-else class="bar-group">
            <div v-for="(rev, type) in store.rentalData.revenueByType" :key="type" class="bar-row">
              <span>{{ type }}</span>
              <div class="bar-track">
                <div class="bar-fill green" :style="{ width: barWidthMap(type, store.rentalData.revenueByType) }"></div>
              </div>
              <span class="bar-count">${{ rev }}</span>
            </div>
          </div>
        </section>
      </div>

      <div class="two-col">
        <!-- Rentals by City -->
        <section class="section-card">
          <h2>Rentals by City</h2>
          <div v-if="Object.keys(store.rentalData.rentalsByCity).length === 0" class="empty-msg">
            No city data yet.
          </div>
          <div v-else class="bar-group">
            <div v-for="(count, city) in store.rentalData.rentalsByCity" :key="city" class="bar-row">
              <span>{{ city }}</span>
              <div class="bar-track">
                <div class="bar-fill purple" :style="{ width: barWidthMap(city, store.rentalData.rentalsByCity) }"></div>
              </div>
              <span class="bar-count">{{ count }}</span>
            </div>
          </div>
        </section>

        <!-- Top Rented Vehicles -->
        <section class="section-card">
          <h2>Top Rented Vehicles</h2>
          <div v-if="Object.keys(store.rentalData.topVehicles).length === 0" class="empty-msg">
            No rentals yet.
          </div>
          <ol v-else class="top-vehicles">
            <li v-for="(count, code) in store.rentalData.topVehicles" :key="code">
              <span class="v-code">{{ code }}</span>
              <span class="v-count">{{ count }} trip{{ count !== 1 ? 's' : '' }}</span>
            </li>
          </ol>
        </section>
      </div>
    </template>
  </div>
</template>

<script lang="ts">
function barWidthMap(key: string, map: Record<string, number>): string {
  const max = Math.max(...Object.values(map))
  if (max === 0) return '0%'
  return Math.round(((map[key] ?? 0) / max) * 100) + '%'
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
.page-header h1 { font-size: 2.2rem; color: #0f172a; margin: 0 0 0.4rem; }
.page-header p { color: #64748b; margin: 0; font-size: 1.05rem; }
.uc-tag {
  display: inline-block; background: #e0f2fe; color: #0369a1;
  padding: 0.15rem 0.5rem; border-radius: 999px;
  font-size: 0.75rem; font-weight: 700; margin-left: 0.4rem;
}
.scope-badge {
  display: inline-flex; align-items: center; gap: 0.4rem;
  background: #f1f5f9; border: 1px solid #e2e8f0;
  border-radius: 999px; padding: 0.4rem 1rem;
  font-size: 0.88rem; color: #475569;
}
.scope-badge.provider { background: #eff6ff; border-color: #bfdbfe; color: #1d4ed8; }
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 1rem; }
.kpi-card { border-radius: 14px; padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 0.35rem; }
.kpi-card.blue { background: linear-gradient(135deg, #dbeafe, #bfdbfe); }
.kpi-card.orange { background: linear-gradient(135deg, #ffedd5, #fed7aa); }
.kpi-card.green { background: linear-gradient(135deg, #dcfce7, #bbf7d0); }
.kpi-card.purple { background: linear-gradient(135deg, #ede9fe, #ddd6fe); }
.kpi-label { font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em; color: #475569; }
.kpi-value { font-size: 2rem; font-weight: 800; color: #0f172a; }
.two-col { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }
.section-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 1.5rem; }
.section-card h2 { margin: 0 0 1.25rem; font-size: 1.1rem; color: #0f172a; }
.bar-group { display: flex; flex-direction: column; gap: 0.75rem; }
.bar-row { display: grid; grid-template-columns: 110px 1fr 60px; align-items: center; gap: 0.75rem; font-size: 0.9rem; color: #334155; }
.bar-track { background: #f1f5f9; border-radius: 999px; height: 10px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; transition: width 0.5s ease; }
.bar-fill.blue { background: #3b82f6; }
.bar-fill.green { background: #22c55e; }
.bar-fill.purple { background: #8b5cf6; }
.bar-count { text-align: right; font-weight: 700; color: #0f172a; font-size: 0.85rem; }
.top-vehicles { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.6rem; counter-reset: rank; }
.top-vehicles li { counter-increment: rank; display: flex; justify-content: space-between; align-items: center; background: #f8fafc; border-radius: 8px; padding: 0.6rem 0.9rem; font-size: 0.9rem; }
.top-vehicles li::before { content: counter(rank); font-weight: 800; color: #94a3b8; margin-right: 0.75rem; }
.v-code { font-weight: 600; color: #0f172a; }
.v-count { color: #64748b; }
.state-msg { text-align: center; padding: 3rem; color: #64748b; background: #fff; border-radius: 14px; }
.state-msg.error { color: #dc2626; background: #fef2f2; }
.state-msg.pulse { animation: pulse 2s infinite; }
.empty-msg { color: #94a3b8; font-size: 0.9rem; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .5; } }
</style>
