import { sleep } from 'k6';
import { login } from './lib/auth.js';
import { browseInventory, writeInventory } from './lib/flows.js';
import { RELIABILITY_THRESHOLDS, RESPONSE_TIME_THRESHOLDS, TREND_STATS } from './lib/config.js';
import { summaryFor } from './lib/report.js';

// Smoke: un solo usuario, pocas iteraciones. No mide capacidad, verifica que el
// entorno responde y que los scripts estan bien antes de gastar minutos de carga.
export const options = {
  vus: 1,
  iterations: 10,
  summaryTrendStats: TREND_STATS,
  thresholds: {
    ...RESPONSE_TIME_THRESHOLDS,
    ...RELIABILITY_THRESHOLDS,
    http_req_failed: ['rate==0'],
  },
};

export function setup() {
  return { token: login() };
}

export default function (data) {
  browseInventory(data.token);
  writeInventory(data.token);
  sleep(1);
}

export function handleSummary(data) {
  return summaryFor('smoke', data);
}
