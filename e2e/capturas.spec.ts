import { test, expect, type Page } from '@playwright/test'
import { login } from './helpers'

// Capturas de pantalla de la interfaz para la documentacion.
//
// No es una prueba funcional: recorre la aplicacion como administrador y guarda una imagen
// por pantalla en docs/capturas/. Vive en su propia configuracion de Playwright para que no
// corra en el porton de cada pull request:
//
//   npx playwright test -c playwright.capturas.config.ts
//
// Las capturas salen del sistema real, con los datos de demostracion sembrados por Flyway
// (db/seed), asi que la documentacion muestra siempre lo que la aplicacion hace de verdad.

const CARPETA = 'docs/capturas'

async function capturar(page: Page, nombre: string): Promise<void> {
  // Las animaciones de entrada de los paneles dejan capturas a medio renderizar.
  await page.waitForTimeout(400)
  await page.screenshot({ path: `${CARPETA}/${nombre}.png`, fullPage: true })
}

test.describe('Capturas para la documentacion', () => {
  test.describe.configure({ mode: 'serial' })

  test('pantalla de acceso', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
    await capturar(page, '01-login')
  })

  test('tablero, inventario y modales', async ({ page }) => {
    await login(page, 'admin')

    await expect(page.getByRole('heading', { name: 'Bienvenida, admin' })).toBeVisible()
    await capturar(page, '02-inicio')

    await page.goto('/products')
    await expect(page.getByRole('heading', { name: 'Productos' })).toBeVisible()
    await expect(page.getByRole('table')).toBeVisible()
    await capturar(page, '03-productos')

    await page.getByRole('button', { name: /Nuevo producto/i }).click()
    await expect(page.getByRole('button', { name: 'Guardar' })).toBeVisible()
    await capturar(page, '04-producto-formulario')
    await page.getByRole('button', { name: 'Cancelar' }).click()

    // El boton de movimiento lleva el nombre del producto: se toma el primero de la tabla.
    const movimiento = page.getByRole('button', { name: /^Registrar movimiento de / }).first()
    if (await movimiento.count()) {
      await movimiento.click()
      // Nombre exacto: "Registrar" a secas tambien casa con los botones
      // "Registrar movimiento de <producto>" de cada fila de la tabla.
      await expect(page.getByRole('button', { name: 'Registrar', exact: true })).toBeVisible()
      await capturar(page, '05-movimiento-formulario')
      await page.getByRole('button', { name: 'Cancelar' }).click()
    }
  })

  test('movimientos, reportes, auditoria y usuarios', async ({ page }) => {
    await login(page, 'admin')

    await page.goto('/movements')
    await expect(page.getByRole('heading', { name: 'Historial de movimientos' })).toBeVisible()
    await capturar(page, '06-movimientos')

    await page.goto('/reports')
    await expect(page.getByRole('heading', { name: 'Reportes' })).toBeVisible()
    await capturar(page, '07-reportes')

    await page.goto('/audit')
    await expect(page.getByRole('heading', { name: 'Auditoría' })).toBeVisible()
    await capturar(page, '08-auditoria')

    await page.goto('/users')
    await expect(page.getByRole('heading', { name: 'Usuarios y roles' })).toBeVisible()
    await capturar(page, '09-usuarios')
  })

  test('panel de alertas de stock', async ({ page }) => {
    await login(page, 'admin')
    await page.getByRole('button', { name: /^Notificaciones/ }).first().click()
    await capturar(page, '10-notificaciones')
  })
})
