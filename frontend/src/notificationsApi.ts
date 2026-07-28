import { API_URL } from './api'

export type NotificationType = 'LOW_STOCK' | 'OUT_OF_STOCK'

export interface Notification {
  id: number
  type: NotificationType
  productId: number
  productName: string
  productSku: string
  quantity: number
  minimumStock: number
  message: string
  createdAt: string
  read: boolean
}

export interface NotificationsResponse {
  items: Notification[]
  unreadCount: number
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

function errorMessage(status: number, detail?: string): string {
  if (detail) return detail
  const messages: Record<number, string> = {
    401: 'Sesión expirada. Por favor inicia sesión nuevamente.',
    403: 'No tienes permiso para ver las notificaciones.',
    404: 'La notificación no fue encontrada.',
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

export async function listNotifications(token: string, onlyUnread = false): Promise<NotificationsResponse> {
  const q = new URLSearchParams({ onlyUnread: String(onlyUnread) })
  const res = await fetch(`${API_URL}/api/notifications?${q}`, { headers: auth(token) })
  return handle(res)
}

export async function markNotificationRead(token: string, id: number): Promise<void> {
  const res = await fetch(`${API_URL}/api/notifications/${id}/read`, {
    method: 'POST', headers: auth(token),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
}

export async function markAllNotificationsRead(token: string): Promise<void> {
  const res = await fetch(`${API_URL}/api/notifications/read-all`, {
    method: 'POST', headers: auth(token),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({})) as { detail?: string }
    throw new Error(errorMessage(res.status, body.detail))
  }
}
