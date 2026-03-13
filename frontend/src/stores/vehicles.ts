import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api'

export interface Vehicle {
  id: string
  providerId: string
  type: 'BIKE' | 'SCOOTER' | 'CAR'
  status: 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE'
  locationCity: string
  batteryLevel: number
  basePrice: number
  pricePerMinute: number
  licensePlate?: string // Only CARs have a license plate
}

export const useVehicleStore = defineStore('vehicles', () => {
  const loading = ref(false)
  const availableVehicles = ref<Vehicle[]>([])
  const error = ref<string | null>(null)

  // Story: Vehicle Search
  async function searchVehicles(city?: string, type?: string) {
    loading.value = true
    error.value = null
    try {
      const params = new URLSearchParams()
      if (city && city !== 'All') params.append('city', city)
      if (type && type !== 'All') params.append('type', type)
      
      const res = await api.get(`/vehicles/search?${params.toString()}`)
      availableVehicles.value = res.data as Vehicle[]
    } catch (e: any) {
      error.value = 'Failed to load vehicles from server.'
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  return { availableVehicles, loading, error, searchVehicles }
})
