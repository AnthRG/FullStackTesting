export { API_URL } from './config'

export interface User {
  username: string
  email: string | null
  roles: string[]
}
