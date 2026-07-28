# language: es
Característica: codigos de estado de autorizacion
  Como consumidor de la API
  quiero que la API distinga "no se quien eres" de "se quien eres pero no puedes"
  para reaccionar correctamente ante cada caso.

  Escenario: sin token la API responde 401
    Dado que no estoy autenticado
    Cuando listo los productos
    Entonces la respuesta tiene codigo 401

  Escenario: un usuario autenticado sin permisos recibe 403
    Dado que estoy autenticado como "user2"
    Cuando listo los productos
    Entonces la respuesta tiene codigo 403

  Escenario: un usuario de solo lectura no puede crear productos
    Dado que estoy autenticado como "user1"
    Cuando creo un producto valido
    Entonces la respuesta tiene codigo 403

  Escenario: un usuario de solo lectura si puede listarlos
    Dado que estoy autenticado como "user1"
    Cuando listo los productos
    Entonces la respuesta tiene codigo 200

  Escenario: el operador de inventario si puede registrar movimientos
    Dado que estoy autenticado como "operator"
    Y que existe un producto con cantidad 10
    Cuando registro una entrada de 5 unidades
    Entonces la respuesta tiene codigo 201
    Y el campo "userId" vale "operator"
