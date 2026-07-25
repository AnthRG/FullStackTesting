import { test, expect } from '@playwright/test'
import { login, statCardValue, sectionByHeading, expectRowsOrEmptyState } from './helpers'

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'admin')
  })

  test('muestra el saludo, las stat cards con numeros y los 3 widgets', async ({ page }) => {
    await expect(page).toHaveURL('/')
    await expect(page.getByRole('heading', { name: 'Bienvenida, admin' })).toBeVisible()

    // Stat cards: Productos / Productos críticos / Movimientos, con valores numericos.
    // Se escopa a <main> porque el sidebar tiene links de texto identico ("Productos", "Movimientos").
    const main = page.locator('main')
    for (const label of ['Productos', 'Productos críticos', 'Movimientos']) {
      await expect(statCardValue(main, label)).toHaveText(/^\d+$/)
    }

    // Los 3 widgets: cada uno con filas o su estado vacio.
    const criticalWidget = sectionByHeading(page, 'Productos críticos')
    await expect(criticalWidget).toBeVisible()
    await expectRowsOrEmptyState(criticalWidget, 'Todo el stock por encima del mínimo.')

    const topWidget = sectionByHeading(page, 'Más vendidos')
    await expect(topWidget).toBeVisible()
    await expectRowsOrEmptyState(topWidget, 'Aún no hay salidas de stock registradas.')

    const recentWidget = sectionByHeading(page, 'Historial reciente')
    await expect(recentWidget).toBeVisible()
    await expectRowsOrEmptyState(recentWidget, 'Sin movimientos registrados.')
  })

  test('el link "Ver todo" navega a /movements', async ({ page }) => {
    await page.getByRole('link', { name: /Ver todo/ }).click()
    await expect(page).toHaveURL(/\/movements$/)
  })

  test('el link "Ver reporte" navega a /reports', async ({ page }) => {
    await page.getByRole('link', { name: /Ver reporte/ }).click()
    await expect(page).toHaveURL(/\/reports$/)
  })
})
