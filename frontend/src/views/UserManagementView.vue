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
  <div class="admin-view">
    <header class="page-header">
      <div>
        <h1>User Role Management</h1>
        <p>Assign or modify roles for any registered user on the platform.</p>
      </div>
      <button class="refresh-btn" :disabled="loading" @click="fetchUsers">↻ Refresh</button>
    </header>

    <div v-if="error" class="state-msg error">{{ error }}</div>
    <div v-if="loading && users.length === 0" class="state-msg pulse">Loading users…</div>

    <div v-else class="user-table-wrapper">
      <table class="user-table">
        <thead>
          <tr>
            <th>User</th>
            <th>Email</th>
            <th>Current Role</th>
            <th>Change Role</th>
            <th>Apply</th>
            <th>Delete</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id" :class="{ 'success-row': successId === user.id }">
            <td class="name-cell">
              <span class="name">{{ user.name }}</span>
            </td>
            <td class="email-cell">{{ user.email }}</td>
            <td>
              <span class="role-pill" :class="user.role.toLowerCase().replace('_', '-')">
                {{ roleLabels[user.role] }}
              </span>
            </td>
            <td>
              <select v-model="pendingRole[user.id]" class="role-select">
                <option v-for="role in ROLES" :key="role" :value="role">
                  {{ roleLabels[role] }}
                </option>
              </select>
            </td>
            <td>
              <button
                class="apply-btn"
                :disabled="loading || pendingRole[user.id] === user.role"
                @click="applyRole(user)">
                {{ successId === user.id ? '✅ Saved' : 'Apply' }}
              </button>
            </td>
            <td>
              <button
                class="delete-btn"
                :disabled="loading || user.id === auth.user?.id"
                :title="user.id === auth.user?.id ? 'Cannot delete your own account' : 'Delete account'"
                @click="deleteUser(user)">
                Delete
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>


  </div>
</template>

<style scoped>
.admin-view {
  padding: 2rem;
  max-width: 1050px;
  margin: 0 auto;
  display: grid;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.page-header h1 {
  font-size: 2.1rem;
  color: #0f172a;
  margin: 0 0 0.35rem;
}

.page-header p {
  color: #64748b;
  margin: 0;
  font-size: 1rem;
}

.uc-tag {
  display: inline-block;
  background: #ffe4e6;
  color: #9f1239;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  margin-left: 0.4rem;
}

.refresh-btn {
  padding: 0.6rem 1.1rem;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  color: #334155;
  transition: background 0.2s;
  white-space: nowrap;
}

.refresh-btn:hover:not(:disabled) { background: #e2e8f0; }
.refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.state-msg {
  padding: 2.5rem;
  text-align: center;
  background: #fff;
  border-radius: 14px;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.state-msg.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.state-msg.pulse { animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

.user-table-wrapper {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th {
  background: #f8fafc;
  padding: 0.85rem 1rem;
  text-align: left;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
  font-weight: 700;
  border-bottom: 1px solid #e2e8f0;
}

.user-table td {
  padding: 0.9rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  color: #0f172a;
  font-size: 0.92rem;
  vertical-align: middle;
}

.user-table tr:last-child td { border-bottom: none; }

.user-table tr.success-row td {
  background: #f0fdf4;
  transition: background 0.5s;
}

.user-table tr:hover td { background: #fafafa; }
.user-table tr.success-row:hover td { background: #f0fdf4; }

.name-cell .name {
  font-weight: 600;
  color: #1e293b;
}

.email-cell {
  color: #475569;
  font-size: 0.87rem;
}

.role-pill {
  display: inline-block;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.role-pill.citizen { background: #dbeafe; color: #1d4ed8; }
.role-pill.mobility-provider { background: #d1fae5; color: #065f46; }
.role-pill.city-admin { background: #ede9fe; color: #5b21b6; }
.role-pill.system-admin { background: #fee2e2; color: #991b1b; }

.role-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  background: #f8fafc;
  color: #0f172a;
  cursor: pointer;
}

.apply-btn {
  padding: 0.5rem 1rem;
  background: #1e40af;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.88rem;
  transition: background 0.2s;
  white-space: nowrap;
}

.apply-btn:hover:not(:disabled) { background: #1e3a8a; }
.apply-btn:disabled { background: #94a3b8; cursor: not-allowed; }

.delete-btn {
  padding: 0.5rem 1rem;
  background: #dc2626;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.88rem;
  transition: background 0.2s;
  white-space: nowrap;
}

.delete-btn:hover:not(:disabled) { background: #b91c1c; }
.delete-btn:disabled { background: #94a3b8; cursor: not-allowed; }

.warning-note {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  padding: 0.85rem 1.1rem;
  font-size: 0.88rem;
  color: #92400e;
  line-height: 1.5;
}
</style>
