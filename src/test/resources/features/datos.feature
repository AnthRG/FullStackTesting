# language: es
Característica: Validacion de datos de la API
  Como consumidor de la API de inventario
  quiero que los datos invalidos se rechacen en el borde y con un error entendible
  para que ningun dato corrupto llegue a la base.

  Antecedentes:
    Dado que estoy autenticado como "admin"

  # Particion valida: el valor mas extremo que la regla todavia acepta.
  Esquema del escenario: los valores limite validos se aceptan
    Cuando creo un producto con "<campo>" igual a "<valor>"
    Entonces la respuesta tiene codigo 201

    Ejemplos:
      | campo        | valor       |
      | price        | 0           |
      | price        | 9999999999.99 |
      | quantity     | 0           |
      | minimumStock | 0           |
      | name         | [texto:150] |
      | category     | [texto:50]  |

  # Particion invalida: el primer valor pasado el limite.
  Esquema del escenario: los valores fuera de rango se rechazan senalando el campo
    Cuando creo un producto con "<campo>" igual a "<valor>"
    Entonces la respuesta tiene codigo 400
    Y el error identifica el campo invalido "<campo>"
    Y la respuesta es un problema con formato RFC 7807

    Ejemplos:
      | campo        | valor       |
      | price        | -0.01       |
      | price        | 1.234       |
      | quantity     | -1          |
      | minimumStock | -1          |
      | name         | [texto:151] |
      | sku          | [texto:51]  |
      | category     | [texto:51]  |

  # Los obligatorios: ausentes y en blanco son casos distintos y ambos deben fallar.
  Esquema del escenario: los campos obligatorios no admiten nulo ni blanco
    Cuando creo un producto con "<campo>" igual a "<valor>"
    Entonces la respuesta tiene codigo 400
    Y el error identifica el campo invalido "<campo>"

    Ejemplos:
      | campo        | valor   |
      | name         | [nulo]  |
      | name         | [vacio] |
      | sku          | [nulo]  |
      | sku          | [vacio] |
      | category     | [nulo]  |
      | category     | [vacio] |
      | price        | [nulo]  |
      | quantity     | [nulo]  |
      | minimumStock | [nulo]  |
      | status       | [nulo]  |

  Escenario: un estado fuera del dominio no se acepta
    Cuando creo un producto con "status" igual a "PENDIENTE"
    Entonces la respuesta tiene codigo 400

  # Ida y vuelta: lo que se guarda tiene que ser exactamente lo que se envio.
  Escenario: el precio se devuelve con los dos decimales que admite la columna
    Cuando creo un producto con "price" igual a "1234.56"
    Entonces la respuesta tiene codigo 201
    Y el campo "price" vale "1234.56"
    Cuando consulto el producto creado
    Entonces la respuesta tiene codigo 200
    Y el campo "price" vale "1234.56"

  Esquema del escenario: la cantidad de un movimiento tiene que ser positiva
    Dado que existe un producto con cantidad 10
    Cuando registro una entrada de <cantidad> unidades
    Entonces la respuesta tiene codigo 400
    Y el error identifica el campo invalido "quantity"

    Ejemplos:
      | cantidad |
      | 0        |
      | -5       |

  Escenario: un rango de fechas invertido se rechaza
    Cuando consulto los movimientos por tipo desde "2026-01-31" hasta "2026-01-01"
    Entonces la respuesta tiene codigo 400
    Y la respuesta es un problema con formato RFC 7807
