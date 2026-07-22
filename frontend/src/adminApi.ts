import { API_URL } from './api'

export interface UserWithRoles {
  id: string
  username: string
  email: string | null
  enabled: boolean
  realmRoles: string[]
}

export interface Role {
  name: string
  description: string | null
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

function errorMessage(status: number, detail?: string): string {
  if (detail) return detail
  const messages: Record<number, string> = {
    401: 'Sesión expirada. Por favor inicia sesión nuevamente.',
    403: 'No tienes permiso para realizar esta acción.',
    404: 'El usuario o rol no fue encontrado.',
    500: 'Error interno del servidor. Intenta de nuevo más tarde.',
  }
  return messages[status] ?? `Error inesperado (${status})`
}

export async function listUsers(token: string): Promise<UserWithRoles[]> {
  const res = await fetch(`${API_URL}/api/admin/users`, { headers: auth(token) })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
  return res.json()
}

export async function listRoles(token: string): Promise<Role[]> {
  const res = await fetch(`${API_URL}/api/admin/roles`, { headers: auth(token) })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
  return res.json()
}

export async function assignRole(token: string, userId: string, role: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/admin/users/${userId}/roles/${role}`, {
    method: 'POST', headers: auth(token),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
}

export async function removeRole(token: string, userId: string, role: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/admin/users/${userId}/roles/${role}`, {
    method: 'DELETE', headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
}
