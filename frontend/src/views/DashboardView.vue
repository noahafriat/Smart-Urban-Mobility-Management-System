<script setup lang="ts">

import ProviderFleetManager from '../components/ProviderFleetManager.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="dashboard fade-in">
    <header class="dash-header">
      <div class="welcome-box">
        <h1>Hello, {{ auth.user?.name }}</h1>
        <p class="role-badge" :class="auth.user?.role?.toLowerCase()">
          {{ auth.user?.role?.replace('_', ' ') }}
        </p>
      </div>
    </header>

    <main class="dash-content" :class="{ 'provider-layout': auth.isProvider }">
      <!-- ── Citizen Interaction Hub ── -->
      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">🏠</div>
          <div class="header-text">
            <h2>Your Commute</h2>
            <p>Search for vehicles, view transit, and reserve parking.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/vehicles" class="action-btn mb">Search Vehicles</RouterLink>
          <RouterLink to="/rentals" class="action-btn secondary">My Rentals</RouterLink>
        </div>
      </div>

      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">🚲</div>
          <div class="header-text">
            <h2>BIXI Availability</h2>
            <p>See real-time bike counts at every active station across Montréal.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/bixi" class="action-btn" style="background: #16a34a;">View BIXI Stations</RouterLink>
        </div>
      </div>

      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">🗺️</div>
          <div class="header-text">
            <h2>Mobility Map</h2>
            <p>View car rentals, scooter docks, BIXI, and garages on one live map.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/mobility-map" class="action-btn" style="background: #0f766e;">Open Mobility Map</RouterLink>
        </div>
      </div>

      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">🅿️</div>
          <div class="header-text">
            <h2>Parking Spaces</h2>
            <p>Find available parking spaces across the city and view live capacity.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/parking-spaces" class="action-btn" style="background: #2b6cb0;">View Parking Spaces</RouterLink>
        </div>
      </div>

      <!-- ── Provider Command Center ── -->
      <div class="card provider-main-card" v-if="auth.isProvider">
        <h2>Fleet Overview</h2>
        <p>Manage your fleet of scooters and cars.</p>
        <ProviderFleetManager v-if="auth.user" :provider-id="auth.user.id" />
      </div>

      <!-- Provider Fleet Analytics card -->
      <div class="card provider-side-card" v-if="auth.isProvider">
        <h2>Fleet Analytics</h2>
        <p>View rental trends and activity for your fleet.</p>
        <RouterLink to="/analytics/rentals" class="action-btn">Rental Analytics</RouterLink>
      </div>
      
      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">🚍</div>
          <div class="header-text">
            <h2>Public Transit</h2>
            <p>Real-time bus tracking and metro status across the city network.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/public-transport" class="action-btn" style="background: #3b82f6;">View Transit Schedules</RouterLink>
        </div>
      </div>

      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">⚙️</div>
          <div class="header-text">
            <h2>Account Settings</h2>
            <p>Control your personal info, and payment methods.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/settings" class="action-btn">Manage Account</RouterLink>
        </div>
      </div>
    </main>

    <!-- ── Administrative Console ── -->
    <div v-if="auth.isAdmin || auth.isSysAdmin" class="admin-container">
      <header class="hub-header">
        <h2>Admin Console</h2>
        <p>Manage user roles and platform analytics.</p>
      </header>

      <div class="admin-grid">
        <!-- City Analytics -->
        <div v-if="auth.isAdmin" class="card admin-feature">
          <div class="af-icon analytics">📊</div>
          <h3>City Analytics</h3>
          <p>Track transit usage, rental data, and parking stats.</p>
          <div class="af-actions">
            <RouterLink to="/analytics/transit" class="btn outline">Transit Data</RouterLink>
            <RouterLink to="/analytics/rentals" class="btn outline">Rental Data</RouterLink>
            <RouterLink to="/analytics/parking" class="btn outline">Parking Data</RouterLink>
          </div>
        </div>

        <div v-if="auth.isSysAdmin" class="card admin-feature dangerous">
          <div class="af-icon system">⚙️</div>
          <h3>User Control</h3>
          <p>Manage user accounts and set permissions.</p>
          <div class="af-actions">
            <RouterLink to="/admin/users" class="btn solid danger">User Management</RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 3rem clamp(1rem, 5vw, 4rem);
  max-width: 1400px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
  color: #0f172a;
}

.dash-header { margin-bottom: 3.5rem; }
.welcome-box { border-radius: 28px; background: white; padding: 2.5rem; border: 1px solid #f1f5f9; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.04); display: flex; justify-content: space-between; align-items: center; }

h1 { margin: 0; font-size: 2.5rem; font-weight: 900; letter-spacing: -0.02em; }
.role-badge { padding: 0.6rem 1.25rem; border-radius: 100px; font-weight: 850; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; }
.role-badge.citizen { background: #eff6ff; color: #3b82f6; }
.role-badge.mobility_provider { background: #f0fdf4; color: #10b981; }
.role-badge.city_admin { background: #fef2f2; color: #ef4444; }
.role-badge.system_admin { background: #fdf4ff; color: #a855f7; }

.dash-content { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 2rem; margin-bottom: 5rem; align-items: start; }
.dash-content.provider-layout { grid-template-columns: 1fr 380px; }

.card { 
  background: white; 
  border: 1px solid #f1f5f9; 
  border-radius: 20px; 
  padding: 1.5rem; 
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.04); 
  transition: 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
  height: 100%;
}
.card:hover { transform: translateY(-4px); box-shadow: 0 20px 50px -15px rgba(0,0,0,0.08); }

.card-header {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1.25rem;
}

.icon-box {
  font-size: 1.5rem;
  background: #f8fafc;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.header-text h2 { 
  font-size: 1.35rem; 
  font-weight: 850; 
  margin: 0 0 0.5rem 0; 
  color: #1e293b; 
}
.header-text p { 
  color: #64748b; 
  font-size: 0.95rem; 
  line-height: 1.5; 
  font-weight: 500;
  margin: 0;
}

.card-footer {
  margin-top: auto;
  padding-top: 1rem;
}

.action-btn { 
  display: block; 
  width: 100%; 
  border: none; 
  padding: 1rem; 
  border-radius: 16px; 
  font-weight: 800; 
  text-decoration: none; 
  text-align: center; 
  font-size: 1rem; 
  cursor: pointer; 
  transition: 0.2s; 
  background: #0f172a; 
  color: white; 
}
.action-btn:hover { background: #1e293b; transform: translateY(-2px); }
.action-btn.secondary { background: #f1f5f9; color: #475569; margin-top: 0.75rem; }
.action-btn.mb { margin-bottom: 0.75rem; }

/* ── Admin Console Styles ── */
.admin-container { margin-top: 2rem; padding-top: 4rem; border-top: 2px solid #f1f5f9; }
.hub-header { margin-bottom: 3rem; }
.hub-header h2 { font-size: 2rem; font-weight: 900; color: #0f172a; }
.hub-header p { font-size: 1.15rem; }

.admin-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 2rem; }
.admin-feature { border: 2px solid #f1f5f9; }
.af-icon { font-size: 2.5rem; margin-bottom: 1.5rem; }
.af-actions { display: flex; flex-wrap: wrap; gap: 0.75rem; margin-top: 2rem; }

.btn { padding: 0.75rem 1.5rem; border-radius: 12px; font-weight: 800; text-decoration: none; font-size: 0.9rem; transition: 0.2s; }
.btn.outline { border: 2px solid #e2e8f0; color: #475569; }
.btn.outline:hover { background: #f8fafc; border-color: #cbd5e1; }
.btn.solid.danger { background: #ef4444; color: white; }
.btn.solid.danger:hover { background: #dc2626; box-shadow: 0 10px 20px -5px rgba(239, 68, 68, 0.4); }

.fade-in { animation: fadeIn 0.8s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 900px) {
  .dash-content.provider-layout { grid-template-columns: 1fr; }
  .admin-grid { grid-template-columns: 1fr; }
}
</style>
