import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { listProducts } from '../productsApi'
import type { Product } from '../productsApi'
import { listMovements } from '../stockMovementsApi'
import type { MovementType, StockMovement } from '../stockMovementsApi'
import Layout from '../components/Layout'

const TOKEN_KEY = 'access_token'

const TYPE_BADGES: Record<MovementType, { label: string; classes: string }> = {
  IN: { label: 'Entrada', classes: 'bg-emerald-50 text-emerald-700' },
  OUT: { label: 'Salida', classes: 'bg-red-50 text-red-700' },
  ADJUSTMENT: { label: 'Ajuste', classes: 'bg-amber-50 text-amber-700' },
}

const QUANTITY_CLASSES: Record<MovementType, string> = {
  IN: 'text-emerald-700',
  OUT: 'text-red-700',
  ADJUSTMENT: 'text-amber-700',
}

function formatQuantity(type: MovementType, quantity: number): string {
  if (type === 'IN') return `+${quantity}`
  if (type === 'OUT') return `−${quantity}`
  return `=${quantity}`
}

function formatShortDate(iso: string): string {
  return new Date(iso).toLocaleString('es-DO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
}

function isCritical(p: Product): boolean {
  return p.quantity <= p.minimumStock
}

function IconBox() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" />
    </svg>
  )
}

function IconWarning() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
    </svg>
  )
}

function IconMovements() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 7.5 7.5 3m0 0L12 7.5M7.5 3v13.5m13.5 0L16.5 21m0 0L12 16.5m4.5 4.5V7.5" />
    </svg>
  )
}

function IconCheck() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m4.5 12.75 6 6 9-13.5" />
    </svg>
  )
}

interface StatCardProps {
  icon: ReactNode
  label: string
  value: number
  loading: boolean
}

function StatCard({ icon, label, value, loading }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white shadow-sm px-5 py-4 flex items-center gap-4">
      <div className="w-11 h-11 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
        {icon}
      </div>
      <div>
        {loading ? (
          <div className="h-7 w-12 rounded bg-slate-100 animate-pulse" />
        ) : (
          <p className="text-2xl font-bold text-slate-900 tabular-nums">{value}</p>
        )}
        <p className="text-xs text-slate-400">{label}</p>
      </div>
    </div>
  )
}

export default function HomePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [products, setProducts] = useState<Product[]>([])
  const [productsTotal, setProductsTotal] = useState(0)
  const [productsLoading, setProductsLoading] = useState(false)
  const [productsError, setProductsError] = useState('')

  const [recentMovements, setRecentMovements] = useState<StockMovement[]>([])
  const [movementsTotal, setMovementsTotal] = useState(0)
  const [movementsLoading, setMovementsLoading] = useState(false)
  const [movementsError, setMovementsError] = useState('')

  const loadProducts = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
    setProductsLoading(true)
    setProductsError('')
    try {
      // Límite conocido: solo evalúa los primeros 100 productos (ordenados por cantidad) para el cálculo de críticos.
      const data = await listProducts(token, { page: 0, size: 100, sort: 'quantity,asc' })
      setProducts(data.content)
      setProductsTotal(data.totalElements)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg.includes('401')) { logout(); navigate('/login', { replace: true }) }
      else setProductsError('No se pudo cargar el resumen de productos.')
    } finally {
      setProductsLoading(false)
    }
  }, [logout, navigate])

  const loadMovements = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
    setMovementsLoading(true)
    setMovementsError('')
    try {
      const data = await listMovements(token, { size: 5 })
      setRecentMovements(data.content)
      setMovementsTotal(data.totalElements)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg.includes('401')) { logout(); navigate('/login', { replace: true }) }
      else setMovementsError('No se pudo cargar el historial reciente.')
    } finally {
      setMovementsLoading(false)
    }
  }, [logout, navigate])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount es el patrón estándar de carga de datos
    loadProducts()
    loadMovements()
  }, [loadProducts, loadMovements])

  const criticalProducts = products.filter(isCritical)
  const topCritical = criticalProducts.slice(0, 5)

  return (
    <Layout>
      <div className="px-8 py-8 max-w-6xl mx-auto">

        {/* Bienvenida */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-900">Bienvenida, {user?.username}</h1>
          <p className="text-sm text-slate-400 mt-1">Resumen del sistema de gestión de inventario</p>
        </div>

        {/* ── Stat cards ── */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <StatCard icon={<IconBox />} label="Productos" value={productsTotal} loading={productsLoading} />
          <StatCard icon={<IconWarning />} label="Productos críticos" value={criticalProducts.length} loading={productsLoading} />
          <StatCard icon={<IconMovements />} label="Movimientos" value={movementsTotal} loading={movementsLoading} />
        </div>

        {/* ── Widgets ── */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

          {/* Productos críticos */}
          <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100">
              <h2 className="text-sm font-semibold text-slate-900">Productos críticos</h2>
            </div>
            {productsLoading && (
              <div className="px-5 py-10 text-center text-sm text-slate-300">Cargando…</div>
            )}
            {!productsLoading && productsError && (
              <p className="px-5 py-6 text-sm text-red-500">{productsError}</p>
            )}
            {!productsLoading && !productsError && topCritical.length === 0 && (
              <div className="mx-5 my-5 flex items-center gap-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                <IconCheck />
                Todo el stock por encima del mínimo.
              </div>
            )}
            {!productsLoading && !productsError && topCritical.length > 0 && (
              <ul className="divide-y divide-slate-100">
                {topCritical.map(p => (
                  <li key={p.id}>
                    <Link
                      to={`/movements?productId=${p.id}`}
                      className="flex items-center justify-between gap-3 px-5 py-3 hover:bg-blue-50 transition-colors"
                    >
                      <div className="min-w-0">
                        <p className="font-medium text-slate-900 truncate">{p.name}</p>
                        <p className="text-xs text-slate-400 font-mono">{p.sku}</p>
                      </div>
                      <span className={`shrink-0 inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        p.quantity === p.minimumStock ? 'bg-amber-50 text-amber-700' : 'bg-red-50 text-red-700'
                      }`}>
                        {p.quantity} / mín {p.minimumStock}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Historial reciente */}
          <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-900">Historial reciente</h2>
              <Link to="/movements" className="text-xs font-medium text-blue-600 hover:text-blue-800 transition-colors">
                Ver todo →
              </Link>
            </div>
            {movementsLoading && (
              <div className="px-5 py-10 text-center text-sm text-slate-300">Cargando…</div>
            )}
            {!movementsLoading && movementsError && (
              <p className="px-5 py-6 text-sm text-red-500">{movementsError}</p>
            )}
            {!movementsLoading && !movementsError && recentMovements.length === 0 && (
              <p className="px-5 py-10 text-center text-sm text-slate-300">Sin movimientos registrados.</p>
            )}
            {!movementsLoading && !movementsError && recentMovements.length > 0 && (
              <ul className="divide-y divide-slate-100">
                {recentMovements.map(m => (
                  <li key={m.id} className="flex items-center justify-between gap-3 px-5 py-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <span className={`shrink-0 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${TYPE_BADGES[m.movementType].classes}`}>
                        {TYPE_BADGES[m.movementType].label}
                      </span>
                      <p className="font-medium text-slate-900 truncate text-sm">{m.productName}</p>
                    </div>
                    <div className="shrink-0 text-right">
                      <p className={`text-sm font-medium tabular-nums ${QUANTITY_CLASSES[m.movementType]}`}>
                        {formatQuantity(m.movementType, m.quantity)}
                      </p>
                      <p className="text-xs text-slate-400">{formatShortDate(m.createdAt)}</p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

        </div>
      </div>
    </Layout>
  )
}
