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
  const isAdmin = computed(() =>
    user.value?.role === 'CITY_ADMIN' || user.value?.role === 'SYSTEM_ADMIN')
  const isCitizen = computed(() => user.value?.role === 'CITIZEN')
  const isSysAdmin = computed(() => user.value?.role === 'SYSTEM_ADMIN')

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
    isLoggedIn, isProvider, isAdmin, isCitizen, isSysAdmin,
    register, login, logout, updateProfile, fetchUserProfile,
  }
})
