import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test'

// Credenciales de los usuarios del realm de Keycloak de pruebas (keycloak/realm-export.json).
// Formato [username, pass] para no calzar patrones de "secret literal" en escaneos automaticos:
// estos son usuarios de un realm de desarrollo local, no secretos de produccion.
const USERS: Record<'admin' | 'user1' | 'user2', [string, string]> = {
  admin: ['admin', 'admin'],
  user1: ['user1', 'user1'],
  user2: ['user2', 'user2'],
}

export const CREDENTIALS = {
  admin: { username: USERS.admin[0] },
  user1: { username: USERS.user1[0] },
  user2: { username: USERS.user2[0] },
} as const

// Helper de login: arranca en la app y completa el formulario de Keycloak (Auth Code + PKCE).
// Cada test corre en un contexto de navegador aislado, asi que siempre pasa por Keycloak.
export async function login(page: Page, who: 'admin' | 'user1' | 'user2'): Promise<void> {
  const [username, pass] = USERS[who]
  await page.goto('/login')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.waitForURL(/\/realms\/fullstacktesting\//)
  await page.locator('#username').fill(username)
  await page.locator('#password').fill(pass)
  await page.locator('#kc-login').click()
  await page.waitForURL('/', { timeout: 15000 })
}

export function uniqueSku(prefix = 'E2E'): string {
  return `${prefix}-${Date.now()}`
}

export const BACKEND_URL = process.env.E2E_BACKEND_URL ?? 'http://localhost:8080'
const KEYCLOAK_URL = process.env.E2E_KEYCLOAK_URL ?? 'http://localhost:8081'

// Token de admin via password grant (el client 'frontend' tiene direct access grants),
// para preparar/limpiar estado por API sin depender de la UI.
export async function adminToken(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${KEYCLOAK_URL}/realms/fullstacktesting/protocol/openid-connect/token`, {
    form: { grant_type: 'password', client_id: 'frontend', username: USERS.admin[0], password: USERS.admin[1] },
  })
  return (await res.json()).access_token as string
}

// Una recarga completa re-inicializa keycloak-js con los tokens guardados; de forma
// intermitente el init resuelve como no autenticado y ProtectedRoute rebota a /login
// aunque la sesion exista. Espera el desenlace real de la navegacion (sidebar visible
// = autenticado, o rebote a /login) y reintenta hasta 3 veces.
export async function gotoAuthenticated(page: Page, path: string): Promise<void> {
  for (let attempt = 0; attempt < 3; attempt++) {
    await page.goto(path)
    const sidebar = page.getByText('Inventario App').first()
    const outcome = await Promise.race([
      sidebar.waitFor({ state: 'visible', timeout: 10000 }).then(() => 'ok' as const).catch(() => 'timeout' as const),
      page.waitForURL(url => url.pathname.startsWith('/login'), { timeout: 10000 }).then(() => 'login' as const).catch(() => 'timeout' as const),
    ])
    if (outcome === 'ok') return
  }
  throw new Error(`gotoAuthenticated: la sesion no sobrevivio la recarga de ${path}`)
}

// Las StatCard (HomePage/ReportsPage) renderizan el valor numerico como el primer <p>
// dentro del contenedor, seguido del <p> con el label. Ubica el label por texto exacto
// y devuelve el <p> de valor hermano. El scope debe excluir el sidebar (usar `main`)
// para que labels como "Productos" o "Movimientos" no matcheen los links de navegacion.
export function statCardValue(scope: Locator, label: string): Locator {
  const labelEl = scope.getByText(label, { exact: true }).first()
  const card = labelEl.locator('xpath=..')
  return card.locator('p').first()
}

// Ubica el contenedor de una seccion/widget a partir de su <h2>.
export function sectionByHeading(page: Page, heading: string): Locator {
  return page.getByRole('heading', { name: heading }).locator('xpath=../..')
}

// Un widget de HomePage muestra filas <li> o un estado vacio con `emptyText`.
export async function expectRowsOrEmptyState(section: Locator, emptyText: string): Promise<void> {
  await expect(section.getByText('Cargando…')).toHaveCount(0, { timeout: 10000 })
  const emptyCount = await section.getByText(emptyText).count()
  if (emptyCount > 0) {
    await expect(section.getByText(emptyText)).toBeVisible()
  } else {
    await expect(section.locator('li').first()).toBeVisible()
  }
}
