<script setup lang="ts">
import ProviderFleetManager from '../components/ProviderFleetManager.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="dashboard">
    <header class="dash-header">
      <div class="welcome-box">
        <h1>Hello, {{ auth.user?.name }}</h1>
        <p class="role-badge" :class="auth.user?.role?.toLowerCase()">
          {{ auth.user?.role?.replace('_', ' ') }}
        </p>
      </div>
    </header>

    <main class="dash-content" :class="{ 'provider-layout': auth.isProvider }">
      <div class="card" v-if="auth.isCitizen">
        <h2>Your Commute</h2>
        <p>You can search for vehicles, view transit, and reserve parking.</p>
        <!-- Epic 2 Rental Components -->
        <RouterLink to="/vehicles" class="action-btn mb">Search Vehicles</RouterLink>
        <RouterLink to="/rentals" class="action-btn secondary">My Rentals</RouterLink>
      </div>

      <div class="card provider-main-card" v-if="auth.isProvider">
        <h2>Fleet Overview</h2>
        <p>Manage your fleet of bikes, scooters, and cars.</p>
        <ProviderFleetManager v-if="auth.user" :provider-id="auth.user.id" />
      </div>

      <div class="card" v-if="auth.isAdmin">
        <h2>City Analytics</h2>
        <p>Monitor mobility trends and parking utilization.</p>
        <!-- Placeholders for Epic 4 components -->
        <div class="placeholder">Analytics Dashboard (Coming Soon)</div>
      </div>
      
      <div class="card profile-card">
        <h2>Profile Details</h2>
        <div class="profile-list">
          <div class="profile-row">
            <span class="profile-label">Email</span>
            <span class="profile-value">{{ auth.user?.email }}</span>
          </div>
          <div v-if="auth.user?.phone" class="profile-row">
            <span class="profile-label">Phone</span>
            <span class="profile-value">{{ auth.user?.phone }}</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

.dash-header {
  margin-bottom: 2rem;
}

.welcome-box {
  background: #fff;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h1 {
  margin: 0;
  font-size: 2rem;
  color: #1a202c;
  font-weight: 700;
}

.role-badge {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0;
}

.role-badge.citizen { background: #fed7d7; color: #9b2c2c; }
.role-badge.mobility_provider { background: #c6f6d5; color: #276749; }
.role-badge.city_admin { background: #bee3f8; color: #2c5282; }
.role-badge.system_admin { background: #e9d8fd; color: #553c9a; }

.dash-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  align-items: start;
}

.dash-content.provider-layout {
  grid-template-columns: minmax(0, 1fr) minmax(240px, 300px);
}

.card {
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.provider-main-card {
  min-width: 0;
}

.profile-card {
  padding: 1rem 1.1rem;
  align-self: start;
}

h2 {
  font-size: 1.25rem;
  margin-top: 0;
  color: #2d3748;
}

p {
  color: #718096;
  font-size: 0.95rem;
  line-height: 1.5;
}

.placeholder {
  margin-top: 1rem;
  background: #edf2f7;
  border: 1px dashed #cbd5e0;
  color: #a0aec0;
  padding: 1.5rem;
  border-radius: 8px;
  text-align: center;
  font-size: 0.9rem;
  font-weight: 600;
}

.action-btn {
  display: block;
  margin-top: 1.5rem;
  padding: 0.85rem;
  background: #3182ce;
  color: white;
  text-align: center;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  transition: background 0.2s;
}

.action-btn:hover {
  background: #2b6cb0;
}

.action-btn.mb {
  margin-bottom: 0.75rem;
}

.action-btn.secondary {
  background: #edf2f7;
  color: #2d3748;
  margin-top: 0;
  border: 1px solid #e2e8f0;
}

.action-btn.secondary:hover {
  background: #e2e8f0;
}

.profile-list {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
}

.profile-row {
  display: grid;
  gap: 0.2rem;
  padding: 0.7rem 0.8rem;
  border: 1px solid #edf2f7;
  border-radius: 10px;
  background: #f8fafc;
}

.profile-label {
  color: #718096;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 700;
}

.profile-value {
  color: #2d3748;
  font-size: 0.95rem;
  word-break: break-word;
}

@media (max-width: 900px) {
  .dash-content.provider-layout {
    grid-template-columns: 1fr;
  }
}
</style>
