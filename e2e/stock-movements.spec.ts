import { test, expect, type APIRequestContext, type Page } from '@playwright/test'
import { BACKEND_URL, adminToken, gotoAuthenticated, login, uniqueSku } from './helpers'

// Registrar movimientos desde la pantalla de Productos. El caso de negocio que cubre:
// un producto con 6 unidades al que se le sacan 4 tiene que quedar en 2, y no tiene que
// existir forma de dejarlo en negativo desde la UI.

const SKU = uniqueSku('E2E-MOV')
const NOMBRE = `Producto movimientos ${SKU}`

async function crearProducto(request: APIRequestContext, quantity: number): Promise<number> {
  const token = await adminToken(request)
  const res = await request.post(`${BACKEND_URL}/api/products`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      name: NOMBRE,
      sku: SKU,
      description: 'Creado por stock-movements.spec.ts',
      category: 'Pruebas',
      price: 10.0,
      quantity,
      minimumStock: 0,
      status: 'ACTIVE',
    },
  })
  expect(res.status()).toBe(201)
  return (await res.json()).id as number
}

async function borrarProducto(request: APIRequestContext, id: number): Promise<void> {
  const token = await adminToken(request)
  await request.delete(`${BACKEND_URL}/api/products/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
}

// La tabla filtrada por SKU deja una sola fila; la cantidad es su 5ta celda.
function cantidadEnTabla(page: Page) {
  return page.getByRole('row').filter({ hasText: SKU }).locator('td').nth(4)
}

async function abrirProductoFiltrado(page: Page): Promise<void> {
  await gotoAuthenticated(page, `/products?search=${SKU}`)
  await expect(page.getByRole('row').filter({ hasText: SKU })).toBeVisible({ timeout: 10000 })
}

test.describe('Movimientos de stock desde Productos', () => {
  test.describe.configure({ mode: 'serial' })

  let productId: number

  test.beforeAll(async ({ request }) => {
    productId = await crearProducto(request, 6)
  })

  test.afterAll(async ({ request }) => {
    await borrarProducto(request, productId)
  })

  test('quitarle 4 unidades a un producto con 6 lo deja en 2', async ({ page }) => {
    await login(page, 'admin')
    await abrirProductoFiltrado(page)
    await expect(cantidadEnTabla(page)).toHaveText('6')

    await page.getByRole('button', { name: `Registrar movimiento de ${NOMBRE}` }).click()
    await page.locator('#movementType').selectOption('OUT')
    await page.locator('#quantity').fill('4')

    // El modal adelanta el resultado antes de enviar nada.
    await expect(page.getByTestId('resulting-quantity')).toHaveText('2')

    await page.getByRole('button', { name: 'Registrar', exact: true }).click()

    await expect(cantidadEnTabla(page)).toHaveText('2', { timeout: 10000 })
  })

  test('el movimiento aparece en el historial con el stock anterior y el nuevo', async ({ page }) => {
    await login(page, 'admin')
    await gotoAuthenticated(page, `/movements?productId=${productId}`)

    const fila = page.getByRole('row').filter({ hasText: SKU }).first()
    await expect(fila).toBeVisible({ timeout: 10000 })
    await expect(fila).toContainText('Salida')
    await expect(fila).toContainText('−4')
    await expect(fila).toContainText('6 → 2')
  })

  test('no deja sacar mas unidades de las que hay y el stock no se mueve', async ({ page }) => {
    await login(page, 'admin')
    await abrirProductoFiltrado(page)
    await expect(cantidadEnTabla(page)).toHaveText('2')

    await page.getByRole('button', { name: `Registrar movimiento de ${NOMBRE}` }).click()
    await page.locator('#movementType').selectOption('OUT')
    await page.locator('#quantity').fill('3')

    await expect(page.getByRole('alert')).toContainText('solo hay 2 disponibles')
    await expect(page.getByTestId('resulting-quantity')).toHaveText('—')
    await expect(page.getByRole('button', { name: 'Registrar', exact: true })).toBeDisabled()

    // Cerrar sin enviar: el stock sigue donde estaba.
    await page.getByRole('button', { name: 'Cancelar' }).click()
    await expect(cantidadEnTabla(page)).toHaveText('2')
  })

  test('una entrada suma al stock', async ({ page }) => {
    await login(page, 'admin')
    await abrirProductoFiltrado(page)
    await expect(cantidadEnTabla(page)).toHaveText('2')

    await page.getByRole('button', { name: `Registrar movimiento de ${NOMBRE}` }).click()
    await page.locator('#movementType').selectOption('IN')
    await page.locator('#quantity').fill('5')
    await expect(page.getByTestId('resulting-quantity')).toHaveText('7')
    await page.getByRole('button', { name: 'Registrar', exact: true }).click()

    await expect(cantidadEnTabla(page)).toHaveText('7', { timeout: 10000 })
  })

  test('user1 sin stock:manage no ve el boton de movimiento', async ({ page }) => {
    await login(page, 'user1')
    await abrirProductoFiltrado(page)

    await expect(page.getByRole('button', { name: `Registrar movimiento de ${NOMBRE}` })).toHaveCount(0)
  })
})
