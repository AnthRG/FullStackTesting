import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (user) navigate('/', { replace: true })
  }, [user, navigate])

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-8 shadow-sm text-center">
        <h1 className="text-xl font-semibold text-slate-900">Iniciar sesión</h1>
        <p className="mt-1 mb-6 text-sm text-slate-500">Accede con tu cuenta de Keycloak</p>

        <button
          type="button"
          onClick={login}
          disabled={loading}
          className="w-full rounded-lg bg-slate-900 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
        >
          {loading ? 'Cargando…' : 'Entrar'}
        </button>
      </div>
    </div>
  )
}
