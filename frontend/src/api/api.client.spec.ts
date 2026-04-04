import { describe, it, expect } from 'vitest'
import { api } from './index'

describe('api client', () => {
  it('uses JSON content type', () => {
    expect(api.defaults.headers['Content-Type']).toBe('application/json')
  })

  it('points at local backend API base', () => {
    expect(api.defaults.baseURL).toMatch(/8080\/api$/)
  })
})
