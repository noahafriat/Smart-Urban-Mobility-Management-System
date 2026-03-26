import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api'
import type { Vehicle } from './vehicles'

export interface Rental {
  id: string
  userId: string
  vehicle: Vehicle
  status: 'ACTIVE' | 'COMPLETED' | 'PAID'
  startTime: string
  endTime: string | null
  totalCost: number
  reservationPaymentMethod?: string
  reservationPaymentStatus?: string
  reservationPaymentProcessedAt?: string
  reservationPaymentAmount?: number
  finalPaymentMethod?: string
}

export const useRentalStore = defineStore('rentals', () => {
  const loading = ref(false)
  const rentals = ref<Rental[]>([])
  const error = ref<string | null>(null)

  async function fetchUserRentals(userId: string) {
    loading.value = true
    error.value = null
    try {
      const res = await api.get(`/rentals/user/${userId}`)
      rentals.value = res.data as Rental[]
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to fetch rentals'
    } finally {
      loading.value = false
    }
  }

  // Epic 2.2: Reservation
  async function reserve(userId: string, vehicleId: string, options?: {
    paymentInfo?: string
    savePaymentMethod?: boolean
  }) {
    loading.value = true
    error.value = null
    try {
      await api.post('/rentals/reserve', {
        userId,
        vehicleId,
        paymentInfo: options?.paymentInfo,
        savePaymentMethod: String(options?.savePaymentMethod ?? false),
      })
      // Refresh the feed
      await fetchUserRentals(userId)
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to reserve vehicle'
      throw new Error(error.value || 'Failed to reserve vehicle')
    } finally {
      loading.value = false
    }
  }

  // Epic 2.4: Return
  async function returnVehicle(rentalId: string, userId: string) {
    loading.value = true
    error.value = null
    try {
      await api.post(`/rentals/${rentalId}/return`)
      await fetchUserRentals(userId)
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Failed to return vehicle'
    } finally {
      loading.value = false
    }
  }

  // Epic 2.3: Payment
  async function pay(rentalId: string, userId: string) {
    loading.value = true
    error.value = null
    try {
      await api.post(`/rentals/${rentalId}/pay`)
      await fetchUserRentals(userId)
    } catch (e: any) {
      error.value = e.response?.data?.error || 'Payment failed'
    } finally {
      loading.value = false
    }
  }

  return { loading, rentals, error, fetchUserRentals, reserve, returnVehicle, pay }
})
