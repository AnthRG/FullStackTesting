import { test, expect } from '@playwright/test'
import { gotoAuthenticated, login } from './helpers'

// Regresion del logout: el boton tiene que cerrar la sesion en Keycloak, no solo
// limpiar el localStorage. Si la sesion de Keycloak sobrevive, volver a una ruta
// protegida re-autentica solo y el usuario nunca sale.

test.describe('Cerrar sesión', () => {
  test('el botón devuelve a /login y la sesión de Keycloak queda cerrada', async ({ page }) => {
    await login(page, 'admin')
    await gotoAuthenticated(page, '/products')

    await page.getByRole('button', { name: 'Cerrar sesión' }).click()

    await page.waitForURL(/\/login$/, { timeout: 15000 })
    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()

    // Volver a una ruta protegida no debe re-entrar: sin sesion en Keycloak, rebota.
    await page.goto('/products')
    await page.waitForURL(/\/login$/, { timeout: 15000 })
    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
  })
})
