import { sleep } from 'k6';
import { login } from './lib/auth.js';
import { mixedTraffic } from './lib/flows.js';
import {
  DURATION, RELIABILITY_THRESHOLDS, RESPONSE_TIME_THRESHOLDS, TREND_STATS, VUS,
} from './lib/config.js';
import { summaryFor } from './lib/report.js';

// Load test: carga esperada en un dia normal. Sube gradualmente hasta VUS usuarios,
// los sostiene y baja. Responde "¿aguanta el sistema el trafico previsto cumpliendo
// los objetivos de tiempo de respuesta?".
export const options = {
  stages: [
    { duration: '30s', target: VUS },
    { duration: DURATION, target: VUS },
    { duration: '30s', target: 0 },
  ],
  summaryTrendStats: TREND_STATS,
  thresholds: {
    ...RESPONSE_TIME_THRESHOLDS,
    ...RELIABILITY_THRESHOLDS,
    // Throughput minimo esperado; si baja de aqui el sistema esta encolando peticiones.
    http_reqs: ['rate>20'],
  },
};

export function setup() {
  return { token: login() };
}

export default function (data) {
  mixedTraffic(data.token, 0.3);
  // Tiempo de lectura del usuario: sin pausa se mide un bombardeo, no un uso realista.
  sleep(Math.random() * 2 + 1);
}

export function handleSummary(data) {
  return summaryFor('load', data);
}
