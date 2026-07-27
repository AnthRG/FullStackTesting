# language: es
Característica: API de movimientos de stock
  Como consumidor de la API de inventario
  quiero registrar entradas, salidas y ajustes por HTTP
  para que el stock del producto quede siempre consistente con su historial.

  Antecedentes:
    Dado que estoy autenticado como "admin"

  Escenario: una entrada suma al stock del producto
    Dado que existe un producto con cantidad 10
    Cuando registro una entrada de 5 unidades
    Entonces la respuesta tiene codigo 201
    Y el campo "previousQuantity" vale 10
    Y el campo "newQuantity" vale 15
    Y el campo "movementType" vale "IN"

  Escenario: una salida resta del stock del producto
    Dado que existe un producto con cantidad 10
    Cuando registro una salida de 3 unidades
    Entonces la respuesta tiene codigo 201
    Y el campo "previousQuantity" vale 10
    Y el campo "newQuantity" vale 7

  Escenario: no se puede sacar mas stock del disponible
    Dado que existe un producto con cantidad 10
    Cuando registro una salida de 50 unidades
    Entonces la respuesta tiene codigo 409
    Y la respuesta es un problema con formato RFC 7807

  Escenario: un ajuste fija la cantidad exacta sin importar la anterior
    Dado que existe un producto con cantidad 10
    Cuando registro un ajuste a 99 unidades
    Entonces la respuesta tiene codigo 201
    Y el campo "previousQuantity" vale 10
    Y el campo "newQuantity" vale 99

  Escenario: no se puede mover stock de un producto inexistente
    Cuando registro una entrada de 5 unidades sobre el producto 999999
    Entonces la respuesta tiene codigo 404

  Escenario: el movimiento queda registrado a nombre de quien lo hizo
    Dado que existe un producto con cantidad 10
    Cuando registro una entrada de 5 unidades
    Entonces la respuesta tiene codigo 201
    Y el campo "userId" vale "admin"

  Escenario: el historial de movimientos viene paginado
    Cuando consulto el historial de movimientos
    Entonces la respuesta tiene codigo 200
    Y la lista viene paginada
