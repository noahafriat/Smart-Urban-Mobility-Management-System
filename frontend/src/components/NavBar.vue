<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function doLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <nav class="navbar">
    <div class="nav-content">
      <div class="brand">
        <RouterLink to="/" class="logo">SUMMS</RouterLink>
      </div>

      <div class="nav-links" v-if="auth.isLoggedIn">
        <RouterLink to="/dashboard">Dashboard</RouterLink>
        <RouterLink to="/mobility-map">Map</RouterLink>
        <RouterLink v-if="auth.isCitizen" to="/public-transport">Public Transit</RouterLink>
        <RouterLink v-if="auth.isCitizen" to="/vehicles">Search Vehicles</RouterLink>
        <RouterLink v-if="auth.isCitizen" to="/bixi">BIXI Bikes</RouterLink>
        <RouterLink v-if="auth.isCitizen" to="/parking-spaces">Parking Garages</RouterLink>
        <RouterLink v-if="auth.isCitizen" to="/rentals">My Rentals</RouterLink>
        <RouterLink v-if="auth.isCitizen" to="/settings">Settings</RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/analytics/transit">Transit</RouterLink>
        <RouterLink v-if="auth.isAdmin || auth.isProvider" to="/analytics/rentals">Rentals</RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/analytics/parking">Parking</RouterLink>
        <RouterLink v-if="auth.isSysAdmin" to="/admin/users" class="admin-link">Users</RouterLink>
      </div>

      <div class="nav-actions">
        <template v-if="auth.isLoggedIn">
          <span class="user-greeting">Hi, {{ auth.user?.name?.split(' ')[0] }}</span>
          <button @click="doLogout" class="logout-btn">Log Out</button>
        </template>
        <template v-else>
          <RouterLink to="/login" class="nav-link">Sign In</RouterLink>
          <RouterLink to="/register" class="register-btn">Sign Up</RouterLink>
        </template>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 1.5rem;
  font-weight: 800;
  color: #2b6cb0;
  text-decoration: none;
  letter-spacing: -0.5px;
}

.nav-links {
  display: flex;
  gap: 1.5rem;
}

.nav-links a {
  text-decoration: none;
  color: #4a5568;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-links a:hover, .nav-links a.router-link-active {
  color: #2b6cb0;
}

.nav-links a.admin-link {
  color: #c53030;
  font-weight: 700;
}

.nav-links a.admin-link:hover {
  color: #9b2c2c;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.user-greeting {
  color: #718096;
  font-weight: 500;
  font-size: 0.95rem;
}

.nav-link {
  text-decoration: none;
  color: #4a5568;
  font-weight: 600;
}

.nav-link:hover {
  color: #2b6cb0;
}

.logout-btn {
  background: transparent;
  border: 1px solid #e2e8f0;
  padding: 0.4rem 1rem;
  border-radius: 6px;
  color: #4a5568;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #edf2f7;
  color: #1a202c;
}

.register-btn {
  background: #3182ce;
  color: white;
  text-decoration: none;
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  font-weight: 600;
  transition: background 0.2s;
}

.register-btn:hover {
  background: #2b6cb0;
}
</style>
