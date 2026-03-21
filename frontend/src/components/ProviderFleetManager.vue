<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  useVehicleStore,
  type ProviderFleetFilters,
  type Vehicle,
  type VehiclePayload,
  type VehicleStatus,
  type VehicleType,
} from '../stores/vehicles'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{
  providerId: string
}>()

const vehicleStore = useVehicleStore()

const locationCatalog: Record<'Montreal' | 'Laval', string[]> = {
  Montreal: [
    'Dock: McGill & Sherbrooke',
    'Dock: Place des Arts',
    'Dock: Guy-Concordia',
    'Dock: Berri-UQAM',
    'Station: Griffintown Surface Lot',
    'Station: Griffintown Underground',
    'FLEX Zone: Mile End',
    'FLEX Zone: Old Montreal'
  ],
  Laval: [
    'Dock: Montmorency Station',
    'Dock: Centropolis',
    'Dock: Cartier Station',
    'Station: Chomedey Reserved',
    'FLEX Zone: Laval-des-Rapides'
  ],
}

type ServiceCity = keyof typeof locationCatalog

const serviceCities = Object.keys(locationCatalog) as ServiceCity[]

const authStore = useAuthStore()
const preferredType = authStore.user?.preferredMobilityType as VehicleType | undefined
const defaultType = preferredType || 'SCOOTER'

const filters = reactive<Required<ProviderFleetFilters>>({
  city: 'All',
  zone: 'All',
  type: preferredType || 'All',
  status: 'All',
  includeHidden: true,
})

const showForm = ref(false)
const editingVehicleId = ref<string | null>(null)

const form = reactive<VehiclePayload>({
  type: defaultType,
  vehicleCode: '',
  locationCity: 'Montreal',
  locationZone: locationCatalog.Montreal[0]!,
  batteryLevel: 100,
  fuelLevel: 0,
  basePrice: 1,
  pricePerMinute: 0.30,
  pricingCategory: 'Standard Scooter',
  maintenanceState: 'READY',
  status: 'AVAILABLE',
  visibleInSearch: true,
  licensePlate: '',
})

const summary = computed(() => vehicleStore.providerSummary)
const vehicles = computed(() => vehicleStore.providerFleet)

const filterZoneOptions = computed(() => {
  if (filters.city !== 'All') {
    return locationCatalog[filters.city as ServiceCity] || []
  }
  return [...locationCatalog.Montreal, ...locationCatalog.Laval]
})

watch(() => filters.city, () => {
  filters.zone = 'All'
})

const vehiclesByLocation = computed(() => {
  const groups: Record<string, Vehicle[]> = {}
  for (const v of vehicles.value) {
    if (filters.zone !== 'All' && v.locationZone !== filters.zone) continue
    const loc = `${v.locationCity} / ${v.locationZone}`
    if (!groups[loc]) groups[loc] = []
    groups[loc].push(v)
  }
  return groups
})

const isCarsExpanded = ref(false)

const breakdownScooters = computed(() => summary.value.byType['Scooter'] || 0)
const breakdownCarsTotal = computed(() => {
  let total = 0
  for (const [key, count] of Object.entries(summary.value.byType)) {
    if (key !== 'Scooter') total += Number(count)
  }
  return total
})
const breakdownCarModels = computed(() => {
  const models: Record<string, number> = {}
  for (const [key, count] of Object.entries(summary.value.byType)) {
    if (key !== 'Scooter') models[key] = Number(count)
  }
  return models
})

function toggleCars() {
  isCarsExpanded.value = !isCarsExpanded.value
}

const cityOptions = computed(() => Object.keys(summary.value.byCity))
const zoneOptions = computed<string[]>(() => {
  const city = form.locationCity as ServiceCity
  if (!city) return []
  const zones = locationCatalog[city]
  if (form.type === 'CAR') {
    return zones.filter(z => !z.includes('Dock:'))
  } else {
    return zones.filter(z => z.includes('Dock:'))
  }
})

const modelOptions = computed(() => {
  return [
    'Honda Civic', 'Toyota Corolla', 'Hyundai Elantra', 
    'Toyota Camry', 'Tesla Model 3', 'BMW 3 Series', 'Ford F-150'
  ]
})

const pricingOptions = computed(() => {
  if (form.type === 'CAR') {
    return ['Economy', 'Premium', 'Luxury', 'Truck']
  }
  return ['Standard Scooter', 'Premium Scooter']
})

watch(
  () => form.type,
  (newType) => {
    if (editingVehicleId.value) return // Don't override when editing existing vehicle
    form.locationZone = zoneOptions.value[0] ?? '' // Snap location to the correct type

    if (newType === 'CAR') {
      form.model = 'Honda Civic'
      form.pricingCategory = 'Economy'
    } else {
      delete form.model
      form.pricingCategory = 'Standard Scooter'
    }
  }
)

watch(
  () => form.pricingCategory,
  (newCat) => {
    if (editingVehicleId.value) return // Let user override independently when editing
    
    // Auto-update price when pricing category changes on creation
    if (newCat === 'Economy') {
      form.basePrice = 15.00
      form.pricePerMinute = 0.40
    } else if (newCat === 'Premium') {
      form.basePrice = 30.00
      form.pricePerMinute = 1.20
    } else if (newCat === 'Luxury') {
      form.basePrice = 40.00
      form.pricePerMinute = 1.50
    } else if (newCat === 'Truck') {
      form.basePrice = 35.00
      form.pricePerMinute = 1.00
    } else if (newCat === 'Standard Scooter') {
      form.basePrice = 1.00
      form.pricePerMinute = 0.30
    } else if (newCat === 'Premium Scooter') {
      form.basePrice = 1.00
      form.pricePerMinute = 0.50
    }
  }
)

onMounted(() => {
  loadFleet()
})

watch(
  () => form.locationCity,
  (city) => {
    const availableZones = city ? [...locationCatalog[city as ServiceCity]] : []
    if (!availableZones.includes(form.locationZone)) {
      form.locationZone = availableZones[0] ?? ''
    }
  },
  { immediate: true },
)

async function loadFleet() {
  await vehicleStore.refreshProviderFleetDashboard(props.providerId, filters)
}

function resetForm() {
  editingVehicleId.value = null
  form.type = defaultType
  form.vehicleCode = ''
  form.locationCity = 'Montreal'
  form.locationZone = locationCatalog.Montreal[0]!
  form.batteryLevel = 100
  form.fuelLevel = 0
  form.basePrice = form.type === 'CAR' ? 15.00 : 1.00
  form.pricePerMinute = form.type === 'CAR' ? 0.40 : 0.30
  form.pricingCategory = form.type === 'CAR' ? 'Economy' : 'Standard Scooter'
  form.maintenanceState = 'READY'
  form.status = 'AVAILABLE'
  form.visibleInSearch = true
  form.licensePlate = ''
  if (form.type === 'CAR') {
    form.model = 'Honda Civic'
  } else {
    delete form.model
  }
}

function openCreateForm() {
  resetForm()
  showForm.value = true
}

function openEditForm(vehicle: Vehicle) {
  editingVehicleId.value = vehicle.id
  form.type = vehicle.type
  form.vehicleCode = vehicle.vehicleCode
  form.locationCity = serviceCities.includes(vehicle.locationCity as ServiceCity)
    ? vehicle.locationCity
    : 'Montreal'
  form.locationZone = zoneOptions.value.includes(vehicle.locationZone)
    ? vehicle.locationZone
    : (zoneOptions.value[0] ?? '')
  form.batteryLevel = vehicle.batteryLevel
  form.fuelLevel = vehicle.fuelLevel
  form.basePrice = vehicle.basePrice
  form.pricePerMinute = vehicle.pricePerMinute
  form.pricingCategory = vehicle.pricingCategory
  form.maintenanceState = vehicle.maintenanceState
  form.status = vehicle.status
  form.visibleInSearch = vehicle.visibleInSearch
  form.licensePlate = vehicle.licensePlate ?? ''
  if (vehicle.type === 'CAR') {
    form.model = vehicle.model ?? 'Honda Civic'
  } else {
    delete form.model
  }
  showForm.value = true
}

async function submitForm() {
  const payload: VehiclePayload = {
    ...form,
    batteryLevel: form.type === 'CAR' ? 0 : Number(form.batteryLevel),
    fuelLevel: form.type === 'CAR' ? Number(form.fuelLevel) : 0,
    licensePlate: form.type === 'CAR' ? form.licensePlate?.trim() : '',
  }

  // Handle auto-generated logic if not editing
  if (!editingVehicleId.value) {
    const randomHex = Math.floor(Math.random() * 0xFFFFFF).toString(16).toUpperCase().padStart(6, '0')
    const locPrefix = form.locationCity === 'Montreal' ? 'MTL' : 'LAV'
    payload.vehicleCode = form.type === 'CAR' ? `CAR-${locPrefix}-${randomHex}` : `SCOOT-${locPrefix}-${randomHex}`
  }

  try {
    if (editingVehicleId.value) {
      await vehicleStore.updateVehicle(props.providerId, editingVehicleId.value, payload, filters)
    } else {
      await vehicleStore.createVehicle(props.providerId, payload, filters)
    }
    showForm.value = false
    resetForm()
  } catch (error: any) {
    window.alert(error.message)
  }
}

async function markMaintenance(vehicle: Vehicle) {
  const reason = window.prompt('Maintenance note', vehicle.maintenanceState === 'READY' ? 'INSPECTION' : vehicle.maintenanceState)
  if (!reason) return
  try {
    await vehicleStore.sendToMaintenance(props.providerId, vehicle.id, reason, filters)
  } catch (error: any) {
    window.alert(error.message)
  }
}

async function restoreVehicle(vehicle: Vehicle) {
  try {
    await vehicleStore.restoreVehicle(props.providerId, vehicle.id, filters)
  } catch (error: any) {
    window.alert(error.message)
  }
}

function relocateVehicle(vehicle: Vehicle) {
  // Can't auto relocate with strict catalogs via simple prompts easily anymore. 
  // Let the user know they need to use the Edit panel now since we strictly validate zones.
  window.alert('Please use the Edit Vehicle form to officially relocate a vehicle to a new dock or zone.')
}

async function deactivateVehicle(vehicle: Vehicle) {
  const approved = window.confirm(`Retire ${vehicle.vehicleCode} from the fleet?`)
  if (!approved) return

  try {
    await vehicleStore.deactivateVehicle(props.providerId, vehicle.id, filters)
  } catch (error: any) {
    window.alert(error.message)
  }
}

function closeForm() {
  showForm.value = false
  resetForm()
}

function getStatusClass(status: VehicleStatus) {
  return status.toLowerCase()
}

function energyLabel(vehicle: Vehicle) {
  if (vehicle.type === 'CAR') {
    return `${Math.round(vehicle.fuelLevel)}% fuel`
  }
  return `${Math.round(vehicle.batteryLevel)}% battery`
}

function vehicleHeading(vehicle: Vehicle) {
  if (vehicle.type === 'SCOOTER') return '🛴 Scooter'
  return `🚗 ${vehicle.model || 'Car'}`
}
</script>

<template>
  <section class="fleet-manager">
    <div class="fleet-topbar">
      <div>
        <h2>Fleet Operations</h2>
        <p>Manage vehicle inventory, availability, maintenance, and search visibility.</p>
      </div>
      <div class="topbar-actions">
        <button class="action-btn secondary" :disabled="vehicleStore.fleetLoading" @click="loadFleet">
          Refresh
        </button>
        <button class="action-btn" @click="openCreateForm">
          Add Vehicle
        </button>
      </div>
    </div>

    <div class="summary-grid">
      <article class="summary-card">
        <span class="eyebrow">Fleet size</span>
        <strong>{{ summary.totalVehicles }}</strong>
        <p>{{ summary.visibleVehicles }} visible in search</p>
      </article>
      <article class="summary-card">
        <span class="eyebrow">Availability</span>
        <strong>{{ summary.availableVehicles }}</strong>
        <p>{{ summary.rentedVehicles }} currently rented</p>
      </article>
      <article class="summary-card">
        <span class="eyebrow">Attention needed</span>
        <strong>{{ summary.issueFlaggedVehicles }}</strong>
        <p>{{ summary.lowEnergyVehicles }} low energy vehicles</p>
      </article>
      <article class="summary-card breakdown-card">
        <span class="eyebrow">Fleet Breakdown</span>
        <div class="breakdown-list">
          <div class="bd-row">
            <span>Scooters</span>
            <strong>{{ breakdownScooters }}</strong>
          </div>
          <div class="bd-row clickable" @click="toggleCars">
            <span>Cars {{ isCarsExpanded ? '▼' : '▶' }}</span>
            <strong>{{ breakdownCarsTotal }}</strong>
          </div>
          <template v-if="isCarsExpanded">
            <div v-for="(count, model) in breakdownCarModels" :key="model" class="bd-row sub-row">
              <span>{{ model }}</span>
              <strong>{{ count }}</strong>
            </div>
          </template>
        </div>
      </article>
      <article class="summary-card">
        <span class="eyebrow">Inactive</span>
        <strong>{{ summary.inactiveVehicles }}</strong>
        <p>Retired units kept for analytics</p>
      </article>
    </div>

    <div class="filters-panel">
      <label>
        City
        <select v-model="filters.city" @change="loadFleet">
          <option value="All">All cities</option>
          <option v-for="city in cityOptions" :key="city" :value="city">{{ city }}</option>
        </select>
      </label>
      <label>
        Zone or Station
        <select v-model="filters.zone" @change="loadFleet">
          <option value="All">All locations</option>
          <option v-for="zone in filterZoneOptions" :key="zone" :value="zone">{{ zone }}</option>
        </select>
      </label>
      <label v-if="!preferredType">
        Type
        <select v-model="filters.type" @change="loadFleet">
          <option value="All">All types</option>
          <option value="SCOOTER">Scooter</option>
          <option value="CAR">Car</option>
        </select>
      </label>
      <label>
        Status
        <select v-model="filters.status" @change="loadFleet">
          <option value="All">All statuses</option>
          <option value="AVAILABLE">Available</option>
          <option value="RESERVED">Reserved</option>
          <option value="RENTED">Rented</option>
          <option value="MAINTENANCE">Maintenance</option>
          <option value="INACTIVE">Inactive</option>
        </select>
      </label>
      <label class="checkbox">
        <input v-model="filters.includeHidden" type="checkbox" @change="loadFleet" />
        Include hidden vehicles
      </label>
    </div>

    <div v-if="showForm" class="modal-backdrop" @click.self="closeForm">
      <div class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="vehicle-modal-title">
        <div class="form-shell">
          <div class="form-header">
            <div>
              <p class="modal-eyebrow">Mobility provider</p>
              <h3 id="vehicle-modal-title">{{ editingVehicleId ? 'Update Vehicle' : 'Add Vehicle' }}</h3>
            </div>
            <button class="modal-close" type="button" aria-label="Close form" @click="closeForm">x</button>
          </div>
          <form class="fleet-form" @submit.prevent="submitForm">
            <label v-if="!preferredType">
              Vehicle type
              <select v-model="form.type" :disabled="!!editingVehicleId">
                <option value="SCOOTER">Scooter</option>
                <option value="CAR">Car</option>
              </select>
            </label>
            <label v-if="editingVehicleId">
              Vehicle code
              <input v-model="form.vehicleCode" disabled class="disabled-input" />
            </label>
            <label v-if="form.type === 'CAR'">
              Make & Model
              <select v-model="form.model" required>
                <option v-for="mod in modelOptions" :key="mod" :value="mod">{{ mod }}</option>
              </select>
            </label>
            <label>
              City
              <select v-model="form.locationCity" required>
                <option v-for="city in serviceCities" :key="city" :value="city">{{ city }}</option>
              </select>
            </label>
            <label>
              Zone or station
              <select v-model="form.locationZone" required>
                <option v-for="zone in zoneOptions" :key="zone" :value="zone">{{ zone }}</option>
              </select>
            </label>
            <label>
              Status
              <select v-model="form.status">
                <option value="AVAILABLE">Available</option>
                <option value="RESERVED">Reserved</option>
                <option value="RENTED">Rented</option>
                <option value="MAINTENANCE">Maintenance</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
            <label>
              Pricing category
              <select v-model="form.pricingCategory" required>
                <option v-for="cat in pricingOptions" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </label>
            <label>
              Base price
              <input v-model.number="form.basePrice" type="number" min="0" step="0.01" required />
            </label>
            <label>
              Price per minute
              <input v-model.number="form.pricePerMinute" type="number" min="0" step="0.01" required />
            </label>
            <label>
              Maintenance state
              <input v-model="form.maintenanceState" required placeholder="READY" />
            </label>
            <label v-if="form.type !== 'CAR'">
              Battery level
              <input v-model.number="form.batteryLevel" type="number" min="0" max="100" required />
            </label>
            <label v-else>
              Fuel level
              <input v-model.number="form.fuelLevel" type="number" min="0" max="100" required />
            </label>
            <label v-if="form.type === 'CAR'">
              License plate
              <input v-model="form.licensePlate" placeholder="ABC 123" />
            </label>
            <label class="checkbox">
              <input v-model="form.visibleInSearch" type="checkbox" />
              Visible in customer search
            </label>
            <div class="form-actions">
              <button class="action-btn secondary" type="button" @click="closeForm">Cancel</button>
              <button class="action-btn" :disabled="vehicleStore.fleetSaving" type="submit">
                {{ editingVehicleId ? 'Save Changes' : 'Create Vehicle' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div v-if="vehicleStore.fleetError" class="state-msg error">{{ vehicleStore.fleetError }}</div>
    <div v-else-if="vehicleStore.fleetLoading" class="state-msg">Loading fleet data...</div>
    <div v-else-if="vehicles.length === 0" class="state-msg">No vehicles match the current fleet filters.</div>

    <div v-else class="fleet-locations">
      <div v-for="(locVehicles, location) in vehiclesByLocation" :key="location" class="location-group">
        <h3 class="location-heading">{{ location }}</h3>
        <div class="fleet-grid">
          <article v-for="vehicle in locVehicles" :key="vehicle.id" class="vehicle-card">
        <div class="card-header">
          <div>
            <span class="card-type">{{ vehicleHeading(vehicle) }}</span>
            <h3>{{ vehicle.vehicleCode }}</h3>
          </div>
          <span class="status-pill" :class="getStatusClass(vehicle.status)">{{ vehicle.status }}</span>
        </div>

        <dl class="card-details">
          <div>
            <dt>Location</dt>
            <dd>{{ vehicle.locationCity }} / {{ vehicle.locationZone }}</dd>
          </div>
          <div>
            <dt>Energy</dt>
            <dd>{{ energyLabel(vehicle) }}</dd>
          </div>
          <div>
            <dt>Pricing</dt>
            <dd>{{ vehicle.pricingCategory }} / ${{ vehicle.pricePerMinute.toFixed(2) }}/min</dd>
          </div>
          <div>
            <dt>Maintenance</dt>
            <dd>{{ vehicle.maintenanceState }}</dd>
          </div>
          <div>
            <dt>Search visibility</dt>
            <dd>{{ vehicle.visibleInSearch ? 'Visible' : 'Hidden' }}</dd>
          </div>
          <div v-if="vehicle.licensePlate">
            <dt>License</dt>
            <dd>{{ vehicle.licensePlate }}</dd>
          </div>
        </dl>

        <div class="card-actions">
          <button class="text-btn action-edit" @click="openEditForm(vehicle)">Edit</button>
          <button class="text-btn action-relocate" @click="relocateVehicle(vehicle)">Relocate</button>
          <button
            v-if="vehicle.status !== 'MAINTENANCE' && vehicle.status !== 'INACTIVE'"
            class="text-btn warning action-maintenance"
            @click="markMaintenance(vehicle)"
          >
            Send to Maintenance
          </button>
          <button
            v-else-if="vehicle.status === 'MAINTENANCE'"
            class="text-btn success action-restore"
            @click="restoreVehicle(vehicle)"
          >
            Restore
          </button>
          <button
            v-if="vehicle.status !== 'INACTIVE'"
            class="text-btn danger action-retire"
            @click="deactivateVehicle(vehicle)"
          >
            Retire
          </button>
        </div>
      </article>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.fleet-manager {
  display: grid;
  gap: 1.1rem;
}

.fleet-topbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  background: linear-gradient(140deg, #eff6ff, #f8fafc);
  border: 1px solid #dbeafe;
  border-radius: 16px;
  padding: 1rem 1.15rem;
}

.fleet-topbar h2 {
  margin: 0 0 0.35rem;
  color: #16324f;
}

.fleet-topbar p {
  margin: 0;
  color: #5f6c7b;
}

.topbar-actions {
  display: flex;
  gap: 0.75rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.85rem;
}

.summary-card {
  background: linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  border: 1px solid #d5e4f3;
  border-radius: 14px;
  padding: 1.1rem;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.summary-card strong {
  display: block;
  font-size: 2rem;
  margin: 0.3rem 0;
  color: #16324f;
}

.summary-card p,
.eyebrow {
  margin: 0;
  color: #506274;
}

.breakdown-card {
  padding: 0.9rem 1.1rem;
}

.breakdown-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-top: 0.5rem;
}

.bd-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: #16324f;
  padding-bottom: 0.2rem;
  border-bottom: 1px dashed #d5e4f3;
}

.bd-row:last-child {
  border-bottom: none;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  background: rgba(0,0,0,0.02);
}

.sub-row {
  padding-left: 1rem;
  font-size: 0.85rem;
  color: #506274;
}

.location-group {
  margin-bottom: 2rem;
}

.location-heading {
  margin-bottom: 1rem;
  font-size: 1.15rem;
  color: #16324f;
  padding-bottom: 0.4rem;
  border-bottom: 2px solid #e2e8f0;
}

.eyebrow {
  text-transform: uppercase;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  font-weight: 700;
}

.filters-panel,
.fleet-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.filters-panel,
.vehicle-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1rem;
}

.filters-panel {
  position: sticky;
  top: 0.75rem;
  z-index: 5;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 1.5rem;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(4px);
}

.modal-dialog {
  width: min(900px, 100%);
  max-height: calc(100vh - 3rem);
  overflow: auto;
}

label {
  display: grid;
  gap: 0.45rem;
  color: #334155;
  font-size: 0.92rem;
  font-weight: 600;
}

input,
select {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.8rem 0.9rem;
  font-size: 0.95rem;
  background: #f8fafc;
}

input[disabled], select[disabled], .disabled-input {
  background: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.checkbox input {
  width: auto;
}

.form-header,
.card-header,
.form-actions,
.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.form-header h3,
.card-header h3 {
  margin: 0;
  color: #16324f;
}

.form-shell {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
  background: #fff;
  border: 1px solid #dbe7f3;
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.22);
}

.modal-eyebrow {
  margin: 0 0 0.25rem;
  color: #64748b;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 700;
}

.modal-close {
  width: 2.25rem;
  height: 2.25rem;
  border: none;
  border-radius: 999px;
  background: #e2e8f0;
  color: #1e293b;
  font-size: 1.35rem;
  line-height: 1;
  cursor: pointer;
}

.modal-close:hover {
  background: #cbd5e1;
}

.form-actions {
  grid-column: 1 / -1;
  justify-content: flex-end;
}

.action-btn,
.text-btn {
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 700;
  transition: 0.2s ease;
}

.action-btn {
  padding: 0.85rem 1.2rem;
  background: #1e88e5;
  color: #fff;
}

.action-btn.secondary {
  background: #e2e8f0;
  color: #1f2937;
}

.text-btn {
  padding: 0.6rem 0.95rem;
  border: 1px solid transparent;
  background: #f8fafc;
  color: #1e4f7a;
  line-height: 1;
}

.text-btn.warning {
  color: #b45309;
}

.text-btn.success {
  color: #047857;
}

.text-btn.danger {
  color: #b91c1c;
}

.text-btn.action-edit {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.text-btn.action-relocate {
  background: #f5f3ff;
  border-color: #ddd6fe;
  color: #6d28d9;
}

.text-btn.action-maintenance {
  background: #fff7ed;
  border-color: #fdba74;
}

.text-btn.action-restore {
  background: #ecfdf5;
  border-color: #86efac;
}

.text-btn.action-retire {
  background: #fef2f2;
  border-color: #fca5a5;
}

.text-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
}

.text-btn:focus-visible {
  outline: 3px solid rgba(59, 130, 246, 0.22);
  outline-offset: 2px;
}

.state-msg {
  padding: 1.25rem;
  text-align: center;
  border-radius: 12px;
  background: #fff;
  border: 1px dashed #cbd5e1;
  color: #475569;
}

.state-msg.error {
  border-style: solid;
  border-color: #fecaca;
  background: #fff5f5;
  color: #b91c1c;
}

.fleet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
}

.vehicle-card {
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.vehicle-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.1);
}

.card-type {
  display: inline-block;
  margin-bottom: 0.3rem;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
  font-weight: 700;
}

.status-pill {
  padding: 0.4rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.status-pill.available {
  background: #dcfce7;
  color: #166534;
}

.status-pill.reserved {
  background: #fef3c7;
  color: #92400e;
}

.status-pill.rented {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-pill.maintenance {
  background: #fee2e2;
  color: #b91c1c;
}

.status-pill.inactive {
  background: #e5e7eb;
  color: #374151;
}

.card-details {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
  margin: 1rem 0;
}

.card-details dt {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 0.2rem;
}

.card-details dd {
  margin: 0;
  color: #0f172a;
  font-weight: 600;
}

.card-actions {
  flex-wrap: wrap;
  justify-content: flex-start;
  border-top: 1px solid #e2e8f0;
  padding-top: 0.9rem;
  gap: 0.65rem;
}

@media (max-width: 700px) {
  .fleet-topbar,
  .form-header,
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .topbar-actions {
    width: 100%;
  }

  .topbar-actions .action-btn {
    flex: 1;
  }

  .card-details {
    grid-template-columns: 1fr;
  }

  .modal-backdrop {
    padding: 0.75rem;
  }

  .modal-dialog {
    max-height: calc(100vh - 1.5rem);
  }

  .form-shell {
    padding: 1rem;
    border-radius: 16px;
  }
}
</style>
