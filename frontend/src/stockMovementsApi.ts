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

export interface StockMovementRequest {
  productId: number
  movementType: MovementType
  quantity: number
  observations: string | null
}

/** Stock que deja el movimiento: IN suma, OUT resta y ADJUSTMENT fija la cantidad exacta. */
export function resultingQuantity(current: number, type: MovementType, quantity: number): number {
  if (type === 'IN') return current + quantity
  if (type === 'OUT') return current - quantity
  return quantity
}

/**
 * Misma regla que StockMovementService.register(): cantidad positiva y stock que
 * nunca queda negativo. El backend vuelve a validarlo (400/409); esto solo evita
 * el viaje y le dice al usuario por que no puede enviarlo.
 */
export function validateMovement(current: number, type: MovementType, quantity: number): string | null {
  if (!Number.isInteger(quantity) || quantity < 1) {
    return 'La cantidad debe ser un entero mayor que cero.'
  }
  const resulting = resultingQuantity(current, type, quantity)
  if (resulting < 0) {
    return `No puedes sacar ${quantity} unidades: solo hay ${current} disponibles.`
  }
  return null
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

function errorMessage(status: number, detail?: string): string {
  if (detail) return detail
  const messages: Record<number, string> = {
    400: 'Los datos del movimiento no son válidos.',
    401: 'Sesión expirada. Por favor inicia sesión nuevamente.',
    403: 'No tienes permiso para esta operación sobre el stock.',
    404: 'El producto o movimiento no fue encontrado.',
    409: 'No hay stock suficiente para registrar esa salida.',
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

export async function registerMovement(token: string, data: StockMovementRequest): Promise<StockMovement> {
  const res = await fetch(`${API_URL}/api/stock-movements`, {
    method: 'POST', headers: auth(token), body: JSON.stringify(data),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
  return res.json()
}
