import { test, expect } from '@playwright/test'
import { gotoAuthenticated, login, uniqueSku } from './helpers'

// Flujo completo: crear y editar un producto genera revisiones Envers que deben
// verse en el modal de historial y en el feed global de /audit.
test.describe('Auditoría', () => {
  const sku = uniqueSku('E2E-AUD')
  const name = `Producto Auditoría ${sku}`

  test('el ciclo crear → editar queda visible en el historial y el feed', async ({ page }) => {
    await login(page, 'admin')

    // Crear producto via UI
    await gotoAuthenticated(page, '/products')
    await page.getByRole('button', { name: 'Nuevo producto' }).click()
    await page.locator('input[name="name"]').fill(name)
    await page.locator('input[name="sku"]').fill(sku)
    await page.locator('input[name="category"]').fill('E2E')
    await page.locator('input[name="price"]').fill('1500.00')
    await page.locator('input[name="quantity"]').fill('9')
    await page.locator('input[name="minimumStock"]').fill('2')
    await page.getByRole('button', { name: 'Guardar' }).click()

    const row = page.getByRole('row').filter({ hasText: sku })
    await expect(row).toBeVisible()

    // Editar el precio
    await row.getByRole('button', { name: 'Editar' }).click()
    await page.locator('input[name="price"]').fill('1600.00')
    await page.getByRole('button', { name: 'Guardar' }).click()
    await expect(page.getByRole('heading', { name: 'Editar producto' })).toHaveCount(0)

    // Historial del producto: Creado + Modificado con el diff del precio, por admin
    await row.getByRole('button', { name: `Ver historial de ${name}` }).click()
    await expect(page.getByRole('heading', { name: `Historial de ${name}` })).toBeVisible()
    await expect(page.getByText('Creado', { exact: true })).toBeVisible()
    await expect(page.getByText('Modificado', { exact: true })).toBeVisible()
    await expect(page.getByText(/Precio:\s*\$1500\.00\s*→\s*\$1600\.00/)).toBeVisible()
    await expect(page.getByText(/· admin/).first()).toBeVisible()
    await page.getByRole('button', { name: 'Cerrar', exact: true }).click()

    // Feed global
    await gotoAuthenticated(page, '/audit')
    await expect(page.getByRole('heading', { name: 'Auditoría' })).toBeVisible()
    await expect(page.getByText(sku).first()).toBeVisible()

    // Cleanup: eliminar el producto (queda la revisión DELETE, es esperado)
    await gotoAuthenticated(page, '/products')
    const rowAgain = page.getByRole('row').filter({ hasText: sku })
    await rowAgain.getByRole('button', { name: 'Eliminar' }).click()
    await rowAgain.getByRole('button', { name: 'Sí' }).click()
    await expect(page.getByRole('row').filter({ hasText: sku })).toHaveCount(0)
  })
})
