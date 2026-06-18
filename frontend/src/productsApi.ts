import { API_URL } from './api'

export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export interface Product {
  id: number
  name: string
  sku: string
  description: string | null
  category: string
  price: number
  quantity: number
  minimumStock: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  name: string
  sku: string
  description: string | null
  category: string
  price: number
  quantity: number
  minimumStock: number
  status: ProductStatus
}

export interface ProductPage {
  content: Product[]
  totalElements: number
  totalPages: number
  number: number
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

export async function listProducts(token: string, params: {
  search?: string; status?: ProductStatus | ''; page?: number
} = {}): Promise<ProductPage> {
  const q = new URLSearchParams()
  if (params.search) q.set('search', params.search)
  if (params.status) q.set('status', params.status)
  q.set('page', String(params.page ?? 0))
  q.set('size', '10')
  q.set('sort', 'createdAt,desc')
  const res = await fetch(`${API_URL}/api/products?${q}`, { headers: auth(token) })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(body.detail ?? `Error ${res.status}`)
  }
  return res.json()
}

export async function createProduct(token: string, data: ProductRequest): Promise<Product> {
  const res = await fetch(`${API_URL}/api/products`, {
    method: 'POST', headers: auth(token), body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(err.detail ?? `Error ${res.status}`)
  }
  return res.json()
}

export async function updateProduct(token: string, id: number, data: ProductRequest): Promise<Product> {
  const res = await fetch(`${API_URL}/api/products/${id}`, {
    method: 'PUT', headers: auth(token), body: JSON.stringify(data),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(err.detail ?? `Error ${res.status}`)
  }
  return res.json()
}

export async function deleteProduct(token: string, id: number): Promise<void> {
  const res = await fetch(`${API_URL}/api/products/${id}`, {
    method: 'DELETE', headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error(`Error ${res.status}`)
}
