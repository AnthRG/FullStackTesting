import { defineConfig, devices } from '@playwright/test'

// Overrideable por env vars para correr contra un stack local ya levantado en otros puertos
// (p. ej. cuando 5173/8080 estan ocupados por otros proyectos). Los defaults quedan intactos
// para CI, que siempre levanta el stack propio via docker compose / gradlew / vite en 5173/8080/8081.
const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'
const BACKEND_URL = process.env.E2E_BACKEND_URL ?? 'http://localhost:8080'
const KEYCLOAK_URL = process.env.E2E_KEYCLOAK_URL ?? 'http://localhost:8081'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: 'html',
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'docker compose up -d --wait db keycloak',
      url: `${KEYCLOAK_URL}/realms/fullstacktesting/.well-known/openid-configuration`,
      reuseExistingServer: true,
      timeout: 120_000,
    },
    {
      command: 'docker compose up -d --wait db && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5440/fullstacktesting ./gradlew bootRun',
      url: `${BACKEND_URL}/actuator/health`,
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      command: 'npm run dev --prefix frontend',
      url: BASE_URL,
      reuseExistingServer: true,
      timeout: 120_000,
    },
  ],
})
