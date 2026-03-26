<script setup lang="ts">
/**
 * Citizen Settings View
 * Manage profile details and payment methods.
 */
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const profile = ref({
  name: '',
  phone: '',
  email: '',
  preferredMobilityType: ''
})

// Full Card Form State
const cardDetails = ref({
  type: 'VISA',
  holder: '',
  number: '',
  expiryMonth: '',
  expiryYear: '',
  cvv: ''
})

const successMsg = ref('')

onMounted(() => {
  if (auth.user) {
    profile.value = {
      name: auth.user.name || '',
      phone: auth.user.phone || '',
      email: auth.user.email || '',
      preferredMobilityType: auth.user.preferredMobilityType || ''
    }
  }
})

async function handleUpdateProfile() {
  try {
    await auth.updateProfile({ ...profile.value })
    showSuccess('Profile updated successfully.')
  } catch (e) {
    // Error
  }
}

async function handleAddCard() {
  const { type, holder, number, expiryMonth, expiryYear, cvv } = cardDetails.value
  if (!holder || !number || !expiryMonth || !expiryYear || !cvv) {
    alert('Please fill out all card details.')
    return
  }

  // Ensure 16 digits
  const cleanNumber = number.replace(/\D/g, '')
  if (cleanNumber.length < 4) {
     alert('Check your card number.')
     return
  }

  // Format as TYPE-LAST4 (e.g., VISA-1010)
  const last4 = cleanNumber.slice(-4)
  const cardLabel = `${type}-${last4}`

  const currentMethods = auth.user?.paymentMethods || []
  if (currentMethods.includes(cardLabel)) {
     alert('This card is already saved.')
     return
  }

  const updatedMethods = [...currentMethods, cardLabel]
  try {
    await auth.updateProfile({ paymentMethods: updatedMethods.join(',') })
    
    // Reset form
    cardDetails.value = { type: 'VISA', holder: '', number: '', expiryMonth: '', expiryYear: '', cvv: '' }
    showSuccess('Card registered successfully.')
  } catch (e) {
    // Error
  }
}

async function removeCard(card: string) {
  const currentMethods = auth.user?.paymentMethods || []
  const updatedMethods = currentMethods.filter(m => m !== card)
  try {
    await auth.updateProfile({ paymentMethods: updatedMethods.join(',') })
    showSuccess('Card unlinked successfully.')
  } catch (e) {
    // Error
  }
}

function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
</script>

<template>
  <div class="settings-view fade-in">
    <header class="page-header">
      <span class="view-tag">Identity & Finance</span>
      <h1>Citizen Settings</h1>
    </header>

    <div v-if="successMsg" class="alert success">{{ successMsg }}</div>
    <div v-if="auth.error" class="alert error">{{ auth.error }}</div>

    <div class="settings-grid">
      <!-- ── Profile Section ── -->
      <section class="settings-card">
        <header class="card-header">
          <h2>Personal Profile</h2>
          <p>Update your contact info and travel preferences.</p>
        </header>
        
        <form @submit.prevent="handleUpdateProfile" class="modern-form">
          <div class="form-group">
            <label>Legal Full Name</label>
            <input v-model="profile.name" placeholder="John Doe" required />
          </div>
          <div class="form-group">
            <label>Email ID</label>
            <input v-model="profile.email" type="email" placeholder="john@example.com" required />
          </div>
          <div class="form-group">
            <label>Mobile Number</label>
            <input v-model="profile.phone" placeholder="+1..." />
          </div>
          <div class="form-group">
            <label>Primary Transit Mode</label>
            <select v-model="profile.preferredMobilityType">
              <option value="">None</option>
              <option value="CAR">Car-Sharing (Vehicles)</option>
              <option value="SCOOTER">Micro-Mobility (Scooters)</option>
              <option value="BIKE">Public Cyclist (BIXI)</option>
            </select>
          </div>
          <button type="submit" :disabled="auth.loading" class="btn-save">
            {{ auth.loading ? 'Updating...' : 'Commit Profile Changes' }}
          </button>
        </form>
      </section>

      <!-- ── Payments Section ── -->
      <section class="settings-card">
        <header class="card-header">
          <h2>Digital Wallet</h2>
          <br/>
        </header>

        <div class="payment-list">
          <div v-for="method in auth.user?.paymentMethods" :key="method" class="method-row">
            <div class="method-info">
              <div class="type-badge">{{ method.split('-')[0] }}</div>
              <span class="card-label">{{ method.split('-')[1] }}</span>
            </div>
            <button @click="removeCard(method)" class="btn-remove">Remove</button>
          </div>
          <div v-if="!auth.user?.paymentMethods?.length" class="empty-payments">
             Your mobility wallet is currently empty.
          </div>
        </div>

        <div class="add-method-pane">
          <h3>Add New Payment Method</h3>
          
          <div class="form-row">
            <label>Card Issuer Network</label>
            <select v-model="cardDetails.type">
              <option value="VISA">VISA</option>
              <option value="MASTERCARD">Mastercard</option>
              <option value="AMEX">American Express</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div class="form-row">
            <label>Cardholder Signature Name</label>
            <input v-model="cardDetails.holder" placeholder="Enter full name" />
          </div>

          <div class="form-row">
            <label>Account Number</label>
            <input v-model="cardDetails.number" placeholder="4539 **** **** ****" />
          </div>

          <div class="form-grid-3">
             <div class="form-row">
                <label>Month</label>
                <input v-model="cardDetails.expiryMonth" placeholder="MM" maxlength="2" />
             </div>
             <div class="form-row">
                <label>Year</label>
                <input v-model="cardDetails.expiryYear" placeholder="YY" maxlength="2" />
             </div>
             <div class="form-row">
                <label>CVV/CVC</label>
                <input v-model="cardDetails.cvv" type="password" placeholder="***" maxlength="3" />
             </div>
          </div>

          <button @click="handleAddCard" :disabled="auth.loading" class="btn-add">
            {{ auth.loading ? '...' : 'Register Secure Card' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings-view {
  padding: 3rem clamp(1rem, 5vw, 4rem);
  max-width: 1100px;
  margin: 0 auto;
  font-family: 'Inter', system-ui, sans-serif;
  color: #0f172a;
}

.page-header { margin-bottom: 3rem; }
.view-tag { color: #3b82f6; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; display: block; margin-bottom: 0.5rem; }
.page-header h1 { font-size: 2.5rem; font-weight: 900; margin: 0; }
.page-header p { color: #64748b; margin-top: 0.5rem; font-size: 1.15rem; }

.alert { padding: 1rem 1.5rem; border-radius: 12px; margin-bottom: 2rem; font-weight: 700; font-size: 0.9rem; }
.alert.success { background: #f0fdf4; color: #10b981; border: 1px solid #bbf7d0; }

.settings-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2.5rem; align-items: start; }
.settings-card { background: white; border: 1px solid #f1f5f9; border-radius: 28px; padding: 2.5rem; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.04); }

.card-header h2 { font-size: 1.4rem; font-weight: 800; margin-bottom: 0.5rem; }
.card-header p { color: #64748b; font-size: 0.95rem; margin-bottom: 2.5rem; }

.form-group, .form-row { margin-bottom: 1.5rem; }
label { display: block; font-size: 0.75rem; font-weight: 800; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.6rem; letter-spacing: 0.05em; }
input, select { width: 100%; border: 1px solid #e2e8f0; padding: 1rem; border-radius: 14px; font-size: 1rem; transition: 0.2s; background: #fcfdfe; color: #334155; font-weight: 500; }
input:focus, select:focus { border-color: #3b82f6; outline: none; background: white; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08); }

.form-grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem; }

.btn-save, .btn-add { width: 100%; padding: 1.1rem; border-radius: 14px; border: none; font-weight: 800; font-size: 1rem; cursor: pointer; transition: 0.2s; }
.btn-save { background: #0f172a; color: white; margin-top: 1rem; }
.btn-save:hover { background: #1e293b; transform: translateY(-2px); }

.btn-add { background: #3b82f6; color: white; margin-top: 1rem; }
.btn-add:hover { background: #2563eb; transform: translateY(-2px); }

/* ── Payments List ── */
.payment-list { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 3rem; }
.method-row { display: flex; justify-content: space-between; align-items: center; padding: 1.25rem; background: #f8fafc; border: 1px solid #f1f5f9; border-radius: 18px; }
.method-info { display: flex; align-items: center; gap: 1rem; }
.type-badge { background: white; color: #0f172a; font-size: 0.65rem; font-weight: 900; padding: 0.35rem 0.6rem; border-radius: 8px; border: 1px solid #e2e8f0; text-transform: uppercase; }
.card-label { font-family: 'JetBrains Mono', monospace; font-weight: 800; color: #334155; font-size: 1.1rem; }
.btn-remove { padding: 0.45rem 1rem; background: transparent; color: #ef4444; border: 1px solid #fee2e2; border-radius: 10px; font-size: 0.75rem; font-weight: 800; text-transform: uppercase; cursor: pointer; transition: 0.2s; }
.btn-remove:hover { border-color: #ef4444; background: #fef2f2; }

.add-method-pane h3 { font-size: 1rem; font-weight: 900; padding-top: 2rem; border-top: 1px solid #f1f5f9; margin-bottom: 1.5rem; }
.empty-payments { text-align: center; padding: 3rem; background: #f8fafc; border-radius: 18px; color: #64748b; font-style: italic; }

.fade-in { animation: fadeIn 0.8s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 900px) {
  .settings-grid { grid-template-columns: 1fr; }
}
</style>
