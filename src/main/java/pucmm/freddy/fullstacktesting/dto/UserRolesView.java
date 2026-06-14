package pucmm.freddy.fullstacktesting.dto;

import java.util.List;

/**
 * Vista de un usuario con sus roles de realm asignados.
 *
 * <p>Es el contrato de salida de la API: al ser un {@code record} con campos
 * nombrados, OpenAPI/Swagger genera el schema sin trabajo adicional.</p>
 *
 * @param id         identificador del usuario en Keycloak
 * @param username   nombre de usuario
 * @param email      correo electronico (puede ser {@code null})
 * @param enabled    si la cuenta esta habilitada
 * @param realmRoles roles de realm asignados (sin los roles internos de Keycloak)
 */
public record UserRolesView(
        String id,
        String username,
        String email,
        boolean enabled,
        List<String> realmRoles) {
}
