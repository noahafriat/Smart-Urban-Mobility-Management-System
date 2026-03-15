<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useRentalStore } from '../stores/rentals'
import { useVehicleStore } from '../stores/vehicles'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const rentalStore = useRentalStore()
const vehicleStore = useVehicleStore()

const vehicleId = route.params.id as string

onMounted(() => {
  vehicleStore.fetchVehicleById(vehicleId)
})

function getEnergyLabel(vehicle: any) {
  if (vehicle.type === 'CAR') return `${Math.round(vehicle.fuelLevel)}% fuel`
  return `${Math.round(vehicle.batteryLevel)}% battery`
}

async function startReservation() {
  if (!authStore.user) return

  try {
    await rentalStore.reserve(authStore.user.id, vehicleId)
    router.push('/rentals')
  } catch (error) {
    window.alert('Failed to reserve. This vehicle might have just been rented by another user.')
    router.push('/vehicles')
  }
}
</script>

<template>
  <div class="reservation-view">
    <button class="back-btn" @click="router.back()">← Back to Search</button>

    <div v-if="vehicleStore.loading" class="state-msg pulse">
      Loading vehicle details...
    </div>

    <div v-else-if="vehicleStore.error" class="state-msg error">
      {{ vehicleStore.error }}
      <br /><br />
      <button class="btn" @click="router.push('/vehicles')">Return to Search</button>
    </div>

    <div v-else-if="vehicleStore.selectedVehicle" class="vehicle-card">
      <div class="card-header">
        <div class="v-icon">
          {{ vehicleStore.selectedVehicle.type === 'CAR' ? '🚗' : '🛴' }}
        </div>
        <div class="v-title">
          <h1>{{ vehicleStore.selectedVehicle.type === 'CAR' ? vehicleStore.selectedVehicle.model : 'Scooter' }}</h1>
          <p class="v-code">{{ vehicleStore.selectedVehicle.type === 'CAR' ? 'Car' : 'Scooter' }} · {{ vehicleStore.selectedVehicle.vehicleCode }}</p>
        </div>
        <div class="v-energy">
          {{ getEnergyLabel(vehicleStore.selectedVehicle) }}
        </div>
      </div>

      <div class="card-body">
        <div class="info-group">
          <span class="label">Location</span>
          <span class="value">{{ vehicleStore.selectedVehicle.locationCity }} / {{ vehicleStore.selectedVehicle.locationZone }}</span>
        </div>

        <div v-if="vehicleStore.selectedVehicle.licensePlate" class="info-group">
          <span class="label">License Plate</span>
          <span class="value license">{{ vehicleStore.selectedVehicle.licensePlate }}</span>
        </div>

        <div class="pricing-card">
          <h3>Pricing Information</h3>
          <div class="pricing-row">
            <span>Unlock Fee</span>
            <strong>${{ vehicleStore.selectedVehicle.basePrice.toFixed(2) }}</strong>
          </div>
          <div class="pricing-row">
            <span>Rate</span>
            <strong>${{ vehicleStore.selectedVehicle.pricePerMinute.toFixed(2) }} / min</strong>
          </div>
        </div>

        <div class="actions">
          <button 
            class="reserve-btn" 
            :disabled="rentalStore.loading"
            @click="startReservation"
          >
            {{ rentalStore.loading ? 'Reserving...' : 'Start Reservation' }}
          </button>
          <p class="terms">By clicking Start Reservation, you agree to the {{ vehicleStore.selectedVehicle.providerId }} terms of service.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reservation-view {
  padding: 2rem;
  max-width: 600px;
  margin: 0 auto;
}

.back-btn {
  background: none;
  border: none;
  color: #3b82f6;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  padding: 0;
  margin-bottom: 1.5rem;
}

.back-btn:hover {
  text-decoration: underline;
}

.state-msg {
  text-align: center;
  padding: 4rem 2rem;
  color: #64748b;
  font-size: 1.15rem;
  background: white;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.state-msg.error {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fca5a5;
}

.pulse {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.btn {
  padding: 0.6rem 1.2rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

.vehicle-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.card-header {
  padding: 2rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.v-icon {
  font-size: 3rem;
  background: white;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
}

.v-title {
  flex: 1;
}

.v-title h1 {
  margin: 0 0 0.25rem;
  font-size: 1.5rem;
  color: #0f172a;
}

.v-code {
  margin: 0;
  color: #64748b;
  font-family: monospace;
  font-size: 1.1rem;
}

.v-energy {
  background: #dcfce7;
  color: #166534;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.9rem;
}

.card-body {
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.value {
  font-size: 1.15rem;
  color: #0f172a;
  font-weight: 500;
}

.value.license {
  font-family: monospace;
  background: #f1f5f9;
  padding: 0.4rem 0.8rem;
  border-radius: 6px;
  display: inline-block;
  width: max-content;
}

.pricing-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.5rem;
  margin-top: 0.5rem;
}

.pricing-card h3 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
  color: #334155;
}

.pricing-row {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-bottom: 1px dashed #cbd5e1;
  font-size: 1.1rem;
  color: #475569;
}

.pricing-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.pricing-row strong {
  color: #0f172a;
  font-size: 1.2rem;
}

.actions {
  margin-top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.reserve-btn {
  width: 100%;
  padding: 1.2rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1.25rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
}

.reserve-btn:hover:not(:disabled) {
  background: #1d4ed8;
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
}

.reserve-btn:disabled {
  background: #94a3b8;
  box-shadow: none;
  cursor: not-allowed;
}

.terms {
  text-align: center;
  font-size: 0.85rem;
  color: #94a3b8;
  margin: 0;
}
</style>
