<script setup lang="ts">
import ParkingGarageManager from '../components/ParkingGarageManager.vue'
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

    <main class="dash-content" :class="{ 'provider-layout': auth.isProvider || auth.canManageParkingGarages }">
      <!-- ── Citizen Interaction Hub ── -->
      <!-- Commute / Search (Typically for Citizens/Renters) -->
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
            <p>View rentals, scooter docks, BIXI, and parking on one live map.</p>
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

      <!-- ── Vehicle provider fleet ── -->
      <div class="card provider-main-card" v-if="auth.isProvider && !auth.isParkingProvider">
        <header class="card-header">
           <div class="icon-box">{{ auth.user?.providerType === 'CAR' ? '🚗' : '🛴' }}</div>
           <div class="header-text">
              <h2>Fleet Operations</h2>
              <p>Manage your vehicle inventory and search visibility.</p>
           </div>
        </header>
        <ProviderFleetManager v-if="auth.user" :provider-id="auth.user.id" />
      </div>

      <!-- ── Municipal or commercial parking operator ── -->
      <div class="card provider-main-card" v-if="auth.canManageParkingGarages && auth.user">
        <header class="card-header">
          <div class="icon-box">🅿️</div>
          <div class="header-text">
            <h2>{{ auth.isCityAdmin ? 'Municipal parking facilities' : 'Garage operations' }}</h2>
            <p>
              {{
                auth.isCityAdmin
                  ? 'Add and edit city-owned garages, capacity, and live availability.'
                  : 'Add locations, set capacity, and monitor your garage availability.'
              }}
            </p>
          </div>
        </header>
        <ParkingGarageManager :provider-id="auth.user.id" />
      </div>
      
      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">🚍</div>
          <div class="header-text">
            <h2>STM &amp; transit</h2>
            <p>Trip planner, official STM metro and bus schedules, and service alerts.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/public-transport" class="action-btn" style="background: #3b82f6;">Open STM &amp; transit</RouterLink>
        </div>
      </div>

      <div class="card" v-if="auth.isCitizen">
        <header class="card-header">
          <div class="icon-box">⚙️</div>
          <div class="header-text">
            <h2>Account Settings</h2>
            <p>Control your personal info and payment methods.</p>
          </div>
        </header>
        <div class="card-footer">
          <RouterLink to="/settings" class="action-btn">Manage Account</RouterLink>
        </div>
      </div>
    </main>

    <!-- ── Administrative / operator hub ── -->
    <div v-if="auth.isAdmin || auth.isSysAdmin || auth.isParkingProvider" class="admin-container">
      <header class="hub-header">
        <div class="governance-badge">{{ auth.isParkingProvider && !auth.isAdmin ? 'Operator tools' : 'Admin Tools' }}</div>
        <h2>{{ auth.isParkingProvider && !auth.isAdmin ? 'Parking operator hub' : 'Admin Hub' }}</h2>
        <p>
          {{
            auth.isParkingProvider && !auth.isAdmin
              ? 'Capacity and occupancy for your garages — same analytics view as city oversight, limited to your account.'
              : auth.isCityAdmin && !auth.isSysAdmin
                ? 'Municipal parking tools and analytics for facilities you manage.'
                : 'Monitor transit, rentals, and manage user roles.'
          }}
        </p>
      </header>

      <div class="admin-grid">
        <!-- Transit Stats -->
        <div v-if="auth.canViewTransitAnalytics" class="card admin-feature">
          <header class="card-header">
            <div class="icon-box info">🚍</div>
            <div class="header-text">
              <h2>Transit Analytics</h2>
              <p>See how many people are using the system to get around.</p>
            </div>
          </header>
          <div class="card-footer">
            <RouterLink to="/analytics/transit" class="action-btn">View Transit Stats</RouterLink>
          </div>
        </div>

        <!-- Rental Stats -->
        <div v-if="auth.canViewRentalAnalytics" class="card admin-feature">
          <header class="card-header">
            <div class="icon-box success">💰</div>
            <div class="header-text">
              <h2>Rental Analytics</h2>
              <p>Track how much money is being made and provider status.</p>
            </div>
          </header>
          <div class="card-footer">
            <RouterLink to="/analytics/rentals" class="action-btn">View Rental Stats</RouterLink>
          </div>
        </div>

        <!-- Parking Stats -->
        <div v-if="auth.canViewParkingAnalytics" class="card admin-feature">
          <header class="card-header">
            <div class="icon-box warning">🅿️</div>
            <div class="header-text">
              <h2>Parking Analytics</h2>
              <p>
                {{
                  auth.canManageParkingGarages
                    ? auth.isCityAdmin
                      ? 'Live capacity and occupancy for municipal garages you manage.'
                      : 'Live capacity and occupancy for your garages only.'
                    : 'Check the live capacity and open spots in city garages.'
                }}
              </p>
            </div>
          </header>
          <div class="card-footer">
            <RouterLink to="/analytics/parking" class="action-btn">View Parking Stats</RouterLink>
          </div>
        </div>

        <!-- User Management (System Admin) -->
        <div v-if="auth.isSysAdmin" class="card admin-feature dangerous">
          <header class="card-header">
            <div class="icon-box danger">🛡️</div>
            <div class="header-text">
              <h2>User Management</h2>
              <p>Manage who can access the system and change their roles.</p>
            </div>
          </header>
          <div class="card-footer">
            <RouterLink to="/admin/users" class="action-btn danger">Manage Users</RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 2rem clamp(1rem, 5vw, 4rem);
  max-width: 1400px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
  color: #0f172a;
}

.dash-header { margin-bottom: 1.5rem; }
.welcome-box { border-radius: 28px; background: white; padding: 1.5rem 2.5rem; border: 1px solid #f1f5f9; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.04); display: flex; justify-content: space-between; align-items: center; }

h1 { margin: 0; font-size: 2.5rem; font-weight: 900; letter-spacing: -0.02em; }
.role-badge { padding: 0.6rem 1.25rem; border-radius: 100px; font-weight: 850; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; }
.role-badge.citizen { background: #eff6ff; color: #3b82f6; }
.role-badge.mobility_provider { background: #f0fdf4; color: #10b981; }
.role-badge.city_admin { background: #fef2f2; color: #ef4444; }
.role-badge.system_admin { background: #fdf4ff; color: #a855f7; }

.dash-content { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.5rem; align-items: start; }
.dash-content.provider-layout { grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); }
.provider-main-card { grid-column: 1 / -1; }

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
.admin-container { margin-top: 0; padding-top: 0; }
.hub-header { margin-bottom: 2.5rem; }
.hub-header .governance-badge { display: inline-block; padding: 0.35rem 0.75rem; background: #f1f5f9; border-radius: 6px; font-size: 0.7rem; font-weight: 800; color: #64748b; text-transform: uppercase; margin-bottom: 1rem; }
.hub-header h2 { font-size: 2rem; font-weight: 900; color: #0f172a; margin: 0 0 0.5rem; }
.hub-header p { font-size: 1.1rem; color: #64748b; margin: 0; }

.admin-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 1.5rem; }
.icon-box.info { background: #eff6ff; color: #3b82f6; }
.icon-box.success { background: #f0fdf4; color: #10b981; }
.icon-box.warning { background: #fffbeb; color: #f59e0b; }
.icon-box.danger { background: #fef2f2; color: #ef4444; }

.action-btn.danger { background: #ef4444; }
.action-btn.danger:hover { background: #dc2626; box-shadow: 0 10px 20px -5px rgba(239, 68, 68, 0.4); }

.fade-in { animation: fadeIn 0.8s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 900px) {
  .dash-content.provider-layout { grid-template-columns: 1fr; }
  .admin-grid { grid-template-columns: 1fr; }
}
</style>