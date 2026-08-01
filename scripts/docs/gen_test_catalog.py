#!/usr/bin/env python3
"""Genera docs/anexo-a-casos-de-prueba.md a partir de los resultados reales de las suites.

Fuentes:
  build/test-results/test/*.xml          JUnit (unitarias, integracion) y Cucumber
  build/test-results/securityTest/*.xml  JUnit (seguridad)
  src/test/resources/features/*.feature  escenarios BDD, para el orden y el nombre
  src/test/java/**/*.java                orden de los casos tal como estan en el codigo
  playwright-report/results.json         resultados E2E (opcional)

Uso:
  ./gradlew test securityTest
  npx playwright test --reporter=json > playwright-report/results.json
  python3 scripts/docs/gen_test_catalog.py
"""
import glob
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(RAIZ, 'src/test/java/pucmm/freddy/fullstacktesting')
DESTINO = os.path.join(RAIZ, 'docs/anexo-a-casos-de-prueba.md')

# Clase de prueba -> prefijo del identificador del caso
PREFIJOS = {
    'ProductServiceTest': 'UT-PROD', 'StockMovementServiceTest': 'UT-MOV',
    'NotificationServiceTest': 'UT-NOTI', 'ReportServiceTest': 'UT-REP',
    'GlobalExceptionHandlerTest': 'UT-EXC', 'JwtAuthoritiesConverterTest': 'UT-JWT',
    'BearerSubprotocolHandshakeInterceptorTest': 'UT-WSH',
    'NotificationWebSocketHandlerTest': 'UT-WSHDL', 'NotificationBroadcasterTest': 'UT-WS-BC',
    'ProductServiceIT': 'IT-PROD', 'ReportServiceIT': 'IT-REP', 'NotificationIT': 'IT-NOTI',
    'AuditServiceIT': 'IT-AUDSVC', 'ProductAuditIT': 'IT-AUDIT',
    'KeycloakAdminClientIT': 'IT-KC', 'AuthorizationIT': 'IT-AUTZ', 'AuthEndpointIT': 'IT-AUTH',
    'NotificationEndpointIT': 'IT-NEP', 'HealthEndpointIT': 'IT-HEALTH',
    'NotificationWebSocketIT': 'IT-WS', 'SchemaIntegrityIT': 'DB-INT', 'DataConsistencyIT': 'DATA',
    'JwtValidationIT': 'SEC-JWT', 'AuthorizationMatrixIT': 'SEC-AUTZ', 'CorsPolicyIT': 'SEC-CORS',
    'SecurityHeadersIT': 'SEC-HDR', 'AuthenticationFlowIT': 'SEC-AUTH', 'ZapPassiveScanIT': 'ZAP',
}

SECCIONES = [
    ('Pruebas unitarias', ['ProductServiceTest', 'StockMovementServiceTest',
                           'NotificationServiceTest', 'ReportServiceTest',
                           'GlobalExceptionHandlerTest', 'JwtAuthoritiesConverterTest',
                           'BearerSubprotocolHandshakeInterceptorTest',
                           'NotificationWebSocketHandlerTest', 'NotificationBroadcasterTest']),
    ('Pruebas de integracion', ['ProductServiceIT', 'ReportServiceIT', 'NotificationIT',
                                'AuditServiceIT', 'ProductAuditIT', 'KeycloakAdminClientIT',
                                'AuthorizationIT', 'AuthEndpointIT', 'NotificationEndpointIT',
                                'HealthEndpointIT', 'NotificationWebSocketIT']),
    ('Pruebas de datos', ['SchemaIntegrityIT', 'DataConsistencyIT']),
    ('Pruebas de seguridad', ['AuthenticationFlowIT', 'JwtValidationIT', 'AuthorizationMatrixIT',
                              'CorsPolicyIT', 'SecurityHeadersIT', 'ZapPassiveScanIT']),
]

FEATURES = [('productos', 'BDD-PROD'), ('movimientos', 'BDD-MOV'), ('datos', 'BDD-DATOS'),
            ('seguridad', 'BDD-SEG'), ('contrato', 'BDD-CONTRATO')]

# Suites que por diseno no corren en el porton de cada PR (tarea Gradle aparte).
FUERA_DEL_PORTON = {'ZapPassiveScanIT'}

E2E_PREFIJOS = {
    ('inventory.spec.ts', 'Login'): 'E2E-LOGIN',
    ('inventory.spec.ts', 'CRUD de Productos'): 'E2E-CRUD',
    ('stock-movements.spec.ts', 'Movimientos de stock desde Productos'): 'E2E-STOCK',
    ('movements.spec.ts', 'Historial de movimientos'): 'E2E-MOV',
    ('notifications.spec.ts', 'Alertas de stock'): 'E2E-NOTI',
    ('reports.spec.ts', 'Reportes'): 'E2E-REP',
    ('audit.spec.ts', 'Auditoría'): 'E2E-AUDIT',
    ('users.spec.ts', 'Usuarios y roles'): 'E2E-USERS',
    ('dashboard.spec.ts', 'Dashboard'): 'E2E-DASH',
    ('logout.spec.ts', 'Cerrar sesión'): 'E2E-LOGOUT',
}
E2E_ORDEN = ['E2E-LOGIN', 'E2E-CRUD', 'E2E-STOCK', 'E2E-MOV', 'E2E-NOTI', 'E2E-REP',
             'E2E-AUDIT', 'E2E-USERS', 'E2E-DASH', 'E2E-LOGOUT']


def orden_en_codigo(ruta):
    """Casos de una clase en el orden en que estan escritos, con su @DisplayName."""
    lineas = open(ruta, encoding='utf-8').read().splitlines()
    casos = []
    for i, linea in enumerate(lineas):
        if not linea.strip().startswith(('@Test', '@ParameterizedTest', '@RepeatedTest')):
            continue
        titulo = nombre = None
        for j in range(i + 1, min(i + 10, len(lineas))):
            actual = lineas[j].strip()
            visible = re.match(r'@DisplayName\("(.*)"\)', actual)
            if visible:
                titulo = visible.group(1)
                continue
            firma = re.match(r'(?:public |private |protected )?(?:void|\S+) (\w+)\(', actual)
            if firma and not actual.startswith('@'):
                nombre = firma.group(1)
                break
        if nombre:
            casos.append((nombre, titulo))
    return casos


def resultados_junit():
    """clase -> (metodo o displayName) -> (ejecuciones, fallos)."""
    datos = defaultdict(dict)
    for patron in ('build/test-results/test/*.xml', 'build/test-results/securityTest/*.xml'):
        for ruta in glob.glob(os.path.join(RAIZ, patron)):
            raiz = ET.parse(ruta).getroot()
            clase = raiz.get('name').split('.')[-1]
            for caso in raiz.findall('testcase'):
                metodo = re.split(r'[(\[]', caso.get('name'))[0].strip()
                malos = len(caso.findall('failure')) + len(caso.findall('error'))
                n, f = datos[clase].get(metodo, (0, 0))
                datos[clase][metodo] = (n + 1, f + malos)
    return datos


def legible(metodo):
    texto = metodo.replace('_', ' | ')
    texto = re.sub(r'(?<=[a-z])(?=[A-Z])', ' ', texto)
    texto = re.sub(r'(?<=[A-Z])(?=[A-Z][a-z])', ' ', texto)
    texto = re.sub(r'(?<=[a-zA-Z])(?=\d)', ' ', texto)
    texto = re.sub(r'(?<=\d)(?=[a-zA-Z])', ' ', texto)
    palabras = [p if (p.isupper() and len(p) > 1) else p.lower() for p in texto.split(' ')]
    salida = ' '.join(p for p in palabras if p).replace(' | ', ': ')
    # El troceo por mayusculas parte los acronimos pegados a una palabra siguiente.
    for antes, despues in (('OU ty', 'OUT y'), ('hs 256', 'HS256'), ('sku ', 'SKU '),
                           ('Sku ', 'SKU '), ('json', 'JSON'), ('cors', 'CORS')):
        salida = salida.replace(antes, despues)
    return salida[0].upper() + salida[1:] if salida else metodo


def tabla_junit(salida, resultados):
    for titulo, clases in SECCIONES:
        salida.append(f"\n## {titulo}\n")
        for clase in clases:
            rutas = glob.glob(os.path.join(SRC, f'**/{clase}.java'), recursive=True)
            if not rutas:
                continue
            casos = orden_en_codigo(rutas[0])
            if not casos:
                continue
            salida.append(f"\n### `{clase}` — prefijo `{PREFIJOS[clase]}`\n")
            salida.append("| ID | Caso | Ejec. | Resultado |")
            salida.append("|---|---|---|---|")
            por_clase = resultados.get(clase, {})
            for i, (nombre, titulo_visible) in enumerate(casos, 1):
                entrada = por_clase.get(nombre) or por_clase.get(titulo_visible)
                if entrada:
                    n, fallos = entrada
                    estado = 'OK' if fallos == 0 else f'FALLA ({fallos})'
                    ejec = str(n)
                elif por_clase:
                    fallos = sum(f for _, f in por_clase.values())
                    estado = 'OK' if fallos == 0 else 'revisar'
                    ejec = 'parametrico'
                elif clase in FUERA_DEL_PORTON:
                    estado, ejec = 'flujo nocturno', '—'
                else:
                    estado, ejec = 'no ejecutado', '0'
                texto = titulo_visible or legible(nombre)
                salida.append(f"| {PREFIJOS[clase]}-{i:02d} | {texto} | {ejec} | {estado} |")


def tabla_bdd(salida):
    ruta = os.path.join(RAIZ, 'build/test-results/test/'
                              'TEST-pucmm.freddy.fullstacktesting.api.RunCucumberTest.xml')
    if not os.path.exists(ruta):
        return
    ejecutados = {}
    for caso in ET.parse(ruta).getroot().findall('testcase'):
        base = re.sub(r' - Ejemplos - Example #.*$', '', caso.get('name'))
        malos = len(caso.findall('failure')) + len(caso.findall('error'))
        n, f = ejecutados.get(base, (0, 0))
        ejecutados[base] = (n + 1, f + malos)

    salida.append("\n## Pruebas BDD de la API (Cucumber)\n")
    for archivo, prefijo in FEATURES:
        ruta_feature = os.path.join(RAIZ, f'src/test/resources/features/{archivo}.feature')
        texto = open(ruta_feature, encoding='utf-8').read()
        caracteristica = re.search(r'Característica: (.+)', texto).group(1).strip()
        escenarios = re.findall(r'^\s*(?:Escenario|Esquema del escenario):\s*(.+)$', texto, re.M)
        salida.append(f"\n### `{archivo}.feature` — {caracteristica} — prefijo `{prefijo}`\n")
        salida.append("| ID | Escenario | Ejemplos | Resultado |")
        salida.append("|---|---|---|---|")
        for i, nombre in enumerate(escenarios, 1):
            n, fallos = ejecutados.get(f"{caracteristica} - {nombre.strip()}", (0, 0))
            estado = 'no ejecutado' if n == 0 else ('OK' if fallos == 0 else f'FALLA ({fallos})')
            salida.append(f"| {prefijo}-{i:02d} | {nombre.strip()} | {n} | {estado} |")


def tabla_e2e(salida):
    ruta = os.path.join(RAIZ, 'playwright-report/results.json')
    if not os.path.exists(ruta):
        salida.append("\n## Pruebas E2E (Playwright)\n")
        salida.append("\n_Sin `playwright-report/results.json`; ejecutar "
                      "`npx playwright test --reporter=json > playwright-report/results.json`._")
        return
    informe = json.load(open(ruta, encoding='utf-8'))
    filas = []

    def recorrer(suites, archivo=None, titulo_suite=None):
        for suite in suites:
            nuevo_archivo = os.path.basename(suite.get('file') or archivo or '')
            nuevo_titulo = suite.get('title') if suite.get('file') is None or archivo else titulo_suite
            for spec in suite.get('specs', []):
                estado = 'OK' if all(t.get('status') == 'expected' for t in spec.get('tests', [])) else 'FALLA'
                filas.append((nuevo_archivo, suite.get('title', ''), spec.get('title', ''), estado))
            recorrer(suite.get('suites', []), nuevo_archivo, nuevo_titulo)

    recorrer(informe.get('suites', []))
    contador = defaultdict(int)
    agrupadas = defaultdict(list)
    for archivo, suite, titulo, estado in filas:
        prefijo = E2E_PREFIJOS.get((archivo, suite))
        if not prefijo:
            continue
        contador[prefijo] += 1
        agrupadas[prefijo].append((f"{prefijo}-{contador[prefijo]:02d}", archivo, suite, titulo, estado))

    salida.append("\n## Pruebas E2E (Playwright)\n")
    salida.append("| ID | Archivo | Suite | Escenario | Resultado |")
    salida.append("|---|---|---|---|---|")
    for prefijo in E2E_ORDEN:
        for fila in agrupadas.get(prefijo, []):
            salida.append("| {} | `{}` | {} | {} | {} |".format(*fila))


def main():
    resultados = resultados_junit()
    if not resultados:
        print("No hay resultados en build/test-results. Ejecuta ./gradlew test securityTest.",
              file=sys.stderr)
        return 1

    total = sum(n for clase in resultados.values() for n, _ in clase.values())
    fallos = sum(f for clase in resultados.values() for _, f in clase.values())

    salida = [
        "# Anexo A — Casos de prueba",
        "",
        "Documento **generado**: no se edita a mano.",
        "",
        "```bash",
        "./gradlew test securityTest",
        "npx playwright test --reporter=json > playwright-report/results.json",
        "python3 scripts/docs/gen_test_catalog.py",
        "```",
        "",
        f"Casos JUnit ejecutados: **{total}** · fallos: **{fallos}**. "
        "Los identificadores son estables y se referencian desde "
        "[01-requisitos.md](01-requisitos.md) y [05-guia-pruebas.md](05-guia-pruebas.md).",
    ]
    tabla_junit(salida, resultados)
    tabla_bdd(salida)
    tabla_e2e(salida)

    with open(DESTINO, 'w', encoding='utf-8') as f:
        f.write('\n'.join(salida) + '\n')
    print(f"escrito {DESTINO} ({total} casos JUnit, {fallos} fallos)")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
