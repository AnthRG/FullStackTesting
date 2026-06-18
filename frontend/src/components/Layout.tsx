import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { ReactNode } from 'react'

function IconHome() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
    </svg>
  )
}

function IconBox() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" />
    </svg>
  )
}

function IconLogout() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15m3 0 3-3m0 0-3-3m3 3H9" />
    </svg>
  )
}

const navLinks = [
  { to: '/',         label: 'Inicio',    icon: <IconHome /> },
  { to: '/products', label: 'Productos', icon: <IconBox /> },
]

interface LayoutProps {
  children: ReactNode
}

export default function Layout({ children }: LayoutProps) {
  const { user, logout } = useAuth()
  const location = useLocation()

  return (
    <div className="flex h-screen bg-slate-50">

      {/* ── Sidebar ── */}
      <aside className="w-56 shrink-0 flex flex-col bg-white border-r border-slate-200">

        {/* Logo */}
        <div className="px-6 py-5 border-b border-slate-100">
          <span className="text-base font-bold text-blue-700 tracking-tight">Inventario App</span>
        </div>

        {/* Links de navegación */}
        <nav className="flex-1 px-3 py-4 space-y-1">
          {navLinks.map(link => (
            <Link
              key={link.to}
              to={link.to}
              className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                location.pathname === link.to
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              {link.icon}
              {link.label}
            </Link>
          ))}
        </nav>

        {/* Usuario y logout */}
        <div className="px-4 py-4 border-t border-slate-200 space-y-2">
          <div className="flex items-center gap-3 px-2">
            <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-700 text-sm font-bold shrink-0">
              {user?.username?.[0]?.toUpperCase()}
            </div>
            <span className="text-sm font-medium text-slate-700 truncate">{user?.username}</span>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-slate-500 hover:text-red-600 hover:bg-red-50 transition-colors"
          >
            <IconLogout />
            Cerrar sesión
          </button>
        </div>

      </aside>

      {/* ── Contenido de la página ── */}
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>

    </div>
  )
}