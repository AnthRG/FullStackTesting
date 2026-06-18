import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

function IconInventory() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-12 h-12 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
    </svg>
  )
}

export default function HomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-8 py-24">
      <div className="flex flex-col items-center justify-center text-center max-w-xl">
        <div className="w-28 h-28 rounded-3xl bg-blue-50 border border-blue-100 flex items-center justify-center mb-8 shadow-sm">
          <IconInventory />
        </div>

        {/* Bienvenida */}
        <h1 className="text-3xl font-bold text-slate-900 mb-2 text-center">
          Bienvenida, {user?.username}
        </h1>
        <p className="text-slate-400 mb-12 text-center text-sm">
          Sistema de gestión de inventario
        </p>

        {/* Botón de acceso */}
        <button
          onClick={() => navigate('/products')}
          className="inline-flex items-center gap-2 px-8 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl transition-colors text-sm shadow-sm"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" />
          </svg>
          Gestión de productos
        </button>
      </div>
    </div>
  )
}
