export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export interface User {
  username: string
  email: string | null
  roles: string[]
}
