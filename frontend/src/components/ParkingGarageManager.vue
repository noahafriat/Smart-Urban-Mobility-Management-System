<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'

export interface ParkingGarageRow {
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

const props = defineProps<{
  providerId: string
}>()

const loading = ref(false)
const errorMsg = ref('')
const garages = ref<ParkingGarageRow[]>([])
const summary = reactive({
  garageCount: 0,
  totalSpaces: 0,
  availableSpaces: 0,
  occupiedSpaces: 0,
})

const showForm = ref(false)
const editingId = ref<string | null>(null)

const form = reactive({
  name: '',
  address: '',
  latitude: 45.5019,
  longitude: -73.5674,
  totalSpaces: 50,
  flatRate: 0,
})

async function loadAll() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [listRes, sumRes] = await Promise.all([
      api.get<ParkingGarageRow[]>(`/parking-garages/provider/${props.providerId}`),
      api.get<typeof summary>(`/parking-garages/provider/${props.providerId}/summary`),
    ])
    garages.value = listRes.data
    Object.assign(summary, sumRes.data)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.error ?? e.message ?? 'Failed to load garages'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadAll()
})

function openCreate() {
  editingId.value = null
  form.name = ''
  form.address = ''
  form.latitude = 45.5019
  form.longitude = -73.5674
  form.totalSpaces = 50
  form.flatRate = 0
  showForm.value = true
}

function openEdit(g: ParkingGarageRow) {
  editingId.value = g.id
  form.name = g.name
  form.address = g.address ?? ''
  form.latitude = g.latitude
  form.longitude = g.longitude
  form.totalSpaces = g.totalSpaces
  form.flatRate = typeof g.flatRate === 'number' ? g.flatRate : 0
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingId.value = null
}

async function submitForm() {
  const payload = {
    name: form.name.trim(),
    address: form.address.trim(),
    latitude: form.latitude,
    longitude: form.longitude,
    totalSpaces: form.totalSpaces,
    flatRate: Math.max(0, Number(form.flatRate) || 0),
  }
  try {
    if (editingId.value) {
      await api.put(`/parking-garages/provider/${props.providerId}/${editingId.value}`, payload)
    } else {
      await api.post(`/parking-garages/provider/${props.providerId}`, payload)
    }
    closeForm()
    await loadAll()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Save failed')
  }
}

async function removeGarage(g: ParkingGarageRow) {
  if (!window.confirm(`Remove "${g.name}" from the platform?`)) return
  try {
    await api.delete(`/parking-garages/provider/${props.providerId}/${g.id}`)
    await loadAll()
  } catch (e: any) {
    window.alert(e.response?.data?.error ?? e.message ?? 'Delete failed')
  }
}

function occupied(g: ParkingGarageRow) {
  return Math.max(0, g.totalSpaces - g.availableSpaces)
}
</script>

<template>
  <section class="garage-manager">
    <div class="actions-summary">
      <button class="action-btn secondary" type="button" :disabled="loading" @click="loadAll">
        Refresh
      </button>
      <button class="action-btn solid" type="button" @click="openCreate">Add garage</button>
    </div>

    <p v-if="errorMsg" class="err">{{ errorMsg }}</p>

    <div class="summary-grid">
      <article class="summary-card">
        <span class="eyebrow">Garages</span>
        <strong>{{ summary.garageCount }}</strong>
        <p>Locations you operate</p>
      </article>
      <article class="summary-card">
        <span class="eyebrow">Total capacity</span>
        <strong>{{ summary.totalSpaces }}</strong>
        <p>Spaces across all sites</p>
      </article>
      <article class="summary-card">
        <span class="eyebrow">Available now</span>
        <strong>{{ summary.availableSpaces }}</strong>
        <p>Open for reservation</p>
      </article>
      <article class="summary-card">
        <span class="eyebrow">Occupied / held</span>
        <strong>{{ summary.occupiedSpaces }}</strong>
        <p>Reserved or in use</p>
      </article>
    </div>

    <div class="garage-list">
      <p v-if="!loading && garages.length === 0" class="empty">No garages yet — add your first location.</p>
      <article v-for="g in garages" :key="g.id" class="garage-card">
        <header>
          <h3>{{ g.name }}</h3>
          <span class="pill">{{ g.availableSpaces }} / {{ g.totalSpaces }} free</span>
        </header>
        <p class="addr">📍 {{ g.address }}</p>
        <p class="rate-line">
          Flat rate: <strong>${{ (typeof g.flatRate === 'number' ? g.flatRate : 0).toFixed(2) }}</strong>
        </p>
        <p class="coords">{{ g.latitude.toFixed(4) }}, {{ g.longitude.toFixed(4) }} · {{ occupied(g) }} occupied</p>
        <div class="card-actions">
          <RouterLink
            class="text-btn"
            :to="`/mobility-map?lat=${g.latitude}&lng=${g.longitude}`"
          >Map</RouterLink>
          <button type="button" class="text-btn action-edit" @click="openEdit(g)">Edit</button>
          <button type="button" class="text-btn danger" @click="removeGarage(g)">Delete</button>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="modal-backdrop" @click.self="closeForm">
        <div class="modal-dialog" role="dialog" aria-modal="true">
          <div class="form-shell">
            <div class="form-header">
              <h3>{{ editingId ? 'Edit garage' : 'New garage' }}</h3>
              <button type="button" class="modal-close" aria-label="Close" @click="closeForm">×</button>
            </div>
            <form class="fleet-form" @submit.prevent="submitForm">
              <label>
                Name
                <input v-model="form.name" required />
              </label>
              <label class="wide">
                Address
                <input v-model="form.address" required placeholder="Street address for display" />
              </label>
              <label>
                Latitude
                <input v-model.number="form.latitude" type="number" step="any" required />
              </label>
              <label>
                Longitude
                <input v-model.number="form.longitude" type="number" step="any" required />
              </label>
              <label>
                Flat rate ($)
                <input v-model.number="form.flatRate" type="number" min="0" step="0.01" required />
              </label>
              <label>
                Total spaces
                <input v-model.number="form.totalSpaces" type="number" min="1" required />
              </label>
              <div class="form-actions wide">
                <button type="button" class="action-btn secondary" @click="closeForm">Cancel</button>
                <button type="submit" class="action-btn solid">Save</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.garage-manager {
  display: grid;
  gap: 1.1rem;
}

.err {
  color: #b91c1c;
  margin: 0;
}

.actions-summary {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-btn {
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
  font-weight: 600;
  cursor: pointer;
}

.action-btn.secondary:hover {
  background: #f1f5f9;
}

.action-btn.solid {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
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
}

.summary-card strong {
  display: block;
  font-size: 2rem;
  margin: 0.3rem 0;
  color: #16324f;
}

.eyebrow {
  text-transform: uppercase;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  font-weight: 700;
  color: #506274;
}

.summary-card p {
  margin: 0;
  color: #506274;
  font-size: 0.9rem;
}

.garage-list {
  display: grid;
  gap: 1rem;
}

.empty {
  padding: 2rem;
  text-align: center;
  color: #64748b;
  background: #f8fafc;
  border-radius: 12px;
}

.garage-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 1.15rem;
}

.garage-card header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.garage-card h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #0f172a;
}

.pill {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: #dbeafe;
  color: #1e40af;
  white-space: nowrap;
}

.addr {
  margin: 0.5rem 0 0.15rem;
  color: #475569;
  font-size: 0.9rem;
}

.rate-line {
  margin: 0.25rem 0 0.15rem;
  font-size: 0.9rem;
  color: #475569;
}

.rate-line strong {
  color: #0f172a;
}

.coords {
  margin: 0 0 0.75rem;
  font-size: 0.8rem;
  color: #94a3b8;
}

.card-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
}

.text-btn {
  background: none;
  border: none;
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  font-size: 0.9rem;
}

.text-btn.danger {
  color: #dc2626;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
}

.modal-dialog {
  width: min(520px, 100%);
  max-height: 90vh;
  overflow: auto;
}

.form-shell {
  background: #fff;
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.form-header h3 {
  margin: 0;
}

.modal-close {
  border: none;
  background: #f1f5f9;
  width: 2rem;
  height: 2rem;
  border-radius: 8px;
  font-size: 1.25rem;
  cursor: pointer;
}

.fleet-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.fleet-form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #334155;
}

.fleet-form input {
  padding: 0.55rem 0.65rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

.wide {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}
</style>
