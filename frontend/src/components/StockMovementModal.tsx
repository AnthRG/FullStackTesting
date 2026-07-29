import { useState, type ChangeEvent, type FormEvent } from 'react'
import type { Product } from '../productsApi'
import type { MovementType } from '../stockMovementsApi'
import { registerMovement, resultingQuantity, validateMovement } from '../stockMovementsApi'
import { getFreshToken } from '../auth/keycloak'


const TYPE_LABELS: Record<MovementType, string> = {
  IN: 'Entrada (sumar)',
  OUT: 'Salida (restar)',
  ADJUSTMENT: 'Ajuste (fijar)',
}

interface Props {
  product: Product
  onClose: () => void
  onSaved: () => void
}

export default function StockMovementModal({ product, onClose, onSaved }: Props) {
  const [movementType, setMovementType] = useState<MovementType>('OUT')
  const [quantity, setQuantity] = useState(1)
  const [observations, setObservations] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const invalid = validateMovement(product.quantity, movementType, quantity)
  const resulting = resultingQuantity(product.quantity, movementType, quantity)

  function handleQuantityChange(e: ChangeEvent<HTMLInputElement>) {
    setQuantity(parseInt(e.target.value) || 0)
    setError('')
  }

  function handleTypeChange(e: ChangeEvent<HTMLSelectElement>) {
    setMovementType(e.target.value as MovementType)
    setError('')
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (invalid) {
      setError(invalid)
      return
    }
    setError('')
    setSubmitting(true)
    const token = await getFreshToken()
    try {
      await registerMovement(token, {
        productId: product.id,
        movementType,
        quantity,
        observations: observations.trim() || null,
      })
      onSaved()
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al registrar el movimiento')
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass = 'w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition bg-white'
  const message = error || invalid

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Fondo oscuro */}
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />

      {/* Panel del modal */}
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden">

        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-50 border-b border-slate-200">
          <div>
            <h2 className="text-sm font-semibold text-slate-900">Movimiento de stock</h2>
            <p className="text-xs text-slate-400 mt-0.5 max-w-[260px] truncate">{product.name}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="w-7 h-7 flex items-center justify-center rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200 transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">

          {/* Tipo y cantidad */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label htmlFor="movementType" className="block text-xs font-medium text-slate-500">Tipo *</label>
              <select id="movementType" name="movementType" value={movementType} onChange={handleTypeChange} className={inputClass}>
                <option value="IN">{TYPE_LABELS.IN}</option>
                <option value="OUT">{TYPE_LABELS.OUT}</option>
                <option value="ADJUSTMENT">{TYPE_LABELS.ADJUSTMENT}</option>
              </select>
            </div>
            <div className="space-y-1">
              <label htmlFor="quantity" className="block text-xs font-medium text-slate-500">Cantidad *</label>
              <input
                id="quantity"
                name="quantity"
                type="number"
                min="1"
                step="1"
                value={quantity}
                onChange={handleQuantityChange}
                required
                className={inputClass}
              />
            </div>
          </div>

          {/* Vista previa del stock resultante */}
          <div className="flex items-center justify-between rounded-xl bg-slate-50 border border-slate-100 px-4 py-3">
            <span className="text-xs text-slate-500">Stock resultante</span>
            <span className="text-sm tabular-nums text-slate-500">
              {product.quantity}
              <span className="mx-2 text-slate-300">→</span>
              <span
                data-testid="resulting-quantity"
                className={`font-semibold ${invalid ? 'text-red-500' : 'text-slate-900'}`}
              >
                {invalid ? '—' : resulting}
              </span>
            </span>
          </div>

          {/* Observaciones */}
          <div className="space-y-1">
            <label htmlFor="observations" className="block text-xs font-medium text-slate-500">Observaciones</label>
            <textarea
              id="observations"
              name="observations"
              value={observations}
              onChange={e => setObservations(e.target.value)}
              rows={2}
              className={`${inputClass} resize-none`}
            />
          </div>

          {/* Error de validacion local o del backend */}
          {message && (
            <div role="alert" className="flex items-start gap-2 rounded-xl bg-red-50 border border-red-100 px-4 py-3">
              <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 text-red-500 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
              </svg>
              <p className="text-xs text-red-600">{message}</p>
            </div>
          )}

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm text-slate-600 border border-slate-200 rounded-xl hover:bg-slate-50 transition-colors">
              Cancelar
            </button>
            <button type="submit" disabled={submitting || invalid !== null}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-xl hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {submitting ? 'Registrando…' : 'Registrar'}
            </button>
          </div>

        </form>
      </div>
    </div>
  )
}
