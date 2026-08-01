#!/usr/bin/env bash
# Regenera la documentacion derivada del sistema. Nada de esto se escribe a mano.
#
#   bash scripts/docs/generate.sh            # todo lo que se pueda
#   bash scripts/docs/generate.sh api        # solo la referencia de la API
#   bash scripts/docs/generate.sh db         # solo el esquema de base de datos
#   bash scripts/docs/generate.sh tests      # solo el catalogo de casos de prueba
#
# Requisitos por seccion:
#   api       backend en marcha (docker compose up -d) y npx
#   db        contenedor de base de datos en marcha y docker
#   tests     ./gradlew test securityTest ya ejecutados; opcionalmente playwright con --reporter=json
#   capturas  sistema completo en marcha y chromium instalado
#   reportes  ./gradlew test securityTest y npx playwright test ya ejecutados
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$RAIZ"

API_URL="${API_URL:-http://localhost:8080}"
SECCION="${1:-todo}"

api() {
  echo "==> Referencia de la API (OpenAPI -> Markdown)"
  local tmp; tmp="$(mktemp -d)"
  if ! curl -fsS "$API_URL/v3/api-docs" -o "$tmp/openapi.json"; then
    echo "    omitido: $API_URL/v3/api-docs no responde (levanta el backend)" >&2
    return 0
  fi
  npx --yes openapi-to-md "$tmp/openapi.json" "$tmp/api.md" >/dev/null
  {
    echo "# Anexo B — Referencia de la API"
    echo
    echo "Documento **generado** desde el contrato OpenAPI que publica la aplicación."
    echo "No se edita a mano."
    echo
    echo '```bash'
    echo "bash scripts/docs/generate.sh api"
    echo '```'
    echo
    echo "---"
    echo
    tail -n +2 "$tmp/api.md"
  } > docs/anexo-b-api.md
  rm -rf "$tmp"
  echo "    docs/anexo-b-api.md"
}

db() {
  echo "==> Esquema de base de datos (PostgreSQL -> Markdown + ER en Mermaid)"
  local contenedor red tmp
  contenedor="$(docker ps --filter 'name=db' --format '{{.Names}}' | grep -m1 'db' || true)"
  if [ -z "$contenedor" ]; then
    echo "    omitido: no hay contenedor de base de datos en marcha" >&2
    return 0
  fi
  red="$(docker inspect "$contenedor" --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')"
  tmp="$(mktemp -d)"
  docker run --rm --network "$red" -v "$tmp:/work" -w /work ghcr.io/k1low/tbls:latest \
    doc "postgres://${POSTGRES_USER:-postgres}:${POSTGRES_PASSWORD:-postgres}@db:5432/${POSTGRES_DB:-fullstacktesting}?sslmode=disable" \
    --er-format mermaid -f /work/dbdoc >/dev/null
  python3 scripts/docs/gen_db_schema.py "$tmp/dbdoc"
  rm -rf "$tmp"
  echo "    docs/anexo-c-esquema-bd.md"
}

tests() {
  echo "==> Catalogo de casos de prueba (JUnit + Cucumber + Playwright -> Markdown)"
  python3 scripts/docs/gen_test_catalog.py
}

reportes() {
  echo "==> Informes de pruebas (build -> docs/reportes)"
  local copiados=0
  copiar() {  # origen destino
    [ -e "$1" ] || { echo "    omitido: falta $1" >&2; return 0; }
    rm -rf "docs/reportes/$2"
    mkdir -p "$(dirname "docs/reportes/$2")"
    cp -R "$1" "docs/reportes/$2"
    copiados=$((copiados + 1))
  }
  copiar build/reports/tests/test          pruebas
  copiar build/reports/tests/securityTest  seguridad
  copiar build/reports/jacoco/test/html    cobertura
  copiar build/reports/cucumber.html       cucumber.html
  copiar playwright-report                 e2e
  echo "    docs/reportes/ ($copiados informes)"
}

capturas() {
  echo "==> Capturas de la interfaz (Playwright -> docs/capturas)"
  if ! curl -fsS "$API_URL/actuator/health" >/dev/null 2>&1; then
    echo "    omitido: el sistema no esta en marcha (docker compose up -d)" >&2
    return 0
  fi
  npx playwright test -c playwright.capturas.config.ts
  echo "    docs/capturas/*.png"
}

case "$SECCION" in
  api)      api ;;
  db)       db ;;
  tests)    tests ;;
  capturas) capturas ;;
  reportes) reportes ;;
  todo)     api; db; tests; capturas; reportes ;;
  *) echo "uso: $0 [api|db|tests|capturas|reportes|todo]" >&2; exit 2 ;;
esac
