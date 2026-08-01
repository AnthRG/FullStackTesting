# Anexo B — Referencia de la API

Documento **generado** desde el contrato OpenAPI que publica la aplicación.
No se edita a mano.

```bash
bash scripts/docs/generate.sh api
```

---


> Version v1

Backend del proyecto. Incluye la administracion de roles de usuarios contra Keycloak.


## Path Table

| Method | Path | Description |
| --- | --- | --- |
| GET | [/api/products/{id}](#getapiproductsid) |  |
| PUT | [/api/products/{id}](#putapiproductsid) |  |
| DELETE | [/api/products/{id}](#deleteapiproductsid) |  |
| GET | [/api/stock-movements](#getapistock-movements) |  |
| POST | [/api/stock-movements](#postapistock-movements) | Registra un movimiento de stock |
| GET | [/api/products](#getapiproducts) |  |
| POST | [/api/products](#postapiproducts) |  |
| POST | [/api/notifications/{id}/read](#postapinotificationsidread) | Marca una alerta como leída |
| POST | [/api/notifications/read-all](#postapinotificationsread-all) | Marca todas las alertas como leídas |
| POST | [/api/admin/users/{id}/roles/{role}](#postapiadminusersidrolesrole) | Asigna un rol a un usuario |
| DELETE | [/api/admin/users/{id}/roles/{role}](#deleteapiadminusersidrolesrole) | Quita un rol a un usuario |
| GET | [/api/stock-movements/{id}](#getapistock-movementsid) |  |
| GET | [/api/reports/top-products](#getapireportstop-products) | Productos más movidos por salida |
| GET | [/api/reports/summary](#getapireportssummary) | Resumen del inventario |
| GET | [/api/reports/movements-by-type](#getapireportsmovements-by-type) | Movimientos agrupados por tipo |
| GET | [/api/reports/low-stock](#getapireportslow-stock) | Productos con stock bajo |
| GET | [/api/notifications](#getapinotifications) | Lista las alertas de stock |
| GET | [/api/auth/me](#getapiauthme) |  |
| GET | [/api/audit/products](#getapiauditproducts) | Feed global de revisiones de productos |
| GET | [/api/audit/products/{id}/revisions](#getapiauditproductsidrevisions) | Historial de revisiones de un producto |
| GET | [/api/admin/users](#getapiadminusers) | Lista los usuarios con sus roles |
| GET | [/api/admin/users/{id}](#getapiadminusersid) | Obtiene un usuario y sus roles |
| GET | [/api/admin/roles](#getapiadminroles) | Lista los roles asignables |

## Reference Table

| Name | Path | Description |
| --- | --- | --- |
| ProductRequest | [#/components/schemas/ProductRequest](#componentsschemasproductrequest) |  |
| ProductResponse | [#/components/schemas/ProductResponse](#componentsschemasproductresponse) |  |
| StockMovementRequest | [#/components/schemas/StockMovementRequest](#componentsschemasstockmovementrequest) |  |
| StockMovementResponse | [#/components/schemas/StockMovementResponse](#componentsschemasstockmovementresponse) |  |
| Pageable | [#/components/schemas/Pageable](#componentsschemaspageable) |  |
| PageStockMovementResponse | [#/components/schemas/PageStockMovementResponse](#componentsschemaspagestockmovementresponse) |  |
| PageableObject | [#/components/schemas/PageableObject](#componentsschemaspageableobject) |  |
| SortObject | [#/components/schemas/SortObject](#componentsschemassortobject) |  |
| TopProductResponse | [#/components/schemas/TopProductResponse](#componentsschemastopproductresponse) |  |
| InventorySummaryResponse | [#/components/schemas/InventorySummaryResponse](#componentsschemasinventorysummaryresponse) |  |
| MovementsByTypeResponse | [#/components/schemas/MovementsByTypeResponse](#componentsschemasmovementsbytyperesponse) |  |
| LowStockProductResponse | [#/components/schemas/LowStockProductResponse](#componentsschemaslowstockproductresponse) |  |
| PageProductResponse | [#/components/schemas/PageProductResponse](#componentsschemaspageproductresponse) |  |
| NotificationListResponse | [#/components/schemas/NotificationListResponse](#componentsschemasnotificationlistresponse) |  |
| NotificationResponse | [#/components/schemas/NotificationResponse](#componentsschemasnotificationresponse) |  |
| PageProductAuditFeedItem | [#/components/schemas/PageProductAuditFeedItem](#componentsschemaspageproductauditfeeditem) |  |
| ProductAuditFeedItem | [#/components/schemas/ProductAuditFeedItem](#componentsschemasproductauditfeeditem) |  |
| ProductRevisionResponse | [#/components/schemas/ProductRevisionResponse](#componentsschemasproductrevisionresponse) |  |
| ProductSnapshot | [#/components/schemas/ProductSnapshot](#componentsschemasproductsnapshot) |  |
| UserRolesView | [#/components/schemas/UserRolesView](#componentsschemasuserrolesview) |  |
| RoleView | [#/components/schemas/RoleView](#componentsschemasroleview) |  |
| bearer-jwt | [#/components/securitySchemes/bearer-jwt](#componentssecurityschemesbearer-jwt) | Access token (JWT) emitido por Keycloak |

## Path Details

***

### [GET]/api/products/{id}

- Operation id  
getById

#### Responses

- 200 OK

`*/*`

```typescript
{
  id?: integer
  name?: string
  sku?: string
  description?: string
  category?: string
  price?: number
  quantity?: integer
  minimumStock?: integer
  status?: enum[ACTIVE, INACTIVE]
  createdAt?: string
  updatedAt?: string
}
```

***

### [PUT]/api/products/{id}

- Operation id  
update

#### RequestBody

- application/json

```typescript
{
  name: string
  sku: string
  description?: string
  category: string
  price: number
  quantity: integer
  minimumStock: integer
  status: enum[ACTIVE, INACTIVE]
}
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  id?: integer
  name?: string
  sku?: string
  description?: string
  category?: string
  price?: number
  quantity?: integer
  minimumStock?: integer
  status?: enum[ACTIVE, INACTIVE]
  createdAt?: string
  updatedAt?: string
}
```

***

### [DELETE]/api/products/{id}

- Operation id  
delete

#### Responses

- 204 No Content

***

### [GET]/api/stock-movements

- Operation id  
list

#### Parameters(Query)

```typescript
productId?: integer
```

```typescript
movementType?: enum[IN, OUT, ADJUSTMENT]
```

```typescript
pageable: {
  page?: integer
  size?: integer
  sort?: string[]
}
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  totalElements?: integer
  totalPages?: integer
  size?: integer
  content: {
    id?: integer
    productId?: integer
    productName?: string
    productSku?: string
    movementType?: enum[IN, OUT, ADJUSTMENT]
    quantity?: integer
    previousQuantity?: integer
    newQuantity?: integer
    userId?: string
    observations?: string
    createdAt?: string
  }[]
  number?: integer
  first?: boolean
  last?: boolean
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  numberOfElements?: integer
  pageable: {
    offset?: integer
    sort:#/components/schemas/SortObject
    pageSize?: integer
    pageNumber?: integer
    paged?: boolean
    unpaged?: boolean
  }
  empty?: boolean
}
```

***

### [POST]/api/stock-movements

- Summary  
Registra un movimiento de stock

- Operation id  
register

- Description  
IN suma al stock, OUT resta (falla con 409 si no hay suficiente) y ADJUSTMENT fija la cantidad exacta. Actualiza el producto y guarda el historial.

#### RequestBody

- application/json

```typescript
{
  productId: integer
  movementType: enum[IN, OUT, ADJUSTMENT]
  quantity: integer
  observations?: string
}
```

#### Responses

- 201 Created

`*/*`

```typescript
{
  id?: integer
  productId?: integer
  productName?: string
  productSku?: string
  movementType?: enum[IN, OUT, ADJUSTMENT]
  quantity?: integer
  previousQuantity?: integer
  newQuantity?: integer
  userId?: string
  observations?: string
  createdAt?: string
}
```

***

### [GET]/api/products

- Operation id  
list_1

#### Parameters(Query)

```typescript
search?: string
```

```typescript
status?: enum[ACTIVE, INACTIVE]
```

```typescript
pageable: {
  page?: integer
  size?: integer
  sort?: string[]
}
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  totalElements?: integer
  totalPages?: integer
  size?: integer
  content: {
    id?: integer
    name?: string
    sku?: string
    description?: string
    category?: string
    price?: number
    quantity?: integer
    minimumStock?: integer
    status?: enum[ACTIVE, INACTIVE]
    createdAt?: string
    updatedAt?: string
  }[]
  number?: integer
  first?: boolean
  last?: boolean
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  numberOfElements?: integer
  pageable: {
    offset?: integer
    sort:#/components/schemas/SortObject
    pageSize?: integer
    pageNumber?: integer
    paged?: boolean
    unpaged?: boolean
  }
  empty?: boolean
}
```

***

### [POST]/api/products

- Operation id  
create

#### RequestBody

- application/json

```typescript
{
  name: string
  sku: string
  description?: string
  category: string
  price: number
  quantity: integer
  minimumStock: integer
  status: enum[ACTIVE, INACTIVE]
}
```

#### Responses

- 201 Created

`*/*`

```typescript
{
  id?: integer
  name?: string
  sku?: string
  description?: string
  category?: string
  price?: number
  quantity?: integer
  minimumStock?: integer
  status?: enum[ACTIVE, INACTIVE]
  createdAt?: string
  updatedAt?: string
}
```

***

### [POST]/api/notifications/{id}/read

- Summary  
Marca una alerta como leída

- Operation id  
markRead

- Description  
404 si la alerta no existe.

#### Responses

- 204 No Content

***

### [POST]/api/notifications/read-all

- Summary  
Marca todas las alertas como leídas

- Operation id  
markAllRead

#### Responses

- 204 No Content

***

### [POST]/api/admin/users/{id}/roles/{role}

- Summary  
Asigna un rol a un usuario

- Operation id  
assignRole

#### Responses

- 204 No Content

***

### [DELETE]/api/admin/users/{id}/roles/{role}

- Summary  
Quita un rol a un usuario

- Operation id  
removeRole

#### Responses

- 204 No Content

***

### [GET]/api/stock-movements/{id}

- Operation id  
getById_1

#### Responses

- 200 OK

`*/*`

```typescript
{
  id?: integer
  productId?: integer
  productName?: string
  productSku?: string
  movementType?: enum[IN, OUT, ADJUSTMENT]
  quantity?: integer
  previousQuantity?: integer
  newQuantity?: integer
  userId?: string
  observations?: string
  createdAt?: string
}
```

***

### [GET]/api/reports/top-products

- Summary  
Productos más movidos por salida

- Operation id  
topProducts

- Description  
Productos ordenados por unidades de salida (movimientos OUT) de mayor a menor. limit se acota al rango [1, 50] (por defecto 5).

#### Parameters(Query)

```typescript
limit?: integer //default: 5
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  productId?: integer
  productName?: string
  productSku?: string
  unitsOut?: integer
  movementCount?: integer
}[]
```

***

### [GET]/api/reports/summary

- Summary  
Resumen del inventario

- Operation id  
summary

- Description  
Totales de productos (por estado), unidades, valor del inventario, productos críticos (stock <= mínimo) y cantidad de movimientos.

#### Responses

- 200 OK

`*/*`

```typescript
{
  totalProducts?: integer
  activeProducts?: integer
  inactiveProducts?: integer
  totalUnits?: integer
  inventoryValue?: number
  criticalProducts?: integer
  totalMovements?: integer
}
```

***

### [GET]/api/reports/movements-by-type

- Summary  
Movimientos agrupados por tipo

- Operation id  
movementsByType

- Description  
Cantidad de movimientos y unidades por tipo. from/to son opcionales (fecha ISO yyyy-MM-dd) y filtran sobre createdAt; from > to responde 400.

#### Parameters(Query)

```typescript
from?: string
```

```typescript
to?: string
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  movementType?: enum[IN, OUT, ADJUSTMENT]
  movementCount?: integer
  totalUnits?: integer
}[]
```

***

### [GET]/api/reports/low-stock

- Summary  
Productos con stock bajo

- Operation id  
lowStock

- Description  
Productos cuya cantidad es menor o igual a su stock mínimo, ordenados por cantidad ascendente.

#### Responses

- 200 OK

`*/*`

```typescript
{
  productId?: integer
  name?: string
  sku?: string
  category?: string
  quantity?: integer
  minimumStock?: integer
  deficit?: integer
}[]
```

***

### [GET]/api/notifications

- Summary  
Lista las alertas de stock

- Operation id  
list_2

- Description  
Últimas 50 alertas ordenadas por fecha descendente más el contador global de no leídas. onlyUnread=true devuelve solo las pendientes. Las alertas son globales del inventario, no por usuario.

#### Parameters(Query)

```typescript
onlyUnread?: boolean
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  items: {
    id?: integer
    type?: enum[LOW_STOCK, OUT_OF_STOCK]
    productId?: integer
    productName?: string
    productSku?: string
    quantity?: integer
    minimumStock?: integer
    message?: string
    createdAt?: string
    read?: boolean
  }[]
  unreadCount?: integer
}
```

***

### [GET]/api/auth/me

- Operation id  
me

#### Responses

- 200 OK

`*/*`

```typescript
{
}
```

***

### [GET]/api/audit/products

- Summary  
Feed global de revisiones de productos

- Operation id  
productFeed

- Description  
Revisiones de todos los productos en orden descendente por número de revisión, paginadas. size se acota al rango [1, 50] (por defecto 10) y page es >= 0.

#### Parameters(Query)

```typescript
page?: integer
```

```typescript
size?: integer //default: 10
```

#### Responses

- 200 OK

`*/*`

```typescript
{
  totalElements?: integer
  totalPages?: integer
  size?: integer
  content: {
    revision?: integer
    revisionDate?: string
    username?: string
    revisionType?: string
    productId?: integer
    productName?: string
    productSku?: string
  }[]
  number?: integer
  first?: boolean
  last?: boolean
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  numberOfElements?: integer
  pageable: {
    offset?: integer
    sort:#/components/schemas/SortObject
    pageSize?: integer
    pageNumber?: integer
    paged?: boolean
    unpaged?: boolean
  }
  empty?: boolean
}
```

***

### [GET]/api/audit/products/{id}/revisions

- Summary  
Historial de revisiones de un producto

- Operation id  
productRevisions

- Description  
Lista todas las revisiones (CREATE, UPDATE, DELETE) de un producto en orden ascendente por número de revisión. Cada elemento incluye el snapshot del producto en esa revisión (en un DELETE trae el ultimo estado conocido, por store_data_at_delete). Responde 404 si el producto no tiene ninguna revisión.

#### Responses

- 200 OK

`*/*`

```typescript
{
  revision?: integer
  revisionDate?: string
  username?: string
  revisionType?: string
  product: {
    name?: string
    sku?: string
    description?: string
    category?: string
    price?: number
    quantity?: integer
    minimumStock?: integer
    status?: enum[ACTIVE, INACTIVE]
  }
}[]
```

***

### [GET]/api/admin/users

- Summary  
Lista los usuarios con sus roles

- Operation id  
listUsers

#### Responses

- 200 OK

`*/*`

```typescript
{
  id?: string
  username?: string
  email?: string
  enabled?: boolean
  realmRoles?: string[]
  effectiveRoles?: string[]
}[]
```

***

### [GET]/api/admin/users/{id}

- Summary  
Obtiene un usuario y sus roles

- Operation id  
getUser

#### Responses

- 200 OK

`*/*`

```typescript
{
  id?: string
  username?: string
  email?: string
  enabled?: boolean
  realmRoles?: string[]
  effectiveRoles?: string[]
}
```

***

### [GET]/api/admin/roles

- Summary  
Lista los roles asignables

- Operation id  
listRoles

#### Responses

- 200 OK

`*/*`

```typescript
{
  name?: string
  description?: string
}[]
```

## References

### #/components/schemas/ProductRequest

```typescript
{
  name: string
  sku: string
  description?: string
  category: string
  price: number
  quantity: integer
  minimumStock: integer
  status: enum[ACTIVE, INACTIVE]
}
```

### #/components/schemas/ProductResponse

```typescript
{
  id?: integer
  name?: string
  sku?: string
  description?: string
  category?: string
  price?: number
  quantity?: integer
  minimumStock?: integer
  status?: enum[ACTIVE, INACTIVE]
  createdAt?: string
  updatedAt?: string
}
```

### #/components/schemas/StockMovementRequest

```typescript
{
  productId: integer
  movementType: enum[IN, OUT, ADJUSTMENT]
  quantity: integer
  observations?: string
}
```

### #/components/schemas/StockMovementResponse

```typescript
{
  id?: integer
  productId?: integer
  productName?: string
  productSku?: string
  movementType?: enum[IN, OUT, ADJUSTMENT]
  quantity?: integer
  previousQuantity?: integer
  newQuantity?: integer
  userId?: string
  observations?: string
  createdAt?: string
}
```

### #/components/schemas/Pageable

```typescript
{
  page?: integer
  size?: integer
  sort?: string[]
}
```

### #/components/schemas/PageStockMovementResponse

```typescript
{
  totalElements?: integer
  totalPages?: integer
  size?: integer
  content: {
    id?: integer
    productId?: integer
    productName?: string
    productSku?: string
    movementType?: enum[IN, OUT, ADJUSTMENT]
    quantity?: integer
    previousQuantity?: integer
    newQuantity?: integer
    userId?: string
    observations?: string
    createdAt?: string
  }[]
  number?: integer
  first?: boolean
  last?: boolean
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  numberOfElements?: integer
  pageable: {
    offset?: integer
    sort:#/components/schemas/SortObject
    pageSize?: integer
    pageNumber?: integer
    paged?: boolean
    unpaged?: boolean
  }
  empty?: boolean
}
```

### #/components/schemas/PageableObject

```typescript
{
  offset?: integer
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  pageSize?: integer
  pageNumber?: integer
  paged?: boolean
  unpaged?: boolean
}
```

### #/components/schemas/SortObject

```typescript
{
  empty?: boolean
  sorted?: boolean
  unsorted?: boolean
}
```

### #/components/schemas/TopProductResponse

```typescript
{
  productId?: integer
  productName?: string
  productSku?: string
  unitsOut?: integer
  movementCount?: integer
}
```

### #/components/schemas/InventorySummaryResponse

```typescript
{
  totalProducts?: integer
  activeProducts?: integer
  inactiveProducts?: integer
  totalUnits?: integer
  inventoryValue?: number
  criticalProducts?: integer
  totalMovements?: integer
}
```

### #/components/schemas/MovementsByTypeResponse

```typescript
{
  movementType?: enum[IN, OUT, ADJUSTMENT]
  movementCount?: integer
  totalUnits?: integer
}
```

### #/components/schemas/LowStockProductResponse

```typescript
{
  productId?: integer
  name?: string
  sku?: string
  category?: string
  quantity?: integer
  minimumStock?: integer
  deficit?: integer
}
```

### #/components/schemas/PageProductResponse

```typescript
{
  totalElements?: integer
  totalPages?: integer
  size?: integer
  content: {
    id?: integer
    name?: string
    sku?: string
    description?: string
    category?: string
    price?: number
    quantity?: integer
    minimumStock?: integer
    status?: enum[ACTIVE, INACTIVE]
    createdAt?: string
    updatedAt?: string
  }[]
  number?: integer
  first?: boolean
  last?: boolean
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  numberOfElements?: integer
  pageable: {
    offset?: integer
    sort:#/components/schemas/SortObject
    pageSize?: integer
    pageNumber?: integer
    paged?: boolean
    unpaged?: boolean
  }
  empty?: boolean
}
```

### #/components/schemas/NotificationListResponse

```typescript
{
  items: {
    id?: integer
    type?: enum[LOW_STOCK, OUT_OF_STOCK]
    productId?: integer
    productName?: string
    productSku?: string
    quantity?: integer
    minimumStock?: integer
    message?: string
    createdAt?: string
    read?: boolean
  }[]
  unreadCount?: integer
}
```

### #/components/schemas/NotificationResponse

```typescript
{
  id?: integer
  type?: enum[LOW_STOCK, OUT_OF_STOCK]
  productId?: integer
  productName?: string
  productSku?: string
  quantity?: integer
  minimumStock?: integer
  message?: string
  createdAt?: string
  read?: boolean
}
```

### #/components/schemas/PageProductAuditFeedItem

```typescript
{
  totalElements?: integer
  totalPages?: integer
  size?: integer
  content: {
    revision?: integer
    revisionDate?: string
    username?: string
    revisionType?: string
    productId?: integer
    productName?: string
    productSku?: string
  }[]
  number?: integer
  first?: boolean
  last?: boolean
  sort: {
    empty?: boolean
    sorted?: boolean
    unsorted?: boolean
  }
  numberOfElements?: integer
  pageable: {
    offset?: integer
    sort:#/components/schemas/SortObject
    pageSize?: integer
    pageNumber?: integer
    paged?: boolean
    unpaged?: boolean
  }
  empty?: boolean
}
```

### #/components/schemas/ProductAuditFeedItem

```typescript
{
  revision?: integer
  revisionDate?: string
  username?: string
  revisionType?: string
  productId?: integer
  productName?: string
  productSku?: string
}
```

### #/components/schemas/ProductRevisionResponse

```typescript
{
  revision?: integer
  revisionDate?: string
  username?: string
  revisionType?: string
  product: {
    name?: string
    sku?: string
    description?: string
    category?: string
    price?: number
    quantity?: integer
    minimumStock?: integer
    status?: enum[ACTIVE, INACTIVE]
  }
}
```

### #/components/schemas/ProductSnapshot

```typescript
{
  name?: string
  sku?: string
  description?: string
  category?: string
  price?: number
  quantity?: integer
  minimumStock?: integer
  status?: enum[ACTIVE, INACTIVE]
}
```

### #/components/schemas/UserRolesView

```typescript
{
  id?: string
  username?: string
  email?: string
  enabled?: boolean
  realmRoles?: string[]
  effectiveRoles?: string[]
}
```

### #/components/schemas/RoleView

```typescript
{
  name?: string
  description?: string
}
```

### #/components/securitySchemes/bearer-jwt

```typescript
// Access token (JWT) emitido por Keycloak
http
```