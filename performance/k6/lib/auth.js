import http from 'k6/http';
import { CLIENT_ID, ENDPOINT, KEYCLOAK_URL, PASSWORD, REALM, USERNAME } from './config.js';

// Password grant contra Keycloak, igual que e2e/helpers.ts y AbstractIntegrationTest.
// Se pide una sola vez en setup(): si cada iteracion pidiera token, la prueba mediria
// a Keycloak en vez de medir a la API.
export function login() {
  const res = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    { grant_type: 'password', client_id: CLIENT_ID, username: USERNAME, password: PASSWORD },
    { tags: { endpoint: ENDPOINT.TOKEN } },
  );

  if (res.status !== 200) {
    throw new Error(`No se pudo obtener el token (${res.status}): ${res.body}`);
  }
  return res.json('access_token');
}

export function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

export function request(token, tag, extra = {}) {
  const base = authHeaders(token);
  return {
    ...base,
    ...extra,
    headers: { ...base.headers, ...(extra.headers || {}) },
    tags: { endpoint: tag, ...(extra.tags || {}) },
  };
}
