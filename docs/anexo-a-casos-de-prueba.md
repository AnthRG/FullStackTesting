# Anexo A — Casos de prueba

Documento **generado**: no se edita a mano.

```bash
./gradlew test securityTest
npx playwright test --reporter=json > playwright-report/results.json
python3 scripts/docs/gen_test_catalog.py
```

Casos JUnit ejecutados: **352** · fallos: **0**. Los identificadores son estables y se referencian desde [01-requisitos.md](01-requisitos.md) y [05-guia-pruebas.md](05-guia-pruebas.md).

## Pruebas unitarias


### `ProductServiceTest` — prefijo `UT-PROD`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-PROD-01 | Create: con SKU nuevo: crea el producto | 1 | OK |
| UT-PROD-02 | Create: con SKU existente: lanza duplicate SKU exception | 1 | OK |
| UT-PROD-03 | Update: con datos validos: actualiza el producto | 1 | OK |
| UT-PROD-04 | Update: con SKU de otro producto: lanza duplicate SKU exception | 1 | OK |
| UT-PROD-05 | Update: con id inexistente: lanza product not found exception | 1 | OK |
| UT-PROD-06 | Find by id: con id existente: retorna producto | 1 | OK |
| UT-PROD-07 | Find by id: con id inexistente: lanza product not found exception | 1 | OK |
| UT-PROD-08 | Delete: con id existente: elimina el producto | 1 | OK |
| UT-PROD-09 | Delete: con id inexistente: lanza product not found exception | 1 | OK |
| UT-PROD-10 | List: sin filtros: retorna pagina de productos | 1 | OK |
| UT-PROD-11 | List: con search: retorna solo coincidencias | 1 | OK |
| UT-PROD-12 | List: con status: retorna solo ese status | 1 | OK |
| UT-PROD-13 | List: con search y status: combina ambos filtos | 1 | OK |
| UT-PROD-14 | List: sin resultados: retorna pagina vacia | 1 | OK |
| UT-PROD-15 | Create: mapea correctamente todos los campos | 1 | OK |
| UT-PROD-16 | Update: actualiza todos los campos del producto | 1 | OK |

### `StockMovementServiceTest` — prefijo `UT-MOV`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-MOV-01 | Register: con tipo IN: suma al stock y guarda el movimiento | 1 | OK |
| UT-MOV-02 | Register: con tipo OUT: resta al stock y guarda el movimiento | 1 | OK |
| UT-MOV-03 | Register: con tipo ADJUSTMENT: fija la cantidad exacta | 1 | OK |
| UT-MOV-04 | Register: con OUT y stock insuficiente: lanza insufficient stock exception | 1 | OK |
| UT-MOV-05 | Register: con seis unidades y salida de cuatro: deja dos | 1 | OK |
| UT-MOV-06 | Register: con salida exactamente igual al stock: lo deja en cero | 1 | OK |
| UT-MOV-07 | Register: con salida de una unidad mas que el stock: no se pasa a negativo | 1 | OK |
| UT-MOV-08 | Register: con salida sobre stock cero: no se pasa a negativo | 1 | OK |
| UT-MOV-09 | Register: con varias salidas seguidas: va descontando desde el stock vigente | 1 | OK |
| UT-MOV-10 | Register: con producto inexistente: lanza product not found exception | 1 | OK |
| UT-MOV-11 | Register: sin autenticacion: asigna user id system | 1 | OK |
| UT-MOV-12 | Register: con autenticacion sin nombre: asigna user id system | 1 | OK |
| UT-MOV-13 | Register: con usuario autenticado: asigna su nombre como user id | 1 | OK |
| UT-MOV-14 | Find by id: con id existente: retorna el movimiento | 1 | OK |
| UT-MOV-15 | Find by id: con id inexistente: lanza stock movement not found exception | 1 | OK |
| UT-MOV-16 | List: sin filtros: retorna pagina de movimientos | 1 | OK |
| UT-MOV-17 | List: con product id: filtra por producto | 1 | OK |
| UT-MOV-18 | List: con movement type: filtra por tipo | 1 | OK |
| UT-MOV-19 | List: con product id y movement type: combina ambos filtros | 1 | OK |
| UT-MOV-20 | List: sin resultados: retorna pagina vacia | 1 | OK |

### `NotificationServiceTest` — prefijo `UT-NOTI`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-NOTI-01 | Evaluate stock: cuando cruza el minimo: genera alerta low stock | 1 | OK |
| UT-NOTI-02 | Evaluate stock: cuando llega a cero desde stock sano: genera alerta out of stock | 1 | OK |
| UT-NOTI-03 | Evaluate stock: cuando llega a cero estando ya bajo minimo: genera una sola alerta out of stock | 1 | OK |
| UT-NOTI-04 | Evaluate stock: al crear producto ya bajo minimo: genera alerta | 1 | OK |
| UT-NOTI-05 | Evaluate stock: al crear producto sin stock: genera alerta out of stock | 1 | OK |
| UT-NOTI-06 | Evaluate stock: cuando el update sube el minimo por encima de la cantidad: genera alerta | 1 | OK |
| UT-NOTI-07 | Evaluate stock: cuando sigue sobre el minimo: no genera alerta | 1 | OK |
| UT-NOTI-08 | Evaluate stock: cuando ya estaba bajo minimo y baja mas: no genera alerta repetida | 1 | OK |
| UT-NOTI-09 | Evaluate stock: cuando ya estaba en cero y sigue en cero: no genera alerta repetida | 1 | OK |
| UT-NOTI-10 | Evaluate stock: cuando se recupera por encima del minimo: no genera alerta | 1 | OK |
| UT-NOTI-11 | Evaluate stock: al crear producto con stock sano: no genera alerta | 1 | OK |
| UT-NOTI-12 | Evaluate stock: publica el evento con el payload de la alerta | 1 | OK |
| UT-NOTI-13 | List: sin filtro: devuelve las ultimas y el contador de no leidas | 1 | OK |
| UT-NOTI-14 | List: con only unread: consulta solo las no leidas | 1 | OK |
| UT-NOTI-15 | List: pide como maximo 50 elementos | 1 | OK |
| UT-NOTI-16 | Mark read: con id existente: marca leida y sella la fecha | 1 | OK |
| UT-NOTI-17 | Mark read: con notificacion ya leida: no vuelve a guardar | 1 | OK |
| UT-NOTI-18 | Mark read: con id inexistente: lanza notification not found exception | 1 | OK |
| UT-NOTI-19 | Mark all read: marca todas las pendientes | 1 | OK |
| UT-NOTI-20 | Unread count: devuelve el contador del repositorio | 1 | OK |

### `ReportServiceTest` — prefijo `UT-REP`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-REP-01 | Summary: ensambla todos los campos desde los repositorios | 1 | OK |
| UT-REP-02 | Top products: con limit dentro del rango: lo usa tal cual | 1 | OK |
| UT-REP-03 | Top products: con limit menor que uno: lo acota a uno | 1 | OK |
| UT-REP-04 | Top products: con limit mayor que cincuenta: lo acota a cincuenta | 1 | OK |
| UT-REP-05 | Low stock: delega en el repositorio | 1 | OK |
| UT-REP-06 | Movements by type: con from mayor que to: lanza invalid date range exception | 1 | OK |
| UT-REP-07 | Movements by type: sin fechas: pasa nulls al repositorio | 1 | OK |
| UT-REP-08 | Movements by type: con rango valido: conviertelas fechas a inicio y fin de dia | 1 | OK |
| UT-REP-09 | Movements by type: con from igual a to: no lanza excepcion | 1 | OK |

### `GlobalExceptionHandlerTest` — prefijo `UT-EXC`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-EXC-01 | Handle not found: devuelve 404 con el mensaje de la excepcion | 1 | OK |
| UT-EXC-02 | Handle duplicate sku: devuelve 409 con el mensaje de la excepcion | 1 | OK |
| UT-EXC-03 | Handle movement not found: devuelve 404 con el mensaje de la excepcion | 1 | OK |
| UT-EXC-04 | Handle insufficient stock: devuelve 409 con el mensaje de la excepcion | 1 | OK |
| UT-EXC-05 | Handle invalid date range: devuelve 400 con el mensaje de la excepcion | 1 | OK |
| UT-EXC-06 | Handle validation errors: devuelve 400 con mapa de errores por campo | 1 | OK |

### `JwtAuthoritiesConverterTest` — prefijo `UT-JWT`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-JWT-01 | cada rol del realm produce una authority con su nombre tal cual, sin prefijo | 1 | OK |
| UT-JWT-02 | Spring Security anade FACTOR_BEARER por su cuenta, ademas de nuestros roles | 1 | OK |
| UT-JWT-03 | los permisos que Keycloak expande de un composite llegan como authorities propias | 1 | OK |
| UT-JWT-04 | un token sin el claim realm_access no otorga ninguna authority | 1 | OK |
| UT-JWT-05 | un realm_access sin la lista de roles no otorga ninguna authority | 1 | OK |
| UT-JWT-06 | el principal sale de preferred_username, no del sub | 1 | OK |

### `BearerSubprotocolHandshakeInterceptorTest` — prefijo `UT-WSH`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-WSH-01 | Before handshake: con token valido y permiso product view: acepta y guarda el usuario | 1 | OK |
| UT-WSH-02 | Before handshake: con el protocolo en dos headers separados: acepta igual | 1 | OK |
| UT-WSH-03 | Before handshake: sin header de subprotocolo: rechaza con 401 | 1 | OK |
| UT-WSH-04 | Before handshake: con subprotocolo sin token: rechaza con 401 | 1 | OK |
| UT-WSH-05 | Before handshake: con otro subprotocolo: rechaza con 401 | 1 | OK |
| UT-WSH-06 | Before handshake: con token invalido: rechaza con 401 | 1 | OK |
| UT-WSH-07 | Before handshake: con token valido sin el permiso: rechaza con 403 | 1 | OK |
| UT-WSH-08 | Before handshake: cuando el converter no devuelve autenticacion: rechaza con 403 | 1 | OK |
| UT-WSH-09 | After handshake: no hace nada ni falla | 1 | OK |

### `NotificationWebSocketHandlerTest` — prefijo `UT-WSHDL`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-WSHDL-01 | Broadcast: envia a todas las sesiones abiertas | 1 | OK |
| UT-WSHDL-02 | Broadcast: serializa el sobre con type y payload | 1 | OK |
| UT-WSHDL-03 | Broadcast: con ping: omite el payload nulo | 1 | OK |
| UT-WSHDL-04 | Broadcast: sin sesiones: no hace nada | 1 | OK |
| UT-WSHDL-05 | Broadcast: con sesion cerrada: la descarta y no le envia | 1 | OK |
| UT-WSHDL-06 | Broadcast: cuando una sesion falla: no propaga y sigue con las demas | 1 | OK |
| UT-WSHDL-07 | Broadcast: cuando la sesion ya no acepta envios: la descarta | 1 | OK |
| UT-WSHDL-08 | Broadcast: cuando la sesion se atasca: la descarta y sigue con las demas | 1 | OK |
| UT-WSHDL-09 | Broadcast: con un mensaje no serializable: no propaga ni envia | 1 | OK |
| UT-WSHDL-10 | After connection closed: remove la sesion | 1 | OK |
| UT-WSHDL-11 | Handle transport error: remove la sesion | 1 | OK |

### `NotificationBroadcasterTest` — prefijo `UT-WS-BC`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| UT-WS-BC-01 | On notification created: difunde el sobre de tipo notification | 1 | OK |
| UT-WS-BC-02 | Ping: difunde el keepalive | 1 | OK |

## Pruebas de integracion


### `ProductServiceIT` — prefijo `IT-PROD`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-PROD-01 | Create: persiste yse puede recuperar por id | 1 | OK |
| IT-PROD-02 | Create: con SKU duplicado: lanza duplicate SKU exception | 1 | OK |
| IT-PROD-03 | Update: modifica los campos persistidos | 1 | OK |
| IT-PROD-04 | Update: con SKU de otro producto: lanza duplicate SKU exception | 1 | OK |
| IT-PROD-05 | Delete: elimina el producto | 1 | OK |
| IT-PROD-06 | List: busca por texto ignorando mayusculas | 1 | OK |
| IT-PROD-07 | List: filtra por status y texto | 1 | OK |
| IT-PROD-08 | List: pagina los resultados | 1 | OK |

### `ReportServiceIT` — prefijo `IT-REP`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-REP-01 | Summary: agrega productos unidades valor y criticos | 1 | OK |
| IT-REP-02 | Top products: ordena por unidades out y respeta limit | 1 | OK |
| IT-REP-03 | Low stock: incluye igual excluye superiores y ordena ascendente | 1 | OK |
| IT-REP-04 | Movements by type: agrupa por tipo y respeta el rango de fechas | 1 | OK |
| IT-REP-05 | Movements by type: con from mayor que to: lanza invalid date range exception | 1 | OK |

### `NotificationIT` — prefijo `IT-NOTI`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-NOTI-01 | Registrar movimiento out que cruza el umbral: persiste la alerta no leida | 1 | OK |
| IT-NOTI-02 | Registrar movimiento out que no cruza el umbral: no genera alerta | 1 | OK |
| IT-NOTI-03 | Registrar movimiento que deja el stock en cero: genera alerta out of stock | 1 | OK |
| IT-NOTI-04 | Segundo movimiento estando ya bajo minimo: no genera alerta repetida | 1 | OK |
| IT-NOTI-05 | Crear producto ya bajo minimo: genera alerta | 1 | OK |
| IT-NOTI-06 | Actualizar producto subiendo el minimo: genera alerta | 1 | OK |
| IT-NOTI-07 | List: devuelve la alerta con sus datos y el contador de no leidas | 1 | OK |
| IT-NOTI-08 | Mark read: cambia el estado y baja el contador | 1 | OK |
| IT-NOTI-09 | Mark read: con id inexistente: lanza notification not found exception | 1 | OK |
| IT-NOTI-10 | Mark all read: deja el contador en cero | 1 | OK |

### `AuditServiceIT` — prefijo `IT-AUDSVC`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-AUDSVC-01 | Product revisions: devuelve create y update con snapshots en orden asc | 1 | OK |
| IT-AUDSVC-02 | Product feed: pagina yordena descendente por revision | 1 | OK |
| IT-AUDSVC-03 | Product feed: acota el tamano de pagina al rango | 1 | OK |
| IT-AUDSVC-04 | Product revisions: producto sin revisiones: lanza product not found exception | 1 | OK |

### `ProductAuditIT` — prefijo `IT-AUDIT`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-AUDIT-01 | Genera revisiones en create update y delete | 1 | OK |

### `KeycloakAdminClientIT` — prefijo `IT-KC`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-KC-01 | List users: devuelve los usuarios del realm | 1 | OK |
| IT-KC-02 | Get user: devuelve sus roles de realm | 1 | OK |
| IT-KC-03 | Assignable roles: excluye internos y devuelve los de negocio | 1 | OK |
| IT-KC-04 | Assign y remove role: se refleja en el usuario | 1 | OK |
| IT-KC-05 | Assign role: con rol inexistente: lanza 404 | 1 | OK |

### `AuthorizationIT` — prefijo `IT-AUTZ`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-AUTZ-01 | sin token, listar productos responde 401 | 1 | OK |
| IT-AUTZ-02 | sin token, la administracion de usuarios responde 401 | 1 | OK |
| IT-AUTZ-03 | user2 no tiene ningun permiso, asi que no ve productos | 1 | OK |
| IT-AUTZ-04 | user1 tiene product:view y lista productos | 1 | OK |
| IT-AUTZ-05 | user1 no tiene product:manage, asi que no puede crear productos | 1 | OK |
| IT-AUTZ-06 | user1 hereda stock:view de INVENTORY_VIEWER y ve los movimientos | 1 | OK |
| IT-AUTZ-07 | user1 no tiene stock:manage, asi que no puede registrar movimientos | 1 | OK |
| IT-AUTZ-08 | user1 hereda report:view y consulta el resumen | 1 | OK |
| IT-AUTZ-09 | user1 no tiene audit:view, asi que no consulta la auditoria | 1 | OK |
| IT-AUTZ-10 | operator tiene stock:manage y registra un movimiento | 1 | OK |
| IT-AUTZ-11 | operator no tiene audit:view, asi que no consulta la auditoria | 1 | OK |
| IT-AUTZ-12 | operator no tiene user:manage, asi que no administra usuarios | 1 | OK |
| IT-AUTZ-13 | auditor tiene audit:view y consulta la auditoria | 1 | OK |
| IT-AUTZ-14 | auditor no tiene product:manage, asi que no puede crear productos | 1 | OK |
| IT-AUTZ-15 | admin hereda user:manage de INVENTORY_ADMIN y administra usuarios | 1 | OK |

### `AuthEndpointIT` — prefijo `IT-AUTH`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-AUTH-01 | Me: sin token: devuelve 401 | 1 | OK |
| IT-AUTH-02 | Me: con token de admin: devuelve sus roles | 1 | OK |
| IT-AUTH-03 | Me: con token de user 1: devuelve solo sus roles | 1 | OK |

### `NotificationEndpointIT` — prefijo `IT-NEP`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-NEP-01 | Listar: sin token: devuelve 401 | 1 | OK |
| IT-NEP-02 | Listar: con usuario sin product view: devuelve 403 | 1 | OK |
| IT-NEP-03 | Listar: con product view: devuelve items y unread count | 1 | OK |
| IT-NEP-04 | Listar: con only unread: oculta las ya leidas | 1 | OK |
| IT-NEP-05 | Marcar leida: devuelve 204 y cambia el estado | 1 | OK |
| IT-NEP-06 | Marcar leida: con id inexistente: devuelve 404 | 1 | OK |
| IT-NEP-07 | Marcar todas leidas: devuelve 204 y deja el contador en cero | 1 | OK |

### `HealthEndpointIT` — prefijo `IT-HEALTH`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-HEALTH-01 | Health: responde up | 1 | OK |

### `NotificationWebSocketIT` — prefijo `IT-WS`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| IT-WS-01 | Con token valido: conecta y recibe la alerta al cruzar el umbral | 1 | OK |
| IT-WS-02 | Con token valido: no recibe nada si el movimiento no cruza el umbral | 1 | OK |
| IT-WS-03 | Con token invalido: rechaza el handshake | 1 | OK |
| IT-WS-04 | Sin subprotocolo bearer: rechaza el handshake | 1 | OK |
| IT-WS-05 | Con usuario sin product view: rechaza el handshake | 1 | OK |

## Pruebas de datos


### `SchemaIntegrityIT` — prefijo `DB-INT`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| DB-INT-01 | Flyway: aplico todas las migraciones sin fallos | 1 | OK |
| DB-INT-02 | Precio negativo: viola el check | 1 | OK |
| DB-INT-03 | Cantidad negativa: viola el check | 1 | OK |
| DB-INT-04 | Stock minimo negativo: viola el check | 1 | OK |
| DB-INT-05 | Status fuera del dominio: viola el check | 1 | OK |
| DB-INT-06 | SKU duplicado: viola la restriccion de unicidad | 1 | OK |
| DB-INT-07 | SKU de mas de 50 caracteres: no cabe en la columna | 1 | OK |
| DB-INT-08 | Movimiento de producto inexistente: viola la clave foranea | 1 | OK |
| DB-INT-09 | Borrar un producto con movimientos: viola la clave foranea | 1 | OK |
| DB-INT-10 | Tipo de movimiento invalido: viola el check | 1 | OK |
| DB-INT-11 | Cantidad de movimiento no positiva: viola el check | 1 | OK |
| DB-INT-12 | Nueva cantidad negativa: viola el check | 1 | OK |
| DB-INT-13 | Los indices de movimientos existen | 1 | OK |
| DB-INT-14 | La auditoria de envers apunta a su tabla de revisiones | 1 | OK |

### `DataConsistencyIT` — prefijo `DATA`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| DATA-01 | El stock refleja la suma algebraica de los movimientos | 1 | OK |
| DATA-02 | Un ajuste fija la cantidad exacta sin importar el historial | 1 | OK |
| DATA-03 | Los movimientos encadenan la cantidad anterior con la nueva | 1 | OK |
| DATA-04 | Una salida mayor al stock: se rechaza sin dejar rastro en la base | 1 | OK |
| DATA-05 | El resumen de inventario cuadra con lo que hay en las tablas | 1 | OK |
| DATA-06 | El valor del inventario coincide con precio por cantidad | 1 | OK |

## Pruebas de seguridad


### `AuthenticationFlowIT` — prefijo `SEC-AUTH`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| SEC-AUTH-01 | Credenciales correctas: emiten token del usuario | 1 | OK |
| SEC-AUTH-02 | Clave incorrecta: no emite token | 1 | OK |
| SEC-AUTH-03 | Usuario inexistente: responde igual que clave incorrecta | 1 | OK |
| SEC-AUTH-04 | Cliente desconocido: no emite token | 1 | OK |
| SEC-AUTH-05 | Usuario deshabilitado: no puede autenticarse | 1 | OK |
| SEC-AUTH-06 | Intentos fallidos seguidos: bloquean temporalmente la cuenta | 1 | OK |
| SEC-AUTH-07 | Respuesta de api con token invalido: no filtra detalles internos | 1 | OK |
| SEC-AUTH-08 | Health es publico pero sin detalles | 1 | OK |
| SEC-AUTH-09 | Actuator sensible no esta expuesto | parametrico | OK |

### `JwtValidationIT` — prefijo `SEC-JWT`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| SEC-JWT-01 | Token emitido por keycloak: es aceptado | 1 | OK |
| SEC-JWT-02 | Sin cabecera authorization: devuelve 401 | 1 | OK |
| SEC-JWT-03 | Cabecera authorization malformada: devuelve 401 | parametrico | OK |
| SEC-JWT-04 | Token con firma alterada: devuelve 401 | 1 | OK |
| SEC-JWT-05 | Token sin firma con alg none: devuelve 401 | 1 | OK |
| SEC-JWT-06 | Token firmado HS256 con la clave publica: devuelve 401 | 1 | OK |
| SEC-JWT-07 | Token firmado con otra llave: devuelve 401 | 1 | OK |
| SEC-JWT-08 | Token de otro realm: devuelve 401 | 1 | OK |
| SEC-JWT-09 | Token en query param: no autentica | 1 | OK |
| SEC-JWT-10 | Token de otro cliente del realm: devuelve 401 | 1 | OK |
| SEC-JWT-11 | Token expirado: devuelve 401 | 1 | OK |

### `AuthorizationMatrixIT` — prefijo `SEC-AUTZ`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| SEC-AUTZ-01 | Acceso segun permisos | parametrico | OK |
| SEC-AUTZ-02 | Sin token todo es 401 | parametrico | OK |
| SEC-AUTZ-03 | El realm otorga exactamente los permisos esperados | parametrico | OK |

### `CorsPolicyIT` — prefijo `SEC-CORS`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| SEC-CORS-01 | Preflight desde origen configurado: es aceptado | parametrico | OK |
| SEC-CORS-02 | Preflight no exige autenticacion | 1 | OK |
| SEC-CORS-03 | Preflight desde origen no configurado: es rechazado | parametrico | OK |
| SEC-CORS-04 | Preflight con metodo fuera de la lista: es rechazado | 1 | OK |
| SEC-CORS-05 | Peticion real desde origen ajeno: no recibe cabeceras CORS | 1 | OK |
| SEC-CORS-06 | Respuesta permitida: no habilita credenciales | 1 | OK |

### `SecurityHeadersIT` — prefijo `SEC-HDR`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| SEC-HDR-01 | Las rutas sin html usan la politica estricta | parametrico | OK |
| SEC-HDR-02 | Swagger ui recibe la politica relajada y una sola | 1 | OK |
| SEC-HDR-03 | La api trae el resto de cabeceras de endurecimiento | 1 | OK |

### `ZapPassiveScanIT` — prefijo `ZAP`

| ID | Caso | Ejec. | Resultado |
|---|---|---|---|
| ZAP-01 | Escaneo pasivo sobre trafico autenticado | — | flujo nocturno |

## Pruebas BDD de la API (Cucumber)


### `productos.feature` — API de productos — prefijo `BDD-PROD`

| ID | Escenario | Ejemplos | Resultado |
|---|---|---|---|
| BDD-PROD-01 | crear un producto valido devuelve el recurso creado | 1 | OK |
| BDD-PROD-02 | el SKU no se puede repetir | 1 | OK |
| BDD-PROD-03 | un producto sin nombre no se acepta | 1 | OK |
| BDD-PROD-04 | un producto con precio negativo no se acepta | 1 | OK |
| BDD-PROD-05 | consultar un producto que no existe devuelve 404 | 1 | OK |
| BDD-PROD-06 | actualizar un producto refleja el cambio | 1 | OK |
| BDD-PROD-07 | un producto eliminado deja de existir | 1 | OK |
| BDD-PROD-08 | el listado de productos viene paginado | 1 | OK |

### `movimientos.feature` — API de movimientos de stock — prefijo `BDD-MOV`

| ID | Escenario | Ejemplos | Resultado |
|---|---|---|---|
| BDD-MOV-01 | una entrada suma al stock del producto | 1 | OK |
| BDD-MOV-02 | una salida resta del stock del producto | 1 | OK |
| BDD-MOV-03 | no se puede sacar mas stock del disponible | 1 | OK |
| BDD-MOV-04 | quitar 4 de 6 unidades deja 2 en el producto | 1 | OK |
| BDD-MOV-05 | sacar todo el stock lo deja en cero, no en negativo | 1 | OK |
| BDD-MOV-06 | una salida de una unidad de mas no toca el stock | 1 | OK |
| BDD-MOV-07 | un ajuste fija la cantidad exacta sin importar la anterior | 1 | OK |
| BDD-MOV-08 | no se puede mover stock de un producto inexistente | 1 | OK |
| BDD-MOV-09 | el movimiento queda registrado a nombre de quien lo hizo | 1 | OK |
| BDD-MOV-10 | el historial de movimientos viene paginado | 1 | OK |

### `datos.feature` — Validacion de datos de la API — prefijo `BDD-DATOS`

| ID | Escenario | Ejemplos | Resultado |
|---|---|---|---|
| BDD-DATOS-01 | los valores limite validos se aceptan | 6 | OK |
| BDD-DATOS-02 | los valores fuera de rango se rechazan senalando el campo | 7 | OK |
| BDD-DATOS-03 | los campos obligatorios no admiten nulo ni blanco | 10 | OK |
| BDD-DATOS-04 | un estado fuera del dominio no se acepta | 1 | OK |
| BDD-DATOS-05 | el precio se devuelve con los dos decimales que admite la columna | 1 | OK |
| BDD-DATOS-06 | la cantidad de un movimiento tiene que ser positiva | 2 | OK |
| BDD-DATOS-07 | un rango de fechas invertido se rechaza | 1 | OK |

### `seguridad.feature` — codigos de estado de autorizacion — prefijo `BDD-SEG`

| ID | Escenario | Ejemplos | Resultado |
|---|---|---|---|
| BDD-SEG-01 | sin token la API responde 401 | 1 | OK |
| BDD-SEG-02 | un usuario autenticado sin permisos recibe 403 | 1 | OK |
| BDD-SEG-03 | un usuario de solo lectura no puede crear productos | 1 | OK |
| BDD-SEG-04 | un usuario de solo lectura si puede listarlos | 1 | OK |
| BDD-SEG-05 | el operador de inventario si puede registrar movimientos | 1 | OK |

### `contrato.feature` — contrato OpenAPI — prefijo `BDD-CONTRATO`

| ID | Escenario | Ejemplos | Resultado |
|---|---|---|---|
| BDD-CONTRATO-01 | el contrato se publica y es OpenAPI 3 | 1 | OK |
| BDD-CONTRATO-02 | el contrato declara los modulos del sistema | 1 | OK |
| BDD-CONTRATO-03 | el contrato declara las operaciones del CRUD de productos | 1 | OK |
| BDD-CONTRATO-04 | el contrato declara como autenticarse | 1 | OK |

## Pruebas E2E (Playwright)

| ID | Archivo | Suite | Escenario | Resultado |
|---|---|---|---|---|
| E2E-LOGIN-01 | `inventory.spec.ts` | Login | credenciales correctas redirigen al home y muestran bienvenida | OK |
| E2E-LOGIN-02 | `inventory.spec.ts` | Login | credenciales incorrectas se quedan en Keycloak con un mensaje de error | OK |
| E2E-CRUD-01 | `inventory.spec.ts` | CRUD de Productos | la página de productos carga la tabla de inventario | OK |
| E2E-CRUD-02 | `inventory.spec.ts` | CRUD de Productos | crear un nuevo producto | OK |
| E2E-CRUD-03 | `inventory.spec.ts` | CRUD de Productos | editar el producto creado | OK |
| E2E-CRUD-04 | `inventory.spec.ts` | CRUD de Productos | eliminar el producto editado | OK |
| E2E-STOCK-01 | `stock-movements.spec.ts` | Movimientos de stock desde Productos | quitarle 4 unidades a un producto con 6 lo deja en 2 | OK |
| E2E-STOCK-02 | `stock-movements.spec.ts` | Movimientos de stock desde Productos | el movimiento aparece en el historial con el stock anterior y el nuevo | OK |
| E2E-STOCK-03 | `stock-movements.spec.ts` | Movimientos de stock desde Productos | no deja sacar mas unidades de las que hay y el stock no se mueve | OK |
| E2E-STOCK-04 | `stock-movements.spec.ts` | Movimientos de stock desde Productos | una entrada suma al stock | OK |
| E2E-STOCK-05 | `stock-movements.spec.ts` | Movimientos de stock desde Productos | user1 sin stock:manage no ve el boton de movimiento | OK |
| E2E-MOV-01 | `movements.spec.ts` | Historial de movimientos | muestra la tabla con sus encabezados | OK |
| E2E-MOV-02 | `movements.spec.ts` | Historial de movimientos | el filtro por tipo deja solo movimientos de salida | OK |
| E2E-MOV-03 | `movements.spec.ts` | Historial de movimientos | el filtro por producto en la URL muestra un chip removible | OK |
| E2E-NOTI-01 | `notifications.spec.ts` | Alertas de stock | una salida que cruza el minimo llega al panel en vivo y navega al producto | OK |
| E2E-NOTI-02 | `notifications.spec.ts` | Alertas de stock | el panel abre y cierra sin interrumpir la navegacion | OK |
| E2E-REP-01 | `reports.spec.ts` | Reportes | muestra las stat cards del resumen con valores | OK |
| E2E-REP-02 | `reports.spec.ts` | Reportes | muestra las secciones de más vendidos, stock bajo y movimientos por tipo | OK |
| E2E-REP-03 | `reports.spec.ts` | Reportes | un rango de fechas invertido no llega al API: error inline o bloqueo nativo | OK |
| E2E-REP-04 | `reports.spec.ts` | Reportes | un rango válido muestra las 3 tarjetas de tipos con conteos | OK |
| E2E-AUDIT-01 | `audit.spec.ts` | Auditoría | el ciclo crear → editar queda visible en el historial y el feed | OK |
| E2E-USERS-01 | `users.spec.ts` | Usuarios y roles | como admin lista los usuarios del realm con sus roles | OK |
| E2E-USERS-02 | `users.spec.ts` | Usuarios y roles | asignar y quitar un rol a user2 deja el estado como estaba | OK |
| E2E-USERS-03 | `users.spec.ts` | Usuarios y roles | user2 sin VIEW_ROLES no ve el link ni la página | OK |
| E2E-DASH-01 | `dashboard.spec.ts` | Dashboard | muestra el saludo, las stat cards con numeros y los 3 widgets | OK |
| E2E-DASH-02 | `dashboard.spec.ts` | Dashboard | el link "Ver todo" navega a /movements | OK |
| E2E-DASH-03 | `dashboard.spec.ts` | Dashboard | el link "Ver reporte" navega a /reports | OK |
| E2E-LOGOUT-01 | `logout.spec.ts` | Cerrar sesión | el botón devuelve a /login y la sesión de Keycloak queda cerrada | OK |
