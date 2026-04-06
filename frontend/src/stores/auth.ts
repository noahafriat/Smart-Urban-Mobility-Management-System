import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '../api'

export interface User {
  id: string
  name: string
  email: string
  role: 'CITIZEN' | 'MOBILITY_PROVIDER' | 'CITY_ADMIN' | 'SYSTEM_ADMIN'
  phone?: string
  providerType?: string
  hasPaymentInfo?: boolean
  paymentInfo?: string
  paymentMethods?: string[]
}

// Pinia store for authenticated user state.
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const error = ref<string | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!user.value)
  const isProvider = computed(() => user.value?.role === 'MOBILITY_PROVIDER')
  const isCityAdmin = computed(() => user.value?.role === 'CITY_ADMIN')
  const isSysAdmin = computed(() => user.value?.role === 'SYSTEM_ADMIN')
  // isAdmin = any elevated admin role (used for shared admin UI sections)
  const isAdmin = computed(() => isCityAdmin.value || isSysAdmin.value)
  const isCitizen = computed(() => user.value?.role === 'CITIZEN')
  const isParkingProvider = computed(
    () => user.value?.role === 'MOBILITY_PROVIDER' && user.value?.providerType === 'PARKING',
  )
  // Rental analytics are for vehicle fleets; parking providers use garage tooling instead.
  const canViewRentalAnalytics = computed(
    () => isSysAdmin.value || (isProvider.value && !isParkingProvider.value),
  )
  // Parking analytics: system admin sees all scopes; city admin and parking operators use the same page scoped to their garages.
  const canViewParkingAnalytics = computed(() => isAdmin.value || isParkingProvider.value)
  /** Transit usage dashboard — system admin only (not city admin). */
  const canViewTransitAnalytics = computed(() => isSysAdmin.value)
  /** CRUD on parking garages (municipal inventory for city admin; commercial for parking providers). */
  const canManageParkingGarages = computed(() => isCityAdmin.value || isParkingProvider.value)

  // User registration
  async function register(payload: {
    name: string; email: string; password: string; role: string; phone?: string; providerType?: string
  }) {
    loading.value = true
    error.value = null
    try {
      await api.post('/users/register', payload)
    } catch (e: any) {
      error.value = e.response?.data?.error ?? 'Registration failed'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  // User authentication
  async function login(email: string, password: string) {
    loading.value = true
    error.value = null
    try {
      const res = await api.post('/users/login', { email, password })
      user.value = res.data as User
    } catch (e: any) {
      error.value = e.response?.data?.error ?? 'Login failed'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(updates: any) {
    if (!user.value) throw new Error('User not logged in')
    loading.value = true
    error.value = null
    try {
      const res = await api.put(`/users/${user.value.id}/profile`, updates)
      user.value = res.data as User
    } catch (e: any) {
      error.value = e.response?.data?.error ?? 'Failed to update profile'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  async function fetchUserProfile(userId: string) {
    try {
      const res = await api.get(`/users/${userId}/profile`)
      user.value = res.data as User
    } catch {
      // Sliently fail if profile fetch or user is invalid
    }
  }

  // Logout
  function logout() {
    user.value = null
    error.value = null
  }

  return {
    user, error, loading,
    isLoggedIn, isProvider, isAdmin, isCityAdmin, isCitizen, isSysAdmin, isParkingProvider, canViewRentalAnalytics, canViewParkingAnalytics, canViewTransitAnalytics, canManageParkingGarages,
    register, login, logout, updateProfile, fetchUserProfile,
  }
})