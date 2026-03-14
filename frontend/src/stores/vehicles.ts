import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api'

export type VehicleType = 'BIKE' | 'SCOOTER' | 'CAR'
export type VehicleStatus = 'AVAILABLE' | 'RESERVED' | 'RENTED' | 'MAINTENANCE' | 'INACTIVE'

export interface Vehicle {
  id: string
  providerId: string
  vehicleCode: string
  type: VehicleType
  status: VehicleStatus
  locationCity: string
  locationZone: string
  batteryLevel: number
  fuelLevel: number
  basePrice: number
  pricePerMinute: number
  pricingCategory: string
  maintenanceState: string
  visibleInSearch: boolean
  retired: boolean
  licensePlate?: string
}

export interface FleetSummary {
  totalVehicles: number
  visibleVehicles: number
  inactiveVehicles: number
  availableVehicles: number
  rentedVehicles: number
  maintenanceVehicles: number
  lowEnergyVehicles: number
  issueFlaggedVehicles: number
  byStatus: Record<string, number>
  byType: Record<string, number>
  byCity: Record<string, number>
}

export interface ProviderFleetFilters {
  city?: string
  type?: string
  status?: string
  includeHidden?: boolean
}

export interface VehiclePayload {
  type: VehicleType
  vehicleCode: string
  locationCity: string
  locationZone: string
  batteryLevel: number
  fuelLevel: number
  basePrice: number
  pricePerMinute: number
  pricingCategory: string
  maintenanceState: string
  status: VehicleStatus
  visibleInSearch: boolean
  licensePlate?: string
}

const defaultSummary = (): FleetSummary => ({
  totalVehicles: 0,
  visibleVehicles: 0,
  inactiveVehicles: 0,
  availableVehicles: 0,
  rentedVehicles: 0,
  maintenanceVehicles: 0,
  lowEnergyVehicles: 0,
  issueFlaggedVehicles: 0,
  byStatus: {},
  byType: {},
  byCity: {},
})

export const useVehicleStore = defineStore('vehicles', () => {
  const loading = ref(false)
  const availableVehicles = ref<Vehicle[]>([])
  const error = ref<string | null>(null)

  const fleetLoading = ref(false)
  const fleetSaving = ref(false)
  const providerFleet = ref<Vehicle[]>([])
  const providerSummary = ref<FleetSummary>(defaultSummary())
  const fleetError = ref<string | null>(null)

  async function searchVehicles(city?: string, type?: string) {
    loading.value = true
    error.value = null
    try {
      const params = new URLSearchParams()
      if (city && city !== 'All') params.append('city', city)
      if (type && type !== 'All') params.append('type', type)

      const query = params.toString()
      const res = await api.get(`/vehicles/search${query ? `?${query}` : ''}`)
      availableVehicles.value = res.data as Vehicle[]
    } catch (e: any) {
      error.value = 'Failed to load vehicles from server.'
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  async function fetchProviderFleet(providerId: string, filters: ProviderFleetFilters = {}) {
    fleetLoading.value = true
    fleetError.value = null
    try {
      const params = new URLSearchParams()
      if (filters.city && filters.city !== 'All') params.append('city', filters.city)
      if (filters.type && filters.type !== 'All') params.append('type', filters.type)
      if (filters.status && filters.status !== 'All') params.append('status', filters.status)
      params.append('includeHidden', String(filters.includeHidden ?? true))

      const res = await api.get(`/vehicles/provider/${providerId}?${params.toString()}`)
      providerFleet.value = res.data as Vehicle[]
    } catch (e: any) {
      fleetError.value = e.response?.data?.error ?? 'Failed to load provider fleet.'
      throw new Error(fleetError.value)
    } finally {
      fleetLoading.value = false
    }
  }

  async function fetchProviderSummary(providerId: string) {
    try {
      const res = await api.get(`/vehicles/provider/${providerId}/summary`)
      providerSummary.value = res.data as FleetSummary
    } catch (e: any) {
      fleetError.value = e.response?.data?.error ?? 'Failed to load fleet summary.'
      throw new Error(fleetError.value)
    }
  }

  async function refreshProviderFleetDashboard(providerId: string, filters: ProviderFleetFilters = {}) {
    await Promise.all([
      fetchProviderFleet(providerId, filters),
      fetchProviderSummary(providerId),
    ])
  }

  async function createVehicle(providerId: string, payload: VehiclePayload, filters: ProviderFleetFilters = {}) {
    await runProviderMutation(async () => {
      await api.post(`/vehicles/provider/${providerId}`, payload)
      await refreshProviderFleetDashboard(providerId, filters)
    })
  }

  async function updateVehicle(
    providerId: string,
    vehicleId: string,
    payload: Partial<VehiclePayload>,
    filters: ProviderFleetFilters = {},
  ) {
    await runProviderMutation(async () => {
      await api.put(`/vehicles/provider/${providerId}/${vehicleId}`, payload)
      await refreshProviderFleetDashboard(providerId, filters)
    })
  }

  async function sendToMaintenance(
    providerId: string,
    vehicleId: string,
    maintenanceState: string,
    filters: ProviderFleetFilters = {},
  ) {
    await runProviderMutation(async () => {
      await api.post(`/vehicles/provider/${providerId}/${vehicleId}/maintenance`, { maintenanceState })
      await refreshProviderFleetDashboard(providerId, filters)
    })
  }

  async function restoreVehicle(providerId: string, vehicleId: string, filters: ProviderFleetFilters = {}) {
    await runProviderMutation(async () => {
      await api.post(`/vehicles/provider/${providerId}/${vehicleId}/restore`)
      await refreshProviderFleetDashboard(providerId, filters)
    })
  }

  async function relocateVehicle(
    providerId: string,
    vehicleId: string,
    locationCity: string,
    locationZone: string,
    filters: ProviderFleetFilters = {},
  ) {
    await runProviderMutation(async () => {
      await api.post(`/vehicles/provider/${providerId}/${vehicleId}/relocate`, { locationCity, locationZone })
      await refreshProviderFleetDashboard(providerId, filters)
    })
  }

  async function deactivateVehicle(providerId: string, vehicleId: string, filters: ProviderFleetFilters = {}) {
    await runProviderMutation(async () => {
      await api.post(`/vehicles/provider/${providerId}/${vehicleId}/deactivate`)
      await refreshProviderFleetDashboard(providerId, filters)
    })
  }

  async function runProviderMutation(action: () => Promise<void>) {
    fleetSaving.value = true
    fleetError.value = null
    try {
      await action()
    } catch (e: any) {
      fleetError.value = e.response?.data?.error ?? e.message ?? 'Fleet update failed.'
      throw new Error(fleetError.value)
    } finally {
      fleetSaving.value = false
    }
  }

  return {
    availableVehicles,
    loading,
    error,
    providerFleet,
    providerSummary,
    fleetLoading,
    fleetSaving,
    fleetError,
    searchVehicles,
    fetchProviderFleet,
    fetchProviderSummary,
    refreshProviderFleetDashboard,
    createVehicle,
    updateVehicle,
    sendToMaintenance,
    restoreVehicle,
    relocateVehicle,
    deactivateVehicle,
  }
})
