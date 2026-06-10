import { useCallback, useEffect, useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import { API_URL, getBackendHealth } from './api'
import './App.css'

type BackendStatus = 'checking' | 'connected' | 'disconnected'

function App() {
  const [status, setStatus] = useState<BackendStatus>('checking')
  const [detail, setDetail] = useState<string>('')

  const checkBackend = useCallback(async () => {
    setStatus('checking')
    setDetail('')
    try {
      const health = await getBackendHealth()
      setStatus('connected')
      setDetail(`status: ${health.status}`)
    } catch (error) {
      setStatus('disconnected')
      setDetail(error instanceof Error ? error.message : String(error))
    }
  }, [])

  useEffect(() => {
    checkBackend()
  }, [checkBackend])

  const statusLabel: Record<BackendStatus, string> = {
    checking: 'Verificando...',
    connected: 'Conectado',
    disconnected: 'Desconectado',
  }

  return (
    <main className="app">
      <div className="hero">
        <img src={viteLogo} className="logo" alt="Vite logo" />
        <img src={reactLogo} className="logo" alt="React logo" />
      </div>
      <h1>Full Stack Testing</h1>
      <section className={`status-card ${status}`}>
        <h2>Backend</h2>
        <p>
          <code>{API_URL}</code>
        </p>
        <p className="status-label">{statusLabel[status]}</p>
        {detail && <p className="status-detail">{detail}</p>}
        <button type="button" onClick={checkBackend} disabled={status === 'checking'}>
          Reintentar
        </button>
      </section>
    </main>
  )
}

export default App
