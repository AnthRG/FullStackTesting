// Configuracion compartida por todos los escenarios. Todo sale del entorno para poder
// apuntar la misma suite a local, a preview/staging o a produccion sin tocar los scripts.
//
// El prefijo es PERF_ y no K6_ a proposito: k6 reserva el espacio de nombres K6_ para su
// propia configuracion (K6_VUS y K6_DURATION son opciones nativas), y una variable con ese
// prefijo pisa silenciosamente el bloque `options` del script.

function envInt(name, fallback) {
  const parsed = parseInt(__ENV[name], 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export const BASE_URL = __ENV.PERF_BASE_URL || 'http://localhost:8080';
export const KEYCLOAK_URL = __ENV.PERF_KEYCLOAK_URL || 'http://localhost:8081';
export const REALM = __ENV.PERF_REALM || 'fullstacktesting';
export const CLIENT_ID = __ENV.PERF_CLIENT_ID || 'frontend';
export const USERNAME = __ENV.PERF_USERNAME || 'admin';
export const PASSWORD = __ENV.PERF_PASSWORD || 'admin';

export const VUS = envInt('PERF_VUS', 20);
export const DURATION = __ENV.PERF_DURATION || '2m';
export const RESULTS_DIR = __ENV.PERF_RESULTS_DIR || '/results';

// Etiquetas por endpoint: k6 desglosa cada metrica por tag, asi que con esto se obtiene
// el p95 de cada endpoint por separado sin declarar metricas custom.
export const ENDPOINT = {
  TOKEN: 'auth_token',
  LIST_PRODUCTS: 'list_products',
  GET_PRODUCT: 'get_product',
  REPORTS_SUMMARY: 'reports_summary',
  CREATE_PRODUCT: 'create_product',
  CREATE_MOVEMENT: 'create_movement',
  LIST_MOVEMENTS: 'list_movements',
};

// Por defecto k6 no calcula el p99 ni el conteo en el resumen; hay que pedirlos.
export const TREND_STATS = ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max', 'count'];

// Objetivos de tiempo de respuesta. Las lecturas se exigen mas que las escrituras,
// y /api/reports/summary agrega sobre toda la tabla, asi que se le da mas margen.
export const RESPONSE_TIME_THRESHOLDS = {
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  [`http_req_duration{endpoint:${ENDPOINT.LIST_PRODUCTS}}`]: ['p(95)<400'],
  [`http_req_duration{endpoint:${ENDPOINT.GET_PRODUCT}}`]: ['p(95)<300'],
  [`http_req_duration{endpoint:${ENDPOINT.REPORTS_SUMMARY}}`]: ['p(95)<800'],
  [`http_req_duration{endpoint:${ENDPOINT.CREATE_PRODUCT}}`]: ['p(95)<700'],
  [`http_req_duration{endpoint:${ENDPOINT.CREATE_MOVEMENT}}`]: ['p(95)<900'],
};

// Presupuesto de error: menos del 1% de peticiones fallidas y mas del 99% de checks en verde.
export const RELIABILITY_THRESHOLDS = {
  http_req_failed: ['rate<0.01'],
  checks: ['rate>0.99'],
};
