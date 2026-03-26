<script setup lang="ts">
/**
 * Vehicle Reservation Completion.
 * Handles final billing authorization and asset locking.
 */
import { computed, onMounted, ref } from 'vue'
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
const reservationError = ref('')

// Payment Selection
const selectedPaymentMethod = ref('') // Stores the actual card label or 'NEW'
const cardType = ref('VISA')
const cardholderName = ref('')
const cardNumber = ref('')
const expiryMonth = ref('')
const expiryYear = ref('')
const cvv = ref('')
const savePaymentMethod = ref(true)

// Restricted Input Helpers
const currentYear = new Date().getFullYear() % 100
const years = Array.from({ length: 10 }, (_, i) => (currentYear + i).toString().padStart(2, '0'))
const months = Array.from({ length: 12 }, (_, i) => (i + 1).toString().padStart(2, '0'))

function onlyNumbers(e: KeyboardEvent) {
  if (!/[0-9]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Tab') {
    e.preventDefault()
  }
}

const savedMethods = computed(() => authStore.user?.paymentMethods || [])
const hasSavedMethods = computed(() => savedMethods.value.length > 0)

onMounted(() => {
  vehicleStore.fetchVehicleById(vehicleId)
  if (hasSavedMethods.value) {
    selectedPaymentMethod.value = savedMethods.value[0]!
  } else {
    selectedPaymentMethod.value = 'NEW'
  }
})

function getEnergyLabel(vehicle: any) {
  if (vehicle.type === 'CAR') return `${Math.round(vehicle.fuelLevel)}% fuel`
  return `${Math.round(vehicle.batteryLevel)}% battery`
}

async function startReservation() {
  if (!authStore.user) return
  reservationError.value = ''

  const isNewCard = selectedPaymentMethod.value === 'NEW'
  let paymentInfo = ''

  if (isNewCard) {
    if (!cardholderName.value || !cardNumber.value || !expiryMonth.value || !expiryYear.value || !cvv.value) {
      reservationError.value = 'Please fill out all credit card details.'
      return
    }
    // Format as TYPE-LAST4 for the simulation label
    const digits = cardNumber.value.replace(/\D/g, '')
    const last4 = digits.slice(-4)
    paymentInfo = `${cardType.value}-${last4}`
  } else {
    paymentInfo = selectedPaymentMethod.value
  }

  try {
    await rentalStore.reserve(authStore.user.id, vehicleId, {
      paymentInfo,
      savePaymentMethod: isNewCard && savePaymentMethod.value,
    })
    
    // Refresh user's saved wallets if new card was cached
    if (isNewCard && savePaymentMethod.value) {
      await authStore.fetchUserProfile(authStore.user.id)
    }
    
    // Redirect to activity feed
    router.push('/rentals')
  } catch (error: any) {
    reservationError.value = error.message || 'Failed to authorize reservation.'
  }
}
</script>

<template>
  <div class="reservation-view fade-in">
    <button class="back-link" @click="router.back()">← Modify Reservation</button>

    <div v-if="vehicleStore.loading" class="state-msg pulse">
       Establishing secure handshake with fleet node...
    </div>

    <div v-else-if="vehicleStore.error" class="state-msg error">
      {{ vehicleStore.error }}
      <button class="btn-retry" @click="router.push('/vehicles')">Return to Search</button>
    </div>

    <div v-else-if="vehicleStore.selectedVehicle" class="reservation-layout">
      
      <!-- ── Left: Summary ── -->
      <section class="summary-panel">
        <header class="v-header">
          <div class="v-icon-box">
             {{ vehicleStore.selectedVehicle.type === 'CAR' ? '🚗' : '🛴' }}
          </div>
          <div class="v-details">
             <h1>{{ vehicleStore.selectedVehicle.type === 'CAR' ? vehicleStore.selectedVehicle.model : 'Eco-Scooter Fleet' }}</h1>
             <span class="v-hash">#{{ vehicleStore.selectedVehicle.vehicleCode }}</span>
          </div>
        </header>

        <div class="specs-grid">
          <div class="spec">
            <span class="label">Primary Hub</span>
            <span class="val">{{ vehicleStore.selectedVehicle.locationCity }} / {{ vehicleStore.selectedVehicle.locationZone }}</span>
          </div>
          <div class="spec">
            <span class="label">Energy Status</span>
            <span class="val">{{ getEnergyLabel(vehicleStore.selectedVehicle) }}</span>
          </div>
          <div v-if="vehicleStore.selectedVehicle.licensePlate" class="spec">
            <span class="label">Fleet ID</span>
            <span class="val plate">{{ vehicleStore.selectedVehicle.licensePlate }}</span>
          </div>
        </div>

        <div class="pricing-context">
           <h3>Financial Estimation</h3>
           <div class="price-row">
             <span>Reservation Charge</span>
             <strong>${{ vehicleStore.selectedVehicle.basePrice.toFixed(2) }}</strong>
           </div>
           <div class="price-row">
             <span>Post-Usage Rate</span>
             <strong>${{ vehicleStore.selectedVehicle.pricePerMinute.toFixed(2) }} / min</strong>
           </div>
           <p class="billing-note">The reservation charge and trip duration will be billed as a single unified invoice upon vehicle return.</p>
        </div>
      </section>

      <!-- ── Right: Checkout ── -->
      <section class="payment-panel">
         <h2>Authorize Mobility Session</h2>
         <p class="p-sub">Select a payment method to finalize your booking.</p>

          <div class="method-selector">
            <div 
              v-for="method in savedMethods" 
              :key="method" 
              class="method-option"
              :class="{ selected: selectedPaymentMethod === method }"
              @click="selectedPaymentMethod = method"
            >
               <div class="radio-custom"></div>
               <div class="m-info">
                  <div class="brand-badge">{{ method.split('-')[0] }}</div>
                  <span class="m-val">•••• {{ method.split('-')[1] }}</span>
               </div>
            </div>

            <div 
              class="method-option"
              :class="{ selected: selectedPaymentMethod === 'NEW' }"
              @click="selectedPaymentMethod = 'NEW'"
            >
               <div class="radio-custom"></div>
               <div class="m-info">
                  <span class="m-label">New Payment Method</span>
                  <span class="m-val">Add a new credit card</span>
               </div>
            </div>
          </div>

         <!-- New Card Form -->
         <div v-if="selectedPaymentMethod === 'NEW'" class="new-card-form">
            <div class="form-row">
              <label>Issuer Network</label>
              <select v-model="cardType" class="modern-select">
                <option value="VISA">VISA</option>
                <option value="MASTERCARD">Mastercard</option>
                <option value="AMEX">Amex</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div class="form-row">
              <label>Full Name</label>
              <input v-model="cardholderName" placeholder="e.g. John Smith" />
            </div>
            <div class="form-row">
              <label>Account Number</label>
              <input 
                v-model="cardNumber" 
                placeholder="0000 0000 0000 0000" 
                maxlength="19"
                @keypress="onlyNumbers" 
              />
            </div>
            <div class="form-grid">
               <div class="form-row">
                  <label>Expiry Date</label>
                  <div class="dual-input">
                    <select v-model="expiryMonth" class="small-select">
                      <option value="" disabled selected>MM</option>
                      <option v-for="m in months" :key="m" :value="m">{{ m }}</option>
                    </select>
                    <select v-model="expiryYear" class="small-select">
                      <option value="" disabled selected>YY</option>
                      <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
                    </select>
                  </div>
               </div>
               <div class="form-row">
                  <label>CVV</label>
                  <input 
                    v-model="cvv" 
                    type="password" 
                    placeholder="***" 
                    maxlength="4" 
                    @keypress="onlyNumbers"
                  />
               </div>
            </div>
            <label class="save-check">
               <input type="checkbox" v-model="savePaymentMethod" />
               <span>Store this card for future trips</span>
            </label>
         </div>

         <div class="final-actions">
            <p v-if="reservationError" class="error-box">{{ reservationError }}</p>
            <button 
              class="btn-checkout" 
              @click="startReservation"
              :disabled="rentalStore.loading"
            >
               {{ rentalStore.loading ? 'Authorizing...' : 'Start Trip' }}
            </button>
            <p class="terms">By proceeding, you authorize a unified charge upon vehicle return according to city transit terms.</p>
         </div>
      </section>

    </div>
  </div>
</template>

<style scoped>
.reservation-view {
  padding: 3rem clamp(1rem, 5vw, 4rem);
  max-width: 1000px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
  color: #0f172a;
}

.back-link { background: none; border: none; font-weight: 700; color: #64748b; cursor: pointer; margin-bottom: 2rem; display: block; transition: 0.2s; }
.back-link:hover { color: #0f172a; text-decoration: underline; }

.reservation-layout { 
  display: grid; 
  grid-template-columns: 1fr 1fr; 
  gap: 3.5rem; 
  background: white; 
  border-radius: 36px; 
  padding: 3.5rem; 
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f1f5f9;
}

/* ── Summary Panel ── */
.v-header { display: flex; align-items: center; gap: 1.5rem; margin-bottom: 3rem; }
.v-icon-box { font-size: 2.75rem; background: #f8fafc; padding: 1.5rem; border-radius: 22px; border: 1px solid #e2e8f0; }
.v-details h1 { margin: 0; font-size: 1.6rem; font-weight: 900; }
.v-hash { color: #94a3b8; font-family: monospace; font-weight: 700; font-size: 1.1rem; }

.specs-grid { display: flex; flex-direction: column; gap: 1.5rem; margin-bottom: 3rem; }
.label { font-size: 0.75rem; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 0.4rem; }
.val { font-size: 1.05rem; font-weight: 700; color: #334155; }
.val.plate { font-family: 'JetBrains Mono', monospace; background: #f1f5f9; padding: 0.25rem 0.75rem; border-radius: 8px; font-size: 0.95rem; }

.pricing-context { background: #f8fafc; border-radius: 24px; padding: 1.75rem; border: 1px solid #f1f5f9; }
.pricing-context h3 { font-size: 0.9rem; margin: 0 0 1.25rem; color: #64748b; font-weight: 900; text-transform: uppercase; }
.price-row { display: flex; justify-content: space-between; margin-bottom: 0.85rem; font-size: 1.15rem; font-weight: 500; }
.price-row strong { font-weight: 900; color: #0f172a; }
.billing-note { font-size: 0.8rem; color: #94a3b8; margin: 1.75rem 0 0; line-height: 1.5; font-weight: 500; }

/* ── Checkout Panel ── */
.payment-panel h2 { font-size: 1.85rem; font-weight: 900; margin: 0; }
.p-sub { color: #64748b; margin: 0.5rem 0 2.5rem; font-size: 1.05rem; }

.method-selector { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 2.5rem; }
.method-option { 
  display: flex; 
  align-items: center; 
  gap: 1.5rem; 
  padding: 1.5rem; 
  border-radius: 24px; 
  border: 2px solid #f1f5f9; 
  cursor: pointer; 
  transition: 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  background: #fdfdfe;
  position: relative;
}
.method-option:hover { border-color: #cbd5e1; background: white; transform: scale(1.015); }
.method-option.selected { border-color: #3b82f6; background: #f0f7ff; box-shadow: 0 10px 25px -10px rgba(59, 130, 246, 0.15); }

.radio-custom {
  width: 22px;
  height: 22px;
  border: 2px solid #cbd5e1;
  border-radius: 50%;
  background: white;
  position: relative;
  transition: 0.2s;
  flex-shrink: 0;
}
.method-option.selected .radio-custom {
  border-color: #3b82f6;
}
.method-option.selected .radio-custom::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: #3b82f6;
  border-radius: 50%;
}

.m-info { display: flex; flex-direction: column; gap: 0.3rem; }
.brand-badge { 
  font-size: 0.65rem; 
  font-weight: 900; 
  background: white; 
  padding: 0.35rem 0.6rem; 
  border-radius: 8px; 
  border: 1px solid #e2e8f0; 
  width: fit-content; 
  color: #1e293b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.m-val { font-weight: 800; color: #1e293b; font-family: 'JetBrains Mono', monospace; font-size: 1.05rem; }
.m-label { font-size: 0.65rem; font-weight: 900; color: #94a3b8; text-transform: uppercase; display: block; letter-spacing: 0.05em; }

.new-card-form { display: flex; flex-direction: column; gap: 1.25rem; padding: 1.75rem; background: #f8fafc; border-radius: 24px; border: 1px solid #f1f5f9; margin-bottom: 2.5rem; }
.form-row label { display: block; font-size: 0.75rem; font-weight: 800; color: #94a3b8; margin-bottom: 0.5rem; }
input {
  padding: 0.85rem 1.1rem;
}

select {
  padding: 0 0.8rem;
  height: 50px;
  line-height: 50px;
}

input, select { 
  width: 100%; 
  border: 1px solid #e2e8f0; 
  border-radius: 14px; 
  font-size: 1rem; 
  color: #0f172a; 
  font-weight: 500; 
}
input:focus, select:focus { border-color: #3b82f6; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08); outline: none; }

.form-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 1.25rem; }
.dual-input { display: flex; gap: 0.5rem; }
.save-check { display: flex; align-items: center; gap: 0.75rem; font-size: 0.85rem; font-weight: 700; color: #64748b; cursor: pointer; }

.btn-checkout { 
  width: 100%; 
  padding: 1.35rem; 
  background: #0f172a; 
  color: white; 
  border: none; 
  border-radius: 20px; 
  font-weight: 900; 
  font-size: 1.15rem; 
  cursor: pointer; 
  transition: 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.btn-checkout:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 15px 35px -10px rgba(15, 23, 42, 0.5); }
.btn-checkout:disabled { opacity: 0.6; cursor: wait; }

.error-box { background: #fef2f2; color: #ef4444; border: 1px solid #fecaca; padding: 1.25rem; border-radius: 16px; font-weight: 800; margin-bottom: 2rem; font-size: 0.95rem; text-align: center; }
.terms { text-align: center; color: #94a3b8; font-size: 0.75rem; margin-top: 1.25rem; line-height: 1.5; font-weight: 500; }

.state-msg { text-align: center; padding: 6rem; color: #64748b; font-size: 1.2rem; font-weight: 500; }
.pulse { animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.fade-in { animation: fadeIn 0.8s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 900px) {
  .reservation-layout { grid-template-columns: 1fr; padding: 2.5rem; }
}
</style>
