import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api'

// ── Types ──────────────────────────────────────────────────────────────────
export interface TransitAnalytics {
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

export interface RentalAnalytics {
  scope: string
  totalRentals: number
  activeRentals: number
  completedRentals: number
  paidRentals: number
  totalRevenue: number
  revenueByType: Record<string, number>
  rentalsByType: Record<string, number>
  rentalsByCity: Record<string, number>
  topVehicles: Record<string, number>
}

export interface ParkingAnalytics {
  totalVehicles: number
  totalAvailableInZones: number
  overallUtilizationRate: string
  parkedPerZone: Record<string, number>
  occupancyRate: Record<string, string>
  maintenancePerCity: Record<string, number>
}

// ── Store ──────────────────────────────────────────────────────────────────
export const useAnalyticsStore = defineStore('analytics', () => {
  const transitData = ref<TransitAnalytics | null>(null)
  const rentalData = ref<RentalAnalytics | null>(null)
  const parkingData = ref<ParkingAnalytics | null>(null)

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

  async function fetchRentals(providerId?: string) {
    loading.value = true
    error.value = null
    try {
      const params = providerId ? { providerId } : {}
      const res = await api.get('/analytics/rentals', { params })
      rentalData.value = res.data as RentalAnalytics
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to load rental analytics'
    } finally {
      loading.value = false
    }
  }

  async function fetchParking() {
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/analytics/parking')
      parkingData.value = res.data as ParkingAnalytics
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to load parking analytics'
    } finally {
      loading.value = false
    }
  }

  return { transitData, rentalData, parkingData, loading, error, fetchTransit, fetchRentals, fetchParking }
})
