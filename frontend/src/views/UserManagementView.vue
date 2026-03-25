<script setup lang="ts">
/**
 * Manage Account Roles
 * Exclusive to System Admin. Allows assigning or modifying roles for any user.
 */
import { onMounted, ref, reactive } from 'vue'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

interface UserRow {
  id: string
  name: string
  email: string
  role: string
  phone?: string
}

const users = ref<UserRow[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successId = ref<string | null>(null)

const ROLES = ['CITIZEN', 'MOBILITY_PROVIDER', 'CITY_ADMIN', 'SYSTEM_ADMIN']

const roleLabels: Record<string, string> = {
  CITIZEN: 'Citizen',
  MOBILITY_PROVIDER: 'Mobility Provider',
  CITY_ADMIN: 'City Admin',
  SYSTEM_ADMIN: 'System Admin',
}

const pendingRole = reactive<Record<string, string>>({})

onMounted(fetchUsers)

async function fetchUsers() {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/users')
    users.value = res.data as UserRow[]
    // Seed local dropdown state to current role
    users.value.forEach(u => { pendingRole[u.id] = u.role })
  } catch (e: any) {
    error.value = 'Failed to load users.'
  } finally {
    loading.value = false
  }
}

async function applyRole(user: UserRow) {
  const newRole = pendingRole[user.id]
  if (!newRole || newRole === user.role) return

  loading.value = true
  error.value = null
  successId.value = null
  try {
    const res = await api.put(`/users/${user.id}/role`, { role: newRole })
    const updated = res.data as UserRow
    // Update the local list entry
    const idx = users.value.findIndex(u => u.id === user.id)
    const row = users.value[idx]
    if (row) row.role = updated.role
    successId.value = user.id
    setTimeout(() => { successId.value = null }, 3000)
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Failed to update role.'
  } finally {
    loading.value = false
  }
}
async function deleteUser(user: UserRow) {
  const confirmed = window.confirm(
    `Are you sure you want to permanently delete "${user.name}" (${user.email})?\nThis cannot be undone.`
  )
  if (!confirmed) return

  loading.value = true
  error.value = null
  try {
    await api.delete(`/users/${user.id}`, { params: { requesterId: auth.user?.id } })
    users.value = users.value.filter(u => u.id !== user.id)
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Failed to delete user.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="analytics-shell">
    <header class="page-header">
      <div class="header-main">
        <span class="view-tag">System Governance</span>
        <h1>User Registry & Access Control</h1>
        <p>Manage authenticated identities and assign administrative privileges across the platform.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" :disabled="loading" @click="fetchUsers">
           ↻ Refresh Registry
        </button>
      </div>
    </header>

    <div v-if="error" class="state-msg error">{{ error }}</div>
    
    <div v-if="loading && users.length === 0" class="state-msg pulse">
       Synching user database...
    </div>

    <main v-else class="registry-container panel-card">
      <div class="registry-header">
        <div class="registry-stats">
          <span class="stat"><strong>{{ users.length }}</strong> Total Accounts</span>
          <span class="divider">|</span>
          <span class="stat"><strong>{{ users.filter(u => u.role === 'SYSTEM_ADMIN').length }}</strong> Administrators</span>
        </div>
        <div class="registry-search">
           <input type="text" placeholder="Search accounts by name or email..." class="search-input" disabled />
        </div>
      </div>

      <div class="table-wrapper">
        <table class="registry-table">
          <thead>
            <tr>
              <th>Account Holder</th>
              <th>Identity</th>
              <th>Current Access Level</th>
              <th>Action Panel</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id" :class="{ 'highlight-row': successId === user.id }">
              <td class="user-info-cell">
                <div class="user-avatar">{{ user.name.charAt(0) }}</div>
                <div class="user-details">
                  <span class="user-name">{{ user.name }}</span>
                  <span class="user-id">ID: {{ user.id.slice(0, 8) }}...</span>
                </div>
              </td>
              <td class="email-cell">{{ user.email }}</td>
              <td>
                <div class="role-management">
                   <span class="role-pill" :class="user.role.toLowerCase().replace('_', '-')">
                    {{ roleLabels[user.role] }}
                  </span>
                  <div class="inline-actions">
                    <select v-model="pendingRole[user.id]" class="registry-select">
                      <option v-for="role in ROLES" :key="role" :value="role">
                        {{ roleLabels[role] }}
                      </option>
                    </select>
                     <button
                        class="btn-save"
                        :disabled="loading || pendingRole[user.id] === user.role"
                        @click="applyRole(user)">
                        {{ successId === user.id ? 'Saved' : 'Apply' }}
                      </button>
                  </div>
                </div>
              </td>
              <td class="actions-cell">
                <button
                  class="btn-delete"
                  :disabled="loading || user.id === auth.user?.id"
                  @click="deleteUser(user)">
                  <span class="icon">🗑️</span>
                  Remove
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <section class="registry-footer">
       <div class="warning-panel">
         <strong>Security Note:</strong> Modified roles take effect upon the user's next session. Access revocation is immediate across all synchronized nodes.
       </div>
    </section>
  </div>
</template>

<style scoped>
.analytics-shell {
  padding: 1.5rem clamp(1rem, 3vw, 3rem);
  max-width: 1440px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
}

/* ── Header ── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2.5rem;
  gap: 2rem;
}

.view-tag {
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #ef4444;
  background: #fef2f2;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  margin-bottom: 0.75rem;
  display: inline-block;
}

.header-main h1 {
  font-size: 2.5rem;
  font-weight: 900;
  letter-spacing: -0.04em;
  margin: 0 0 0.5rem;
  color: #0f172a;
}

.header-main p { color: #64748b; font-size: 1.1rem; margin: 0; max-width: 800px; }

.btn-refresh {
  padding: 0.75rem 1.25rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh:hover { background: #f8fafc; border-color: #cbd5e1; }

/* ── Registry Container ── */
.panel-card {
  background: white;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0,0,0,0.02);
}

.registry-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
}

.registry-stats {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  font-size: 0.9rem;
  color: #64748b;
}

.registry-stats .divider { opacity: 0.3; }
.registry-stats strong { color: #0f172a; font-weight: 800; }

.search-input {
  padding: 0.6rem 1rem;
  width: 320px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  font-size: 0.9rem;
  background: white;
}

/* ── Table Styling ── */
.table-wrapper { overflow-x: auto; }
.registry-table { width: 100%; border-collapse: collapse; text-align: left; }

.registry-table th {
  padding: 1rem 2rem;
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  color: #94a3b8;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #f1f5f9;
}

.registry-table td {
  padding: 1.25rem 2rem;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.registry-table tr:last-child td { border-bottom: none; }
.registry-table tr.highlight-row td { background: #f0fdf4; transition: background 0.5s; }

/* ── Specific Cells ── */
.user-info-cell { display: flex; align-items: center; gap: 1.25rem; }
.user-avatar {
  width: 44px; height: 44px;
  background: #f1f5f9; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 900; color: #3b82f6; font-size: 1.2rem;
}

.user-details { display: flex; flex-direction: column; }
.user-name { font-weight: 800; color: #1e293b; font-size: 1.1rem; line-height: 1.2; }
.user-id { font-size: 0.75rem; color: #94a3b8; font-weight: 600; font-family: monospace; }

.role-management { display: flex; flex-direction: column; gap: 0.75rem; }

.inline-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.registry-select {
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  font-size: 0.85rem;
  background: white;
  color: #475569;
}

.btn-save {
  padding: 0.4rem 0.8rem;
  background: #3b82f6;
  color: white; border: none;
  border-radius: 8px; font-weight: 700;
  font-size: 0.8rem; cursor: pointer;
}
.btn-save:disabled { background: #e2e8f0; color: #94a3b8; cursor: not-allowed; }

.btn-delete {
  display: flex; align-items: center; gap: 0.5rem;
  background: white; border: 1px solid #fee2e2;
  color: #991b1b; padding: 0.5rem 0.75rem;
  border-radius: 8px; font-weight: 700;
  font-size: 0.8rem; cursor: pointer;
  transition: all 0.2s;
}
.btn-delete:hover:not(:disabled) { background: #fee2e2; border-color: #fecaca; }
.btn-delete:disabled { opacity: 0.5; cursor: not-allowed; filter: grayscale(1); }

/* ── Role Badges ── */
.role-pill {
  width: fit-content;
  font-size: 0.65rem; font-weight: 900;
  padding: 0.2rem 0.6rem; border-radius: 999px;
  text-transform: uppercase; letter-spacing: 0.04em;
}
.role-pill.citizen { background: #eff6ff; color: #1e40af; }
.role-pill.mobility-provider { background: #ecfdf5; color: #065f46; }
.role-pill.city-admin { background: #f5f3ff; color: #5b21b6; }
.role-pill.system-admin { background: #fef2f2; color: #b91c1c; }

/* ── Footer ── */
.registry-footer { margin-top: 2rem; }
.warning-panel {
  background: #fffbeb; border: 1px solid #fef3c7;
  padding: 1.25rem 1.5rem; border-radius: 16px;
  color: #92400e; font-size: 0.9rem; line-height: 1.5;
}

/* ── States ── */
.state-msg { text-align: center; padding: 4rem; color: #64748b; background: white; border-radius: 20px; border: 1px dashed #e2e8f0; }
.state-msg.error { background: #fef2f2; border-color: #fecaca; color: #dc2626; }
.state-msg.pulse { animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: .5; } }

@media (max-width: 1024px) {
  .registry-header { flex-direction: column; gap: 1rem; align-items: flex-start; }
  .search-input { width: 100%; }
  .inline-actions { flex-wrap: wrap; }
}
</style>
