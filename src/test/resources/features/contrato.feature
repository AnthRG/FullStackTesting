# language: es
Característica: contrato OpenAPI
  Como quien programa contra esta API
  quiero que el documento publicado en /v3/api-docs describa lo que la aplicacion expone
  para poder confiar en Swagger como fuente de verdad.

  Antecedentes:
    Cuando consulto el contrato OpenAPI

  Escenario: el contrato se publica y es OpenAPI 3
    Entonces la respuesta tiene codigo 200
    Y el documento declara una version de OpenAPI 3

  Escenario: el contrato declara los modulos del sistema
    Entonces el contrato declara la ruta "/api/products"
    Y el contrato declara la ruta "/api/stock-movements"
    Y el contrato declara la ruta "/api/reports/summary"

  Escenario: el contrato declara las operaciones del CRUD de productos
    Entonces la ruta "/api/products" declara la operacion "get"
    Y la ruta "/api/products" declara la operacion "post"
    Y la ruta "/api/products/{id}" declara la operacion "put"
    Y la ruta "/api/products/{id}" declara la operacion "delete"

  Escenario: el contrato declara como autenticarse
    Entonces el contrato declara el esquema de seguridad Bearer JWT
