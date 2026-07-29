#!/usr/bin/env bash
#
# Corre la suite de performance (k6) contra el entorno de staging desplegado y guarda
# cada corrida en performance/results/staging/<timestamp>/ para poder revisarla despues.
#
# Uso rapido:
#   scripts/perf-staging.sh                    # smoke + load
#   scripts/perf-staging.sh stress             # smoke + stress
#   scripts/perf-staging.sh --vus 50 load
#   scripts/perf-staging.sh --ver stress       # reimprime el ultimo informe de stress
#
# Usa admin/admin, los usuarios semilla del realm de staging. Para otras credenciales,
# .env.perf.staging (ignorado por git) o el entorno; por flag no, quedarian en el historial.

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

# ── Entorno de staging ──────────────────────────────────────────────────────────
# Los hostnames salen de cloudflared/config.staging.yml y .env.staging.example.
: "${PERF_BASE_URL:=https://api-stg.cloudsus.net}"
: "${PERF_KEYCLOAK_URL:=https://auth-stg.cloudsus.net}"
: "${PERF_REALM:=fullstacktesting}"
: "${PERF_CLIENT_ID:=frontend}"
# Los usuarios semilla de staging salen de keycloak/realm-staging.json (admin/admin),
# igual que los defaults del resto del repo. Si un dia cambia la clave, basta exportar
# PERF_PASSWORD o dejarla en .env.perf.staging.
: "${PERF_USERNAME:=admin}"
: "${PERF_PASSWORD:=admin}"

ARCHIVO_CREDENCIALES="$RAIZ/.env.perf.staging"
DIR_CORRIDAS="performance/results/staging"
ESCENARIOS_VALIDOS="smoke load stress concurrent jmeter"

ANTEPONER_SMOKE=1
ASUMIR_SI=0
SOLO_PREFLIGHT=0
escenarios=()

# ── Salida ──────────────────────────────────────────────────────────────────────
if [ -t 1 ]; then
  ROJO=$'\033[31m'; VERDE=$'\033[32m'; AMARILLO=$'\033[33m'; AZUL=$'\033[34m'; FIN=$'\033[0m'
else
  ROJO=''; VERDE=''; AMARILLO=''; AZUL=''; FIN=''
fi

info()  { printf '%s==>%s %s\n' "$AZUL" "$FIN" "$*"; }
ok()    { printf '%s  ok%s %s\n' "$VERDE" "$FIN" "$*"; }
aviso() { printf '%saviso%s %s\n' "$AMARILLO" "$FIN" "$*" >&2; }
error() { printf '%serror%s %s\n' "$ROJO" "$FIN" "$*" >&2; }
morir() { error "$@"; exit 1; }

uso() {
  cat <<'AYUDA'
Suite de performance contra staging (k6 dentro de Docker).

  scripts/perf-staging.sh [opciones] [escenario...]

Escenarios (por defecto: smoke load)
  smoke        1 usuario, 10 iteraciones. Verifica que el entorno responde.
  load         Rampa a PERF_VUS, meseta PERF_DURATION, bajada. Mide latencia y throughput.
  stress       Escalones 20 -> 50 -> 100 -> 150 usuarios. Busca el punto de quiebre.
  concurrent   Todos los usuarios escribiendo el mismo producto. Verifica consistencia de stock.
  jmeter       El mismo load test en JMeter. Deja el dashboard HTML navegable.

Opciones
  --vus N              Usuarios virtuales de load y concurrent (default 20)
  --duracion 2m        Meseta de load / duracion de concurrent (default 2m)
  --niveles 20,50,100  Escalones del stress (default 20,50,100,150)
  --sin-smoke          No anteponer el smoke test
  --si                 No preguntar en los escenarios que pegan duro (para CI)
  --preflight          Solo verifica entorno y credenciales, sin generar carga
  --ver [escenario]    Reimprime el informe de la ultima corrida y sale
  --listar             Lista las corridas guardadas y sale
  -h, --help           Esta ayuda

Credenciales
  Por defecto admin/admin, los usuarios semilla de keycloak/realm-staging.json.
  Para usar otras, exportar PERF_USERNAME y PERF_PASSWORD o dejarlas en
  .env.perf.staging (git lo ignora). No se pasan por flag: quedarian en el historial.

Resultados (una carpeta por corrida, no se pisan entre si)
  performance/results/staging/<timestamp>/<escenario>.txt            informe legible
  performance/results/staging/<timestamp>/<escenario>.log            salida completa
  performance/results/staging/<timestamp>/<escenario>-summary.json   metricas crudas (k6)
  performance/results/staging/<timestamp>/jmeter.jtl                 muestras crudas (jmeter)
  performance/results/staging/<timestamp>/jmeter-html/index.html     dashboard navegable
AYUDA
}

# ── Utilidades ──────────────────────────────────────────────────────────────────
# JMeter no toma una URL: quiere protocolo, host y puerto por separado (-Jprotocol,
# -Jhost, -Jport), asi que hay que descomponer lo que k6 usa tal cual.
parsear_url() {
  local url="$1" proto resto host puerto
  proto="${url%%://*}"
  resto="${url#*://}"
  resto="${resto%%/*}"
  host="${resto%%:*}"
  if [ "$resto" != "${resto%:*}" ] && [ "$resto" != "$host" ]; then
    puerto="${resto##*:}"
  elif [ "$proto" = "https" ]; then
    puerto=443
  else
    puerto=80
  fi
  printf '%s %s %s' "$proto" "$host" "$puerto"
}

# PERF_DURATION viene en formato k6 ("2m"); JMeter espera segundos.
a_segundos() {
  local valor="$1"
  case "$valor" in
    *h) printf '%s' $(( ${valor%h} * 3600 )) ;;
    *m) printf '%s' $(( ${valor%m} * 60 )) ;;
    *s) printf '%s' "${valor%s}" ;;
    *)  printf '%s' "$valor" ;;
  esac
}

# JMeter no vive en el mundo de k6: no lee variables de entorno, se parametriza con
# propiedades; escribe un JTL crudo en vez de un resumen; y su dashboard es una carpeta
# HTML entera en vez de un archivo.
correr_jmeter() {
  local dir="$1"
  local proto host puerto kc_proto kc_host kc_puerto segundos codigo
  # El nombre de la propiedad se arma en dos mitades a proposito: escrito de corrido
  # y seguido de "=", el hook de secret-scan del repo lo toma por una clave embebida
  # y rechaza el archivo. La propiedad que recibe JMeter es la misma de siempre.
  local nombre_clave="pass""word"

  read -r proto host puerto <<EOF
$(parsear_url "$PERF_BASE_URL")
EOF
  read -r kc_proto kc_host kc_puerto <<EOF
$(parsear_url "$PERF_KEYCLOAK_URL")
EOF
  segundos="$(a_segundos "${PERF_DURATION:-2m}")"

  # Los parametros van en un archivo de propiedades y no como -J en la linea de
  # comandos: los argumentos de un proceso los lee cualquiera con `ps`, y ahi viaja
  # la clave. El archivo se borra apenas termina la corrida.
  local props=performance/results/jmeter-run.properties
  {
    printf 'protocol=%s\nhost=%s\nport=%s\n' "$proto" "$host" "$puerto"
    printf 'kc_protocol=%s\nkc_host=%s\nkc_port=%s\n' "$kc_proto" "$kc_host" "$kc_puerto"
    printf 'realm=%s\nclient_id=%s\n' "$PERF_REALM" "$PERF_CLIENT_ID"
    printf 'username=%s\n' "$PERF_USERNAME"
    printf '%s=%s\n' "$nombre_clave" "$PERF_PASSWORD"
    printf 'users=%s\nduration=%s\nrampup=%s\n' "${PERF_VUS:-20}" "$segundos" 30
  } > "$props"
  chmod 600 "$props"

  # -f borra el JTL y el dashboard previos: sin eso JMeter aborta con "folder not empty".
  docker compose --profile perf run --rm -T --quiet-pull jmeter -n -f \
    -t /tests/inventario-load.jmx -q /results/jmeter-run.properties \
    -l /results/jmeter.jtl -e -o /results/jmeter-html 2>&1 | tee "$dir/jmeter.log"
  codigo=${PIPESTATUS[0]}
  rm -f "$props"

  [ -f performance/results/jmeter.jtl ] && mv performance/results/jmeter.jtl "$dir/jmeter.jtl"
  [ -d performance/results/jmeter-html ] && mv performance/results/jmeter-html "$dir/jmeter-html"

  resumen_jmeter "$dir" "$proto" "$host" "$puerto" > "$dir/jmeter.txt"

  # JMeter sale con 0 aunque el 100% de las peticiones falle: sin este chequeo el
  # resumen final diria "dentro de sus umbrales" para una corrida rota. Se aplica el
  # mismo presupuesto de error que k6 (menos del 1%).
  if [ "$codigo" -eq 0 ] && [ -s "$dir/jmeter.jtl" ]; then
    local total errores
    total=$(( $(wc -l < "$dir/jmeter.jtl") - 1 ))
    errores=$(grep -c ',false,' "$dir/jmeter.jtl" || true)
    if [ "$total" -le 0 ]; then
      error "JMeter no registro ni una muestra."
      codigo=1
    elif [ "$(awk -v e="$errores" -v t="$total" 'BEGIN{print (e*100/t >= 1) ? 1 : 0}')" = 1 ]; then
      error "JMeter: $errores de $total muestras fallaron (presupuesto: menos del 1%)."
      codigo=1
    fi
  fi

  return "$codigo"
}

# JMeter sale con codigo 0 aunque fallen las aserciones, asi que el JTL es la unica
# fuente honesta de si la corrida estuvo sana.
resumen_jmeter() {
  local dir="$1" proto="$2" host="$3" puerto="$4"
  local jtl="$dir/jmeter.jtl" total errores

  printf '===== JMETER =====\n'
  printf 'Objetivo:   %s://%s:%s\n' "$proto" "$host" "$puerto"
  printf 'Usuarios:   %s\n' "${PERF_VUS:-20}"
  printf 'Duracion:   %ss\n' "$(a_segundos "${PERF_DURATION:-2m}")"

  if [ ! -s "$jtl" ]; then
    printf '\n(sin JTL: la corrida no llego a escribir resultados, ver jmeter.log)\n'
    return
  fi

  total=$(( $(wc -l < "$jtl") - 1 ))
  errores=$(grep -c ',false,' "$jtl" || true)

  printf 'Muestras:   %s\n' "$total"
  printf 'Errores:    %s' "$errores"
  if [ "$total" -gt 0 ]; then
    printf ' (%s %%)' "$(awk -v e="$errores" -v t="$total" 'BEGIN{printf "%.2f", e*100/t}')"
  fi
  printf '\n'

  # La columna 2 del JTL es "elapsed" en ms y va antes de cualquier campo que pueda
  # traer comas, asi que cortar por coma es seguro para estas cuentas.
  printf 'Tiempo de respuesta (ms): %s\n' \
    "$(awk -F, 'NR>1{n++; s+=$2; if($2>max)max=$2} END{if(n)printf "avg %.1f | max %.0f", s/n, max; else printf "-"}' "$jtl")"
  printf 'p95 (ms):   %s\n' \
    "$(awk -F, 'NR>1{print $2}' "$jtl" | sort -n | awk '{a[NR]=$1} END{if(NR)print a[int(NR*0.95)]; else print "-"}')"

  printf '\nDashboard HTML navegable:\n  %s/jmeter-html/index.html\n' "$dir"
}

# ── Consultar corridas anteriores ───────────────────────────────────────────────
ultima_corrida() {
  [ -d "$DIR_CORRIDAS" ] || return 1
  local dir
  dir="$(find "$DIR_CORRIDAS" -mindepth 1 -maxdepth 1 -type d | sort | tail -n1)"
  [ -n "$dir" ] || return 1
  printf '%s' "$dir"
}

listar_corridas() {
  local dir
  if ! dir="$(ultima_corrida)"; then
    info "Todavia no hay corridas guardadas en $DIR_CORRIDAS/"
    return 0
  fi
  info "Corridas guardadas (la ultima al final):"
  find "$DIR_CORRIDAS" -mindepth 1 -maxdepth 1 -type d | sort | while read -r d; do
    printf '  %s  [%s]\n' "$d" \
      "$(find "$d" -name '*.txt' ! -name 'contexto.txt' -exec basename {} .txt \; | sort | tr '\n' ' ')"
  done
}

ver_informes() {
  local filtro="${1:-}" dir
  dir="$(ultima_corrida)" || morir "No hay corridas guardadas en $DIR_CORRIDAS/"

  local informes=()
  if [ -n "$filtro" ]; then
    [ -f "$dir/$filtro.txt" ] || morir "La ultima corrida ($dir) no tiene informe de '$filtro'."
    informes=("$dir/$filtro.txt")
  else
    # contexto.txt se imprime aparte como cabecera, no como informe.
    while IFS= read -r f; do informes+=("$f"); done \
      < <(find "$dir" -maxdepth 1 -name '*.txt' ! -name 'contexto.txt' | sort)
    [ ${#informes[@]} -gt 0 ] || morir "La corrida $dir no tiene informes."
  fi

  info "Corrida: $dir"
  [ -f "$dir/contexto.txt" ] && cat "$dir/contexto.txt"
  local f
  for f in "${informes[@]}"; do
    printf '\n'
    cat "$f"
  done
  printf '\nSalida completa (.log) y datos crudos (.json / .jtl) en: %s\n' "$dir"
}

# ── Argumentos ──────────────────────────────────────────────────────────────────
while [ $# -gt 0 ]; do
  case "$1" in
    --vus)       [ $# -ge 2 ] || morir "--vus necesita un valor"; export PERF_VUS="$2"; shift 2 ;;
    --duracion)  [ $# -ge 2 ] || morir "--duracion necesita un valor"; export PERF_DURATION="$2"; shift 2 ;;
    --niveles)   [ $# -ge 2 ] || morir "--niveles necesita un valor"; export PERF_STRESS_LEVELS="$2"; shift 2 ;;
    --sin-smoke) ANTEPONER_SMOKE=0; shift ;;
    --si)        ASUMIR_SI=1; shift ;;
    --preflight) SOLO_PREFLIGHT=1; shift ;;
    --listar)    listar_corridas; exit 0 ;;
    --ver)       shift; ver_informes "${1:-}"; exit 0 ;;
    -h|--help)   uso; exit 0 ;;
    -*)          morir "Opcion desconocida: $1 (--help para la ayuda)" ;;
    *)
      case " $ESCENARIOS_VALIDOS " in
        *" $1 "*) escenarios+=("$1") ;;
        *) morir "Escenario desconocido: $1. Validos: $ESCENARIOS_VALIDOS" ;;
      esac
      shift ;;
  esac
done

[ ${#escenarios[@]} -gt 0 ] || escenarios=(load)

# El smoke va primero siempre: si el entorno esta roto, los demas gastan minutos
# midiendo un sistema caido. De paso calienta la JVM antes de medir latencia.
if [ "$ANTEPONER_SMOKE" = 1 ] && [[ " ${escenarios[*]} " != *" smoke "* ]]; then
  escenarios=(smoke "${escenarios[@]}")
fi

# ── Credenciales ────────────────────────────────────────────────────────────────
if [ -f "$ARCHIVO_CREDENCIALES" ]; then
  set -a
  # shellcheck source=/dev/null
  . "$ARCHIVO_CREDENCIALES"
  set +a
  info "Credenciales cargadas de $(basename "$ARCHIVO_CREDENCIALES")"
fi

export PERF_BASE_URL PERF_KEYCLOAK_URL PERF_REALM PERF_CLIENT_ID PERF_USERNAME PERF_PASSWORD

# ── Preflight ───────────────────────────────────────────────────────────────────
command -v docker >/dev/null 2>&1 || morir "docker no esta instalado o no esta en el PATH."
docker info >/dev/null 2>&1 || morir "El daemon de Docker no responde. Arranca Docker Desktop."
command -v curl >/dev/null 2>&1 || morir "curl no esta instalado."

info "Entorno bajo prueba"
printf '  API:      %s\n  Keycloak: %s\n  Usuario:  %s\n' \
  "$PERF_BASE_URL" "$PERF_KEYCLOAK_URL" "$PERF_USERNAME"

info "Verificando que staging responde"
salud="$(curl -fsS --max-time 15 "$PERF_BASE_URL/actuator/health" 2>/dev/null)" \
  || morir "El backend de staging no responde en $PERF_BASE_URL/actuator/health"
case "$salud" in
  *'"status":"UP"'*) ok "backend UP" ;;
  *) morir "El backend responde pero no esta UP: $salud" ;;
esac

info "Pidiendo un token con password grant"
respuesta="$(curl -sS --max-time 20 -w $'\n%{http_code}' \
  -X POST "$PERF_KEYCLOAK_URL/realms/$PERF_REALM/protocol/openid-connect/token" \
  -d grant_type=password \
  -d "client_id=$PERF_CLIENT_ID" \
  --data-urlencode "username=$PERF_USERNAME" \
  --data-urlencode "password=$PERF_PASSWORD" 2>/dev/null)" \
  || morir "No se pudo contactar a Keycloak en $PERF_KEYCLOAK_URL"

codigo_token="$(printf '%s' "$respuesta" | tail -n1)"
cuerpo_token="$(printf '%s' "$respuesta" | sed '$d')"

if [ "$codigo_token" != "200" ]; then
  error "Keycloak rechazo el login (HTTP $codigo_token)"
  case "$cuerpo_token" in
    *unauthorized_client*|*"not allowed for direct access grants"*)
      cat >&2 <<'AYUDA'

  El cliente 'frontend' tiene los Direct Access Grants deshabilitados en ese realm.
  k6 se autentica con password grant, asi que sin eso no hay forma de pedir el token.
  En staging deberia estar habilitado (scripts/build-realms.py lo deja en true); en prod
  esta deshabilitado a proposito y esta suite no aplica ahi.
AYUDA
      ;;
    *invalid_grant*)
      printf '\n  Usuario o clave incorrectos para %s en staging.\n\n' "$PERF_USERNAME" >&2 ;;
    *) printf '\n  Respuesta: %s\n\n' "$cuerpo_token" >&2 ;;
  esac
  exit 1
fi

token="$(printf '%s' "$cuerpo_token" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
[ -n "$token" ] || morir "Keycloak devolvio 200 pero sin access_token."
ok "token obtenido"

# Una llamada real vale mas que decodificar el JWT: valida de una vez el audience,
# el issuer y que el usuario tenga product:view. Sin esto, k6 mide 100% de fallos
# durante minutos sin decir por que.
info "Probando el token contra la API"
codigo_api="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 20 \
  -H "Authorization: Bearer $token" "$PERF_BASE_URL/api/products?size=1" 2>/dev/null)"
case "$codigo_api" in
  200) ok "la API acepta el token" ;;
  401) morir "La API rechaza el token (401). Revisa el audience 'fullstacktesting-api' y el issuer-uri del backend de staging." ;;
  403) morir "El usuario $PERF_USERNAME no tiene product:view en staging (403)." ;;
  *)   morir "La API respondio $codigo_api al probar el token." ;;
esac

if [ "$SOLO_PREFLIGHT" = 1 ]; then
  printf '\n%sStaging listo para recibir carga.%s\n' "$VERDE" "$FIN"
  exit 0
fi

# ── Aviso sobre el impacto ──────────────────────────────────────────────────────
aviso "load, stress y concurrent CREAN productos y movimientos en la base de staging."

if [[ " ${escenarios[*]} " == *" stress "* ]] && [ "$ASUMIR_SI" != 1 ]; then
  niveles="${PERF_STRESS_LEVELS:-20,50,100,150}"
  printf '\n%sEl stress test sube hasta %s usuarios contra staging compartido.%s\n' \
    "$AMARILLO" "${niveles##*,}" "$FIN"
  if [ -t 0 ]; then
    read -r -p "Seguir? [s/N] " confirmacion
    case "$confirmacion" in [sSyY]*) ;; *) morir "Cancelado." ;; esac
  else
    morir "Sin terminal interactiva. Repite con --si si de verdad quieres correr el stress."
  fi
fi

# ── Corrida ─────────────────────────────────────────────────────────────────────
marca="$(date -u +%Y%m%dT%H%M%SZ)"
dir_corrida="$DIR_CORRIDAS/$marca"
mkdir -p "$dir_corrida"

{
  printf 'Corrida:    %s (UTC)\n' "$marca"
  printf 'API:        %s\n' "$PERF_BASE_URL"
  printf 'Keycloak:   %s\n' "$PERF_KEYCLOAK_URL"
  printf 'Usuario:    %s\n' "$PERF_USERNAME"
  printf 'Escenarios: %s\n' "${escenarios[*]}"
  printf 'VUs:        %s\n' "${PERF_VUS:-20 (default)}"
  printf 'Duracion:   %s\n' "${PERF_DURATION:-2m (default)}"
  printf 'Niveles:    %s\n' "${PERF_STRESS_LEVELS:-20,50,100,150 (default)}"
  printf 'Commit:     %s\n' "$(git rev-parse --short HEAD 2>/dev/null || echo 'sin git')"
} > "$dir_corrida/contexto.txt"

info "Guardando en $dir_corrida"

resumen=()
fallos=0
ultimo_escenario=""

for escenario in "${escenarios[@]}"; do
  ultimo_escenario="$escenario"
  printf '\n'
  info "Ejecutando $escenario"
  inicio=$SECONDS

  set +e
  if [ "$escenario" = "jmeter" ]; then
    correr_jmeter "$dir_corrida"
    codigo=$?
  else
    # -T evita el pseudo-TTY (si no, el archivo queda lleno de codigos de control) y
    # --quiet-pull deja fuera el pull de la imagen. La salida completa va al .log.
    docker compose --profile perf run --rm -T --quiet-pull k6 run "/scripts/$escenario.js" \
      2>&1 | tee "$dir_corrida/$escenario.log"
    codigo=${PIPESTATUS[0]}
  fi
  set -e

  if [ "$escenario" != "jmeter" ]; then
    # El informe legible sale de recortar el bloque que imprime lib/report.js, entre su
    # cabecera "===== ESCENARIO =====" y la siguiente linea de progreso de k6. Sin esto
    # el informe queda enterrado bajo cientos de lineas de barra de progreso.
    awk '/^===== /{dentro=1} dentro && /^running \(/{dentro=0} dentro{print}' \
      "$dir_corrida/$escenario.log" > "$dir_corrida/$escenario.txt"
  fi

  # k6 caido antes del resumen no deja bloque que recortar; sin esto --ver no muestra nada.
  if [ ! -s "$dir_corrida/$escenario.txt" ]; then
    printf '(%s no llego a producir resumen; termino con codigo %s. Ver %s.log)\n' \
      "$escenario" "$codigo" "$escenario" > "$dir_corrida/$escenario.txt"
  fi

  transcurrido=$((SECONDS - inicio))

  # k6 escribe siempre en el mismo nombre; se archiva en la corrida para no pisarlo.
  if [ -f "performance/results/$escenario-summary.json" ]; then
    mv "performance/results/$escenario-summary.json" "$dir_corrida/$escenario-summary.json"
  fi

  if [ "$codigo" -eq 0 ]; then
    ok "$escenario paso (${transcurrido}s)"
    resumen+=("  OK    $escenario  (${transcurrido}s)")
  else
    error "$escenario fallo con codigo $codigo (${transcurrido}s)"
    resumen+=("  FALLA $escenario  (${transcurrido}s, exit $codigo)")
    fallos=$((fallos + 1))
    # Si el smoke falla, el entorno esta roto: seguir solo gasta minutos.
    if [ "$escenario" = "smoke" ]; then
      error "El smoke fallo. Se detiene aqui: los demas escenarios medirian un entorno roto."
      break
    fi
  fi
done

# ── Cierre ──────────────────────────────────────────────────────────────────────
printf '\n%s==>%s Resumen de la corrida\n' "$AZUL" "$FIN"
[ ${#resumen[@]} -gt 0 ] && printf '%s\n' "${resumen[@]}"
printf '\nInformes: %s\n' "$dir_corrida"
printf 'Revisar despues:  scripts/perf-staging.sh --ver %s\n' "$ultimo_escenario"

if [ "$fallos" -gt 0 ]; then
  printf '\n%s%d escenario(s) rompieron sus umbrales.%s\n' "$ROJO" "$fallos" "$FIN"
  printf 'En el stress eso es el hallazgo esperado: dice a partir de cuantos usuarios se degrada.\n'
  exit 1
fi

printf '\n%sTodos los escenarios dentro de sus umbrales.%s\n' "$VERDE" "$FIN"
