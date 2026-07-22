import { useCallback, useEffect, useState } from 'react'
import { listProductRevisions } from '../auditApi'
import type { ProductRevision, ProductSnapshot, RevisionType } from '../auditApi'

const TOKEN_KEY = 'access_token'

const REVISION_BADGES: Record<RevisionType, { label: string; badgeClasses: string; dotClasses: string }> = {
  CREATE: { label: 'Creado', badgeClasses: 'bg-emerald-50 text-emerald-700', dotClasses: 'bg-emerald-500' },
  UPDATE: { label: 'Modificado', badgeClasses: 'bg-blue-50 text-blue-700', dotClasses: 'bg-blue-500' },
  DELETE: { label: 'Eliminado', badgeClasses: 'bg-red-50 text-red-700', dotClasses: 'bg-red-500' },
}

interface FieldChange {
  label: string
  before: string
  after: string
}

interface SummaryField {
  label: string
  value: string
}

type HistoryEntry =
  | { kind: 'create'; revision: ProductRevision; summary: SummaryField[] }
  | { kind: 'update'; revision: ProductRevision; changes: FieldChange[] }
  | { kind: 'delete'; revision: ProductRevision }

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('es-DO', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function formatPrice(value: number): string {
  return `$${value.toFixed(2)}`
}

function formatStatus(status: ProductSnapshot['status']): string {
  return status === 'ACTIVE' ? 'Activo' : 'Inactivo'
}

function diffSnapshots(prev: ProductSnapshot, curr: ProductSnapshot): FieldChange[] {
  const changes: FieldChange[] = []
  if (prev.name !== curr.name) changes.push({ label: 'Nombre', before: prev.name, after: curr.name })
  if (prev.sku !== curr.sku) changes.push({ label: 'SKU', before: prev.sku, after: curr.sku })
  if (prev.description !== curr.description) {
    changes.push({ label: 'Descripción', before: prev.description ?? '—', after: curr.description ?? '—' })
  }
  if (prev.category !== curr.category) changes.push({ label: 'Categoría', before: prev.category, after: curr.category })
  if (prev.price !== curr.price) changes.push({ label: 'Precio', before: formatPrice(prev.price), after: formatPrice(curr.price) })
  if (prev.quantity !== curr.quantity) {
    changes.push({ label: 'Cantidad', before: String(prev.quantity), after: String(curr.quantity) })
  }
  if (prev.minimumStock !== curr.minimumStock) {
    changes.push({ label: 'Stock mínimo', before: String(prev.minimumStock), after: String(curr.minimumStock) })
  }
  if (prev.status !== curr.status) {
    changes.push({ label: 'Estado', before: formatStatus(prev.status), after: formatStatus(curr.status) })
  }
  return changes
}

function createSummary(snapshot: ProductSnapshot): SummaryField[] {
  return [
    { label: 'Categoría', value: snapshot.category },
    { label: 'Precio', value: formatPrice(snapshot.price) },
    { label: 'Cantidad', value: String(snapshot.quantity) },
    { label: 'Stock mínimo', value: String(snapshot.minimumStock) },
    { label: 'Estado', value: formatStatus(snapshot.status) },
  ]
}

function buildEntries(revisions: ProductRevision[]): HistoryEntry[] {
  const entries: HistoryEntry[] = []
  for (let i = 0; i < revisions.length; i++) {
    const revision = revisions[i]
    if (revision.revisionType === 'CREATE') {
      entries.push({ kind: 'create', revision, summary: revision.product ? createSummary(revision.product) : [] })
    } else if (revision.revisionType === 'UPDATE') {
      const previousProduct = revisions[i - 1]?.product ?? null
      const changes = previousProduct && revision.product ? diffSnapshots(previousProduct, revision.product) : []
      entries.push({ kind: 'update', revision, changes })
    } else {
      entries.push({ kind: 'delete', revision })
    }
  }
  return entries.reverse()
}

interface Props {
  productId: number
  productName: string
  onClose: () => void
}

export default function ProductHistoryModal({ productId, productName, onClose }: Props) {
  const [revisions, setRevisions] = useState<ProductRevision[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
    setLoading(true)
    setError('')
    try {
      const data = await listProductRevisions(token, productId)
      setRevisions(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cargar el historial.')
    } finally {
      setLoading(false)
    }
  }, [productId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount es el patrón estándar de carga de datos
    load()
  }, [load])

  const entries = buildEntries(revisions)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Fondo oscuro */}
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />

      {/* Panel del modal */}
      <div className="relative w-full max-w-lg max-h-[85vh] bg-white rounded-2xl shadow-xl overflow-hidden flex flex-col">

        {/* Header */}
        <div className="flex items-center justify-between gap-3 px-6 py-4 bg-slate-50 border-b border-slate-200 shrink-0">
          <div className="min-w-0">
            <h2 className="text-sm font-semibold text-slate-900 truncate">Historial de {productName}</h2>
            <p className="text-xs text-slate-400 mt-0.5">Producto #{productId}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="w-7 h-7 flex items-center justify-center rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200 transition-colors shrink-0 cursor-pointer"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Contenido */}
        <div className="overflow-y-auto px-6 py-5">
          {loading && (
            <p className="text-center text-sm text-slate-300 py-12">Cargando…</p>
          )}
          {!loading && error && (
            <p className="text-center text-sm text-red-500 py-12">{error}</p>
          )}
          {!loading && !error && entries.length === 0 && (
            <p className="text-center text-sm text-slate-300 py-12">Sin historial de cambios.</p>
          )}
          {!loading && !error && entries.length > 0 && (
            <ol className="space-y-5">
              {entries.map(entry => (
                <li key={entry.revision.revision} className="relative pl-5 border-l-2 border-slate-100">
                  <span className={`absolute -left-[5px] top-1 w-2 h-2 rounded-full ${REVISION_BADGES[entry.revision.revisionType].dotClasses}`} />

                  <div className="flex items-center gap-2 flex-wrap">
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${REVISION_BADGES[entry.revision.revisionType].badgeClasses}`}>
                      {REVISION_BADGES[entry.revision.revisionType].label}
                    </span>
                    <span className="text-xs text-slate-400">Rev. #{entry.revision.revision}</span>
                  </div>

                  <p className="text-xs text-slate-500 mt-1">
                    {formatDate(entry.revision.revisionDate)} · {entry.revision.username}
                  </p>

                  <div className="mt-2">
                    {entry.kind === 'delete' && (
                      <p className="text-sm text-slate-600">Producto eliminado</p>
                    )}
                    {entry.kind === 'create' && (
                      <ul className="space-y-1">
                        {entry.summary.map(field => (
                          <li key={field.label} className="text-xs text-slate-600">
                            <span className="font-medium text-slate-500">{field.label}:</span> {field.value}
                          </li>
                        ))}
                      </ul>
                    )}
                    {entry.kind === 'update' && (
                      entry.changes.length === 0 ? (
                        <p className="text-xs text-slate-400">Sin cambios en los campos auditados</p>
                      ) : (
                        <ul className="space-y-1">
                          {entry.changes.map(change => (
                            <li key={change.label} className="text-xs text-slate-600">
                              <span className="font-medium text-slate-500">{change.label}:</span> {change.before} → {change.after}
                            </li>
                          ))}
                        </ul>
                      )
                    )}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>
    </div>
  )
}
