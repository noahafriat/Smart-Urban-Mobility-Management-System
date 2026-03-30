import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'summs-accessibility-mode'

function applyToDocument(enabled: boolean) {
  document.documentElement.classList.toggle('summs-a11y', enabled)
}

/**
 * Enhanced accessibility mode: larger text, stronger focus, link underlines, reduced motion.
 * Preference is stored in localStorage and applied on the root element.
 */
export const useAccessibilityStore = defineStore('accessibility', () => {
  const enabled = ref(false)

  function loadFromStorage() {
    try {
      const v = localStorage.getItem(STORAGE_KEY)
      enabled.value = v === '1' || v === 'true'
    } catch {
      /* private mode */
    }
    applyToDocument(enabled.value)
  }

  function setEnabled(value: boolean) {
    enabled.value = value
    try {
      localStorage.setItem(STORAGE_KEY, value ? '1' : '0')
    } catch {
      /* ignore */
    }
    applyToDocument(value)
  }

  function toggle() {
    setEnabled(!enabled.value)
  }

  return { enabled, loadFromStorage, setEnabled, toggle }
})
