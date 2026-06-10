# language: es
Característica: Salud de la aplicación
  El backend expone su estado en /actuator/health para que
  el frontend y la infraestructura puedan verificarlo sin autenticarse.

  Escenario: El endpoint de salud responde UP
    Cuando consulto el endpoint de salud
    Entonces la respuesta tiene codigo 200
    Y el estado reportado es "UP"
