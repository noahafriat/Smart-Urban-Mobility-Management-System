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

export interface ParkingAreaBreakdownRow {
  garageId: string
  areaName: string
  addressSnippet?: string
  spotsTaken: number
  spotsTotal: number
  activeReservationSpots: number
  /** Payments recorded on reservations (non-cancelled). */
  revenue: number
  /** Occupied spots × flat rate — aligns utilization with “should have earned” for sensor-style occupancy. */
  occupancyImpliedRevenue: number
  /** Non-cancelled reservations in scope (lifetime count per garage). */
  allTimeReservations: number
  flatRate?: number
  capacityUtilizationPercent: number
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
  garageDetails: Array<{ id: string, name: string, address?: string, totalSpaces: number, availableSpaces: number, flatRate?: number }>
  totalParkingRevenue?: number
  /** Sum of occupancyImpliedRevenue across garages in scope. */
  totalOccupancyImpliedRevenue?: number
  paidParkingSessions?: number
  averageParkingPayment?: number
  parkingRevenueByGarageId?: Record<string, number>
  /** Session counts per garage (non-cancelled, same scope as analytics). */
  parkingSessionsByGarageId?: Record<string, number>
  topParkingGaragesByRevenue?: Array<{ garageId: string, garageName: string, revenue: number, paidSessions: number }>
  bestParkingGarage?: Record<string, unknown>
  totalParkingSpotsTaken?: number
  activeParkingSpotsInScope?: number
  parkingCapacityUtilizationPercent?: number
  parkingAreaBreakdown?: ParkingAreaBreakdownRow[]
}

function coerceNum(v: unknown): number {
  if (v == null || v === '') return 0
  if (typeof v === 'number') return Number.isFinite(v) ? v : 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function coerceInt(v: unknown): number {
  return Math.round(coerceNum(v))
}

function normalizeParkingRevenueByGarageId(m: unknown): Record<string, number> {
  if (m == null || typeof m !== 'object' || Array.isArray(m)) return {}
  const out: Record<string, number> = {}
  for (const [k, val] of Object.entries(m as Record<string, unknown>)) {
    out[String(k)] = coerceNum(val)
  }
  return out
}

function normalizeGarageDetails(arr: unknown): ParkingAnalytics['garageDetails'] {
  if (!Array.isArray(arr)) return []
  return arr.map((g: Record<string, unknown>) => ({
    id: String(g.id ?? g.garageId ?? ''),
    name: String(g.name ?? ''),
    address: g.address != null ? String(g.address) : undefined,
    totalSpaces: coerceInt(g.totalSpaces),
    availableSpaces: coerceInt(g.availableSpaces),
    flatRate: g.flatRate != null ? coerceNum(g.flatRate) : undefined,
  }))
}

function normalizeParkingAreaBreakdown(arr: unknown): ParkingAreaBreakdownRow[] {
  if (!Array.isArray(arr)) return []
  return arr.map((row: Record<string, unknown>) => {
    const spotsTaken = coerceInt(row.spotsTaken)
    const flat = coerceNum(row.flatRate)
    const hasImpliedKey = row.occupancyImpliedRevenue != null && String(row.occupancyImpliedRevenue) !== ''
    const implied = hasImpliedKey
      ? coerceNum(row.occupancyImpliedRevenue)
      : Math.round(spotsTaken * flat * 100) / 100
    return {
      garageId: String(row.garageId ?? ''),
      areaName: String(row.areaName ?? ''),
      addressSnippet: row.addressSnippet != null ? String(row.addressSnippet) : '',
      spotsTaken,
      spotsTotal: coerceInt(row.spotsTotal),
      activeReservationSpots: coerceInt(row.activeReservationSpots),
      revenue: coerceNum(row.revenue),
      occupancyImpliedRevenue: implied,
      flatRate: flat,
      capacityUtilizationPercent: coerceNum(row.capacityUtilizationPercent),
      allTimeReservations: coerceInt(row.allTimeReservations),
    }
  })
}

function normalizeParkingPayload(raw: Record<string, unknown>): ParkingAnalytics {
  const base = raw as unknown as ParkingAnalytics
  const revByGarage = normalizeParkingRevenueByGarageId(raw.parkingRevenueByGarageId)
  const sessionsByGarage = normalizeParkingRevenueByGarageId(raw.parkingSessionsByGarageId)
  const breakdown = normalizeParkingAreaBreakdown(raw.parkingAreaBreakdown)
  const garageDetails = normalizeGarageDetails(raw.garageDetails)

  return {
    ...base,
    totalVehicles: coerceInt(raw.totalVehicles),
    totalAvailableInZones: coerceInt(raw.totalAvailableInZones),
    totalGarages: coerceInt(raw.totalGarages),
    totalGarageSpaces: coerceInt(raw.totalGarageSpaces),
    totalAvailableGarageSpaces: coerceInt(raw.totalAvailableGarageSpaces),
    totalParkingRevenue: coerceNum(raw.totalParkingRevenue),
    totalOccupancyImpliedRevenue: coerceNum(raw.totalOccupancyImpliedRevenue),
    paidParkingSessions: coerceInt(raw.paidParkingSessions),
    averageParkingPayment: coerceNum(raw.averageParkingPayment),
    totalParkingSpotsTaken: coerceInt(raw.totalParkingSpotsTaken),
    activeParkingSpotsInScope: coerceInt(raw.activeParkingSpotsInScope),
    parkingCapacityUtilizationPercent: coerceNum(raw.parkingCapacityUtilizationPercent),
    parkingRevenueByGarageId: revByGarage,
    parkingSessionsByGarageId: sessionsByGarage,
    parkingAreaBreakdown: breakdown,
    garageDetails,
  }
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
      const raw = res.data as Record<string, unknown>
      parkingData.value = normalizeParkingPayload(raw)
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
