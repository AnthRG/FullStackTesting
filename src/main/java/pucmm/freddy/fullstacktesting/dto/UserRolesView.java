package pucmm.freddy.fullstacktesting.dto;

import java.util.List;

/**
 * Vista de un usuario con sus roles de realm asignados.
 *
 * <p>Es el contrato de salida de la API: al ser un {@code record} con campos
 * nombrados, OpenAPI/Swagger genera el schema sin trabajo adicional.</p>
 *
 * @param id             identificador del usuario en Keycloak
 * @param username       nombre de usuario
 * @param email          correo electronico (puede ser {@code null})
 * @param enabled        si la cuenta esta habilitada
 * @param realmRoles     roles asignados directamente, los unicos que se pueden quitar
 * @param effectiveRoles todo lo que el usuario tiene de verdad, incluido lo que hereda de
 *                       un rol compuesto. Es lo que viaja en su token, y por tanto lo que
 *                       decide que puede hacer. Sin este campo la UI muestra un permiso
 *                       como quitado cuando el compuesto se lo sigue otorgando.
 */
public record UserRolesView(
        String id,
        String username,
        String email,
        boolean enabled,
        List<String> realmRoles,
        List<String> effectiveRoles) {
}
