import { test, expect } from '@playwright/test'
import { BACKEND_URL, adminToken, gotoAuthenticated, login } from './helpers'

test.describe('Usuarios y roles', () => {
  test('como admin lista los usuarios del realm con sus roles', async ({ page }) => {
    await login(page, 'admin')
    await gotoAuthenticated(page, '/users')
    await expect(page.getByRole('heading', { name: 'Usuarios y roles' })).toBeVisible()
    for (const username of ['admin', 'user1', 'user2']) {
      await expect(page.getByTestId('user-card').filter({ hasText: username }).first()).toBeVisible()
    }
  })

  test('asignar y quitar un rol a user2 deja el estado como estaba', async ({ page, request }) => {
    // Estado limpio aunque una corrida anterior haya fallado a mitad: quita el rol por API.
    const token = await adminToken(request)
    const auth = { Authorization: `Bearer ${token}` }
    const users = await (await request.get(`${BACKEND_URL}/api/admin/users`, { headers: auth })).json() as Array<{ id: string; username: string }>
    const user2 = users.find(u => u.username === 'user2')
    if (user2) await request.delete(`${BACKEND_URL}/api/admin/users/${user2.id}/roles/product:view`, { headers: auth })

    await login(page, 'admin')
    await gotoAuthenticated(page, '/users')
    const card = page.getByTestId('user-card').filter({ hasText: 'user2' })
    // Sin el rol asignado no hay chip que quitar; el control de alta si esta siempre.
    const removeChip = card.getByRole('button', { name: 'Quitar rol product:view' })
    await expect(card.getByRole('combobox', { name: 'Agregar rol a user2' })).toBeVisible()
    await expect(removeChip).toHaveCount(0)

    // Asignar product:view
    await card.getByRole('combobox', { name: 'Agregar rol a user2' }).selectOption('product:view')
    await card.getByRole('button', { name: 'Agregar', exact: true }).click()
    await expect(removeChip).toBeVisible()

    // Quitarlo con su confirmación inline → estado restaurado
    await removeChip.click()
    await card.getByRole('button', { name: 'Sí' }).click()
    await expect(removeChip).toHaveCount(0)
  })

  test('user2 sin VIEW_ROLES no ve el link ni la página', async ({ page }) => {
    await login(page, 'user2')
    await expect(page.getByRole('heading', { name: 'Bienvenida, user2' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Usuarios' })).toHaveCount(0)

    await gotoAuthenticated(page, '/users')
    await expect(page.getByRole('heading', { name: 'No tienes permiso para ver esta sección' })).toBeVisible()
  })
})
