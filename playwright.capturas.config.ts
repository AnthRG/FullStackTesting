import { defineConfig, devices } from '@playwright/test'
import base from './playwright.config'

// Capturas de pantalla para la documentacion. Configuracion aparte porque no son pruebas
// funcionales: no deben correr en el porton de cada pull request ni contar como casos.
//
//   npx playwright test -c playwright.capturas.config.ts
//
// Reutiliza la configuracion principal (webServer, baseURL, credenciales) y solo cambia
// que archivos ejecuta y el tamano de ventana, para que todas las imagenes salgan iguales.
export default defineConfig({
  ...base,
  testIgnore: undefined,
  testMatch: /capturas\.spec\.ts/,
  reporter: 'line',
  projects: [
    {
      name: 'capturas',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],
})
