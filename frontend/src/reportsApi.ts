import { API_URL } from './api'
import type { MovementType } from './stockMovementsApi'

export interface ReportSummary {
  totalProducts: number
  activeProducts: number
  inactiveProducts: number
  totalUnits: number
  inventoryValue: number
  criticalProducts: number
  totalMovements: number
}

export interface TopProduct {
  productId: number
  productName: string
  productSku: string
  unitsOut: number
  movementCount: number
}

export interface LowStockProduct {
  productId: number
  name: string
  sku: string
  category: string
  quantity: number
  minimumStock: number
  deficit: number
}

export interface MovementsByType {
  movementType: MovementType
  movementCount: number
  totalUnits: number
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

function errorMessage(status: number, detail?: string): string {
  if (detail) return detail
  const messages: Record<number, string> = {
    400: 'El rango de fechas no es válido.',
    401: 'Sesión expirada. Por favor inicia sesión nuevamente.',
    403: 'No tienes permiso para ver los reportes.',
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

export async function getReportSummary(token: string): Promise<ReportSummary> {
  const res = await fetch(`${API_URL}/api/reports/summary`, { headers: auth(token) })
  return handle(res)
}

export async function getTopProducts(token: string, limit = 5): Promise<TopProduct[]> {
  const q = new URLSearchParams({ limit: String(limit) })
  const res = await fetch(`${API_URL}/api/reports/top-products?${q}`, { headers: auth(token) })
  return handle(res)
}

export async function getLowStockProducts(token: string): Promise<LowStockProduct[]> {
  const res = await fetch(`${API_URL}/api/reports/low-stock`, { headers: auth(token) })
  return handle(res)
}

export async function getMovementsByType(token: string, params: {
  from?: string; to?: string
} = {}): Promise<MovementsByType[]> {
  const q = new URLSearchParams()
  if (params.from) q.set('from', params.from)
  if (params.to) q.set('to', params.to)
  const res = await fetch(`${API_URL}/api/reports/movements-by-type?${q}`, { headers: auth(token) })
  return handle(res)
}
