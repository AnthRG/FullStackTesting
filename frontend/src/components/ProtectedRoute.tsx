import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

/**
 * Envuelve rutas que requieren sesion. Si no hay usuario, redirige a /login.
 * Mientras valida el token guardado, muestra un estado de carga.
 */
export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm text-slate-400">
        Cargando…
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}
