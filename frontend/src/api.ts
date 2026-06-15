export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export interface User {
  username: string
  email: string | null
  roles: string[]
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  refreshToken?: string
}

/** Cambia credenciales por un token llamando al controlador de auth del backend. */
export async function loginRequest(username: string, password: string): Promise<LoginResult> {
  const res = await fetch(`${API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) {
    if (res.status === 401) throw new Error('Usuario o contraseña incorrectos')
    throw new Error(`Error ${res.status}`)
  }
  return res.json()
}

/** Llama al endpoint protegido /api/auth/me con el Bearer token (lo valida Spring Security). */
export async function fetchMe(token: string): Promise<User> {
  const res = await fetch(`${API_URL}/api/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}
