<script setup lang="ts">
/**
 * Dashboard Settings Card.
 * Quick access to profile summarize and account management.
 */
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="card settings-card fade-in">
    <div class="card-header">
       <div class="icon-box">⚙️</div>
       <div class="header-text">
          <h3>Account Settings</h3>
          <p>Control your account settings and payment methods.</p>
       </div>
    </div>

    <div class="settings-body">
       <div class="setting-summary">
          <div class="summary-item">
             <span class="label">Primary Mode</span>
             <span class="val">{{ auth.user?.preferredMobilityType || 'Not Set' }}</span>
          </div>
          <div class="summary-item">
             <span class="label">Saved Cards</span>
             <span class="val">{{ auth.user?.paymentMethods?.length || 0 }}</span>
          </div>
       </div>

       <div class="profile-preview">
          <div class="p-row">
             <span class="p-icon">📧</span>
             <span>{{ auth.user?.email }}</span>
          </div>
          <div v-if="auth.user?.phone" class="p-row">
             <span class="p-icon">📱</span>
             <span>{{ auth.user?.phone }}</span>
          </div>
       </div>

       <RouterLink to="/settings" class="action-btn settings-link">
          Manage Profile & Payments
       </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.settings-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding: 1.5rem;
  border: 1px solid #f1f5f9;
}

.card-header {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1.5rem;
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
}

.header-text h3 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: #1e293b;
}

.header-text p {
  margin: 0;
  font-size: 0.8rem;
  color: #94a3b8;
  font-weight: 600;
}

.settings-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.setting-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.summary-item {
  background: #f8fafc;
  padding: 0.85rem;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}

.summary-item .label {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  color: #94a3b8;
  text-transform: uppercase;
  margin-bottom: 0.25rem;
}

.summary-item .val {
  font-size: 0.95rem;
  font-weight: 750;
  color: #334155;
}

.profile-preview {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-bottom: 0.5rem;
}

.p-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.9rem;
  color: #64748b;
  font-weight: 500;
}

.p-icon {
  font-size: 1.1rem;
}

.settings-link {
  background: #0f172a;
  color: white;
  margin-top: 0.5rem;
  padding: 0.75rem;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
  text-align: center;
  transition: 0.2s;
}

.settings-link:hover {
  background: #1e293b;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.15);
}

.fade-in {
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
