import { useState, type FormEvent, type ChangeEvent } from 'react'
import type { Product, ProductRequest } from '../productsApi'
import { createProduct, updateProduct } from '../productsApi'

const TOKEN_KEY = 'access_token'

function initialForm(product: Product | null): ProductRequest {
  if (!product) {
    return { name: '', sku: '', description: null, category: '', price: 0, quantity: 0, minimumStock: 0, status: 'ACTIVE' }
  }
  return {
    name: product.name, sku: product.sku, description: product.description,
    category: product.category, price: product.price, quantity: product.quantity,
    minimumStock: product.minimumStock, status: product.status,
  }
}

interface Props {
  product: Product | null
  onClose: () => void
  onSaved: () => void
}

export default function ProductModal({ product, onClose, onSaved }: Props) {
  const [form, setForm] = useState<ProductRequest>(() => initialForm(product))
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) {
    const { name, value } = e.target
    setForm(prev => ({
      ...prev,
      [name]: name === 'price' ? parseFloat(value) || 0
        : name === 'quantity' || name === 'minimumStock' ? parseInt(value) || 0
        : name === 'description' ? (value || null)
        : value,
    } as ProductRequest))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
    try {
      if (product) await updateProduct(token, product.id, form)
      else await createProduct(token, form)
      onSaved()
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar')
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass = 'w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition bg-white'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Fondo oscuro */}
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />

      {/* Panel del modal */}
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-xl overflow-hidden">

        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-50 border-b border-slate-200">
          <h2 className="text-sm font-semibold text-slate-900">
            {product ? 'Editar producto' : 'Nuevo producto'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="w-7 h-7 flex items-center justify-center rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200 transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">

          {/* Nombre y SKU */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">Nombre *</label>
              <input name="name" value={form.name} onChange={handleChange} required className={inputClass} />
            </div>
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">SKU *</label>
              <input name="sku" value={form.sku} onChange={handleChange} required className={inputClass} />
            </div>
          </div>

          {/* Categoría y Estado */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">Categoría *</label>
              <input name="category" value={form.category} onChange={handleChange} required className={inputClass} />
            </div>
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">Estado *</label>
              <select name="status" value={form.status} onChange={handleChange} className={inputClass}>
                <option value="ACTIVE">Activo</option>
                <option value="INACTIVE">Inactivo</option>
              </select>
            </div>
          </div>

          {/* Precio, Cantidad, Stock mínimo */}
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">Precio *</label>
              <input name="price" type="number" min="0" step="0.01" value={form.price} onChange={handleChange} required className={inputClass} />
            </div>
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">Cantidad *</label>
              <input name="quantity" type="number" min="0" step="1" value={form.quantity} onChange={handleChange} required className={inputClass} />
            </div>
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-500">Stock mín. *</label>
              <input name="minimumStock" type="number" min="0" step="1" value={form.minimumStock} onChange={handleChange} required className={inputClass} />
            </div>
          </div>

          {/* Descripción */}
          <div className="space-y-1">
            <label className="block text-xs font-medium text-slate-500">Descripción</label>
            <textarea name="description" value={form.description ?? ''} onChange={handleChange} rows={3}
              className={`${inputClass} resize-none`} />
          </div>

          {/* Error */}
          {error && (
            <div className="flex items-start gap-2 rounded-xl bg-red-50 border border-red-100 px-4 py-3">
              <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 text-red-500 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
              </svg>
              <p className="text-xs text-red-600">{error}</p>
            </div>
          )}

          {/* Botones */}
          <div className="flex justify-end gap-3 pt-1">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm text-slate-600 border border-slate-200 rounded-xl hover:bg-slate-50 transition-colors">
              Cancelar
            </button>
            <button type="submit" disabled={submitting}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-xl hover:bg-blue-700 disabled:opacity-50 transition-colors">
              {submitting ? 'Guardando…' : 'Guardar'}
            </button>
          </div>

        </form>
      </div>
    </div>
  )
}
