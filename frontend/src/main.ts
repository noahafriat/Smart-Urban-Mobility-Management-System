import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAccessibilityStore } from './stores/accessibility'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
useAccessibilityStore(pinia).loadFromStorage()

app.use(router)

app.mount('#app')
