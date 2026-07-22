import { test, expect } from '@playwright/test'
import { gotoAuthenticated, login, statCardValue, sectionByHeading } from './helpers'

test.describe('Reportes', () => {
  test.beforeEach(async ({ page }) => {
    await login(page, 'admin')
    await gotoAuthenticated(page, '/reports')
    await expect(page.getByRole('heading', { name: 'Reportes' })).toBeVisible()
  })

  test('muestra las stat cards del resumen con valores', async ({ page }) => {
    const main = page.locator('main')
    for (const label of ['Productos', 'Unidades en stock', 'Movimientos']) {
      await expect(statCardValue(main, label)).toHaveText(/\d+/)
    }
    await expect(statCardValue(main, 'Valor del inventario')).toHaveText(/\$/)
  })

  test('muestra las secciones de más vendidos, stock bajo y movimientos por tipo', async ({ page }) => {
    for (const heading of ['Más vendidos', 'Stock bajo', 'Movimientos por tipo']) {
      await expect(page.getByRole('heading', { name: heading })).toBeVisible()
    }
  })

  test('un rango de fechas invertido no llega al API: error inline o bloqueo nativo', async ({ page }) => {
    const section = sectionByHeading(page, 'Movimientos por tipo')
    const desde = section.locator('label:has-text("Desde") input')
    const hasta = section.locator('label:has-text("Hasta") input')

    await desde.fill('2026-07-10')
    await hasta.fill('2026-07-01')
    await section.getByRole('button', { name: 'Aplicar' }).click()

    const inlineError = await page
      .getByText('La fecha "desde" no puede ser posterior a la fecha "hasta".')
      .isVisible()
    const nativeBlocked = await hasta.evaluate(el => !(el as HTMLInputElement).validity.valid)
    expect(inlineError || nativeBlocked).toBeTruthy()
  })

  test('un rango válido muestra las 3 tarjetas de tipos con conteos', async ({ page }) => {
    const section = sectionByHeading(page, 'Movimientos por tipo')
    await section.locator('label:has-text("Desde") input').fill('2026-01-01')
    await section.locator('label:has-text("Hasta") input').fill('2026-12-31')
    await section.getByRole('button', { name: 'Aplicar' }).click()

    for (const label of ['Entradas', 'Salidas', 'Ajustes']) {
      await expect(section.getByText(label, { exact: true })).toBeVisible()
    }
  })
})
