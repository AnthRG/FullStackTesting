import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import { API_URL } from '../api'
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '../notificationsApi'
import type { Notification } from '../notificationsApi'

const TOKEN_KEY = 'access_token'
const RECONNECT_MIN_DELAY = 1000
const RECONNECT_MAX_DELAY = 30000
const MAX_ITEMS = 50

interface NotificationsData {
  items: Notification[]
  unreadCount: number
}

interface NotificationsState extends NotificationsData {
  connected: boolean
  markRead: (id: number) => Promise<void>
  markAllRead: () => Promise<void>
  reload: () => Promise<void>
}

interface IncomingMessage {
  type: string
  payload?: Notification
}

const NotificationsContext = createContext<NotificationsState | undefined>(undefined)

function wsUrl(): string {
  const url = new URL('/ws/notifications', API_URL)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  return url.toString()
}

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [data, setData] = useState<NotificationsData>({ items: [], unreadCount: 0 })
  const [connected, setConnected] = useState(false)

  const socketRef = useRef<WebSocket | null>(null)
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const reconnectDelayRef = useRef(RECONNECT_MIN_DELAY)

  const reload = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) return
    try {
      const res = await listNotifications(token)
      setData({ items: res.items, unreadCount: res.unreadCount })
    } catch {
      // Un fallo de REST no debe romper la app; se conserva el estado previo.
    }
  }, [])

  const markRead = useCallback(async (id: number) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) return
    let wasUnread = false
    setData(prev => {
      const target = prev.items.find(n => n.id === id)
      if (!target || target.read) return prev
      wasUnread = true
      return {
        items: prev.items.map(n => (n.id === id ? { ...n, read: true } : n)),
        unreadCount: Math.max(0, prev.unreadCount - 1),
      }
    })
    if (!wasUnread) return
    try {
      await markNotificationRead(token, id)
    } catch {
      // El estado optimista se reconcilia en la próxima recarga si el servidor difiere.
    }
  }, [])

  const markAllRead = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) return
    setData(prev => ({ items: prev.items.map(n => ({ ...n, read: true })), unreadCount: 0 }))
    try {
      await markAllNotificationsRead(token)
    } catch {
      // El estado optimista se reconcilia en la próxima recarga si el servidor difiere.
    }
  }, [])

  // Carga inicial (y en cada login) por REST.
  useEffect(() => {
    if (!user) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- reset al cerrar sesión, sincroniza con el estado de auth
      setData({ items: [], unreadCount: 0 })
      return
    }
    reload()
  }, [user, reload])

  // Conexión WebSocket con reconexión automática por backoff exponencial acotado (1s -> 30s).
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!user || !token) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- sincroniza el indicador de conexión con el estado de auth
      setConnected(false)
      return
    }

    let cancelled = false

    function connect() {
      if (cancelled) return
      const currentToken = localStorage.getItem(TOKEN_KEY)
      if (!currentToken) return

      const socket = new WebSocket(wsUrl(), ['bearer', currentToken])
      socketRef.current = socket

      socket.onopen = () => {
        if (cancelled) return
        setConnected(true)
        reconnectDelayRef.current = RECONNECT_MIN_DELAY
      }

      socket.onmessage = (event: MessageEvent) => {
        if (cancelled) return
        let msg: IncomingMessage
        try {
          msg = JSON.parse(event.data as string) as IncomingMessage
        } catch {
          return
        }
        // Mensajes tipo PING u otros desconocidos se ignoran.
        if (msg.type !== 'NOTIFICATION' || !msg.payload) return
        const notification = msg.payload
        setData(prev => ({
          items: [notification, ...prev.items.filter(n => n.id !== notification.id)].slice(0, MAX_ITEMS),
          unreadCount: notification.read ? prev.unreadCount : prev.unreadCount + 1,
        }))
      }

      socket.onclose = () => {
        socketRef.current = null
        if (cancelled) return
        setConnected(false)
        reconnectTimerRef.current = setTimeout(connect, reconnectDelayRef.current)
        reconnectDelayRef.current = Math.min(reconnectDelayRef.current * 2, RECONNECT_MAX_DELAY)
      }

      socket.onerror = () => {
        socket.close()
      }
    }

    connect()

    return () => {
      cancelled = true
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current)
      reconnectDelayRef.current = RECONNECT_MIN_DELAY
      socketRef.current?.close()
      socketRef.current = null
      setConnected(false)
    }
  }, [user])

  return (
    <NotificationsContext.Provider value={{ ...data, connected, markRead, markAllRead, reload }}>
      {children}
    </NotificationsContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications(): NotificationsState {
  const ctx = useContext(NotificationsContext)
  if (!ctx) throw new Error('useNotifications debe usarse dentro de <NotificationsProvider>')
  return ctx
}
