import Keycloak from 'keycloak-js'
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, KEYCLOAK_URL } from '../config'

export const ACCESS_TOKEN_KEY = 'access_token'
export const REFRESH_TOKEN_KEY = 'refresh_token'
export const ID_TOKEN_KEY = 'id_token'

const keycloak = new Keycloak({
  url: KEYCLOAK_URL,
  realm: KEYCLOAK_REALM,
  clientId: KEYCLOAK_CLIENT_ID,
})

let initPromise: Promise<boolean> | null = null

export function initKeycloak(): Promise<boolean> {
  if (!initPromise) {
    initPromise = keycloak.init({
      pkceMethod: 'S256',
      checkLoginIframe: false,
      token: localStorage.getItem(ACCESS_TOKEN_KEY) ?? undefined,
      refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) ?? undefined,
      // Sin el id_token restaurado, tras recargar la pagina el logout sale sin
      // `id_token_hint` y Keycloak intercala una pantalla de confirmacion en vez
      // de cerrar la sesion y volver a la app.
      idToken: localStorage.getItem(ID_TOKEN_KEY) ?? undefined,
    })
  }
  return initPromise
}

/**
 * Cierra la sesion en Keycloak y vuelve a `/login`.
 *
 * `keycloak.logout` solo existe despues de que `init()` resuelve (el adaptador se crea
 * ahi), asi que se espera al init antes de llamarlo. Si el init fallo o el redirect no
 * llega a salir, se navega a /login igual: los tokens locales ya se borraron y quedarse
 * en la pantalla anterior es justo lo que se siente como "el logout no sirve".
 */
export async function logoutKeycloak(): Promise<void> {
  const redirectUri = `${window.location.origin}/login`
  try {
    await initPromise
    await keycloak.logout({ redirectUri })
  } catch {
    window.location.replace(redirectUri)
  }
}

/**
 * Devuelve un access token vigente, renovandolo si esta por vencer.
 *
 * Leer `localStorage` directamente entrega el token que habia en el ultimo render, que
 * con `accessTokenLifespan` en 5 minutos puede estar vencido: el backend responde 401 y
 * la sesion se cae aunque el refresh token siguiera siendo valido.
 *
 * `updateToken(30)` renueva solo si al token le quedan menos de 30 segundos, asi que
 * llamarlo en cada peticion es barato. Si la renovacion falla (refresh token vencido o
 * revocado) se devuelve lo que haya y se deja que el 401 dispare el logout de la pagina.
 */
export async function getFreshToken(): Promise<string> {
  try {
    await keycloak.updateToken(30)
  } catch {
    /* el 401 de la peticion se encarga de cerrar la sesion */
  }
  return keycloak.token ?? localStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}

export default keycloak
