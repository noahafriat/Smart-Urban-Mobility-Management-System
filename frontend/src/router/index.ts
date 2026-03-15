import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingView,
    },
    {
      // User registration
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
    },
    {
      // User authentication
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      // Epic 2: Vehicle Search
      path: '/vehicles',
      name: 'vehicles',
      component: () => import('../views/VehicleSearchView.vue'),
      meta: { requiresAuth: true },
    },
    {
      // Epic 2: Rental Lifecycle
      path: '/rentals',
      name: 'rentals',
      component: () => import('../views/MyRentalsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true },
    },
    {
      // Transit Analytics (Admin)
      path: '/analytics/transit',
      name: 'analytics-transit',
      component: () => import('../views/TransitAnalyticsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      // Rental Analytics (Admin + Provider)
      path: '/analytics/rentals',
      name: 'analytics-rentals',
      component: () => import('../views/RentalAnalyticsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      // Parking Analytics (Admin)
      path: '/analytics/parking',
      name: 'analytics-parking',
      component: () => import('../views/ParkingAnalyticsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      // User Role Management (System Admin only)
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/UserManagementView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// Navigation guard to protect routes that require authentication
router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && auth.isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
