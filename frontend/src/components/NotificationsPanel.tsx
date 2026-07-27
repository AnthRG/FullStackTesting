import { useEffect, useRef, type RefObject } from 'react'
import { useNavigate } from 'react-router-dom'
import { useNotifications } from '../notifications/NotificationsContext'
import type { Notification } from '../notificationsApi'

const TYPE_BADGE: Record<Notification['type'], { label: string; classes: string }> = {
  OUT_OF_STOCK: { label: 'Sin stock', classes: 'bg-red-50 text-red-700' },
  LOW_STOCK: { label: 'Stock bajo', classes: 'bg-amber-50 text-amber-700' },
}

function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const diffMin = Math.round(diffMs / 60000)
  if (diffMin < 1) return 'ahora mismo'
  if (diffMin < 60) return `hace ${diffMin} min`
  const diffHours = Math.round(diffMin / 60)
  if (diffHours < 24) return `hace ${diffHours} h`
  const diffDays = Math.round(diffHours / 24)
  if (diffDays === 1) return 'ayer'
  if (diffDays < 7) return `hace ${diffDays} días`
  return new Date(iso).toLocaleDateString('es-DO', { day: '2-digit', month: 'short', year: 'numeric' })
}

interface Props {
  onClose: () => void
  anchorRef: RefObject<HTMLElement | null>
}

export default function NotificationsPanel({ onClose, anchorRef }: Props) {
  const { items, unreadCount, connected, markRead, markAllRead } = useNotifications()
  const navigate = useNavigate()
  const panelRef = useRef<HTMLDivElement>(null)

  // Cierra al hacer click fuera (excepto sobre el botón que abre/cierra el panel) y con Escape.
  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node
      if (panelRef.current?.contains(target)) return
      if (anchorRef.current?.contains(target)) return
      onClose()
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('mousedown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose, anchorRef])

  // Foco manejable: al abrir, el panel recibe el foco (es un landmark de tipo dialog no bloqueante).
  useEffect(() => {
    panelRef.current?.focus()
  }, [])

  function handleItemClick(notification: Notification) {
    if (!notification.read) void markRead(notification.id)
    onClose()
    navigate(`/products?search=${encodeURIComponent(notification.productSku)}`)
  }

  return (
    <div
      ref={panelRef}
      role="dialog"
      aria-label="Notificaciones de stock"
      tabIndex={-1}
      className="fixed inset-x-4 top-16 z-40 rounded-2xl border border-slate-200 bg-white shadow-xl overflow-hidden flex flex-col focus:outline-none md:absolute md:inset-x-auto md:top-0 md:left-full md:ml-6 md:w-96"
    >
      {/* Punta que lo ancla visualmente al botón de la campana */}
      <span
        aria-hidden="true"
        className="hidden md:block absolute -left-1.5 top-5 w-3 h-3 rotate-45 border-l border-b border-slate-200 bg-white"
      />

      {/* Encabezado */}
      <div className="flex items-center justify-between gap-2 px-4 py-3 border-b border-slate-100 shrink-0">
        <div className="flex items-center gap-2 min-w-0">
          <h2 className="text-sm font-semibold text-slate-900">Notificaciones</h2>
          <span
            className={`inline-flex items-center gap-1 text-[11px] font-medium shrink-0 ${connected ? 'text-emerald-700' : 'text-slate-400'}`}
            title={connected ? 'Conectado en tiempo real' : 'Sin conexión en tiempo real, reintentando…'}
          >
            <span aria-hidden="true" className={`w-1.5 h-1.5 rounded-full ${connected ? 'bg-emerald-600' : 'bg-slate-300'}`} />
            {connected ? 'En vivo' : 'Reconectando…'}
          </span>
        </div>
        <div className="flex items-center gap-1 shrink-0">
          {unreadCount > 0 && (
            <button
              type="button"
              onClick={() => { void markAllRead() }}
              aria-label="Marcar todas como leídas"
              title="Marcar todas como leídas"
              className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-blue-700 hover:bg-blue-50 transition-colors cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue-500 focus-visible:outline-offset-2"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round" d="m2.5 12.75 4.5 4.5 8-11.5" />
                <path strokeLinecap="round" strokeLinejoin="round" d="m10.5 15.75 1.5 1.5 9-12" />
              </svg>
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar notificaciones"
            title="Cerrar"
            className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue-500 focus-visible:outline-offset-2"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      {/* Lista */}
      <div className="max-h-[60vh] overflow-y-auto divide-y divide-slate-100">
        {items.length === 0 && (
          <div className="flex flex-col items-center gap-2 px-4 py-10 text-center">
            <svg xmlns="http://www.w3.org/2000/svg" className="w-8 h-8 text-slate-200" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
            </svg>
            <p className="text-sm text-slate-300">No hay alertas de stock</p>
          </div>
        )}
        {items.map(n => (
          <button
            key={n.id}
            type="button"
            onClick={() => handleItemClick(n)}
            className="w-full text-left px-4 py-3 flex gap-3 hover:bg-slate-50 transition-colors cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-blue-500"
          >
            <span aria-hidden="true" className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${n.read ? '' : 'bg-blue-500'}`} />
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium ${TYPE_BADGE[n.type].classes}`}>
                  {TYPE_BADGE[n.type].label}
                </span>
                <span className="text-xs text-slate-400">{formatRelativeTime(n.createdAt)}</span>
              </div>
              <p className="text-sm font-medium text-slate-900 mt-1 truncate">{n.productName}</p>
              <p className="text-xs text-slate-400 font-mono">{n.productSku}</p>
              <p className="text-xs text-slate-600 mt-1">{n.message}</p>
              <p className="text-xs text-slate-400 mt-0.5">Cantidad: {n.quantity} · Mínimo: {n.minimumStock}</p>
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
