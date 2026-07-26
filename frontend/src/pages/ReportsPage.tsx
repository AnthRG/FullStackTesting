import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import {
  getLowStockProducts,
  getMovementsByType,
  getReportSummary,
  getTopProducts,
} from '../reportsApi'
import type { LowStockProduct, MovementsByType, ReportSummary, TopProduct } from '../reportsApi'
import type { MovementType } from '../stockMovementsApi'
import Layout from '../components/Layout'
import { getFreshToken } from '../auth/keycloak'

const TOP_PRODUCTS_LIMIT = 10

const MOVEMENT_TYPES: MovementType[] = ['IN', 'OUT', 'ADJUSTMENT']

const MOVEMENT_TYPE_META: Record<MovementType, { label: string; icon: ReactNode; iconBg: string; iconText: string }> = {
  IN: { label: 'Entradas', icon: <IconArrowDownTray />, iconBg: 'bg-emerald-50', iconText: 'text-emerald-600' },
  OUT: { label: 'Salidas', icon: <IconArrowUpTray />, iconBg: 'bg-red-50', iconText: 'text-red-600' },
  ADJUSTMENT: { label: 'Ajustes', icon: <IconAdjustments />, iconBg: 'bg-amber-50', iconText: 'text-amber-600' },
}

function formatCurrency(value: number): string {
  return `$${value.toFixed(2)}`
}

function normalizeMovementsByType(data: MovementsByType[]): Record<MovementType, MovementsByType> {
  const byType = new Map(data.map(d => [d.movementType, d]))
  const result = {} as Record<MovementType, MovementsByType>
  for (const type of MOVEMENT_TYPES) {
    result[type] = byType.get(type) ?? { movementType: type, movementCount: 0, totalUnits: 0 }
  }
  return result
}

function IconBox() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" />
    </svg>
  )
}

function IconArchive() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M20.25 7.5 19.625 18.13a2.25 2.25 0 0 1-2.247 2.12H6.622a2.25 2.25 0 0 1-2.247-2.12L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125Z" />
    </svg>
  )
}

function IconCurrency() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
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

function IconArrowDownTray() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" />
    </svg>
  )
}

function IconArrowUpTray() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5" />
    </svg>
  )
}

function IconAdjustments() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 1 1-3 0m3 0a1.5 1.5 0 1 0-3 0M3.75 6H7.5m9 12h3.75m-3.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-9.75 0h9.75M3.75 12h9.75m-9.75 0a1.5 1.5 0 0 1 3 0m-3 0a1.5 1.5 0 0 0 3 0m9.75 0h3.75" />
    </svg>
  )
}

interface StatCardProps {
  icon: ReactNode
  label: string
  value: string
  breakdown?: string
  loading: boolean
}

function StatCard({ icon, label, value, breakdown, loading }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white shadow-sm px-5 py-4 flex items-center gap-4">
      <div className="w-11 h-11 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
        {icon}
      </div>
      <div className="min-w-0">
        {loading ? (
          <div className="h-7 w-16 rounded bg-slate-100 animate-pulse" />
        ) : (
          <p className="text-2xl font-bold text-slate-900 tabular-nums truncate">{value}</p>
        )}
        <p className="text-xs text-slate-400">{label}</p>
        {!loading && breakdown && (
          <p className="text-xs text-slate-400 mt-0.5">{breakdown}</p>
        )}
      </div>
    </div>
  )
}

export default function ReportsPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [summary, setSummary] = useState<ReportSummary | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(false)
  const [summaryError, setSummaryError] = useState('')

  const [topProducts, setTopProducts] = useState<TopProduct[]>([])
  const [topLoading, setTopLoading] = useState(false)
  const [topError, setTopError] = useState('')

  const [lowStock, setLowStock] = useState<LowStockProduct[]>([])
  const [lowStockLoading, setLowStockLoading] = useState(false)
  const [lowStockError, setLowStockError] = useState('')

  const [movementsByType, setMovementsByType] = useState<MovementsByType[]>([])
  const [movementsLoading, setMovementsLoading] = useState(false)
  const [movementsError, setMovementsError] = useState('')

  const [fromInput, setFromInput] = useState('')
  const [toInput, setToInput] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [dateRangeError, setDateRangeError] = useState('')

  const handleUnauthorized = useCallback((err: unknown): boolean => {
    const msg = err instanceof Error ? err.message : ''
    if (msg.includes('401')) {
      logout()
      navigate('/login', { replace: true })
      return true
    }
    return false
  }, [logout, navigate])

  const loadSummary = useCallback(async () => {
    const token = await getFreshToken()
    setSummaryLoading(true)
    setSummaryError('')
    try {
      const data = await getReportSummary(token)
      setSummary(data)
    } catch (err) {
      if (!handleUnauthorized(err)) setSummaryError('No se pudo cargar el resumen.')
    } finally {
      setSummaryLoading(false)
    }
  }, [handleUnauthorized])

  const loadTopProducts = useCallback(async () => {
    const token = await getFreshToken()
    setTopLoading(true)
    setTopError('')
    try {
      const data = await getTopProducts(token, TOP_PRODUCTS_LIMIT)
      setTopProducts(data)
    } catch (err) {
      if (!handleUnauthorized(err)) setTopError('No se pudo cargar el ranking de productos más vendidos.')
    } finally {
      setTopLoading(false)
    }
  }, [handleUnauthorized])

  const loadLowStock = useCallback(async () => {
    const token = await getFreshToken()
    setLowStockLoading(true)
    setLowStockError('')
    try {
      const data = await getLowStockProducts(token)
      setLowStock(data)
    } catch (err) {
      if (!handleUnauthorized(err)) setLowStockError('No se pudo cargar el listado de stock bajo.')
    } finally {
      setLowStockLoading(false)
    }
  }, [handleUnauthorized])

  const loadMovementsByType = useCallback(async () => {
    const token = await getFreshToken()
    setMovementsLoading(true)
    setMovementsError('')
    try {
      const data = await getMovementsByType(token, { from: from || undefined, to: to || undefined })
      setMovementsByType(data)
    } catch (err) {
      if (!handleUnauthorized(err)) setMovementsError('No se pudo cargar los movimientos por tipo.')
    } finally {
      setMovementsLoading(false)
    }
  }, [from, to, handleUnauthorized])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount es el patrón estándar de carga de datos
    loadSummary()
    loadTopProducts()
    loadLowStock()
  }, [loadSummary, loadTopProducts, loadLowStock])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-filter-change es el patrón estándar de carga de datos
    loadMovementsByType()
  }, [loadMovementsByType])

  function handleApplyDateRange(e: FormEvent) {
    e.preventDefault()
    if (fromInput && toInput && fromInput > toInput) {
      setDateRangeError('La fecha "desde" no puede ser posterior a la fecha "hasta".')
      return
    }
    setDateRangeError('')
    setFrom(fromInput)
    setTo(toInput)
  }

  function handleClearDateRange() {
    setFromInput('')
    setToInput('')
    setDateRangeError('')
    setFrom('')
    setTo('')
  }

  const movementsMap = normalizeMovementsByType(movementsByType)

  return (
    <Layout>
      <div className="px-8 py-8 max-w-6xl mx-auto">

        {/* Encabezado */}
        <div className="mb-8">
          <h1 className="text-xl font-semibold text-slate-900">Reportes</h1>
          <p className="text-sm text-slate-400 mt-0.5">Panorama general del inventario y su actividad</p>
        </div>

        {/* ── Stat cards ── */}
        {summaryError ? (
          <p className="mb-8 rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-sm text-red-600">{summaryError}</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
            <StatCard
              icon={<IconBox />}
              label="Productos"
              value={summary ? String(summary.totalProducts) : '—'}
              breakdown={summary ? `${summary.activeProducts} activos · ${summary.inactiveProducts} inactivos` : undefined}
              loading={summaryLoading}
            />
            <StatCard
              icon={<IconArchive />}
              label="Unidades en stock"
              value={summary ? String(summary.totalUnits) : '—'}
              loading={summaryLoading}
            />
            <StatCard
              icon={<IconCurrency />}
              label="Valor del inventario"
              value={summary ? formatCurrency(summary.inventoryValue) : '—'}
              loading={summaryLoading}
            />
            <StatCard
              icon={<IconMovements />}
              label="Movimientos"
              value={summary ? String(summary.totalMovements) : '—'}
              loading={summaryLoading}
            />
          </div>
        )}

        {/* ── Más vendidos ── */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden mb-8">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-900">Más vendidos</h2>
          </div>
          {topError && (
            <p className="px-6 py-4 text-sm text-red-500 border-b border-slate-100">{topError}</p>
          )}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">#</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Producto</th>
                  <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Unidades salidas</th>
                  <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Movimientos</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {topLoading && (
                  <tr>
                    <td colSpan={4} className="px-5 py-12 text-center text-sm text-slate-300">Cargando…</td>
                  </tr>
                )}
                {!topLoading && !topError && topProducts.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-12 text-center text-sm text-slate-300">
                      Aún no hay salidas de stock registradas.
                    </td>
                  </tr>
                )}
                {!topLoading && topProducts.map((p, i) => (
                  <tr key={p.productId} className="hover:bg-blue-50 transition-colors">
                    <td className="px-5 py-3 text-slate-400 font-medium tabular-nums">{i + 1}</td>
                    <td className="px-5 py-3">
                      <div className="font-medium text-slate-900 max-w-[220px] truncate">{p.productName}</div>
                      <div className="text-xs text-slate-400 font-mono">{p.productSku}</div>
                    </td>
                    <td className="px-5 py-3 text-right text-slate-900 font-medium tabular-nums">{p.unitsOut}</td>
                    <td className="px-5 py-3 text-right text-slate-500 tabular-nums">{p.movementCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* ── Stock bajo ── */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden mb-8">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-900">Stock bajo</h2>
          </div>
          {lowStockError && (
            <p className="px-6 py-4 text-sm text-red-500 border-b border-slate-100">{lowStockError}</p>
          )}
          {lowStockLoading && (
            <div className="px-5 py-12 text-center text-sm text-slate-300">Cargando…</div>
          )}
          {!lowStockLoading && !lowStockError && lowStock.length === 0 && (
            <div className="mx-5 my-5 flex items-center gap-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              <IconCheck />
              Todo el stock está por encima del mínimo.
            </div>
          )}
          {!lowStockLoading && !lowStockError && lowStock.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50">
                    <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Producto</th>
                    <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Categoría</th>
                    <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Stock actual / mín.</th>
                    <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Déficit</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {lowStock.map(p => (
                    <tr key={p.productId} className="hover:bg-blue-50 transition-colors">
                      <td className="px-5 py-3">
                        <div className="font-medium text-slate-900 max-w-[220px] truncate">{p.name}</div>
                        <div className="text-xs text-slate-400 font-mono">{p.sku}</div>
                      </td>
                      <td className="px-5 py-3 text-slate-600">{p.category}</td>
                      <td className="px-5 py-3 text-right text-slate-600 tabular-nums">{p.quantity} / {p.minimumStock}</td>
                      <td className="px-5 py-3 text-right">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium tabular-nums ${
                          p.deficit > 0 ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-700'
                        }`}>
                          {p.deficit > 0 ? `-${p.deficit}` : p.deficit}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* ── Movimientos por tipo ── */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-900">Movimientos por tipo</h2>
          </div>

          <form onSubmit={handleApplyDateRange} className="px-5 py-4 border-b border-slate-100 flex flex-wrap items-end gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-slate-500">Desde</span>
              <input
                type="date"
                value={fromInput}
                onChange={e => setFromInput(e.target.value)}
                max={toInput || undefined}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-slate-500">Hasta</span>
              <input
                type="date"
                value={toInput}
                onChange={e => setToInput(e.target.value)}
                min={fromInput || undefined}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
              />
            </label>
            <button
              type="submit"
              className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
            >
              Aplicar
            </button>
            <button
              type="button"
              onClick={handleClearDateRange}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 transition"
            >
              Limpiar
            </button>
            {dateRangeError && (
              <p className="w-full text-sm text-red-500">{dateRangeError}</p>
            )}
          </form>

          {movementsError && (
            <p className="px-6 py-4 text-sm text-red-500 border-b border-slate-100">{movementsError}</p>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-5">
            {MOVEMENT_TYPES.map(type => {
              const meta = MOVEMENT_TYPE_META[type]
              const entry = movementsMap[type]
              return (
                <div key={type} className="rounded-2xl border border-slate-200 px-5 py-4 flex items-center gap-4">
                  <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${meta.iconBg} ${meta.iconText}`}>
                    {meta.icon}
                  </div>
                  <div className="min-w-0">
                    {movementsLoading ? (
                      <div className="h-7 w-14 rounded bg-slate-100 animate-pulse" />
                    ) : (
                      <p className="text-2xl font-bold text-slate-900 tabular-nums">{entry.movementCount}</p>
                    )}
                    <p className="text-xs text-slate-400">{meta.label}</p>
                    {!movementsLoading && (
                      <p className="text-xs text-slate-400 mt-0.5">{entry.totalUnits} unidades</p>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="mt-6 text-right">
          <Link to="/" className="text-sm font-medium text-blue-600 hover:text-blue-800 transition-colors">
            ← Volver al inicio
          </Link>
        </div>

      </div>
    </Layout>
  )
}
