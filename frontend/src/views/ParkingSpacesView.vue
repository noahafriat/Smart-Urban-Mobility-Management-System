<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

function isMunicipalGarage(providerId?: string | null): boolean {
  return !providerId || providerId === '__CITY__' || providerId === 'city-admin-id'
}

interface ParkingGarage {
  id: string
  providerId?: string
  name: string
  address: string
  latitude: number
  longitude: number
  totalSpaces: number
  availableSpaces: number
  flatRate?: number
}

interface EnrichedReservation {
  id: string
  garageId: string
  garageName: string
  spots: number
  status: string
  startTime: string
  endTime: string
  paymentMethod?: string
  paymentAmount?: number
  paymentStatus?: string
}

const auth = useAuthStore()
const garages = ref<ParkingGarage[]>([])
const loading = ref(true)
const error = ref('')
const route = useRoute()
const actionMsg = ref('')
const spotsToBook = ref<Record<string, number>>({})

const myReservations = ref<EnrichedReservation[]>([])
const resLoading = ref(false)

const checkoutGarage = ref<ParkingGarage | null>(null)
const checkoutError = ref('')
const selectedPaymentMethod = ref('')
const cardType = ref('VISA')
const cardholderName = ref('')
const cardNumber = ref('')
const expiryMonth = ref('')
const expiryYear = ref('')
const cvv = ref('')
const savePaymentMethod = ref(true)
const checkoutSubmitting = ref(false)

const currentYear = new Date().getFullYear() % 100
const years = Array.from({ length: 10 }, (_, i) => (currentYear + i).toString().padStart(2, '0'))
const months = Array.from({ length: 12 }, (_, i) => (i + 1).toString().padStart(2, '0'))
const supportedCardTypes = [
  { value: 'VISA', label: 'Visa' },
  { value: 'MASTERCARD', label: 'Mastercard' },
  { value: 'AMEX', label: 'American Express' },
  { value: 'CARD', label: 'Other / Generic' },
]

const activeReservations = computed(() =>
  myReservations.value.filter((r) => r.status === 'ACTIVE'),
)

const savedMethods = computed(() => auth.user?.paymentMethods || [])

const checkoutSpots = computed(() => {
  const g = checkoutGarage.value
  if (!g) return 1
  return Math.max(1, Math.floor(Number(spotsToBook.value[g.id]) || 1))
})

const checkoutTotal = computed(() => {
  const g = checkoutGarage.value
  if (!g) return 0
  const rate = typeof g.flatRate === 'number' ? g.flatRate : 0
  return Math.round(rate * checkoutSpots.value * 100) / 100
})

watch(checkoutGarage, (g) => {
  checkoutError.value = ''
  if (!g) return
  if (savedMethods.value.length > 0) {
    selectedPaymentMethod.value = savedMethods.value[0]!
  } else {
    selectedPaymentMethod.value = 'NEW'
  }
})

function onlyNumbers(e: KeyboardEvent) {
  if (!/[0-9]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Tab') {
    e.preventDefault()
  }
}

function openCheckout(g: ParkingGarage) {
  if (!auth.isCitizen || !auth.user?.id) return
  checkoutGarage.value = g
}

function closeCheckout() {
  checkoutGarage.value = null
  checkoutError.value = ''
}

function labelForGarage(g: ParkingGarage) {
  if (isMunicipalGarage(g.providerId)) return 'City facility'
  return 'Partner garage'
}

function formatFlatRate(g: ParkingGarage): string {
  const n = typeof g.flatRate === 'number' ? g.flatRate : 0
  return n.toFixed(2)
}

async function fetchGarages() {
  try {
    const res = await api.get<ParkingGarage[]>('/parking-garages')
    garages.value = res.data
    for (const g of res.data) {
      if (spotsToBook.value[g.id] == null) {
        spotsToBook.value[g.id] = 1
      }
    }
  } catch (err: any) {
    error.value = err.response?.data?.error ?? err.message ?? 'Failed to load parking garages'
  } finally {
    loading.value = false
  }
}

async function fetchMyReservations() {
  if (!auth.isCitizen || !auth.user?.id) {
    myReservations.value = []
    return
  }
  resLoading.value = true
  try {
    const res = await api.get<EnrichedReservation[]>(`/parking-reservations/user/${auth.user.id}`)
    myReservations.value = res.data
  } catch {
    myReservations.value = []
  } finally {
    resLoading.value = false
  }
}

onMounted(() => {
  void fetchGarages()
  void fetchMyReservations()
})

async function refreshAll() {
  loading.value = true
  error.value = ''
  await fetchGarages()
  await fetchMyReservations()
  loading.value = false
}

async function confirmCheckout() {
  const g = checkoutGarage.value
  if (!g || !auth.user?.id) return
  checkoutError.value = ''
  const spots = checkoutSpots.value

  const isNewCard = selectedPaymentMethod.value === 'NEW'
  let paymentInfo = ''
  if (isNewCard) {
    if (!cardholderName.value || !cardNumber.value || !expiryMonth.value || !expiryYear.value || !cvv.value) {
      checkoutError.value = 'Please fill out all credit card details.'
      return
    }
    const digits = cardNumber.value.replace(/\D/g, '')
    const last4 = digits.slice(-4)
    paymentInfo = `${cardType.value}-${last4}`
  } else {
    paymentInfo = selectedPaymentMethod.value
  }

  checkoutSubmitting.value = true
  try {
    await api.post('/parking-reservations', {
      userId: auth.user.id,
      garageId: g.id,
      spots,
      paymentInfo,
      savePaymentMethod: isNewCard && savePaymentMethod.value,
    })
    if (isNewCard && savePaymentMethod.value) {
      await auth.fetchUserProfile(auth.user.id)
    }
    actionMsg.value = `Reserved ${spots} spot(s) at ${g.name}. Charged $${checkoutTotal.value.toFixed(2)}.`
    closeCheckout()
    await fetchGarages()
    await fetchMyReservations()
  } catch (e: any) {
    checkoutError.value = e.response?.data?.error ?? e.message ?? 'Reservation failed'
  } finally {
    checkoutSubmitting.value = false
  }
}

async function completeReservation(r: EnrichedReservation) {
  if (!auth.user?.id) return
  try {
    await api.post(`/parking-reservations/${r.id}/complete`, { userId: auth.user.id })
    await fetchGarages()
    await fetchMyReservations()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Could not end stay')
  }
}

async function cancelReservation(r: EnrichedReservation) {
  if (!auth.user?.id) return
  try {
    await api.post(`/parking-reservations/${r.id}/cancel`, { userId: auth.user.id })
    await fetchGarages()
    await fetchMyReservations()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Could not cancel')
  }
}

const getAvailabilityClass = (available: number, total: number) => {
  if (total === 0) return 'empty'
  const ratio = available / total
  if (ratio > 0.5) return 'high'
  if (ratio > 0.1) return 'medium'
  return 'low'
}
</script>

<template>
  <div class="parking-page">
    <header class="page-header">
      <div class="header-content">
        <h1>🅿️ Parking Spaces</h1>
        <p>
          {{ auth.isCitizen ? 'Reserve spots and track your active parking.' : 'Live capacity at every garage (same data citizens see).' }}
        </p>
      </div>
      <div class="header-actions">
        <button type="button" class="refresh-header-btn" :disabled="loading" @click="refreshAll">Refresh</button>
        <RouterLink to="/dashboard" class="back-btn">Dashboard</RouterLink>
      </div>
    </header>

    <p v-if="actionMsg" class="action-banner">{{ actionMsg }}</p>

    <section v-if="auth.isCitizen" class="my-parking panel">
      <h2>Your parking</h2>
      <p v-if="resLoading" class="muted">Loading reservations…</p>
      <p v-else-if="activeReservations.length === 0" class="muted">No active parking. Reserve a spot below.</p>
      <ul v-else class="res-list">
        <li v-for="r in activeReservations" :key="r.id" class="res-item">
          <div>
            <strong>{{ r.garageName }}</strong>
            <span class="muted"
              >{{ r.spots }} spot(s) · ${{ (r.paymentAmount ?? 0).toFixed(2) }} · {{ r.paymentMethod ?? '—' }} · since
              {{ r.startTime }}</span
            >
          </div>
          <div class="res-btns">
            <button type="button" class="btn-done" @click="completeReservation(r)">End stay</button>
            <button type="button" class="btn-cancel" @click="cancelReservation(r)">Cancel</button>
          </div>
        </li>
      </ul>
    </section>

    <div v-if="loading" class="loading-state">Loading parking data...</div>

    <div v-else-if="error" class="error-state">
      {{ error }}
      <button class="retry-btn" @click="refreshAll">Retry</button>
    </div>

    <div v-else class="garages-grid">
      <div
        v-for="garage in garages"
        :key="garage.id"
        class="garage-card"
        :class="{ 'highlighted-garage': garage.id === route.query.selectedId }"
      >
        <div class="card-top">
          <h3>{{ garage.name }}</h3>
          <span class="source-pill">{{ labelForGarage(garage) }}</span>
        </div>

        <div class="availability-meter">
          <div class="meter-text">
            <span class="count">{{ garage.availableSpaces }}</span>
            <span class="total">/ {{ garage.totalSpaces }} available</span>
          </div>
          <div class="meter-bar-bg">
            <div
              class="meter-bar-fill"
              :class="getAvailabilityClass(garage.availableSpaces, garage.totalSpaces)"
              :style="{ width: `${(garage.availableSpaces / garage.totalSpaces) * 100}%` }"
            ></div>
          </div>
        </div>

        <div class="location-info">
          📍 {{ garage.address || `${garage.latitude.toFixed(4)}, ${garage.longitude.toFixed(4)}` }}
        </div>
        <p class="flat-rate-line">Flat rate: ${{ formatFlatRate(garage) }} <span class="rate-note">(per session)</span></p>

        <div v-if="auth.isCitizen" class="reserve-row">
          <label>
            Spots
            <input
              v-model.number="spotsToBook[garage.id]"
              type="number"
              min="1"
              :max="Math.max(1, garage.availableSpaces)"
              class="spots-input"
            />
          </label>
          <button
            type="button"
            class="reserve-btn"
            :disabled="garage.availableSpaces < 1"
            @click="openCheckout(garage)"
          >
            Reserve &amp; pay
          </button>
        </div>

        <RouterLink
          :to="`/mobility-map?lat=${garage.latitude}&lng=${garage.longitude}`"
          class="map-link mt-2 block"
        >
          View on Map →
        </RouterLink>
      </div>

      <div v-if="garages.length === 0" class="empty-state">No parking spaces found.</div>
    </div>

    <Teleport to="body">
      <div v-if="checkoutGarage" class="checkout-backdrop" @click.self="closeCheckout">
        <div class="checkout-dialog" role="dialog" aria-modal="true">
          <div class="checkout-head">
            <h3>Pay for parking</h3>
            <button type="button" class="checkout-x" aria-label="Close" @click="closeCheckout">×</button>
          </div>
          <p class="checkout-garage-name">{{ checkoutGarage.name }}</p>
          <p class="checkout-summary">
            {{ checkoutSpots }} spot(s) × ${{ formatFlatRate(checkoutGarage) }} =
            <strong>${{ checkoutTotal.toFixed(2) }}</strong>
            <span class="checkout-note">Flat rate charged upfront (same idea as vehicle reservation).</span>
          </p>

          <div class="method-selector">
            <div
              v-for="method in savedMethods"
              :key="method"
              class="method-option"
              :class="{ selected: selectedPaymentMethod === method }"
              @click="selectedPaymentMethod = method"
            >
              <div class="radio-custom" />
              <div class="m-info">
                <span class="m-brand">{{ method.split('-')[0] }}</span>
                <span class="m-val">•••• {{ method.split('-')[1] }}</span>
              </div>
            </div>
            <div
              class="method-option"
              :class="{ selected: selectedPaymentMethod === 'NEW' }"
              @click="selectedPaymentMethod = 'NEW'"
            >
              <div class="radio-custom" />
              <div class="m-info">
                <span class="m-label">New card</span>
                <span class="m-val">Enter card details</span>
              </div>
            </div>
          </div>

          <div v-if="selectedPaymentMethod === 'NEW'" class="new-card-block">
            <div class="form-row">
              <label>Card type</label>
              <select v-model="cardType" class="full-input">
                <option v-for="b in supportedCardTypes" :key="b.value" :value="b.value">{{ b.label }}</option>
              </select>
            </div>
            <div class="form-row">
              <label>Cardholder</label>
              <input v-model="cardholderName" class="full-input" placeholder="Name on card" />
            </div>
            <div class="form-row">
              <label>Card number</label>
              <input
                v-model="cardNumber"
                class="full-input"
                maxlength="19"
                placeholder="0000 0000 0000 0000"
                @keypress="onlyNumbers"
              />
            </div>
            <div class="form-row-grid">
              <div>
                <label>Expiry</label>
                <div class="expiry-row">
                  <select v-model="expiryMonth" class="full-input">
                    <option value="" disabled>MM</option>
                    <option v-for="m in months" :key="m" :value="m">{{ m }}</option>
                  </select>
                  <select v-model="expiryYear" class="full-input">
                    <option value="" disabled>YY</option>
                    <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
                  </select>
                </div>
              </div>
              <div>
                <label>CVV</label>
                <input v-model="cvv" class="full-input" maxlength="4" placeholder="CVV" @keypress="onlyNumbers" />
              </div>
            </div>
            <label class="save-row">
              <input v-model="savePaymentMethod" type="checkbox" />
              Save this card to my account
            </label>
          </div>

          <p v-if="checkoutError" class="checkout-err">{{ checkoutError }}</p>

          <div class="checkout-actions">
            <button type="button" class="btn-secondary" :disabled="checkoutSubmitting" @click="closeCheckout">
              Cancel
            </button>
            <button type="button" class="btn-primary" :disabled="checkoutSubmitting" @click="confirmCheckout">
              {{ checkoutSubmitting ? 'Processing…' : `Pay $${checkoutTotal.toFixed(2)}` }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.parking-page {
  padding: 2rem clamp(1rem, 2vw, 2.5rem);
  width: min(96vw, 1200px);
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  background: white;
  padding: 1.5rem 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  gap: 1rem;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.refresh-header-btn {
  padding: 0.65rem 1.2rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-weight: 600;
  cursor: pointer;
}

.page-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  color: #1a202c;
  font-weight: 700;
}

.page-header p {
  margin: 0;
  color: #718096;
}

.action-banner {
  background: #ecfdf5;
  border: 1px solid #6ee7b7;
  color: #065f46;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  margin-bottom: 1rem;
}

.panel {
  background: white;
  border-radius: 12px;
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.panel h2 {
  margin: 0 0 0.75rem;
  font-size: 1.15rem;
}

.muted {
  color: #64748b;
  font-size: 0.9rem;
}

.res-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.res-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f5f9;
}

.res-item:last-child {
  border-bottom: none;
}

.res-btns {
  display: flex;
  gap: 0.5rem;
}

.btn-done {
  padding: 0.45rem 0.85rem;
  border-radius: 8px;
  border: none;
  background: #0d9488;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel {
  padding: 0.45rem 0.85rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: white;
  font-weight: 600;
  cursor: pointer;
}

.back-btn {
  padding: 0.75rem 1.5rem;
  background: #edf2f7;
  color: #2d3748;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #e2e8f0;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 4rem;
  background: white;
  border-radius: 12px;
  color: #718096;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.error-state {
  color: #c53030;
  background: #fff5f5;
  border: 1px solid #fed7d7;
}

.retry-btn {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  background: #c53030;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.garages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.garage-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s;
}

.garage-card:hover {
  transform: translateY(-4px);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.garage-card h3 {
  margin: 0;
  font-size: 1.25rem;
  color: #2d3748;
}

.source-pill {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  background: #e0e7ff;
  color: #3730a3;
  white-space: nowrap;
}

.availability-meter {
  margin-bottom: 1.5rem;
}

.meter-text {
  margin-bottom: 0.5rem;
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.count {
  font-size: 1.5rem;
  font-weight: 700;
  color: #2b6cb0;
}

.total {
  color: #718096;
  font-size: 0.9rem;
}

.meter-bar-bg {
  height: 8px;
  background: #edf2f7;
  border-radius: 4px;
  overflow: hidden;
}

.meter-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.meter-bar-fill.high {
  background: #38a169;
}
.meter-bar-fill.medium {
  background: #d69e2e;
}
.meter-bar-fill.low {
  background: #e53e3e;
}
.meter-bar-fill.empty {
  background: #cbd5e0;
}

.location-info {
  font-size: 0.85rem;
  color: #a0aec0;
  padding-top: 1rem;
  border-top: 1px solid #edf2f7;
}

.flat-rate-line {
  margin: 0.5rem 0 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #2d3748;
}

.flat-rate-line .rate-note {
  font-weight: 500;
  font-size: 0.8rem;
  color: #718096;
}

.reserve-row {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.reserve-row label {
  display: flex;
  flex-direction: column;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  gap: 0.25rem;
}

.spots-input {
  width: 4rem;
  padding: 0.4rem 0.5rem;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.reserve-btn {
  padding: 0.5rem 1.1rem;
  border-radius: 8px;
  border: none;
  background: #2563eb;
  color: white;
  font-weight: 700;
  cursor: pointer;
}

.reserve-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.map-link {
  font-size: 0.82rem;
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  display: inline-block;
  margin-top: 0.75rem;
}

.map-link:hover {
  text-decoration: underline;
}

.highlighted-garage {
  border: 2px solid #3b82f6 !important;
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.4) !important;
  transform: translateY(-4px);
}

.checkout-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.checkout-dialog {
  background: #fff;
  border-radius: 16px;
  width: min(440px, 100%);
  max-height: 90vh;
  overflow: auto;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.checkout-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.checkout-head h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #0f172a;
}

.checkout-x {
  border: none;
  background: #f1f5f9;
  width: 2rem;
  height: 2rem;
  border-radius: 8px;
  font-size: 1.25rem;
  cursor: pointer;
  line-height: 1;
}

.checkout-garage-name {
  margin: 0 0 0.5rem;
  font-weight: 700;
  color: #334155;
}

.checkout-summary {
  margin: 0 0 1rem;
  font-size: 0.95rem;
  color: #475569;
  line-height: 1.5;
}

.checkout-summary strong {
  color: #0f172a;
}

.checkout-note {
  display: block;
  font-size: 0.78rem;
  color: #94a3b8;
  margin-top: 0.35rem;
}

.method-selector {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.method-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.method-option.selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.radio-custom {
  width: 18px;
  height: 18px;
  border: 2px solid #cbd5e1;
  border-radius: 50%;
  flex-shrink: 0;
}

.method-option.selected .radio-custom {
  border-color: #2563eb;
  box-shadow: inset 0 0 0 4px #2563eb;
}

.m-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.m-brand {
  font-size: 0.7rem;
  font-weight: 800;
  color: #64748b;
  text-transform: uppercase;
}

.m-label {
  font-weight: 700;
  font-size: 0.85rem;
  color: #0f172a;
}

.m-val {
  font-size: 0.8rem;
  color: #64748b;
}

.new-card-block {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #f8fafc;
  border-radius: 10px;
}

.form-row label,
.form-row-grid label {
  display: block;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  margin-bottom: 0.25rem;
}

.full-input {
  width: 100%;
  padding: 0.5rem 0.6rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  box-sizing: border-box;
}

.form-row-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.expiry-row {
  display: flex;
  gap: 0.5rem;
}

.save-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #475569;
  cursor: pointer;
}

.checkout-err {
  color: #b91c1c;
  font-size: 0.85rem;
  margin: 0 0 0.75rem;
}

.checkout-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.btn-secondary {
  padding: 0.6rem 1rem;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary {
  padding: 0.6rem 1.15rem;
  border-radius: 10px;
  border: none;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
