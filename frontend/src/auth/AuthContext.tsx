import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { KeycloakTokenParsed } from 'keycloak-js'
import type { User } from '../api'
import keycloak, { initKeycloak, ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from './keycloak'

interface AuthState {
  user: User | null
  loading: boolean
  login: () => void
  logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

type Claims = KeycloakTokenParsed & { preferred_username?: string; email?: string }

function persistTokens() {
  if (keycloak.token) localStorage.setItem(ACCESS_TOKEN_KEY, keycloak.token)
  if (keycloak.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, keycloak.refreshToken)
}

function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

function currentUser(): User | null {
  const claims = keycloak.tokenParsed as Claims | undefined
  if (!claims) return null
  return {
    username: claims.preferred_username ?? '',
    email: claims.email ?? null,
    roles: claims.realm_access?.roles ?? [],
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true

    keycloak.onAuthSuccess = () => {
      persistTokens()
      if (mounted) setUser(currentUser())
    }
    keycloak.onAuthRefreshSuccess = persistTokens
    keycloak.onAuthLogout = () => {
      clearTokens()
      if (mounted) setUser(null)
    }
    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => {})
    }

    initKeycloak()
      .then((authenticated) => {
        if (!mounted) return
        if (authenticated) {
          persistTokens()
          setUser(currentUser())
        } else {
          clearTokens()
        }
      })
      .catch(() => {
        if (mounted) clearTokens()
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })

    return () => {
      mounted = false
    }
  }, [])

  const login = () => {
    initKeycloak().then(() => keycloak.login())
  }

  const logout = () => {
    clearTokens()
    keycloak.logout({ redirectUri: window.location.origin + '/login' })
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
