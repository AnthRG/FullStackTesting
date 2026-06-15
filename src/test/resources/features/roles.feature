# language: es
Característica: Autenticación y roles
  El backend autentica contra Keycloak, protege los endpoints con Spring Security
  y aplica los roles de cada usuario.

  Escenario: Un usuario válido obtiene un token
    Cuando inicio sesion con "admin" y "admin"
    Entonces el login responde codigo 200
    Y recibo un token de acceso

  Escenario: Las credenciales inválidas son rechazadas
    Cuando inicio sesion con "admin" y "claveincorrecta"
    Entonces el login responde codigo 401

  Escenario: El endpoint protegido rechaza peticiones sin token
    Cuando consulto mis datos sin token
    Entonces la consulta responde codigo 401

  Esquema del escenario: Cada usuario reporta sus roles asignados
    Cuando inicio sesion con "<usuario>" y "<clave>"
    Y consulto mis datos con el token
    Entonces la consulta responde codigo 200
    Y mis roles son "<roles>"

    Ejemplos:
      | usuario | clave | roles                 |
      | admin   | admin | EDIT_ROLES,VIEW_ROLES |
      | user1   | user1 | VIEW_ROLES            |
      | user2   | user2 |                       |

  Escenario: Un administrador puede asignar y quitar un rol
    Dado que el usuario "user2" no tiene el rol "EDIT_ROLES"
    Cuando asigno el rol "EDIT_ROLES" al usuario "user2"
    Entonces el usuario "user2" tiene el rol "EDIT_ROLES"
    Cuando quito el rol "EDIT_ROLES" al usuario "user2"
    Entonces el usuario "user2" no tiene el rol "EDIT_ROLES"
