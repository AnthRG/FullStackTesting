import { test, expect, type Page } from '@playwright/test'

const CREDENTIALS = { username: 'admin', password: 'admin' }

// SKU único por ejecución para evitar conflictos con datos existentes
const TEST_SKU = `E2E-${Date.now()}`

const TEST_PRODUCT = {
  name:         'Laptop E2E Test',
  sku:          TEST_SKU,
  category:     'Tecnología',
  price:        '1500.00',
  quantity:     '5',
  minimumStock: '1',
}

//Helper de login: arranca en la app y completa el formulario de Keycloak (Auth Code + PKCE)
async function login(page: Page, username = CREDENTIALS.username, password = CREDENTIALS.password) {
  await page.goto('/login')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.waitForURL(/\/realms\/fullstacktesting\//)
  await page.locator('#username').fill(username)
  await page.locator('#password').fill(password)
  await page.locator('#kc-login').click()
}

//  Tests de autenticación
test.describe('Login', () => {
  test('credenciales correctas redirigen al home y muestran bienvenida', async ({ page }) => {
    await login(page)

    await expect(page).toHaveURL('/', { timeout: 15000 })
    await expect(page.getByText(`Bienvenida, ${CREDENTIALS.username}`)).toBeVisible()
  })

  test('credenciales incorrectas se quedan en Keycloak con un mensaje de error', async ({ page }) => {
    await login(page, 'usuariofalso', 'clavefalsa')

    await expect(page).toHaveURL(/\/realms\/fullstacktesting\//)
    await expect(page.getByText(/Invalid username or password|Usuario o contraseña inválidos/i)).toBeVisible()
  })
})

// CRUD de Productos (en serie: cada test depende del anterior)

test.describe('CRUD de Productos', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeEach(async ({ page }) => {
    await login(page)
    await expect(page).toHaveURL('/', { timeout: 15000 })
    await page.goto('/products')
    await expect(page.getByRole('heading', { name: 'Productos' })).toBeVisible()
  })

  test('la página de productos carga la tabla de inventario', async ({ page }) => {
    await expect(page.getByRole('table')).toBeVisible()
    await expect(page.getByRole('button', { name: /Nuevo producto/i })).toBeVisible()
  })

  test('crear un nuevo producto', async ({ page }) => {
    await page.getByRole('button', { name: /Nuevo producto/i }).click()

    await page.locator('input[name="name"]').fill(TEST_PRODUCT.name)
    await page.locator('input[name="sku"]').fill(TEST_PRODUCT.sku)
    await page.locator('input[name="category"]').fill(TEST_PRODUCT.category)
    await page.locator('input[name="price"]').fill(TEST_PRODUCT.price)
    await page.locator('input[name="quantity"]').fill(TEST_PRODUCT.quantity)
    await page.locator('input[name="minimumStock"]').fill(TEST_PRODUCT.minimumStock)

    await page.getByRole('button', { name: 'Guardar' }).click()

    await expect(page.getByText(TEST_PRODUCT.name)).toBeVisible()
  })

  test('editar el producto creado', async ({ page }) => {
    const fila = page.getByRole('row').filter({ hasText: TEST_PRODUCT.name })
    await fila.getByRole('button', { name: 'Editar' }).click()

    await page.locator('input[name="name"]').clear()
    await page.locator('input[name="name"]').fill('Laptop E2E Editada')

    await page.getByRole('button', { name: 'Guardar' }).click()

    await expect(page.getByText('Laptop E2E Editada')).toBeVisible()
  })

  test('eliminar el producto editado', async ({ page }) => {
    const fila = page.getByRole('row').filter({ hasText: 'Laptop E2E Editada' })
    await fila.getByRole('button', { name: 'Eliminar' }).click()
    await page.getByRole('button', { name: 'Sí' }).click()

    await expect(page.getByText('Laptop E2E Editada')).not.toBeVisible()
  })
})
