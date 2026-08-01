# Documentación

Sistema de Gestión de Inventarios Empresarial · versión 1.0 · 2026-07-29

## Documentos

| Documento | Contenido | Estándar aplicado |
|---|---|---|
| [01 — Requisitos](01-requisitos.md) | Requisitos funcionales y no funcionales, reglas de negocio y trazabilidad | ISO/IEC/IEEE 29148:2018 (SRS) |
| [02 — Arquitectura](02-arquitectura.md) | Contexto, contenedores, componentes, despliegue y decisiones | Modelo C4, niveles 1-3 |
| [03 — Instalación](03-instalacion.md) | Puesta en marcha local y despliegue a staging y producción | — |
| [04 — Mantenimiento](04-mantenimiento.md) | Operación, observabilidad, respaldos, migraciones y runbooks | — |
| [05 — Guía de pruebas](05-guia-pruebas.md) | Plan, especificación de casos e informe de cierre | ISO/IEC/IEEE 29119-3:2021 |

## Anexos generados

Estos cuatro documentos **no se editan a mano**: se generan desde el sistema real, así que no
pueden quedar desincronizados con el código.

| Anexo | Origen | Herramienta |
|---|---|---|
| [A — Casos de prueba](anexo-a-casos-de-prueba.md) | Resultados JUnit, Cucumber y Playwright | `scripts/docs/gen_test_catalog.py` |
| [B — Referencia de la API](anexo-b-api.md) | Contrato OpenAPI que publica la aplicación | [openapi-to-md](https://www.npmjs.com/package/openapi-to-md) |
| [C — Esquema de base de datos](anexo-c-esquema-bd.md) | Base de datos tras aplicar las migraciones | [tbls](https://github.com/k1LoW/tbls) |
| [D — Interfaz de usuario](anexo-d-interfaz.md) | Capturas de la aplicación en ejecución | Playwright, `playwright.capturas.config.ts` |

```bash
docker compose up -d                     # el sistema debe estar en marcha
./gradlew test securityTest              # para el anexo A
npx playwright test --reporter=json > playwright-report/results.json

bash scripts/docs/generate.sh            # regenera anexos, capturas e informes
bash scripts/docs/generate.sh api        # o solo uno: api | db | tests | capturas | reportes
```

## Informes de pruebas

Los informes HTML completos están versionados en [`reportes/`](reportes): pruebas de backend,
de seguridad, escenarios BDD, cobertura JaCoCo y extremo a extremo. Se abren directamente
desde el repositorio clonado, sin volver a ejecutar la suite. El resumen en Markdown de todos
ellos está en la [guía de pruebas](05-guia-pruebas.md#3-informe-de-cierre).

## Convenciones

- Los diagramas están en **Mermaid** dentro del propio Markdown: GitHub los renderiza sin
  imágenes externas y se revisan en el *diff* como texto.
- Los identificadores son estables: `RF-xx` y `RNF-xx` para requisitos, `RN-xx` para reglas de
  negocio, `AD-xx` para decisiones de arquitectura, y los prefijos por suite (`UT-`, `IT-`,
  `BDD-`, `SEC-`, `E2E-`, `DB-INT-`, `DATA-`) para los casos de prueba.
- La trazabilidad va en una sola dirección: cada requisito del SRS declara qué caso lo
  verifica.
