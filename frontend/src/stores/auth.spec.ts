import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { api } from '../api'
import { useAuthStore } from './auth'

vi.mock('../api', () => ({
  api: {
    post: vi.fn(),
    put: vi.fn(),
    get: vi.fn(),
  },
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('login success stores user and sets role computeds', async () => {
    vi.mocked(api.post).mockResolvedValue({
      data: {
        id: 'u1',
        name: 'Test',
        email: 't@t.ca',
        role: 'CITIZEN',
      },
    })
    const store = useAuthStore()
    await store.login('t@t.ca', 'secret')
    expect(store.isLoggedIn).toBe(true)
    expect(store.isCitizen).toBe(true)
    expect(store.isAdmin).toBe(false)
    expect(store.isSysAdmin).toBe(false)
    expect(store.user?.email).toBe('t@t.ca')
  })

  it('login failure sets error and does not log in', async () => {
    vi.mocked(api.post).mockRejectedValue({
      response: { data: { error: 'Bad credentials' } },
    })
    const store = useAuthStore()
    await expect(store.login('x@x.ca', 'wrong')).rejects.toBeDefined()
    expect(store.isLoggedIn).toBe(false)
    expect(store.error).toBe('Bad credentials')
  })

  it('logout clears session', async () => {
    vi.mocked(api.post).mockResolvedValue({
      data: {
        id: 'u1',
        name: 'Test',
        email: 't@t.ca',
        role: 'MOBILITY_PROVIDER',
      },
    })
    const store = useAuthStore()
    await store.login('t@t.ca', 'secret')
    store.logout()
    expect(store.isLoggedIn).toBe(false)
    expect(store.user).toBeNull()
    expect(store.error).toBeNull()
  })
})
