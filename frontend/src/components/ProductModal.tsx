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

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-slate-900/50" onClick={onClose} />
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-xl overflow-hidden">
        <div className="px-6 py-5 border-b border-slate-200">
          <h2 className="text-base font-semibold text-slate-900">
            {product ? 'Editar producto' : 'Nuevo producto'}
          </h2>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Nombre *</label>
              <input name="name" value={form.name} onChange={handleChange} required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">SKU *</label>
              <input name="sku" value={form.sku} onChange={handleChange} required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Categoría *</label>
              <input name="category" value={form.category} onChange={handleChange} required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Estado *</label>
              <select name="status" value={form.status} onChange={handleChange}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition bg-white">
                <option value="ACTIVE">Activo</option>
                <option value="INACTIVE">Inactivo</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Precio *</label>
              <input name="price" type="number" min="0" step="0.01" value={form.price} onChange={handleChange} required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Cantidad *</label>
              <input name="quantity" type="number" min="0" step="1" value={form.quantity} onChange={handleChange} required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition" />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Stock mín. *</label>
              <input name="minimumStock" type="number" min="0" step="1" value={form.minimumStock} onChange={handleChange} required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Descripción</label>
            <textarea name="description" value={form.description ?? ''} onChange={handleChange} rows={3}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition resize-none" />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 transition">
              Cancelar
            </button>
            <button type="submit" disabled={submitting}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-800 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition">
              {submitting ? 'Guardando…' : 'Guardar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
