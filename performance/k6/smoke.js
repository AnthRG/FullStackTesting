import { sleep } from 'k6';
import { login } from './lib/auth.js';
import { browseInventory, writeInventory } from './lib/flows.js';
import { RELIABILITY_THRESHOLDS, TREND_STATS } from './lib/config.js';
import { summaryFor } from './lib/report.js';

// Smoke: un solo usuario, pocas iteraciones. Verifica que el entorno responde y que los
// scripts estan bien antes de gastar minutos de carga.
//
// A proposito NO usa los umbrales de tiempo de respuesta del resto de la suite: con 10
// iteraciones el p95 es casi "la peor peticion", y la primera llamada a cada endpoint
// paga el arranque en frio (JIT de la JVM, inicializacion de Hibernate, creacion del pool
// de conexiones). Un solo pico de 4 s tumbaria la prueba sin que nada este mal. La
// latencia se mide en load.js, con miles de muestras y una rampa que calienta el sistema.
// Aqui el unico limite es un techo generoso para detectar un sistema colgado.
export const options = {
  vus: 1,
  iterations: 10,
  summaryTrendStats: TREND_STATS,
  thresholds: {
    ...RELIABILITY_THRESHOLDS,
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<5000'],
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
