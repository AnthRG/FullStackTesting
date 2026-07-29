#!/usr/bin/env python3
"""Genera los realms de staging y produccion a partir de keycloak/realm-export.json.

realm-export.json es la fuente unica: lo consumen los tests (AbstractIntegrationTest
lo carga por nombre de classpath) y el compose de desarrollo. Los otros dos se derivan
de el para que no puedan quedar desincronizados cuando se agrega un rol o un scope.

Uso: python3 scripts/build-realms.py
"""

import json
import pathlib

RAIZ = pathlib.Path(__file__).resolve().parent.parent
ORIGEN = RAIZ / "keycloak" / "realm-export.json"

DESTINOS = {
    "realm-staging.json": {
        "origen_publico": "https://stg.cloudsus.net",
        "conservar_usuarios": True,
        "direct_access_grants": True,
    },
    "realm-prod.json": {
        "origen_publico": "https://app.cloudsus.net",
        "conservar_usuarios": False,
        "direct_access_grants": False,
    },
}


def derivar(base: dict, opciones: dict) -> dict:
    realm = json.loads(json.dumps(base))
    publico = opciones["origen_publico"]

    for cliente in realm.get("clients", []):
        cliente["redirectUris"] = [f"{publico}/*"]
        cliente["webOrigins"] = [publico]
        cliente["directAccessGrantsEnabled"] = opciones["direct_access_grants"]

        # El logout tambien valida su redirect contra una lista aparte. Si se queda con el
        # valor de desarrollo, Keycloak rechaza el post_logout_redirect_uri del entorno
        # publicado y "cerrar sesion" no cierra nada.
        atributos = cliente.get("attributes")
        if atributos and "post.logout.redirect.uris" in atributos:
            atributos["post.logout.redirect.uris"] = f"{publico}/*"

    if not opciones["conservar_usuarios"]:
        realm.pop("users", None)

    return realm


def main() -> None:
    base = json.loads(ORIGEN.read_text())

    for nombre, opciones in DESTINOS.items():
        destino = ORIGEN.parent / nombre
        destino.write_text(json.dumps(derivar(base, opciones), indent=2) + "\n")
        usuarios = len(json.loads(destino.read_text()).get("users", []))
        print(f"{nombre}: {opciones['origen_publico']}, {usuarios} usuarios")


if __name__ == "__main__":
    main()
