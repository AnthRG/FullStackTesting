import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { fetchMe, loginRequest, type User } from '../api'

const TOKEN_KEY = 'access_token'

interface AuthState {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

/**
 * Mantiene la sesion en el frontend: guarda el token en localStorage y expone
 * login/logout y el usuario actual. Al montar, si hay token, valida la sesion
 * pidiendo /api/auth/me.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setLoading(false)
      return
    }
    fetchMe(token)
      .then(setUser)
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false))
  }, [])

  const login = async (username: string, password: string) => {
    const { accessToken } = await loginRequest(username, password)
    localStorage.setItem(TOKEN_KEY, accessToken)
    setUser(await fetchMe(accessToken))
  }

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
