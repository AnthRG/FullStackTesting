import { test, expect, type APIRequestContext } from '@playwright/test'
import { BACKEND_URL, adminToken, gotoAuthenticated, login, uniqueSku } from './helpers'

// Crea un producto por API con stock justo por encima del minimo, para luego
// cruzar el umbral con un movimiento OUT y disparar la alerta.
async function createProduct(request: APIRequestContext, token: string, sku: string) {
  const res = await request.post(`${BACKEND_URL}/api/products`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      name: `Producto Alerta ${sku}`, sku, description: null, category: 'E2E',
      price: 25.5, quantity: 10, minimumStock: 5, status: 'ACTIVE',
    },
  })
  return (await res.json()).id as number
}

test.describe('Alertas de stock', () => {
  test('una salida que cruza el minimo llega al panel en vivo y navega al producto', async ({ page, request }) => {
    const token = await adminToken(request)
    const auth = { Authorization: `Bearer ${token}` }
    const sku = uniqueSku('E2E-ALERT')

    const productId = await createProduct(request, token, sku)

    await login(page, 'admin')
    await page.getByRole('button', { name: /^Notificaciones/ }).click()
    const panel = page.getByRole('dialog', { name: 'Notificaciones de stock' })
    await expect(panel).toBeVisible()
    await expect(panel.getByText('En vivo')).toBeVisible({ timeout: 10000 })

    // Con el panel abierto, un OUT de 6 deja el stock en 4 (minimo 5) y debe empujar la alerta por WebSocket.
    await request.post(`${BACKEND_URL}/api/stock-movements`, {
      headers: auth,
      data: { productId, movementType: 'OUT', quantity: 6, observations: 'E2E alerta' },
    })

    const alert = panel.getByText(sku, { exact: false }).first()
    await expect(alert).toBeVisible({ timeout: 15000 })
    await expect(panel.getByText('Stock bajo').first()).toBeVisible()

    // Al clickearla navega a la tabla de productos ya filtrada por ese SKU.
    await alert.click()
    await expect(page).toHaveURL(new RegExp(`/products\\?search=${sku}`))
    await expect(page.getByRole('row').filter({ hasText: sku })).toBeVisible()

    // Cleanup: se desactiva en vez de borrar porque un producto con movimientos
    // no se puede eliminar (la FK de stock_movements no tiene cascade).
    const cleanup = await request.put(`${BACKEND_URL}/api/products/${productId}`, {
      headers: auth,
      data: {
        name: `Producto Alerta ${sku}`, sku, description: null, category: 'E2E',
        price: 25.5, quantity: 4, minimumStock: 5, status: 'INACTIVE',
      },
    })
    expect(cleanup.ok()).toBeTruthy()
  })

  test('el panel abre y cierra sin interrumpir la navegacion', async ({ page }) => {
    await login(page, 'admin')
    await gotoAuthenticated(page, '/products')

    const bell = page.getByRole('button', { name: /^Notificaciones/ })
    await bell.click()
    const panel = page.getByRole('dialog', { name: 'Notificaciones de stock' })
    await expect(panel).toBeVisible()

    // Escape lo cierra y la pagina de fondo sigue operativa.
    await page.keyboard.press('Escape')
    await expect(panel).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Nuevo producto' })).toBeEnabled()
  })
})
