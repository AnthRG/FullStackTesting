# Guía de instalación

## 1. Requisitos previos

| Herramienta | Versión | Necesaria para |
|---|---|---|
| Docker Engine + Compose v2 | 24+ | Todo (único requisito del camino recomendado) |
| JDK Temurin | 25 | Compilar o probar fuera de Docker |
| Node.js | 22+ | Desarrollo del frontend y pruebas E2E |

El stack completo con observabilidad usa ~4 GB de RAM; sin ella, ~1,5 GB.

## 2. Instalación con Docker

```bash
git clone https://github.com/AnthRG/FullStackTesting.git
cd FullStackTesting
cp .env.example .env
docker compose up -d
```

`.env.example` ya define `COMPOSE_FILE=docker-compose.yml:docker-compose.dev.yml`, por eso
`docker compose up -d` a secas levanta el entorno de desarrollo. El compose base no publica
puertos ni construye imágenes: siempre se usa con el override de un entorno.

Keycloak tarda 30-60 s en importar el realm la primera vez.

### Direcciones

| Servicio | URL | Credenciales |
|---|---|---|
| Aplicación web | http://localhost:5173 | ver usuarios de prueba |
| API | http://localhost:8080 | Bearer JWT |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| Keycloak | http://localhost:8081 | `admin` / `admin` |
| PostgreSQL | localhost:5432 | `postgres` / `postgres` |
| Grafana | http://localhost:3001 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | — |

### Usuarios de prueba

Sembrados en `keycloak/realm-export.json` (solo desarrollo y staging).

| Usuario | Contraseña | Rol | Para probar |
|---|---|---|---|
| `admin` | `admin` | `INVENTORY_ADMIN` | Todo, incluida la gestión de roles |
| `operator` | `operator` | `INVENTORY_OPERATOR` | Productos y movimientos |
| `auditor` | `auditor` | `AUDITOR` | Auditoría y reportes, sin modificar |
| `user1` | `user1` | `INVENTORY_VIEWER` | Solo lectura |
| `user2` | `user2` | sin roles | Respuestas 403 |

### Perfiles opcionales

```bash
COMPOSE_PROFILES=observability docker compose up -d   # + Prometheus, Loki, Tempo, Alloy, Grafana
docker compose up -d db keycloak                      # solo infraestructura
docker compose --profile perf run --rm k6 run /scripts/smoke.js
```

## 3. Desarrollo con recarga en caliente

```bash
docker compose up -d db keycloak      # infraestructura
./gradlew bootRun                     # backend en :8080
cd frontend && npm install && npm run dev   # frontend en :5173
```

Si PostgreSQL quedó publicado en otro puerto:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5440/fullstacktesting ./gradlew bootRun
```

## 4. Configuración

Variables principales del backend (todas con valor por defecto para desarrollo):

| Variable | Por defecto | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev`, `staging` o `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/fullstacktesting` | Cadena JDBC |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Orígenes permitidos |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8081/realms/fullstacktesting` | Debe coincidir con el claim `iss` (URL **pública**) |
| `KEYCLOAK_JWK_SET_URI` | `.../protocol/openid-connect/certs` | Descarga de llaves (red **interna** en Compose) |
| `JWT_EXPECTED_AUDIENCE` | `fullstacktesting-api` | Audiencia obligatoria del token |
| `KEYCLOAK_ADMIN_SERVER_URL` | `http://localhost:8081` | Base de la Admin API, sin `/realms` |
| `METRICS_NAMESPACE` | `local` | Etiqueta de las métricas |

> `issuer-uri` usa la URL **pública** de Keycloak; `jwk-set-uri` y las de administración, la
> **interna** de Compose. Mezclarlas produce 401 en todas las llamadas.

El frontend recibe su configuración en tiempo de arranque: el `entrypoint.sh` del contenedor
escribe `/config.js` a partir de `PUBLIC_API_URL`, `PUBLIC_KEYCLOAK_URL`,
`PUBLIC_KEYCLOAK_REALM` y `PUBLIC_KEYCLOAK_CLIENT_ID`. Para `npm run dev` manda
`frontend/.env` con las variables `VITE_*`. Así una sola imagen sirve para todos los entornos.

| Perfil | Swagger | Flyway clean | Errores | Actuator |
|---|---|---|---|---|
| `dev` | Sí | Permitido | Completos | health, info, prometheus |
| `staging` | Sí | Deshabilitado | Sin detalle | health, info, prometheus |
| `prod` | No | Deshabilitado | Sin detalle | health, prometheus |

## 5. Base de datos

El esquema lo crea Flyway al arrancar (`V1`…`V5`); no hay que ejecutar SQL a mano. Con
`ddl-auto=validate`, si el esquema no coincide con las entidades la aplicación no levanta.

**Datos de demostración.** En el perfil `dev` se carga además `db/seed/R__seed_demo_data.sql`,
una migración repetible e idempotente con diez productos que cubren los cuatro estados de
interés —stock sano, crítico, sin stock e inactivo— y su historial de movimientos. Se activa
por `spring.flyway.locations` en `application-dev.properties`, así que las pruebas (perfil
`test`) y producción (perfil `prod`) nunca la ven. Para cargarla en otro entorno basta añadir
`classpath:db/seed` a esa propiedad.

```bash
docker compose exec db psql -U postgres -d fullstacktesting \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## 6. Despliegue a staging y producción

Un `push` a `staging` o `prod` dispara el workflow **CD**, que construye las imágenes (o
reusa las existentes si el árbol no cambió), las publica en GHCR y ejecuta el despliegue por
SSH. La promoción entre entornos es *fast-forward*, de modo que producción despliega la misma
imagen que staging validó.

Preparación del servidor (una sola vez):

```bash
curl -fsSL https://get.docker.com | sh
sudo mkdir -p /srv/fullstacktesting && sudo chown "$USER" /srv/fullstacktesting
cp .env.staging.example /srv/fullstacktesting/.env   # rellenar los CAMBIAME
chmod 600 /srv/fullstacktesting/.env
cp credentials.json /srv/fullstacktesting/cloudflared/credentials.json
```

Secretos necesarios en GitHub: `SSH_HOST`, `SSH_USER`, `SSH_KEY` y, opcionalmente,
`NVD_API_KEY`, `PERF_USERNAME`, `PERF_PASSWORD`.

Despliegue manual o reversión:

```bash
cd /srv/fullstacktesting
sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=<tag>|" .env
docker compose -f docker-compose.yml -f docker-compose.deploy.yml pull
docker compose -f docker-compose.yml -f docker-compose.deploy.yml up -d --wait
```

`--wait` hace que el despliegue falle si algún contenedor no queda sano, en vez de terminar
en verde con el sistema caído.

## 7. Verificación posterior

```bash
docker compose ps                                       # todo "healthy"
curl -fsS http://localhost:8080/actuator/health         # {"status":"UP"}
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products   # 401 sin token

TOKEN=$(curl -s -X POST "http://localhost:8081/realms/fullstacktesting/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=frontend&username=admin&password=admin" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/products                    # 200 con token
```

El último paso detecta el fallo más confuso: si al cliente `frontend` del realm le falta el
mapper de audiencia, Keycloak emite el token pero la API responde 401 a todo.

## 8. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| 401 con un token recién emitido | Falta el mapper de audiencia o `JWT_EXPECTED_AUDIENCE` no coincide | Revisar el mapper `audience-fullstacktesting-api` |
| 401 por emisor inválido | `KEYCLOAK_ISSUER_URI` no coincide con el `iss` del token | Usar la URL pública |
| El login vuelve a `/login` | `redirectUris` o `webOrigins` no incluyen el origen | Ajustar el cliente en Keycloak |
| Error de CORS | Origen no listado | Agregarlo a `CORS_ALLOWED_ORIGINS` |
| *Schema-validation* al arrancar | Falta una migración | Escribir la migración; en desarrollo, `docker compose down -v` |
| `port is already allocated` | Puerto ocupado | Cambiar `POSTGRES_PORT`, `BACKEND_PORT` o `FRONTEND_PORT` en `.env` |
| Las pruebas de integración no arrancan | Docker apagado (Testcontainers) | Iniciar Docker |
| Swagger da 404 en producción | Comportamiento esperado | `springdoc` está deshabilitado en `prod` |

## 9. Desinstalación

```bash
docker compose down      # conserva los datos
docker compose down -v   # borra también la base de datos
```
