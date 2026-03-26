<script setup lang="ts">
/**
 * Citizen Rental Management & Live Receipt View.
 * Displays both active trips and historical bookings.
 */
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useRentalStore } from '../stores/rentals'

const auth = useAuthStore()
const rentalStore = useRentalStore()
const router = useRouter()

onMounted(() => {
  if (auth.user) {
    rentalStore.fetchUserRentals(auth.user.id)
  }
})

function formatTime(isoString: string | null) {
  if (!isoString) return 'Ongoing'
  return new Date(isoString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function handleReturn(rentalId: string) {
  if (auth.user) rentalStore.returnVehicle(rentalId, auth.user.id)
}

function handlePay(rentalId: string) {
  if (auth.user) rentalStore.pay(rentalId, auth.user.id)
}
</script>

<template>
  <div class="rentals-view fade-in">
    <header class="page-header">
      <div class="header-content">
        <span class="view-tag">Travel Monitoring</span>
        <h1>My Rentals</h1>
        <p>Review active travel sessions and settle outstanding trip invoices.</p>
      </div>
    </header>

    <div v-if="rentalStore.loading && rentalStore.rentals.length === 0" class="state-msg pulse">
       Retrieving your city movement history...
    </div>

    <div v-else-if="rentalStore.error" class="state-msg error">
      {{ rentalStore.error }}
    </div>

    <div v-else-if="rentalStore.rentals.length === 0" class="empty-state">
      <div class="empty-icon">📂</div>
      <h2>No rental history found.</h2>
      <p>Your journey with SUMMS starts here. Explore our live transit fleet and book your first trip.</p>
      <RouterLink to="/vehicles" class="action-btn">Find a Vehicle</RouterLink>
    </div>

    <div v-else class="rental-list">
      <div v-for="rental in rentalStore.rentals" :key="rental.id" class="rental-card-modern">
        
        <header class="card-top">
          <div class="v-meta">
            <span class="v-type">{{ rental.vehicle.type }}</span>
            <h3>{{ rental.vehicle.type === 'CAR' ? rental.vehicle.model : 'Eco-Scooter Fleet' }}</h3>
            <span class="v-code">#{{ rental.vehicle.vehicleCode }}</span>
          </div>
          <span class="status-pill" :class="rental.status.toLowerCase()">
            {{ rental.status }}
          </span>
        </header>

        <main class="card-body">
          <div class="trip-info">
            <div class="loc-grid">
              <div class="loc-item">
                <span class="label">Initial Hub</span>
                <span class="val">{{ rental.vehicle.locationCity }} / {{ rental.vehicle.locationZone }}</span>
              </div>
              <div class="loc-item" v-if="rental.vehicle.licensePlate">
                <span class="label">Fleet ID</span>
                <span class="val">{{ rental.vehicle.licensePlate }}</span>
              </div>
            </div>

            <div class="timeline-row">
              <div class="time-block">
                <span class="label">Started</span>
                <span class="val">{{ formatTime(rental.startTime) }}</span>
              </div>
              <div class="time-divider"></div>
              <div class="time-block">
                <span class="label">Finished</span>
                <span class="val">{{ formatTime(rental.endTime) }}</span>
              </div>
            </div>
          </div>

          <!-- Financial Context -->
          <div class="finance-block">
            <div class="invoice-row">
              <span class="label">Associated Method</span>
              <span class="val card-label">💳 {{ rental.reservationPaymentMethod }}</span>
            </div>
            
            <div class="invoice-row total" v-if="rental.status !== 'ACTIVE'">
              <span class="label">Total Invoice</span>
              <span class="total-val">${{ rental.totalCost?.toFixed(2) }}</span>
            </div>
          </div>
        </main>

        <footer class="card-footer">
          <!-- Active Actions -->
          <div v-if="rental.status === 'ACTIVE'" class="actions-group">
            <button @click="handleReturn(rental.id)" class="btn-primary return">End Trip</button>
          </div>

          <!-- Pending Payment -->
          <div v-else-if="rental.status === 'COMPLETED'" class="payment-setup">
            <div class="payment-instruction">
               <p>Your trip is complete. Charge will be applied to <strong>{{ rental.reservationPaymentMethod }}</strong>.</p>
            </div>
            <button @click="handlePay(rental.id)" class="btn-primary pay">Authorize Settlement Charge</button>
          </div>
          
          <!-- Reconciliation -->
          <div v-else-if="rental.status === 'PAID'" class="paid-confirmation">
             <div class="receipt-label">
                <span class="check">✔</span> 
                Invoiced & Settled
             </div>
             <div class="receipt-info">
               Processed via <strong>{{ rental.finalPaymentMethod || rental.reservationPaymentMethod }}</strong>
             </div>
          </div>
        </footer>

      </div>
    </div>
  </div>
</template>

<style scoped>
.rentals-view {
  padding: 3rem clamp(1rem, 5vw, 4rem);
  max-width: 900px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
}

.page-header { margin-bottom: 3.5rem; }
.view-tag { color: #3b82f6; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; display: block; margin-bottom: 0.5rem; }
.page-header h1 { font-size: 2.5rem; font-weight: 800; color: #0f172a; margin: 0; letter-spacing: -0.02em; }
.page-header p { color: #64748b; font-size: 1.1rem; margin-top: 0.5rem; }

/* ── List Layout ── */
.rental-list { display: flex; flex-direction: column; gap: 2rem; }
.rental-card-modern { background: white; border: 1px solid #f1f5f9; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 25px -10px rgba(0,0,0,0.05); }

/* ── Card Header ── */
.card-top { padding: 1.5rem 2rem; border-bottom: 1px solid #f8fafc; display: flex; justify-content: space-between; align-items: center; }
.v-meta h3 { margin: 0; font-size: 1.25rem; font-weight: 800; color: #0f172a; }
.v-type { font-size: 0.65rem; font-weight: 700; color: #94a3b8; text-transform: uppercase; }
.v-code { font-size: 0.85rem; color: #64748b; font-family: 'JetBrains Mono', monospace; }
.status-pill { padding: 0.35rem 0.75rem; border-radius: 100px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em; }
.status-pill.active { background: #eff6ff; color: #3b82f6; }
.status-pill.completed { background: #fef2f2; color: #ef4444; }
.status-pill.paid { background: #f0fdf4; color: #10b981; }

/* ── Card Body ── */
.card-body { padding: 2rem; display: grid; grid-template-columns: 1fr 280px; gap: 2rem; }
.label { font-size: 0.75rem; font-weight: 600; color: #94a3b8; text-transform: uppercase; display: block; margin-bottom: 0.25rem; }
.val { font-size: 1rem; font-weight: 600; color: #334155; }

.loc-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.timeline-row { display: flex; align-items: center; gap: 1rem; }
.time-divider { flex: 1; height: 1px; background: #e2e8f0; position: relative; }
.time-divider::after { content: '→'; position: absolute; right: 0; top: -10px; color: #cbd5e1; font-size: 0.8rem; }

.finance-block { background: #f8fafc; padding: 1.5rem; border-radius: 16px; border: 1px solid #f1f5f9; display: flex; flex-direction: column; gap: 1rem; }
.invoice-row { display: flex; justify-content: space-between; align-items: center; color: #334155; }
.invoice-row.total { border-top: 1px dashed #e2e8f0; padding-top: 1rem; margin-top: 0.5rem; }
.total-val { font-size: 1.5rem; font-weight: 800; color: #0f172a; }
.card-label { font-family: monospace; font-size: 0.95rem; font-weight: 700; }

/* ── Card Footer ── */
.card-footer { padding: 1.5rem 2rem; background: #fcfdfe; border-top: 1px solid #f1f5f9; }

.payment-instruction { margin-bottom: 1.25rem; }
.payment-instruction p { margin: 0; color: #64748b; font-size: 0.9rem; line-height: 1.5; }
.payment-instruction strong { color: #0f172a; }

.btn-primary { padding: 1rem 1.75rem; border-radius: 14px; font-weight: 750; cursor: pointer; border: none; transition: 0.2s; background: #0f172a; color: white; width: 100%; font-size: 1rem; }
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 10px 20px -5px rgba(15, 23, 42, 0.25); }
.btn-primary.return { background: #f97316; }
.btn-primary.return:hover { background: #ea580c; box-shadow: 0 10px 20px -5px rgba(249, 115, 22, 0.25); }

.paid-confirmation { display: flex; justify-content: space-between; align-items: center; }
.receipt-label { font-size: 1.1rem; font-weight: 800; color: #10b981; display: flex; align-items: center; gap: 0.5rem; }
.receipt-info { font-size: 0.9rem; color: #64748b; }
.receipt-info strong { color: #0f172a; }

.state-msg { text-align: center; padding: 5rem; color: #64748b; font-size: 1.1rem; }
.empty-state { text-align: center; padding: 5rem; background: white; border-radius: 20px; border: 1px dashed #cbd5e1; }
.empty-icon { font-size: 4rem; margin-bottom: 1.5rem; opacity: 0.3; }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.pulse { animation: pulse 2s infinite; }

.fade-in { animation: fadeIn 0.8s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 768px) {
  .card-body { grid-template-columns: 1fr; }
  .loc-grid { grid-template-columns: 1fr; }
}
</style>
