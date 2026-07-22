import { API_URL } from './api'

export type MovementType = 'IN' | 'OUT' | 'ADJUSTMENT'

export interface StockMovement {
  id: number
  productId: number
  productName: string
  productSku: string
  movementType: MovementType
  quantity: number
  previousQuantity: number
  newQuantity: number
  userId: string
  observations: string | null
  createdAt: string
}

export interface MovementPage {
  content: StockMovement[]
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
    403: 'No tienes permiso para ver los movimientos de stock.',
    404: 'El movimiento no fue encontrado.',
    500: 'Error interno del servidor. Intenta de nuevo más tarde.',
  }
  return messages[status] ?? `Error inesperado (${status})`
}

export async function listMovements(token: string, params: {
  productId?: number; movementType?: MovementType | ''; page?: number; size?: number
} = {}): Promise<MovementPage> {
  const q = new URLSearchParams()
  if (params.productId != null) q.set('productId', String(params.productId))
  if (params.movementType) q.set('movementType', params.movementType)
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 10))
  q.set('sort', 'createdAt,desc')
  const res = await fetch(`${API_URL}/api/stock-movements?${q}`, { headers: auth(token) })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
  return res.json()
}
