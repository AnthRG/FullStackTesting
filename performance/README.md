# Pruebas de performance (DEV-110)

Suite de carga del inventario. Cubre los cinco puntos exigidos: **load testing**,
**stress testing**, **concurrent users**, **tiempo de respuesta** y **throughput**.

La herramienta principal es **k6** (scripts en JavaScript, versionables). Se incluye además un
plan equivalente en **JMeter** para el load test.

---

## Requisitos

Solo Docker. Ni k6 ni JMeter ni Java hacen falta en la maquina: ambos corren como servicios
del `docker-compose.yml` bajo el perfil `perf`, dentro de la misma red que el backend. El
mismo comando funciona igual en Windows, macOS y Linux.

```bash
docker compose up -d db keycloak backend
```

---

## Escenarios de k6

| Escenario | Comando | Que responde |
|---|---|---|
| Smoke | `npm run perf:smoke` | ¿El entorno responde y los scripts estan bien? 1 usuario, 10 iteraciones. |
| Load | `npm run perf:load` | ¿Aguanta el trafico previsto cumpliendo los objetivos? Sube a 20 usuarios, los sostiene 2 min y baja. |
| Stress | `npm run perf:stress` | ¿Donde esta el punto de quiebre? Escalones de 20 → 50 → 100 → 150 usuarios. |
| Concurrent | `npm run perf:concurrent` | ¿La concurrencia corrompe el stock? 20 usuarios escribiendo sobre el mismo producto. |

Siempre correr **smoke primero**, por dos razones: si falla, los otros solo van a gastar
minutos midiendo un entorno roto; y de paso calienta el sistema.

### El arranque en frio y por que el smoke no mide latencia

La primera peticion contra un backend recien levantado (o que lleva horas sin trafico)
puede tardar segundos: la JVM todavia interpreta el bytecode en vez de compilarlo, Hibernate
inicializa sus metadatos y el pool de conexiones abre la primera conexion. Despues, esa misma
peticion baja a decenas de milisegundos.

Por eso el smoke **no** aplica los umbrales de tiempo de respuesta del resto de la suite: con
10 iteraciones, el p95 es practicamente "la peor peticion", y un unico pico de arranque en
frio la tumbaria sin que nada este mal. El smoke verifica disponibilidad y correctitud
(`http_req_failed` en 0 y `checks` al 100%), con un techo suelto de 5 s solo para detectar un
sistema colgado.

La latencia se mide en `load.js`, donde la rampa de subida calienta el sistema y hay miles de
muestras que hacen del p95 un numero estable. Si un dia el load test tambien sale raro,
correr el smoke antes y repetir.

### Que mide cada metrica

- `http_req_duration` → **tiempo de respuesta**. Se reporta avg, mediana, p95 y p99. El p95 es
  el numero que importa: "el 95% de los usuarios espero menos que esto". El promedio miente
  cuando hay picos.
- `http_reqs` (rate) → **throughput**, peticiones por segundo que el sistema sostuvo.
- `http_req_failed` → porcentaje de peticiones con error. Presupuesto: menos del 1%.
- `checks` → porcentaje de aserciones funcionales en verde (que el 200 traiga el cuerpo
  esperado, no solo que responda).
- `vus` → **usuarios concurrentes** activos.

Los objetivos viven en `k6/lib/config.js` como *thresholds*. Si uno se rompe, k6 termina con
codigo de salida distinto de cero: la prueba de carga **falla**, no solo informa.

### Leer el resultado

Cada corrida imprime un resumen y deja el JSON completo en `performance/results/`:

```
===== LOAD =====
Duracion real:      180.4 s
Usuarios maximos:   20 VUs
Throughput:         58.72 req/s  (10592 peticiones)
Peticiones fallidas: 0.09 %
Checks en verde:    99.95 %
Tiempo de respuesta (ms): avg 120.4 | med 98.0 | p95 380.2 | p99 720.1 | max 1802.0

Por endpoint (ms):
  reports_summary    n=3530    avg 210.7   p95 640.1   p99 810.0
  list_products      n=3530    avg 88.2    p95 240.5   p99 390.2
  ...

Umbrales:
  OK   http_req_duration p(95)<500
  OK   http_req_failed rate<0.01
```

En el **stress test** los umbrales por escalon (`http_req_duration{carga:100}`) son la parte
util: muestran a partir de cuantos usuarios se degrada el sistema. Que aparezcan en rojo ahi
es el hallazgo, no un fallo de la prueba.

En el **concurrent test**, el umbral que de verdad importa es `stock_consistente`: al terminar
compara el stock final del producto contra la cantidad de movimientos realmente guardados. Si
no cuadran, hubo una actualizacion perdida pese al lock pesimista de
`StockMovementService.register`.

---

## Ajustar la carga

Todo sale del entorno; los defaults estan en `docker-compose.yml` (servicio `k6`) y
documentados en `.env.example`. Para cambiarlos basta editar el `.env` de la raiz:

| Variable | Default | Que hace |
|---|---|---|
| `PERF_BASE_URL` | `http://backend:8080` | API bajo prueba |
| `PERF_KEYCLOAK_URL` | `http://keycloak:8080` | Emisor del token |
| `PERF_USERNAME` / `PERF_PASSWORD` | `admin` / `admin` | Usuario de carga |
| `PERF_VUS` | `20` | Usuarios virtuales en load y concurrent |
| `PERF_DURATION` | `2m` | Meseta del load / duracion del concurrent |
| `PERF_STRESS_LEVELS` | `20,50,100,150` | Escalones del stress |

> **Por que `PERF_` y no `K6_`.** k6 reserva el prefijo `K6_` para su propia
> configuracion: `K6_VUS` y `K6_DURATION` son opciones nativas que sobrescriben el bloque
> `options` del script **sin avisar**. Con ese prefijo, el smoke test (1 usuario, 10
> iteraciones) terminaba corriendo con 20 usuarios durante 2 minutos. La unica variable
> que conserva el prefijo original es `K6_PROMETHEUS_RW_SERVER_URL`, porque esa si es de k6.

Los nombres de servicio (`backend`, `keycloak`) funcionan porque k6 corre dentro de la red de
Compose. Por eso **no** se usa `host.docker.internal`, que se comporta distinto en cada SO.

### Correr contra preview/staging

Las pruebas de performance tienen sentido contra el sistema desplegado, no contra el build.
Basta apuntar las URLs al entorno:

```bash
PERF_BASE_URL=https://api.staging.ejemplo.com \
PERF_KEYCLOAK_URL=https://auth.staging.ejemplo.com \
PERF_USERNAME=carga PERF_PASSWORD=... \
docker compose --profile perf run --rm k6 run /scripts/load.js
```

En PowerShell la sintaxis de las variables cambia (`$env:PERF_BASE_URL="..."`), asi que lo
mas comodo en Windows es dejarlas en el `.env`.

---

## Metricas en vivo en Grafana (opcional)

Prometheus ya arranca con `--web.enable-remote-write-receiver`, asi que k6 puede escribirle
sus metricas mientras corre y se ven en Grafana en tiempo real:

```bash
docker compose --profile perf run --rm \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  k6 run -o experimental-prometheus-rw /scripts/load.js
```

Requiere el stack de observabilidad levantado (`docker compose up -d prometheus grafana`).
Las series aparecen con el prefijo `k6_`.

---

## JMeter

```bash
npm run perf:jmeter
```

Genera `performance/results/jmeter.jtl` (datos crudos) y
`performance/results/jmeter-html/index.html` (dashboard). El plan es autosuficiente: un
*setUp Thread Group* pide el token a Keycloak y lo publica como propiedad global, asi que no
hay que pegar ningun token a mano.

Se parametriza con propiedades `-J` (JMeter no lee variables de entorno):

```bash
docker compose --profile perf run --rm jmeter -n -f \
  -t /tests/inventario-load.jmx \
  -Jhost=api.staging.ejemplo.com -Jport=443 -Jusers=50 -Jduration=300 \
  -l /results/jmeter.jtl -e -o /results/jmeter-html
```

Para verlo en la GUI clasica, abrir `performance/jmeter/inventario-load.jmx` con JMeter
instalado. La GUI **solo** sirve para editar y depurar: medir con la GUI abierta falsea los
numeros porque el renderizado compite por CPU con los hilos de carga.

---

## Por que esto no corre en cada pull request

Un runner compartido de GitHub Actions no da latencias reproducibles: el mismo test puede
marcar 200 ms o 2 s segun con quien comparta la maquina. Poner un umbral de tiempo ahi produce
fallos aleatorios que la gente termina ignorando, que es peor que no tener la prueba.

Por eso `.github/workflows/performance.yml` es de ejecucion manual (`workflow_dispatch`) y
apunta al entorno preview/staging, que es donde el sistema esta desplegado de verdad.
