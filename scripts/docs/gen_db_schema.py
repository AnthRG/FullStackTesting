#!/usr/bin/env python3
"""Convierte la salida de `tbls doc` en un unico anexo Markdown.

Se invoca desde scripts/docs/generate.sh; recibe la carpeta que produjo tbls.
Deja fuera flyway_schema_history, que es tabla de la herramienta y no del dominio.
"""
import json
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DESTINO = os.path.join(RAIZ, 'docs/anexo-c-esquema-bd.md')
EXCLUIDAS = {'public.flyway_schema_history'}

CABECERA = """# Anexo C — Esquema de base de datos

Documento **generado** con [tbls](https://github.com/k1LoW/tbls) a partir de la base de datos
real, después de aplicar las migraciones de Flyway. No se edita a mano.

```bash
bash scripts/docs/generate.sh db
```

---
"""


def diagrama_er(readme):
    bloque = re.search(r'```mermaid\n(.*?)```', readme, re.S)
    if not bloque:
        return ''
    contenido = bloque.group(1)
    # Quitar la tabla de control de Flyway del diagrama
    contenido = re.sub(r'"public\.flyway_schema_history" \{.*?\n\}\n', '', contenido, flags=re.S)
    contenido = re.sub(r'.*flyway_schema_history.*\n', '', contenido)
    contenido = contenido.replace('"public.', '"')
    return "\n## Diagrama entidad-relación\n\n```mermaid\n" + contenido.strip() + "\n```\n"


def tablas(esquema):
    salida = ["\n## Tablas\n"]
    for tabla in esquema.get('tables', []):
        nombre = tabla['name']
        if nombre in EXCLUIDAS:
            continue
        salida.append(f"\n### `{nombre.replace('public.', '')}`\n")
        salida.append("| Columna | Tipo | Nulo | Por defecto |")
        salida.append("|---|---|---|---|")
        for col in tabla.get('columns', []):
            nulo = 'sí' if col.get('nullable') else 'no'
            defecto = col.get('default') or '—'
            salida.append(f"| `{col['name']}` | {col['type']} | {nulo} | {defecto} |")
        restricciones = [c for c in tabla.get('constraints', [])
                         if c.get('type') in ('CHECK', 'FOREIGN KEY', 'UNIQUE', 'PRIMARY KEY')]
        if restricciones:
            salida.append("\n**Restricciones**\n")
            for c in restricciones:
                salida.append(f"- `{c['name']}` — {c.get('def', c.get('type'))}")
        indices = tabla.get('indexes', [])
        if indices:
            salida.append("\n**Índices**\n")
            for i in indices:
                salida.append(f"- `{i['name']}`")
    return '\n'.join(salida)


def main():
    if len(sys.argv) < 2:
        print("uso: gen_db_schema.py <carpeta de tbls>", file=sys.stderr)
        return 2
    carpeta = sys.argv[1]
    readme = open(os.path.join(carpeta, 'README.md'), encoding='utf-8').read()
    esquema = json.load(open(os.path.join(carpeta, 'schema.json'), encoding='utf-8'))

    with open(DESTINO, 'w', encoding='utf-8') as f:
        f.write(CABECERA + diagrama_er(readme) + tablas(esquema) + '\n')
    print(f"escrito {DESTINO}")
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
