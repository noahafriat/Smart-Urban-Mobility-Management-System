<script setup lang="ts">
import ProviderFleetManager from '../components/ProviderFleetManager.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="dashboard-shell">
    
    <!-- ── Welcome Banner ── -->
    <section class="welcome-banner" :class="auth.user?.role?.toLowerCase().replace('_', '-')">
      <div class="banner-content">
        <div class="banner-text">
          <p class="banner-eyebrow">Smart Urban Mobility Management System</p>
          <h1>Hello, {{ auth.user?.name }}</h1>
          <p class="banner-subtext">Welcome to your central hub for exploring and managing Montréal mobility.</p>
        </div>
        <div class="banner-badge">
          {{ auth.user?.role?.replace('_', ' ') }}
        </div>
      </div>
    </section>

    <!-- ── Citizen Hub ── -->
    <div v-if="auth.isCitizen" class="hub-container">
      <header class="hub-header">
        <h2>Your Mobility Services</h2>
        <p>Explore tools for your daily commute and city travels.</p>
      </header>

      <div class="services-grid">
        <!-- Mobility Map Card -->
        <RouterLink to="/mobility-map" class="feature-card map-card">
          <div class="fc-icon">🗺️</div>
          <div class="fc-body">
            <h3>Unified Mobility Map</h3>
            <p>View car rentals, scooter docks, BIXI bikes, and parking garages on one live map.</p>
            <div class="fc-action">Open Map →</div>
          </div>
        </RouterLink>

        <!-- Transit Hub Card -->
        <RouterLink to="/public-transport" class="feature-card transit-card">
          <div class="fc-icon">🚇</div>
          <div class="fc-body">
            <h3>Public Transit Hub</h3>
            <p>Plan trips with live schedules and step-by-step directions across Montréal.</p>
            <div class="fc-action">Start Planning →</div>
          </div>
        </RouterLink>

        <!-- Vehicles Card -->
        <RouterLink to="/vehicles" class="feature-card vehicle-card">
          <div class="fc-icon">🚗</div>
          <div class="fc-body">
            <h3>Vehicle Search</h3>
            <p>Find and reserve electric cars or scooters in your immediate area.</p>
            <div class="fc-action">Find Vehicles →</div>
          </div>
        </RouterLink>

        <!-- BIXI Card -->
        <RouterLink to="/bixi" class="feature-card bixi-card">
          <div class="fc-icon">🚲</div>
          <div class="fc-body">
            <h3>BIXI Stations</h3>
            <p>Check real-time bike and dock availability at any station across the city.</p>
            <div class="fc-action">Check Availability →</div>
          </div>
        </RouterLink>

        <!-- Parking Card -->
        <RouterLink to="/parking-spaces" class="feature-card parking-card">
          <div class="fc-icon">🅿️</div>
          <div class="fc-body">
            <h3>Parking Capacity</h3>
            <p>Locate available parking garages and monitor live occupancy data.</p>
            <div class="fc-action">Find Parking →</div>
          </div>
        </RouterLink>

        <!-- My Rentals Card -->
        <RouterLink to="/rentals" class="feature-card rental-card">
          <div class="fc-icon">📋</div>
          <div class="fc-body">
            <h3>My Rentals</h3>
            <p>Manage your active reservations, view history, and handle vehicle returns.</p>
            <div class="fc-action">Manage Rentals →</div>
          </div>
        </RouterLink>
      </div>
    </div>

    <!-- ── Provider Command Center ── -->
    <div v-if="auth.isProvider" class="admin-container">
      <header class="hub-header">
        <h2>Fleet Command Center</h2>
        <p>Real-time oversight and maintenance of your mobility inventory.</p>
      </header>

      <div class="admin-layout">
        <section class="main-admin-panel">
          <div class="card manager-card">
            <div class="card-header">
              <h3>Inventory & Fleet Management</h3>
              <RouterLink to="/analytics/rentals" class="text-link">Full Analytics ↗</RouterLink>
            </div>
            <ProviderFleetManager v-if="auth.user" :provider-id="auth.user.id" />
          </div>
        </section>
        
        <aside class="side-admin-panel">
          <div class="card analytics-mini">
            <h3>Fleet Health</h3>
            <p>Overview of utilization and energy levels across your current fleet.</p>
            <RouterLink to="/analytics/rentals" class="btn">View Analytics Feed</RouterLink>
          </div>
        </aside>
      </div>
    </div>

    <!-- ── Admin Controls ── -->
    <div v-if="auth.isAdmin || auth.isSysAdmin" class="admin-container">
       <header class="hub-header">
        <h2>Administrative Console</h2>
        <p>Operational oversight for urban mobility and platform governance.</p>
      </header>

      <div class="admin-grid">
        <!-- City Analytics -->
        <div v-if="auth.isAdmin" class="card admin-feature">
          <div class="af-icon analytics">📊</div>
          <h3>Central Intelligence</h3>
          <p>Analyze mobility density, transit flow, and infrastructure stress across all sectors.</p>
          <div class="af-actions">
            <RouterLink to="/analytics/transit" class="btn outline">Transit Data</RouterLink>
            <RouterLink to="/analytics/rentals" class="btn outline">Rental Data</RouterLink>
            <RouterLink to="/analytics/parking" class="btn outline">Parking Data</RouterLink>
          </div>
        </div>

        <!-- System Settings -->
        <div v-if="auth.isSysAdmin" class="card admin-feature dangerous">
          <div class="af-icon system">⚙️</div>
          <h3>System Governance</h3>
          <p>Identity management, role audits, and enterprise-level platform configuration.</p>
          <div class="af-actions">
            <RouterLink to="/admin/users" class="btn solid danger">User Management</RouterLink>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* ── Variables & Setup ── */
.dashboard-shell {
  padding: 1.5rem clamp(1rem, 3vw, 3rem);
  max-width: 1440px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  color: #1e293b;
}

/* ── Banner ── */
.welcome-banner {
  border-radius: 24px;
  padding: 3rem;
  margin-bottom: 3rem;
  position: relative;
  overflow: hidden;
  background: white;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 25px rgba(0, 0, 0, 0.04);
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: 0; right: 0; bottom: 0; left: 0;
  opacity: 0.04;
  background-image: radial-gradient(#1e293b 1.5px, transparent 0);
  background-size: 24px 24px;
}

/* Role Specific Banner Colors */
.welcome-banner.citizen { border-left: 8px solid #3b82f6; }
.welcome-banner.mobility-provider { border-left: 8px solid #10b981; }
.welcome-banner.city-admin { border-left: 8px solid #8b5cf6; }
.welcome-banner.system-admin { border-left: 8px solid #ef4444; }

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.banner-eyebrow {
  font-size: 0.8rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #64748b;
  margin-bottom: 0.5rem;
}

.banner-text h1 {
  font-size: clamp(1.8rem, 4vw, 2.75rem);
  font-weight: 900;
  letter-spacing: -0.03em;
  margin: 0 0 0.5rem;
  color: #0f172a;
}

.banner-subtext {
  font-size: 1.1rem;
  color: #475569;
  margin: 0;
  max-width: 500px;
}

.banner-badge {
  padding: 0.5rem 1.25rem;
  background: #f1f5f9;
  color: #334155;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border: 1px solid #e2e8f0;
}

/* ── Container Headers ── */
.hub-header {
  margin-bottom: 2rem;
}

.hub-header h2 {
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0 0 0.4rem;
  color: #0f172a;
}

.hub-header p {
  color: #64748b;
  font-size: 1.05rem;
  margin: 0;
}

/* ── Services Grid ── */
.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 1.5rem;
  margin-bottom: 4rem;
}

.feature-card {
  display: flex;
  gap: 1.5rem;
  padding: 2rem;
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1);
  position: relative;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-8px);
  border-color: transparent;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

.fc-icon {
  font-size: 2.5rem;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border-radius: 16px;
  transition: transform 0.3s;
}

.feature-card:hover .fc-icon {
  transform: scale(1.1) rotate(-5deg);
}

.fc-body h3 {
  font-size: 1.35rem;
  font-weight: 800;
  margin: 0 0 0.6rem;
  color: #0f172a;
}

.fc-body p {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #64748b;
  margin: 0 0 1.25rem;
}

.fc-action {
  font-size: 0.9rem;
  font-weight: 700;
  color: #3b82f6;
  transition: padding-left 0.2s;
}

.feature-card:hover .fc-action {
  padding-left: 6px;
}

/* Card Themes */
.map-card:hover     { background: linear-gradient(135deg, #fff 60%, #f0fdfa 100%); }
.map-card:hover .fc-action { color: #0d9488; }

.transit-card:hover { background: linear-gradient(135deg, #fff 60%, #f5f3ff 100%); }
.transit-card:hover .fc-action { color: #7c3aed; }

.vehicle-card:hover { background: linear-gradient(135deg, #fff 60%, #eff6ff 100%); }
.vehicle-card:hover .fc-action { color: #3b82f6; }

.bixi-card:hover    { background: linear-gradient(135deg, #fff 60%, #f0fdf4 100%); }
.bixi-card:hover .fc-action { color: #16a34a; }

.parking-card:hover { background: linear-gradient(135deg, #fff 60%, #f8fafc 100%); }
.parking-card:hover .fc-action { color: #475569; }

.rental-card:hover  { background: linear-gradient(135deg, #fff 60%, #fffbeb 100%); }
.rental-card:hover .fc-action { color: #d97706; }

/* ── Provider/Admin Layouts ── */
.admin-container {
  margin-top: 2rem;
}

.admin-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
  align-items: start;
}

.card {
  background: white;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  padding: 1.75rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.02);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.card-header h3 { margin: 0; font-size: 1.3rem; font-weight: 800; }

.text-link {
  font-size: 0.85rem;
  font-weight: 700;
  color: #3b82f6;
  text-decoration: none;
}

.analytics-mini h3 { font-size: 1.2rem; font-weight: 800; margin-bottom: 0.75rem; }
.analytics-mini p { margin-bottom: 1.5rem; }

/* ── Admin Grid ── */
.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1.5rem;
}

.admin-feature {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}

.af-icon {
  font-size: 2rem;
  margin-bottom: 1.25rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 14px;
}

.admin-feature h3 { font-size: 1.4rem; font-weight: 800; margin: 0 0 0.75rem; }
.admin-feature p { margin-bottom: 1.5rem; max-width: 450px; }

.af-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: auto;
}

/* ── Shared Buttons ── */
.btn {
  padding: 0.7rem 1.25rem;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-block;
}

.btn.outline {
  border: 1.5px solid #e2e8f0;
  color: #475569;
}

.btn.outline:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.btn.solid {
  background: #3b82f6;
  color: white;
  border: none;
}

.btn.solid:hover { background: #2563eb; }

.btn.danger {
  background: #ef4444;
  color: white;
}
.btn.danger:hover { background: #dc2626; }

/* ── Responsive ── */
@media (max-width: 1024px) {
  .admin-layout { grid-template-columns: 1fr; }
  .services-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
}

@media (max-width: 640px) {
  .welcome-banner { padding: 2rem; }
  .banner-content { flex-direction: column; align-items: flex-start; gap: 1.5rem; }
  .services-grid { grid-template-columns: 1fr; }
  .feature-card { padding: 1.5rem; }
  .fc-icon { width: 48px; height: 48px; font-size: 1.75rem; }
}
</style>
