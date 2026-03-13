<script setup lang="ts">
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

    <main class="dash-content">
      <div class="card" v-if="auth.isCitizen">
        <h2>Your Commute</h2>
        <p>You can search for vehicles, view transit, and reserve parking.</p>
        <!-- Placeholders for Epic 2 & Epic 4 components -->
        <RouterLink to="/vehicles" class="action-btn">Search Vehicles</RouterLink>
        <div class="placeholder">My Rentals (Coming Soon)</div>
      </div>

      <div class="card" v-if="auth.isProvider">
        <h2>Fleet Overview</h2>
        <p>Manage your fleet of bikes, scooters, and cars.</p>
        <!-- Placeholders for Epic 3 components -->
        <div class="placeholder">Vehicle Management (Coming Soon)</div>
      </div>

      <div class="card" v-if="auth.isAdmin">
        <h2>City Analytics</h2>
        <p>Monitor mobility trends and parking utilization.</p>
        <!-- Placeholders for Epic 4 components -->
        <div class="placeholder">Analytics Dashboard (Coming Soon)</div>
      </div>
      
      <div class="card">
        <h2>Profile Details</h2>
        <ul class="profile-list">
          <li><strong>Email:</strong> {{ auth.user?.email }}</li>
          <li v-if="auth.user?.phone"><strong>Phone:</strong> {{ auth.user?.phone }}</li>
        </ul>
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
}

.card {
  background: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
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

.profile-list {
  list-style: none;
  padding: 0;
  margin: 1rem 0 0;
}

.profile-list li {
  padding: 0.75rem 0;
  border-bottom: 1px solid #edf2f7;
  color: #4a5568;
  font-size: 0.95rem;
}

.profile-list li:last-child {
  border-bottom: none;
}
</style>
