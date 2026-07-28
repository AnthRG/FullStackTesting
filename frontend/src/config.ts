type ConfigKey = 'API_URL' | 'KEYCLOAK_URL' | 'KEYCLOAK_REALM' | 'KEYCLOAK_CLIENT_ID'

declare global {
  interface Window {
    __APP_CONFIG__?: Partial<Record<ConfigKey, string>>
  }
}

const runtime = window.__APP_CONFIG__ ?? {}

// Un placeholder sin sustituir ("${API_URL}") significa que el entrypoint no corrio:
// se descarta para caer al valor de build o al default de desarrollo.
function value(key: ConfigKey, buildTime: string | undefined, fallback: string): string {
  const fromRuntime = runtime[key]
  if (fromRuntime && !fromRuntime.startsWith('${')) return fromRuntime
  return buildTime || fallback
}

export const API_URL = value('API_URL', import.meta.env.VITE_API_URL, 'http://localhost:8080')
export const KEYCLOAK_URL = value('KEYCLOAK_URL', import.meta.env.VITE_KEYCLOAK_URL, 'http://localhost:8081')
export const KEYCLOAK_REALM = value('KEYCLOAK_REALM', import.meta.env.VITE_KEYCLOAK_REALM, 'fullstacktesting')
export const KEYCLOAK_CLIENT_ID = value('KEYCLOAK_CLIENT_ID', import.meta.env.VITE_KEYCLOAK_CLIENT_ID, 'frontend')
