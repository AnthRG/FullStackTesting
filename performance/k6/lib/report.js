import { RESULTS_DIR } from './config.js';

// Resumen propio en vez de la libreria remota k6-summary: importarla exigiria internet
// en cada corrida y esta suite tiene que funcionar igual en cualquier maquina.

const pad = (text, width) => String(text).padEnd(width);
const num = (value, decimals = 1) =>
  value === undefined || value === null ? '-' : Number(value).toFixed(decimals);
const pct = (rate) => (rate === undefined ? '-' : `${(rate * 100).toFixed(2)} %`);

function durationLine(values) {
  return `avg ${num(values.avg)} | med ${num(values.med)} | p95 ${num(values['p(95)'])}`
    + ` | p99 ${num(values['p(99)'])} | max ${num(values.max)}`;
}

// k6 solo crea submetricas por tag cuando ese tag tiene un umbral declarado; de ahi salen
// estas filas.
function tagBreakdown(metrics, tag, sortByValue = true) {
  const prefix = `http_req_duration{${tag}:`;
  const rows = Object.keys(metrics)
    .filter((name) => name.startsWith(prefix))
    .map((name) => ({
      label: name.slice(prefix.length, -1),
      values: metrics[name].values,
    }));

  if (rows.length === 0) return null;

  rows.sort((a, b) => (sortByValue
    ? b.values['p(95)'] - a.values['p(95)']
    : Number(a.label) - Number(b.label)));

  return rows
    .map((row) =>
      `  ${pad(row.label, 18)}n=${pad(num(row.values.count, 0), 9)}`
      + `avg ${pad(num(row.values.avg), 10)}p95 ${pad(num(row.values['p(95)']), 10)}`
      + `p99 ${num(row.values['p(99)'])}`,
    )
    .join('\n');
}

function thresholdBreakdown(metrics) {
  const lines = [];
  Object.keys(metrics).forEach((name) => {
    const thresholds = metrics[name].thresholds || {};
    Object.keys(thresholds).forEach((source) => {
      const passed = thresholds[source].ok !== undefined
        ? thresholds[source].ok
        : !thresholds[source].fails;
      lines.push(`  ${passed ? 'OK  ' : 'FALLA'} ${name} ${source}`);
    });
  });
  return lines.length > 0 ? lines.join('\n') : '  (sin umbrales declarados)';
}

function textReport(scenarioName, data) {
  const metrics = data.metrics;
  const duration = metrics.http_req_duration ? metrics.http_req_duration.values : {};
  const reqs = metrics.http_reqs ? metrics.http_reqs.values : {};
  const failed = metrics.http_req_failed ? metrics.http_req_failed.values : {};
  const checks = metrics.checks ? metrics.checks.values : {};
  const vus = metrics.vus_max ? metrics.vus_max.values : {};

  const porCarga = tagBreakdown(metrics, 'carga', false);
  const seccionCarga = porCarga
    ? ['', 'Por nivel de carga, usuarios simultaneos (ms):', porCarga]
    : [];

  return [
    '',
    `===== ${scenarioName.toUpperCase()} =====`,
    `Duracion real:      ${num(data.state.testRunDurationMs / 1000)} s`,
    `Usuarios maximos:   ${vus.max === undefined ? '-' : vus.max} VUs`,
    `Throughput:         ${num(reqs.rate, 2)} req/s  (${reqs.count || 0} peticiones)`,
    `Peticiones fallidas: ${pct(failed.rate)}`,
    `Checks en verde:    ${pct(checks.rate)}`,
    `Tiempo de respuesta (ms): ${durationLine(duration)}`,
    '',
    'Por endpoint (ms):',
    tagBreakdown(metrics, 'endpoint') || '  (sin desglose por endpoint)',
    ...seccionCarga,
    '',
    'Umbrales:',
    thresholdBreakdown(metrics),
    '',
  ].join('\n');
}

export function summaryFor(scenarioName, data) {
  const output = {
    stdout: textReport(scenarioName, data),
  };
  output[`${RESULTS_DIR}/${scenarioName}-summary.json`] = JSON.stringify(data, null, 2);
  return output;
}
