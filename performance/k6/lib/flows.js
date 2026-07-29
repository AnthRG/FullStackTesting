import http from 'k6/http';
import { check, group } from 'k6';
import { BASE_URL, ENDPOINT } from './config.js';
import { request } from './auth.js';
import { movementPayload, productPayload, randomPage } from './data.js';

// Recorridos de negocio reutilizados por smoke, load y stress. Tenerlos en un solo
// lugar hace que los tres escenarios midan exactamente el mismo trabajo y que la
// comparacion entre ellos tenga sentido.

export function browseInventory(token) {
  group('lectura de inventario', () => {
    const list = http.get(
      `${BASE_URL}/api/products?page=${randomPage()}&size=10`,
      request(token, ENDPOINT.LIST_PRODUCTS),
    );
    check(list, {
      'listado responde 200': (r) => r.status === 200,
      'listado viene paginado': (r) => r.json('content') !== undefined,
    });

    const summary = http.get(
      `${BASE_URL}/api/reports/summary`,
      request(token, ENDPOINT.REPORTS_SUMMARY),
    );
    check(summary, { 'resumen responde 200': (r) => r.status === 200 });

    const first = list.status === 200 ? list.json('content.0.id') : null;
    if (first) {
      const detail = http.get(
        `${BASE_URL}/api/products/${first}`,
        request(token, ENDPOINT.GET_PRODUCT),
      );
      check(detail, { 'detalle responde 200': (r) => r.status === 200 });
    }
  });
}

export function writeInventory(token) {
  let productId = null;

  group('alta de producto', () => {
    const created = http.post(
      `${BASE_URL}/api/products`,
      JSON.stringify(productPayload()),
      request(token, ENDPOINT.CREATE_PRODUCT),
    );
    check(created, { 'producto creado 201': (r) => r.status === 201 });
    if (created.status === 201) productId = created.json('id');
  });

  if (productId) {
    group('movimiento de stock', () => {
      const movement = http.post(
        `${BASE_URL}/api/stock-movements`,
        JSON.stringify(movementPayload(productId, 'IN', 5)),
        request(token, ENDPOINT.CREATE_MOVEMENT),
      );
      check(movement, { 'movimiento creado 201': (r) => r.status === 201 });
    });
  }

  return productId;
}

// Mezcla realista: la mayoria del trafico de un inventario es consulta, no escritura.
export function mixedTraffic(token, writeRatio = 0.3) {
  browseInventory(token);
  if (Math.random() < writeRatio) writeInventory(token);
}
