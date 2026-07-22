import { test, expect } from '@playwright/test'
import { gotoAuthenticated, login } from './helpers'

test.describe('Historial de movimientos', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'admin')
    await gotoAuthenticated(page, '/movements')
  })

  test('muestra la tabla con sus encabezados', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Historial de movimientos' })).toBeVisible()
    for (const header of ['Fecha', 'Producto', 'Tipo', 'Cantidad', 'Stock', 'Usuario', 'Observaciones']) {
      await expect(page.getByRole('columnheader', { name: header })).toBeVisible()
    }
  })

  test('el filtro por tipo deja solo movimientos de salida', async ({ page }) => {
    await page.locator('select').selectOption('OUT')

    // O bien no hay salidas (estado vacio) o todas las celdas de la columna Tipo dicen "Salida".
    const emptyState = page.getByText('Sin movimientos registrados.')
    const typeCells = page.locator('tbody tr td:nth-child(3)')
    await expect(emptyState.or(typeCells.first())).toBeVisible()
    if (!(await emptyState.isVisible())) {
      const labels = await typeCells.allTextContents()
      expect(labels.length).toBeGreaterThan(0)
      for (const label of labels) expect(label.trim()).toBe('Salida')
    }
  })

  test('el filtro por producto en la URL muestra un chip removible', async ({ page }) => {
    await gotoAuthenticated(page, '/movements?productId=1')
    const chip = page.getByText('Filtrando por producto #1')
    await expect(chip).toBeVisible({ timeout: 10000 })
    await page.getByRole('button', { name: 'Quitar filtro de producto' }).click()
    await expect(chip).toHaveCount(0)
    await expect(page).toHaveURL(/\/movements$/)
  })
})
