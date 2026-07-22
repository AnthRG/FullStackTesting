import { API_URL } from './api'

export type RevisionType = 'CREATE' | 'UPDATE' | 'DELETE'

export interface ProductSnapshot {
  name: string
  sku: string
  description: string | null
  category: string
  price: number
  quantity: number
  minimumStock: number
  status: 'ACTIVE' | 'INACTIVE'
}

export interface ProductRevision {
  revision: number
  revisionDate: string
  username: string
  revisionType: RevisionType
  product: ProductSnapshot | null
}

export interface AuditFeedItem {
  revision: number
  revisionDate: string
  username: string
  revisionType: RevisionType
  productId: number
  productName: string | null
  productSku: string | null
}

export interface AuditFeedPage {
  content: AuditFeedItem[]
  totalElements: number
  totalPages: number
  number: number
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

function errorMessage(status: number, detail?: string): string {
  if (detail) return detail
  const messages: Record<number, string> = {
    401: 'Sesión expirada. Por favor inicia sesión nuevamente.',
    403: 'No tienes permiso para ver la auditoría.',
    404: 'El producto no tiene historial de cambios.',
    500: 'Error interno del servidor. Intenta de nuevo más tarde.',
  }
  return messages[status] ?? `Error inesperado (${status})`
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
  return res.json()
}

export async function listAuditFeed(token: string, params: {
  page?: number; size?: number
} = {}): Promise<AuditFeedPage> {
  const q = new URLSearchParams()
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 10))
  const res = await fetch(`${API_URL}/api/audit/products?${q}`, { headers: auth(token) })
  return handle(res)
}

export async function listProductRevisions(token: string, productId: number): Promise<ProductRevision[]> {
  const res = await fetch(`${API_URL}/api/audit/products/${productId}/revisions`, { headers: auth(token) })
  return handle(res)
}
