import Keycloak from 'keycloak-js'

export const ACCESS_TOKEN_KEY = 'access_token'
export const REFRESH_TOKEN_KEY = 'refresh_token'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'fullstacktesting',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'frontend',
})

let initPromise: Promise<boolean> | null = null

export function initKeycloak(): Promise<boolean> {
  if (!initPromise) {
    initPromise = keycloak.init({
      pkceMethod: 'S256',
      checkLoginIframe: false,
      token: localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined,
      refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) ?? undefined,
    })
  }
  return initPromise
}

export default keycloak
