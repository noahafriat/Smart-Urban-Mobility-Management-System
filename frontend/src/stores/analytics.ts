import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api'

// ── Types ──────────────────────────────────────────────────────────────────
export interface TransitAnalytics {
  activeRentals: number
  todayTrips: number
  revenueByCity: Record<string, number>
  totalTrips: number
  totalRevenue: number
  avgTripDurationMinutes: number
  tripsByType: Record<string, number>
  tripsByCity: Record<string, number>
  fleetAvailable: number
  fleetInUse: number
  fleetMaintenance: number
  totalUsers: number
  recentEvents: string[]
}

export interface GatewayData {
  provider: string
  endpoint: string
  status: 'UP' | 'DEGRADED' | 'DOWN'
  responseMs: number
  totalStations?: number
  activeStations?: number
  bikesAvailable?: number
  syncedAt?: string
  error?: string
}

export interface RentalAnalytics {
  scope: string
  totalRentals: number
  activeRentals: number
  completedRentals: number
  paidRentals: number
  fleetSize: number
  totalRevenue: number
  revenueByType: Record<string, number>
  rentalsByType: Record<string, number>
  rentalsByCity: Record<string, number>
  topVehicles: Record<string, number>
}

export interface ParkingAnalytics {
  // Rental zone info
  totalVehicles: number
  totalAvailableInZones: number
  overallUtilizationRate: string
  parkedPerZone: Record<string, number>
  totalPerZone: Record<string, number>
  occupancyRate: Record<string, string>
  maintenancePerCity: Record<string, number>

  // Parking garage info
  totalGarages: number
  totalGarageSpaces: number
  totalAvailableGarageSpaces: number
  garageUtilizationRate: string
  garageDetails: Array<{ id: string, name: string, totalSpaces: number, availableSpaces: number }>
}

// ── Store ──────────────────────────────────────────────────────────────────
export const useAnalyticsStore = defineStore('analytics', () => {
  let parkingFetchSeq = 0

  const transitData = ref<TransitAnalytics | null>(null)
  const rentalData = ref<RentalAnalytics | null>(null)
  const parkingData = ref<ParkingAnalytics | null>(null)
  const gatewayData = ref<GatewayData | null>(null)
  const gatewayLoading = ref(false)

  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchTransit() {
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/analytics/transit')
      transitData.value = res.data as TransitAnalytics
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to load transit analytics'
    } finally {
      loading.value = false
    }
  }

  async function fetchRentals(providerId?: string, userId?: string) {
    loading.value = true
    error.value = null
    try {
      const params: Record<string, string> = {}
      if (providerId) params.providerId = providerId
      if (userId) params.userId = userId
      const res = await api.get('/analytics/rentals', { params })
      rentalData.value = res.data as RentalAnalytics
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to load rental analytics'
    } finally {
      loading.value = false
    }
  }

  async function fetchParking(providerId?: string, garageProviderId?: string, requesterId?: string) {
    const seq = ++parkingFetchSeq
    loading.value = true
    error.value = null
    try {
      const params: Record<string, string> = {}
      if (providerId) params.providerId = providerId
      if (garageProviderId) params.garageProviderId = garageProviderId
      if (requesterId) params.requesterId = requesterId
      const res = await api.get('/analytics/parking', { params })
      if (seq !== parkingFetchSeq) return
      const raw = res.data as ParkingAnalytics
      parkingData.value = {
        ...raw,
        garageDetails: Array.isArray(raw.garageDetails) ? [...raw.garageDetails] : [],
      }
    } catch (e: any) {
      if (seq !== parkingFetchSeq) return
      error.value = e.response?.data?.error || 'Failed to load parking analytics'
    } finally {
      if (seq === parkingFetchSeq) loading.value = false
    }
  }

  async function fetchGateway() {
    gatewayLoading.value = true
    try {
      const res = await api.get('/analytics/gateway')
      gatewayData.value = res.data as GatewayData
    } catch (e: any) {
      gatewayData.value = { provider: 'BIXI Montréal', endpoint: '', status: 'DOWN', responseMs: 0, error: 'Request failed' }
    } finally {
      gatewayLoading.value = false
    }
  }

  return { transitData, rentalData, parkingData, gatewayData, loading, gatewayLoading, error, fetchTransit, fetchRentals, fetchParking, fetchGateway }
})
