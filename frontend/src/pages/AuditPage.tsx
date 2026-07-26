import { useCallback, useEffect, useState, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { listAuditFeed } from '../auditApi'
import type { AuditFeedItem, RevisionType } from '../auditApi'
import ProductHistoryModal from '../components/ProductHistoryModal'
import Layout from '../components/Layout'
import { getFreshToken } from '../auth/keycloak'


const REVISION_BADGES: Record<RevisionType, { label: string; classes: string }> = {
  CREATE: { label: 'Creado', classes: 'bg-emerald-50 text-emerald-700' },
  UPDATE: { label: 'Modificado', classes: 'bg-blue-50 text-blue-700' },
  DELETE: { label: 'Eliminado', classes: 'bg-red-50 text-red-700' },
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('es-DO', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

interface Selected {
  id: number
  name: string
}

export default function AuditPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [items, setItems] = useState<AuditFeedItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [selected, setSelected] = useState<Selected | null>(null)

  const load = useCallback(async () => {
    const token = await getFreshToken()
    setLoading(true)
    setError('')
    try {
      const data = await listAuditFeed(token, { page })
      setItems(data.content)
      setTotalElements(data.totalElements)
      setTotalPages(data.totalPages)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg.includes('401')) { logout(); navigate('/login', { replace: true }) }
      else setError('No se pudo cargar la auditoría.')
    } finally {
      setLoading(false)
    }
  }, [page, logout, navigate])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-filter-change es el patrón estándar de carga de datos
    load()
  }, [load])

  function openHistory(item: AuditFeedItem) {
    setSelected({ id: item.productId, name: item.productName ?? `Producto #${item.productId}` })
  }

  function handleRowKeyDown(e: KeyboardEvent<HTMLTableRowElement>, item: AuditFeedItem) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      openHistory(item)
    }
  }

  return (
    <Layout>
      <div className="px-8 py-8 max-w-6xl mx-auto">

        {/* Encabezado */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Auditoría</h1>
            {!loading && (
              <p className="text-sm text-slate-400 mt-0.5">{totalElements} cambios registrados</p>
            )}
          </div>
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
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Usuario</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Producto</th>
                  <th className="px-5 py-3 text-center text-xs font-semibold text-slate-400 uppercase tracking-wide">Acción</th>
                  <th className="px-5 py-3 text-right text-xs font-semibold text-slate-400 uppercase tracking-wide">Revisión #</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td colSpan={5} className="px-5 py-12 text-center text-sm text-slate-300">
                      Cargando…
                    </td>
                  </tr>
                )}
                {!loading && !error && items.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-5 py-12 text-center text-sm text-slate-300">
                      Sin cambios registrados.
                    </td>
                  </tr>
                )}
                {!loading && items.map(item => {
                  const productLabel = item.productName ?? `Producto #${item.productId}`
                  return (
                    <tr
                      key={item.revision}
                      onClick={() => openHistory(item)}
                      onKeyDown={e => handleRowKeyDown(e, item)}
                      role="button"
                      tabIndex={0}
                      aria-label={`Ver historial de ${productLabel}`}
                      className="cursor-pointer hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-300 transition-colors"
                    >
                      <td className="px-5 py-3 text-slate-600 whitespace-nowrap">{formatDate(item.revisionDate)}</td>
                      <td className="px-5 py-3 text-slate-600">{item.username}</td>
                      <td className="px-5 py-3">
                        <div className="font-medium text-slate-900 max-w-[220px] truncate">{productLabel}</div>
                        {item.productSku && (
                          <div className="text-xs text-slate-400 font-mono">{item.productSku}</div>
                        )}
                      </td>
                      <td className="px-5 py-3 text-center">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${REVISION_BADGES[item.revisionType].classes}`}>
                          {REVISION_BADGES[item.revisionType].label}
                        </span>
                      </td>
                      <td className="px-5 py-3 text-right text-slate-500 tabular-nums">{item.revision}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* Paginación */}
          {totalPages > 1 && (
            <div className="border-t border-slate-100 px-5 py-3 flex items-center justify-between">
              <p className="text-xs text-slate-400">
                Página {page + 1} de {totalPages} · {totalElements} cambios
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

      {/* Modal */}
      {selected && (
        <ProductHistoryModal
          productId={selected.id}
          productName={selected.name}
          onClose={() => setSelected(null)}
        />
      )}
    </Layout>
  )
}
