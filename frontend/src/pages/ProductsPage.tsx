import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { deleteProduct, listProducts } from '../productsApi'
import type { Product, ProductStatus } from '../productsApi'
import ProductModal from '../components/ProductModal'

const TOKEN_KEY = 'access_token'

export default function ProductsPage() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [products, setProducts] = useState<Product[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<ProductStatus | ''>('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

  const load = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
    setLoading(true)
    setError('')
    try {
      const data = await listProducts(token, { search: search || undefined, status: statusFilter || undefined, page })
      setProducts(data.content)
      setTotalElements(data.totalElements)
      setTotalPages(data.totalPages)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg.includes('401')) { logout(); navigate('/login', { replace: true }) }
      else setError('No se pudo cargar el listado.')
    } finally {
      setLoading(false)
    }
  }, [page, search, statusFilter, refreshKey, logout, navigate])

  useEffect(() => { load() }, [load])

  function handleSearch(e: FormEvent) {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  function handleStatusChange(e: ChangeEvent<HTMLSelectElement>) {
    setStatusFilter(e.target.value as ProductStatus | '')
    setPage(0)
  }

  function openCreate() {
    setEditing(null)
    setModalOpen(true)
  }

  function openEdit(p: Product) {
    setEditing(p)
    setModalOpen(true)
  }

  async function handleDelete() {
    if (deleteId == null) return
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
    setDeleting(true)
    try {
      await deleteProduct(token, deleteId)
      setDeleteId(null)
      setRefreshKey(k => k + 1)
    } catch {
      setError('No se pudo eliminar el producto.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <span className="font-semibold text-blue-900">Full Stack Testing</span>
          <nav className="flex items-center gap-6">
            <Link to="/" className="text-sm text-slate-500 hover:text-slate-800 transition">Inicio</Link>
            <span className="text-sm font-medium text-blue-800 border-b-2 border-blue-800 pb-0.5">Productos</span>
            <button onClick={logout}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700 transition hover:bg-slate-100">
              Cerrar sesión
            </button>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Productos</h1>
            {!loading && <p className="text-sm text-slate-500 mt-0.5">{totalElements} productos en total</p>}
          </div>
          <button onClick={openCreate}
            className="rounded-lg bg-blue-800 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition">
            + Nuevo producto
          </button>
        </div>

        <div className="flex flex-wrap gap-3 mb-4">
          <form onSubmit={handleSearch} className="flex gap-2">
            <input value={searchInput} onChange={e => setSearchInput(e.target.value)}
              placeholder="Buscar por nombre o SKU…"
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-600 transition w-72" />
            <button type="submit"
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 transition">
              Buscar
            </button>
          </form>
          <select value={statusFilter} onChange={handleStatusChange}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 outline-none focus:border-blue-600 transition bg-white">
            <option value="">Todos los estados</option>
            <option value="ACTIVE">Activo</option>
            <option value="INACTIVE">Inactivo</option>
          </select>
        </div>

        <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          {error && <p className="px-6 py-4 text-sm text-red-600">{error}</p>}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Nombre</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">SKU</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Categoría</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wide">Precio</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wide">Cantidad</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wide">Stock mín.</th>
                  <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wide">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr><td colSpan={8} className="px-4 py-10 text-center text-sm text-slate-400">Cargando…</td></tr>
                )}
                {!loading && products.length === 0 && (
                  <tr><td colSpan={8} className="px-4 py-10 text-center text-sm text-slate-400">No se encontraron productos.</td></tr>
                )}
                {!loading && products.map(p => (
                  <tr key={p.id} className="hover:bg-blue-50 transition-colors">
                    <td className="px-4 py-3 font-medium text-slate-900 max-w-[180px] truncate">{p.name}</td>
                    <td className="px-4 py-3 text-slate-500 font-mono text-xs">{p.sku}</td>
                    <td className="px-4 py-3 text-slate-600">{p.category}</td>
                    <td className="px-4 py-3 text-right text-slate-900">${p.price.toFixed(2)}</td>
                    <td className="px-4 py-3 text-right text-slate-600">{p.quantity}</td>
                    <td className="px-4 py-3 text-right text-slate-600">{p.minimumStock}</td>
                    <td className="px-4 py-3 text-center">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        p.status === 'ACTIVE' ? 'bg-blue-100 text-blue-800' : 'bg-slate-100 text-slate-500'
                      }`}>
                        {p.status === 'ACTIVE' ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {deleteId === p.id ? (
                        <span className="inline-flex items-center gap-2">
                          <span className="text-xs text-slate-500">¿Eliminar?</span>
                          <button onClick={handleDelete} disabled={deleting}
                            className="text-xs font-medium text-red-600 hover:text-red-700 disabled:opacity-50">
                            {deleting ? '…' : 'Sí'}
                          </button>
                          <button onClick={() => setDeleteId(null)} className="text-xs text-slate-500 hover:text-slate-700">No</button>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-3">
                          <button onClick={() => openEdit(p)} className="text-xs font-medium text-blue-700 hover:text-blue-900 transition">
                            Editar
                          </button>
                          <button onClick={() => setDeleteId(p.id)} className="text-xs font-medium text-slate-400 hover:text-red-500 transition">
                            Eliminar
                          </button>
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="border-t border-slate-200 px-4 py-3 flex items-center justify-between">
              <p className="text-xs text-slate-500">Página {page + 1} de {totalPages} · {totalElements} productos</p>
              <div className="flex gap-2">
                <button onClick={() => setPage(p => p - 1)} disabled={page === 0}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs text-slate-700 hover:bg-slate-50 disabled:opacity-40 transition">
                  Anterior
                </button>
                <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs text-slate-700 hover:bg-slate-50 disabled:opacity-40 transition">
                  Siguiente
                </button>
              </div>
            </div>
          )}
        </div>
      </main>

      <ProductModal open={modalOpen} product={editing} onClose={() => setModalOpen(false)} onSaved={() => setRefreshKey(k => k + 1)} />
    </div>
  )
}
