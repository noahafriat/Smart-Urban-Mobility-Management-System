<script setup lang="ts">
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
  <div class="rentals-view">
    <header class="page-header">
      <h1>My Rentals</h1>
      <p>Manage your active trips and payment history.</p>
    </header>

    <div v-if="rentalStore.loading && rentalStore.rentals.length === 0" class="state-msg pulse">
      Loading your trips...
    </div>

    <div v-else-if="rentalStore.error" class="state-msg error">
      {{ rentalStore.error }}
    </div>

    <div v-else-if="rentalStore.rentals.length === 0" class="empty-state">
      <div class="empty-icon">🚲</div>
      <h2>No rental history found.</h2>
      <p>Go to the Vehicle Search page to start your first trip!</p>
      <RouterLink to="/vehicles" class="action-btn">Find a Vehicle</RouterLink>
    </div>

    <div v-else class="rental-list">
      <div v-for="rental in rentalStore.rentals" :key="rental.id" class="rental-card">
        
        <!-- Status Badge -->
        <span class="status-badge" :class="rental.status.toLowerCase()">
          Current Status: {{ rental.status }}
        </span>

        <div class="r-details">
          <div class="r-vehicle">
            <h3>{{ rental.vehicle.type === 'CAR' ? rental.vehicle.model : 'Scooter' }}</h3>
            <p><strong>Code:</strong> {{ rental.vehicle.vehicleCode }}</p>
            <p><strong>Location:</strong> {{ rental.vehicle.locationCity }} / {{ rental.vehicle.locationZone }}</p>
            <p v-if="rental.vehicle.licensePlate"><strong>License:</strong> {{ rental.vehicle.licensePlate }}</p>
          </div>

        <div class="r-timeline">
            <p><strong>Started:</strong> {{ formatTime(rental.startTime) }}</p>
            <p><strong>Ended:</strong> {{ formatTime(rental.endTime) }}</p>

            <div v-if="rental.reservationPaymentStatus === 'SUCCEEDED'" class="payment-summary">
              <p><strong>Reservation Payment:</strong> {{ rental.reservationPaymentMethod }}</p>
              <p><strong>Charged at Booking:</strong> ${{ rental.reservationPaymentAmount?.toFixed(2) }}</p>
            </div>
            
            <div class="cost-box" v-if="rental.status !== 'ACTIVE'">
              <strong>Total Invoice:</strong> <span class="cost">${{ rental.totalCost?.toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <div class="r-actions">
          <button 
            v-if="rental.status === 'ACTIVE'" 
            @click="handleReturn(rental.id)"
            :disabled="rentalStore.loading"
            class="action-btn return">
            End Trip & Return
          </button>

          <button 
            v-else-if="rental.status === 'COMPLETED'" 
            @click="handlePay(rental.id)"
            :disabled="rentalStore.loading"
            class="action-btn pay">
            Pay Invoice Now
          </button>
          
          <div class="receipt" v-else-if="rental.status === 'PAID'">
            ✅ Paid in full using {{ auth.user?.paymentInfo || "card" }}
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.rentals-view {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  font-size: 2.25rem;
  color: #1a202c;
  margin: 0 0 0.5rem;
}

.page-header p {
  color: #718096;
  font-size: 1.1rem;
  margin: 0;
}

.state-msg {
  text-align: center;
  padding: 4rem;
  color: #718096;
  font-size: 1.25rem;
}

.pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}

.empty-state {
  text-align: center;
  background: white;
  padding: 4rem 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h2 {
  color: #2d3748;
  margin-top: 0;
}

.empty-state p {
  color: #718096;
  margin-bottom: 2rem;
}

.action-btn {
  display: inline-block;
  padding: 0.85rem 1.5rem;
  background: #3182ce;
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: 0.2s;
  border: none;
  cursor: pointer;
  font-size: 1rem;
}

.action-btn:hover:not(:disabled) {
  background: #2b6cb0;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.return { background: #ed8936; }
.action-btn.return:hover:not(:disabled) { background: #dd6b20; }

.action-btn.pay { background: #38a169; width: 100%; }
.action-btn.pay:hover:not(:disabled) { background: #2f855a; }

.rental-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.rental-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 10px rgba(0,0,0,0.03);
  position: relative;
}

.status-badge {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  padding: 0.4rem 0.8rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.status-badge.active { background: #feebc8; color: #c05621; }
.status-badge.completed { background: #e2e8f0; color: #4a5568; }
.status-badge.paid { background: #c6f6d5; color: #22543d; }

.r-details {
  display: flex;
  justify-content: space-between;
  margin-top: 2rem;
  margin-bottom: 1.5rem;
}

.r-vehicle h3 {
  margin: 0 0 0.5rem;
  font-size: 1.25rem;
  color: #1a202c;
}

.r-vehicle p, .r-timeline p {
  margin: 0 0 0.5rem;
  color: #4a5568;
}

.cost-box {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px dashed #e2e8f0;
}

.payment-summary {
  margin-top: 1rem;
  padding: 0.85rem 1rem;
  background: #eff6ff;
  border-radius: 10px;
  color: #1d4ed8;
}

.cost-box .cost {
  font-size: 1.25rem;
  font-weight: 800;
  color: #2d3748;
}

.r-actions {
  border-top: 1px solid #edf2f7;
  padding-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.receipt {
  font-weight: 600;
  color: #38a169;
  background: #f0fff4;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  width: 100%;
  text-align: center;
}
</style>
