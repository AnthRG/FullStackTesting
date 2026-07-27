import { useCallback, useEffect, useState, type ChangeEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { listMovements } from '../stockMovementsApi'
import type { MovementType, StockMovement } from '../stockMovementsApi'
import Layout from '../components/Layout'
import { getFreshToken } from '../auth/keycloak'


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

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('es-DO', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function MovementsPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const productIdParam = searchParams.get('productId')
  const productId = productIdParam && /^\d+$/.test(productIdParam) ? Number(productIdParam) : undefined

  const [movements, setMovements] = useState<StockMovement[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [typeFilter, setTypeFilter] = useState<MovementType | ''>('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    const token = await getFreshToken()
    setLoading(true)
    setError('')
    try {
      const data = await listMovements(token, { productId, movementType: typeFilter || undefined, page })
      setMovements(data.content)
      setTotalElements(data.totalElements)
      setTotalPages(data.totalPages)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg.includes('401')) { logout(); navigate('/login', { replace: true }) }
      else setError('No se pudo cargar el historial de movimientos.')
    } finally {
      setLoading(false)
    }
  }, [page, productId, typeFilter, logout, navigate])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-filter-change es el patrón estándar de carga de datos
    load()
  }, [load])

  function handleTypeChange(e: ChangeEvent<HTMLSelectElement>) {
    setTypeFilter(e.target.value as MovementType | '')
    setPage(0)
  }

  function clearProductFilter() {
    const next = new URLSearchParams(searchParams)
    next.delete('productId')
    setSearchParams(next)
    setPage(0)
  }

  return (
    <Layout>
      <div className="px-8 py-8 max-w-6xl mx-auto">

        {/* Encabezado */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Historial de movimientos</h1>
            {!loading && (
              <p className="text-sm text-slate-400 mt-0.5">{totalElements} movimientos en total</p>
            )}
          </div>
        </div>

        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 mb-5">
          <select
            value={typeFilter}
            onChange={handleTypeChange}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
          >
            <option value="">Todos</option>
            <option value="IN">Entrada</option>
            <option value="OUT">Salida</option>
            <option value="ADJUSTMENT">Ajuste</option>
          </select>

          {productId != null && (
            <span className="inline-flex items-center gap-2 rounded-full bg-blue-50 pl-3 pr-1.5 py-1 text-xs font-medium text-blue-700">
              Filtrando por producto #{productId}
              <button
                onClick={clearProductFilter}
                aria-label="Quitar filtro de producto"
                className="rounded-full p-1 text-blue-500 hover:bg-blue-100 hover:text-blue-800 transition-colors cursor-pointer"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
          )}
        </div>

        {/* Tabla */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          {error && (
            <p className="px-6 py-4 text-sm text-red-500 border-b border-slate-100">{error}</p>
          )}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Fecha</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Producto</th>
                  <th className="px-5 py-3 text-center text-xs font-semibold text-slate-400 uppercase tracking-wide">Tipo</th>
                  <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Cantidad</th>
                  <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Stock</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Usuario</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Observaciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td colSpan={7} className="px-5 py-12 text-center text-sm text-slate-300">
                      Cargando…
                    </td>
                  </tr>
                )}
                {!loading && !error && movements.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-5 py-12 text-center text-sm text-slate-300">
                      Sin movimientos registrados.
                    </td>
                  </tr>
                )}
                {!loading && movements.map(m => (
                  <tr key={m.id} className="hover:bg-blue-50 transition-colors">
                    <td className="px-5 py-3 text-slate-600 whitespace-nowrap">{formatDate(m.createdAt)}</td>
                    <td className="px-5 py-3">
                      <div className="font-medium text-slate-900 max-w-[220px] truncate">{m.productName}</div>
                      <div className="text-xs text-slate-400 font-mono">{m.productSku}</div>
                    </td>
                    <td className="px-5 py-3 text-center">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${TYPE_BADGES[m.movementType].classes}`}>
                        {TYPE_BADGES[m.movementType].label}
                      </span>
                    </td>
                    <td className={`px-5 py-3 text-right font-medium tabular-nums ${QUANTITY_CLASSES[m.movementType]}`}>
                      {formatQuantity(m.movementType, m.quantity)}
                    </td>
                    <td className="px-5 py-3 text-right text-slate-500 tabular-nums whitespace-nowrap">
                      {m.previousQuantity} → {m.newQuantity}
                    </td>
                    <td className="px-5 py-3 text-slate-600">{m.userId}</td>
                    <td className="px-5 py-3 text-slate-500 max-w-[220px] truncate" title={m.observations ?? undefined}>
                      {m.observations ?? '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Paginación */}
          {totalPages > 1 && (
            <div className="border-t border-slate-100 px-5 py-3 flex items-center justify-between">
              <p className="text-xs text-slate-400">
                Página {page + 1} de {totalPages} · {totalElements} movimientos
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => p - 1)}
                  disabled={page === 0}
                  className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30 transition"
                >
                  ← Anterior
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= totalPages - 1}
                  className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30 transition"
                >
                  Siguiente →
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  )
}
