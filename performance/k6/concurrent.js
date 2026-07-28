import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
import { login, request } from './lib/auth.js';
import { movementPayload, productPayload } from './lib/data.js';
import { BASE_URL, DURATION, ENDPOINT, TREND_STATS, VUS } from './lib/config.js';
import { summaryFor } from './lib/report.js';

// Concurrent users: todos los usuarios virtuales escriben sobre EL MISMO producto.
// StockMovementService.register toma un lock pesimista (SELECT ... FOR UPDATE), asi que
// este escenario mide la contencion real de ese lock y, sobre todo, verifica que la
// concurrencia no corrompe el stock.
const stockConsistente = new Rate('stock_consistente');

export const options = {
  scenarios: {
    escrituras_simultaneas: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  summaryTrendStats: TREND_STATS,
  thresholds: {
    // La consistencia del stock es el criterio de exito: no admite fallos.
    stock_consistente: ['rate==1'],
    [`http_req_duration{endpoint:${ENDPOINT.CREATE_MOVEMENT}}`]: ['p(95)<1500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const token = login();
  const created = http.post(
    `${BASE_URL}/api/products`,
    JSON.stringify(productPayload({ name: 'Producto bajo concurrencia', quantity: 0 })),
    request(token, ENDPOINT.CREATE_PRODUCT),
  );

  if (created.status !== 201) {
    throw new Error(`No se pudo preparar el producto (${created.status}): ${created.body}`);
  }
  return { token, productId: created.json('id') };
}

export default function (data) {
  const res = http.post(
    `${BASE_URL}/api/stock-movements`,
    JSON.stringify(movementPayload(data.productId, 'IN', 1)),
    request(data.token, ENDPOINT.CREATE_MOVEMENT),
  );
  check(res, { 'movimiento aceptado 201': (r) => r.status === 201 });
}

export function teardown(data) {
  const product = http.get(
    `${BASE_URL}/api/products/${data.productId}`,
    request(data.token, ENDPOINT.GET_PRODUCT),
  );
  const movements = http.get(
    `${BASE_URL}/api/stock-movements?productId=${data.productId}&size=1`,
    request(data.token, ENDPOINT.LIST_MOVEMENTS),
  );
  check(product, { 'producto consultable al final': (r) => r.status === 200 });
  check(movements, { 'movimientos consultables al final': (r) => r.status === 200 });

  const cantidadFinal = product.json('quantity');
  const movimientosPersistidos = movements.json('totalElements');

  // Cada movimiento sumo exactamente 1 y el producto arranco en 0: si el stock final no
  // coincide con la cantidad de movimientos guardados, hubo una actualizacion perdida.
  const consistente = cantidadFinal === movimientosPersistidos;
  stockConsistente.add(consistente);

  console.log(
    `stock final=${cantidadFinal} movimientos=${movimientosPersistidos} `
    + `consistente=${consistente}`,
  );

  // Un umbral sobre una metrica sin muestras se reporta en verde, asi que si el teardown
  // no llegara a correr el fallo pasaria inadvertido. Lanzar aqui hace que k6 termine
  // con codigo de error de forma inequivoca.
  if (!consistente) {
    throw new Error(
      `Actualizacion perdida: el stock quedo en ${cantidadFinal} `
      + `pero hay ${movimientosPersistidos} movimientos registrados.`,
    );
  }
}

export function handleSummary(data) {
  return summaryFor('concurrent', data);
}
