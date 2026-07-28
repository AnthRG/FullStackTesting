# language: es
Característica: API de productos
  Como consumidor de la API de inventario
  quiero que el CRUD de productos responda con los codigos y payloads acordados
  para poder programar contra ella sin sorpresas.

  Antecedentes:
    Dado que estoy autenticado como "admin"

  Escenario: crear un producto valido devuelve el recurso creado
    Cuando creo un producto valido
    Entonces la respuesta tiene codigo 201
    Y la respuesta incluye el campo "id"
    Y la respuesta incluye el campo "sku"
    Y la respuesta incluye el campo "createdAt"

  Escenario: el SKU no se puede repetir
    Cuando creo un producto valido
    Y creo otro producto con el mismo SKU
    Entonces la respuesta tiene codigo 409
    Y la respuesta es un problema con formato RFC 7807

  Escenario: un producto sin nombre no se acepta
    Cuando creo un producto sin nombre
    Entonces la respuesta tiene codigo 400
    Y el error identifica el campo invalido "name"

  Escenario: un producto con precio negativo no se acepta
    Cuando creo un producto con precio negativo
    Entonces la respuesta tiene codigo 400
    Y el error identifica el campo invalido "price"

  Escenario: consultar un producto que no existe devuelve 404
    Cuando consulto el producto con id 999999
    Entonces la respuesta tiene codigo 404
    Y la respuesta es un problema con formato RFC 7807

  Escenario: actualizar un producto refleja el cambio
    Cuando creo un producto valido
    Y actualizo el producto creado con el nombre "Nombre actualizado"
    Entonces la respuesta tiene codigo 200
    Y el campo "name" vale "Nombre actualizado"

  Escenario: un producto eliminado deja de existir
    Cuando creo un producto valido
    Y elimino el producto creado
    Entonces la respuesta tiene codigo 204
    Cuando consulto el producto creado
    Entonces la respuesta tiene codigo 404

  Escenario: el listado de productos viene paginado
    Cuando listo los productos
    Entonces la respuesta tiene codigo 200
    Y la lista viene paginada
