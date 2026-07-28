import exec from 'k6/execution';
import { sleep } from 'k6';
import { login } from './lib/auth.js';
import { mixedTraffic } from './lib/flows.js';
import { RESPONSE_TIME_THRESHOLDS, TREND_STATS } from './lib/config.js';
import { summaryFor } from './lib/report.js';

// Stress test: escalones crecientes hasta encontrar el punto de quiebre. A diferencia
// del load test, aqui romper los umbrales es el resultado esperado; lo que interesa es
// en que escalon se rompen.
const LEVELS = (__ENV.PERF_STRESS_LEVELS || '20,50,100,150')
  .split(',')
  .map((value) => parseInt(value.trim(), 10));

const RAMP = __ENV.PERF_STRESS_RAMP || '30s';
const HOLD = __ENV.PERF_STRESS_HOLD || '1m';

const stages = [];
LEVELS.forEach((target) => {
  stages.push({ duration: RAMP, target });
  stages.push({ duration: HOLD, target });
});
stages.push({ duration: RAMP, target: 0 });

// k6 solo genera submetricas por tag cuando ese tag tiene un umbral declarado. Declarar
// uno por escalon es lo que permite leer en el resumen a partir de cuantos usuarios se
// degrada el tiempo de respuesta.
const perLevelThresholds = {};
LEVELS.forEach((level) => {
  perLevelThresholds[`http_req_duration{carga:${level}}`] = [
    { threshold: 'p(95)<500', abortOnFail: false },
  ];
  perLevelThresholds[`http_req_failed{carga:${level}}`] = [
    { threshold: 'rate<0.05', abortOnFail: false },
  ];
});

export const options = {
  stages,
  summaryTrendStats: TREND_STATS,
  thresholds: {
    ...RESPONSE_TIME_THRESHOLDS,
    ...perLevelThresholds,
  },
};

export function setup() {
  return { token: login() };
}

function currentLevel() {
  const active = exec.instance.vusActive;
  return LEVELS.find((level) => active <= level) || LEVELS[LEVELS.length - 1];
}

export default function (data) {
  // Etiquetar el VU marca todas las peticiones de esta iteracion con el escalon actual.
  exec.vu.tags.carga = String(currentLevel());
  mixedTraffic(data.token, 0.3);
  sleep(1);
}

export function handleSummary(data) {
  return summaryFor('stress', data);
}
