<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  loading?: boolean
}>()

const emit = defineEmits(['add-card'])

const cardType = ref('VISA')
const holder = ref('')
const number = ref('')
const expiryMonth = ref('')
const expiryYear = ref('')
const cvv = ref('')
const storeForFuture = ref(true)

// Generate restricted options
const currentYear = new Date().getFullYear() % 100
const years = Array.from({ length: 10 }, (_, i) => (currentYear + i).toString().padStart(2, '0'))
const months = Array.from({ length: 12 }, (_, i) => (i + 1).toString().padStart(2, '0'))

function submitCard() {
  if (!holder.value || !number.value || !expiryMonth.value || !expiryYear.value || !cvv.value) {
    return
  }
  emit('add-card', {
    type: cardType.value,
    holder: holder.value,
    number: number.value,
    expiryMonth: expiryMonth.value,
    expiryYear: expiryYear.value,
    cvv: cvv.value,
    save: storeForFuture.value
  })
  
  // Optional: Reset form after emit if handled successfully by parent
}

// Simple numeric restriction for inputs
function onlyNumbers(e: KeyboardEvent) {
  if (!/[0-9]/.test(e.key) && e.key !== 'Backspace' && e.key !== 'Tab') {
    e.preventDefault()
  }
}
</script>

<template>
  <div class="settings-card-comp">
    <div class="card-header">
       <h3>Add New Payment Method</h3>
       <p>Link a credit or debit card to your mobility wallet.</p>
    </div>

    <div class="form-body">
      <div class="field-row">
        <div class="field flex-2">
          <label>Card Issuer</label>
          <select v-model="cardType">
            <option value="VISA">VISA</option>
            <option value="MASTERCARD">Mastercard</option>
            <option value="AMEX">American Express</option>
          </select>
        </div>
        <div class="field flex-3">
          <label>Cardholder Name</label>
          <input v-model="holder" placeholder="e.g. John Smith" />
        </div>
      </div>

      <div class="field">
        <label>Card Number</label>
        <input 
          v-model="number" 
          placeholder="0000 0000 0000 0000" 
          maxlength="19"
          @keypress="onlyNumbers" 
        />
      </div>

      <div class="security-row">
        <div class="expiry-group">
          <label>Expiry Date</label>
          <div class="expiry-inputs">
            <select v-model="expiryMonth" class="small-select">
              <option value="" disabled selected>MM</option>
              <option v-for="m in months" :key="m" :value="m">{{ m }}</option>
            </select>
            <select v-model="expiryYear" class="small-select">
              <option value="" disabled selected>YY</option>
              <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
            </select>
          </div>
        </div>

        <div class="cvv-group">
          <label>CVV/CVC</label>
          <input 
            v-model="cvv" 
            type="password" 
            placeholder="***" 
            maxlength="4" 
            @keypress="onlyNumbers"
          />
        </div>
      </div>

      <div class="checkbox-row">
        <div class="custom-checkbox">
          <input type="checkbox" id="store-card" v-model="storeForFuture" />
          <label for="store-card">Store this card for future trips</label>
        </div>
      </div>

      <button 
        @click="submitCard" 
        :disabled="loading" 
        class="proceed-btn"
      >
        <span v-if="!loading">Add Secure Card</span>
        <span v-else class="loader-dots">Processing...</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.settings-card-comp {
  background: white;
  border-radius: 24px;
  padding: 2.5rem;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.05);
  border: 1px solid #f1f5f9;
  font-family: 'Inter', system-ui, sans-serif;
}

.card-header {
  margin-bottom: 2rem;
}

.card-header h3 {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 0.5rem;
}

.card-header p {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.field-row {
  display: flex;
  gap: 1rem;
}

.flex-2 { flex: 2; }
.flex-3 { flex: 3; }

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

input {
  padding: 0.85rem 1.1rem;
}

select {
  padding: 0 0.8rem;
  height: 50px;
  line-height: 50px;
}

input, select {
  width: 100%;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  font-size: 1rem;
  color: #0f172a;
  font-weight: 500;
  transition: all 0.2s;
  outline: none;
}

input:focus, select:focus {
  background: white;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.06);
}

.security-row {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
}

.expiry-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.expiry-inputs {
  display: flex;
  gap: 0.5rem;
}

.small-select {
  flex: 1;
}

.cvv-group {
  width: 80px;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.checkbox-row {
  margin-top: 0.5rem;
}

.custom-checkbox {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
}

.custom-checkbox input {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.custom-checkbox label {
  color: #64748b;
  text-transform: none;
  font-size: 0.95rem;
  font-weight: 500;
  letter-spacing: 0;
  cursor: pointer;
}

.proceed-btn {
  background: #0f172a;
  color: white;
  border: none;
  border-radius: 14px;
  padding: 1.1rem;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
  margin-top: 0.5rem;
}

.proceed-btn:hover:not(:disabled) {
  background: #1e293b;
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(15, 23, 42, 0.2);
}

.proceed-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loader-dots {
  display: inline-block;
}
</style>
